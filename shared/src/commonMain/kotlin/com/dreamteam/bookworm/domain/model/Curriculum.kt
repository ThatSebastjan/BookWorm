package com.dreamteam.bookworm.domain.model

/*
 * Learning paths (curricula). A path is an ordered list of chapters, each with
 * its lessons, for one subject. Chapters are shared content (notes and
 * flashcards live on the chapter once), and a path is stored once and assigned
 * to any number of classes or groups, so nothing is copied between classes.
 *
 * Which path a student follows for a subject: the path assigned to one of the
 * student's groups if there is one, otherwise the path assigned to the class.
 * A class or group follows at most one path per subject; assigning a new one
 * for the same subject replaces the old assignment.
 */

/** How students move through a path on their own. */
enum class UnlockMode {
    /** One lesson after another; finishing a lesson opens the next. */
    InOrder,

    /** Every chapter is open; inside a chapter lessons still go in order. */
    AnyChapter,
}

/**
 * A lesson slot on the path; [isChapterQuiz] marks the quiz that ends the chapter.
 * Its questions are written in the lesson editor (see [LessonContent]). Until the
 * teacher writes some, students get practice questions made from the chapter's flashcards.
 */
data class CurriculumLesson(
    /** Empty for a lesson added in the editor that hasn't been saved yet. */
    val id: String,
    val title: String,
    val isChapterQuiz: Boolean = false,
    /** Questions the teacher wrote; 0 means students get practice made from the chapter's flashcards. Read-only. */
    val questionCount: Int = 0,
)

data class CurriculumUnit(
    val chapter: ChapterSummary,
    val lessons: List<CurriculumLesson>,
)

/** Who a path is assigned to: a whole class or one group in it. */
sealed interface AssignmentTarget {
    val classId: String

    data class WholeClass(override val classId: String) : AssignmentTarget
    data class Group(override val classId: String, val groupId: String) : AssignmentTarget
}

data class Curriculum(
    /** Empty for a path that hasn't been saved yet. */
    val id: String,
    val name: String,
    val subjectId: String,
    val grade: Int,
    val unlockMode: UnlockMode,
    val units: List<CurriculumUnit>,
    val assignedTo: List<AssignmentTarget>,
)

data class CurriculumSummary(
    val id: String,
    val name: String,
    val subjectId: String,
    val grade: Int,
    val chapterCount: Int,
    val lessonCount: Int,
    val assignedTo: List<AssignmentTarget>,
)

/** Everything the learning path editor needs besides the path being edited. */
data class CurriculumLibrary(
    val curricula: List<CurriculumSummary>,
    val subjects: List<Subject>,
    /** Every shared chapter, in every subject; paths pick from these. */
    val chapters: List<ChapterSummary>,
    val classes: List<ClassSummary>,
)

/**
 * A question as the teacher writes it: what students see ([question], the same
 * model the student app renders) plus the answer key, which never leaves the server.
 */
data class AuthoredQuestion(
    val question: Question,
    /** The right answer, in the same shape the student app sends (see [Answer]). */
    val answer: Answer,
    /** Shown after a right answer. */
    val explanation: String,
    /** Shown after a wrong answer, before the student tries again. */
    val retryHint: String,
)

/** The questions of one lesson on a path, for the lesson editor. */
data class LessonContent(
    val lessonId: String,
    val title: String,
    val isChapterQuiz: Boolean,
    val subjectName: String,
    val chapter: ChapterSummary,
    /** In the order students get them. Empty until the teacher writes some. */
    val questions: List<AuthoredQuestion>,
)
