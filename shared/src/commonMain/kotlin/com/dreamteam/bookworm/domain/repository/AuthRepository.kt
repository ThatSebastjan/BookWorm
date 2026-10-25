package com.dreamteam.bookworm.domain.repository

import com.dreamteam.bookworm.domain.model.AuthSession
import com.dreamteam.bookworm.domain.model.Registration
import kotlinx.coroutines.flow.StateFlow

/**
 * Student sign-in. The app shows the login screen while [session] is null.
 * A real implementation should keep the session in secure storage (Keychain,
 * EncryptedSharedPreferences) so students stay signed in between launches.
 */
interface AuthRepository {
    val session: StateFlow<AuthSession?>

    /** Throws [com.dreamteam.bookworm.domain.model.AuthException] for wrong email or password. */
    suspend fun login(email: String, password: String): AuthSession

    /** Throws [com.dreamteam.bookworm.domain.model.AuthException] if the email is taken or the class code is unknown. */
    suspend fun register(registration: Registration): AuthSession

    suspend fun logout()
}
