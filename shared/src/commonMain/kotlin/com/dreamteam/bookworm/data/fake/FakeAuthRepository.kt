package com.dreamteam.bookworm.data.fake

import com.dreamteam.bookworm.domain.model.AuthException
import com.dreamteam.bookworm.domain.model.AuthFailure
import com.dreamteam.bookworm.domain.model.AuthSession
import com.dreamteam.bookworm.domain.model.Registration
import com.dreamteam.bookworm.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory accounts. Starts signed out, with one demo student:
 * mia@example.com / bookworm. Signing up takes a teacher's class join code
 * (Class 3B's is K7M2Q). Sessions are not persisted, so every launch starts at the login screen.
 */
class FakeAuthRepository(
    private val latencyMs: Long = 500,
    /** Checks a class join code at sign-up. */
    private val isValidClassCode: (String) -> Boolean = { true },
    /** Puts a newly registered student in the class for their code. */
    private val onRegistered: (session: AuthSession, classCode: String) -> Unit = { _, _ -> },
) : AuthRepository {

    private class Account(val id: String, val name: String, val email: String, val password: String)

    private val accounts = mutableListOf(Account(FakeData.studentId, "Mia", "mia@example.com", "bookworm"))

    private val _session = MutableStateFlow<AuthSession?>(null)
    override val session: StateFlow<AuthSession?> = _session.asStateFlow()

    override suspend fun login(email: String, password: String): AuthSession {
        delay(latencyMs)
        val account = accounts.firstOrNull { it.email.equals(email.trim(), ignoreCase = true) && it.password == password }
            ?: throw AuthException(AuthFailure.InvalidCredentials)
        return signIn(account)
    }

    override suspend fun register(registration: Registration): AuthSession {
        delay(latencyMs)
        val email = registration.email.trim()
        if (accounts.any { it.email.equals(email, ignoreCase = true) }) throw AuthException(AuthFailure.EmailTaken)
        if (!isValidClassCode(registration.classCode)) throw AuthException(AuthFailure.InvalidClassCode)
        val account = Account("student-${accounts.size + 1}", registration.name.trim(), email, registration.password)
        accounts += account
        return signIn(account).also { onRegistered(it, registration.classCode) }
    }

    override suspend fun logout() {
        _session.value = null
    }

    private fun signIn(account: Account): AuthSession =
        AuthSession(account.id, account.name, account.email, token = "fake-token-${account.id}").also { _session.value = it }
}
