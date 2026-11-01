import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Transform } from 'class-transformer';
import {
  IsArray,
  IsBoolean,
  IsEmail,
  IsInt,
  IsMongoId,
  IsNotEmpty,
  IsOptional,
  IsString,
  Max,
  MaxLength,
  Min,
} from 'class-validator';

export class CreateClassroomDto {
  @ApiProperty({ description: 'Classroom name, at least one character', example: '4B' })
  @IsNotEmpty()
  name: string;

  @ApiProperty({ description: 'School year, 6 to 9', example: 7 })
  @IsInt()
  @Min(6)
  @Max(9)
  grade: number;

  @ApiPropertyOptional({
    description: 'Students who join with the code wait for the teacher to let them in',
    default: false,
  })
  @IsOptional()
  @IsBoolean()
  approveJoins?: boolean;
}

export enum MembershipStatus {
  JOINED = 'joined',
  /** Joined with the code, but the class approves new students and the teacher hasn't yet. */
  WAITING = 'waiting',
}

/** A class as a student sees it. Never includes the join code or the other students. */
export class StudentClassDto {
  @ApiProperty({ example: '6ad79efc7e8166600315d05f' })
  id: string;

  @ApiProperty({ example: 'Maths 4B' })
  name: string;

  @ApiProperty({ example: 4, nullable: true })
  grade: number | null;

  @ApiProperty({ example: 'Ana Kovač' })
  teacherName: string;

  @ApiProperty({ enum: MembershipStatus })
  status: MembershipStatus;
}

export class UpdateClassroomDto {
  @ApiPropertyOptional({ example: '4B' })
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

  @ApiPropertyOptional()
  @IsOptional()
  @IsBoolean()
  approveJoins?: boolean;
}

export class ClassroomClassesDto {
  @ApiProperty({ description: 'Every class this classroom uses (the teacher\'s own)', type: [String] })
  @IsArray()
  @IsMongoId({ each: true })
  classIds: string[];
}

/** A classroom in its teacher's list. */
export class ClassroomSummaryDto {
  @ApiProperty()
  id: string;

  @ApiProperty({ example: '4B' })
  name: string;

  @ApiProperty({ example: 4, nullable: true })
  grade: number | null;

  @ApiProperty({ example: 24 })
  studentCount: number;

  @ApiProperty({ description: 'Students waiting to be let in', example: 1 })
  waitingCount: number;

  @ApiProperty({ description: 'The user owns it (otherwise they co-teach it)' })
  isOwner: boolean;
}

export class ClassroomTeacherDto {
  @ApiProperty()
  id: string;

  @ApiProperty({ example: 'Ana Kovač' })
  name: string;

  @ApiProperty({ example: 'ana@school.si' })
  email: string;

  @ApiProperty()
  isOwner: boolean;
}

/** A class another teacher of the classroom added. Read-only for everyone else. */
export class SharedClassDto {
  @ApiProperty()
  id: string;

  @ApiProperty({ example: 'Nature 4' })
  name: string;

  @ApiProperty({ example: 'Marko Novak' })
  teacherName: string;
}

export class AddTeacherDto {
  @ApiProperty({ description: "The other teacher's account email", example: 'marko@school.si' })
  @Transform(({ value }: { value: unknown }) => (typeof value === 'string' ? value.trim().toLowerCase() : value))
  @IsEmail()
  email: string;
}

export class PersonDto {
  @ApiProperty()
  id: string;

  @ApiProperty({ example: 'Mia Novak' })
  name: string;
}

/** Everything the teacher's classroom screen shows. */
export class ClassroomDetailDto {
  @ApiProperty()
  id: string;

  @ApiProperty({ example: '4B' })
  name: string;

  @ApiProperty({ example: 4, nullable: true })
  grade: number | null;

  @ApiProperty({ example: 'K7M2Q' })
  code: string;

  @ApiProperty()
  approveJoins: boolean;

  @ApiProperty({ type: [PersonDto] })
  students: PersonDto[];

  @ApiProperty({ type: [PersonDto], description: 'Joined with the code, waiting for the teacher' })
  waiting: PersonDto[];

  @ApiProperty({ type: [String], description: "The user's own classes this classroom uses" })
  classIds: string[];

  @ApiProperty({ type: [SharedClassDto], description: "Other teachers' classes this classroom uses" })
  otherClasses: SharedClassDto[];

  @ApiProperty({ type: [ClassroomTeacherDto], description: 'Owner first, then co-teachers' })
  teachers: ClassroomTeacherDto[];

  @ApiProperty({ description: 'The user owns it (otherwise they co-teach it)' })
  isOwner: boolean;
}
