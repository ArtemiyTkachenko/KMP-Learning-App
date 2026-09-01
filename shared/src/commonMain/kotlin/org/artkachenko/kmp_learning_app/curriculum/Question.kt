package org.artkachenko.kmp_learning_app.curriculum

import kotlinx.serialization.Serializable

@Serializable
internal data class Question(
    val id: String,
    val topicId: String,
    val subtopicId: String,
    val text: String,
    val answers: List<AnswerOption>,
    val selectionMode: AnswerSelectionMode,
    val level: QuestionLevel,
    val correctAnswerIds: List<String>,
    val explanation: String,
    val sources: List<SourceReference>,
    val status: ContentStatus = ContentStatus.ACTIVE,
)
