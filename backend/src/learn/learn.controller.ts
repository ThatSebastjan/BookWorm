import { Body, Controller, Get, Headers, HttpCode, HttpStatus, Param, Post, UseGuards } from '@nestjs/common';
import { CurrentUser } from '@nestjs/authentication';
import {
  ApiBearerAuth,
  ApiForbiddenResponse,
  ApiNotFoundResponse,
  ApiOkResponse,
  ApiOperation,
} from '@nestjs/swagger';
import type { UserType } from '../auth/schema/user.schema.js';
import { Role } from '../constants/api.enums.js';
import { Roles } from '../guards/roles.decorator.js';
import { RolesGuard } from '../guards/roles.guards.js';
import {
  AnswerResultDto,
  LearnClassDto,
  LessonSummaryDto,
  StudentLessonDto,
  StudentProfileDto,
  StudentStatsDto,
  SubmitAnswerDto,
} from './dto/learn.dto.js';
import { LearnService } from './learn.service.js';

/**
 * The student app's Learn tab and lessons: the learning path of each class, doing its
 * lessons, and the stars and streaks that come with them. Student only.
 */
@ApiBearerAuth()
@UseGuards(RolesGuard)
@Roles(Role.STUDENT)
@Controller('learn')
export class LearnController {
  constructor(private readonly learnService: LearnService) {}

  @Get('stats')
  @ApiOperation({ summary: "The student's day streak and stars" })
  @ApiOkResponse({ type: StudentStatsDto })
  stats(@CurrentUser() user: UserType): Promise<StudentStatsDto> {
    return this.learnService.stats(user);
  }

  @Get('profile')
  @ApiOperation({ summary: "The student's profile: level, stars, this week, progress per class and badges" })
  @ApiOkResponse({ type: StudentProfileDto })
  profile(@CurrentUser() user: UserType): Promise<StudentProfileDto> {
    return this.learnService.profile(user);
  }

  @Get('class/:classId')
  @ApiOperation({ summary: 'The learning path the student follows for a class, with the status of each lesson' })
  @ApiOkResponse({ type: LearnClassDto })
  @ApiNotFoundResponse({ description: 'No such class, or none of the student\'s classrooms uses it' })
  learnClass(@CurrentUser() user: UserType, @Param('classId') classId: string): Promise<LearnClassDto> {
    return this.learnService.learnClass(user, classId);
  }

  @Get('lesson/:lessonId')
  @ApiOperation({
    summary:
      'Start a lesson: its questions without answers. Practice from the chapter\'s flashcards when the teacher wrote none, ' +
      'in the language of Accept-Language (English or Slovenian).',
  })
  @ApiOkResponse({ type: StudentLessonDto })
  @ApiNotFoundResponse({ description: "No such lesson on the student's paths" })
  @ApiForbiddenResponse({ description: 'The lesson is still locked' })
  getLesson(
    @CurrentUser() user: UserType,
    @Param('lessonId') lessonId: string,
    @Headers('accept-language') acceptLanguage?: string,
  ): Promise<StudentLessonDto> {
    return this.learnService.getLesson(user, lessonId, acceptLanguage);
  }

  @Post('lesson/:lessonId/answer')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Check an answer to one question of the started lesson' })
  @ApiOkResponse({ type: AnswerResultDto })
  @ApiNotFoundResponse({ description: 'The lesson was not started, or has no such question' })
  submitAnswer(
    @CurrentUser() user: UserType,
    @Param('lessonId') lessonId: string,
    @Body() dto: SubmitAnswerDto,
  ): Promise<AnswerResultDto> {
    return this.learnService.submitAnswer(user, lessonId, dto.questionId, dto.answer);
  }

  @Post('lesson/:lessonId/complete')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Finish the started lesson; the first time opens the next lesson' })
  @ApiOkResponse({ type: LessonSummaryDto })
  @ApiNotFoundResponse({ description: 'The lesson was not started' })
  completeLesson(@CurrentUser() user: UserType, @Param('lessonId') lessonId: string): Promise<LessonSummaryDto> {
    return this.learnService.completeLesson(user, lessonId);
  }
}
