import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import mongoose, { HydratedDocument } from 'mongoose';

/** One section of a chapter's notes. */
@Schema({ _id: false })
export class NoteSection {
  @Prop({ required: true, trim: true })
  heading: string;

  @Prop({ required: true })
  body: string;

  /** Optional highlighted "Remember" box shown under the body. */
  @Prop({ trim: true })
  remember?: string;
}

export const NoteSectionSchema = SchemaFactory.createForClass(NoteSection);

/** A term and its definition. Keeps its own `_id`, which student progress points at. */
@Schema()
export class Flashcard {
  @Prop({ required: true, trim: true })
  term: string;

  @Prop({ required: true, trim: true })
  definition: string;
}

export const FlashcardSchema = SchemaFactory.createForClass(Flashcard);

export type ChapterType = HydratedDocument<Chapter>;

/** A numbered chapter of a class, with its notes and flashcards. Edited by the class's teacher. */
@Schema({ timestamps: true })
export class Chapter {
  @Prop({
    type: mongoose.Schema.Types.ObjectId,
    ref: 'SchoolClass',
    required: true,
  })
  schoolClass: mongoose.Types.ObjectId;

  /** 1, 2, 3… in the order students work through them. Unique within the class. */
  @Prop({ required: true, min: 1 })
  number: number;

  @Prop({ required: true, trim: true })
  title: string;

  @Prop({ type: [NoteSectionSchema], default: [] })
  notes: NoteSection[];

  @Prop({ type: [FlashcardSchema], default: [] })
  flashcards: mongoose.Types.DocumentArray<Flashcard>;
}

export const ChapterSchema = SchemaFactory.createForClass(Chapter);
ChapterSchema.index({ schoolClass: 1, number: 1 }, { unique: true });
ChapterSchema.index({ 'flashcards._id': 1 });
