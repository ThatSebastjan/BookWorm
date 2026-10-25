package com.dreamteam.bookworm.data.fake

import com.dreamteam.bookworm.domain.model.Answer
import com.dreamteam.bookworm.domain.model.AnswerResult
import com.dreamteam.bookworm.domain.model.AssignmentTarget
import com.dreamteam.bookworm.domain.model.AuthSession
import com.dreamteam.bookworm.domain.model.AuthoredQuestion
import com.dreamteam.bookworm.domain.model.Badge
import com.dreamteam.bookworm.domain.model.BadgeKind
import com.dreamteam.bookworm.domain.model.CardRequest
import com.dreamteam.bookworm.domain.model.Chapter
import com.dreamteam.bookworm.domain.model.ChapterSummary
import com.dreamteam.bookworm.domain.model.ClassGroup
import com.dreamteam.bookworm.domain.model.ClassProgress
import com.dreamteam.bookworm.domain.model.ClassStudent
import com.dreamteam.bookworm.domain.model.ClassSummary
import com.dreamteam.bookworm.domain.model.Classroom
import com.dreamteam.bookworm.domain.model.Curriculum
import com.dreamteam.bookworm.domain.model.CurriculumLesson
import com.dreamteam.bookworm.domain.model.CurriculumLibrary
import com.dreamteam.bookworm.domain.model.CurriculumSummary
import com.dreamteam.bookworm.domain.model.CurriculumUnit
import com.dreamteam.bookworm.domain.model.DayActivity
import com.dreamteam.bookworm.domain.model.DraftCard
import com.dreamteam.bookworm.domain.model.DraftStatus
import com.dreamteam.bookworm.domain.model.FileKind
import com.dreamteam.bookworm.domain.model.Flashcard
import com.dreamteam.bookworm.domain.model.JoinClassException
import com.dreamteam.bookworm.domain.model.JoinFailure
import com.dreamteam.bookworm.domain.model.LearnPath
import com.dreamteam.bookworm.domain.model.Lesson
import com.dreamteam.bookworm.domain.model.LessonContent
import com.dreamteam.bookworm.domain.model.LessonNode
import com.dreamteam.bookworm.domain.model.LessonStatus
import com.dreamteam.bookworm.domain.model.LessonSummary
import com.dreamteam.bookworm.domain.model.LiveQuiz
import com.dreamteam.bookworm.domain.model.MaterialsOverview
import com.dreamteam.bookworm.domain.model.MembershipStatus
import com.dreamteam.bookworm.domain.model.PickedFile
import com.dreamteam.bookworm.domain.model.Question
import com.dreamteam.bookworm.domain.model.QuestionResult
import com.dreamteam.bookworm.domain.model.QuizConfig
import com.dreamteam.bookworm.domain.model.QuizSetup
import com.dreamteam.bookworm.domain.model.ReviewBatch
import com.dreamteam.bookworm.domain.model.StudentClass
import com.dreamteam.bookworm.domain.model.StudentProfile
import com.dreamteam.bookworm.domain.model.StudentStats
import com.dreamteam.bookworm.domain.model.StudentStatus
import com.dreamteam.bookworm.domain.model.Subject
import com.dreamteam.bookworm.domain.model.TeacherProfile
import com.dreamteam.bookworm.domain.model.UnlockMode
import com.dreamteam.bookworm.domain.model.Upload
import com.dreamteam.bookworm.domain.model.UploadStatus
import com.dreamteam.bookworm.domain.model.normalizeClassCode
import com.dreamteam.bookworm.domain.repository.ClassroomRepository
import com.dreamteam.bookworm.domain.repository.CurriculumRepository
import com.dreamteam.bookworm.domain.repository.FlashcardReviewRepository
import com.dreamteam.bookworm.domain.repository.LearnRepository
import com.dreamteam.bookworm.domain.repository.LessonRepository
import com.dreamteam.bookworm.domain.repository.LibraryRepository
import com.dreamteam.bookworm.domain.repository.MaterialsRepository
import com.dreamteam.bookworm.domain.repository.PopQuizRepository
import com.dreamteam.bookworm.domain.repository.ProfileRepository
import com.dreamteam.bookworm.domain.repository.StudentClassesRepository
import com.dreamteam.bookworm.domain.repository.TeacherRepository
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlinx.coroutines.delay

