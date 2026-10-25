package com.dreamteam.bookworm.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.ChapterSummary
import com.dreamteam.bookworm.domain.model.Subject
import com.dreamteam.bookworm.domain.repository.LibraryRepository
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui.loadState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val subjects: UiState<List<Subject>> = UiState.Loading,
    val selectedSubjectId: String? = null,
    val chapters: UiState<List<ChapterSummary>> = UiState.Loading,
)

class LibraryViewModel(private val repository: LibraryRepository) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()
    private var chaptersJob: Job? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(subjects = UiState.Loading) }
            val subjects = loadState { repository.getSubjects() }
            _state.update {
                it.copy(
                    subjects = subjects,
                    selectedSubjectId = it.selectedSubjectId ?: (subjects as? UiState.Success)?.data?.firstOrNull()?.id,
                )
            }
            loadChapters(showLoading = true)
        }
    }

    fun selectSubject(subjectId: String) {
        if (subjectId == _state.value.selectedSubjectId) return
        _state.update { it.copy(selectedSubjectId = subjectId) }
        loadChapters(showLoading = true)
    }

    /** Reloads chapters in the background, e.g. after practising flashcards. */
    fun refresh() = loadChapters(showLoading = false)

    fun retryChapters() = loadChapters(showLoading = true)

    private fun loadChapters(showLoading: Boolean) {
        val subjectId = _state.value.selectedSubjectId ?: return
        chaptersJob?.cancel()
        chaptersJob = viewModelScope.launch {
            if (showLoading || _state.value.chapters !is UiState.Success) {
                _state.update { it.copy(chapters = UiState.Loading) }
            }
            val chapters = loadState { repository.getChapters(subjectId) }
            if (chapters is UiState.Error && !showLoading && _state.value.chapters is UiState.Success) return@launch
            _state.update { it.copy(chapters = chapters) }
        }
    }
}
