package com.dreamteam.bookworm.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.AuthException
import com.dreamteam.bookworm.domain.model.AuthFailure
import com.dreamteam.bookworm.domain.repository.AuthRepository
import com.dreamteam.bookworm.screens.auth.MIN_PASSWORD_LENGTH
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui.loadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How the last save of one form went. */
enum class FormResult { Saved, Failed, WrongPassword }

data class EditProfileUiState(
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val savingDetails: Boolean = false,
    val detailsResult: FormResult? = null,
    val currentPassword: String = "",
    val newPassword: String = "",
    val changingPassword: Boolean = false,
    val passwordResult: FormResult? = null,
    /** Set after trying to save with empty or too weak fields. */
    val showErrors: Boolean = false,
) {
    val newPasswordWeak: Boolean
        get() = newPassword.length < MIN_PASSWORD_LENGTH || newPassword.none { it.isLetter() } || newPassword.none { it.isDigit() }
}

/** The signed-in user's name and password, for the student app's Edit profile and the teacher app's Profile. */
class EditProfileViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    init {
        auth.session.value?.let { session ->
            _state.update { it.copy(name = session.name, surname = session.surname, email = session.email) }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value, detailsResult = null) }
    fun setSurname(value: String) = _state.update { it.copy(surname = value, detailsResult = null) }
    fun setCurrentPassword(value: String) = _state.update { it.copy(currentPassword = value, passwordResult = null) }
    fun setNewPassword(value: String) = _state.update { it.copy(newPassword = value, passwordResult = null) }

    fun saveDetails() {
        val current = _state.value
        if (current.savingDetails) return
        if (current.name.isBlank() || current.surname.isBlank()) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        _state.update { it.copy(savingDetails = true, detailsResult = null) }
        viewModelScope.launch {
            val result = if (loadState { auth.updateProfile(current.name.trim(), current.surname.trim()) } is UiState.Error) FormResult.Failed else FormResult.Saved
            _state.update { it.copy(savingDetails = false, detailsResult = result) }
        }
    }

    fun changePassword() {
        val current = _state.value
        if (current.changingPassword) return
        if (current.currentPassword.isEmpty() || current.newPasswordWeak) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        _state.update { it.copy(changingPassword = true, passwordResult = null) }
        viewModelScope.launch {
            val result = try {
                auth.changePassword(current.currentPassword, current.newPassword)
                FormResult.Saved
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                if (e.failure == AuthFailure.InvalidCredentials) FormResult.WrongPassword else FormResult.Failed
            } catch (e: Exception) {
                FormResult.Failed
            }
            _state.update {
                it.copy(
                    changingPassword = false,
                    passwordResult = result,
                    // Empty the fields once it worked, so the new password isn't left on screen.
                    currentPassword = if (result == FormResult.Saved) "" else it.currentPassword,
                    newPassword = if (result == FormResult.Saved) "" else it.newPassword,
                    showErrors = false,
                )
            }
        }
    }
}
