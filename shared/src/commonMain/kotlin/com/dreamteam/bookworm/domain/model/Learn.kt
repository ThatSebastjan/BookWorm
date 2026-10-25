package com.dreamteam.bookworm.domain.model

/** What the Learn tab shows for one subject: the current chapter and its lessons. */
data class LearnPath(
    val subject: Subject,
    val chapter: ChapterSummary,
    val lessons: List<LessonNode>,
    val nextChapter: ChapterSummary?,
)

data class LessonNode(
    val lessonId: String,
    val title: String,
    val status: LessonStatus,
    /** The chapter quiz at the end of the path gets a trophy instead of a lock. */
    val isChapterQuiz: Boolean = false,
)

enum class LessonStatus { Done, Current, Locked }

data class StudentStats(
    val streakDays: Int,
    val stars: Int,
)
