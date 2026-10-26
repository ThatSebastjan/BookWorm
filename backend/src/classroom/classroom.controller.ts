import {
  Controller,
  Get,
  Post,
  Body,
  Param,
  Delete,
  Patch,
  Put,
  UseGuards,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { ClassroomService } from './classroom.service.js';
import {
  AddTeacherDto,
  ClassroomClassesDto,
  ClassroomDetailDto,
  ClassroomSummaryDto,
  CreateClassroomDto,
  StudentClassDto,
  UpdateClassroomDto,
} from './dto/classroom.dto.js';
import { CurrentUser } from '@nestjs/authentication';
import * as userSchema from '../auth/schema/user.schema.js';
import { RolesGuard } from '../guards/roles.guards.js';
import { Roles } from '../guards/roles.decorator.js';
import { Role } from '../constants/api.enums.js';
import {
  ApiBearerAuth,
  ApiOkResponse,
  ApiOperation,
  ApiResponse,
} from '@nestjs/swagger';

@UseGuards(RolesGuard)
@Controller('classroom')
export class ClassroomController {
  constructor(private readonly classroomService: ClassroomService) {}

  /* 
    Teacher only routes
  */

  @Post()
  @Roles(Role.TEACHER)
  @ApiBearerAuth()
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Create a new classroom - teacher only route' })
  @ApiResponse({
    status: 200,
    description: 'Response when user has a valid role',
    schema: {
      example: {
        _id: '6ad79efc7e8166600315d05f',
        name: 'classroom#123',
        teacher: '6ad3bb729b14e4b3e7db7ab4',
        students: [],
        code: 'K7M2Q',
        grade: 4,
        approveJoins: false,
        pending: [],
        createdAt: '2026-10-20T12:00:00.00Z',
      },
    },
  })
  @ApiResponse({
    status: 401,
    description: 'Insufficient permissions',
  })
  create(
    @CurrentUser() user: userSchema.UserType,
    @Body() createClassroomDto: CreateClassroomDto,
  ) {
    return this.classroomService.create(createClassroomDto, user);
  }

  @Get('listCreated')
  @Roles(Role.TEACHER)
  @ApiBearerAuth()
  @HttpCode(HttpStatus.OK)
  @ApiOperation({
    summary: 'List the classrooms this user owns or co-teaches - teacher only route',
  })
  @ApiOkResponse({ type: [ClassroomSummaryDto] })
  @ApiResponse({
    status: 403,
    description: 'Not a teacher',
  })
  findAllByTeacher(
    @CurrentUser() user: userSchema.UserType,
  ): Promise<ClassroomSummaryDto[]> {
    return this.classroomService.findAllByTeacher(user);
  }

  @Delete(':id')
  @Roles(Role.TEACHER)
  @ApiOperation({
    summary:
      'Delete a classroom that this user has created - teacher only route',
  })
  @ApiBearerAuth()
  @ApiResponse({
    status: 200,
    description: 'Classroom was deleted successfully',
  })
  @ApiResponse({
    status: 403,
    description: 'Not a teacher, or not the teacher of this classroom',
  })
  @ApiResponse({
    status: 404,
    description: 'Provided classroom id was invalid',
  })
  remove(@CurrentUser() user: userSchema.UserType, @Param('id') id: string) {
    return this.classroomService.delete(user, id);
  }

  // Classes (chapters, notes, flashcards) are managed under /class, learning paths under /learning-path.

  /*
    Routes accessible to students
  */

  @Post('join/:code')
  @ApiOperation({
    summary:
      'Join a classroom with the 5 character code from the teacher (any case; the join link works too)',
  })
  @ApiBearerAuth()
  @HttpCode(HttpStatus.OK)
  @ApiOkResponse({
    type: StudentClassDto,
    description:
      'Joined, or "waiting" when the teacher approves new students first',
  })
  @ApiResponse({
    status: 401,
    description: 'Insufficient permissions',
  })
  @ApiResponse({
    status: 404,
    description: 'Provided classroom code was invalid',
  })
  @ApiResponse({
    status: 409,
    description: 'User is already a member of this classroom',
  })
  joinClassroom(
    @CurrentUser() user: userSchema.UserType,
    @Param('code') code: string,
  ): Promise<StudentClassDto> {
    return this.classroomService.joinClassroom(user, code);
  }

  @Get('list')
  @ApiBearerAuth()
  @HttpCode(HttpStatus.OK)
  @ApiOperation({
    summary:
      'Classes the user is in, then the ones still waiting for the teacher',
  })
  @ApiOkResponse({ type: [StudentClassDto] })
  @ApiResponse({
    status: 401,
    description: 'Insufficient permissions',
  })
  listClassrooms(
    @CurrentUser() user: userSchema.UserType,
  ): Promise<StudentClassDto[]> {
    return this.classroomService.listClassrooms(user);
  }

  /*
    Teacher routes for one classroom. Declared last so 'list' and 'listCreated' aren't read as ids.
  */

  @Get(':id')
  @Roles(Role.TEACHER)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'A classroom with its code, students and classes - teacher only route' })
  @ApiOkResponse({ type: ClassroomDetailDto })
  @ApiResponse({ status: 404, description: 'No such classroom of this teacher' })
  getDetail(
    @CurrentUser() user: userSchema.UserType,
    @Param('id') id: string,
  ): Promise<ClassroomDetailDto> {
    return this.classroomService.getDetail(user, id);
  }

  @Patch(':id')
  @Roles(Role.TEACHER)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Rename a classroom, change its grade, or switch approving new students' })
  @ApiOkResponse({ type: ClassroomDetailDto })
  update(
    @CurrentUser() user: userSchema.UserType,
    @Param('id') id: string,
    @Body() dto: UpdateClassroomDto,
  ): Promise<ClassroomDetailDto> {
    return this.classroomService.update(user, id, dto);
  }

  @Put(':id/classes')
  @Roles(Role.TEACHER)
  @ApiBearerAuth()
  @ApiOperation({ summary: "Set which of the teacher's classes this classroom uses" })
  @ApiOkResponse({ type: ClassroomDetailDto })
  setClasses(
    @CurrentUser() user: userSchema.UserType,
    @Param('id') id: string,
    @Body() dto: ClassroomClassesDto,
  ): Promise<ClassroomDetailDto> {
    return this.classroomService.setClasses(user, id, dto.classIds);
  }

  @Post(':id/students/:studentId/admit')
  @Roles(Role.TEACHER)
  @ApiBearerAuth()
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Let a waiting student in' })
  @ApiOkResponse({ type: ClassroomDetailDto })
  admit(
    @CurrentUser() user: userSchema.UserType,
    @Param('id') id: string,
    @Param('studentId') studentId: string,
  ): Promise<ClassroomDetailDto> {
    return this.classroomService.admit(user, id, studentId);
  }

  @Post(':id/teachers')
  @Roles(Role.TEACHER)
  @ApiBearerAuth()
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Add a co-teacher by their account email - owner only' })
  @ApiOkResponse({ type: ClassroomDetailDto })
  @ApiResponse({ status: 403, description: 'Not the owner' })
  @ApiResponse({ status: 404, description: 'No teacher account with that email' })
  @ApiResponse({ status: 409, description: 'Already teaches this classroom' })
  addTeacher(
    @CurrentUser() user: userSchema.UserType,
    @Param('id') id: string,
    @Body() dto: AddTeacherDto,
  ): Promise<ClassroomDetailDto> {
    return this.classroomService.addTeacher(user, id, dto.email);
  }

  @Delete(':id/teachers/:teacherId')
  @Roles(Role.TEACHER)
  @ApiBearerAuth()
  @ApiOperation({
    summary:
      'Remove a co-teacher (owner), or leave (the co-teacher themselves). Their classes come off the classroom.',
  })
  @ApiOkResponse({ type: ClassroomDetailDto, description: 'The classroom; empty when the user left it' })
  async removeTeacher(
    @CurrentUser() user: userSchema.UserType,
    @Param('id') id: string,
    @Param('teacherId') teacherId: string,
  ): Promise<ClassroomDetailDto | void> {
    return (await this.classroomService.removeTeacher(user, id, teacherId)) ?? undefined;
  }

  @Delete(':id/students/:studentId')
  @Roles(Role.TEACHER)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Remove a student, or turn down one who is waiting' })
  @ApiOkResponse({ type: ClassroomDetailDto })
  removeStudent(
    @CurrentUser() user: userSchema.UserType,
    @Param('id') id: string,
    @Param('studentId') studentId: string,
  ): Promise<ClassroomDetailDto> {
    return this.classroomService.removeStudent(user, id, studentId);
  }
}
