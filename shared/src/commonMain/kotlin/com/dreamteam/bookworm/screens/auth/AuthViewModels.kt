package com.dreamteam.bookworm.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.AuthException
import com.dreamteam.bookworm.domain.model.AuthFailure
import com.dreamteam.bookworm.domain.model.Registration
import com.dreamteam.bookworm.domain.model.normalizeClassCode
import com.dreamteam.bookworm.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A problem with one field, found before anything is sent. */
enum class FieldError { Required, InvalidEmail, PasswordTooShort }

/** Why the last submit failed; [Network] covers everything that isn't an [AuthFailure]. */
sealed interface SubmitError {
    data class Auth(val failure: AuthFailure) : SubmitError
    data object Network : SubmitError
}

const val MIN_PASSWORD_LENGTH = 8

private fun validateEmail(email: String) = when {
    email.isBlank() -> FieldError.Required
    !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email.trim()) -> FieldError.InvalidEmail
    else -> null
}

/** Runs [block], turning failures into a [SubmitError]; null on success. */
private suspend fun submit(block: suspend () -> Unit): SubmitError? = try {
    block()
    null
} catch (e: CancellationException) {
    throw e
} catch (e: AuthException) {
    SubmitError.Auth(e.failure)
} catch (e: Exception) {
    SubmitError.Network
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: FieldError? = null,
    val passwordError: FieldError? = null,
    val submitting: Boolean = false,
    val error: SubmitError? = null,
)

/** Signing in flips [AuthRepository.session], and the app moves on from there. */
class LoginViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun setEmail(value: String) = _state.update { it.copy(email = value, emailError = null, error = null) }
    fun setPassword(value: String) = _state.update { it.copy(password = value, passwordError = null, error = null) }

    fun login() {
        val current = _state.value
        if (current.submitting) return
        val emailError = validateEmail(current.email)
        val passwordError = if (current.password.isEmpty()) FieldError.Required else null
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val error = submit { auth.login(current.email.trim(), current.password) }
            _state.update { it.copy(submitting = false, error = error) }
        }
    }
}

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val classCode: String = "",
    val nameError: FieldError? = null,
    val emailError: FieldError? = null,
    val passwordError: FieldError? = null,
    val classCodeError: FieldError? = null,
    val submitting: Boolean = false,
    val error: SubmitError? = null,
)

class RegisterViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun setName(value: String) = _state.update { it.copy(name = value, nameError = null, error = null) }
    fun setEmail(value: String) = _state.update { it.copy(email = value, emailError = null, error = null) }
    fun setPassword(value: String) = _state.update { it.copy(password = value, passwordError = null, error = null) }
    fun setClassCode(value: String) = _state.update { it.copy(classCode = value.uppercase(), classCodeError = null, error = null) }

    fun register() {
        val current = _state.value
        if (current.submitting) return
        val checked = current.copy(
            nameError = if (current.name.isBlank()) FieldError.Required else null,
            emailError = validateEmail(current.email),
            passwordError = when {
                current.password.isEmpty() -> FieldError.Required
                current.password.length < MIN_PASSWORD_LENGTH -> FieldError.PasswordTooShort
                else -> null
            },
            classCodeError = if (current.classCode.isBlank()) FieldError.Required else null,
        )
        if (listOf(checked.nameError, checked.emailError, checked.passwordError, checked.classCodeError).any { it != null }) {
            _state.value = checked
            return
        }
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val error = submit {
                auth.register(Registration(current.name.trim(), current.email.trim(), current.password, normalizeClassCode(current.classCode)))
            }
            _state.update { it.copy(submitting = false, error = error) }
        }
    }
}
