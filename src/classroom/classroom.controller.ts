import { Controller, Get, Post, Body, Patch, Param, Delete, UseGuards } from '@nestjs/common';
import { ClassroomService } from './classroom.service.js';
import { CreateClassroomDto } from './dto/classroom.dto.js';
import { CurrentUser } from '@nestjs/authentication';
import * as userSchema from '../auth/schema/user.schema.js';
import { RolesGuard } from '../guards/roles.guards.js';
import { Roles } from '../guards/roles.decorator.js';
import { Role } from '../constants/api.enums.js';


@UseGuards(RolesGuard)
@Controller('classroom')
export class ClassroomController {
  constructor(private readonly classroomService: ClassroomService) {}


  @Post()
  @Roles(Role.TEACHER)
  create(
    @CurrentUser() user: userSchema.UserType,
    @Body() createClassroomDto: CreateClassroomDto
  ) {
    return this.classroomService.create(createClassroomDto, user);
  }


  @Get('mine')
  @Roles(Role.TEACHER)
  findAllByTeacher(
    @CurrentUser() user: userSchema.UserType
  ) {
    return this.classroomService.findAllByTeacher(user);
  }


  @Delete(':id')
  @Roles(Role.TEACHER)
  remove(@Param('id') id: string) {
    return this.classroomService.delete(id);
  }
}
