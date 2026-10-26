import { Body, Controller, Get, HttpCode, HttpStatus, Patch, Post } from '@nestjs/common';
import { UserService } from './user.service.js';
import { CurrentUser } from '@nestjs/authentication';
import type { UserType } from '../auth/schema/user.schema.js';
import {
  ApiBearerAuth,
  ApiForbiddenResponse,
  ApiNoContentResponse,
  ApiOkResponse,
  ApiOperation,
} from '@nestjs/swagger';
import { ChangePasswordDto, UpdateProfileDto } from './dto/profile.dto.js';
import { MeDto } from './dto/me.dto.js';

@ApiBearerAuth()
@Controller('user')
export class UserController {
  constructor(private readonly userService: UserService) {}

  @Get()
  @ApiOperation({ summary: 'The signed-in user with their name and surname' })
  @ApiOkResponse({ type: MeDto })
  me(@CurrentUser() user: UserType): Promise<MeDto> {
    return this.userService.getMe(user);
  }

  @Patch()
  @ApiOperation({ summary: "Change the signed-in user's name and surname" })
  @ApiOkResponse({ type: MeDto })
  updateProfile(@CurrentUser() user: UserType, @Body() dto: UpdateProfileDto): Promise<MeDto> {
    return this.userService.updateProfile(user, dto);
  }

  @Post('password')
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiOperation({ summary: 'Change the password; the current one is needed' })
  @ApiNoContentResponse({ description: 'Changed' })
  @ApiForbiddenResponse({ description: 'Current password is wrong' })
  async changePassword(@CurrentUser() user: UserType, @Body() dto: ChangePasswordDto): Promise<void> {
    await this.userService.changePassword(user, dto.currentPassword, dto.newPassword);
  }
}
