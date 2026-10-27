package com.dreamteam.bookworm.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.dreamteam.bookworm.data.remote.AuthorizedApi
import com.dreamteam.bookworm.data.remote.InMemorySessionStore
import com.dreamteam.bookworm.data.remote.RemoteAuthRepository
import com.dreamteam.bookworm.data.remote.RemoteClassesRepository
import com.dreamteam.bookworm.data.remote.RemoteLearnRepository
import com.dreamteam.bookworm.data.remote.RemoteTeacherRepository
import com.dreamteam.bookworm.data.remote.SessionStore
import com.dreamteam.bookworm.data.remote.defaultApiBaseUrl
import com.dreamteam.bookworm.data.remote.defaultHttpClient
import com.dreamteam.bookworm.domain.model.UserRole
import com.dreamteam.bookworm.domain.repository.AuthRepository
import com.dreamteam.bookworm.domain.repository.ClassesRepository
import com.dreamteam.bookworm.domain.repository.ClassroomRepository
import com.dreamteam.bookworm.domain.repository.CurriculumRepository
import com.dreamteam.bookworm.domain.repository.LearnRepository
import com.dreamteam.bookworm.domain.repository.LessonRepository
import com.dreamteam.bookworm.domain.repository.LibraryRepository
import com.dreamteam.bookworm.domain.repository.ProfileRepository
import com.dreamteam.bookworm.domain.repository.StudentClassesRepository
import com.dreamteam.bookworm.domain.repository.TeacherRepository

/**
 * The single place that decides which repository implementations the app uses.
 * Tests can pass their own implementations to the constructor.
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
    val classesRepository: ClassesRepository,
    val classroomRepository: ClassroomRepository,
    val curriculumRepository: CurriculumRepository,
) {
    companion object {
        /**
         * Every repository talks to the backend at [apiBaseUrl].
         * [sessionStore] keeps the user signed in between launches; the default forgets on exit.
         * [requiredRole] limits who can sign in: the teacher app passes [UserRole.Teacher].
         */
        fun create(
            apiBaseUrl: String = defaultApiBaseUrl,
            sessionStore: SessionStore = InMemorySessionStore(),
            requiredRole: UserRole? = null,
        ): AppContainer {
            val client = defaultHttpClient(apiBaseUrl)
            val auth = RemoteAuthRepository(apiBaseUrl, sessionStore, client, requiredRole)
            val api = AuthorizedApi(client, auth)
            val classes = RemoteClassesRepository(api)
            val teacher = RemoteTeacherRepository(api, teacherName = { auth.session.value?.let { "${it.name} ${it.surname}" }.orEmpty() })
            val learn = RemoteLearnRepository(api)
            return AppContainer(
                authRepository = auth,
                libraryRepository = classes,
                learnRepository = learn,
                lessonRepository = learn,
                profileRepository = learn,
                studentClassesRepository = classes,
                teacherRepository = teacher,
                classesRepository = teacher,
                classroomRepository = teacher,
                curriculumRepository = teacher,
            )
        }
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("No AppContainer provided. Wrap the UI in App() or TeacherApp(), which provide one.")
}
