package org.artkachenko.kmp_learning_app.learning_progress

internal object LearningProgressPolicy {
    const val WeakAccuracyThresholdPercentage = 70.0
    /** A recommendation should be based on a pattern, not one unlucky question. */
    const val WeakAreaMinimumAnswered = 5
    const val WeakTopicMinimumAnswered = WeakAreaMinimumAnswered
    const val WeakSubtopicMinimumAnswered = WeakAreaMinimumAnswered

    /**
     * Whether [answeredCount] answers in one Topic or Subtopic are enough to state an accuracy
     * figure for it at all. The same minimum as the weak verdict, for the same reason: below it a
     * percentage describes a few questions rather than the learner's standing in the scope.
     *
     * [answeredCount] is occurrence-based — every answer recorded in the scope — and never the
     * coverage count of unique current Questions.
     */
    fun hasAccuracyEvidence(answeredCount: Int): Boolean =
        answeredCount >= WeakAreaMinimumAnswered

    fun isWeakTopic(
        answeredCount: Int,
        percentage: Double,
    ): Boolean =
        answeredCount >= WeakTopicMinimumAnswered &&
            percentage < WeakAccuracyThresholdPercentage

    fun isWeakSubtopic(
        answeredCount: Int,
        percentage: Double,
    ): Boolean =
        answeredCount >= WeakSubtopicMinimumAnswered &&
            percentage < WeakAccuracyThresholdPercentage
}
