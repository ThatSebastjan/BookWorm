import {
  Controller,
  Get,
  Post,
  Body,
  Param,
  Delete,
  UseGuards,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { ClassroomService } from './classroom.service.js';
import { CreateClassroomDto } from './dto/classroom.dto.js';
import { CurrentUser } from '@nestjs/authentication';
import * as userSchema from '../auth/schema/user.schema.js';
import { RolesGuard } from '../guards/roles.guards.js';
import { Roles } from '../guards/roles.decorator.js';
import { Role } from '../constants/api.enums.js';
import { ApiBearerAuth, ApiOperation, ApiResponse } from '@nestjs/swagger';

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
        code: '37ba',
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
    summary: 'List all classrooms this user has created - teacher only route',
  })
  @ApiResponse({
    status: 200,
    description: 'Response when user has a valid role',
    schema: {
      example: [
        {
          _id: '6ad79efc7e8166600315d05f',
          name: 'classroom#123',
          teacher: '6ad3bb729b14e4b3e7db7ab4',
          students: [],
          code: '37ba',
          createdAt: '2026-10-20T12:00:00.00Z',
        },
      ],
    },
  })
  @ApiResponse({
    status: 401,
    description: 'Insufficient permissions',
  })
  findAllByTeacher(@CurrentUser() user: userSchema.UserType) {
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
    status: 401,
    description: 'Insufficient permissions',
  })
  @ApiResponse({
    status: 404,
    description: 'Provided classroom id was invalid',
  })
  remove(@CurrentUser() user: userSchema.UserType, @Param('id') id: string) {
    return this.classroomService.delete(user, id);
  }

  // TODO: routes for creating notes and flashcards and quizes and stuff?
  // NEED STRUCTURE FOR ALL OF THOSE FIRST!

  /*
    Routes accessible to students
  */

  @Post('join/:code')
  @ApiOperation({ summary: 'Join a classroom using a 4 digit code' })
  @ApiBearerAuth()
  @HttpCode(HttpStatus.OK)
  @ApiResponse({
    status: 200,
    description: 'User joined the classroom successfully',
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
  ) {
    return this.classroomService.joinClassroom(user, code);
  }

  @Get('list')
  @ApiBearerAuth()
  @HttpCode(HttpStatus.OK)
  @ApiResponse({
    status: 200,
    description: 'List of classrooms current user is a member of',
    schema: {
      example: [
        {
          _id: '6ad79efc7e8166600315d05f',
          name: 'classroom#123',
          teacher: '6ad3bb729b14e4b3e7db7ab4',
          code: '37ba',
          createdAt: '2026-10-20T12:00:00.00Z',
        },
      ],
    },
  })
  @ApiResponse({
    status: 401,
    description: 'Insufficient permissions',
  })
  listClassrooms(@CurrentUser() user: userSchema.UserType) {
    return this.classroomService.listClassrooms(user);
  }

  // TODO: routes for listing study materials and quizes of a classroom
}
