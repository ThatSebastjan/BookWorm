import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import mongoose, { isValidObjectId, Model } from 'mongoose';
import { UserType } from '../auth/schema/user.schema.js';
import { Chapter } from '../chapter/schema/chapter.schema.js';
import { ClassService } from '../class/class.service.js';
import { SchoolClass } from '../class/schema/class.schema.js';
import { Classroom, teachingFilter } from '../classroom/schema/classroom.schema.js';
import { currentChapter } from '../learn/path-progress.js';
import { LessonCompletion } from '../learn/schema/lesson-progress.schema.js';
import { displayNames } from '../profile/display-names.js';
import { Profile } from '../profile/schema/profile.schema.js';
import {
  LearningPathDto,
  LearningPathSummaryDto,
  LessonContentDto,
  PathProgressDto,
  SaveLearningPathDto,
} from './dto/learning-path.dto.js';
import { LearningPath, LearningPathType } from './schema/learning-path.schema.js';

/** Learning paths through a class's chapters. Only the class's teacher sees and edits them. */
@Injectable()
export class LearningPathService {
  constructor(
    @InjectModel(LearningPath.name) private pathModel: Model<LearningPath>,
    @InjectModel(Chapter.name) private chapterModel: Model<Chapter>,
    @InjectModel(Classroom.name) private classroomModel: Model<Classroom>,
    @InjectModel(SchoolClass.name) private classModel: Model<SchoolClass>,
    @InjectModel(LessonCompletion.name) private completionModel: Model<LessonCompletion>,
    @InjectModel(Profile.name) private profileModel: Model<Profile>,
    private readonly classService: ClassService,
  ) {}

  async list(user: UserType): Promise<LearningPathSummaryDto[]> {
    const paths = await this.pathModel.find({ teacher: user._id }).sort({ name: 1 });
    const grades = await this.grades(paths);
    return paths.map((path) => ({
      id: path._id.toString(),
      name: path.name,
      classId: path.schoolClass.toString(),
      grade: grades.get(path.schoolClass.toString()) ?? null,
      chapterCount: path.units.length,
      lessonCount: path.units.reduce((sum, unit) => sum + unit.lessons.length, 0),
      classroomIds: path.classrooms.map((c) => c.toString()),
    }));
  }

  async get(user: UserType, id: string): Promise<LearningPathDto> {
    return this.toDto(await this.findOwned(user, id));
  }

  async create(user: UserType, dto: SaveLearningPathDto): Promise<LearningPathDto> {
    const path = new this.pathModel({ teacher: user._id });
    return this.save(user, path, dto);
  }

  async update(user: UserType, id: string, dto: SaveLearningPathDto): Promise<LearningPathDto> {
    return this.save(user, await this.findOwned(user, id), dto);
  }

  async delete(user: UserType, id: string): Promise<void> {
    await (await this.findOwned(user, id)).deleteOne();
  }

  /** Each assigned classroom's students with how far they've got, counted over the lessons on the path now. */
  async progress(user: UserType, id: string): Promise<PathProgressDto> {
    const path = await this.findOwned(user, id);
    const chapters = await this.chapterModel.find({ _id: { $in: path.units.map((u) => u.chapter) } }).distinct('_id');
    // The same units students see: deleted chapters and chapters without lessons left out.
    const units = path.units.filter((u) => u.lessons.length > 0 && chapters.some((c) => c.equals(u.chapter)));
    const lessonIds = units.flatMap((u) => u.lessons.map((l) => l._id));
    const classrooms = await this.classroomModel.find({ _id: { $in: path.classrooms } }).sort({ name: 1 });
    const studentIds = classrooms.flatMap((room) => room.students);
    const [completions, names] = await Promise.all([
      this.completionModel.find({ user: { $in: studentIds }, lesson: { $in: lessonIds } }),
      displayNames(this.profileModel, studentIds),
    ]);
    const student = (studentId: mongoose.Types.ObjectId) => {
      const own = completions.filter((c) => c.user.equals(studentId));
      const done = new Set(own.map((c) => c.lesson.toString()));
      const last = own.reduce<Date | null>((latest, c) => (latest == null || c.createdAt > latest ? c.createdAt : latest), null);
      return {
        id: studentId.toString(),
        name: names.get(studentId.toString()) ?? '',
        lessonsDone: done.size,
        currentChapter: currentChapter(units, done),
        stars: own.reduce((sum, c) => sum + c.stars, 0),
        lastActive: last,
      };
    };
    return {
      lessonCount: lessonIds.length,
      chapterCount: units.length,
      classrooms: classrooms.map((room) => ({
        classroomId: room._id.toString(),
        classroomName: room.name,
        students: room.students.map(student).sort((a, b) => a.name.localeCompare(b.name)),
      })),
    };
  }

  async getLesson(user: UserType, lessonId: string): Promise<LessonContentDto> {
    const { path, unitIndex, lessonIndex } = await this.findLesson(user, lessonId);
    return this.lessonDto(path, unitIndex, lessonIndex);
  }

  async saveQuestions(user: UserType, lessonId: string, questions: object[]): Promise<LessonContentDto> {
    const { path, unitIndex, lessonIndex } = await this.findLesson(user, lessonId);
    path.units[unitIndex].lessons[lessonIndex].questions = questions;
    path.markModified('units');
    await path.save();
    return this.lessonDto(path, unitIndex, lessonIndex);
  }

