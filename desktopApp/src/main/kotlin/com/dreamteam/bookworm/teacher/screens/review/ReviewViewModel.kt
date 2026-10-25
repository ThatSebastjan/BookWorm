package com.dreamteam.bookworm.teacher.screens.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamteam.bookworm.domain.model.DraftCard
import com.dreamteam.bookworm.domain.model.DraftStatus
import com.dreamteam.bookworm.domain.model.ReviewBatch
import com.dreamteam.bookworm.domain.repository.FlashcardReviewRepository
import com.dreamteam.bookworm.ui.UiState
import com.dreamteam.bookworm.ui.loadState
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewUiState(
    /** Success(null) means there is nothing to review yet. */
    val batch: UiState<ReviewBatch?> = UiState.Loading,
    val selectedId: String? = null,
    /** How many cards the last publish made visible, for the confirmation banner. */
    val justPublished: Int? = null,
    val saveFailed: Boolean = false,
) {
    private val cards: List<DraftCard> get() = (batch as? UiState.Success)?.data?.cards.orEmpty()
    val selected: DraftCard? get() = cards.firstOrNull { it.id == selectedId } ?: cards.firstOrNull()
    val approved: Int get() = cards.count { it.status == DraftStatus.Approved }
    val pending: Int get() = cards.count { it.status == DraftStatus.Pending }
    val removed: Int get() = cards.count { it.status == DraftStatus.Removed }
    val clearPending: Int get() = cards.count { it.status == DraftStatus.Pending && it.issue == null }
    val toPublish: Int get() = cards.count { it.status == DraftStatus.Approved && !it.published }
}

class ReviewViewModel(
    private val uploadId: String?,
    private val repository: FlashcardReviewRepository,
    /** Tells the sidebar its "to review" badge changed. */
    private val onCardsChanged: () -> Unit,
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    /** Cards edited since they were last saved. */
    private val dirty = mutableSetOf<String>()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(batch = UiState.Loading) }
            val batch = loadState { repository.getReview(uploadId) }
            _state.update { current ->
                val cards = (batch as? UiState.Success)?.data?.cards.orEmpty()
                current.copy(batch = batch, selectedId = (cards.firstOrNull { it.status == DraftStatus.Pending } ?: cards.firstOrNull())?.id)
            }
        }
    }

    private val batch: ReviewBatch? get() = (_state.value.batch as? UiState.Success)?.data

    fun select(cardId: String) {
        saveDirty()
        _state.update { it.copy(selectedId = cardId) }
    }

    fun editFront(text: String) = editSelected { it.copy(flashcard = it.flashcard.copy(term = text), edited = true) }

    /** Fixing the back is how teachers resolve a flagged card, so it clears the warning. */
    fun editBack(text: String) = editSelected { it.copy(flashcard = it.flashcard.copy(definition = text), edited = true, issue = null) }

    fun approve() = decide(DraftStatus.Approved)

    fun remove() = decide(DraftStatus.Removed)

    private fun editSelected(change: (DraftCard) -> DraftCard) {
        val card = _state.value.selected ?: return
        dirty += card.id
        replaceCard(change(card))
    }

    private fun decide(status: DraftStatus) {
        val card = _state.value.selected ?: return
        val decided = card.copy(status = status, issue = if (status == DraftStatus.Approved) null else card.issue)
        dirty -= card.id
        replaceCard(decided)
        _state.update { it.copy(selectedId = nextPendingAfter(card.id) ?: card.id) }
        save(decided)
    }

    fun approveClearCards() {
        val current = batch ?: return
        saveDirty()
        viewModelScope.launch {
            when (val result = loadState { repository.approveClearCards(current.uploadId) }) {
                is UiState.Success -> {
                    _state.update { it.copy(batch = UiState.Success(result.data.withLocalEdits()), saveFailed = false) }
                    onCardsChanged()
                }
                is UiState.Error -> _state.update { it.copy(saveFailed = true) }
                UiState.Loading -> Unit
            }
        }
    }

    fun publish() {
        val current = batch ?: return
        val count = _state.value.toPublish
        if (count == 0) return
        saveDirty()
        viewModelScope.launch {
            when (val result = loadState { repository.publish(current.uploadId) }) {
                is UiState.Success -> _state.update {
                    it.copy(batch = UiState.Success(result.data.withLocalEdits()), justPublished = count, saveFailed = false)
                }
                is UiState.Error -> _state.update { it.copy(saveFailed = true) }
                UiState.Loading -> Unit
            }
        }
    }

    private fun nextPendingAfter(cardId: String): String? {
        val cards = batch?.cards.orEmpty()
        val start = cards.indexOfFirst { it.id == cardId }
        return (1..cards.size)
            .map { cards[(start + it) % cards.size] }
            .firstOrNull { it.status == DraftStatus.Pending }
            ?.id
    }

    private fun replaceCard(card: DraftCard) = _state.update { current ->
        val data = (current.batch as? UiState.Success)?.data ?: return@update current
        current.copy(batch = UiState.Success(data.copy(cards = data.cards.map { if (it.id == card.id) card else it })))
    }

    /** Keeps unsaved edits when the server sends a fresh copy of the batch. */
    private fun ReviewBatch.withLocalEdits(): ReviewBatch {
        val local = batch?.cards.orEmpty().filter { it.id in dirty }.associateBy { it.id }
        return copy(cards = cards.map { server -> local[server.id]?.copy(status = server.status, published = server.published) ?: server })
    }

    private fun saveDirty() {
        val cards = batch?.cards.orEmpty().filter { it.id in dirty }
        dirty.clear()
        cards.forEach(::save)
    }

    private fun save(card: DraftCard) {
        val uploadId = batch?.uploadId ?: return
        viewModelScope.launch {
            val result = loadState { repository.updateCard(uploadId, card) }
            _state.update { it.copy(saveFailed = result is UiState.Error) }
            onCardsChanged()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCleared() {
        // Edits typed just before leaving the screen still reach the server.
        val uploadId = batch?.uploadId ?: return
        val cards = batch?.cards.orEmpty().filter { it.id in dirty }
        if (cards.isEmpty()) return
        GlobalScope.launch { cards.forEach { loadState { repository.updateCard(uploadId, it) } } }
    }
}
