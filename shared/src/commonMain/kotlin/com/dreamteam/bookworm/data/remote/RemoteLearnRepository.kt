package com.dreamteam.bookworm.data.remote

import com.dreamteam.bookworm.domain.model.Answer
import com.dreamteam.bookworm.domain.model.AnswerResult
import com.dreamteam.bookworm.domain.model.Badge
import com.dreamteam.bookworm.domain.model.BadgeKind
import com.dreamteam.bookworm.domain.model.ClassProgress
import com.dreamteam.bookworm.domain.model.DayActivity
import com.dreamteam.bookworm.domain.model.LearnChapter
import com.dreamteam.bookworm.domain.model.LearnPath
import com.dreamteam.bookworm.domain.model.Lesson
import com.dreamteam.bookworm.domain.model.LessonNode
import com.dreamteam.bookworm.domain.model.LessonStatus
import com.dreamteam.bookworm.domain.model.LessonSummary
import com.dreamteam.bookworm.domain.model.StudentProfile
import com.dreamteam.bookworm.domain.model.StudentStats
import com.dreamteam.bookworm.domain.model.Subject
import com.dreamteam.bookworm.domain.model.UnlockMode
import com.dreamteam.bookworm.domain.repository.LearnRepository
import com.dreamteam.bookworm.domain.repository.LessonRepository
import com.dreamteam.bookworm.domain.repository.ProfileRepository
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The Learn tab, lessons and the profile, from the backend: `GET /learn/class/{id}`,
 * `GET /learn/stats`, `GET /learn/profile`, `GET /learn/lesson/{id}`, and
 * `POST /learn/lesson/{id}/answer` and `/complete`. Answers are checked on the server,
 * which also keeps the stars and streaks.
 */
class RemoteLearnRepository(private val api: AuthorizedApi) : LearnRepository, LessonRepository, ProfileRepository {

    override suspend fun getLearnPath(subjectId: String): LearnPath? {
        val learn = get<LearnClassJson>("learn", "class", subjectId)
        val path = learn.path ?: return null
        return LearnPath(
            subject = Subject(learn.classId, learn.className),
            name = path.name,
            unlockMode = if (path.unlockMode == "anyChapter") UnlockMode.AnyChapter else UnlockMode.InOrder,
            chapters = path.chapters.map { chapter ->
                LearnChapter(chapter.chapter.toModel(), chapter.lessons.map { it.toModel() })
            },
        ).takeIf { it.chapters.isNotEmpty() }
    }

    override suspend fun getStats(): StudentStats =
        get<StatsJson>("learn", "stats").let { StudentStats(it.streakDays, it.stars) }

    override suspend fun getLesson(lessonId: String): Lesson {
        val lesson = get<LessonJson>("learn", "lesson", lessonId)
        return Lesson(lesson.id, lesson.title, lesson.questions.map { it.toModel() }, lesson.stars, lesson.alreadyDone)
    }

    override suspend fun submitAnswer(lessonId: String, questionId: String, answer: Answer): AnswerResult {
        val result = post<AnswerResultJson>("learn", "lesson", lessonId, "answer", body = SubmitAnswerJson(questionId, answer.toJson()))
        return AnswerResult(result.correct, result.explanation)
    }

    override suspend fun completeLesson(lessonId: String): LessonSummary {
        val summary = post<LessonSummaryJson>("learn", "lesson", lessonId, "complete")
        return LessonSummary(summary.starsEarned, summary.correctAnswers, summary.totalQuestions)
    }

    override suspend fun getProfile(): StudentProfile {
        val profile = get<ProfileJson>("learn", "profile")
        return StudentProfile(
            name = profile.name,
            grade = profile.grade,
            level = profile.level,
            starsToNextLevel = profile.starsToNextLevel,
            levelProgress = profile.levelProgress,
            streakDays = profile.streakDays,
            stars = profile.stars,
            lessonsDone = profile.lessonsDone,
            week = profile.week.map { DayActivity(it.practised, it.isToday) },
            classes = profile.classes.map {
                ClassProgress(it.classId, it.className, it.progress, it.currentChapter, it.chapterCount)
            },
            // Badges this version of the app doesn't know yet are left out.
            practisedToday = profile.practisedToday,
            badges = profile.badges.mapNotNull { badge ->
                badgeKinds[badge.kind]?.let { Badge(it, badge.goal, badge.progress, badge.earned) }
            },
        )
    }

    private val badgeKinds = mapOf(
        "firstLesson" to BadgeKind.FirstLesson,
        "lessons" to BadgeKind.Lessons,
        "streak" to BadgeKind.Streak,
        "chapterQuiz" to BadgeKind.ChapterQuiz,
        "chapterDone" to BadgeKind.ChapterDone,
        "stars" to BadgeKind.Stars,
    )

    // Requests

    private suspend inline fun <reified T> get(vararg path: String): T =
        api.request { url { appendPathSegments(*path) } }.bodyOrThrow()

    private suspend inline fun <reified T> post(vararg path: String, body: Any? = null): T =
        api.request {
            method = HttpMethod.Post
            url { appendPathSegments(*path) }
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }.bodyOrThrow()

    // JSON

    @Serializable
    private data class LearnClassJson(val classId: String, val className: String, val path: PathJson? = null)

    @Serializable
    private data class PathJson(val id: String, val name: String, val unlockMode: String, val chapters: List<ChapterJson>)

    @Serializable
    private data class ChapterJson(val chapter: ChapterSummaryJson, val lessons: List<LessonNodeJson>)

    @Serializable
    private enum class StatusJson {
        @SerialName("done") Done,
        @SerialName("current") Current,
        @SerialName("locked") Locked,
    }

    @Serializable
    private data class LessonNodeJson(val id: String, val title: String, val isChapterQuiz: Boolean, val status: StatusJson, val stars: Int = 0) {
        fun toModel() = LessonNode(
            lessonId = id,
            title = title,
            status = when (status) {
                StatusJson.Done -> LessonStatus.Done
                StatusJson.Current -> LessonStatus.Current
                StatusJson.Locked -> LessonStatus.Locked
            },
            isChapterQuiz = isChapterQuiz,
            stars = stars,
        )
    }

    @Serializable
    private data class StatsJson(val streakDays: Int, val stars: Int)

    @Serializable
    private data class LessonJson(
        val id: String,
        val title: String,
        val questions: List<QuestionJson>,
        val stars: Int = 0,
        val alreadyDone: Boolean = false,
    )

    @Serializable
    private data class SubmitAnswerJson(val questionId: String, val answer: AnswerJson)

    @Serializable
    private data class AnswerResultJson(val correct: Boolean, val explanation: String)

    @Serializable
    private data class LessonSummaryJson(val starsEarned: Int, val correctAnswers: Int, val totalQuestions: Int)

    @Serializable
    private data class DayJson(val practised: Boolean, val isToday: Boolean)

    @Serializable
    private data class ClassProgressJson(
        val classId: String,
        val className: String,
        val progress: Float,
        val currentChapter: Int,
        val chapterCount: Int,
    )

    @Serializable
    private data class BadgeJson(val id: String, val kind: String, val goal: Int, val progress: Int, val earned: Boolean)

    @Serializable
    private data class ProfileJson(
        val name: String,
        val grade: Int? = null,
        val level: Int,
        val starsToNextLevel: Int,
        val levelProgress: Float,
        val streakDays: Int,
        val stars: Int,
        val lessonsDone: Int,
        val week: List<DayJson>,
        val classes: List<ClassProgressJson>,
        val practisedToday: Boolean = false,
        val badges: List<BadgeJson>,
    )
}
