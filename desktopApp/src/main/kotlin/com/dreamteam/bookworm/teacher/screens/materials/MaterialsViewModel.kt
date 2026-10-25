package com.dreamteam.bookworm.teacher.screens.materials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.CardRequest
import com.dreamteam.bookworm.domain.model.CardStyle
import com.dreamteam.bookworm.domain.model.MaterialsOverview
import com.dreamteam.bookworm.domain.model.PickedFile
import com.dreamteam.bookworm.domain.model.UploadStatus
import com.dreamteam.bookworm.domain.repository.MaterialsRepository
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui.loadState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MaterialsUiState(
    val overview: UiState<MaterialsOverview> = UiState.Loading,
    val subjectId: String? = null,
    val chapterId: String? = null,
    val style: CardStyle = CardStyle.Terms,
    val readingGrade: Int? = null,
    /** Files picked or dropped, waiting for "Make flashcards". */
    val staged: List<PickedFile> = emptyList(),
    val uploading: Boolean = false,
    val uploadFailed: Boolean = false,
) {
    val canMakeCards: Boolean get() = staged.isNotEmpty() && chapterId != null && !uploading
}

class MaterialsViewModel(
    private val repository: MaterialsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MaterialsUiState())
    val state: StateFlow<MaterialsUiState> = _state.asStateFlow()

    private var polling: Job? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(overview = UiState.Loading) }
            applyOverview(loadState { repository.getMaterials() })
        }
    }

    private fun applyOverview(overview: UiState<MaterialsOverview>) {
        _state.update { current ->
            val data = (overview as? UiState.Success)?.data
            val subjectId = current.subjectId ?: data?.subjects?.firstOrNull()?.id
            current.copy(
                overview = overview,
                subjectId = subjectId,
                chapterId = current.chapterId ?: data?.chapters?.firstOrNull { it.subjectId == subjectId }?.id,
                readingGrade = current.readingGrade ?: data?.defaultGrade,
            )
        }
        val processing = (overview as? UiState.Success)?.data?.uploads?.any { it.status is UploadStatus.Processing } == true
        if (processing) startPolling()
    }

    /** Refreshes quietly while cards are being written, so the progress bars move. */
    private fun startPolling() {
        if (polling?.isActive == true) return
        polling = viewModelScope.launch {
            while (true) {
                delay(POLL_MS)
                val refreshed = loadState { repository.getMaterials() }
                if (refreshed !is UiState.Success) continue
                _state.update { it.copy(overview = refreshed) }
                if (refreshed.data.uploads.none { it.status is UploadStatus.Processing }) break
            }
        }
    }

    fun selectSubject(subjectId: String) = _state.update { current ->
        val chapters = (current.overview as? UiState.Success)?.data?.chapters.orEmpty()
        current.copy(subjectId = subjectId, chapterId = chapters.firstOrNull { it.subjectId == subjectId }?.id)
    }

    fun selectChapter(chapterId: String) = _state.update { it.copy(chapterId = chapterId) }

    fun selectStyle(style: CardStyle) = _state.update { it.copy(style = style) }

    fun selectGrade(grade: Int) = _state.update { it.copy(readingGrade = grade) }

    fun stage(files: List<PickedFile>) {
        if (files.isEmpty()) return
        _state.update { current ->
            val names = current.staged.map { it.name }.toSet()
            current.copy(staged = current.staged + files.filter { it.name !in names }, uploadFailed = false)
        }
    }

    fun unstage(file: PickedFile) = _state.update { it.copy(staged = it.staged - file) }

    fun makeFlashcards() {
        val current = _state.value
        val chapterId = current.chapterId ?: return
        if (!current.canMakeCards) return
        val request = CardRequest(chapterId, current.style, current.readingGrade ?: return)
        viewModelScope.launch {
            _state.update { it.copy(uploading = true, uploadFailed = false) }
            val sent = mutableListOf<PickedFile>()
            val result = loadState {
                current.staged.forEach { file ->
                    repository.upload(file, request)
                    sent += file
                }
            }
            _state.update { it.copy(uploading = false, staged = it.staged - sent.toSet(), uploadFailed = result is UiState.Error) }
            refreshAfterUpload()
        }
    }

    fun replace(uploadId: String, file: PickedFile) {
        viewModelScope.launch {
            val result = loadState { repository.replaceUpload(uploadId, file) }
            _state.update { it.copy(uploadFailed = result is UiState.Error) }
            refreshAfterUpload()
        }
    }

    private suspend fun refreshAfterUpload() {
        val refreshed = loadState { repository.getMaterials() }
        if (refreshed is UiState.Success) applyOverview(refreshed)
    }

    private companion object {
        const val POLL_MS = 1_500L
    }
}
