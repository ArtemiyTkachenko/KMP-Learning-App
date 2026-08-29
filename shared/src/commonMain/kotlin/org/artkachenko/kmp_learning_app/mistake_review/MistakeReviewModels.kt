package org.artkachenko.kmp_learning_app.mistake_review

import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem

/**
 * A Question whose most recent completed occurrence was answered incorrectly.
 *
 * [sourceAttemptId] identifies the attempt that occurrence came from. It is not user-visible; it
 * exists so the latest-occurrence selection stays provable in tests and debuggable later.
 */
internal data class UnresolvedMistake(
    val questionId: String,
    val sourceAttemptId: String,
    val reviewItem: ReviewQuestionItem,
)
