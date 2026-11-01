import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import mongoose, { isValidObjectId, Model } from 'mongoose';
import { UserType } from '../auth/schema/user.schema.js';
import { ChapterSummaryDto } from '../chapter/dto/chapter.dto.js';
import { Chapter, ChapterType } from '../chapter/schema/chapter.schema.js';
import { FlashcardProgress } from '../chapter/schema/flashcard-progress.schema.js';
import { Classroom } from '../classroom/schema/classroom.schema.js';
import { LearningPath } from '../learning-path/schema/learning-path.schema.js';
import { displayNames } from '../profile/display-names.js';
import { Profile } from '../profile/schema/profile.schema.js';
import { CreateClassDto, LibraryClassDto, TeacherClassDto, UpdateClassDto } from './dto/class.dto.js';
import { SchoolClass, SchoolClassType } from './schema/class.schema.js';

type Id = string | mongoose.Types.ObjectId;

/**
 * Classes (teaching content) and their chapter lists. Also the access rules every
 * class-related route uses: its teacher may change it, and students of a classroom
 * using it may read it. Anyone else gets 404, so ids can't be probed.
 */
@Injectable()
export class ClassService {
  constructor(
    @InjectModel(SchoolClass.name) private classModel: Model<SchoolClass>,
    @InjectModel(Classroom.name) private classroomModel: Model<Classroom>,
    @InjectModel(Chapter.name) private chapterModel: Model<Chapter>,
    @InjectModel(FlashcardProgress.name) private progressModel: Model<FlashcardProgress>,
    @InjectModel(LearningPath.name) private pathModel: Model<LearningPath>,
    @InjectModel(Profile.name) private profileModel: Model<Profile>,
  ) {}

  // Access

  /** The class, if [user] teaches it. */
  async findOwned(user: UserType, classId: Id): Promise<SchoolClassType> {
    const schoolClass = isValidObjectId(classId) ? await this.classModel.findById(classId) : null;
    if (schoolClass == null || !schoolClass.teacher.equals(user._id)) {
      throw new NotFoundException('Class not found');
    }
    return schoolClass;
  }

  /** The class, if [user] teaches it or is a (let in) student of a classroom that uses it. */
  async findReadable(user: UserType, classId: Id): Promise<SchoolClassType> {
    const schoolClass = isValidObjectId(classId) ? await this.classModel.findById(classId) : null;
    const allowed =
      schoolClass != null &&
      (schoolClass.teacher.equals(user._id) ||
        (await this.classroomModel.exists({ students: user._id, classes: schoolClass._id })) != null);
    if (!allowed) {
      throw new NotFoundException('Class not found');
    }
    return schoolClass;
  }

  async teacherName(schoolClass: SchoolClassType): Promise<string> {
    return (await displayNames(this.profileModel, [schoolClass.teacher])).get(schoolClass.teacher.toString()) ?? '';
  }

  // Teacher

  async listForTeacher(user: UserType): Promise<TeacherClassDto[]> {
    const classes = await this.classModel.find({ teacher: user._id }).sort({ name: 1 });
    const [chapters, classrooms] = await Promise.all([
      this.chapterModel.find({ schoolClass: { $in: classes.map((c) => c._id) } }).sort({ number: 1 }),
      // Any classroom using the class, including ones the teacher co-teaches.
      this.classroomModel.find({ classes: { $in: classes.map((c) => c._id) } }, { classes: 1 }),
    ]);
    return classes.map((schoolClass) => ({
      id: schoolClass._id.toString(),
      name: schoolClass.name,
      grade: schoolClass.grade ?? null,
      chapters: this.summaries(chapters.filter((c) => c.schoolClass.equals(schoolClass._id))),
      classroomIds: classrooms
        .filter((room) => room.classes.some((id) => id.equals(schoolClass._id)))
        .map((room) => room._id.toString()),
    }));
  }

  async create(user: UserType, dto: CreateClassDto): Promise<TeacherClassDto> {
    const created = await this.classModel.create({ name: dto.name, grade: dto.grade, teacher: user._id });
    return { id: created._id.toString(), name: created.name, grade: created.grade ?? null, chapters: [], classroomIds: [] };
  }

  async update(user: UserType, classId: string, dto: UpdateClassDto): Promise<void> {
    const schoolClass = await this.findOwned(user, classId);
    if (dto.name !== undefined) schoolClass.name = dto.name;
    if (dto.grade !== undefined) schoolClass.grade = dto.grade;
    await schoolClass.save();
  }