/**
 * In-memory stand-in for the backend, shared by the student and teacher apps.
 * Implements every repository so both run end to end, and keeps state between calls.
 *
 * Content lives once: chapters (with their notes and flashcards) are shared, and
 * learning paths order them and are assigned to classes or groups. The student
 * (Mia, Class 3B, Reading stars group) learns along the paths assigned to her,
 * so whatever a teacher changes in a path or publishes to a chapter shows up in
 * the student app.
 */
class FakeBackend(
    private val latencyMs: Long = 300,
    /** Who is signed in to the student app; null falls back to the sample student Mia. */
    private val currentStudent: () -> AuthSession? = { null },
) : LibraryRepository, LearnRepository, LessonRepository, ProfileRepository, StudentClassesRepository,
    TeacherRepository, MaterialsRepository, FlashcardReviewRepository, ClassroomRepository,
    CurriculumRepository, PopQuizRepository {

    private suspend fun network() = delay(latencyMs)

    private var nextId = 1

    // Shared content

    /** Chapters teachers created, on top of the sample ones. */
    private val createdChapters = mutableListOf<ChapterSummary>()

    /** Cards teachers published, by chapter id, on top of the chapter's own flashcards. */
    private val publishedFlashcards = mutableMapOf<String, MutableList<Flashcard>>()

    private val curricula = FakeTeacherData.curricula.map { seed ->
        Curriculum(
            id = seed.id,
            name = seed.name,
            subjectId = seed.subjectId,
            grade = seed.grade,
            unlockMode = seed.unlockMode,
            units = seed.chapterIds.map { CurriculumUnit(FakeData.chapters.first { c -> c.id == it }, FakeTeacherData.defaultLessons(it)) },
            assignedTo = seed.assignedTo,
        )
    }.toMutableList()

    private class StoredClass(
        val id: String,
        var name: String,
        val grade: Int,
        val joinCode: String,
        var approveJoins: Boolean,
        val students: MutableList<ClassStudent>,
        val groups: MutableList<ClassGroup>,
    )

    /** Questions teachers wrote, by lesson id. Lessons without any serve practice from flashcards. */
    private val lessonContents: MutableMap<String, List<AuthoredQuestion>> =
        (FakeTeacherData.authoredLessons + ("maths-3-comparing" to FakeTeacherData.sampleLessonContent())).toMutableMap()

    private val classes = FakeTeacherData.classes.map {
        StoredClass(it.id, it.name, it.grade, it.joinCode, approveJoins = true, it.students.toMutableList(), it.groups.toMutableList())
    }.toMutableList()

    // Student state

    private var stars = 428
    private val knownFlashcards = mutableSetOf<String>()
    private val lessonStars = mutableMapOf<String, Int>()
    private val lessonCorrect = mutableMapOf<String, Int>()

    /** Answer keys of the lessons as last served, by lesson id then question id. */
    private val servedKeys = mutableMapOf<String, Map<String, FakeData.Key>>()

    /** Lessons the student finished: the sample progress, plus every lesson in chapters before her current one. */
    private val doneLessons: MutableSet<String> = buildSet {
        addAll(FakeData.lessonStatus.filterValues { it == LessonStatus.Done }.keys)
        FakeData.subjects.forEach { subject ->
            val current = FakeData.chapters.firstOrNull { it.id == FakeData.currentChapterBySubject[subject.id] } ?: return@forEach
            curricula.filter { it.subjectId == subject.id }.forEach { path ->
                path.units.filter { it.chapter.number < current.number }.forEach { unit -> addAll(unit.lessons.map { it.id }) }
            }
        }
    }.toMutableSet()

    // LibraryRepository

    /** Only subjects the student has a learning path for. */
    override suspend fun getSubjects(): List<Subject> {
        network()
        return FakeData.subjects.filter { studentPath(it.id) != null }
    }

    /** The chapters of the student's path for [subjectId], in path order. */
    override suspend fun getChapters(subjectId: String): List<ChapterSummary> {
        network()
        return studentPath(subjectId)?.units.orEmpty().map { it.chapter }
    }

    override suspend fun getChapter(chapterId: String): Chapter {
        network()
        val chapter = FakeData.chapterDetails[chapterId] ?: FakeData.placeholderChapter(chapterSummary(chapterId))
        return chapter.copy(flashcards = chapter.flashcards + publishedFlashcards[chapterId].orEmpty())
    }

    override suspend fun recordFlashcard(flashcardId: String, known: Boolean) {
        network()
        if (known) knownFlashcards += flashcardId else knownFlashcards -= flashcardId
    }

    // LearnRepository

    override suspend fun getLearnPath(subjectId: String): LearnPath {
        network()
        val path = studentPath(subjectId) ?: error("No learning path for $subjectId")
        val statuses = lessonStatuses(path)
        val currentIndex = path.units.indexOfFirst { unit -> unit.lessons.any { it.id !in doneLessons } }
            .takeIf { it >= 0 } ?: path.units.lastIndex
        val current = path.units[currentIndex]
        return LearnPath(
            subject = FakeData.subjects.first { it.id == subjectId },
            chapter = current.chapter,
            lessons = current.lessons.map { LessonNode(it.id, it.title, statuses.getValue(it.id), it.isChapterQuiz) },
            nextChapter = path.units.getOrNull(currentIndex + 1)?.chapter,
        )
    }

    override suspend fun getStats(): StudentStats {
        network()
        return StudentStats(streakDays = 6, stars = stars)
    }

    // LessonRepository

    /**
     * The teacher's questions, or practice made from the chapter's flashcards when
     * there are none. Can be empty, which the lesson screen shows as "not ready yet".
     * Answer keys stay here; order and match items are shuffled for the student.
     */
    override suspend fun getLesson(lessonId: String): Lesson {
        network()
        lessonStars[lessonId] = 0
        lessonCorrect[lessonId] = 0
        val (_, unit, lesson) = findLesson(lessonId)
        val questions = lessonContents[lessonId].orEmpty()
            .ifEmpty { FakeTeacherData.questionsFromFlashcards(flashcardsOf(unit.chapter.id)) }
        servedKeys[lessonId] = questions.associate { it.question.id to FakeData.Key(it.answer, it.explanation, it.retryHint) }
        return Lesson(lessonId, lesson.title, questions.map { it.question.shuffledForStudent() })
    }

    override suspend fun submitAnswer(lessonId: String, questionId: String, answer: Answer): AnswerResult {
        network()
        val key = servedKeys[lessonId]?.get(questionId) ?: FakeData.answerKey.getValue(questionId)
        val correct = when (answer) {
            is Answer.Text -> (key.answer as? Answer.Text)?.value?.normalised() == answer.value.normalised()
            else -> key.answer == answer
        }
        val earned = if (correct) 10 else 0
        if (correct) {
            lessonStars[lessonId] = (lessonStars[lessonId] ?: 0) + earned
            lessonCorrect[lessonId] = (lessonCorrect[lessonId] ?: 0) + 1
        }
        return AnswerResult(correct, if (correct) key.explanation else key.retryHint, earned)
    }

    /** Finishing an open lesson marks it done, which opens the next one on the path. */
    override suspend fun completeLesson(lessonId: String): LessonSummary {
        network()
        val earned = lessonStars[lessonId] ?: 0
        stars += earned
        val open = FakeData.subjects.mapNotNull { studentPath(it.id) }.any { lessonStatuses(it)[lessonId] == LessonStatus.Current }
        if (open) doneLessons += lessonId
        return LessonSummary(earned, lessonCorrect[lessonId] ?: 0, servedKeys[lessonId]?.size ?: 0)
    }

    /** Typed answers ignore case and spaces, so "3 / 4" counts as "3/4". */
    private fun String.normalised() = filterNot { it.isWhitespace() }.lowercase()

    /** Authored items are stored in their correct order, so move them around before serving. */
    private fun Question.shuffledForStudent(): Question = when (this) {
        is Question.Order -> copy(items = items.rotated())
        is Question.Match -> copy(right = right.rotated())
        else -> this
    }

    private fun <T> List<T>.rotated(): List<T> = if (size < 2) this else drop(1) + first()

    private fun flashcardsOf(chapterId: String): List<Flashcard> =
        FakeData.chapterDetails[chapterId]?.flashcards.orEmpty() + publishedFlashcards[chapterId].orEmpty()

    private fun findLesson(lessonId: String): Triple<Curriculum, CurriculumUnit, CurriculumLesson> {
        curricula.forEach { path ->
            path.units.forEach { unit ->
                unit.lessons.firstOrNull { it.id == lessonId }?.let { return Triple(path, unit, it) }
            }
        }
        error("No lesson $lessonId")
    }

    // ProfileRepository

    override suspend fun getProfile(): StudentProfile {
        network()
        val studentClass = classes.first { it.id == FakeTeacherData.studentClassId }
        return StudentProfile(
            name = "Mia",
            grade = studentClass.grade,
            level = 7,
            starsToNextLevel = 72,
            levelProgress = 0.86f,
            streakDays = 6,
            stars = stars,
            lessonsDone = 41,
            week = List(7) { DayActivity(practised = it < 5, isToday = it == 4) },
            classes = FakeData.subjects.mapNotNull { subject ->
                val path = studentPath(subject.id) ?: return@mapNotNull null
                val lessons = path.units.flatMap { it.lessons }
                val current = path.units.indexOfFirst { unit -> unit.lessons.any { it.id !in doneLessons } }
                    .takeIf { it >= 0 } ?: path.units.lastIndex
                ClassProgress(
                    subjectId = subject.id,
                    subjectName = subject.name,
                    progress = if (lessons.isEmpty()) 0f else lessons.count { it.id in doneLessons }.toFloat() / lessons.size,
                    currentChapter = current + 1,
                    chapterCount = path.units.size,
                )
            },
            badges = listOf(
                Badge("first", "First lesson", BadgeKind.Star, earned = true),
                Badge("streak5", "5-day streak", BadgeKind.Streak, earned = true),
                Badge("fractions", "Fraction fan", BadgeKind.Fractions, earned = true),
                Badge("quiz", "Quiz champion", BadgeKind.Quiz, earned = false),
            ),
        )
    }

    // Learning paths as the student sees them

    /** The student's path for a subject: her group's path wins over her class's. */
    private fun studentPath(subjectId: String): Curriculum? {
        val studentClass = classes.first { it.id == FakeTeacherData.studentClassId }
        val groupTargets = studentClass.groups.filter { FakeData.studentId in it.studentIds }
            .map { AssignmentTarget.Group(studentClass.id, it.id) }
        val forSubject = curricula.filter { it.subjectId == subjectId }
        val path = forSubject.firstOrNull { path -> path.assignedTo.any { it in groupTargets } }
            ?: forSubject.firstOrNull { AssignmentTarget.WholeClass(studentClass.id) in it.assignedTo }
        return path?.withFreshChapters()
    }

    /** Done lessons stay done; the rest open one at a time, along the whole path or per chapter. */
    private fun lessonStatuses(path: Curriculum): Map<String, LessonStatus> {
        val statuses = mutableMapOf<String, LessonStatus>()
        var opened = false
        path.units.forEach { unit ->
            if (path.unlockMode == UnlockMode.AnyChapter) opened = false
            unit.lessons.forEach { lesson ->
                statuses[lesson.id] = when {
                    lesson.id in doneLessons -> LessonStatus.Done
                    !opened -> LessonStatus.Current.also { opened = true }
                    else -> LessonStatus.Locked
                }
            }
        }
        return statuses
    }

    // Shared helpers

    /** Chapters with published cards counted and the student's known cards applied. */
    private fun chapterSummaries(): List<ChapterSummary> = (FakeData.chapters + createdChapters).map { chapter ->
        val published = publishedFlashcards[chapter.id].orEmpty()
        val ids = FakeData.chapterDetails[chapter.id]?.flashcards.orEmpty().map { it.id } + published.map { it.id }
        chapter.copy(
            flashcardCount = chapter.flashcardCount + published.size,
            knownFlashcards = maxOf(chapter.knownFlashcards, ids.count { it in knownFlashcards }),
        )
    }

    private fun chapterSummary(chapterId: String) = chapterSummaries().first { it.id == chapterId }

    private fun subjectName(subjectId: String) = FakeData.subjects.first { it.id == subjectId }.name

    /** Current chapter data and question counts, as the server would send them. */
    private fun Curriculum.withFreshChapters() = copy(
        units = units.map { unit ->
            unit.copy(
                chapter = chapterSummary(unit.chapter.id),
                lessons = unit.lessons.map { it.copy(questionCount = lessonContents[it.id]?.size ?: 0) },
            )
        },
    )

    private fun Curriculum.toSummary() = CurriculumSummary(
        id = id,
        name = name,
        subjectId = subjectId,
        grade = grade,
        chapterCount = units.size,
        lessonCount = units.sumOf { it.lessons.size },
        assignedTo = assignedTo,
    )

    private fun StoredClass.toSummary() = ClassSummary(id, name, grade, students.size, groups.toList())

    private fun storedClass(classId: String) = classes.first { it.id == classId }

    // Teacher state

    private class StoredUpload(
        val id: String,
        var fileName: String,
        val chapterId: String,
        var status: UploadStatus,
        var cards: List<DraftCard>,
    )

    private class RunningQuiz(
        val id: String,
        val config: QuizConfig,
        val className: String,
        val studentCount: Int,
        var totalSeconds: Int,
        var banked: Duration = Duration.ZERO,
        var runningSince: TimeSource.Monotonic.ValueTimeMark? = TimeSource.Monotonic.markNow(),
        var finished: Boolean = false,
    ) {
        fun elapsedSeconds(): Int {
            val running = runningSince?.elapsedNow() ?: Duration.ZERO
            return (banked + running).inWholeSeconds.toInt().coerceAtMost(totalSeconds)
        }

        fun pause() {
            runningSince?.let { banked += it.elapsedNow() }
            runningSince = null
        }
    }

    private val quizzes = mutableMapOf<String, RunningQuiz>()

    private val uploads = mutableListOf(
        StoredUpload("u-worksheet", "Fractions worksheet.pdf", "maths-3",
            UploadStatus.Ready(FakeTeacherData.worksheetCards.size), FakeTeacherData.worksheetCards),
        StoredUpload("u-slides", "Chapter 3 slides.pptx", "maths-3",
            UploadStatus.Processing(0.6f), FakeTeacherData.slidesCards),
        StoredUpload("u-photo", "Photo of blackboard.jpg", "maths-3", UploadStatus.Unreadable, emptyList()),
    )

    // TeacherRepository

    override suspend fun getTeacher(): TeacherProfile {
        network()
        val toReview = uploads.filter { it.status is UploadStatus.Ready }
            .sumOf { upload -> upload.cards.count { it.status == DraftStatus.Pending } }
        return TeacherProfile(FakeData.teacherName, classes.map { it.toSummary() }, toReview)
    }

    override suspend fun createClass(name: String, grade: Int): ClassSummary {
        network()
        val code = "N${nextId}C${(nextId * 7) % 10}${(nextId * 3) % 10}".take(5)
        val created = StoredClass("class-${nextId++}", name, grade, code, approveJoins = true, mutableListOf(), mutableListOf())
        classes += created
        return created.toSummary()
    }

    // MaterialsRepository

    override suspend fun getMaterials(): MaterialsOverview {
        network()
        advanceProcessing()
        return MaterialsOverview(
            subjects = FakeData.subjects,
            chapters = chapterSummaries(),
            readingGrades = FakeTeacherData.readingGrades,
            defaultGrade = FakeTeacherData.defaultGrade,
            uploads = uploads.map { it.toUpload() },
        )
    }

    override suspend fun upload(file: PickedFile, request: CardRequest): Upload {
        network()
        val id = "u-${nextId++}"
        val stored = StoredUpload(
            id = id,
            fileName = file.name,
            chapterId = request.chapterId,
            status = UploadStatus.Processing(0f),
            cards = FakeTeacherData.generatedCards(id, chapterSummary(request.chapterId).title, file.name),
        )
        uploads.add(0, stored)
        return stored.toUpload()
    }

    override suspend fun replaceUpload(uploadId: String, file: PickedFile): Upload {
        network()
        val stored = uploads.first { it.id == uploadId }
        stored.fileName = file.name
        stored.status = UploadStatus.Processing(0f)
        stored.cards = FakeTeacherData.generatedCards(uploadId, chapterSummary(stored.chapterId).title, file.name)
        return stored.toUpload()
    }

    /** Each poll moves card writing along, so uploads finish after a few seconds. */
    private fun advanceProcessing() {
        uploads.forEach { upload ->
            val status = upload.status as? UploadStatus.Processing ?: return@forEach
            val next = status.progress + 0.1f
            upload.status = if (next >= 1f) UploadStatus.Ready(upload.cards.size) else UploadStatus.Processing(next)
        }
    }

    // FlashcardReviewRepository

    override suspend fun getReview(uploadId: String?): ReviewBatch? {
        network()
        val ready = uploads.filter { it.status is UploadStatus.Ready }
        val upload = if (uploadId != null) {
            ready.firstOrNull { it.id == uploadId }
        } else {
            ready.firstOrNull { upload -> upload.cards.any { it.status == DraftStatus.Pending } } ?: ready.firstOrNull()
        }
        return upload?.toBatch()
    }

    override suspend fun updateCard(uploadId: String, card: DraftCard): DraftCard {
        network()
        val upload = uploads.first { it.id == uploadId }
        upload.cards = upload.cards.map { if (it.id == card.id) card.copy(published = it.published) else it }
        return card
    }

    override suspend fun approveClearCards(uploadId: String): ReviewBatch {
        network()
        val upload = uploads.first { it.id == uploadId }
        upload.cards = upload.cards.map {
            if (it.status == DraftStatus.Pending && it.issue == null) it.copy(status = DraftStatus.Approved) else it
        }
        return upload.toBatch()
    }

    /** Approved cards become flashcards in the shared chapter, so every path that includes it gets them. */
    override suspend fun publish(uploadId: String): ReviewBatch {
        network()
        val upload = uploads.first { it.id == uploadId }
        val fresh = upload.cards.filter { it.status == DraftStatus.Approved && !it.published }
        publishedFlashcards.getOrPut(upload.chapterId) { mutableListOf() } += fresh.map { it.flashcard }
        upload.cards = upload.cards.map { if (it in fresh) it.copy(published = true) else it }
        return upload.toBatch()
    }

    // StudentClassesRepository

    private fun studentIdentity(): Pair<String, String> =
        currentStudent()?.let { it.userId to it.name } ?: (FakeData.studentId to "Mia")

    override suspend fun getMyClasses(): List<StudentClass> {
        network()
        val (studentId, _) = studentIdentity()
        return classes.mapNotNull { stored ->
            stored.students.firstOrNull { it.id == studentId }?.let { stored.toStudentClass(it.status) }
        }
    }

    override suspend fun joinClass(code: String): StudentClass {
        network()
        val (studentId, name) = studentIdentity()
        return addStudentToClass(studentId, name, code)
    }

    /** True if a class uses this join code. Lets the fake sign-up check codes without a network round trip. */
    internal fun hasJoinCode(code: String): Boolean = classes.any { it.joinCode == normalizeClassCode(code) }

    /** Adds the student to the class; they wait for the teacher if the class approves new students. */
    internal fun addStudentToClass(studentId: String, name: String, code: String): StudentClass {
        val stored = classes.firstOrNull { it.joinCode == normalizeClassCode(code) }
            ?: throw JoinClassException(JoinFailure.CodeNotFound)
        val existing = stored.students.firstOrNull { it.id == studentId }
        return when {
            existing == null -> {
                val status = if (stored.approveJoins) StudentStatus.Waiting else StudentStatus.InClass
                stored.students += ClassStudent(studentId, name, status)
                stored.toStudentClass(status)
            }
            // Asking again while waiting just shows the same request.
            existing.status == StudentStatus.Waiting -> stored.toStudentClass(existing.status)
            else -> throw JoinClassException(JoinFailure.AlreadyInClass)
        }
    }

    private fun StoredClass.toStudentClass(status: StudentStatus) = StudentClass(
        id = id,
        name = name,
        grade = grade,
        teacherName = FakeData.teacherName,
        status = if (status == StudentStatus.Waiting) MembershipStatus.WaitingForTeacher else MembershipStatus.Joined,
    )

    // ClassroomRepository

    override suspend fun getClassroom(classId: String): Classroom {
        network()
        val stored = storedClass(classId)
        return Classroom(
            id = stored.id,
            name = stored.name,
            grade = stored.grade,
            joinCode = stored.joinCode,
            joinLink = FakeTeacherData.joinLink(stored.joinCode),
            approveJoins = stored.approveJoins,
            students = stored.students.toList(),
            groups = stored.groups.toList(),
            learningPaths = curricula.filter { path -> path.assignedTo.any { it.classId == classId } }.map { it.toSummary() },
        )
    }

    override suspend fun admit(classId: String, studentId: String): ClassStudent {
        network()
        val students = storedClass(classId).students
        val index = students.indexOfFirst { it.id == studentId }
        val admitted = students[index].copy(status = StudentStatus.InClass)
        students[index] = admitted
        return admitted
    }

    override suspend fun setApproveJoins(classId: String, approve: Boolean) {
        network()
        storedClass(classId).approveJoins = approve
    }

    override suspend fun saveGroup(classId: String, group: ClassGroup): ClassGroup {
        network()
        val stored = storedClass(classId)
        val members = stored.students.map { it.id }.toSet()
        val saved = group.copy(
            id = group.id.ifEmpty { "g-${nextId++}" },
            studentIds = group.studentIds.filter { it in members }.distinct(),
        )
        val index = stored.groups.indexOfFirst { it.id == saved.id }
        if (index >= 0) stored.groups[index] = saved else stored.groups += saved
        return saved
    }

    override suspend fun deleteGroup(classId: String, groupId: String) {
        network()
        storedClass(classId).groups.removeAll { it.id == groupId }
        val target = AssignmentTarget.Group(classId, groupId)
        curricula.updateAll { it.copy(assignedTo = it.assignedTo - target) }
    }

    // CurriculumRepository

    override suspend fun getLibrary(): CurriculumLibrary {
        network()
        return CurriculumLibrary(
            curricula = curricula.map { it.toSummary() },
            subjects = FakeData.subjects,
            chapters = chapterSummaries(),
            classes = classes.map { it.toSummary() },
        )
    }

    override suspend fun getCurriculum(curriculumId: String): Curriculum {
        network()
        return curricula.first { it.id == curriculumId }.withFreshChapters()
    }

    override suspend fun saveCurriculum(curriculum: Curriculum): Curriculum {
        network()
        val saved = curriculum.copy(
            id = curriculum.id.ifEmpty { "cur-${nextId++}" },
            units = curriculum.units.distinctBy { it.chapter.id }.map { unit ->
                unit.copy(lessons = unit.lessons.map { it.copy(id = it.id.ifEmpty { "les-${nextId++}" }) })
            },
            assignedTo = curriculum.assignedTo.distinct(),
        )
        // A class or group follows one path per subject: take these targets off the subject's other paths.
        curricula.updateAll { other ->
            if (other.id != saved.id && other.subjectId == saved.subjectId) other.copy(assignedTo = other.assignedTo - saved.assignedTo.toSet()) else other
        }
        val index = curricula.indexOfFirst { it.id == saved.id }
        if (index >= 0) curricula[index] = saved else curricula += saved
        return saved.withFreshChapters()
    }

    override suspend fun deleteCurriculum(curriculumId: String) {
        network()
        curricula.removeAll { it.id == curriculumId }
    }

    override suspend fun createChapter(subjectId: String, title: String): ChapterSummary {
        network()
        val number = (chapterSummaries().filter { it.subjectId == subjectId }.maxOfOrNull { it.number } ?: 0) + 1
        val chapter = ChapterSummary("$subjectId-${nextId++}", subjectId, number, title.trim(), noteCount = 0, flashcardCount = 0, knownFlashcards = 0)
        createdChapters += chapter
        return chapter
    }

    override suspend fun getLessonContent(lessonId: String): LessonContent {
        network()
        return lessonContent(lessonId)
    }

    override suspend fun saveLessonContent(lessonId: String, questions: List<AuthoredQuestion>): LessonContent {
        network()
        findLesson(lessonId)
        if (questions.isEmpty()) lessonContents -= lessonId else lessonContents[lessonId] = questions
        return lessonContent(lessonId)
    }

    override suspend fun suggestQuestions(lessonId: String): List<AuthoredQuestion> {
        network()
        val (_, unit, _) = findLesson(lessonId)
        return FakeTeacherData.questionsFromFlashcards(flashcardsOf(unit.chapter.id))
    }

    private fun lessonContent(lessonId: String): LessonContent {
        val (path, unit, lesson) = findLesson(lessonId)
        return LessonContent(
            lessonId = lessonId,
            title = lesson.title,
            isChapterQuiz = lesson.isChapterQuiz,
            subjectName = subjectName(path.subjectId),
            chapter = chapterSummary(unit.chapter.id),
            questions = lessonContents[lessonId].orEmpty(),
        )
    }

    // PopQuizRepository

    /** Chapters from the paths of the class and its groups that students have cards for. */
    override suspend fun getQuizSetup(classId: String): QuizSetup {
        network()
        val stored = storedClass(classId)
        val chapters = curricula.filter { path -> path.assignedTo.any { it.classId == classId } }
            .sortedBy { path -> FakeData.subjects.indexOfFirst { it.id == path.subjectId } }
            .flatMap { it.withFreshChapters().units }
            .map { it.chapter }
            .distinctBy { it.id }
            .filter { it.flashcardCount > 0 }
        return QuizSetup(
            className = stored.name,
            studentsInClass = studentsInClass(classId),
            subjects = FakeData.subjects,
            chapters = chapters,
            questionCounts = listOf(5, 8, 10),
            minuteOptions = listOf(3, 5, 8, 10),
        )
    }

    override suspend fun startQuiz(config: QuizConfig): LiveQuiz {
        network()
        val stored = storedClass(config.classId)
        val quiz = RunningQuiz("q-${nextId++}", config, stored.name, studentsInClass(config.classId), config.minutes * 60)
        quizzes[quiz.id] = quiz
        return quiz.toLive()
    }

    override suspend fun getLiveQuiz(quizId: String): LiveQuiz {
        network()
        return quizzes.getValue(quizId).toLive()
    }

    override suspend fun addMinute(quizId: String): LiveQuiz {
        network()
        val quiz = quizzes.getValue(quizId)
        quiz.totalSeconds += 60
        return quiz.toLive()
    }

    override suspend fun setPaused(quizId: String, paused: Boolean): LiveQuiz {
        network()
        val quiz = quizzes.getValue(quizId)
        if (paused) quiz.pause() else if (quiz.runningSince == null) quiz.runningSince = TimeSource.Monotonic.markNow()
        return quiz.toLive()
    }

    override suspend fun finishQuiz(quizId: String) {
        network()
        val quiz = quizzes.getValue(quizId)
        quiz.pause()
        quiz.finished = true
    }

    override suspend fun getResult(quizId: String, number: Int): QuestionResult {
        network()
        val quiz = quizzes.getValue(quizId)
        val bank = FakeTeacherData.quizQuestions
        val entry = bank[(number - 1).mod(bank.size)]
        val options = entry.question.options
        val students = quiz.studentCount.coerceAtLeast(1)
        val right = (students * entry.correctShare).roundToInt().coerceIn(0, students)
        // Spread the wrong answers over the other options, one at a time.
        val counts = options.associate { it.id to 0 }.toMutableMap()
        counts[entry.correctOptionId] = right
        val wrong = options.map { it.id }.filter { it != entry.correctOptionId }
        repeat(students - right) { counts[wrong[it % wrong.size]] = counts.getValue(wrong[it % wrong.size]) + 1 }
        return QuestionResult(
            quizId = quizId,
            className = quiz.className,
            chapter = chapterSummary(quiz.config.chapterId),
            number = number,
            questionCount = quiz.config.questionCount,
            question = entry.question,
            correctOptionId = entry.correctOptionId,
            answerCounts = counts,
            explanation = entry.explanation,
            fraction = entry.fraction,
        )
    }

    private fun studentsInClass(classId: String) = storedClass(classId).students.count { it.status == StudentStatus.InClass }

    private fun StoredUpload.toUpload() = Upload(id, fileName, kindOf(fileName), status)

    private fun StoredUpload.toBatch(): ReviewBatch {
        val chapter = chapterSummary(chapterId)
        return ReviewBatch(id, fileName, subjectName(chapter.subjectId), chapter, cards)
    }

    private fun RunningQuiz.toLive(): LiveQuiz {
        val elapsed = elapsedSeconds()
        return LiveQuiz(
            id = id,
            className = className,
            chapter = chapterSummary(config.chapterId),
            questionCount = config.questionCount,
            calmMode = config.calmMode,
            chime = config.chime,
            totalSeconds = totalSeconds,
            elapsedSeconds = elapsed,
            paused = runningSince == null,
            studentCount = studentCount,
            // Roughly one student finishes every 40 seconds.
            studentsFinished = if (finished) studentCount else (elapsed / 40).coerceAtMost(studentCount),
        )
    }

    private fun kindOf(fileName: String): FileKind = when (fileName.substringAfterLast('.', "").lowercase()) {
        "pdf" -> FileKind.Pdf
        "doc", "docx", "odt", "rtf" -> FileKind.Document
        "ppt", "pptx", "odp", "key" -> FileKind.Slides
        "jpg", "jpeg", "png", "heic", "webp" -> FileKind.Photo
        else -> FileKind.Text
    }
}

/** In-place map. `MutableList.replaceAll` is Java-only and experimental on iOS, so common code uses this. */
private inline fun <T> MutableList<T>.updateAll(transform: (T) -> T) {
    for (i in indices) this[i] = transform(this[i])
}
