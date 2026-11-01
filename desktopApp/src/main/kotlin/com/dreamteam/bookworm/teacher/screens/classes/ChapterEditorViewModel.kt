package com.dreamteam.bookworm.teacher.screens.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.ChapterContent
import com.dreamteam.bookworm.domain.model.Flashcard
import com.dreamteam.bookworm.domain.model.NoteSection
import com.dreamteam.bookworm.domain.repository.ClassesRepository
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui.loadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChapterEditorUiState(
    val loaded: UiState<ChapterContent> = UiState.Loading,
    /** The chapter with unsaved changes. */
    val draft: ChapterContent? = null,
    val className: String = "",
    val dirty: Boolean = false,
    val saving: Boolean = false,
    val justSaved: Boolean = false,
    val saveFailed: Boolean = false,
    /** Set after a save with empty fields, so they show what's missing. */
    val showErrors: Boolean = false,
    val confirmLeave: Boolean = false,
) {
    val canSave: Boolean get() = draft != null && dirty && !saving
}

class ChapterEditorViewModel(
    private val chapterId: String,
    private val repository: ClassesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChapterEditorUiState())
    val state: StateFlow<ChapterEditorUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loaded = UiState.Loading) }
            val chapter = loadState { repository.getChapter(chapterId) }
            val className = (chapter as? UiState.Success)?.let { loaded ->
                (loadState { repository.getClasses() } as? UiState.Success)?.data?.firstOrNull { it.id == loaded.data.classId }?.name
            }.orEmpty()
            _state.update { it.copy(loaded = chapter, draft = (chapter as? UiState.Success)?.data, className = className, dirty = false) }
        }
    }

    fun setTitle(title: String) = edit { it.copy(title = title) }

    // Notes

    fun addNote() = editNotes { it.add(NoteSection("", "", null)) }

    fun updateNote(index: Int, note: NoteSection) = editNotes { it[index] = note }

    fun removeNote(index: Int) = editNotes { it.removeAt(index) }

    fun moveNote(index: Int, by: Int) = editNotes { it.move(index, by) }

    // Flashcards

    /** New cards have no id until saved. */
    fun addCard() = editCards { it.add(Flashcard("", "", "")) }

    fun updateCard(index: Int, card: Flashcard) = editCards { it[index] = card }

    fun removeCard(index: Int) = editCards { it.removeAt(index) }

    fun moveCard(index: Int, by: Int) = editCards { it.move(index, by) }

    // Saving and leaving

    /**
     * Saves the chapter. Sections and cards left completely empty are dropped; half-filled
     * ones (and an empty title) stop the save and get marked instead.
     */
    fun save() {
        val draft = _state.value.draft ?: return
        if (!_state.value.canSave) return
        val cleaned = draft.copy(
            notes = draft.notes.filterNot { it.heading.isBlank() && it.body.isBlank() && it.remember.isNullOrBlank() },
            flashcards = draft.flashcards.filterNot { it.term.isBlank() && it.definition.isBlank() },
        )
        val incomplete = cleaned.title.isBlank() ||
            cleaned.notes.any { it.heading.isBlank() || it.body.isBlank() } ||
            cleaned.flashcards.any { it.term.isBlank() || it.definition.isBlank() }
        if (incomplete) {
            _state.update { it.copy(draft = cleaned, showErrors = true, justSaved = false) }
            return
        }
        _state.update { it.copy(saving = true, saveFailed = false, showErrors = false) }
        viewModelScope.launch {
            when (val saved = loadState { repository.saveChapter(cleaned) }) {
                is UiState.Success -> _state.update { it.copy(draft = saved.data, saving = false, dirty = false, justSaved = true) }
                is UiState.Error -> _state.update { it.copy(saving = false, saveFailed = true) }
                UiState.Loading -> Unit
            }
        }
    }

    /** True when the editor can close now; otherwise asks first. */
    fun requestLeave(): Boolean {
        if (!_state.value.dirty) return true
        _state.update { it.copy(confirmLeave = true) }
        return false
    }

    fun stay() = _state.update { it.copy(confirmLeave = false) }

    private fun edit(change: (ChapterContent) -> ChapterContent) = _state.update { current ->
        val draft = current.draft ?: return@update current
        val changed = change(draft)
        if (changed == draft) current else current.copy(draft = changed, dirty = true, justSaved = false, saveFailed = false)
    }

    private fun editNotes(change: (MutableList<NoteSection>) -> Unit) = edit { it.copy(notes = it.notes.toMutableList().also(change)) }

    private fun editCards(change: (MutableList<Flashcard>) -> Unit) = edit { it.copy(flashcards = it.flashcards.toMutableList().also(change)) }

    private fun <T> MutableList<T>.move(index: Int, by: Int) {
        val target = index + by
        if (target !in indices) return
        add(target, removeAt(index))
    }
}
