package org.artkachenko.kmp_learning_app.mixed_interview

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.assessment_review.ReviewAnswerUiModel
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionUiModel

/**
 * The per-Topic breakdown of an interview, derived from the transcript alone.
 *
 * This used to be a suspending method on the result ViewModel interleaved with curriculum lookups,
 * so counting could only be checked through a ViewModel, two repositories and a review loader.
 * Nothing here needs any of them.
 */
internal class TopicAnswerCountsTest {

    @Test
    fun topicsAppearInFirstAskedOrderNotInScoreOrder() {
        val counts = listOf(
            item("q1", topicId = "topic-b", isCorrect = false),
            item("q2", topicId = "topic-a", isCorrect = true),
            item("q3", topicId = "topic-b", isCorrect = true),
            item("q4", topicId = "topic-c", isCorrect = true),
        ).topicAnswerCounts()

        // topic-a and topic-c both scored 100%, topic-b 50%; the order still follows the
        // transcript, which is the list rendered beside this breakdown.
        assertEquals(listOf("topic-b", "topic-a", "topic-c"), counts.map { it.topicId })
    }

    @Test
    fun eachTopicCountsItsOwnQuestionsAndItsOwnCorrectAnswers() {
        val counts = listOf(
            item("q1", topicId = "topic-a", isCorrect = true),
            item("q2", topicId = "topic-a", isCorrect = false),
            item("q3", topicId = "topic-a", isCorrect = true),
            item("q4", topicId = "topic-b", isCorrect = false),
        ).topicAnswerCounts()

        assertEquals(
            listOf(
                TopicAnswerCounts("topic-a", questionCount = 3, correctCount = 2),
                TopicAnswerCounts("topic-b", questionCount = 1, correctCount = 0),
            ),
            counts,
        )
        assertEquals(listOf(2.0 / 3 * 100.0, 0.0), counts.map { it.percentage })
    }

    /**
     * A Question the curriculum no longer holds has no Topic to attribute it to. Counting it
     * anywhere would misreport a run the learner can no longer inspect, so it is left out of the
     * breakdown entirely — the overall score above it still counts it, because that is persisted
     * with the attempt rather than derived from current content.
     */
    @Test
    fun aQuestionWhoseContentIsGoneIsCountedNowhere() {
        val counts = listOf(
            item("q1", topicId = "topic-a", isCorrect = true),
            ReviewQuestionItem.Missing("q2"),
        ).topicAnswerCounts()

        assertEquals(listOf(TopicAnswerCounts("topic-a", 1, 1)), counts)
    }

    @Test
    fun aTranscriptWithNoResolvableContentProducesNoBreakdownRatherThanAnEmptyTopic() {
        assertTrue(listOf(ReviewQuestionItem.Missing("q1")).topicAnswerCounts().isEmpty())
        assertTrue(emptyList<ReviewQuestionItem>().topicAnswerCounts().isEmpty())
    }

    /** The invalid state the derivation above cannot produce is also unconstructible by hand. */
    @Test
    fun aCountedTopicCannotHaveAnEmptyDenominator() {
        assertFailsWith<IllegalArgumentException> {
            TopicAnswerCounts(topicId = "topic-a", questionCount = 0, correctCount = 0)
        }
    }
}

private fun item(
    questionId: String,
    topicId: String,
    isCorrect: Boolean,
): ReviewQuestionItem = ReviewQuestionItem.Available(
    ReviewQuestionUiModel(
        questionId = questionId,
        topicId = topicId,
        subtopicId = "subtopic",
        text = "$questionId?",
        isCorrect = isCorrect,
        answers = listOf(
            ReviewAnswerUiModel("a", "A", wasSelected = isCorrect, isCorrectAnswer = true),
        ),
        explanation = "Explanation",
        sources = emptyList(),
    ),
)
