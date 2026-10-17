import { applyDecorators } from '@nestjs/common';
import { IsNotEmpty, IsString, Matches, MaxLength, MinLength } from 'class-validator';

export function PasswordProperty(description = 'Password') {
  return applyDecorators(
    IsString(),
    IsNotEmpty(),
    MinLength(8),
    MaxLength(128),
    Matches(/^(?=.*[A-Za-z])(?=.*\d).+$/, {
      message: 'Password must contain at least one letter and one number',
    }),
  );
}
