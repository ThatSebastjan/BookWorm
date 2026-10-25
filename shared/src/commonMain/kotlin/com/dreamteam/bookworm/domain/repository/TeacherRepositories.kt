package com.dreamteam.bookworm.domain.repository

import com.dreamteam.bookworm.domain.model.AuthoredQuestion
import com.dreamteam.bookworm.domain.model.CardRequest
import com.dreamteam.bookworm.domain.model.ChapterSummary
import com.dreamteam.bookworm.domain.model.ClassGroup
import com.dreamteam.bookworm.domain.model.ClassStudent
import com.dreamteam.bookworm.domain.model.ClassSummary
import com.dreamteam.bookworm.domain.model.Classroom
import com.dreamteam.bookworm.domain.model.Curriculum
import com.dreamteam.bookworm.domain.model.CurriculumLibrary
import com.dreamteam.bookworm.domain.model.DraftCard
import com.dreamteam.bookworm.domain.model.LessonContent
import com.dreamteam.bookworm.domain.model.LiveQuiz
import com.dreamteam.bookworm.domain.model.MaterialsOverview
import com.dreamteam.bookworm.domain.model.PickedFile
import com.dreamteam.bookworm.domain.model.QuestionResult
import com.dreamteam.bookworm.domain.model.QuizConfig
import com.dreamteam.bookworm.domain.model.QuizSetup
import com.dreamteam.bookworm.domain.model.ReviewBatch
import com.dreamteam.bookworm.domain.model.TeacherProfile
import com.dreamteam.bookworm.domain.model.Upload

/*
 * What the teacher desktop app talks to. Same rules as the student repositories:
 * every call is suspend and may throw, and AppContainer picks the implementation.
 */

interface TeacherRepository {
    suspend fun getTeacher(): TeacherProfile
    suspend fun createClass(name: String, grade: Int): ClassSummary
}

interface MaterialsRepository {
    suspend fun getMaterials(): MaterialsOverview

    /** Uploads [file] and starts writing cards for it. */
    suspend fun upload(file: PickedFile, request: CardRequest): Upload

    /** Swaps an unreadable upload for a better copy, keeping where its cards go. */
    suspend fun replaceUpload(uploadId: String, file: PickedFile): Upload
}

interface FlashcardReviewRepository {
    /** The cards made from [uploadId], or from the latest upload that still has cards to review when null. */
    suspend fun getReview(uploadId: String?): ReviewBatch?

    /** Saves the teacher's edits and decision for one card. */
    suspend fun updateCard(uploadId: String, card: DraftCard): DraftCard

    /** Approves every pending card without an issue. Returns the updated batch. */
    suspend fun approveClearCards(uploadId: String): ReviewBatch

    /** Makes the approved cards visible to students. Returns the updated batch. */
    suspend fun publish(uploadId: String): ReviewBatch
}

interface ClassroomRepository {
    suspend fun getClassroom(classId: String): Classroom
    suspend fun admit(classId: String, studentId: String): ClassStudent
    suspend fun setApproveJoins(classId: String, approve: Boolean)

    /** Creates the group when its id is empty, otherwise renames it and replaces its members. */
    suspend fun saveGroup(classId: String, group: ClassGroup): ClassGroup

    /** Also drops the group's learning path assignments. */
    suspend fun deleteGroup(classId: String, groupId: String)
}

interface CurriculumRepository {
    suspend fun getLibrary(): CurriculumLibrary
    suspend fun getCurriculum(curriculumId: String): Curriculum

    /**
     * Creates the path when its id is empty, otherwise replaces it: name, unlock
     * mode, chapters, lessons and assignments. Lessons with an empty id get one.
     * Assigning it to a class or group replaces that target's other path for the subject.
     */
    suspend fun saveCurriculum(curriculum: Curriculum): Curriculum

    suspend fun deleteCurriculum(curriculumId: String)

    /** Adds a new, empty shared chapter at the end of [subjectId]. */
    suspend fun createChapter(subjectId: String, title: String): ChapterSummary

    /** The lesson must be on a saved path. */
    suspend fun getLessonContent(lessonId: String): LessonContent

    /** Replaces all of the lesson's questions. An empty list goes back to practice made from flashcards. */
    suspend fun saveLessonContent(lessonId: String, questions: List<AuthoredQuestion>): LessonContent

    /** Suggested questions made from the chapter's flashcards, for the teacher to edit. Nothing is saved. */
    suspend fun suggestQuestions(lessonId: String): List<AuthoredQuestion>
}

interface PopQuizRepository {
    suspend fun getQuizSetup(classId: String): QuizSetup
    suspend fun startQuiz(config: QuizConfig): LiveQuiz
    suspend fun getLiveQuiz(quizId: String): LiveQuiz
    suspend fun addMinute(quizId: String): LiveQuiz
    suspend fun setPaused(quizId: String, paused: Boolean): LiveQuiz

    /** Stops the countdown so answers can be revealed. */
    suspend fun finishQuiz(quizId: String)

    /** [number] is 1-based. */
    suspend fun getResult(quizId: String, number: Int): QuestionResult
}
