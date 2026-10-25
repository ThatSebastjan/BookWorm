package com.dreamteam.bookworm.teacher.screens.lessoneditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.AuthoredQuestion
import com.dreamteam.bookworm.domain.model.LessonContent
import com.dreamteam.bookworm.domain.repository.CurriculumRepository
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui.loadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LessonEditorUiState(
    val content: UiState<LessonContent> = UiState.Loading,
    /** The questions with unsaved changes. */
    val questions: List<AuthoredQuestion> = emptyList(),
    val selected: Int = 0,
    val dirty: Boolean = false,
    val saving: Boolean = false,
    val justSaved: Boolean = false,
    val suggesting: Boolean = false,
    /** The chapter has no flashcards to suggest questions from. */
    val noSuggestions: Boolean = false,
    val actionFailed: Boolean = false,
    val confirmLeave: Boolean = false,
) {
    val current: AuthoredQuestion? get() = questions.getOrNull(selected)
    val incomplete: Int get() = questions.count { !it.isComplete() }
    val canSave: Boolean get() = dirty && !saving && incomplete == 0
}

class LessonEditorViewModel(
    private val lessonId: String,
    private val repository: CurriculumRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LessonEditorUiState())
    val state: StateFlow<LessonEditorUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(content = UiState.Loading) }
            val content = loadState { repository.getLessonContent(lessonId) }
            _state.update { current ->
                val questions = (content as? UiState.Success)?.data?.questions.orEmpty().map { it.normalised() }
                current.copy(content = content, questions = questions, selected = 0, dirty = false)
            }
        }
    }

    fun select(index: Int) = _state.update { it.copy(selected = index) }

    fun add(kind: QuestionKind) = _state.update {
        it.copy(questions = it.questions + newQuestion(kind, freshId(it.questions)), selected = it.questions.size, dirty = true, justSaved = false)
    }

    fun remove(index: Int) = _state.update {
        val questions = it.questions.filterIndexed { i, _ -> i != index }
        it.copy(questions = questions, selected = it.selected.coerceAtMost(questions.lastIndex).coerceAtLeast(0), dirty = true, justSaved = false)
    }

    fun move(index: Int, by: Int) = _state.update {
        val target = index + by
        if (target !in it.questions.indices) return@update it
        val questions = it.questions.toMutableList().apply { add(target, removeAt(index)) }
        it.copy(questions = questions, selected = target, dirty = true, justSaved = false)
    }

    /** Applies an edit from QuestionEditing.kt to the selected question. */
    fun edit(change: AuthoredQuestion.() -> AuthoredQuestion) = _state.update {
        val current = it.current ?: return@update it
        it.copy(questions = it.questions.toMutableList().apply { set(it.selected, current.change()) }, dirty = true, justSaved = false)
    }

    /** Adds questions made from the chapter's flashcards after the existing ones, for the teacher to edit. */
    fun suggest() {
        viewModelScope.launch {
            _state.update { it.copy(suggesting = true, noSuggestions = false, actionFailed = false) }
            when (val suggested = loadState { repository.suggestQuestions(lessonId) }) {
                is UiState.Success -> _state.update { current ->
                    if (suggested.data.isEmpty()) return@update current.copy(suggesting = false, noSuggestions = true)
                    var questions = current.questions
                    suggested.data.forEach { questions = questions + it.withId(freshId(questions)) }
                    current.copy(questions = questions, selected = current.questions.size, suggesting = false, dirty = true, justSaved = false)
                }
                is UiState.Error -> _state.update { it.copy(suggesting = false, actionFailed = true) }
                UiState.Loading -> Unit
            }
        }
    }

    fun save() {
        if (!_state.value.canSave) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, actionFailed = false) }
            when (val saved = loadState { repository.saveLessonContent(lessonId, _state.value.questions) }) {
                is UiState.Success -> _state.update {
                    it.copy(content = saved, questions = saved.data.questions.map { q -> q.normalised() }, saving = false, dirty = false, justSaved = true)
                }
                is UiState.Error -> _state.update { it.copy(saving = false, actionFailed = true) }
                UiState.Loading -> Unit
            }
        }
    }

    /** Leaving with unsaved changes asks first; returns true when it's fine to go now. */
    fun requestLeave(): Boolean {
        if (!_state.value.dirty) return true
        _state.update { it.copy(confirmLeave = true) }
        return false
    }

    fun stay() = _state.update { it.copy(confirmLeave = false) }

    private fun freshId(questions: List<AuthoredQuestion>): String {
        val taken = questions.map { it.question.id }.toSet()
        return generateSequence(questions.size + 1) { it + 1 }.map { "$lessonId-q$it" }.first { it !in taken }
    }
}
