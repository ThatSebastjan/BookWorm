package com.dreamteam.bookworm.data.remote

import androidx.compose.ui.text.intl.Locale
import com.dreamteam.bookworm.domain.model.AuthException
import com.dreamteam.bookworm.domain.model.AuthFailure
import com.dreamteam.bookworm.domain.model.AuthSession
import com.dreamteam.bookworm.domain.model.Registration
import com.dreamteam.bookworm.domain.model.UserRole
import com.dreamteam.bookworm.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Sign-in against the BookWorm backend: `POST /auth/register`, `POST /auth/login`,
 * then `GET /user` for the name and surname.
 * The session is saved in [store], so the student stays signed in between launches;
 * [verifySession] checks it with the backend and refreshes expired tokens.
 * With [requiredRole] set, only accounts with that role can sign in (the teacher app),
 * and [register] creates accounts with that role.
 */
class RemoteAuthRepository(
    baseUrl: String = defaultApiBaseUrl,
    private val store: SessionStore = InMemorySessionStore(),
    private val client: HttpClient = defaultHttpClient(baseUrl),
    private val requiredRole: UserRole? = null,
) : AuthRepository {

    private val _session = MutableStateFlow(loadStoredSession())
    override val session: StateFlow<AuthSession?> = _session.asStateFlow()
    private val refreshLock = Mutex()

    override suspend fun login(email: String, password: String): AuthSession {
        val response = client.post("auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email.trim(), password))
        }
        return signIn(response.tokensOrThrow())
    }

    /** The teacher app signs up teachers; every other app signs up students. */
    override suspend fun register(registration: Registration): AuthSession {
        val route = if (requiredRole == UserRole.Teacher) "auth/register/teacher" else "auth/register"
        val response = client.post(route) {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRequest(
                    name = registration.name.trim(),
                    surname = registration.surname.trim(),
                    email = registration.email.trim(),
                    password = registration.password,
                ),
            )
        }
        return signIn(response.tokensOrThrow())
    }

    override suspend fun logout() {
        // The backend has no logout endpoint yet: forgetting the tokens signs the student out here.
        store.clear()
        _session.value = null
    }

    /** Calls as the signed-in user, refreshing an expired token like every other repository. */
    private val api by lazy { AuthorizedApi(client, this) }

    override suspend fun updateProfile(name: String, surname: String) {
        val stored = _session.value ?: throw ApiException(HttpStatusCode.Unauthorized)
        val response = api.request {
            method = HttpMethod.Patch
            url { appendPathSegments("user") }
            contentType(ContentType.Application.Json)
            setBody(ProfileRequest(name.trim(), surname.trim()))
        }
        if (!response.status.isSuccess()) throw ApiException(response.status)
        keepIfCurrent(stored, (_session.value ?: stored).withProfile(response.body()))
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String) {
        val response = api.request {
            method = HttpMethod.Post
            url { appendPathSegments("user", "password") }
            contentType(ContentType.Application.Json)
            setBody(PasswordRequest(currentPassword, newPassword))
        }
        when {
            response.status.isSuccess() -> Unit
            // 403, not 401: a wrong password must not look like an expired session.
            response.status == HttpStatusCode.Forbidden -> throw AuthException(AuthFailure.InvalidCredentials)
            response.status == HttpStatusCode.BadRequest -> throw AuthException(AuthFailure.InvalidDetails)
            else -> throw ApiException(response.status)
        }
    }

    override suspend fun verifySession() {
        val stored = _session.value ?: return
        try {
            val me = client.get("user") { bearerAuth(stored.token) }
            when {
                me.status.isSuccess() -> {
                    val updated = stored.withProfile(me.body())
                    // The account's role changed since it signed in here.
                    if (!updated.hasRequiredRole()) logout() else keepIfCurrent(stored, updated)
                }
                me.status == HttpStatusCode.Unauthorized -> tokenAfterUnauthorized(stored.token)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Offline or the backend is down: keep the remembered session and try again next launch.
        }
    }

    /**
     * The backend answered 401 to [rejected]: returns a token to retry with, or null once signed out.
     * The refresh token works only once, so concurrent callers share one refresh: whoever comes
     * second finds the token already replaced and just gets the new one.
     */
    suspend fun tokenAfterUnauthorized(rejected: String): String? = refreshLock.withLock {
        val current = _session.value ?: return null
        if (current.token != rejected) return current.token
        val response = client.post("auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(current.refreshToken))
        }
        when {
            response.status.isSuccess() -> fetchSession(response.body()).also { keepIfCurrent(current, it) }.token
            response.status == HttpStatusCode.Unauthorized -> {
                if (_session.value == current) logout()
                null
            }
            else -> throw ApiException(response.status)
        }
    }

    /** Saves [updated] unless the student signed out or switched accounts while it was being fetched. */
    private fun keepIfCurrent(stored: AuthSession, updated: AuthSession) {
        if (_session.value?.userId == stored.userId) saveSession(updated)
    }

    /** Signs in, unless the account is for the other app: then its tokens are simply dropped. */
    private suspend fun signIn(tokens: TokenResponse): AuthSession {
        val session = fetchSession(tokens)
        if (!session.hasRequiredRole()) throw AuthException(AuthFailure.WrongRole)
        saveSession(session)
        return session
    }

    private fun AuthSession.hasRequiredRole() = requiredRole == null || role == requiredRole

    private suspend fun fetchSession(tokens: TokenResponse): AuthSession {
        val me = client.get("user") { bearerAuth(tokens.accessToken) }
        if (!me.status.isSuccess()) throw ApiException(me.status)
        val user = me.body<MeResponse>()
        return AuthSession(
            userId = user.id,
            name = user.name,
            surname = user.surname,
            email = user.email,
            role = user.userRole(),
            token = tokens.accessToken,
            refreshToken = tokens.refreshToken,
        )
    }

    private fun AuthSession.withProfile(user: MeResponse) =
        copy(userId = user.id, name = user.name, surname = user.surname, email = user.email, role = user.userRole())

    private fun saveSession(session: AuthSession) {
        store.write(Json.encodeToString(StoredSession.from(session)))
        _session.value = session
    }

    private fun loadStoredSession(): AuthSession? = try {
        store.read()?.let { Json.decodeFromString<StoredSession>(it).toSession() }
    } catch (_: Exception) {
        // Unreadable (e.g. saved by an older version): start at the login screen.
        store.clear()
        null
    }

    private suspend fun HttpResponse.tokensOrThrow(): TokenResponse = when {
        status.isSuccess() -> body()
        status == HttpStatusCode.Unauthorized -> throw AuthException(AuthFailure.InvalidCredentials)
        status == HttpStatusCode.Conflict -> throw AuthException(AuthFailure.EmailTaken)
        status == HttpStatusCode.BadRequest -> throw AuthException(AuthFailure.InvalidDetails)
        else -> throw ApiException(status)
    }

    @Serializable
    private data class LoginRequest(val email: String, val password: String)

    @Serializable
    private data class RegisterRequest(val name: String, val surname: String, val email: String, val password: String)

    @Serializable
    private data class RefreshRequest(val refreshToken: String)

    @Serializable
    private data class ProfileRequest(val name: String, val surname: String)

    @Serializable
    private data class PasswordRequest(val currentPassword: String, val newPassword: String)

    /** What is written to the [SessionStore]. */
    @Serializable
    private data class StoredSession(
        val userId: String,
        val name: String,
        val surname: String,
        val email: String,
        // Sessions saved before roles existed were all students.
        val role: UserRole = UserRole.Student,
        val token: String,
        val refreshToken: String,
    ) {
        fun toSession() = AuthSession(userId, name, surname, email, role, token, refreshToken)

        companion object {
            fun from(s: AuthSession) = StoredSession(s.userId, s.name, s.surname, s.email, s.role, s.token, s.refreshToken)
        }
    }

    @Serializable
    private data class TokenResponse(val accessToken: String, val expiresIn: Long, val refreshToken: String)

    @Serializable
    private data class MeResponse(val id: String, val email: String, val role: String, val name: String, val surname: String) {
        fun userRole() = if (role == "teacher") UserRole.Teacher else UserRole.Student
    }
}

/** A response the app didn't expect. Shown to the user as a connection error. */
class ApiException(val status: HttpStatusCode) : Exception("Unexpected response: $status")

fun defaultHttpClient(baseUrl: String): HttpClient = HttpClient {
    expectSuccess = false
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    defaultRequest {
        url(baseUrl.trimEnd('/') + "/")
        // Practice questions the server makes from flashcards come in this language.
        header(HttpHeaders.AcceptLanguage, Locale.current.language)
    }
}
