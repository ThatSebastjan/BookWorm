import { Injectable } from '@nestjs/common';
import { User, UserType } from '../auth/schema/user.schema.js';
import { Model } from 'mongoose';
import { InjectModel } from '@nestjs/mongoose';

@Injectable()
export class UserService {
  constructor(@InjectModel(User.name) private userModel: Model<User>) {}

  async findById(userId: string): Promise<UserType | null> {
    return await this.userModel.findById(userId);
  }
}
