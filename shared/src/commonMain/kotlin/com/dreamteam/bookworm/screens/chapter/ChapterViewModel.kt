package com.dreamteam.bookworm.screens.chapter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.Chapter
import com.dreamteam.bookworm.domain.model.Flashcard
import com.dreamteam.bookworm.domain.repository.LibraryRepository
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui.loadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ChapterTab { Notes, Flashcards }

data class ChapterUiState(
    val chapter: UiState<Chapter> = UiState.Loading,
    val tab: ChapterTab = ChapterTab.Notes,
    /** Ids of the cards still to learn, in the order they come up. Known cards are left out. */
    val queue: List<String> = emptyList(),
    val known: Set<String> = emptySet(),
    val flipped: Boolean = false,
    val startingOver: Boolean = false,
    val startOverFailed: Boolean = false,
) {
    private val cards: List<Flashcard> get() = (chapter as? UiState.Success)?.data?.flashcards.orEmpty()
    val current: Flashcard? get() = queue.firstOrNull()?.let { id -> cards.firstOrNull { it.id == id } }
    val total: Int get() = cards.size
    val knownCount: Int get() = cards.count { it.id in known }

    /** Every card is known: practice offers to start over. */
    val mastered: Boolean get() = total > 0 && queue.isEmpty()
}

class ChapterViewModel(
    private val chapterId: String,
    private val repository: LibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChapterUiState())
    val state: StateFlow<ChapterUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(chapter = UiState.Loading) }
            val chapter = loadState { repository.getChapter(chapterId) }
            _state.update { current ->
                val loaded = (chapter as? UiState.Success)?.data ?: return@update current.copy(chapter = chapter)
                val known = loaded.knownFlashcardIds
                current.copy(chapter = chapter, known = known, queue = loaded.flashcards.map { it.id }.filterNot { it in known }, flipped = false)
            }
        }
    }

    fun selectTab(tab: ChapterTab) = _state.update { it.copy(tab = tab, flipped = false) }

    fun flip() = _state.update { it.copy(flipped = !it.flipped) }

    /** "Got it" takes the card out of practice; "Still learning" sends it to the back of the queue. */
    fun answerCard(known: Boolean) {
        val card = _state.value.current ?: return
        _state.update {
            val rest = it.queue.drop(1)
            if (known) {
                it.copy(queue = rest, known = it.known + card.id, flipped = false)
            } else {
                it.copy(queue = rest + card.id, flipped = false)
            }
        }
        // Fire and forget: progress is not worth blocking the card flow on.
        viewModelScope.launch { loadState { repository.recordFlashcard(card.id, known) } }
    }

    /** Forgets what the student knows here and brings every card back. */
    fun startOver() {
        val cards = (_state.value.chapter as? UiState.Success)?.data?.flashcards ?: return
        if (_state.value.startingOver) return
        _state.update { it.copy(startingOver = true, startOverFailed = false) }
        viewModelScope.launch {
            if (loadState { repository.resetFlashcards(chapterId) } is UiState.Error) {
                _state.update { it.copy(startingOver = false, startOverFailed = true) }
            } else {
                _state.update { it.copy(startingOver = false, known = emptySet(), queue = cards.map { card -> card.id }, flipped = false) }
            }
        }
    }
}
