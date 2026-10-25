package com.dreamteam.bookworm.domain.model

/*
 * Teacher-side models. They reuse the student models where the data is the
 * same thing: chapters are ChapterSummary (flashcardCount = cards students can
 * study), generated cards wrap a Flashcard, and quiz questions are
 * Question.SingleChoice with Option answers.
 */

data class TeacherProfile(
    val name: String,
    /** Every class the teacher teaches, in the order the class switcher lists them. */
    val classes: List<ClassSummary>,
    /** Generated cards still waiting for the teacher's review, shown on the sidebar. */
    val cardsToReview: Int,
)

/** A file the teacher picked or dropped. [readBytes] is only called when it is uploaded. */
class PickedFile(
    val name: String,
    val sizeBytes: Long,
    val readBytes: suspend () -> ByteArray,
)

// Materials

enum class CardStyle { Terms, QuestionsAndAnswers, Mix }

/** Where generated cards go and how they are written. */
data class CardRequest(
    val chapterId: String,
    val style: CardStyle,
    val readingGrade: Int,
)

enum class FileKind { Pdf, Document, Slides, Text, Photo }

sealed interface UploadStatus {
    /** Cards are being written; [progress] is 0..1. */
    data class Processing(val progress: Float) : UploadStatus
    data class Ready(val cardCount: Int) : UploadStatus

    /** The file could not be read well enough; the teacher should replace it. */
    data object Unreadable : UploadStatus
}

data class Upload(
    val id: String,
    val fileName: String,
    val kind: FileKind,
    val status: UploadStatus,
)

data class MaterialsOverview(
    val subjects: List<Subject>,
    val chapters: List<ChapterSummary>,
    val readingGrades: List<Int>,
    val defaultGrade: Int,
    val uploads: List<Upload>,
)

// Flashcard review

enum class DraftStatus { Pending, Approved, Removed }

/** A generated [flashcard] the teacher checks before students see it. */
data class DraftCard(
    val flashcard: Flashcard,
    val sourcePage: Int,
    val sourceQuote: String,
    val status: DraftStatus,
    val edited: Boolean,
    /** Students can already study it. */
    val published: Boolean,
    /** Set when the generator thinks the card disagrees with the material. Teacher-facing content, not localised. */
    val issue: String?,
) {
    val id: String get() = flashcard.id
}

/** Cards go to a shared chapter, so every class whose learning path includes it gets them. */
data class ReviewBatch(
    val uploadId: String,
    val fileName: String,
    val subjectName: String,
    val chapter: ChapterSummary,
    val cards: List<DraftCard>,
)

// Classes

/** A class as listed in the class switcher and in learning path assignments. */
data class ClassSummary(
    val id: String,
    val name: String,
    val grade: Int,
    val studentCount: Int,
    val groups: List<ClassGroup>,
)

/**
 * Some students of one class who follow their own learning path for a subject,
 * e.g. a reading group. A student can be in several groups of the same class.
 */
data class ClassGroup(
    /** Empty for a group that hasn't been saved yet. */
    val id: String,
    val name: String,
    val studentIds: List<String>,
)

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
    val students: List<ClassStudent>,
    val groups: List<ClassGroup>,
    /** Learning paths assigned to the whole class or to one of its groups. */
    val learningPaths: List<CurriculumSummary>,
)

// Pop quiz

data class QuizSetup(
    val className: String,
    val studentsInClass: Int,
    val subjects: List<Subject>,
    /** Chapters on the class's learning paths that students have cards for. */
    val chapters: List<ChapterSummary>,
    val questionCounts: List<Int>,
    val minuteOptions: List<Int>,
)

data class QuizConfig(
    val classId: String,
    val chapterId: String,
    val questionCount: Int,
    val minutes: Int,
    val calmMode: Boolean,
    val chime: Boolean,
)

data class LiveQuiz(
    val id: String,
    val className: String,
    val chapter: ChapterSummary,
    val questionCount: Int,
    val calmMode: Boolean,
    val chime: Boolean,
    val totalSeconds: Int,
    val elapsedSeconds: Int,
    val paused: Boolean,
    val studentCount: Int,
    val studentsFinished: Int,
) {
    val secondsLeft: Int get() = (totalSeconds - elapsedSeconds).coerceAtLeast(0)
    val progress: Float get() = if (totalSeconds == 0) 1f else (elapsedSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
}

/** How the class answered one question, shown after the quiz. */
data class QuestionResult(
    val quizId: String,
    val className: String,
    val chapter: ChapterSummary,
    val number: Int,
    val questionCount: Int,
    val question: Question.SingleChoice,
    val correctOptionId: String,
    /** How many students picked each option, by option id. */
    val answerCounts: Map<String, Int>,
    val explanation: String,
    /** Drawn next to the explanation when the question is about a fraction. */
    val fraction: Fraction?,
) {
    val studentCount: Int get() = answerCounts.values.sum()
    val correctCount: Int get() = answerCounts[correctOptionId] ?: 0
}
