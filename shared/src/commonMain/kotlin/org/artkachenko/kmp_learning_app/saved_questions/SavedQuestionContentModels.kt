package org.artkachenko.kmp_learning_app.saved_questions

import org.artkachenko.kmp_learning_app.assessment_review.ReviewSourceUiModel

/**
 * One saved identity, together with whatever the curriculum can currently say about it.
 *
 * [savedQuestion] is present on both branches because the saved identity is the learner's own data
 * and stays valid whether or not content resolves. A [Missing] entry is therefore still a real,
 * removable row rather than something to hide or delete.
 */
internal sealed interface SavedQuestionItem {
    val savedQuestion: SavedQuestion

    val questionId: String get() = savedQuestion.questionId

    data class Available(
        override val savedQuestion: SavedQuestion,
        val question: SavedQuestionContentUiModel,
    ) : SavedQuestionItem

    data class Missing(
        override val savedQuestion: SavedQuestion,
    ) : SavedQuestionItem
}

/**
 * A saved Question as authored content, with no assessment occurrence attached.
 *
 * Deliberately not `ReviewQuestionUiModel`: that model describes one historical attempt, so it
 * carries `isCorrect` and a selected flag per answer. A saved Question has no attempt — the learner
 * may have saved it after answering correctly, incorrectly, or from any review path — and E18-03
 * does not define which attempt would represent it. Rather than invent one, this model carries only
 * what is true of the Question itself.
 */
internal data class SavedQuestionContentUiModel(
    val questionId: String,
    val text: String,
    val answers: List<SavedQuestionAnswerUiModel>,
    val explanation: String,
    val sources: List<ReviewSourceUiModel>,
)

/** [isCorrectAnswer] is authored curriculum content, not a statement about any learner answer. */
internal data class SavedQuestionAnswerUiModel(
    val id: String,
    val text: String,
    val isCorrectAnswer: Boolean,
)
