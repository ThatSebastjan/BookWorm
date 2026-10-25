package com.dreamteam.bookworm.teacher.screens.quizsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.ChapterSummary
import com.dreamteam.bookworm.domain.model.QuizConfig
import com.dreamteam.bookworm.domain.model.QuizSetup
import com.dreamteam.bookworm.domain.repository.PopQuizRepository
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui.loadState
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuizSetupUiState(
    val setup: UiState<QuizSetup> = UiState.Loading,
    val chapterId: String? = null,
    val questionCount: Int = 8,
    val minutes: Int = 5,
    val calmMode: Boolean = true,
    val chime: Boolean = true,
    val starting: Boolean = false,
    val startFailed: Boolean = false,
) {
    val chapter: ChapterSummary?
        get() = (setup as? UiState.Success)?.data?.chapters?.firstOrNull { it.id == chapterId }

    /** Rounded to 5 seconds, like the canvas summary. */
    val secondsPerQuestion: Int
        get() = ((minutes * 60f) / questionCount / 5f).roundToInt() * 5

    val canStart: Boolean get() = chapter != null && !starting
}

class QuizSetupViewModel(
    private val classId: String,
    private val repository: PopQuizRepository,
    /** Called with the new quiz id once it is running. */
    private val onStarted: (String) -> Unit,
) : ViewModel() {

    private val _state = MutableStateFlow(QuizSetupUiState())
    val state: StateFlow<QuizSetupUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(setup = UiState.Loading) }
            val setup = loadState { repository.getQuizSetup(classId) }
            _state.update { current ->
                val data = (setup as? UiState.Success)?.data
                current.copy(
                    setup = setup,
                    chapterId = current.chapterId ?: data?.chapters?.firstOrNull()?.id,
                    questionCount = data?.questionCounts?.let { if (current.questionCount in it) current.questionCount else it.first() } ?: current.questionCount,
                    minutes = data?.minuteOptions?.let { if (current.minutes in it) current.minutes else it.first() } ?: current.minutes,
                )
            }
        }
    }

    fun selectChapter(chapterId: String) = _state.update { it.copy(chapterId = chapterId) }
    fun selectCount(count: Int) = _state.update { it.copy(questionCount = count) }
    fun selectMinutes(minutes: Int) = _state.update { it.copy(minutes = minutes) }
    fun setCalmMode(on: Boolean) = _state.update { it.copy(calmMode = on) }
    fun setChime(on: Boolean) = _state.update { it.copy(chime = on) }

    fun start() {
        val current = _state.value
        val chapterId = current.chapterId ?: return
        if (!current.canStart) return
        val config = QuizConfig(classId, chapterId, current.questionCount, current.minutes, current.calmMode, current.chime)
        viewModelScope.launch {
            _state.update { it.copy(starting = true, startFailed = false) }
            when (val result = loadState { repository.startQuiz(config) }) {
                is UiState.Success -> onStarted(result.data.id)
                is UiState.Error -> _state.update { it.copy(starting = false, startFailed = true) }
                UiState.Loading -> Unit
            }
        }
    }
}
