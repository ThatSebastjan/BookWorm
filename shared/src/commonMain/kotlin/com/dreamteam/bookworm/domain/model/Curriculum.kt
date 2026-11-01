package com.dreamteam.bookworm.domain.model

/*
 * Learning paths (curricula). A path is an ordered list of one class's chapters,
 * each with its lessons. ("Subject" here is the class, the teaching content; see
 * Teacher.kt.) A path is stored once and assigned to any number of classrooms.
 * A classroom follows at most one path per class; assigning a new one for the
 * same class replaces the old assignment, and makes the classroom use the class.
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
    /** Stars a student gets the first time they finish it, [MIN_LESSON_STARS] to [MAX_LESSON_STARS]. */
    val stars: Int = if (isChapterQuiz) DEFAULT_QUIZ_STARS else DEFAULT_LESSON_STARS,
)

const val DEFAULT_LESSON_STARS = 10
const val DEFAULT_QUIZ_STARS = 20
const val MIN_LESSON_STARS = 0
const val MAX_LESSON_STARS = 100

data class CurriculumUnit(
    val chapter: ChapterSummary,
    val lessons: List<CurriculumLesson>,
)

data class Curriculum(
    /** Empty for a path that hasn't been saved yet. */
    val id: String,
    val name: String,
    val subjectId: String,
    val grade: Int,
    val unlockMode: UnlockMode,
    val units: List<CurriculumUnit>,
    /** Ids of the classrooms that follow this path. */
    val assignedTo: List<String>,
)

data class CurriculumSummary(
    val id: String,
    val name: String,
    val subjectId: String,
    val grade: Int,
    val chapterCount: Int,
    val lessonCount: Int,
    /** Ids of the classrooms that follow this path. */
    val assignedTo: List<String>,
)

/** How far the students of each classroom following a path have got, for the teacher. */
data class PathProgress(
    val lessonCount: Int,
    val chapterCount: Int,
    val classrooms: List<ClassroomProgress>,
)

data class ClassroomProgress(
    val classroomId: String,
    val classroomName: String,
    /** By name. */
    val students: List<StudentProgress>,
)

data class StudentProgress(
    val id: String,
    val name: String,
    /** Different lessons of the path finished. */
    val lessonsDone: Int,
    /** 1-based position on the path of the chapter they are on. */
    val currentChapter: Int,
    /** Stars earned on this path. */
    val stars: Int,
)

/** Everything the learning path editor needs besides the path being edited. */
data class CurriculumLibrary(
    val curricula: List<CurriculumSummary>,
    /** The teacher's classes (teaching content). */
    val subjects: List<Subject>,
    /** Every chapter of those classes; paths pick from these. */
    val chapters: List<ChapterSummary>,
    /** The teacher's classrooms, to assign paths to. */
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

/** Practice questions made from flashcards: "What does X mean?" with up to four meanings to choose from. */
fun questionsFromFlashcards(cards: List<Flashcard>, max: Int = 6): List<AuthoredQuestion> {
    if (cards.size < 2) return emptyList()
    return cards.take(max).mapIndexed { index, card ->
        val others = (cards - card).let { rest -> List(minOf(3, rest.size)) { rest[(index + it) % rest.size] } }
        val meanings = (others.map { it.definition } + card.definition).distinct()
        // Put the right meaning in a different place each time.
        val rotated = meanings.indices.map { meanings[(it + index) % meanings.size] }
        val options = rotated.mapIndexed { i, text -> Option(('a' + i).toString(), text) }
        AuthoredQuestion(
            question = Question.SingleChoice("fc-${card.id}", "What does \u201C${card.term}\u201D mean?", options),
            answer = Answer.Choice(options.first { it.text == card.definition }.id),
            explanation = "${card.term}: ${card.definition}",
            retryHint = "Think back to the flashcard for \u201C${card.term}\u201D.",
        )
    }
}
