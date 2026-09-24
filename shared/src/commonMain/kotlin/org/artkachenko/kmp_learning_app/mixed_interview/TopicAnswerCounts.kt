package org.artkachenko.kmp_learning_app.mixed_interview

import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem

/**
 * How one interview went in one Topic, counted from the transcript alone.
 *
 * Deliberately carries the stable Topic ID and no name: naming a Topic is a curriculum read, and
 * keeping it out of here is what makes the counting a pure function.
 */
internal data class TopicAnswerCounts(
    val topicId: String,
    val questionCount: Int,
    val correctCount: Int,
) {
    init {
        require(questionCount > 0) { "A counted Topic must hold at least one question." }
    }

    val percentage: Double get() = correctCount.toDouble() / questionCount * 100.0
}

/**
 * The per-Topic breakdown of an interview transcript, in the order each Topic was first asked
 * about.
 *
 * A Mixed interview is a cross-topic run, so the breakdown is the one thing its result says that a
 * Focused result cannot: which Topics carried it and which did not. The order is first-occurrence
 * rather than best-or-worst-first, because the transcript beside it is in asked order and a
 * re-sorted summary would stop lining up with it.
 *
 * [ReviewQuestionItem.Missing] is skipped: a Question the curriculum no longer holds has no Topic to
 * attribute it to, and counting it against a Topic picked from elsewhere would misreport a run the
 * learner can no longer inspect. A Topic every one of whose Questions has gone is therefore absent
 * rather than present at 0 of 0 — which is also why [TopicAnswerCounts] can require a positive
 * denominator instead of having to represent an empty one.
 *
 * Pure by construction: this used to be a suspending method on the result ViewModel, interleaved
 * with the per-Topic name lookups, with a mutable counter class behind it. Nothing here reads a
 * repository, so the aggregation is testable on its own and the ViewModel is left with only the
 * name resolution it genuinely needs one for.
 */
internal fun List<ReviewQuestionItem>.topicAnswerCounts(): List<TopicAnswerCounts> {
    val questionCounts = linkedMapOf<String, Int>()
    val correctCounts = mutableMapOf<String, Int>()
    forEach { item ->
        val question = (item as? ReviewQuestionItem.Available)?.question ?: return@forEach
        questionCounts[question.topicId] = (questionCounts[question.topicId] ?: 0) + 1
        if (question.isCorrect) {
            correctCounts[question.topicId] = (correctCounts[question.topicId] ?: 0) + 1
        }
    }
    return questionCounts.map { (topicId, questionCount) ->
        TopicAnswerCounts(
            topicId = topicId,
            questionCount = questionCount,
            correctCount = correctCounts[topicId] ?: 0,
        )
    }
}
