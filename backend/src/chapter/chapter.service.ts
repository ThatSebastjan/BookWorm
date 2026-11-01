import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import mongoose, { isValidObjectId, Model } from 'mongoose';
import { UserType } from '../auth/schema/user.schema.js';
import { ClassService } from '../class/class.service.js';
import { Role } from '../constants/api.enums.js';
import { dayKey } from '../learn/path-progress.js';
import { PracticeDay, recordPracticeDay } from '../learn/schema/lesson-progress.schema.js';
import { LearningPath } from '../learning-path/schema/learning-path.schema.js';
import { ChapterDto, UpdateChapterDto } from './dto/chapter.dto.js';
import { Chapter, ChapterType } from './schema/chapter.schema.js';
import { FlashcardProgress } from './schema/flashcard-progress.schema.js';

/** One chapter: students read it and practise its cards; the class's teacher edits it. */
@Injectable()
export class ChapterService {
  constructor(
    @InjectModel(Chapter.name) private chapterModel: Model<Chapter>,
    @InjectModel(FlashcardProgress.name) private progressModel: Model<FlashcardProgress>,
    @InjectModel(LearningPath.name) private pathModel: Model<LearningPath>,
    @InjectModel(PracticeDay.name) private practiceDayModel: Model<PracticeDay>,
    private readonly classService: ClassService,
  ) {}

  async getChapter(user: UserType, chapterId: string): Promise<ChapterDto> {
    const chapter = await this.find(chapterId);
    const schoolClass = await this.classService.findReadable(user, chapter.schoolClass);
    const known = await this.progressModel.find({ user: user._id, chapter: chapter._id, known: true });
    return this.toDto(
      chapter,
      schoolClass.name,
      await this.classService.teacherName(schoolClass),
      new Set(known.map((p) => p.flashcard.toString())),
    );
  }

  /** Forgets which of the chapter's cards [user] knows, so they can go through them all again. */
  async resetProgress(user: UserType, chapterId: string): Promise<void> {
    const chapter = await this.find(chapterId);
    await this.classService.findReadable(user, chapter.schoolClass);
    await this.progressModel.deleteMany({ user: user._id, chapter: chapter._id });
  }

  /**
   * Replaces title, notes and flashcards. Cards sent with their id keep it, so students'
   * progress on them stays; cards left out are deleted together with that progress.
   */
  async updateChapter(user: UserType, chapterId: string, dto: UpdateChapterDto): Promise<ChapterDto> {
    const chapter = await this.find(chapterId);
    const schoolClass = await this.classService.findOwned(user, chapter.schoolClass);

    const existing = new Set(chapter.flashcards.map((card) => card._id.toString()));
    const kept = new Set(dto.flashcards.map((card) => card.id).filter((id): id is string => id != null && existing.has(id)));
    const removed = [...existing].filter((id) => !kept.has(id));

    chapter.title = dto.title;
    chapter.notes = dto.notes.map((note) => ({
      heading: note.heading,
      body: note.body,
      remember: note.remember?.trim() ? note.remember : undefined,
    }));
    chapter.set(
      'flashcards',
      dto.flashcards.map((card) => ({
        // An id from another chapter (or made up) is ignored: the card is new.
        _id: card.id != null && kept.has(card.id) ? new mongoose.Types.ObjectId(card.id) : new mongoose.Types.ObjectId(),
        term: card.term,
        definition: card.definition,
      })),
    );
    await chapter.save();
    if (removed.length > 0) {
      await this.progressModel.deleteMany({ flashcard: { $in: removed.map((id) => new mongoose.Types.ObjectId(id)) } });
    }
    return this.toDto(chapter, schoolClass.name, await this.classService.teacherName(schoolClass));
  }

  /** Deletes the chapter and its progress, takes it off learning paths, and closes the gap in the numbering. */
  async deleteChapter(user: UserType, chapterId: string): Promise<void> {
    const chapter = await this.find(chapterId);
    const schoolClass = await this.classService.findOwned(user, chapter.schoolClass);
    await this.progressModel.deleteMany({ chapter: chapter._id });
    await this.pathModel.updateMany({ 'units.chapter': chapter._id }, { $pull: { units: { chapter: chapter._id } } });
    await chapter.deleteOne();
    const rest = await this.chapterModel.find({ schoolClass: schoolClass._id }).sort({ number: 1 });
    await this.classService.renumber(rest.map((c) => c._id));
  }

  /** Remembers whether [user] knew the card this time; the latest answer wins. */
  async recordFlashcard(user: UserType, flashcardId: string, known: boolean) {
    const chapter = isValidObjectId(flashcardId)
      ? await this.chapterModel.findOne({ 'flashcards._id': flashcardId })
      : null;
    if (chapter == null) {
      throw new NotFoundException('Flashcard not found');
    }
    await this.classService.findReadable(user, chapter.schoolClass);

    await this.progressModel.updateOne(
      { user: user._id, flashcard: flashcardId },
      { $set: { known, chapter: chapter._id } },
      { upsert: true },
    );
    // Going through flashcards keeps the student's streak going too.
    if (user.role === Role.STUDENT) await recordPracticeDay(this.practiceDayModel, user._id, dayKey(new Date()));
  }

  private async find(chapterId: string): Promise<ChapterType> {
    const chapter = isValidObjectId(chapterId) ? await this.chapterModel.findById(chapterId) : null;
    if (chapter == null) {
      throw new NotFoundException('Chapter not found');
    }
    return chapter;
  }

  private toDto(
    chapter: ChapterType,
    className: string,
    teacherName: string,
    knownCardIds: Set<string> = new Set(),
  ): ChapterDto {
    return {
      id: chapter._id.toString(),
      classId: chapter.schoolClass.toString(),
      className,
      teacherName,
      number: chapter.number,
      title: chapter.title,
      notes: chapter.notes.map((note) => ({
        heading: note.heading,
        body: note.body,
        remember: note.remember ?? null,
      })),
      flashcards: chapter.flashcards.map((card) => ({
        id: card._id.toString(),
        term: card.term,
        definition: card.definition,
        known: knownCardIds.has(card._id.toString()),
      })),
    };
  }
}
