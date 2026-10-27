import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import mongoose, { HydratedDocument } from 'mongoose';

export type ProfileType = HydratedDocument<Profile>;

/** Personal details of a user, kept apart from the login data on `User`. */
@Schema({ timestamps: true })
export class Profile {
  @Prop({
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
    unique: true,
  })
  user: mongoose.Types.ObjectId;

  @Prop({ required: true, trim: true })
  name: string;

  @Prop({ required: true, trim: true })
  surname: string;
}

export const ProfileSchema = SchemaFactory.createForClass(Profile);
