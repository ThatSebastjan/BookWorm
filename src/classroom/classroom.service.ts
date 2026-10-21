import {
  ConflictException,
  Injectable,
  NotFoundException,
  UnauthorizedException,
} from '@nestjs/common';
import { CreateClassroomDto } from './dto/classroom.dto.js';
import { Classroom, ClassroomType } from './schema/classroom.schema.js';
import { Model } from 'mongoose';
import { InjectModel } from '@nestjs/mongoose';
import * as crypto from 'node:crypto';
import { UserType } from '../auth/schema/user.schema.js';

@Injectable()
export class ClassroomService {
  constructor(
    @InjectModel(Classroom.name) private classroomModel: Model<Classroom>,
  ) {}

  async create(createClassroomDto: CreateClassroomDto, user: UserType) {
    // Generate a unique classroom code first
    let code;

    while (true) {
      code = crypto.randomBytes(2).toString('hex'); // 2 bytes -> 4 digits

      const existing = await this.classroomModel.exists({ code: code });

      if (existing == null) {
        break;
      }
    }

    const newClassroom = new this.classroomModel({
      name: createClassroomDto.name,
      teacher: user._id.toString(),
      code: code,
    });

    await newClassroom.save();
    return newClassroom;
  }

  async findAllByTeacher(user: UserType): Promise<ClassroomType[]> {
    return await this.classroomModel.find({ teacher: user._id.toString() });
  }

  async delete(user: UserType, id: string) {
    const classroom = await this.classroomModel.findById(id);

    if (classroom == null) {
      throw new NotFoundException('Classroom with this id does not exist');
    }

    if (classroom.teacher != user._id) {
      throw new UnauthorizedException('Can only delete owned classrooms');
    }

    await this.classroomModel.findByIdAndDelete(id);
  }

  async joinClassroom(user: UserType, code: string) {
    const classroom = await this.classroomModel.findOne({ code: code });

    if (classroom == null) {
      throw new NotFoundException('Invalid classroom code');
    }

    if (classroom.students.includes(user._id)) {
      throw new ConflictException('User is already a member of this classroom');
    }

    classroom.students.push(user._id);
    await classroom.save();
  }

  async listClassrooms(user: UserType) {
    const list = await this.classroomModel.find({ students: user._id });
    return list;
  }

  // TODO: joining classrooms and listing classrooms a student is in
  // Learning materials and stuff
}
