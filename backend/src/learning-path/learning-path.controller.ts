import { Body, Controller, Delete, Get, HttpCode, HttpStatus, Param, Post, Put, UseGuards } from '@nestjs/common';
import { CurrentUser } from '@nestjs/authentication';
import {
  ApiBadRequestResponse,
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
import {
  LearningPathDto,
  LearningPathSummaryDto,
  LessonContentDto,
  PathProgressDto,
  SaveLearningPathDto,
  SaveQuestionsDto,
} from './dto/learning-path.dto.js';
import { LearningPathService } from './learning-path.service.js';

/** Learning paths and their lessons' questions. Teacher only, and only their own. */
@ApiBearerAuth()
@UseGuards(RolesGuard)
@Roles(Role.TEACHER)
@Controller('learning-path')
export class LearningPathController {
  constructor(private readonly pathService: LearningPathService) {}

  @Get()
  @ApiOperation({ summary: "The teacher's learning paths" })
  @ApiOkResponse({ type: [LearningPathSummaryDto] })
  list(@CurrentUser() user: UserType): Promise<LearningPathSummaryDto[]> {
    return this.pathService.list(user);
  }

  @Post()
  @ApiOperation({
    summary:
      'Create a path. Assigned classrooms start using its class and leave the class\'s other paths.',
  })
  @ApiOkResponse({ type: LearningPathDto })
  @ApiBadRequestResponse({ description: "A chapter is twice on the path or not in the path's class" })
  create(@CurrentUser() user: UserType, @Body() dto: SaveLearningPathDto): Promise<LearningPathDto> {
    return this.pathService.create(user, dto);
  }

  @Get('lesson/:lessonId')
  @ApiOperation({ summary: "One lesson's questions, for the lesson editor" })
  @ApiOkResponse({ type: LessonContentDto })
  @ApiNotFoundResponse({ description: "No such lesson on this teacher's paths" })
  getLesson(@CurrentUser() user: UserType, @Param('lessonId') lessonId: string): Promise<LessonContentDto> {
    return this.pathService.getLesson(user, lessonId);
  }

  @Put('lesson/:lessonId/questions')
  @ApiOperation({ summary: "Replace a lesson's questions" })
  @ApiOkResponse({ type: LessonContentDto })
  saveQuestions(
    @CurrentUser() user: UserType,
    @Param('lessonId') lessonId: string,
    @Body() dto: SaveQuestionsDto,
  ): Promise<LessonContentDto> {
    return this.pathService.saveQuestions(user, lessonId, dto.questions);
  }

  @Get(':id')
  @ApiOperation({ summary: 'One path with its chapters and lessons' })
  @ApiOkResponse({ type: LearningPathDto })
  @ApiNotFoundResponse({ description: 'No such path of this teacher' })
  get(@CurrentUser() user: UserType, @Param('id') id: string): Promise<LearningPathDto> {
    return this.pathService.get(user, id);
  }

  @Get(':id/progress')
  @ApiOperation({ summary: "How far each student of the path's classrooms has got" })
  @ApiOkResponse({ type: PathProgressDto })
  @ApiNotFoundResponse({ description: 'No such path of this teacher' })
  progress(@CurrentUser() user: UserType, @Param('id') id: string): Promise<PathProgressDto> {
    return this.pathService.progress(user, id);
  }

  @Put(':id')
  @ApiOperation({ summary: 'Replace a path: name, unlock mode, chapters, lessons and classrooms' })
  @ApiOkResponse({ type: LearningPathDto })
  update(@CurrentUser() user: UserType, @Param('id') id: string, @Body() dto: SaveLearningPathDto): Promise<LearningPathDto> {
    return this.pathService.update(user, id, dto);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiOperation({ summary: 'Delete a path' })
  @ApiNoContentResponse({ description: 'Deleted' })
  async delete(@CurrentUser() user: UserType, @Param('id') id: string): Promise<void> {
    await this.pathService.delete(user, id);
  }
}
