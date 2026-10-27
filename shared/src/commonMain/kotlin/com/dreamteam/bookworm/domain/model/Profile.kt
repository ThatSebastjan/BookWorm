package com.dreamteam.bookworm.domain.model

data class StudentProfile(
    val name: String,
    /** Grade of the student's first classroom that has one. */
    val grade: Int?,
    val level: Int,
    val starsToNextLevel: Int,
    /** 0..1 progress towards the next level. */
    val levelProgress: Float,
    val streakDays: Int,
    val stars: Int,
    val lessonsDone: Int,
    /** Seven entries, Monday first. */
    val week: List<DayActivity>,
    val classes: List<ClassProgress>,
    /** Finished a lesson or went through flashcards today, so the streak is safe. */
    val practisedToday: Boolean,
    val badges: List<Badge>,
)

data class DayActivity(
    val practised: Boolean,
    val isToday: Boolean,
)

data class ClassProgress(
    val subjectId: String,
    val subjectName: String,
    val progress: Float,
    val currentChapter: Int,
    val chapterCount: Int,
)

/** Something to earn: e.g. kind [BadgeKind.Streak] with [goal] 7 is a 7-day streak. */
data class Badge(
    val kind: BadgeKind,
    val goal: Int,
    /** How far the student has got, up to [goal]. */
    val progress: Int,
    val earned: Boolean,
)

/** What a badge is for; the app has a name and an icon for each. */
enum class BadgeKind { FirstLesson, Lessons, Streak, ChapterQuiz, ChapterDone, Stars }
