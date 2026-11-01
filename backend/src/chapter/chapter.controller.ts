import { Body, Controller, Delete, Get, HttpCode, HttpStatus, Param, Post, Put, UseGuards } from '@nestjs/common';
import { CurrentUser } from '@nestjs/authentication';
import {
  ApiBearerAuth,
  ApiNoContentResponse,
  ApiNotFoundResponse,
  ApiOkResponse,
  ApiOperation,
} from '@nestjs/swagger';
import type { UserType } from '../auth/schema/user.schema.js';
import { Role } from '../constants/api.enums.js';
import { Roles } from '../guards/roles.decorator.js';
import { RolesGuard } from '../guards/roles.guards.js';
import { ChapterService } from './chapter.service.js';
import { ChapterDto, FlashcardProgressDto, UpdateChapterDto } from './dto/chapter.dto.js';

/**
 * One chapter of a class: students read it and record flashcard progress, the class's
 * teacher edits and deletes it. A class's chapter list is at `GET /class/{id}/chapters`.
 */
@ApiBearerAuth()
@UseGuards(RolesGuard)
@Controller()
export class ChapterController {
  constructor(private readonly chapterService: ChapterService) {}

  @Get('chapter/:id')
  @ApiOperation({ summary: 'One chapter with its notes and flashcards' })
  @ApiOkResponse({ type: ChapterDto })
  @ApiNotFoundResponse({ description: 'No such chapter, or the user may not read its class' })
  getChapter(@CurrentUser() user: UserType, @Param('id') id: string): Promise<ChapterDto> {
    return this.chapterService.getChapter(user, id);
  }

  @Put('chapter/:id')
  @Roles(Role.TEACHER)
  @ApiOperation({
    summary: "Replace a chapter's title, notes and flashcards. Cards sent with their id keep students' progress.",
  })
  @ApiOkResponse({ type: ChapterDto })
  @ApiNotFoundResponse({ description: "No such chapter in this teacher's classes" })
  updateChapter(@CurrentUser() user: UserType, @Param('id') id: string, @Body() dto: UpdateChapterDto): Promise<ChapterDto> {
    return this.chapterService.updateChapter(user, id, dto);
  }

  @Delete('chapter/:id')
  @Roles(Role.TEACHER)
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiOperation({ summary: 'Delete a chapter; the chapters after it move up a number' })
  @ApiNoContentResponse({ description: 'Deleted' })
  @ApiNotFoundResponse({ description: "No such chapter in this teacher's classes" })
  async deleteChapter(@CurrentUser() user: UserType, @Param('id') id: string): Promise<void> {
    await this.chapterService.deleteChapter(user, id);
  }

  @Delete('chapter/:id/progress')
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiOperation({ summary: "Forget which of the chapter's flashcards the student knows, to start over" })
  @ApiNoContentResponse({ description: 'Progress cleared' })
  @ApiNotFoundResponse({ description: 'No such chapter, or the user may not read its class' })
  async resetProgress(@CurrentUser() user: UserType, @Param('id') id: string): Promise<void> {
    await this.chapterService.resetProgress(user, id);
  }

  @Post('flashcard/:id/progress')
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiOperation({ summary: 'Record whether the student knew a flashcard' })
  @ApiNoContentResponse({ description: 'Saved' })
  @ApiNotFoundResponse({ description: 'No such flashcard, or the user may not read its class' })
  async recordFlashcard(
    @CurrentUser() user: UserType,
    @Param('id') id: string,
    @Body() body: FlashcardProgressDto,
  ): Promise<void> {
    await this.chapterService.recordFlashcard(user, id, body.known);
  }
}
