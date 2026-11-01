import { ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import mongoose, { isValidObjectId, Model } from 'mongoose';
import { UserType } from '../auth/schema/user.schema.js';
import { Chapter, ChapterType } from '../chapter/schema/chapter.schema.js';
import { FlashcardProgress } from '../chapter/schema/flashcard-progress.schema.js';
import { ClassService } from '../class/class.service.js';
import { SchoolClass } from '../class/schema/class.schema.js';
import { Classroom, ClassroomType } from '../classroom/schema/classroom.schema.js';
import { LearningPath, LearningPathType, PathLesson } from '../learning-path/schema/learning-path.schema.js';
import { displayNames } from '../profile/display-names.js';
import { Profile } from '../profile/schema/profile.schema.js';
import {
  AnswerResultDto,
  BadgeDto,
  ClassProgressDto,
  LearnClassDto,
  LessonStatus,
  LessonSummaryDto,
  StudentLessonDto,
  StudentProfileDto,
  StudentStatsDto,
} from './dto/learn.dto.js';
import { currentChapter, dayKey, lessonStatuses, longestStreak, streak, thisWeek } from './path-progress.js';
import {
  AnswerJson,
  answerKey,
  AuthoredQuestion,
  forStudent,
  isCorrect,
  practiceLanguage,
  questionsFromFlashcards,
} from './questions.js';
import { LessonAttempt, LessonCompletion, PracticeDay, recordPracticeDay } from './schema/lesson-progress.schema.js';

/** Stars per level on the profile. */
const STARS_PER_LEVEL = 100;

/** A path's chapters as students see them: deleted chapters and chapters without lessons left out. */
interface StudentUnit {
  chapter: ChapterType;
  lessons: (PathLesson & { _id: mongoose.Types.ObjectId })[];
}

/**
 * The student side of learning paths: each class's path with lesson statuses, lessons
 * without their answers, checking answers, and the stars and streaks finished lessons add up to.
 * A student follows the path assigned to their classroom for each class.
 */
@Injectable()
export class LearnService {
  constructor(
    @InjectModel(LearningPath.name) private pathModel: Model<LearningPath>,
    @InjectModel(Chapter.name) private chapterModel: Model<Chapter>,
    @InjectModel(Classroom.name) private classroomModel: Model<Classroom>,
    @InjectModel(SchoolClass.name) private classModel: Model<SchoolClass>,
    @InjectModel(FlashcardProgress.name) private flashcardProgressModel: Model<FlashcardProgress>,
    @InjectModel(Profile.name) private profileModel: Model<Profile>,
    @InjectModel(LessonAttempt.name) private attemptModel: Model<LessonAttempt>,
    @InjectModel(LessonCompletion.name) private completionModel: Model<LessonCompletion>,
    @InjectModel(PracticeDay.name) private practiceDayModel: Model<PracticeDay>,
    private readonly classService: ClassService,
  ) {}

  // Learn tab

  async learnClass(user: UserType, classId: string): Promise<LearnClassDto> {
    const schoolClass = await this.classService.findReadable(user, classId);
    const path = await this.pathFor(user, schoolClass._id, await this.classrooms(user));
    if (path == null) {
      return { classId: schoolClass._id.toString(), className: schoolClass.name, path: null };
    }
    const units = await this.studentUnits(path);
    const done = await this.doneLessons(user, units);
    const statuses = lessonStatuses(path.unlockMode, units, done);
    const known = await this.flashcardProgressModel.find({
      user: user._id,
      chapter: { $in: units.map((u) => u.chapter._id) },
      known: true,
    });
    const summaries = this.classService.summaries(
      units.map((u) => u.chapter),
      new Set(known.map((p) => p.flashcard.toString())),
    );
    return {
      classId: schoolClass._id.toString(),
      className: schoolClass.name,
      path: {
        id: path._id.toString(),
        name: path.name,
        unlockMode: path.unlockMode,
        chapters: units.map((unit, i) => ({
          chapter: summaries[i],
          lessons: unit.lessons.map((lesson) => ({
            id: lesson._id.toString(),
            title: lesson.title,
            isChapterQuiz: lesson.isChapterQuiz,
            status: statuses.get(lesson._id.toString())!,
            stars: lesson.stars,
          })),
        })),
      },
    };
  }

  // Lessons

  /**
   * Starts the lesson: the teacher's questions, or practice made from the chapter's
   * flashcards when there are none. Can be empty, which the app shows as "not ready yet".
   */
  async getLesson(user: UserType, lessonId: string, acceptLanguage?: string): Promise<StudentLessonDto> {
    const { path, unit, lesson } = await this.openLesson(user, lessonId);
    const authored = lesson.questions as AuthoredQuestion[];
    const questions =
      authored.length > 0
        ? authored
        : questionsFromFlashcards(
            unit.chapter.flashcards.map((card) => ({ id: card._id.toString(), term: card.term, definition: card.definition })),
            practiceLanguage(acceptLanguage),
          );
    await this.attemptModel.updateOne(
      { user: user._id, lesson: lesson._id },
      { $set: { path: path._id, keys: questions.map(answerKey), correct: [] } },
      { upsert: true },
    );
    const alreadyDone = (await this.completionModel.exists({ user: user._id, lesson: lesson._id })) != null;
    return {
      id: lesson._id.toString(),
      title: lesson.title,
      chapterId: unit.chapter._id.toString(),
      stars: alreadyDone ? 0 : lesson.stars,
      alreadyDone,
      questions: questions.map(forStudent),
    };
  }

  async submitAnswer(user: UserType, lessonId: string, questionId: string, answer: AnswerJson): Promise<AnswerResultDto> {
    const attempt = await this.attempt(user, lessonId);
    const key = attempt.keys.find((k) => k.id === questionId);
    if (key == null) {
      throw new NotFoundException('Question not found in this lesson');
    }
    const correct = isCorrect(key.answer, answer);
    if (correct) {
      // Counted once per attempt, however often it's sent.
      await this.attemptModel.updateOne({ _id: attempt._id, correct: { $ne: questionId } }, { $push: { correct: questionId } });
    }
    return { correct, explanation: correct ? key.explanation : key.retryHint, starsEarned: 0 };
  }

  /**
   * Finishes the attempt. The first time, the lesson earns the stars the teacher set for it,
   * and opens the next one on the path; doing it again earns no more.
   */
  async completeLesson(user: UserType, lessonId: string): Promise<LessonSummaryDto> {
    const attempt = await this.attempt(user, lessonId);
    // Throws if the path changed and the lesson is locked now.
    const { path, lesson } = await this.openLesson(user, lessonId);
    const doneBefore = (await this.completionModel.exists({ user: user._id, lesson: lesson._id })) != null;
    const summary = {
      starsEarned: doneBefore ? 0 : lesson.stars,
      correctAnswers: attempt.correct.length,
      totalQuestions: attempt.keys.length,
    };
    await this.completionModel.create({
      user: user._id,
      path: path._id,
      lesson: lesson._id,
      isChapterQuiz: lesson.isChapterQuiz,
      stars: summary.starsEarned,
      correct: summary.correctAnswers,
      total: summary.totalQuestions,
    });
    await attempt.deleteOne();
    await recordPracticeDay(this.practiceDayModel, user._id, dayKey(new Date()));
    return summary;
  }

  // Stats and profile

  async stats(user: UserType): Promise<StudentStatsDto> {
    const [stars, days] = await Promise.all([this.stars(user), this.practiceDays(user)]);
    return { streakDays: streak(days), stars };
  }

  async profile(user: UserType): Promise<StudentProfileDto> {
    const classrooms = await this.classrooms(user);
    const [names, stars, days, lessonsDone, quizzesDone, { classes, chaptersDone }] = await Promise.all([
      displayNames(this.profileModel, [user._id]),
      this.stars(user),
      this.practiceDays(user),
      this.completionModel.distinct('lesson', { user: user._id }).then((ids) => ids.length),
      this.completionModel.distinct('lesson', { user: user._id, isChapterQuiz: true }).then((ids) => ids.length),
      this.classProgress(user, classrooms),
    ]);
    const today = dayKey(new Date());
    const longest = longestStreak(days);
    const badge = (id: string, kind: string, goal: number, value: number): BadgeDto => ({
      id,
      kind,
      goal,
      progress: Math.min(value, goal),
      earned: value >= goal,
    });
    // Badges are kept once earned: they count the best so far (longest streak, stars ever).
    const badges: BadgeDto[] = [
      badge('first-lesson', 'firstLesson', 1, lessonsDone),
      badge('streak-3', 'streak', 3, longest),
      badge('chapter-quiz', 'chapterQuiz', 1, quizzesDone),
      badge('lessons-10', 'lessons', 10, lessonsDone),
      badge('streak-7', 'streak', 7, longest),
      badge('chapter-done', 'chapterDone', 1, chaptersDone),
      badge('stars-100', 'stars', 100, stars),
      badge('lessons-25', 'lessons', 25, lessonsDone),
      badge('stars-500', 'stars', 500, stars),
    ];
    return {
      name: names.get(user._id.toString()) ?? '',
      grade: classrooms.find((room) => room.grade != null)?.grade ?? null,
      level: Math.floor(stars / STARS_PER_LEVEL) + 1,
      starsToNextLevel: STARS_PER_LEVEL - (stars % STARS_PER_LEVEL),
      levelProgress: (stars % STARS_PER_LEVEL) / STARS_PER_LEVEL,
      streakDays: streak(days),
      stars,
      lessonsDone,
      week: thisWeek().map((day) => ({ practised: days.has(dayKey(day)), isToday: dayKey(day) === today })),
      classes,
      practisedToday: days.has(today),
      badges,
    };
  }

  /** Progress on each class's path, and how many chapters the student finished on them. */
  private async classProgress(
    user: UserType,
    classrooms: ClassroomType[],
  ): Promise<{ classes: ClassProgressDto[]; chaptersDone: number }> {
    const classIds = [...new Map(classrooms.flatMap((room) => room.classes).map((id) => [id.toString(), id])).values()];
    const classes = await this.classModel.find({ _id: { $in: classIds } }).sort({ name: 1 });
    const progress: ClassProgressDto[] = [];
    let chaptersDone = 0;
    for (const schoolClass of classes) {
      const path = await this.pathFor(user, schoolClass._id, classrooms);
      if (path == null) continue;
      const units = await this.studentUnits(path);
      const done = await this.doneLessons(user, units);
      const lessons = units.flatMap((unit) => unit.lessons);
      chaptersDone += units.filter((unit) => unit.lessons.every((l) => done.has(l._id.toString()))).length;
      progress.push({
        classId: schoolClass._id.toString(),
        className: schoolClass.name,
        progress: lessons.length === 0 ? 0 : lessons.filter((l) => done.has(l._id.toString())).length / lessons.length,
        currentChapter: currentChapter(units, done),
        chapterCount: units.length,
      });
    }
    return { classes: progress, chaptersDone };
  }

  // Helpers

  /** Classrooms [user] was let into, oldest first. */
  private classrooms(user: UserType): Promise<ClassroomType[]> {
    return this.classroomModel.find({ students: user._id }).sort({ _id: 1 });
  }

  /** The path the student follows for a class: the one assigned to the first of their classrooms that has one. */
  private async pathFor(
    user: UserType,
    classId: mongoose.Types.ObjectId,
    classrooms: ClassroomType[],
  ): Promise<LearningPathType | null> {
    const rooms = classrooms.filter((room) => room.classes.some((id) => id.equals(classId)));
    const paths = await this.pathModel.find({ schoolClass: classId, classrooms: { $in: rooms.map((r) => r._id) } });
    for (const room of rooms) {
      const path = paths.find((p) => p.classrooms.some((id) => id.equals(room._id)));
      if (path != null) return path;
    }
    return null;
  }

  private async studentUnits(path: LearningPathType): Promise<StudentUnit[]> {
    const chapters = await this.chapterModel.find({ _id: { $in: path.units.map((u) => u.chapter) } });
    const byId = new Map(chapters.map((c) => [c._id.toString(), c]));
    return path.units
      .filter((unit) => byId.has(unit.chapter.toString()) && unit.lessons.length > 0)
      .map((unit) => ({ chapter: byId.get(unit.chapter.toString())!, lessons: unit.lessons }));
  }

  /** Lessons of [units] the student has finished at least once. */
  private async doneLessons(user: UserType, units: StudentUnit[]): Promise<Set<string>> {
    const ids = await this.completionModel.distinct('lesson', {
      user: user._id,
      lesson: { $in: units.flatMap((u) => u.lessons.map((l) => l._id)) },
    });
    return new Set(ids.map((id) => id.toString()));
  }

  /**
   * The lesson, if it is on a path assigned to one of the student's classrooms and isn't
   * locked for them. 404 for anything they can't see, 403 for a lesson they can't do yet.
   */
  private async openLesson(user: UserType, lessonId: string) {
    const path = isValidObjectId(lessonId) ? await this.pathModel.findOne({ 'units.lessons._id': lessonId }) : null;
    const assigned =
      path != null && (await this.classroomModel.exists({ _id: { $in: path.classrooms }, students: user._id })) != null;
    if (path == null || !assigned) {
      throw new NotFoundException('Lesson not found');
    }
    const units = await this.studentUnits(path);
    const unit = units.find((u) => u.lessons.some((l) => l._id.toString() === lessonId));
    if (unit == null) {
      throw new NotFoundException('Lesson not found');
    }
    const lesson = unit.lessons.find((l) => l._id.toString() === lessonId)!;
    const statuses = lessonStatuses(path.unlockMode, units, await this.doneLessons(user, units));
    if (statuses.get(lessonId) === LessonStatus.LOCKED) {
      throw new ForbiddenException('Finish the lessons before this one first');
    }
    return { path, unit, lesson };
  }

  private async attempt(user: UserType, lessonId: string) {
    const attempt = isValidObjectId(lessonId)
      ? await this.attemptModel.findOne({ user: user._id, lesson: lessonId })
      : null;
    if (attempt == null) {
      throw new NotFoundException('Open the lesson first');
    }
    return attempt;
  }

  private async stars(user: UserType): Promise<number> {
    const [total] = await this.completionModel.aggregate<{ stars: number }>([
      { $match: { user: user._id } },
      { $group: { _id: null, stars: { $sum: '$stars' } } },
    ]);
    return total?.stars ?? 0;
  }

  /** Days (YYYY-MM-DD, server time) the student practised: finished a lesson or went through flashcards. */
  private async practiceDays(user: UserType): Promise<Set<string>> {
    const [days, completions] = await Promise.all([
      this.practiceDayModel.find({ user: user._id }, { day: 1 }),
      // Lessons finished before practice days were recorded.
      this.completionModel.find({ user: user._id }, { createdAt: 1 }),
    ]);
    return new Set([...days.map((d) => d.day), ...completions.map((c) => dayKey(c.createdAt))]);
  }
}
