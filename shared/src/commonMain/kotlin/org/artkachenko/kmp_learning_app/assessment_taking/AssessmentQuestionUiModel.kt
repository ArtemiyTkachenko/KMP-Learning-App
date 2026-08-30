package org.artkachenko.kmp_learning_app.assessment_taking

import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode

internal data class AssessmentQuestionUiModel(
    val id: String,
    val text: String,
    val answers: List<AnswerOption>,
    val selectionMode: AnswerSelectionMode,
)
