import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Role } from '../../constants/api.enums.js';
import mongoose, { HydratedDocument } from 'mongoose';

export type UserType = HydratedDocument<User>; // This has _id prop

@Schema()
export class User {
  @Prop({ required: true, unique: true, lowercase: true, trim: true })
  email: string;

  @Prop({ required: true })
  password: string;

  @Prop({ type: String, enum: Role, required: true })
  role: Role;

  @Prop({ required: true, default: Date.now })
  createdAt: Date;

  /** Name and surname live on the profile. Set right after the user is created. */
  @Prop({ type: mongoose.Schema.Types.ObjectId, ref: 'Profile' })
  profile?: mongoose.Types.ObjectId;
}

export const UserSchema = SchemaFactory.createForClass(User);
