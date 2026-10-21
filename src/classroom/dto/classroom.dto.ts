import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty } from 'class-validator';

export class CreateClassroomDto {
  @ApiProperty({ description: 'Classroom name, at least one character' })
  @IsNotEmpty()
  name: string;
}
