package org.artkachenko.kmp_learning_app.topic_study.focused_practice

import org.artkachenko.kmp_learning_app.curriculum.AnswerOption

internal data class FocusedQuestionUiModel(
    val id: String,
    val text: String,
    val answers: List<AnswerOption>,
    val selectionMode: AnswerSelectionMode,
)
