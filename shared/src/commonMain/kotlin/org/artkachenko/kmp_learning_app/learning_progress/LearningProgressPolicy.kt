package org.artkachenko.kmp_learning_app.learning_progress

internal object LearningProgressPolicy {
    const val WeakAccuracyThresholdPercentage = 70.0
    /** A recommendation should be based on a pattern, not one unlucky question. */
    const val WeakAreaMinimumAnswered = 5
    const val WeakTopicMinimumAnswered = WeakAreaMinimumAnswered
    const val WeakSubtopicMinimumAnswered = WeakAreaMinimumAnswered

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
