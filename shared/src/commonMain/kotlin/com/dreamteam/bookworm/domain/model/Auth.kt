package com.dreamteam.bookworm.domain.model

/** The signed-in student. [token] is what a real backend would send with each request. */
data class AuthSession(
    val userId: String,
    val name: String,
    val email: String,
    val token: String,
)

data class Registration(
    val name: String,
    val email: String,
    val password: String,
    /** Code the teacher hands out to join their class. */
    val classCode: String,
)

/** Failures the sign-in screens explain to the student. Anything else counts as a network error. */
enum class AuthFailure { InvalidCredentials, EmailTaken, InvalidClassCode }

class AuthException(val failure: AuthFailure) : Exception(failure.name)
