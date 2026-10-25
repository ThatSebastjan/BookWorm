package com.dreamteam.bookworm.domain.repository

import com.dreamteam.bookworm.domain.model.Answer
import com.dreamteam.bookworm.domain.model.AnswerResult
import com.dreamteam.bookworm.domain.model.Chapter
import com.dreamteam.bookworm.domain.model.ChapterSummary
import com.dreamteam.bookworm.domain.model.LearnPath
import com.dreamteam.bookworm.domain.model.Lesson
import com.dreamteam.bookworm.domain.model.LessonSummary
import com.dreamteam.bookworm.domain.model.StudentClass
import com.dreamteam.bookworm.domain.model.StudentProfile
import com.dreamteam.bookworm.domain.model.StudentStats
import com.dreamteam.bookworm.domain.model.Subject

/*
 * The app only talks to these interfaces. Swap the fake implementations in
 * AppContainer for real ones (HTTP client, local cache) when the backend is ready.
 * Every call is suspend and may throw; ViewModels turn failures into error states.
 */

interface LibraryRepository {
    suspend fun getSubjects(): List<Subject>
    suspend fun getChapters(subjectId: String): List<ChapterSummary>
    suspend fun getChapter(chapterId: String): Chapter
    suspend fun recordFlashcard(flashcardId: String, known: Boolean)
}

interface LearnRepository {
    suspend fun getLearnPath(subjectId: String): LearnPath
    suspend fun getStats(): StudentStats
}

interface LessonRepository {
    suspend fun getLesson(lessonId: String): Lesson
    suspend fun submitAnswer(lessonId: String, questionId: String, answer: Answer): AnswerResult
    suspend fun completeLesson(lessonId: String): LessonSummary
}

interface ProfileRepository {
    suspend fun getProfile(): StudentProfile
}

/** The student's classes. Teachers hand out a join code (or link) for each class. */
interface StudentClassesRepository {
    suspend fun getMyClasses(): List<StudentClass>

    /**
     * Joins the class with [code] (already normalised with normalizeClassCode).
     * Returns the membership, which may still be waiting for the teacher.
     * Throws [com.dreamteam.bookworm.domain.model.JoinClassException] for an unknown code or a class the student is already in.
     */
    suspend fun joinClass(code: String): StudentClass
}
