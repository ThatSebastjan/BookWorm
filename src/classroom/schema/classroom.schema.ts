import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import mongoose, { HydratedDocument } from 'mongoose';

export type ClassroomType = HydratedDocument<Classroom>;

@Schema()
export class Classroom {
  @Prop({ required: true })
  name: string;

  @Prop({ type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true })
  teacher: mongoose.Types.ObjectId; // NOTE: this is only an object id reference, must be populated manually

  @Prop({ default: Date.now })
  createdAt: Date;

  @Prop({ type: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }] })
  students: mongoose.Types.ObjectId[]; // NOTE: only object id, must be populated manually

  @Prop({ required: true })
  code: string;
}

export const ClassroomSchema = SchemaFactory.createForClass(Classroom);
