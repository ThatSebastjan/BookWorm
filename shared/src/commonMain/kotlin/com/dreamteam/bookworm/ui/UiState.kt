package com.dreamteam.bookworm.ui

import kotlinx.coroutines.CancellationException

/** Screen state for anything loaded from a repository. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Error(val cause: Throwable) : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
}

/** Runs [block] and wraps the outcome, letting coroutine cancellation through. */
suspend fun <T> loadState(block: suspend () -> T): UiState<T> = try {
    UiState.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    UiState.Error(e)
}