  /**
   * Writes [dto] into [path]. Chapters must belong to the class; lessons sent with their id
   * keep their questions. Assigned classrooms start using the class (if they didn't), and
   * leave the class's other paths, since a classroom follows one path per class.
   */
  private async save(user: UserType, path: LearningPathType, dto: SaveLearningPathDto): Promise<LearningPathDto> {
    const schoolClass = await this.classService.findOwned(user, dto.classId);

    const chapterIds = dto.units.map((unit) => unit.chapterId);
    if (new Set(chapterIds).size !== chapterIds.length) {
      throw new BadRequestException('A chapter can only be on the path once');
    }
    const ownChapters = await this.chapterModel.countDocuments({ _id: { $in: chapterIds }, schoolClass: schoolClass._id });
    if (ownChapters !== chapterIds.length) {
      throw new BadRequestException("Every chapter must belong to the path's class");
    }

    const classroomIds = [...new Set(dto.classroomIds)];
    const classrooms = await this.classroomModel
      .find({ _id: { $in: classroomIds }, ...teachingFilter(user._id) })
      .distinct('_id');
    if (classrooms.length !== classroomIds.length) {
      throw new NotFoundException('Classroom not found');
    }

    // Lessons already on the path, by id: the ones that stay keep their questions (and stars, unless sent).
    const keptById = new Map<string, { questions: unknown[]; stars: number }>();
    path.units.forEach((unit) =>
      unit.lessons.forEach((lesson) => keptById.set(lesson._id.toString(), { questions: lesson.questions, stars: lesson.stars })),
    );

    path.name = dto.name;
    path.schoolClass = schoolClass._id;
    path.unlockMode = dto.unlockMode;
    path.classrooms = classrooms;
    path.set(
      'units',
      dto.units.map((unit) => ({
        chapter: new mongoose.Types.ObjectId(unit.chapterId),
        lessons: unit.lessons.map((lesson) => {
          const kept = lesson.id != null ? keptById.get(lesson.id) : undefined;
          return {
            _id: kept != null ? new mongoose.Types.ObjectId(lesson.id) : new mongoose.Types.ObjectId(),
            title: lesson.title,
            isChapterQuiz: lesson.isChapterQuiz,
            stars: lesson.stars ?? kept?.stars ?? (lesson.isChapterQuiz ? 20 : 10),
            questions: kept?.questions ?? [],
          };
        }),
      })),
    );
    await path.save();

    if (classrooms.length > 0) {
      await this.classroomModel.updateMany({ _id: { $in: classrooms } }, { $addToSet: { classes: schoolClass._id } });
      await this.pathModel.updateMany(
        { schoolClass: schoolClass._id, _id: { $ne: path._id } },
        { $pull: { classrooms: { $in: classrooms } } },
      );
    }
    return this.toDto(path);
  }

  private async findOwned(user: UserType, id: string): Promise<LearningPathType> {
    const path = isValidObjectId(id) ? await this.pathModel.findById(id) : null;
    if (path == null || !path.teacher.equals(user._id)) {
      throw new NotFoundException('Learning path not found');
    }
    return path;
  }

  private async findLesson(user: UserType, lessonId: string) {
    const path = isValidObjectId(lessonId)
      ? await this.pathModel.findOne({ 'units.lessons._id': lessonId, teacher: user._id })
      : null;
    if (path == null) {
      throw new NotFoundException('Lesson not found');
    }
    const unitIndex = path.units.findIndex((unit) => unit.lessons.some((l) => l._id.toString() === lessonId));
    const lessonIndex = path.units[unitIndex].lessons.findIndex((l) => l._id.toString() === lessonId);
    return { path, unitIndex, lessonIndex };
  }

  private async lessonDto(path: LearningPathType, unitIndex: number, lessonIndex: number): Promise<LessonContentDto> {
    const unit = path.units[unitIndex];
    const lesson = unit.lessons[lessonIndex];
    const [chapter, schoolClass] = await Promise.all([
      this.chapterModel.findById(unit.chapter),
      this.classModel.findById(path.schoolClass),
    ]);
    if (chapter == null || schoolClass == null) {
      throw new NotFoundException('Lesson not found');
    }
    return {
      lessonId: lesson._id.toString(),
      title: lesson.title,
      isChapterQuiz: lesson.isChapterQuiz,
      className: schoolClass.name,
      chapter: this.classService.summaries([chapter])[0],
      questions: lesson.questions,
    };
  }

  private async toDto(path: LearningPathType): Promise<LearningPathDto> {
    const chapters = await this.chapterModel.find({ _id: { $in: path.units.map((u) => u.chapter) } });
    const summaries = new Map(this.classService.summaries(chapters).map((s) => [s.id, s]));
    const grades = await this.grades([path]);
    return {
      id: path._id.toString(),
      name: path.name,
      classId: path.schoolClass.toString(),
      grade: grades.get(path.schoolClass.toString()) ?? null,
      unlockMode: path.unlockMode,
      // A chapter deleted in the meantime simply drops off.
      units: path.units
        .filter((unit) => summaries.has(unit.chapter.toString()))
        .map((unit) => ({
          chapter: summaries.get(unit.chapter.toString())!,
          lessons: unit.lessons.map((lesson) => ({
            id: lesson._id.toString(),
            title: lesson.title,
            isChapterQuiz: lesson.isChapterQuiz,
            questionCount: lesson.questions.length,
            stars: lesson.stars,
          })),
        })),
      classroomIds: path.classrooms.map((c) => c.toString()),
    };
  }

  /** The grade of each path's class, by class id. */
  private async grades(paths: LearningPathType[]): Promise<Map<string, number | null>> {
    const classes = await this.classModel.find({ _id: { $in: paths.map((p) => p.schoolClass) } });
    return new Map(classes.map((c) => [c._id.toString(), c.grade ?? null]));
  }
}
