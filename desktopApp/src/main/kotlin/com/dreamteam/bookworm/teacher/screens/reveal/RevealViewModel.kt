package com.dreamteam.bookworm.teacher.screens.reveal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.QuestionResult
import com.dreamteam.bookworm.domain.repository.PopQuizRepository
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui.loadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RevealUiState(
    val number: Int = 1,
    val result: UiState<QuestionResult> = UiState.Loading,
) {
    val isLast: Boolean get() = (result as? UiState.Success)?.data?.let { it.number >= it.questionCount } ?: false
}

class RevealViewModel(
    private val quizId: String,
    private val repository: PopQuizRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RevealUiState())
    val state: StateFlow<RevealUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        val number = _state.value.number
        viewModelScope.launch {
            _state.update { it.copy(result = UiState.Loading) }
            val result = loadState { repository.getResult(quizId, number) }
            _state.update { if (it.number == number) it.copy(result = result) else it }
        }
    }

    fun next() {
        if (_state.value.isLast) return
        _state.update { it.copy(number = it.number + 1) }
        load()
    }
}