  /** Deletes the class with its chapters, paths and everyone's progress, and takes it off classrooms. */
  async delete(user: UserType, classId: string): Promise<void> {
    const schoolClass = await this.findOwned(user, classId);
    const chapterIds = await this.chapterModel.find({ schoolClass: schoolClass._id }).distinct('_id');
    await this.progressModel.deleteMany({ chapter: { $in: chapterIds } });
    await this.chapterModel.deleteMany({ schoolClass: schoolClass._id });
    await this.pathModel.deleteMany({ schoolClass: schoolClass._id });
    await this.classroomModel.updateMany({ classes: schoolClass._id }, { $pull: { classes: schoolClass._id } });
    await schoolClass.deleteOne();
  }

  /** Adds an empty chapter at the end of the class. */
  async addChapter(user: UserType, classId: string, title: string): Promise<ChapterSummaryDto> {
    const schoolClass = await this.findOwned(user, classId);
    const last = await this.chapterModel.findOne({ schoolClass: schoolClass._id }).sort({ number: -1 });
    const chapter = await this.chapterModel.create({
      schoolClass: schoolClass._id,
      number: (last?.number ?? 0) + 1,
      title,
    });
    return this.summaries([chapter])[0];
  }

  /** Renumbers the chapters in the given order. Must list every chapter of the class once. */
  async reorderChapters(user: UserType, classId: string, chapterIds: string[]): Promise<ChapterSummaryDto[]> {
    const schoolClass = await this.findOwned(user, classId);
    const current = await this.chapterModel.find({ schoolClass: schoolClass._id }).distinct('_id');
    const same =
      chapterIds.length === current.length &&
      new Set(chapterIds).size === chapterIds.length &&
      current.every((id) => chapterIds.includes(id.toString()));
    if (!same) {
      throw new BadRequestException("Chapter list doesn't match the class's chapters");
    }
    await this.renumber(chapterIds.map((id) => new mongoose.Types.ObjectId(id)));
    return this.summaries(await this.chapterModel.find({ schoolClass: schoolClass._id }).sort({ number: 1 }));
  }

  /**
   * Gives [orderedIds] the numbers 1, 2, 3… Done in two steps through negative numbers,
   * so the unique (class, number) index never sees two chapters with the same number.
   */
  async renumber(orderedIds: mongoose.Types.ObjectId[]): Promise<void> {
    if (orderedIds.length === 0) return;
    const step = (sign: number) =>
      orderedIds.map((_id, i) => ({ updateOne: { filter: { _id }, update: { $set: { number: sign * (i + 1) } } } }));
    await this.chapterModel.bulkWrite(step(-1));
    await this.chapterModel.bulkWrite(step(1));
  }

  // Students

  /** Every class of the classrooms the student was let into, without duplicates. */
  async listForStudent(user: UserType): Promise<LibraryClassDto[]> {
    const classIds = await this.classroomModel.find({ students: user._id }).distinct('classes');
    const classes = await this.classModel.find({ _id: { $in: classIds } }).sort({ name: 1 });
    const names = await displayNames(this.profileModel, classes.map((c) => c.teacher));
    return classes.map((c) => ({
      id: c._id.toString(),
      name: c.name,
      grade: c.grade ?? null,
      teacherName: names.get(c.teacher.toString()) ?? '',
    }));
  }

  /** The class's chapters by number, with how many of each chapter's cards [user] knows. */
  async listChapters(user: UserType, classId: string): Promise<ChapterSummaryDto[]> {
    const schoolClass = await this.findReadable(user, classId);
    const chapters = await this.chapterModel.find({ schoolClass: schoolClass._id }).sort({ number: 1 });
    // Only cards that still exist count: the teacher may have removed some since.
    const known = await this.progressModel.find({
      user: user._id,
      chapter: { $in: chapters.map((c) => c._id) },
      known: true,
    });
    return this.summaries(chapters, new Set(known.map((p) => p.flashcard.toString())));
  }

  summaries(chapters: ChapterType[], knownCardIds: Set<string> = new Set()): ChapterSummaryDto[] {
    return chapters.map((chapter) => ({
      id: chapter._id.toString(),
      classId: chapter.schoolClass.toString(),
      number: chapter.number,
      title: chapter.title,
      noteCount: chapter.notes.length,
      flashcardCount: chapter.flashcards.length,
      knownFlashcards: chapter.flashcards.filter((card) => knownCardIds.has(card._id.toString())).length,
    }));
  }
}
