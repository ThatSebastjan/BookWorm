import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import mongoose, { HydratedDocument, Model } from 'mongoose';
import type { AnswerKey } from '../questions.js';

export type LessonAttemptType = HydratedDocument<LessonAttempt>;

/**
 * The lesson a student is doing right now. Opening a lesson starts a new attempt with the
 * answer keys of the questions it served; answers and finishing are checked against those.
 */
@Schema({ timestamps: true })
export class LessonAttempt {
  @Prop({ type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true })
  user: mongoose.Types.ObjectId;

  @Prop({ type: mongoose.Schema.Types.ObjectId, ref: 'LearningPath', required: true })
  path: mongoose.Types.ObjectId;

  /** The `_id` of a lesson inside the path. */
  @Prop({ type: mongoose.Schema.Types.ObjectId, required: true })
  lesson: mongoose.Types.ObjectId;

  @Prop({ type: [mongoose.Schema.Types.Mixed], default: [] })
  keys: AnswerKey[];

  /** Questions answered right in this attempt; each earns stars once. */
  @Prop({ type: [String], default: [] })
  correct: string[];
}

export const LessonAttemptSchema = SchemaFactory.createForClass(LessonAttempt);
LessonAttemptSchema.index({ user: 1, lesson: 1 }, { unique: true });

export type LessonCompletionType = HydratedDocument<LessonCompletion>;

/**
 * One finished lesson. A student can finish a lesson again, so there may be several per
 * lesson. Stars, streaks and path progress are all counted from these, and they stay when
 * the teacher changes or deletes the path, so students keep the stars they earned.
 */
@Schema({ timestamps: { createdAt: true, updatedAt: false } })
export class LessonCompletion {
  @Prop({ type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true })
  user: mongoose.Types.ObjectId;

  @Prop({ type: mongoose.Schema.Types.ObjectId, ref: 'LearningPath', required: true })
  path: mongoose.Types.ObjectId;

  @Prop({ type: mongoose.Schema.Types.ObjectId, required: true })
  lesson: mongoose.Types.ObjectId;

  @Prop({ default: false })
  isChapterQuiz: boolean;

  @Prop({ required: true })
  stars: number;

  @Prop({ required: true })
  correct: number;

  @Prop({ required: true })
  total: number;

  createdAt: Date;
}

export const LessonCompletionSchema = SchemaFactory.createForClass(LessonCompletion);
LessonCompletionSchema.index({ user: 1, createdAt: -1 });
LessonCompletionSchema.index({ lesson: 1 });

/** A day (YYYY-MM-DD, server time) a student practised: finished a lesson or went through flashcards. Streaks count these. */
@Schema({ collection: 'practice_days', versionKey: false })
export class PracticeDay {
  @Prop({ type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true })
  user: mongoose.Types.ObjectId;

  @Prop({ required: true })
  day: string;
}

export const PracticeDaySchema = SchemaFactory.createForClass(PracticeDay);
PracticeDaySchema.index({ user: 1, day: 1 }, { unique: true });

/** Marks today as practised for [user]. */
export async function recordPracticeDay(model: Model<PracticeDay>, user: mongoose.Types.ObjectId, day: string): Promise<void> {
  await model.updateOne({ user, day }, { $setOnInsert: { user, day } }, { upsert: true });
}
