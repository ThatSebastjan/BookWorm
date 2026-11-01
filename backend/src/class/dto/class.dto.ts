import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsInt, IsNotEmpty, IsOptional, IsString, Max, MaxLength, Min } from 'class-validator';
import { ChapterSummaryDto } from '../../chapter/dto/chapter.dto.js';

export class CreateClassDto {
  @ApiProperty({ example: 'Maths 4' })
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  name: string;

  @ApiProperty({ description: 'School year, 6 to 9', example: 7 })
  @IsInt()
  @Min(6)
  @Max(9)
  grade: number;
}

export class UpdateClassDto {
  @ApiPropertyOptional({ example: 'Maths 4' })
  @IsOptional()
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  name?: string;

  @ApiPropertyOptional({ example: 7 })
  @IsOptional()
  @IsInt()
  @Min(6)
  @Max(9)
  grade?: number;
}

/** A class as its teacher sees it: chapters and the classrooms using it. */
export class TeacherClassDto {
  @ApiProperty()
  id: string;

  @ApiProperty({ example: 'Maths 4' })
  name: string;

  @ApiProperty({ example: 4, nullable: true })
  grade: number | null;

  @ApiProperty({ type: [ChapterSummaryDto] })
  chapters: ChapterSummaryDto[];

  @ApiProperty({ type: [String] })
  classroomIds: string[];
}

/** A class in a student's library: one of the classes their classrooms use. */
export class LibraryClassDto {
  @ApiProperty()
  id: string;

  @ApiProperty({ example: 'Maths 4' })
  name: string;

  @ApiProperty({ example: 4, nullable: true })
  grade: number | null;

  @ApiProperty({ example: 'Ana Kovač' })
  teacherName: string;
}
