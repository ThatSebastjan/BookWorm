import { Module } from '@nestjs/common';
import { ClassroomService } from './classroom.service.js';
import { ClassroomController } from './classroom.controller.js';
import { MongooseModule } from '@nestjs/mongoose';
import { Classroom, ClassroomSchema } from './schema/classroom.schema.js';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: Classroom.name, schema: ClassroomSchema },
    ]),
  ],
  controllers: [ClassroomController],
  providers: [ClassroomService],
})
export class ClassroomModule {}
