package com.dreamteam.bookworm.domain.repository

import com.dreamteam.bookworm.domain.model.AuthoredQuestion
import com.dreamteam.bookworm.domain.model.ChapterContent
import com.dreamteam.bookworm.domain.model.ChapterSummary
import com.dreamteam.bookworm.domain.model.ClassSummary
import com.dreamteam.bookworm.domain.model.Classroom
import com.dreamteam.bookworm.domain.model.Curriculum
import com.dreamteam.bookworm.domain.model.CurriculumLibrary
import com.dreamteam.bookworm.domain.model.LessonContent
import com.dreamteam.bookworm.domain.model.PathProgress
import com.dreamteam.bookworm.domain.model.TeacherClass
import com.dreamteam.bookworm.domain.model.TeacherProfile

/*
 * What the teacher desktop app talks to. Same rules as the student repositories:
 * every call is suspend and may throw, and AppContainer picks the implementation.
 * "Class" means teaching content and "classroom" a group of students (see Teacher.kt).
 */

interface TeacherRepository {
    suspend fun getTeacher(): TeacherProfile

    /** Creates a classroom with a fresh join code. */
    suspend fun createClassroom(name: String, grade: Int): ClassSummary
}

/** Classes and their chapters: notes and flashcards the teacher writes by hand. */
interface ClassesRepository {
    suspend fun getClasses(): List<TeacherClass>
    suspend fun createClass(name: String, grade: Int): TeacherClass
    suspend fun updateClass(classId: String, name: String, grade: Int)

    /** Also deletes its chapters, learning paths and students' progress. */
    suspend fun deleteClass(classId: String)

    /** Adds an empty chapter at the end. */
    suspend fun addChapter(classId: String, title: String): ChapterSummary

    /** [chapterIds] lists every chapter of the class in the new order. Returns them renumbered. */
    suspend fun reorderChapters(classId: String, chapterIds: List<String>): List<ChapterSummary>

    suspend fun getChapter(chapterId: String): ChapterContent

    /** Replaces title, notes and flashcards. Cards keep their id (and students' progress); new ones get one. */
    suspend fun saveChapter(chapter: ChapterContent): ChapterContent

    /** The chapters after it move up a number. */
    suspend fun deleteChapter(chapterId: String)
}

interface ClassroomRepository {
    suspend fun getClassroom(classId: String): Classroom

    /** Lets a waiting student in. */
    suspend fun admit(classId: String, studentId: String): Classroom

    /** Removes a student, or turns down one who is waiting. */
    suspend fun removeStudent(classId: String, studentId: String): Classroom

    suspend fun setApproveJoins(classId: String, approve: Boolean): Classroom

    /** Which of the teacher's own classes the classroom uses. Taking one off also drops its learning path. */
    suspend fun setClasses(classId: String, classIds: Set<String>): Classroom

    /**
     * Owner only: adds the teacher with that account email as a co-teacher.
     * Throws [com.dreamteam.bookworm.domain.model.AddTeacherException] for an unknown email or a teacher already there.
     */
    suspend fun addTeacher(classId: String, email: String): Classroom

    /**
     * The owner removes a co-teacher, or a co-teacher passes their own id to leave.
     * Their classes come off the classroom. Returns null after leaving.
     */
    suspend fun removeTeacher(classId: String, teacherId: String): Classroom?
}

interface CurriculumRepository {
    suspend fun getLibrary(): CurriculumLibrary
    suspend fun getCurriculum(curriculumId: String): Curriculum

    /**
     * Creates the path when its id is empty, otherwise replaces it: name, unlock
     * mode, chapters, lessons and assignments. Lessons with an empty id get one.
     * Assigning it to a classroom replaces that classroom's other path for the class.
     */
    suspend fun saveCurriculum(curriculum: Curriculum): Curriculum

    suspend fun deleteCurriculum(curriculumId: String)

    /** Adds a new, empty chapter at the end of class [subjectId]. */
    suspend fun createChapter(subjectId: String, title: String): ChapterSummary

    /** The lesson must be on a saved path. */
    suspend fun getLessonContent(lessonId: String): LessonContent

    /** Replaces all of the lesson's questions. An empty list goes back to practice made from flashcards. */
    suspend fun saveLessonContent(lessonId: String, questions: List<AuthoredQuestion>): LessonContent

    /** How far each student of the path's classrooms has got. */
    suspend fun getProgress(curriculumId: String): PathProgress

    /** Suggested questions made from the chapter's flashcards, for the teacher to edit. Nothing is saved. */
    suspend fun suggestQuestions(lessonId: String): List<AuthoredQuestion>
}

