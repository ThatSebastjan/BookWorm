import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsObject, IsString } from 'class-validator';
import { ChapterSummaryDto } from '../../chapter/dto/chapter.dto.js';
import { UnlockMode } from '../../learning-path/schema/learning-path.schema.js';

export enum LessonStatus {
  DONE = 'done',
  /** Open to do now. */
  CURRENT = 'current',
  LOCKED = 'locked',
}

export class LearnLessonDto {
  @ApiProperty()
  id: string;

  @ApiProperty()
  title: string;

  @ApiProperty()
  isChapterQuiz: boolean;

  @ApiProperty({ enum: LessonStatus })
  status: LessonStatus;

  @ApiProperty({ description: 'Stars for finishing it the first time' })
  stars: number;
}

export class LearnChapterDto {
  @ApiProperty({ type: ChapterSummaryDto })
  chapter: ChapterSummaryDto;

  @ApiProperty({ type: [LearnLessonDto] })
  lessons: LearnLessonDto[];
}

export class StudentPathDto {
  @ApiProperty()
  id: string;

  @ApiProperty()
  name: string;

  @ApiProperty({ enum: UnlockMode })
  unlockMode: UnlockMode;

  @ApiProperty({ type: [LearnChapterDto], description: 'Chapters with lessons, in path order' })
  chapters: LearnChapterDto[];
}

/** One class on the Learn tab: the path the student's classroom follows, if any. */
export class LearnClassDto {
  @ApiProperty()
  classId: string;

  @ApiProperty()
  className: string;

  @ApiProperty({ type: StudentPathDto, nullable: true, description: 'Null until the teacher assigns a path' })
  path: StudentPathDto | null;
}

/** A lesson as the student gets it: questions without their answers. */
export class StudentLessonDto {
  @ApiProperty()
  id: string;

  @ApiProperty()
  title: string;

  @ApiProperty()
  chapterId: string;

  @ApiProperty({ description: 'Stars for finishing it; 0 when the student already earned them' })
  stars: number;

  @ApiProperty({ description: 'The student finished it before, so it earns no stars again' })
  alreadyDone: boolean;

  @ApiProperty({ type: [Object], description: "Questions in the app's JSON shape, without answer keys" })
  questions: unknown[];
}

export class SubmitAnswerDto {
  @ApiProperty()
  @IsString()
  @IsNotEmpty()
  questionId: string;

  @ApiProperty({ description: 'One of choice, choices, text, ordered or pairs', example: { choice: 'b' } })
  @IsObject()
  answer: object;
}

export class AnswerResultDto {
  @ApiProperty()
  correct: boolean;

  @ApiProperty({ description: 'The explanation when right, a hint to try again when wrong' })
  explanation: string;

  @ApiProperty({ description: 'Always 0: stars come with finishing the lesson' })
  starsEarned: number;
}

export class LessonSummaryDto {
  @ApiProperty()
  starsEarned: number;

  @ApiProperty()
  correctAnswers: number;

  @ApiProperty()
  totalQuestions: number;
}

export class StudentStatsDto {
  @ApiProperty({ description: 'Days in a row, up to today or yesterday, with a finished lesson' })
  streakDays: number;

  @ApiProperty()
  stars: number;
}

export class DayActivityDto {
  @ApiProperty()
  practised: boolean;

  @ApiProperty()
  isToday: boolean;
}

export class ClassProgressDto {
  @ApiProperty()
  classId: string;

  @ApiProperty()
  className: string;

  @ApiProperty({ description: 'Share of the path\'s lessons done, 0 to 1' })
  progress: number;

  @ApiProperty({ description: 'Position on the path of the first chapter with lessons to do' })
  currentChapter: number;

  @ApiProperty()
  chapterCount: number;
}

export class BadgeDto {
  @ApiProperty({ example: 'streak-7' })
  id: string;

  @ApiProperty({ enum: ['firstLesson', 'lessons', 'streak', 'chapterQuiz', 'chapterDone', 'stars'], description: 'What it is for; the app picks name and icon from it' })
  kind: string;

  @ApiProperty({ description: 'How many it takes, e.g. 7 for a 7-day streak' })
  goal: number;

  @ApiProperty({ description: 'How far the student has got, at most goal' })
  progress: number;

  @ApiProperty()
  earned: boolean;
}

export class StudentProfileDto {
  @ApiProperty()
  name: string;

  @ApiProperty({ nullable: true })
  grade: number | null;

  @ApiProperty()
  level: number;

  @ApiProperty()
  starsToNextLevel: number;

  @ApiProperty({ description: '0 to 1' })
  levelProgress: number;

  @ApiProperty()
  streakDays: number;

  @ApiProperty()
  stars: number;

  @ApiProperty({ description: 'Different lessons finished' })
  lessonsDone: number;

  @ApiProperty({ type: [DayActivityDto], description: 'This week, Monday first' })
  week: DayActivityDto[];

  @ApiProperty({ type: [ClassProgressDto], description: 'Classes with a learning path' })
  classes: ClassProgressDto[];

  @ApiProperty({ description: 'Finished a lesson or practised flashcards today' })
  practisedToday: boolean;

  @ApiProperty({ type: [BadgeDto] })
  badges: BadgeDto[];
}
