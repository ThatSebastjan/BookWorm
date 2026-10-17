import { Controller, Get } from '@nestjs/common';
import { UserService } from './user.service.js';
import { CurrentUser } from '@nestjs/authentication';
import type { UserType } from '../auth/schema/user.schema.js';

@Controller('user')
export class UserController {
  constructor(private readonly userService: UserService) {}

  @Get()
  me(@CurrentUser() user: UserType) {
    return user;
  }
}
