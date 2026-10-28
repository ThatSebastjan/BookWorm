/**
 * Development data: one teacher, three classes (content) with chapters, three
 * classrooms that use them (4B and 4A share Maths 4) and a learning path.
 * Safe to run again: it only adds what is missing, keeps students in classrooms,
 * and resets the demo classrooms' names and classes. It also removes chapters
 * from before classes existed (they belonged to classrooms).
 *
 *   npm run build && npm run seed
 */
import mongoose from 'mongoose';
import bcrypt from 'bcrypt';
import { User, UserSchema } from './auth/schema/user.schema.js';
import { Profile, ProfileSchema } from './profile/schema/profile.schema.js';
import { Classroom, ClassroomSchema } from './classroom/schema/classroom.schema.js';
import { Chapter, ChapterSchema, NoteSection } from './chapter/schema/chapter.schema.js';
import { Role } from './constants/api.enums.js';
import { SchoolClass, SchoolClassSchema } from './class/schema/class.schema.js';
import { FlashcardProgress, FlashcardProgressSchema } from './chapter/schema/flashcard-progress.schema.js';
import { LearningPath, LearningPathSchema } from './learning-path/schema/learning-path.schema.js';

const TEACHER = {
  email: 'teacher@bookworm.test',
  password: 'bookworm-teacher1', // Development only.
  name: 'Ana',
  surname: 'Kovač',
};

type SeedChapter = {
  title: string;
  notes: NoteSection[];
  flashcards: { term: string; definition: string }[];
};

const CLASSES: {
  name: string;
  grade: number;
  chapters: SeedChapter[];
}[] = [
  {
    name: 'Maths 4',
    grade: 4,
    chapters: [
      {
        title: 'Numbers to 1000',
        notes: [
          {
            heading: 'Hundreds, tens and ones',
            body: 'In 352 the 3 means three hundreds, the 5 means five tens and the 2 means two ones.',
            remember: 'A digit is worth more the further left it stands.',
          },
          {
            heading: 'Comparing numbers',
            body: 'Compare the hundreds first. If they are the same, compare the tens, then the ones.',
          },
        ],
        flashcards: [
          { term: 'Digit', definition: 'One of the signs 0 to 9 we use to write numbers.' },
          { term: 'Hundred', definition: 'Ten tens. We write it as 100.' },
          { term: 'Place value', definition: 'What a digit is worth because of where it stands.' },
        ],
      },
      {
        title: 'Add and subtract',
        notes: [
          {
            heading: 'Adding in columns',
            body: 'Write the numbers under each other, ones under ones. Add the ones first, then the tens, then the hundreds.',
            remember: 'If a column adds up to 10 or more, carry the ten to the next column.',
          },
        ],
        flashcards: [
          { term: 'Sum', definition: 'The answer when we add numbers.' },
          { term: 'Difference', definition: 'The answer when we take one number away from another.' },
        ],
      },
      {
        title: 'Fractions',
        notes: [
          {
            heading: 'What is a fraction?',
            body:
              'A fraction is a part of a whole. When we cut something into equal parts, each part is a fraction of the whole. ' +
              'The top number is the numerator: the parts we have. The bottom number is the denominator: the parts in all.',
          },
          {
            heading: 'Equal parts',
            body: 'The parts must all be the same size. If one piece is bigger than the other, they are not halves.',
            remember: 'The more equal parts there are, the smaller each part is. A quarter is smaller than a half.',
          },
        ],
        flashcards: [
          { term: 'Numerator', definition: 'The top number. It tells how many parts we have.' },
          { term: 'Denominator', definition: 'The bottom number. It tells how many equal parts make the whole.' },
          { term: 'Half', definition: 'One of two equal parts. We write it as 1/2.' },
          { term: 'Quarter', definition: 'One of four equal parts. We write it as 1/4.' },
        ],
      },
      {
        title: 'Telling the time',
        notes: [
          {
            heading: 'The two hands',
            body: 'The short hand shows the hour. The long hand shows the minutes.',
          },
        ],
        flashcards: [
          { term: 'Quarter past', definition: '15 minutes after the hour.' },
          { term: 'Half past', definition: '30 minutes after the hour.' },
        ],
      },
    ],
  },
  {
    name: 'Nature 4',
    grade: 4,
    chapters: [
      {
        title: 'Plants and seeds',
        notes: [
          {
            heading: 'What a plant needs',
            body: 'Plants need water, light, air and soil to grow.',
            remember: 'Leaves use sunlight to make food for the plant.',
          },
        ],
        flashcards: [
          { term: 'Seed', definition: 'The part of a plant a new plant grows from.' },
          { term: 'Root', definition: 'The part under the ground that takes in water.' },
        ],
      },
      {
        title: 'Animal homes',
        notes: [
          {
            heading: 'Nests, burrows and ponds',
            body: 'Birds build nests, rabbits dig burrows and frogs live near ponds.',
          },
        ],
        flashcards: [
          { term: 'Burrow', definition: 'A hole in the ground an animal digs to live in.' },
          { term: 'Habitat', definition: 'The place where an animal or plant lives.' },
        ],
      },
    ],
  },
  {
    name: 'Reading',
    grade: 4,
    chapters: [
      {
        title: 'Writing a story',
        notes: [
          {
            heading: 'Beginning, middle and end',
            body: 'A story starts by telling who and where, has a problem in the middle and solves it at the end.',
          },
        ],
        flashcards: [{ term: 'Character', definition: 'A person or animal in a story.' }],
      },
    ],
  },
];

