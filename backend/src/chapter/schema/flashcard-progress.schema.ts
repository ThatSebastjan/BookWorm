import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import mongoose, { HydratedDocument } from 'mongoose';

export type FlashcardProgressType = HydratedDocument<FlashcardProgress>;

/** Whether one student knew one flashcard the last time they practised it. */
@Schema({ timestamps: true })
export class FlashcardProgress {
  @Prop({ type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true })
  user: mongoose.Types.ObjectId;

  @Prop({ type: mongoose.Schema.Types.ObjectId, ref: 'Chapter', required: true })
  chapter: mongoose.Types.ObjectId;

  /** The `_id` of a flashcard inside the chapter. */
  @Prop({ type: mongoose.Schema.Types.ObjectId, required: true })
  flashcard: mongoose.Types.ObjectId;

  @Prop({ required: true })
  known: boolean;
}

export const FlashcardProgressSchema =
  SchemaFactory.createForClass(FlashcardProgress);
FlashcardProgressSchema.index({ user: 1, flashcard: 1 }, { unique: true });
FlashcardProgressSchema.index({ user: 1, chapter: 1 });
