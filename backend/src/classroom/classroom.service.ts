import {
  ConflictException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import {
  ClassroomDetailDto,
  ClassroomSummaryDto,
  CreateClassroomDto,
  MembershipStatus,
  StudentClassDto,
  UpdateClassroomDto,
} from './dto/classroom.dto.js';
import { Classroom, ClassroomType, teachingFilter } from './schema/classroom.schema.js';
import mongoose, { isValidObjectId, Model } from 'mongoose';
import { InjectModel } from '@nestjs/mongoose';
import * as crypto from 'node:crypto';
import { User, UserType } from '../auth/schema/user.schema.js';
import { Role } from '../constants/api.enums.js';
import { Profile } from '../profile/schema/profile.schema.js';
import { SchoolClass } from '../class/schema/class.schema.js';
import { LearningPath } from '../learning-path/schema/learning-path.schema.js';
import { displayNames } from '../profile/display-names.js';

/** No 0/O or 1/I, so a code read off the board can't be mistyped. */
const CODE_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
const CODE_LENGTH = 5;

export function generateJoinCode(): string {
  return Array.from(
    { length: CODE_LENGTH },
    () => CODE_ALPHABET[crypto.randomInt(CODE_ALPHABET.length)],
  ).join('');
}

/** What students type: also accepts the join link (…/join/K7M2Q), any case and spaces. */
export function normalizeJoinCode(input: string): string {
  return input
    .trim()
    .split('/join/')
    .pop()!
    .split('?')[0]
    .replace(/\s/g, '')
    .toUpperCase();
}

@Injectable()
export class ClassroomService {
  constructor(
    @InjectModel(Classroom.name) private classroomModel: Model<Classroom>,
    @InjectModel(User.name) private userModel: Model<User>,
    @InjectModel(Profile.name) private profileModel: Model<Profile>,
    @InjectModel(SchoolClass.name) private classModel: Model<SchoolClass>,
    @InjectModel(LearningPath.name) private pathModel: Model<LearningPath>,
  ) {}

  async create(createClassroomDto: CreateClassroomDto, user: UserType) {
    // Generate a unique classroom code first
    let code: string;

    while (true) {
      code = generateJoinCode();

      const existing = await this.classroomModel.exists({ code: code });

      if (existing == null) {
        break;
      }
    }

    const newClassroom = new this.classroomModel({
      name: createClassroomDto.name,
      grade: createClassroomDto.grade,
      approveJoins: createClassroomDto.approveJoins ?? false,
      teacher: user._id,
      code: code,
    });

    await newClassroom.save();
    return newClassroom;
  }

  /** Every classroom the user teaches, as owner or co-teacher. */
  async findAllByTeacher(user: UserType): Promise<ClassroomSummaryDto[]> {
    const classrooms = await this.classroomModel.find(teachingFilter(user._id)).sort({ name: 1 });
    return classrooms.map((c) => ({
      id: c._id.toString(),
      name: c.name,
      grade: c.grade ?? null,
      studentCount: c.students.length,
      waitingCount: c.pending.length,
      isOwner: c.teacher.equals(user._id),
    }));
  }

  async getDetail(user: UserType, id: string): Promise<ClassroomDetailDto> {
    const classroom = await this.findTeaching(user, id);
    const teacherIds = [classroom.teacher, ...classroom.teachers];
    const [names, teachers, classes] = await Promise.all([
      displayNames(this.profileModel, [...classroom.students, ...classroom.pending, ...teacherIds]),
      this.userModel.find({ _id: { $in: teacherIds } }, { email: 1 }),
      this.classModel.find({ _id: { $in: classroom.classes } }).sort({ name: 1 }),
    ]);
    const nameOf = (userId: mongoose.Types.ObjectId) => names.get(userId.toString()) ?? '';
    const person = (userId: mongoose.Types.ObjectId) => ({ id: userId.toString(), name: nameOf(userId) });
    const byName = (a: { name: string }, b: { name: string }) => a.name.localeCompare(b.name);
    return {
      id: classroom._id.toString(),
      name: classroom.name,
      grade: classroom.grade ?? null,
      code: classroom.code,
      approveJoins: classroom.approveJoins,
      students: classroom.students.map(person).sort(byName),
      waiting: classroom.pending.map(person).sort(byName),
      classIds: classes.filter((c) => c.teacher.equals(user._id)).map((c) => c._id.toString()),
      otherClasses: classes
        .filter((c) => !c.teacher.equals(user._id))
        .map((c) => ({ id: c._id.toString(), name: c.name, teacherName: nameOf(c.teacher) })),
      teachers: teacherIds.map((teacherId) => ({
        id: teacherId.toString(),
        name: nameOf(teacherId),
        email: teachers.find((t) => t._id.equals(teacherId))?.email ?? '',
        isOwner: teacherId.equals(classroom.teacher),
      })),
      isOwner: classroom.teacher.equals(user._id),
    };
  }

  /** Any teacher may switch approving new students; only the owner renames or regrades. */
  async update(user: UserType, id: string, dto: UpdateClassroomDto): Promise<ClassroomDetailDto> {
    const classroom = await this.findTeaching(user, id);
    if ((dto.name !== undefined || dto.grade !== undefined) && !classroom.teacher.equals(user._id)) {
      throw new ForbiddenException('Only the owner can rename the classroom');
    }
    if (dto.name !== undefined) classroom.name = dto.name;
    if (dto.grade !== undefined) classroom.grade = dto.grade;
    if (dto.approveJoins !== undefined) classroom.approveJoins = dto.approveJoins;
    await classroom.save();
    return this.getDetail(user, id);
  }

  /**
   * Sets which of the user's own classes the classroom uses; other teachers' classes stay.
   * A class taken off also takes the classroom off that class's learning paths.
   */
  async setClasses(user: UserType, id: string, classIds: string[]): Promise<ClassroomDetailDto> {
    const classroom = await this.findTeaching(user, id);
    const chosen = await this.classModel.find({ _id: { $in: classIds }, teacher: user._id }).distinct('_id');
    if (chosen.length !== new Set(classIds).size) {
      throw new NotFoundException('Class not found');
    }
    const mine = await this.classModel.find({ _id: { $in: classroom.classes }, teacher: user._id }).distinct('_id');
    const removed = mine.filter((c) => !chosen.some((o) => o.equals(c)));
    const others = classroom.classes.filter((c) => !mine.some((m) => m.equals(c)));
    classroom.classes = [...others, ...chosen];
    await classroom.save();
    if (removed.length > 0) {
      await this.pathModel.updateMany(
        { schoolClass: { $in: removed } },
        { $pull: { classrooms: classroom._id } },
      );
    }
    return this.getDetail(user, id);
  }

  /** Lets a waiting student in. */
  async admit(user: UserType, id: string, studentId: string): Promise<ClassroomDetailDto> {
    const classroom = await this.findTeaching(user, id);
    if (!classroom.pending.some((p) => p.toString() === studentId)) {
      throw new NotFoundException('Student is not waiting for this classroom');
    }
    await this.classroomModel.updateOne(
      { _id: classroom._id },
      { $pull: { pending: classroom.pending.find((p) => p.toString() === studentId) }, $addToSet: { students: new mongoose.Types.ObjectId(studentId) } },
    );
    return this.getDetail(user, id);
  }

  /** Removes a student from the classroom, or turns down one who is waiting. */
  async removeStudent(user: UserType, id: string, studentId: string): Promise<ClassroomDetailDto> {
    const classroom = await this.findTeaching(user, id);
    if (!isValidObjectId(studentId)) {
      throw new NotFoundException('Student not found');
    }
    const student = new mongoose.Types.ObjectId(studentId);
    await this.classroomModel.updateOne({ _id: classroom._id }, { $pull: { students: student, pending: student } });
    return this.getDetail(user, id);
  }

  /** The owner adds another teacher by the email of their (teacher) account. */
  async addTeacher(user: UserType, id: string, email: string): Promise<ClassroomDetailDto> {
    const classroom = await this.findOwned(user, id);
    const teacher = await this.userModel.findOne({ email, role: Role.TEACHER });
    if (teacher == null) {
      throw new NotFoundException('No teacher account with that email');
    }
    if (classroom.teacher.equals(teacher._id) || classroom.teachers.some((t) => t.equals(teacher._id))) {
      throw new ConflictException('Already teaches this classroom');
    }
    await this.classroomModel.updateOne({ _id: classroom._id }, { $addToSet: { teachers: teacher._id } });
    return this.getDetail(user, id);
  }

  /**
   * The owner removes a co-teacher, or a co-teacher leaves. Their classes come off the
   * classroom and the classroom comes off their learning paths. Returns null when the
   * user just left (they can't see it any more).
   */
  async removeTeacher(user: UserType, id: string, teacherId: string): Promise<ClassroomDetailDto | null> {
    const classroom = await this.findTeaching(user, id);
    const leaving = classroom.teachers.find((t) => t.toString() === teacherId);
    if (leaving == null) {
      throw new NotFoundException('Not a co-teacher of this classroom');
    }
    const isOwner = classroom.teacher.equals(user._id);
    if (!isOwner && !leaving.equals(user._id)) {
      throw new ForbiddenException('Only the owner can remove other teachers');
    }
    const theirClasses = await this.classModel.find({ _id: { $in: classroom.classes }, teacher: leaving }).distinct('_id');
    await this.classroomModel.updateOne(
      { _id: classroom._id },
      { $pull: { teachers: leaving, classes: { $in: theirClasses } } },
    );
    await this.pathModel.updateMany({ teacher: leaving, classrooms: classroom._id }, { $pull: { classrooms: classroom._id } });
    return isOwner ? this.getDetail(user, id) : null;
  }

  /** The classroom, if [user] owns it. Anyone else gets 404, co-teachers 403. */
  private async findOwned(user: UserType, id: string): Promise<ClassroomType> {
    const classroom = await this.findTeaching(user, id);
    if (!classroom.teacher.equals(user._id)) {
      throw new ForbiddenException('Only the owner can do this');
    }
    return classroom;
  }

  /** The classroom, if [user] owns or co-teaches it. Anyone else gets 404. */
  private async findTeaching(user: UserType, id: string): Promise<ClassroomType> {
    const classroom = isValidObjectId(id) ? await this.classroomModel.findById(id) : null;
    const teaches = classroom != null &&
      (classroom.teacher.equals(user._id) || classroom.teachers.some((t) => t.equals(user._id)));
    if (!teaches) {
      throw new NotFoundException('Classroom not found');
    }
    return classroom;
  }

  /** The owner deletes the classroom; it comes off everyone's learning paths. The classes it used stay. */
  async delete(user: UserType, id: string) {
    const classroom = isValidObjectId(id)
      ? await this.classroomModel.findById(id)
      : null;

    if (classroom == null) {
      throw new NotFoundException('Classroom with this id does not exist');
    }

    if (!classroom.teacher.equals(user._id)) {
      throw new ForbiddenException('Can only delete owned classrooms');
    }

    await this.pathModel.updateMany({ classrooms: classroom._id }, { $pull: { classrooms: classroom._id } });
    await classroom.deleteOne();
  }

  /** Adds the student to the class, or to its waiting list when the teacher approves new students. */
  async joinClassroom(user: UserType, code: string): Promise<StudentClassDto> {
    const classroom = await this.classroomModel.findOne({
      code: normalizeJoinCode(code),
    });

    if (classroom == null) {
      throw new NotFoundException('Invalid classroom code');
    }

    const isMember = [...classroom.students, ...classroom.pending].some((id) =>
      id.equals(user._id),
    );
    const teaches = classroom.teacher.equals(user._id) || classroom.teachers.some((t) => t.equals(user._id));
    if (isMember || teaches) {
      throw new ConflictException('User is already a member of this classroom');
    }

    // $addToSet keeps two quick taps on "Join" from adding the student twice.
    const status = classroom.approveJoins
      ? MembershipStatus.WAITING
      : MembershipStatus.JOINED;
    const list = status === MembershipStatus.WAITING ? 'pending' : 'students';
    await this.classroomModel.updateOne(
      { _id: classroom._id },
      { $addToSet: { [list]: user._id } },
    );

    const names = await this.teacherNames([classroom]);
    return this.toStudentClass(classroom, status, names);
  }

  /** The classes the student is in, then the ones still waiting for the teacher. */
  async listClassrooms(user: UserType): Promise<StudentClassDto[]> {
    const [joined, waiting] = await Promise.all([
      this.classroomModel.find({ students: user._id }).sort({ name: 1 }),
      this.classroomModel.find({ pending: user._id }).sort({ name: 1 }),
    ]);
    const names = await this.teacherNames([...joined, ...waiting]);
    return [
      ...joined.map((c) => this.toStudentClass(c, MembershipStatus.JOINED, names)),
      ...waiting.map((c) => this.toStudentClass(c, MembershipStatus.WAITING, names)),
    ];
  }

  /** "Name Surname" of each classroom's teacher, by teacher id. */
  private teacherNames(classrooms: ClassroomType[]): Promise<Map<string, string>> {
    return displayNames(this.profileModel, classrooms.map((c) => c.teacher));
  }

  private toStudentClass(
    classroom: ClassroomType,
    status: MembershipStatus,
    teacherNames: Map<string, string>,
  ): StudentClassDto {
    return {
      id: classroom._id.toString(),
      name: classroom.name,
      grade: classroom.grade ?? null,
      teacherName: teacherNames.get(classroom.teacher.toString()) ?? '',
      status,
    };
  }
}
