import { Body, Controller, Delete, Get, HttpCode, HttpStatus, Param, Patch, Post, Put, UseGuards } from '@nestjs/common';
import { CurrentUser } from '@nestjs/authentication';
import {
  ApiBearerAuth,
  ApiForbiddenResponse,
  ApiNoContentResponse,
  ApiNotFoundResponse,
  ApiOkResponse,
  ApiOperation,
} from '@nestjs/swagger';
import type { UserType } from '../auth/schema/user.schema.js';
import { Role } from '../constants/api.enums.js';
import { Roles } from '../guards/roles.decorator.js';
import { RolesGuard } from '../guards/roles.guards.js';
import { ChapterOrderDto, ChapterSummaryDto, CreateChapterDto } from '../chapter/dto/chapter.dto.js';
import { ClassService } from './class.service.js';
import { CreateClassDto, LibraryClassDto, TeacherClassDto, UpdateClassDto } from './dto/class.dto.js';

/**
 * Classes: teaching content (chapters with notes and flashcards) that classrooms use.
 * Teachers manage their own; students read the classes of their classrooms.
 */
@ApiBearerAuth()
@UseGuards(RolesGuard)
@Controller('class')
export class ClassController {
  constructor(private readonly classService: ClassService) {}

  @Get()
  @Roles(Role.TEACHER)
  @ApiOperation({ summary: "The teacher's classes with their chapters and the classrooms using them" })
  @ApiOkResponse({ type: [TeacherClassDto] })
  @ApiForbiddenResponse({ description: 'Not a teacher' })
  list(@CurrentUser() user: UserType): Promise<TeacherClassDto[]> {
    return this.classService.listForTeacher(user);
  }

  @Get('mine')
  @ApiOperation({ summary: "A student's library: every class of the classrooms they were let into" })
  @ApiOkResponse({ type: [LibraryClassDto] })
  mine(@CurrentUser() user: UserType): Promise<LibraryClassDto[]> {
    return this.classService.listForStudent(user);
  }

  @Post()
  @Roles(Role.TEACHER)
  @ApiOperation({ summary: 'Create a class (no chapters yet)' })
  @ApiOkResponse({ type: TeacherClassDto })
  create(@CurrentUser() user: UserType, @Body() dto: CreateClassDto): Promise<TeacherClassDto> {
    return this.classService.create(user, dto);
  }

  @Patch(':id')
  @Roles(Role.TEACHER)
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiOperation({ summary: 'Rename a class or change its grade' })
  @ApiNoContentResponse({ description: 'Saved' })
  @ApiNotFoundResponse({ description: 'No such class of this teacher' })
  async update(@CurrentUser() user: UserType, @Param('id') id: string, @Body() dto: UpdateClassDto): Promise<void> {
    await this.classService.update(user, id, dto);
  }

  @Delete(':id')
  @Roles(Role.TEACHER)
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiOperation({ summary: "Delete a class with its chapters, learning paths and students' progress" })
  @ApiNoContentResponse({ description: 'Deleted' })
  @ApiNotFoundResponse({ description: 'No such class of this teacher' })
  async delete(@CurrentUser() user: UserType, @Param('id') id: string): Promise<void> {
    await this.classService.delete(user, id);
  }

  @Get(':id/chapters')
  @ApiOperation({ summary: "A class's chapters by number, with the user's flashcard progress" })
  @ApiOkResponse({ type: [ChapterSummaryDto] })
  @ApiNotFoundResponse({ description: 'No such class, or the user may not read it' })
  listChapters(@CurrentUser() user: UserType, @Param('id') id: string): Promise<ChapterSummaryDto[]> {
    return this.classService.listChapters(user, id);
  }

  @Post(':id/chapters')
  @Roles(Role.TEACHER)
  @ApiOperation({ summary: 'Add an empty chapter at the end of the class' })
  @ApiOkResponse({ type: ChapterSummaryDto })
  @ApiNotFoundResponse({ description: 'No such class of this teacher' })
  addChapter(@CurrentUser() user: UserType, @Param('id') id: string, @Body() dto: CreateChapterDto): Promise<ChapterSummaryDto> {
    return this.classService.addChapter(user, id, dto.title);
  }

  @Put(':id/chapters/order')
  @Roles(Role.TEACHER)
  @ApiOperation({ summary: 'Renumber the chapters in a new order' })
  @ApiOkResponse({ type: [ChapterSummaryDto] })
  reorderChapters(
    @CurrentUser() user: UserType,
    @Param('id') id: string,
    @Body() dto: ChapterOrderDto,
  ): Promise<ChapterSummaryDto[]> {
    return this.classService.reorderChapters(user, id, dto.chapterIds);
  }
}
