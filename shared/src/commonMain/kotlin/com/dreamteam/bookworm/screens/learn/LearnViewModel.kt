package com.dreamteam.bookworm.screens.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.LearnChapter
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
    /** Empty until the student is let into a classroom. */
    val subjects: List<Subject>,
    val subject: Subject?,
    /** Null while the teacher hasn't assigned a path for [subject]. */
    val path: LearnPath?,
    val stats: StudentStats,
    /** The chapter the student picked; null follows the one they are working on. */
    val chosenChapterId: String? = null,
) {
    val chapter: LearnChapter?
        get() = path?.let { p -> p.chapters.firstOrNull { it.chapter.id == chosenChapterId && it.isOpen } ?: p.chapters.getOrNull(p.currentChapterIndex) }
}

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

    /** Shows another chapter's lessons. Picking the one the student is working on follows it again. */
    fun selectChapter(chapterId: String) = _state.update { state ->
        val content = (state as? UiState.Success)?.data ?: return@update state
        val chapter = content.path?.chapters?.firstOrNull { it.chapter.id == chapterId }?.takeIf { it.isOpen } ?: return@update state
        val current = content.path.chapters.getOrNull(content.path.currentChapterIndex)
        UiState.Success(content.copy(chosenChapterId = chapterId.takeIf { chapter != current }))
    }

    private fun load(showLoading: Boolean) {
        job?.cancel()
        job = viewModelScope.launch {
            val previous = (_state.value as? UiState.Success)?.data
            if (showLoading || previous == null) _state.value = UiState.Loading
            val result = loadState {
                val subjects = libraryRepository.getSubjects()
                // The student may have left the class picked before.
                val subject = subjects.firstOrNull { it.id == subjectId } ?: subjects.firstOrNull()
                subjectId = subject?.id
                coroutineScope {
                    val path = async { subject?.let { learnRepository.getLearnPath(it.id) } }
                    val stats = async { learnRepository.getStats() }
                    LearnContent(
                        subjects = subjects,
                        subject = subject,
                        path = path.await(),
                        stats = stats.await(),
                        // A chapter picked by hand stays picked while it still exists and is open.
                        chosenChapterId = previous?.chosenChapterId?.takeIf { previous.subject?.id == subject?.id },
                    )
                }
            }
            if (result is UiState.Error && !showLoading && _state.value is UiState.Success) return@launch
            _state.update { result }
        }
    }
}
