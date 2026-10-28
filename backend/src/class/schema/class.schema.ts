import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import mongoose, { HydratedDocument } from 'mongoose';

export type SchoolClassType = HydratedDocument<SchoolClass>;

/**
 * A class is teaching content, e.g. "Maths 4": numbered chapters with notes and
 * flashcards (the `Chapter` collection) and its learning paths. One teacher owns
 * and edits it. Any number of classrooms (groups of students) can use it.
 */
@Schema({ collection: 'classes' })
export class SchoolClass {
  @Prop({ required: true, trim: true })
  name: string;

  /** School year it is meant for, 1 to 9. */
  @Prop({ min: 1, max: 9 })
  grade?: number;

  @Prop({ type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true })
  teacher: mongoose.Types.ObjectId;

  @Prop({ default: Date.now })
  createdAt: Date;
}

export const SchoolClassSchema = SchemaFactory.createForClass(SchoolClass);
SchoolClassSchema.index({ teacher: 1 });
