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

/**
 * Whether this item is [questionId] as content the learner can actually act on.
 *
 * The mutation boundary of every review surface: a save action may only be honoured for a Question
 * the surface is currently showing as available, so persistence can never record an identity whose
 * content the learner has no way to reach. The Focused result, the Mixed result, and the Mistake
 * queue each stated this rule for themselves, over three different traversals of the same type; one
 * predicate is what keeps the three surfaces from drifting on which saves they accept.
 */
internal fun ReviewQuestionItem.isAvailableFor(questionId: String): Boolean =
    this is ReviewQuestionItem.Available && question.questionId == questionId
