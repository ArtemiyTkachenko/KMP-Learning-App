package org.artkachenko.kmp_learning_app.mistake_review

internal sealed interface MistakeReviewUiState {
    data object Loading : MistakeReviewUiState

    data object Empty : MistakeReviewUiState

    data object Error : MistakeReviewUiState

    data class Content(
        val mistakes: List<UnresolvedMistake>,
    ) : MistakeReviewUiState
}
