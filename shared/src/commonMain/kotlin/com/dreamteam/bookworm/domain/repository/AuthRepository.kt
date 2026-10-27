package com.dreamteam.bookworm.domain.repository

import com.dreamteam.bookworm.domain.model.AuthSession
import com.dreamteam.bookworm.domain.model.Registration
import kotlinx.coroutines.flow.StateFlow

/**
 * Student sign-in. The app shows the login screen while [session] is null.
 * The session is remembered between launches, so [session] may already be set at startup.
 */
interface AuthRepository {
    val session: StateFlow<AuthSession?>

    /**
     * Checks a remembered session with the backend once the app starts: refreshes expired
     * tokens, and signs out if the backend no longer accepts them. Never throws for network problems.
     */
    suspend fun verifySession()

    /** Throws [com.dreamteam.bookworm.domain.model.AuthException] for wrong email or password. */
    suspend fun login(email: String, password: String): AuthSession

    /** Throws [com.dreamteam.bookworm.domain.model.AuthException] if the email is taken or a field is invalid. */
    suspend fun register(registration: Registration): AuthSession

    suspend fun logout()

    /** Changes the signed-in user's name and surname; [session] follows. */
    suspend fun updateProfile(name: String, surname: String)

    /**
     * Changes the password. Throws [com.dreamteam.bookworm.domain.model.AuthException] with
     * [com.dreamteam.bookworm.domain.model.AuthFailure.InvalidCredentials] when [currentPassword] is wrong.
     */
    suspend fun changePassword(currentPassword: String, newPassword: String)
}
