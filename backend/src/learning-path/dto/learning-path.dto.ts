import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import {
  IsArray,
  IsBoolean,
  IsEnum,
  IsInt,
  IsMongoId,
  IsNotEmpty,
  IsObject,
  IsOptional,
  IsString,
  Max,
  MaxLength,
  Min,
  ValidateNested,
} from 'class-validator';
import { ChapterSummaryDto } from '../../chapter/dto/chapter.dto.js';
import { UnlockMode } from '../schema/learning-path.schema.js';

export class PathLessonInput {
  @ApiPropertyOptional({ description: 'Leave out for a new lesson. Kept lessons keep their questions.' })
  @IsOptional()
  @IsMongoId()
  id?: string;

  @ApiProperty({ example: 'Halves' })
  @IsString()
  @IsNotEmpty()
  @MaxLength(200)
  title: string;

  @ApiProperty()
  @IsBoolean()
  isChapterQuiz: boolean;

  @ApiPropertyOptional({ description: 'Stars for finishing it the first time, 0 to 100. Left out: kept, or 10 for a new lesson (20 for a chapter quiz).' })
  @IsOptional()
  @IsInt()
  @Min(0)
  @Max(100)
  stars?: number;
}

export class PathUnitInput {
  @ApiProperty({ description: 'A chapter of the path\'s class' })
  @IsMongoId()
  chapterId: string;

  @ApiProperty({ type: [PathLessonInput] })
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => PathLessonInput)
  lessons: PathLessonInput[];
}

/**
 * Creates or replaces a path. Assigning it to a classroom also adds the class to that
 * classroom, and takes the classroom off the class's other paths.
 */
export class SaveLearningPathDto {
  @ApiProperty({ example: 'Maths 4 – main path' })
  @IsString()
  @IsNotEmpty()
  @MaxLength(200)
  name: string;

  @ApiProperty({ description: 'The class whose chapters the path goes through' })
  @IsMongoId()
  classId: string;

  @ApiProperty({ enum: UnlockMode })
  @IsEnum(UnlockMode)
  unlockMode: UnlockMode;

  @ApiProperty({ type: [PathUnitInput] })
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => PathUnitInput)
  units: PathUnitInput[];

  @ApiProperty({ type: [String], description: "The teacher's classrooms that follow this path" })
  @IsArray()
  @IsMongoId({ each: true })
  classroomIds: string[];
}

export class PathLessonDto {
  @ApiProperty()
  id: string;

  @ApiProperty()
  title: string;

  @ApiProperty()
  isChapterQuiz: boolean;

  @ApiProperty({ description: 'Questions the teacher wrote; 0 means practice from flashcards' })
  questionCount: number;

  @ApiProperty({ description: 'Stars for finishing it the first time' })
  stars: number;
}

export class PathUnitDto {
  @ApiProperty({ type: ChapterSummaryDto })
  chapter: ChapterSummaryDto;

  @ApiProperty({ type: [PathLessonDto] })
  lessons: PathLessonDto[];
}

export class LearningPathSummaryDto {
  @ApiProperty()
  id: string;

  @ApiProperty()
  name: string;

  @ApiProperty()
  classId: string;

  @ApiProperty({ nullable: true })
  grade: number | null;

  @ApiProperty()
  chapterCount: number;

  @ApiProperty()
  lessonCount: number;

  @ApiProperty({ type: [String] })
  classroomIds: string[];
}

export class LearningPathDto {
  @ApiProperty()
  id: string;

  @ApiProperty()
  name: string;

  @ApiProperty()
  classId: string;

  @ApiProperty({ nullable: true })
  grade: number | null;

  @ApiProperty({ enum: UnlockMode })
  unlockMode: UnlockMode;

  @ApiProperty({ type: [PathUnitDto] })
  units: PathUnitDto[];

  @ApiProperty({ type: [String] })
  classroomIds: string[];
}

/** One lesson's questions, for the lesson editor. */
export class LessonContentDto {
  @ApiProperty()
  lessonId: string;

  @ApiProperty()
  title: string;

  @ApiProperty()
  isChapterQuiz: boolean;

  @ApiProperty({ example: 'Maths 4' })
  className: string;

  @ApiProperty({ type: ChapterSummaryDto })
  chapter: ChapterSummaryDto;

  @ApiProperty({ description: "Questions in the app's JSON shape, with answer keys", type: [Object] })
  questions: unknown[];
}

export class SaveQuestionsDto {
  @ApiProperty({ description: 'Replaces all of the lesson\'s questions; empty goes back to flashcard practice', type: [Object] })
  @IsArray()
  @IsObject({ each: true })
  questions: object[];
}

export class StudentPathProgressDto {
  @ApiProperty()
  id: string;

  @ApiProperty({ example: 'Ana Novak' })
  name: string;

  @ApiProperty({ description: 'Different lessons of the path finished' })
  lessonsDone: number;

  @ApiProperty({ description: '1-based position on the path of the first chapter with lessons to do' })
  currentChapter: number;

  @ApiProperty({ description: 'Stars earned on this path' })
  stars: number;

  @ApiProperty({ nullable: true, description: 'When they last finished a lesson of the path' })
  lastActive: Date | null;
}

export class ClassroomPathProgressDto {
  @ApiProperty()
  classroomId: string;

  @ApiProperty({ example: '4B' })
  classroomName: string;

  @ApiProperty({ type: [StudentPathProgressDto], description: 'By name' })
  students: StudentPathProgressDto[];
}

/** How the students of each assigned classroom are getting on with a path. */
export class PathProgressDto {
  @ApiProperty()
  lessonCount: number;

  @ApiProperty()
  chapterCount: number;

  @ApiProperty({ type: [ClassroomPathProgressDto] })
  classrooms: ClassroomPathProgressDto[];
}
