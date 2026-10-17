import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Role } from '../../constants/api.enums.js';
import { HydratedDocument } from 'mongoose';

export type UserType = HydratedDocument<User>;

@Schema()
export class User {

  @Prop({ required: true })
  username: string;

  @Prop({ required: true })
  password: string;

  @Prop({ type: String, enum: Role, required: true })
  role: Role;

  @Prop({ required: true, default: Date.now })
  createdAt: Date;


  // TODO: add a ObjectId ref to profile schema once we have it
  /*
  @Prop({ type: mongoose.Schema.Types.ObjectId, ref: 'Profile' })
  profile: Profile;
  */

}

export const UserSchema = SchemaFactory.createForClass(User);
