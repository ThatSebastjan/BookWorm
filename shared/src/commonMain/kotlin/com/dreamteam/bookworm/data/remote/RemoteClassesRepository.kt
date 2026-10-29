package com.dreamteam.bookworm.data.remote

import com.dreamteam.bookworm.domain.model.Chapter
import com.dreamteam.bookworm.domain.model.ChapterSummary
import com.dreamteam.bookworm.domain.model.JoinClassException
import com.dreamteam.bookworm.domain.model.JoinFailure
import com.dreamteam.bookworm.domain.model.MembershipStatus
import com.dreamteam.bookworm.domain.model.StudentClass
import com.dreamteam.bookworm.domain.model.Subject
import com.dreamteam.bookworm.domain.repository.LibraryRepository
import com.dreamteam.bookworm.domain.repository.StudentClassesRepository
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The student's classrooms and their library, from the backend:
 * `GET /classroom/list`, `POST /classroom/join/{code}`, `GET /class/mine`,
 * `GET /class/{id}/chapters`, `GET /chapter/{id}`, `POST /flashcard/{id}/progress` and
 * `DELETE /chapter/{id}/progress`.
 *
 * Classrooms are what students join ([StudentClass]); the library shows the classes
 * (teaching content) those classrooms use. Each class is one [Subject] chip, so a
 * chapter's [ChapterSummary.subjectId] is the id of its class.
 */
class RemoteClassesRepository(private val api: AuthorizedApi) : StudentClassesRepository, LibraryRepository {

    override suspend fun getMyClasses(): List<StudentClass> =
        api.request { url { appendPathSegments("classroom", "list") } }.bodyOrThrow<List<ClassResponse>>().map { it.toModel() }

    override suspend fun joinClass(code: String): StudentClass {
        val response = api.request {
            method = HttpMethod.Post
            url { appendPathSegments("classroom", "join", code) }
        }
        return when (response.status) {
            HttpStatusCode.NotFound -> throw JoinClassException(JoinFailure.CodeNotFound)
            HttpStatusCode.Conflict -> throw JoinClassException(JoinFailure.AlreadyInClass)
            else -> response.bodyOrThrow<ClassResponse>().toModel()
        }
    }

    /** The classes of every classroom the student was let into; waiting ones add nothing yet. */
    override suspend fun getSubjects(): List<Subject> =
        api.request { url { appendPathSegments("class", "mine") } }.bodyOrThrow<List<LibraryClassResponse>>()
            .map { Subject(it.id, it.name) }

    override suspend fun getChapters(subjectId: String): List<ChapterSummary> =
        api.request { url { appendPathSegments("class", subjectId, "chapters") } }
            .bodyOrThrow<List<ChapterSummaryJson>>()
            .map { it.toModel() }

    override suspend fun getChapter(chapterId: String): Chapter {
        val chapter = api.request { url { appendPathSegments("chapter", chapterId) } }.bodyOrThrow<ChapterJson>()
        return Chapter(
            id = chapter.id,
            subjectName = chapter.className,
            number = chapter.number,
            title = chapter.title,
            teacherName = chapter.teacherName,
            notes = chapter.notes.map { it.toModel() },
            flashcards = chapter.flashcards.map { it.toModel() },
            knownFlashcardIds = chapter.flashcards.filter { it.known }.mapNotNull { it.id }.toSet(),
        )
    }

    override suspend fun resetFlashcards(chapterId: String) {
        api.request {
            method = HttpMethod.Delete
            url { appendPathSegments("chapter", chapterId, "progress") }
        }.successOrThrow()
    }

    override suspend fun recordFlashcard(flashcardId: String, known: Boolean) {
        api.request {
            method = HttpMethod.Post
            url { appendPathSegments("flashcard", flashcardId, "progress") }
            contentType(ContentType.Application.Json)
            setBody(ProgressRequest(known))
        }.successOrThrow()
    }

    @Serializable
    private enum class StatusResponse {
        @SerialName("joined") Joined,
        @SerialName("waiting") Waiting,
    }

    @Serializable
    private data class ClassResponse(
        val id: String,
        val name: String,
        val grade: Int? = null,
        val teacherName: String,
        val status: StatusResponse,
    ) {
        fun toModel() = StudentClass(
            id = id,
            name = name,
            grade = grade,
            teacherName = teacherName,
            status = when (status) {
                StatusResponse.Joined -> MembershipStatus.Joined
                StatusResponse.Waiting -> MembershipStatus.WaitingForTeacher
            },
        )
    }

    @Serializable
    private data class LibraryClassResponse(val id: String, val name: String)

    @Serializable
    private data class ProgressRequest(val known: Boolean)
}
