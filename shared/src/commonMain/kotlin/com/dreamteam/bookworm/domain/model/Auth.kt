package com.dreamteam.bookworm.domain.model

/** Students use the phone app, teachers the desktop app. */
enum class UserRole { Student, Teacher }

/** The signed-in user. [token] is sent as `Authorization: Bearer <token>` on API calls. */
data class AuthSession(
    val userId: String,
    val name: String,
    val surname: String,
    val email: String,
    val role: UserRole,
    val token: String,
    /** Single use: exchanged at `/auth/refresh` for a new pair when [token] expires. */
    val refreshToken: String,
)

data class Registration(
    val name: String,
    val surname: String,
    val email: String,
    val password: String,
)

/** Failures the sign-in screens explain to the student. Anything else counts as a network error. */
enum class AuthFailure {
    InvalidCredentials,
    EmailTaken,
    /** The server rejected a field (e.g. a password it finds too weak). */
    InvalidDetails,
    /** Right password, but the account is for the other app (a student signing in to the teacher app). */
    WrongRole,
}

class AuthException(val failure: AuthFailure) : Exception(failure.name)
