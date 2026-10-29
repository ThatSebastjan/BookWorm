package com.dreamteam.bookworm.domain.model

/*
 * Teacher-side models. Two kinds of "class" exist:
 * - a class is teaching content, e.g. "Maths 4": numbered chapters with notes and
 *   flashcards, and learning paths through them. In shared code it is a [Subject]
 *   wherever only its id and name matter (library chips, path editor).
 * - a classroom is a group of students with a join code, e.g. "4B" ([ClassSummary],
 *   [Classroom]). It uses one or more classes, and follows one path per class.
 * Chapters are ChapterSummary (flashcardCount = cards students can study).
 */

data class TeacherProfile(
    val name: String,
    /** Every classroom the teacher runs, in the order the switcher lists them. */
    val classes: List<ClassSummary>,
)

/** A file the teacher picked or dropped. [readBytes] is only called when it is uploaded. */
class PickedFile(
    val name: String,
    val sizeBytes: Long,
    val readBytes: suspend () -> ByteArray,
)

/** School years the app is for; classes and classrooms are given one of these. */
val GRADES = 6..9

// Classes (teaching content)

/** A class with its chapters, as the teacher manages it. */
data class TeacherClass(
    val id: String,
    val name: String,
    val grade: Int?,
    /** In number order. */
    val chapters: List<ChapterSummary>,
    /** Classrooms that use this class. */
    val classroomIds: List<String>,
)

/** A chapter as the teacher edits it. A flashcard with an empty id is new. */
data class ChapterContent(
    val id: String,
    val classId: String,
    val number: Int,
    val title: String,
    val notes: List<NoteSection>,
    val flashcards: List<Flashcard>,
)

// Classrooms

/** A classroom as listed in the switcher and in learning path assignments. */
data class ClassSummary(
    val id: String,
    val name: String,
    val grade: Int,
    val studentCount: Int,
    /** The teacher owns it; otherwise they co-teach it. */
    val isOwner: Boolean = true,
)

/** A teacher of a classroom: its owner or a co-teacher. */
data class ClassroomTeacher(
    val id: String,
    val name: String,
    val email: String,
    val isOwner: Boolean,
)

/** A class another teacher of the classroom added; only they can change it. */
data class SharedClass(
    val id: String,
    val name: String,
    val teacherName: String,
)

enum class AddTeacherFailure { NotFound, AlreadyTeaches }

class AddTeacherException(val failure: AddTeacherFailure) : Exception(failure.name)

enum class StudentStatus { InClass, Waiting, Offline }

data class ClassStudent(
    val id: String,
    val name: String,
    val status: StudentStatus,
)

data class Classroom(
    val id: String,
    val name: String,
    val grade: Int,
    val joinCode: String,
    val joinLink: String,
    val approveJoins: Boolean,
    /** Students in the classroom, then the ones waiting to be let in ([StudentStatus.Waiting]). */
    val students: List<ClassStudent>,
    /** The signed-in teacher's own classes this classroom uses. */
    val classIds: Set<String>,
    /** Every class of the signed-in teacher, to pick from. */
    val availableClasses: List<Subject>,
    /** Other teachers' classes this classroom uses, read-only here. */
    val otherClasses: List<SharedClass>,
    /** Owner first, then co-teachers. */
    val teachers: List<ClassroomTeacher>,
    /** The signed-in teacher owns it: they can rename it and add or remove teachers. */
    val isOwner: Boolean,
    /** Learning paths this classroom follows (at most one per class). */
    val learningPaths: List<CurriculumSummary>,
)

