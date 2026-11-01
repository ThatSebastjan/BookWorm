package com.dreamteam.bookworm.data.remote

import com.dreamteam.bookworm.domain.model.AddTeacherException
import com.dreamteam.bookworm.domain.model.AddTeacherFailure
import com.dreamteam.bookworm.domain.model.AuthoredQuestion
import com.dreamteam.bookworm.domain.model.ChapterContent
import com.dreamteam.bookworm.domain.model.ChapterSummary
import com.dreamteam.bookworm.domain.model.ClassStudent
import com.dreamteam.bookworm.domain.model.ClassSummary
import com.dreamteam.bookworm.domain.model.Classroom
import com.dreamteam.bookworm.domain.model.ClassroomProgress
import com.dreamteam.bookworm.domain.model.ClassroomTeacher
import com.dreamteam.bookworm.domain.model.Curriculum
import com.dreamteam.bookworm.domain.model.CurriculumLesson
import com.dreamteam.bookworm.domain.model.CurriculumLibrary
import com.dreamteam.bookworm.domain.model.CurriculumSummary
import com.dreamteam.bookworm.domain.model.CurriculumUnit
import com.dreamteam.bookworm.domain.model.LessonContent
import com.dreamteam.bookworm.domain.model.PathProgress
import com.dreamteam.bookworm.domain.model.SharedClass
import com.dreamteam.bookworm.domain.model.StudentProgress
import com.dreamteam.bookworm.domain.model.StudentStatus
import com.dreamteam.bookworm.domain.model.Subject
import com.dreamteam.bookworm.domain.model.TeacherClass
import com.dreamteam.bookworm.domain.model.TeacherProfile
import com.dreamteam.bookworm.domain.model.UnlockMode
import com.dreamteam.bookworm.domain.model.questionsFromFlashcards
import com.dreamteam.bookworm.domain.repository.ClassesRepository
import com.dreamteam.bookworm.domain.repository.ClassroomRepository
import com.dreamteam.bookworm.domain.repository.CurriculumRepository
import com.dreamteam.bookworm.domain.repository.TeacherRepository
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The teacher desktop app against the backend: classes and chapters (`/class`, `/chapter`),
 * classrooms (`/classroom`) and learning paths (`/learning-path`). [teacherName] is the
 * signed-in teacher's name.
 */
