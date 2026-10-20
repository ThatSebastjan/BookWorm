import { Injectable, NotFoundException, UnauthorizedException } from '@nestjs/common';
import { CreateClassroomDto } from './dto/classroom.dto.js';
import { Classroom, ClassroomType } from './schema/classroom.schema.js';
import { Model } from 'mongoose';
import { InjectModel } from '@nestjs/mongoose';
import * as crypto from "node:crypto";
import { UserType } from '../auth/schema/user.schema.js';


@Injectable()
export class ClassroomService {

  constructor(
    @InjectModel(Classroom.name) private classroomModel: Model<Classroom>
  ) {};


  async create(createClassroomDto: CreateClassroomDto, user: UserType) {
    
    // Generate a unique classroom code first
    let code;

    while(true){
      code = crypto.randomBytes(4).toString('hex');

      const existing = await this.classroomModel.exists({ code: code });

      if(existing == null){
        break;
      }
    };

    const newClassroom = new this.classroomModel({
      name: createClassroomDto.name,
      teacher: user._id.toString(),
      code: code
    });

    await newClassroom.save();
  }


  async findAllByTeacher(user: UserType): Promise<ClassroomType[]> {
    return await this.classroomModel.find({ teacher: user._id.toString() });
  }


  async delete(id: string) {
    const classroom = await this.classroomModel.findById(id);

    if(classroom == null){
      throw new NotFoundException("Classroom with this id does not exist");
    };

    await this.classroomModel.findByIdAndDelete(id);
  }
}
