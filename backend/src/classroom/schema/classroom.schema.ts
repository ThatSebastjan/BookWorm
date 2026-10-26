import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import mongoose, { HydratedDocument } from 'mongoose';

export type ClassroomType = HydratedDocument<Classroom>;

/**
 * A group of students, e.g. "4B", that students join with [code]. Its owner ([teacher])
 * can invite co-teachers ([teachers]). It uses classes (`SchoolClass`) of any of its
 * teachers, whose chapters its students can read; each teacher adds only their own.
 */
@Schema()
export class Classroom {
  @Prop({ required: true, trim: true })
  name: string;

  /** School year the class is in, 1 to 9. */
  @Prop({ min: 1, max: 9 })
  grade?: number;

  /** The owner: renames and deletes the classroom and decides who teaches it. */
  @Prop({ type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true })
  teacher: mongoose.Types.ObjectId; // NOTE: this is only an object id reference, must be populated manually

  /** Co-teachers. They manage students and their own classes here, like the owner. */
  @Prop({ type: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }], default: [] })
  teachers: mongoose.Types.ObjectId[];

  @Prop({ default: Date.now })
  createdAt: Date;

  @Prop({ type: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }], default: [] })
  students: mongoose.Types.ObjectId[]; // NOTE: only object id, must be populated manually

  /** Students who used the code while [approveJoins] is on, waiting for the teacher to let them in. */
  @Prop({ type: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }], default: [] })
  pending: mongoose.Types.ObjectId[];

  /** The classes (teaching content) this classroom uses. Only the teacher's own classes. */
  @Prop({ type: [{ type: mongoose.Schema.Types.ObjectId, ref: 'SchoolClass' }], default: [] })
  classes: mongoose.Types.ObjectId[];

  /** When on, joining with the code puts the student in [pending] instead of [students]. */
  @Prop({ default: false })
  approveJoins: boolean;

  /** Five characters, upper case, without look-alikes (0/O, 1/I). See `generateJoinCode`. */
  @Prop({ required: true, unique: true, uppercase: true, trim: true })
  code: string;
}

export const ClassroomSchema = SchemaFactory.createForClass(Classroom);
ClassroomSchema.index({ teachers: 1 });

/** Query for the classrooms [userId] teaches, as owner or co-teacher. */
export function teachingFilter(userId: mongoose.Types.ObjectId) {
  return { $or: [{ teacher: userId }, { teachers: userId }] };
}
