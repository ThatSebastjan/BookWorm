package com.dreamteam.bookworm.domain.model

data class StudentProfile(
    val name: String,
    val grade: Int,
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

data class Badge(
    val id: String,
    val name: String,
    val kind: BadgeKind,
    val earned: Boolean,
)

/** Picks the badge icon; unknown kinds from the backend fall back to [Star]. */
enum class BadgeKind { Star, Streak, Fractions, Quiz }
