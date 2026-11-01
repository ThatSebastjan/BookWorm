import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import mongoose from 'mongoose';

/**
 * One refresh token, stored by the SHA-256 of the token (never the token itself).
 * Rotating a token adds a new one to the same family, the family of the original sign-in.
 */
@Schema({ collection: 'refresh_tokens', versionKey: false })
export class RefreshToken {
  /** SHA-256 of the token. */
  @Prop({ type: String, required: true })
  _id: string;

  @Prop({ required: true })
  familyId: string;

  @Prop({ required: true })
  userId: string;

  @Prop({ required: true })
  createdAt: Date;

  @Prop({ required: true })
  expiresAt: Date;

  @Prop({ required: true })
  familyExpiresAt: Date;

  @Prop()
  usedAt?: Date;

  @Prop({ type: mongoose.Schema.Types.Mixed })
  claims?: Record<string, unknown>;
}

export const RefreshTokenSchema = SchemaFactory.createForClass(RefreshToken);
RefreshTokenSchema.index({ familyId: 1 });
RefreshTokenSchema.index({ userId: 1 });
RefreshTokenSchema.index({ familyExpiresAt: 1 });

/**
 * A revoked family: signed out, or a used token was presented again. Kept apart from the
 * tokens so it also covers tokens of the family saved after the revocation.
 */
@Schema({ collection: 'refresh_token_revocations', versionKey: false })
export class RefreshTokenRevocation {
  /** The family id. */
  @Prop({ type: String, required: true })
  _id: string;

  /** When the family would have ended anyway; the revocation can go after that. Unset when unknown. */
  @Prop()
  familyExpiresAt?: Date;
}

export const RefreshTokenRevocationSchema = SchemaFactory.createForClass(RefreshTokenRevocation);
RefreshTokenRevocationSchema.index({ familyExpiresAt: 1 });
