import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import mongoose, { HydratedDocument } from 'mongoose';

export enum UnlockMode {
  /** One lesson after another; finishing a lesson opens the next. */
  IN_ORDER = 'inOrder',
  /** Every chapter is open; inside a chapter lessons still go in order. */
  ANY_CHAPTER = 'anyChapter',
}

/**
 * A lesson slot on the path. Keeps its `_id` across edits of the path, so its
 * questions stay attached. The last lesson of a chapter is usually its quiz.
 */
@Schema()
export class PathLesson {
  @Prop({ required: true, trim: true })
  title: string;

  @Prop({ default: false })
  isChapterQuiz: boolean;

  /** Stars a student gets the first time they finish the lesson. Set by the teacher. */
  @Prop({ default: 10, min: 0, max: 100 })
  stars: number;

  /**
   * Questions the teacher wrote, in the app's own JSON shape (question plus answer key).
   * Stored as they come for now; empty means students practise with the chapter's flashcards.
   */
  @Prop({ type: [mongoose.Schema.Types.Mixed], default: [] })
  questions: unknown[];
}

export const PathLessonSchema = SchemaFactory.createForClass(PathLesson);

/** One chapter of the class on the path, with its lessons in order. */
@Schema({ _id: false })
export class PathUnit {
  @Prop({ type: mongoose.Schema.Types.ObjectId, ref: 'Chapter', required: true })
  chapter: mongoose.Types.ObjectId;

  @Prop({ type: [PathLessonSchema], default: [] })
  lessons: mongoose.Types.DocumentArray<PathLesson>;
}

export const PathUnitSchema = SchemaFactory.createForClass(PathUnit);

export type LearningPathType = HydratedDocument<LearningPath>;

/**
 * An ordered way through one class's chapters, with lessons in each. Assigned to
 * classrooms that use the class; a classroom follows at most one path per class.
 */
@Schema({ timestamps: true })
export class LearningPath {
  @Prop({ type: mongoose.Schema.Types.ObjectId, ref: 'SchoolClass', required: true })
  schoolClass: mongoose.Types.ObjectId;

  @Prop({ type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true })
  teacher: mongoose.Types.ObjectId;

  @Prop({ required: true, trim: true })
  name: string;

  @Prop({ type: String, enum: UnlockMode, default: UnlockMode.IN_ORDER })
  unlockMode: UnlockMode;

  @Prop({ type: [PathUnitSchema], default: [] })
  units: PathUnit[];

  @Prop({ type: [{ type: mongoose.Schema.Types.ObjectId, ref: 'Classroom' }], default: [] })
  classrooms: mongoose.Types.ObjectId[];
}

export const LearningPathSchema = SchemaFactory.createForClass(LearningPath);
LearningPathSchema.index({ teacher: 1 });
LearningPathSchema.index({ 'units.lessons._id': 1 });
