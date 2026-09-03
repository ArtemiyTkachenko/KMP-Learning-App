package org.artkachenko.kmp_learning_app.saved_questions

/**
 * What the Saved Questions screen shows.
 *
 * Deliberately not [SavedQuestionsState] itself: that state knows saved identity and which
 * mutations are in flight, but nothing about resolved Question content. This composes the two
 * without changing either.
 *
 * [Error] is a screen state here, unlike on the result surfaces where saving is optional
 * enrichment: saved identity is this destination's primary data, so a saved table that cannot be
 * read leaves nothing to browse.
 */
internal sealed interface SavedQuestionsUiState {
    data object Loading : SavedQuestionsUiState

    data object Empty : SavedQuestionsUiState

    data object Error : SavedQuestionsUiState

    /**
     * [items] is in the repository's saved order, preserved through resolution.
     * [pendingQuestionIds] comes from the shared holder unchanged, so only the Question whose
     * removal is being persisted loses its action.
     */
    data class Content(
        val items: List<SavedQuestionItem>,
        val pendingQuestionIds: Set<String> = emptySet(),
    ) : SavedQuestionsUiState
}
