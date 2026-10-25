package com.dreamteam.bookworm.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.dreamteam.bookworm.data.fake.FakeAuthRepository
import com.dreamteam.bookworm.data.fake.FakeBackend
import com.dreamteam.bookworm.domain.repository.AuthRepository
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

/**
 * The single place that decides which repository implementations the app uses.
 * Replace the fake backend here with real implementations when the API is ready.
 */
class AppContainer(
    val authRepository: AuthRepository,
    val libraryRepository: LibraryRepository,
    val learnRepository: LearnRepository,
    val lessonRepository: LessonRepository,
    val profileRepository: ProfileRepository,
    val studentClassesRepository: StudentClassesRepository,
    // Teacher desktop app
    val teacherRepository: TeacherRepository,
    val materialsRepository: MaterialsRepository,
    val flashcardReviewRepository: FlashcardReviewRepository,
    val classroomRepository: ClassroomRepository,
    val curriculumRepository: CurriculumRepository,
    val popQuizRepository: PopQuizRepository,
) {
    companion object {
        fun fake(): AppContainer {
            lateinit var backend: FakeBackend
            val auth = FakeAuthRepository(
                isValidClassCode = { backend.hasJoinCode(it) },
                onRegistered = { session, code -> backend.addStudentToClass(session.userId, session.name, code) },
            )
            backend = FakeBackend(currentStudent = { auth.session.value })
            return AppContainer(
                authRepository = auth,
                libraryRepository = backend,
                learnRepository = backend,
                lessonRepository = backend,
                profileRepository = backend,
                studentClassesRepository = backend,
                teacherRepository = backend,
                materialsRepository = backend,
                flashcardReviewRepository = backend,
                classroomRepository = backend,
                curriculumRepository = backend,
                popQuizRepository = backend,
            )
        }
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("No AppContainer provided. Wrap the UI in App() or TeacherApp(), which provide one.")
}
