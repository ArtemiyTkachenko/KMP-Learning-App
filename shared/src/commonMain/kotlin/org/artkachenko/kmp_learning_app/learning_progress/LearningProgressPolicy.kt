package org.artkachenko.kmp_learning_app.learning_progress

internal object LearningProgressPolicy {
    const val WeakAccuracyThresholdPercentage = 70.0
    const val WeakTopicMinimumAnswered = 3
    const val WeakSubtopicMinimumAnswered = 2

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
