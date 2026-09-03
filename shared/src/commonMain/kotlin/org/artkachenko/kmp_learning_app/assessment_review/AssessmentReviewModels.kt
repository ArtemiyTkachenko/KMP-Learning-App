package org.artkachenko.kmp_learning_app.assessment_review

internal sealed interface ReviewQuestionItem {
    data class Available(val question: ReviewQuestionUiModel) : ReviewQuestionItem
    data class Missing(val questionId: String) : ReviewQuestionItem
}

/**
 * [topicId] and [subtopicId] are the authored scope the Question belongs to, copied verbatim from
 * the curriculum. They are not displayed; they let a review surface offer an action for a scope it
 * can already name, without any surface having to look the Question up again.
 */
internal data class ReviewQuestionUiModel(
    val questionId: String,
    val topicId: String,
    val subtopicId: String,
    val text: String,
    val isCorrect: Boolean,
    val answers: List<ReviewAnswerUiModel>,
    val explanation: String,
    val sources: List<ReviewSourceUiModel>,
)

internal data class ReviewAnswerUiModel(
    val id: String,
    val text: String,
    val wasSelected: Boolean,
    val isCorrectAnswer: Boolean,
)

internal data class ReviewSourceUiModel(
    val title: String,
    val url: String,
)
