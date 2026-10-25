package com.dreamteam.bookworm.screens.chapter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.Chapter
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
    val cardIndex: Int = 0,
    val flipped: Boolean = false,
)

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
            _state.update { it.copy(chapter = chapter) }
        }
    }

    fun selectTab(tab: ChapterTab) = _state.update { it.copy(tab = tab, flipped = false) }

    fun flip() = _state.update { it.copy(flipped = !it.flipped) }

    /** Records the answer for the current card and moves on, wrapping around at the end. */
    fun answerCard(known: Boolean) {
        val current = _state.value
        val cards = (current.chapter as? UiState.Success)?.data?.flashcards ?: return
        if (cards.isEmpty()) return
        val card = cards[current.cardIndex]
        _state.update { it.copy(cardIndex = (it.cardIndex + 1) % cards.size, flipped = false) }
        // Fire and forget: progress is not worth blocking the card flow on.
        viewModelScope.launch { loadState { repository.recordFlashcard(card.id, known) } }
    }
}
