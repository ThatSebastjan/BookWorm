import { IsNotEmpty, IsEnum } from 'class-validator';
import { PasswordProperty } from './password.decorator.js';
import { Role } from '../../constants/api.enums.js';
import { ApiProperty } from '@nestjs/swagger';

export class RegisterUserDto {
  @ApiProperty({
    description: 'Username for this user, must be at least one character',
  })
  @IsNotEmpty()
  username: string;

  @ApiProperty()
  @PasswordProperty()
  password: string;

  @IsEnum(Role)
  role: Role;
}

export class LoginUserDto {
  @ApiProperty()
  @IsNotEmpty()
  username: string;

  @ApiProperty()
  @PasswordProperty()
  password: string;
}

export class RefreshTokenDto {
  @ApiProperty({ description: 'Refresh token returned from /login route' })
  @IsNotEmpty()
  refreshToken: string;
}
