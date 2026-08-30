package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import org.artkachenko.kmp_learning_app.curriculum.Subtopic

internal data class SubtopicPracticeItem(
    val subtopic: Subtopic,
    val questionCount: Int,
    /** Observed accuracy from completed history, or null when this subtopic was never answered. */
    val accuracyPercentage: Double? = null,
)
