package com.dreamteam.bookworm.screens.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.JoinClassException
import com.dreamteam.bookworm.domain.model.JoinFailure
import com.dreamteam.bookworm.domain.model.StudentClass
import com.dreamteam.bookworm.domain.model.normalizeClassCode
import com.dreamteam.bookworm.domain.repository.StudentClassesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface JoinError {
    data object Empty : JoinError
    data class Failed(val failure: JoinFailure) : JoinError
    data object Network : JoinError
}

data class JoinClassUiState(
    val code: String = "",
    val submitting: Boolean = false,
    val error: JoinError? = null,
    /** Set once the join went through; the screen then shows the outcome. */
    val joined: StudentClass? = null,
)

class JoinClassViewModel(private val repository: StudentClassesRepository) : ViewModel() {

    private val _state = MutableStateFlow(JoinClassUiState())
    val state: StateFlow<JoinClassUiState> = _state.asStateFlow()

    fun setCode(value: String) = _state.update { it.copy(code = value.uppercase(), error = null) }

    fun join() {
        val current = _state.value
        if (current.submitting) return
        val code = normalizeClassCode(current.code)
        if (code.isEmpty()) {
            _state.update { it.copy(error = JoinError.Empty) }
            return
        }
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            try {
                val joined = repository.joinClass(code)
                _state.update { it.copy(submitting = false, joined = joined) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: JoinClassException) {
                _state.update { it.copy(submitting = false, error = JoinError.Failed(e.failure)) }
            } catch (e: Exception) {
                _state.update { it.copy(submitting = false, error = JoinError.Network) }
            }
        }
    }

    /** Back to an empty form to join another class. */
    fun reset() = _state.update { JoinClassUiState() }
}
