import { ApiProperty } from '@nestjs/swagger';
import { Transform } from 'class-transformer';
import { IsNotEmpty, IsString, MaxLength } from 'class-validator';
import { PasswordProperty } from '../../auth/dto/password.decorator.js';

const trim = ({ value }: { value: unknown }) => (typeof value === 'string' ? value.trim() : value);

export class UpdateProfileDto {
  @ApiProperty({ example: 'Ana' })
  @Transform(trim)
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  name: string;

  @ApiProperty({ example: 'Kovač' })
  @Transform(trim)
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  surname: string;
}

export class ChangePasswordDto {
  @ApiProperty()
  @IsString()
  @IsNotEmpty()
  currentPassword: string;

  @ApiProperty({ description: 'At least 8 characters, with at least one letter and one number' })
  @PasswordProperty()
  newPassword: string;
}
