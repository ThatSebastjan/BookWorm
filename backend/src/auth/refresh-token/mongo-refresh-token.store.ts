import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { AuthenticationStorage } from '@nestjs/authentication';
import type { RefreshTokenRecord, RefreshTokenStore } from '@nestjs/authentication';
import { Model } from 'mongoose';
import { RefreshToken, RefreshTokenRevocation } from './refresh-token.schema.js';

/**
 * Keeps refresh tokens in MongoDB instead of the library's in-memory default, so users
 * stay signed in across backend restarts (and several instances can share them).
 */
@Injectable()
export class MongoRefreshTokenStore implements RefreshTokenStore {
  constructor(
    @InjectModel(RefreshToken.name) private tokenModel: Model<RefreshToken>,
    @InjectModel(RefreshTokenRevocation.name) private revocationModel: Model<RefreshTokenRevocation>,
    storage: AuthenticationStorage,
  ) {
    storage.registerSource({ refreshTokens: this });
  }

  async getRefreshToken(id: string): Promise<RefreshTokenRecord | undefined> {
    const token = await this.tokenModel.findById(id).lean();
    if (token == null) return undefined;
    const record: RefreshTokenRecord = {
      id: token._id,
      familyId: token.familyId,
      userId: token.userId,
      createdAt: token.createdAt,
      expiresAt: token.expiresAt,
      familyExpiresAt: token.familyExpiresAt,
    };
    // Optional fields come back absent, never null.
    if (token.usedAt != null) record.usedAt = token.usedAt;
    if (token.claims != null) record.claims = token.claims;
    return record;
  }

  async saveRefreshToken(record: RefreshTokenRecord): Promise<void> {
    // Times come from the token service's clock: tokens and revocations of families past their end can go.
    const now = record.createdAt;
    await this.tokenModel.deleteMany({ familyExpiresAt: { $lte: now } });
    await this.revocationModel.deleteMany({ familyExpiresAt: { $lte: now } });
    await this.tokenModel.create({
      _id: record.id,
      familyId: record.familyId,
      userId: record.userId,
      createdAt: record.createdAt,
      expiresAt: record.expiresAt,
      familyExpiresAt: record.familyExpiresAt,
      ...(record.usedAt != null && { usedAt: record.usedAt }),
      ...(record.claims != null && { claims: record.claims }),
    });
  }

  /** One conditional write, so of two concurrent refreshes with the same token only one wins. */
  async markRefreshTokenUsed(id: string, at: Date): Promise<boolean> {
    const result = await this.tokenModel.updateOne({ _id: id, usedAt: { $exists: false } }, { $set: { usedAt: at } });
    return result.modifiedCount === 1;
  }

  async revokeRefreshTokenFamily(familyId: string): Promise<void> {
    const token = await this.tokenModel.findOne({ familyId }, { familyExpiresAt: 1 }).lean();
    await this.revocationModel.updateOne(
      { _id: familyId },
      { $setOnInsert: token != null ? { familyExpiresAt: token.familyExpiresAt } : {} },
      { upsert: true },
    );
  }

  async isRefreshTokenFamilyRevoked(familyId: string): Promise<boolean> {
    return (await this.revocationModel.exists({ _id: familyId })) != null;
  }

  async revokeUserRefreshTokens(userId: string): Promise<void> {
    const families = await this.tokenModel.aggregate<{ _id: string; familyExpiresAt: Date }>([
      { $match: { userId } },
      { $group: { _id: '$familyId', familyExpiresAt: { $max: '$familyExpiresAt' } } },
    ]);
    if (families.length === 0) return;
    await this.revocationModel.bulkWrite(
      families.map((family) => ({
        updateOne: {
          filter: { _id: family._id },
          update: { $setOnInsert: { familyExpiresAt: family.familyExpiresAt } },
          upsert: true,
        },
      })),
    );
  }
}