const CLASSROOMS = [
  { name: '4B', grade: 4, code: 'K7M2Q', approveJoins: false, classes: ['Maths 4', 'Nature 4'] },
  { name: '4A', grade: 4, code: 'P4X9T', approveJoins: false, classes: ['Maths 4'] },
  // The teacher lets each student in, so joining shows "waiting for the teacher".
  { name: 'Reading club', grade: 4, code: 'R2D7W', approveJoins: true, classes: ['Reading'] },
];

/** Practice lessons per chapter number of Maths 4; each chapter also ends with a quiz. */
const MATHS_PATH = { name: 'Maths 4 – main path', classroom: 'K7M2Q', lessons: [['Place value'], ['Columns', 'Carrying'], ['Halves', 'Quarters']] };

async function seed() {
  const uri = process.env.DATABASE_URL;
  if (!uri) throw new Error('DATABASE_URL is not set. Run with --env-file=.env');
  await mongoose.connect(uri);

  const users = mongoose.model(User.name, UserSchema);
  const profiles = mongoose.model(Profile.name, ProfileSchema);
  const classrooms = mongoose.model(Classroom.name, ClassroomSchema);
  const classes = mongoose.model(SchoolClass.name, SchoolClassSchema);
  const chapters = mongoose.model(Chapter.name, ChapterSchema);
  const progress = mongoose.model(FlashcardProgress.name, FlashcardProgressSchema);
  const paths = mongoose.model(LearningPath.name, LearningPathSchema);
  // Unique indexes (emails, join codes, chapter numbers) must exist before inserting.
  await users.syncIndexes();
  await profiles.syncIndexes();
  await classrooms.syncIndexes();
  await classes.syncIndexes();
  // Drops the old (classroom, number) index too.
  await chapters.syncIndexes();
  await paths.syncIndexes();

  let teacher = await users.findOne({ email: TEACHER.email });
  if (teacher == null) {
    const rounds = Number(process.env.BCRYPT_SALT_ROUNDS ?? 10);
    teacher = await users.create({
      email: TEACHER.email,
      password: await bcrypt.hash(TEACHER.password, rounds),
      role: Role.TEACHER,
    });
    const profile = await profiles.create({ user: teacher._id, name: TEACHER.name, surname: TEACHER.surname });
    teacher.profile = profile._id;
    await teacher.save();
    console.log(`Teacher ${TEACHER.email} / ${TEACHER.password}`);
  }

  // Chapters from before classes existed hung off classrooms.
  const legacy = await chapters.find({ schoolClass: { $exists: false } }).distinct('_id');
  if (legacy.length > 0) {
    await progress.deleteMany({ chapter: { $in: legacy } });
    await chapters.deleteMany({ _id: { $in: legacy } });
    console.log(`Removed ${legacy.length} chapters from before classes`);
  }

  const classIds = new Map<string, mongoose.Types.ObjectId>();
  for (const seedClass of CLASSES) {
    let schoolClass = await classes.findOne({ teacher: teacher._id, name: seedClass.name });
    if (schoolClass == null) {
      schoolClass = await classes.create({ name: seedClass.name, grade: seedClass.grade, teacher: teacher._id });
      console.log(`Class ${seedClass.name}`);
    }
    classIds.set(seedClass.name, schoolClass._id);
    for (const [index, chapter] of seedClass.chapters.entries()) {
      const number = index + 1;
      if (await chapters.exists({ schoolClass: schoolClass._id, number })) continue;
      await chapters.create({ schoolClass: schoolClass._id, number, ...chapter });
      console.log(`  Chapter ${number}: ${chapter.title}`);
    }
  }

  for (const room of CLASSROOMS) {
    const fields = {
      name: room.name,
      grade: room.grade,
      approveJoins: room.approveJoins,
      teacher: teacher._id,
      classes: room.classes.map((name) => classIds.get(name)!),
    };
    // Students who already joined stay.
    await classrooms.updateOne({ code: room.code }, { $set: fields, $setOnInsert: { code: room.code } }, { upsert: true });
    console.log(`Classroom ${room.name}, code ${room.code}: ${room.classes.join(', ')}`);
  }

  const mathsId = classIds.get('Maths 4')!;
  if (!(await paths.exists({ schoolClass: mathsId }))) {
    const mathsChapters = await chapters.find({ schoolClass: mathsId }).sort({ number: 1 });
    const classroom = await classrooms.findOne({ code: MATHS_PATH.classroom });
    await paths.create({
      schoolClass: mathsId,
      teacher: teacher._id,
      name: MATHS_PATH.name,
      classrooms: classroom ? [classroom._id] : [],
      units: MATHS_PATH.lessons.map((titles, i) => ({
        chapter: mathsChapters[i]._id,
        lessons: [...titles.map((title) => ({ title, stars: 10 })), { title: 'Chapter quiz', isChapterQuiz: true, stars: 20 }],
      })),
    });
    console.log(`Learning path ${MATHS_PATH.name}`);
  }
}

try {
  await seed();
} finally {
  await mongoose.disconnect();
}
