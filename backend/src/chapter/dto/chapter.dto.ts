import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import {
  IsArray,
  IsBoolean,
  IsMongoId,
  IsNotEmpty,
  IsOptional,
  IsString,
  MaxLength,
  ValidateNested,
} from 'class-validator';

/** A chapter in a class's library list, with the signed-in student's progress. */
export class ChapterSummaryDto {
  @ApiProperty({ example: '6ad79efc7e8166600315d060' })
  id: string;

  @ApiProperty({ example: '6ad79efc7e8166600315d05f' })
  classId: string;

  @ApiProperty({ example: 3 })
  number: number;

  @ApiProperty({ example: 'Fractions' })
  title: string;

  @ApiProperty({ example: 2 })
  noteCount: number;

  @ApiProperty({ example: 4 })
  flashcardCount: number;

  @ApiProperty({ description: 'Flashcards the student knew the last time they practised them', example: 1 })
  knownFlashcards: number;
}

export class NoteSectionDto {
  @ApiProperty({ example: 'What is a fraction?' })
  heading: string;

  @ApiProperty()
  body: string;

  @ApiPropertyOptional({ description: 'Highlighted "Remember" box', nullable: true })
  remember: string | null;
}

export class FlashcardDto {
  @ApiProperty({ example: '6ad79efc7e8166600315d061' })
  id: string;

  @ApiProperty({ example: 'Numerator' })
  term: string;

  @ApiProperty({ example: 'The top number. It tells how many parts we have.' })
  definition: string;

  @ApiProperty({ description: 'The signed-in student knew it the last time they practised it' })
  known: boolean;
}

/** Everything the chapter screen shows: notes and flashcards. */
export class ChapterDto {
  @ApiProperty()
  id: string;

  @ApiProperty()
  classId: string;

  @ApiProperty({ example: 'Maths 4' })
  className: string;

  @ApiProperty({ example: 'Ana Kovač' })
  teacherName: string;

  @ApiProperty({ example: 3 })
  number: number;

  @ApiProperty({ example: 'Fractions' })
  title: string;

  @ApiProperty({ type: [NoteSectionDto] })
  notes: NoteSectionDto[];

  @ApiProperty({ type: [FlashcardDto] })
  flashcards: FlashcardDto[];
}

export class FlashcardProgressDto {
  @ApiProperty({ description: 'Whether the student knew the card this time' })
  @IsBoolean()
  known: boolean;
}

export class CreateChapterDto {
  @ApiProperty({ example: 'Fractions' })
  @IsString()
  @IsNotEmpty()
  @MaxLength(200)
  title: string;
}

export class ChapterOrderDto {
  @ApiProperty({ description: "All of the class's chapter ids, in their new order", type: [String] })
  @IsArray()
  @IsMongoId({ each: true })
  chapterIds: string[];
}

export class NoteSectionInput {
  @ApiProperty()
  @IsString()
  @IsNotEmpty()
  heading: string;

  @ApiProperty()
  @IsString()
  @IsNotEmpty()
  body: string;

  @ApiPropertyOptional({ nullable: true })
  @IsOptional()
  @IsString()
  remember?: string | null;
}

export class FlashcardInput {
  @ApiPropertyOptional({ description: 'Leave out for a new card. Kept cards keep students\' progress.' })
  @IsOptional()
  @IsMongoId()
  id?: string;

  @ApiProperty()
  @IsString()
  @IsNotEmpty()
  term: string;

  @ApiProperty()
  @IsString()
  @IsNotEmpty()
  definition: string;
}

/** Replaces the chapter's title, notes and flashcards. Cards left out are deleted. */
export class UpdateChapterDto {
  @ApiProperty()
  @IsString()
  @IsNotEmpty()
  @MaxLength(200)
  title: string;

  @ApiProperty({ type: [NoteSectionInput] })
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => NoteSectionInput)
  notes: NoteSectionInput[];

  @ApiProperty({ type: [FlashcardInput] })
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => FlashcardInput)
  flashcards: FlashcardInput[];
}
