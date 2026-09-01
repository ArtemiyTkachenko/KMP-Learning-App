package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.ui.LearningContextUiModel

internal data class SubtopicPracticeItem(
    val subtopic: Subtopic,
    /**
     * Authored ACTIVE questions under this Subtopic, which is what decides whether it can be
     * practised at all. Coverage carries the same total for display; this one gates the row.
     */
    val questionCount: Int,
    /**
     * Coverage and accuracy for this Subtopic, or `null` when analytics have not loaded or could
     * not be derived. A null here is unknown history, never "never studied": practice stays
     * available either way.
     */
    val learningContext: LearningContextUiModel? = null,
)
