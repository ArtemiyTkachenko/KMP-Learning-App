package org.artkachenko.kmp_learning_app.assessment_review

import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionsState

/**
 * One reviewed Question's saved state, and the learner action that changes it.
 *
 * Saved state is layered onto review content here rather than carried inside
 * [ReviewQuestionUiModel]: what the curriculum authored and what the learner saved are separate
 * truths, and only the first belongs to the review loader.
 */
internal data class ReviewSaveAction(
    val isSaved: Boolean,
    val isPending: Boolean,
    val onToggle: () -> Unit,
)

/**
 * The save affordance for [questionId], or null when there is none to offer.
 *
 * Null while saved state is loading or unreadable, because "not known to be saved" is not the same
 * as "unsaved", and a card must not present the second when only the first is true. Every review
 * surface derives its cards through this one function so the three of them cannot drift apart.
 */
internal fun SavedQuestionsState.reviewSaveAction(
    questionId: String,
    onToggleSaved: (String) -> Unit,
): ReviewSaveAction? = when (this) {
    SavedQuestionsState.Loading, SavedQuestionsState.Error -> null
    is SavedQuestionsState.Loaded -> ReviewSaveAction(
        isSaved = questionId in savedQuestionIds,
        isPending = questionId in pendingQuestionIds,
        onToggle = { onToggleSaved(questionId) },
    )
}
