package com.dreamteam.bookworm.domain.model

/**
 * What the Learn tab shows for one class (subject): the learning path the student's
 * classroom follows, chapter by chapter, with the status of every lesson.
 */
data class LearnPath(
    val subject: Subject,
    val name: String,
    val unlockMode: UnlockMode,
    /** In path order; only chapters with lessons. */
    val chapters: List<LearnChapter>,
) {
    /** The first chapter with a lesson left to do; the last one once everything is done. */
    val currentChapterIndex: Int
        get() = chapters.indexOfFirst { chapter -> chapter.lessons.any { it.status != LessonStatus.Done } }
            .takeIf { it >= 0 } ?: chapters.lastIndex
}

data class LearnChapter(
    val chapter: ChapterSummary,
    val lessons: List<LessonNode>,
) {
    val lessonsDone: Int get() = lessons.count { it.status == LessonStatus.Done }

    /** Locked chapters only show their title; their lessons open once the ones before are done. */
    val isOpen: Boolean get() = lessons.any { it.status != LessonStatus.Locked }
}

data class LessonNode(
    val lessonId: String,
    val title: String,
    val status: LessonStatus,
    /** The chapter quiz at the end of the path gets a trophy instead of a lock. */
    val isChapterQuiz: Boolean = false,
    /** Stars for finishing it the first time, as the teacher set them. */
    val stars: Int = 0,
)

enum class LessonStatus { Done, Current, Locked }

data class StudentStats(
    val streakDays: Int,
    val stars: Int,
)
