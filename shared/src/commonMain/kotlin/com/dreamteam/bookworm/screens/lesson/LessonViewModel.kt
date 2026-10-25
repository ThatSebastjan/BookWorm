package com.dreamteam.bookworm.screens.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.Answer
import com.dreamteam.bookworm.domain.model.AnswerResult
import com.dreamteam.bookworm.domain.model.Lesson
import com.dreamteam.bookworm.domain.model.LessonSummary
import com.dreamteam.bookworm.domain.model.Question
import com.dreamteam.bookworm.domain.repository.LessonRepository
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui.loadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the student has entered for the current question so far. */
data class AnswerDraft(
    val selected: Set<String> = emptySet(),
    val text: String = "",
    val numerator: String = "",
    val denominator: String = "",
    val word: String? = null,
    val order: List<String> = emptyList(),
    val pairs: Map<String, String> = emptyMap(),
    val pendingLeft: String? = null,
)

sealed interface Phase {
    data object Answering : Phase
    data object Checking : Phase
    data class Checked(val result: AnswerResult) : Phase
}

data class LessonUiState(
    val lesson: UiState<Lesson> = UiState.Loading,
    val index: Int = 0,
    val draft: AnswerDraft = AnswerDraft(),
    val phase: Phase = Phase.Answering,
    val starsEarned: Int = 0,
    val submitFailed: Boolean = false,
    val finishing: Boolean = false,
    val summary: LessonSummary? = null,
) {
    val question: Question? get() = (lesson as? UiState.Success)?.data?.questions?.getOrNull(index)
    val questionCount: Int get() = (lesson as? UiState.Success)?.data?.questions?.size ?: 0
    val locked: Boolean get() = phase != Phase.Answering
    val canCheck: Boolean get() = phase == Phase.Answering && question?.let { draft.answerFor(it) } != null
}

/** Builds the answer to send, or null while the question isn't fully answered. */
fun AnswerDraft.answerFor(question: Question): Answer? = when (question) {
    is Question.SingleChoice -> selected.singleOrNull()?.let { Answer.Choice(it) }
    is Question.MultiChoice -> selected.takeIf { it.isNotEmpty() }?.let { Answer.Choices(it) }
    is Question.TypedAnswer ->
        if (question.fractionInput) {
            if (numerator.isNotBlank() && denominator.isNotBlank()) Answer.Text("${numerator.trim()}/${denominator.trim()}") else null
        } else {
            text.takeIf { it.isNotBlank() }?.let { Answer.Text(it.trim()) }
        }
    is Question.FillBlank -> word?.let { Answer.Text(it) }
    is Question.Order -> order.takeIf { it.size == question.items.size }?.let { Answer.Ordered(it) }
    is Question.Match -> pairs.takeIf { it.size == question.left.size }?.let { Answer.Pairs(it) }
}

class LessonViewModel(
    private val lessonId: String,
    private val repository: LessonRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LessonUiState())
    val state: StateFlow<LessonUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = LessonUiState()
            _state.update { it.copy(lesson = loadState { repository.getLesson(lessonId) }) }
        }
    }

    private fun edit(block: AnswerDraft.() -> AnswerDraft) {
        if (_state.value.locked) return
        _state.update { it.copy(draft = it.draft.block()) }
    }

    fun toggleOption(optionId: String) = edit {
        when (_state.value.question) {
            is Question.MultiChoice -> copy(selected = if (optionId in selected) selected - optionId else selected + optionId)
            else -> copy(selected = setOf(optionId))
        }
    }

    fun setText(value: String) = edit { copy(text = value) }
    fun setNumerator(value: String) = edit { copy(numerator = value.filter(Char::isDigit).take(2)) }
    fun setDenominator(value: String) = edit { copy(denominator = value.filter(Char::isDigit).take(2)) }
    fun pickWord(word: String) = edit { copy(word = word) }
    fun clearWord() = edit { copy(word = null) }

    fun placeItem(itemId: String) = edit { if (itemId in order) this else copy(order = order + itemId) }
    fun removeItemAt(slot: Int) = edit { copy(order = order.filterIndexed { i, _ -> i != slot }) }

    fun pickLeft(leftId: String) = edit {
        // Tapping a paired item un-pairs it so it can be matched again.
        if (leftId in pairs) copy(pairs = pairs - leftId, pendingLeft = leftId)
        else copy(pendingLeft = if (pendingLeft == leftId) null else leftId)
    }

    fun pickRight(rightId: String) = edit {
        val left = pendingLeft ?: return@edit copy(pairs = pairs.filterValues { it != rightId })
        copy(pairs = pairs.filterValues { it != rightId } + (left to rightId), pendingLeft = null)
    }

    fun check() {
        val current = _state.value
        val question = current.question ?: return
        val answer = current.draft.answerFor(question) ?: return
        _state.update { it.copy(phase = Phase.Checking, submitFailed = false) }
        viewModelScope.launch {
            when (val result = loadState { repository.submitAnswer(lessonId, question.id, answer) }) {
                is UiState.Success -> _state.update {
                    it.copy(phase = Phase.Checked(result.data), starsEarned = it.starsEarned + result.data.starsEarned)
                }
                else -> _state.update { it.copy(phase = Phase.Answering, submitFailed = true) }
            }
        }
    }

    /** After a wrong answer: clear the answer and let the student try again. */
    fun retry() = _state.update { it.copy(draft = AnswerDraft(), phase = Phase.Answering) }

    fun next() {
        val current = _state.value
        if (current.index + 1 < current.questionCount) {
            _state.update { it.copy(index = it.index + 1, draft = AnswerDraft(), phase = Phase.Answering) }
        } else {
            finish()
        }
    }

    private fun finish() {
        if (_state.value.finishing) return
        _state.update { it.copy(finishing = true, submitFailed = false) }
        viewModelScope.launch {
            val summary = loadState { repository.completeLesson(lessonId) }
            _state.update {
                // On failure the last result stays on screen, so "Continue" can be pressed again.
                it.copy(finishing = false, summary = (summary as? UiState.Success)?.data, submitFailed = summary !is UiState.Success)
            }
        }
    }
}
