package org.artkachenko.kmp_learning_app.mistake_review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class MistakeReviewViewModel(
    private val mistakeReviewService: MistakeReviewService,
) : ViewModel() {
    private val _uiState = MutableStateFlow<MistakeReviewUiState>(MistakeReviewUiState.Loading)
    val uiState: StateFlow<MistakeReviewUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.value = MistakeReviewUiState.Loading
        loadJob = viewModelScope.launch {
            try {
                // The service already orders the queue by most recent unresolved occurrence, so
                // presentation preserves that list exactly.
                val mistakes = mistakeReviewService.load()
                _uiState.value = if (mistakes.isEmpty()) {
                    MistakeReviewUiState.Empty
                } else {
                    MistakeReviewUiState.Content(mistakes)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                _uiState.value = MistakeReviewUiState.Error
            }
        }
    }
}