class RemoteTeacherRepository(
    private val api: AuthorizedApi,
    private val teacherName: () -> String,
) : TeacherRepository, ClassesRepository, ClassroomRepository, CurriculumRepository {

    // TeacherRepository

    override suspend fun getTeacher(): TeacherProfile = TeacherProfile(teacherName(), classrooms())

    override suspend fun createClassroom(name: String, grade: Int): ClassSummary {
        val created = call<CreatedClassroomJson>(HttpMethod.Post, "classroom", body = CreateJson(name.trim(), grade))
        return ClassSummary(created.id, created.name, created.grade ?: grade, studentCount = 0)
    }

    private suspend fun classrooms(): List<ClassSummary> =
        call<List<ClassroomSummaryJson>>(HttpMethod.Get, "classroom", "listCreated")
            .map { ClassSummary(it.id, it.name, it.grade ?: 0, it.studentCount, it.isOwner) }

    // ClassesRepository

    override suspend fun getClasses(): List<TeacherClass> =
        call<List<TeacherClassJson>>(HttpMethod.Get, "class").map { it.toModel() }

    override suspend fun createClass(name: String, grade: Int): TeacherClass =
        call<TeacherClassJson>(HttpMethod.Post, "class", body = CreateJson(name.trim(), grade)).toModel()

    override suspend fun updateClass(classId: String, name: String, grade: Int) =
        send(HttpMethod.Patch, "class", classId, body = CreateJson(name.trim(), grade))

    override suspend fun deleteClass(classId: String) = send(HttpMethod.Delete, "class", classId)

    override suspend fun addChapter(classId: String, title: String): ChapterSummary =
        call<ChapterSummaryJson>(HttpMethod.Post, "class", classId, "chapters", body = TitleJson(title.trim())).toModel()

    override suspend fun reorderChapters(classId: String, chapterIds: List<String>): List<ChapterSummary> =
        call<List<ChapterSummaryJson>>(HttpMethod.Put, "class", classId, "chapters", "order", body = ChapterOrderJson(chapterIds))
            .map { it.toModel() }

    override suspend fun getChapter(chapterId: String): ChapterContent =
        call<ChapterJson>(HttpMethod.Get, "chapter", chapterId).toContent()

    override suspend fun saveChapter(chapter: ChapterContent): ChapterContent {
        val body = ChapterUpdateJson(
            title = chapter.title.trim(),
            notes = chapter.notes.map { it.copy(heading = it.heading.trim(), body = it.body.trim()).toJson() },
            flashcards = chapter.flashcards.map { it.copy(term = it.term.trim(), definition = it.definition.trim()).toJson() },
        )
        return call<ChapterJson>(HttpMethod.Put, "chapter", chapter.id, body = body).toContent()
    }

    override suspend fun deleteChapter(chapterId: String) = send(HttpMethod.Delete, "chapter", chapterId)

    // ClassroomRepository

    override suspend fun getClassroom(classId: String): Classroom = coroutineScope {
        val detail = async { call<ClassroomDetailJson>(HttpMethod.Get, "classroom", classId) }
        val classes = async { call<List<TeacherClassJson>>(HttpMethod.Get, "class") }
        val paths = async { call<List<PathSummaryJson>>(HttpMethod.Get, "learning-path") }
        toClassroom(detail.await(), classes.await(), paths.await())
    }

    override suspend fun admit(classId: String, studentId: String): Classroom {
        send(HttpMethod.Post, "classroom", classId, "students", studentId, "admit")
        return getClassroom(classId)
    }

    override suspend fun removeStudent(classId: String, studentId: String): Classroom {
        send(HttpMethod.Delete, "classroom", classId, "students", studentId)
        return getClassroom(classId)
    }

    override suspend fun setApproveJoins(classId: String, approve: Boolean): Classroom {
        send(HttpMethod.Patch, "classroom", classId, body = ApproveJoinsJson(approve))
        return getClassroom(classId)
    }

    override suspend fun setClasses(classId: String, classIds: Set<String>): Classroom {
        send(HttpMethod.Put, "classroom", classId, "classes", body = ClassIdsJson(classIds.toList()))
        return getClassroom(classId)
    }

    override suspend fun addTeacher(classId: String, email: String): Classroom {
        val response = api.request { build(HttpMethod.Post, arrayOf("classroom", classId, "teachers"), EmailJson(email.trim())) }
        when (response.status) {
            HttpStatusCode.NotFound -> throw AddTeacherException(AddTeacherFailure.NotFound)
            HttpStatusCode.Conflict -> throw AddTeacherException(AddTeacherFailure.AlreadyTeaches)
            else -> response.successOrThrow()
        }
        return getClassroom(classId)
    }

    override suspend fun removeTeacher(classId: String, teacherId: String): Classroom? {
        val response = api.request { build(HttpMethod.Delete, arrayOf("classroom", classId, "teachers", teacherId), null) }
        response.successOrThrow()
        // An empty answer means the teacher left: they can't open the classroom any more.
        return if (response.bodyAsText().isBlank()) null else getClassroom(classId)
    }

    private fun toClassroom(detail: ClassroomDetailJson, classes: List<TeacherClassJson>, paths: List<PathSummaryJson>) = Classroom(
        id = detail.id,
        name = detail.name,
        grade = detail.grade ?: 0,
        joinCode = detail.code,
        joinLink = joinLink(detail.code),
        approveJoins = detail.approveJoins,
        students = detail.students.map { ClassStudent(it.id, it.name, StudentStatus.InClass) } +
            detail.waiting.map { ClassStudent(it.id, it.name, StudentStatus.Waiting) },
        classIds = detail.classIds.toSet(),
        availableClasses = classes.map { Subject(it.id, it.name) },
        otherClasses = detail.otherClasses.map { SharedClass(it.id, it.name, it.teacherName) },
        teachers = detail.teachers.map { ClassroomTeacher(it.id, it.name, it.email, it.isOwner) },
        isOwner = detail.isOwner,
        learningPaths = paths.filter { detail.id in it.classroomIds }.map { it.toModel() },
    )

    // CurriculumRepository

    override suspend fun getLibrary(): CurriculumLibrary = coroutineScope {
        val paths = async { call<List<PathSummaryJson>>(HttpMethod.Get, "learning-path") }
        val classes = async { call<List<TeacherClassJson>>(HttpMethod.Get, "class") }
        val classrooms = async { classrooms() }
        CurriculumLibrary(
            curricula = paths.await().map { it.toModel() },
            subjects = classes.await().map { Subject(it.id, it.name) },
            chapters = classes.await().flatMap { c -> c.chapters.map { it.toModel() } },
            classes = classrooms.await(),
        )
    }

    override suspend fun getCurriculum(curriculumId: String): Curriculum =
        call<PathJson>(HttpMethod.Get, "learning-path", curriculumId).toModel()

    override suspend fun saveCurriculum(curriculum: Curriculum): Curriculum {
        val body = SavePathJson(
            name = curriculum.name.trim(),
            classId = curriculum.subjectId,
            unlockMode = curriculum.unlockMode.toJson(),
            units = curriculum.units.map { unit ->
                SavePathUnitJson(
                    chapterId = unit.chapter.id,
                    lessons = unit.lessons.map { SavePathLessonJson(it.id.ifEmpty { null }, it.title.trim(), it.isChapterQuiz, it.stars) },
                )
            },
            classroomIds = curriculum.assignedTo,
        )
        return if (curriculum.id.isEmpty()) {
            call<PathJson>(HttpMethod.Post, "learning-path", body = body).toModel()
        } else {
            call<PathJson>(HttpMethod.Put, "learning-path", curriculum.id, body = body).toModel()
        }
    }

    override suspend fun deleteCurriculum(curriculumId: String) = send(HttpMethod.Delete, "learning-path", curriculumId)

    override suspend fun createChapter(subjectId: String, title: String): ChapterSummary = addChapter(subjectId, title)

    override suspend fun getLessonContent(lessonId: String): LessonContent =
        call<LessonJson>(HttpMethod.Get, "learning-path", "lesson", lessonId).toModel()

    override suspend fun saveLessonContent(lessonId: String, questions: List<AuthoredQuestion>): LessonContent =
        call<LessonJson>(HttpMethod.Put, "learning-path", "lesson", lessonId, "questions", body = QuestionsJson(questions.map { it.toJson() }))
            .toModel()

    override suspend fun getProgress(curriculumId: String): PathProgress {
        val progress = call<PathProgressJson>(HttpMethod.Get, "learning-path", curriculumId, "progress")
        return PathProgress(
            lessonCount = progress.lessonCount,
            chapterCount = progress.chapterCount,
            classrooms = progress.classrooms.map { room ->
                ClassroomProgress(
                    room.classroomId,
                    room.classroomName,
                    room.students.map { StudentProgress(it.id, it.name, it.lessonsDone, it.currentChapter, it.stars) },
                )
            },
        )
    }

    override suspend fun suggestQuestions(lessonId: String): List<AuthoredQuestion> {
        val lesson = getLessonContent(lessonId)
        val chapter = call<ChapterJson>(HttpMethod.Get, "chapter", lesson.chapter.id)
        return questionsFromFlashcards(chapter.flashcards.map { it.toModel() })
    }

    // Requests

    private suspend inline fun <reified T> call(method: HttpMethod, vararg path: String, body: Any? = null): T =
        api.request { build(method, path, body) }.bodyOrThrow()

    private suspend fun send(method: HttpMethod, vararg path: String, body: Any? = null) {
        api.request { build(method, path, body) }.successOrThrow()
    }

    private fun HttpRequestBuilder.build(method: HttpMethod, path: Array<out String>, body: Any?) {
        this.method = method
        url { appendPathSegments(*path) }
        if (body != null) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    // JSON

    @Serializable
    private data class CreateJson(val name: String, val grade: Int)

    @Serializable
    private data class CreatedClassroomJson(@SerialName("_id") val id: String, val name: String, val grade: Int? = null)

    @Serializable
    private data class ClassroomSummaryJson(val id: String, val name: String, val grade: Int? = null, val studentCount: Int, val isOwner: Boolean = true)

    @Serializable
    private data class EmailJson(val email: String)

    @Serializable
    private data class TeacherJson(val id: String, val name: String, val email: String, val isOwner: Boolean)

    @Serializable
    private data class SharedClassJson(val id: String, val name: String, val teacherName: String)

    @Serializable
    private data class PersonJson(val id: String, val name: String)

    @Serializable
    private data class ClassroomDetailJson(
        val id: String,
        val name: String,
        val grade: Int? = null,
        val code: String,
        val approveJoins: Boolean,
        val students: List<PersonJson>,
        val waiting: List<PersonJson>,
        val classIds: List<String>,
        val otherClasses: List<SharedClassJson> = emptyList(),
        val teachers: List<TeacherJson> = emptyList(),
        val isOwner: Boolean = true,
    )

    @Serializable
    private data class ApproveJoinsJson(val approveJoins: Boolean)

    @Serializable
    private data class ClassIdsJson(val classIds: List<String>)

    @Serializable
    private data class TeacherClassJson(
        val id: String,
        val name: String,
        val grade: Int? = null,
        val chapters: List<ChapterSummaryJson>,
        val classroomIds: List<String>,
    ) {
        fun toModel() = TeacherClass(id, name, grade, chapters.map { it.toModel() }, classroomIds)
    }

    @Serializable
    private data class TitleJson(val title: String)

    @Serializable
    private data class ChapterOrderJson(val chapterIds: List<String>)

    @Serializable
    private data class ChapterUpdateJson(val title: String, val notes: List<NoteJson>, val flashcards: List<FlashcardJson>)

    private fun ChapterJson.toContent() = ChapterContent(
        id = id,
        classId = classId,
        number = number,
        title = title,
        notes = notes.map { it.toModel() },
        flashcards = flashcards.map { it.toModel() },
    )

    @Serializable
    private data class StudentProgressJson(val id: String, val name: String, val lessonsDone: Int, val currentChapter: Int, val stars: Int)

    @Serializable
    private data class ClassroomProgressJson(val classroomId: String, val classroomName: String, val students: List<StudentProgressJson>)

    @Serializable
    private data class PathProgressJson(val lessonCount: Int, val chapterCount: Int, val classrooms: List<ClassroomProgressJson>)

    @Serializable
    private data class PathSummaryJson(
        val id: String,
        val name: String,
        val classId: String,
        val grade: Int? = null,
        val chapterCount: Int,
        val lessonCount: Int,
        val classroomIds: List<String>,
    ) {
        fun toModel() = CurriculumSummary(id, name, classId, grade ?: 0, chapterCount, lessonCount, classroomIds)
    }

    @Serializable
    private data class PathLessonJson(val id: String, val title: String, val isChapterQuiz: Boolean, val questionCount: Int, val stars: Int = 10)

    @Serializable
    private data class PathUnitJson(val chapter: ChapterSummaryJson, val lessons: List<PathLessonJson>)

    @Serializable
    private data class PathJson(
        val id: String,
        val name: String,
        val classId: String,
        val grade: Int? = null,
        val unlockMode: String,
        val units: List<PathUnitJson>,
        val classroomIds: List<String>,
    ) {
        fun toModel() = Curriculum(
            id = id,
            name = name,
            subjectId = classId,
            grade = grade ?: 0,
            unlockMode = if (unlockMode == "anyChapter") UnlockMode.AnyChapter else UnlockMode.InOrder,
            units = units.map { unit ->
                CurriculumUnit(unit.chapter.toModel(), unit.lessons.map { CurriculumLesson(it.id, it.title, it.isChapterQuiz, it.questionCount, it.stars) })
            },
            assignedTo = classroomIds,
        )
    }

    private fun UnlockMode.toJson() = when (this) {
        UnlockMode.InOrder -> "inOrder"
        UnlockMode.AnyChapter -> "anyChapter"
    }

    @Serializable
    private data class SavePathLessonJson(val id: String? = null, val title: String, val isChapterQuiz: Boolean, val stars: Int)

    @Serializable
    private data class SavePathUnitJson(val chapterId: String, val lessons: List<SavePathLessonJson>)

    @Serializable
    private data class SavePathJson(
        val name: String,
        val classId: String,
        val unlockMode: String,
        val units: List<SavePathUnitJson>,
        val classroomIds: List<String>,
    )

    @Serializable
    private data class LessonJson(
        val lessonId: String,
        val title: String,
        val isChapterQuiz: Boolean,
        val className: String,
        val chapter: ChapterSummaryJson,
        val questions: List<AuthoredQuestionJson>,
    ) {
        fun toModel() = LessonContent(lessonId, title, isChapterQuiz, className, chapter.toModel(), questions.map { it.toModel() })
    }

    @Serializable
    private data class QuestionsJson(val questions: List<AuthoredQuestionJson>)

    private companion object {
        fun joinLink(code: String) = "https://bookworm.app/join/$code"
    }
}
