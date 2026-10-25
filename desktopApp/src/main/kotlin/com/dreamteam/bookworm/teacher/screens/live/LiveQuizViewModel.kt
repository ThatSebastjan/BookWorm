package com.dreamteam.bookworm.teacher.screens.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.LiveQuiz
import com.dreamteam.bookworm.domain.repository.PopQuizRepository
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui.loadState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class LiveQuizUiState(
    val quiz: UiState<LiveQuiz> = UiState.Loading,
    /** Waiting for the server before leaving the screen. */
    val leaving: Boolean = false,
    val actionFailed: Boolean = false,
)

class LiveQuizViewModel(
    private val quizId: String,
    private val repository: PopQuizRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LiveQuizUiState())
    val state: StateFlow<LiveQuizUiState> = _state.asStateFlow()

    init {
        load()
        tick()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(quiz = UiState.Loading) }
            val quiz = loadState { repository.getLiveQuiz(quizId) }
            _state.update { it.copy(quiz = quiz) }
        }
    }

    /**
     * Counts down locally every second so the ring moves smoothly, and asks the
     * server every few seconds for the real time and how many students finished.
     */
    private fun tick() {
        viewModelScope.launch {
            var ticks = 0
            while (isActive) {
                delay(1_000)
                ticks++
                updateQuiz { quiz ->
                    if (quiz.paused || quiz.secondsLeft == 0) quiz else quiz.copy(elapsedSeconds = quiz.elapsedSeconds + 1)
                }
                if (ticks % SYNC_EVERY_TICKS == 0) {
                    val synced = loadState { repository.getLiveQuiz(quizId) }
                    if (synced is UiState.Success) _state.update { it.copy(quiz = synced) }
                }
            }
        }
    }

    fun addMinute() = send { repository.addMinute(quizId) }

    fun togglePause() {
        val paused = (_state.value.quiz as? UiState.Success)?.data?.paused ?: return
        updateQuiz { it.copy(paused = !paused) }
        send { repository.setPaused(quizId, !paused) }
    }

    /** Stops the countdown on the server, then runs [then] (show results, or leave). */
    fun finish(then: () -> Unit) {
        if (_state.value.leaving) return
        viewModelScope.launch {
            _state.update { it.copy(leaving = true, actionFailed = false) }
            if (loadState { repository.finishQuiz(quizId) } is UiState.Success) {
                then()
            } else {
                _state.update { it.copy(leaving = false, actionFailed = true) }
            }
        }
    }

    private fun send(call: suspend () -> LiveQuiz) {
        viewModelScope.launch {
            when (val result = loadState { call() }) {
                is UiState.Success -> _state.update { it.copy(quiz = result, actionFailed = false) }
                is UiState.Error -> _state.update { it.copy(actionFailed = true) }
                UiState.Loading -> Unit
            }
        }
    }

    private fun updateQuiz(change: (LiveQuiz) -> LiveQuiz) = _state.update { current ->
        val quiz = (current.quiz as? UiState.Success)?.data ?: return@update current
        current.copy(quiz = UiState.Success(change(quiz)))
    }

    private companion object {
        const val SYNC_EVERY_TICKS = 5
    }
}
