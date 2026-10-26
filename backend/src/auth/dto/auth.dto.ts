import {
  IsEmail,
  IsNotEmpty,
  IsString,
  MaxLength,
} from 'class-validator';
import { Transform } from 'class-transformer';
import { PasswordProperty } from './password.decorator.js';
import { ApiProperty } from '@nestjs/swagger';

const trim = ({ value }: { value: unknown }) =>
  typeof value === 'string' ? value.trim() : value;
const normaliseEmail = ({ value }: { value: unknown }) =>
  typeof value === 'string' ? value.trim().toLowerCase() : value;

export class RegisterUserDto {
  @ApiProperty({ example: 'Mia' })
  @Transform(trim)
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  name: string;

  @ApiProperty({ example: 'Novak' })
  @Transform(trim)
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  surname: string;

  @ApiProperty({ example: 'mia@example.com' })
  @Transform(normaliseEmail)
  @IsEmail()
  email: string;

  @ApiProperty({
    description: 'At least 8 characters, with at least one letter and one number',
  })
  @PasswordProperty()
  password: string;

  // No role here: the route decides it (`/auth/register` for students,
  // `/auth/register/teacher` for the teacher app).
}

export class LoginUserDto {
  @ApiProperty({ example: 'mia@example.com' })
  @Transform(normaliseEmail)
  @IsEmail()
  email: string;

  // No strength rules here: a wrong password should get 401, not a validation error.
  @ApiProperty()
  @IsString()
  @IsNotEmpty()
  password: string;
}

export class RefreshTokenDto {
  @ApiProperty({ description: 'Refresh token returned from /login route' })
  @IsNotEmpty()
  refreshToken: string;
}
