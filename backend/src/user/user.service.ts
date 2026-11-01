import { ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import bcrypt from 'bcrypt';
import { User, UserType } from '../auth/schema/user.schema.js';
import { Profile } from '../profile/schema/profile.schema.js';
import { Model } from 'mongoose';
import { InjectModel } from '@nestjs/mongoose';
import { MeDto } from './dto/me.dto.js';
import { UpdateProfileDto } from './dto/profile.dto.js';

@Injectable()
export class UserService {
  constructor(
    @InjectModel(User.name) private userModel: Model<User>,
    @InjectModel(Profile.name) private profileModel: Model<Profile>,
    private readonly configService: ConfigService,
  ) {}

  async updateProfile(user: UserType, dto: UpdateProfileDto): Promise<MeDto> {
    const updated = await this.profileModel.updateOne(
      { user: user._id },
      { $set: { name: dto.name, surname: dto.surname } },
    );
    if (updated.matchedCount === 0) {
      throw new NotFoundException('Profile not found');
    }
    return this.getMe(user);
  }

  /**
   * Answers 403 (not 401) for a wrong current password, so apps don't take it for an
   * expired session. Existing sessions on other devices stay signed in.
   */
  async changePassword(user: UserType, currentPassword: string, newPassword: string): Promise<void> {
    if (!(await bcrypt.compare(currentPassword, user.password))) {
      throw new ForbiddenException('Current password is wrong');
    }
    const rounds = Number(this.configService.get<number>('BCRYPT_SALT_ROUNDS') ?? 10);
    user.password = await bcrypt.hash(newPassword, await bcrypt.genSalt(rounds));
    await user.save();
  }

  async findById(userId: string): Promise<UserType | null> {
    return await this.userModel.findById(userId);
  }

  /** The signed-in user with their profile. Never includes the password hash. */
  async getMe(user: UserType): Promise<MeDto> {
    const profile = await this.profileModel.findOne({ user: user._id });
    if (profile == null) {
      throw new NotFoundException('Profile not found');
    }
    return {
      id: user._id.toString(),
      email: user.email,
      role: user.role,
      name: profile.name,
      surname: profile.surname,
      createdAt: user.createdAt,
    };
  }
}
