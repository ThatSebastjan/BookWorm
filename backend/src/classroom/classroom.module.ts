import { Module } from '@nestjs/common';
import { ClassroomService } from './classroom.service.js';
import { ClassroomController } from './classroom.controller.js';
import { MongooseModule } from '@nestjs/mongoose';
import { Classroom, ClassroomSchema } from './schema/classroom.schema.js';
import { Profile, ProfileSchema } from '../profile/schema/profile.schema.js';
import { User, UserSchema } from '../auth/schema/user.schema.js';
import { Chapter, ChapterSchema } from '../chapter/schema/chapter.schema.js';
import {
  FlashcardProgress,
  FlashcardProgressSchema,
} from '../chapter/schema/flashcard-progress.schema.js';
import { ChapterController } from '../chapter/chapter.controller.js';
import { ChapterService } from '../chapter/chapter.service.js';
import { SchoolClass, SchoolClassSchema } from '../class/schema/class.schema.js';
import { ClassController } from '../class/class.controller.js';
import { ClassService } from '../class/class.service.js';
import { LearningPath, LearningPathSchema } from '../learning-path/schema/learning-path.schema.js';
import { LearningPathController } from '../learning-path/learning-path.controller.js';
import { LearningPathService } from '../learning-path/learning-path.service.js';
import { LearnController } from '../learn/learn.controller.js';
import { LearnService } from '../learn/learn.service.js';
import {
  LessonAttempt,
  LessonAttemptSchema,
  LessonCompletion,
  LessonCompletionSchema,
  PracticeDay,
  PracticeDaySchema,
} from '../learn/schema/lesson-progress.schema.js';

/**
 * Classrooms (students), classes (content: chapters with notes and flashcards),
 * learning paths and students' progress on them. They reference each other too much to live in separate modules.
 */
@Module({
  imports: [
    MongooseModule.forFeature([
      { name: Classroom.name, schema: ClassroomSchema },
      { name: SchoolClass.name, schema: SchoolClassSchema },
      { name: Profile.name, schema: ProfileSchema },
      { name: User.name, schema: UserSchema },
      { name: Chapter.name, schema: ChapterSchema },
      { name: FlashcardProgress.name, schema: FlashcardProgressSchema },
      { name: LearningPath.name, schema: LearningPathSchema },
      { name: LessonAttempt.name, schema: LessonAttemptSchema },
      { name: LessonCompletion.name, schema: LessonCompletionSchema },
      { name: PracticeDay.name, schema: PracticeDaySchema },
    ]),
  ],
  controllers: [ClassroomController, ClassController, ChapterController, LearningPathController, LearnController],
  providers: [ClassroomService, ClassService, ChapterService, LearningPathService, LearnService],
})
export class ClassroomModule {}
