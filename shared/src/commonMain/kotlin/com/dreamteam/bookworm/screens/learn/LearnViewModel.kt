package com.dreamteam.bookworm.screens.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.LearnPath
import com.dreamteam.bookworm.domain.model.StudentStats
import com.dreamteam.bookworm.domain.model.Subject
import com.dreamteam.bookworm.domain.repository.LearnRepository
import com.dreamteam.bookworm.domain.repository.LibraryRepository
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui.loadState
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LearnContent(
    val subjects: List<Subject>,
    val path: LearnPath,
    val stats: StudentStats,
)

class LearnViewModel(
    private val learnRepository: LearnRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<LearnContent>>(UiState.Loading)
    val state: StateFlow<UiState<LearnContent>> = _state.asStateFlow()
    private var subjectId: String? = null
    private var job: Job? = null

    init {
        load(showLoading = true)
    }

    fun retry() = load(showLoading = true)

    /** Reloads in the background, e.g. after finishing a lesson. */
    fun refresh() = load(showLoading = false)

    fun selectSubject(id: String) {
        if (id == subjectId) return
        subjectId = id
        load(showLoading = true)
    }

    private fun load(showLoading: Boolean) {
        job?.cancel()
        job = viewModelScope.launch {
            if (showLoading || _state.value !is UiState.Success) _state.value = UiState.Loading
            val result = loadState {
                val subjects = libraryRepository.getSubjects()
                val id = subjectId ?: subjects.first().id.also { subjectId = it }
                coroutineScope {
                    val path = async { learnRepository.getLearnPath(id) }
                    val stats = async { learnRepository.getStats() }
                    LearnContent(subjects, path.await(), stats.await())
                }
            }
            if (result is UiState.Error && !showLoading && _state.value is UiState.Success) return@launch
            _state.update { result }
        }
    }
}
