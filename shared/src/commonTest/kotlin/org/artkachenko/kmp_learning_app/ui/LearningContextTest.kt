package org.artkachenko.kmp_learning_app.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.learning_progress.CurriculumCoverage
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressPolicy
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressSnapshot
import org.artkachenko.kmp_learning_app.learning_progress.RecentPerformance
import org.artkachenko.kmp_learning_app.learning_progress.SubtopicCoverage
import org.artkachenko.kmp_learning_app.learning_progress.SubtopicPerformance
import org.artkachenko.kmp_learning_app.learning_progress.TopicCoverage
import org.artkachenko.kmp_learning_app.learning_progress.TopicPerformance

internal class LearningContextTest {

    @Test
    fun evidenceMinimumIsTheWeakAreaMinimum() {
        assertFalse(LearningProgressPolicy.hasAccuracyEvidence(0))
        assertFalse(LearningProgressPolicy.hasAccuracyEvidence(4))
        assertTrue(LearningProgressPolicy.hasAccuracyEvidence(5))
        assertTrue(LearningProgressPolicy.hasAccuracyEvidence(6))
    }

    @Test
    fun neverAnsweredScopeHasNoAccuracyAndNoEvidence() {
        val context = LearningContextIndex(snapshot()).forTopic(TopicId)

        assertNull(context.accuracyPercentage)
        assertEquals(0, context.answeredCount)
        assertFalse(context.hasAccuracyEvidence)
        assertTrue(context.isUnstudied)
    }

    @Test
    fun fourAnswersKeepThePercentageButNotTheEvidence() {
        val context = LearningContextIndex(
            snapshot(topics = listOf(topicPerformance(answered = 4, correct = 4))),
        ).forTopic(TopicId)

        // The figure is still carried, so "answered" and "never answered" stay apart; it is the
        // evidence flag that keeps surfaces from drawing it.
        assertEquals(100.0, context.accuracyPercentage)
        assertEquals(4, context.answeredCount)
        assertFalse(context.hasAccuracyEvidence)
        assertFalse(context.isUnstudied)
    }

    @Test
    fun fiveAnswersAreEnoughEvidence() {
        val context = LearningContextIndex(
            snapshot(topics = listOf(topicPerformance(answered = 5, correct = 4))),
        ).forTopic(TopicId)

        assertEquals(80.0, context.accuracyPercentage)
        assertEquals(5, context.answeredCount)
        assertTrue(context.hasAccuracyEvidence)
    }

    @Test
    fun evidenceIsTheOccurrenceCountNotCoverage() {
        // One current Question answered six times: coverage says 1 attempted, evidence says 6
        // answers. The minimum is judged on answers.
        val context = LearningContextIndex(
            snapshot(
                topics = listOf(topicPerformance(answered = 6, correct = 3)),
                topicCoverage = listOf(
                    TopicCoverage(TopicId, attemptedQuestionCount = 1, totalQuestionCount = 20),
                ),
            ),
        ).forTopic(TopicId)

        assertEquals(1, context.attemptedQuestionCount)
        assertEquals(6, context.answeredCount)
        assertTrue(context.hasAccuracyEvidence)
    }

    @Test
    fun subtopicCarriesItsOwnAnswerCount() {
        val index = LearningContextIndex(
            snapshot(
                subtopics = listOf(
                    subtopicPerformance(SubtopicId, answered = 4, correct = 2),
                    subtopicPerformance(OtherSubtopicId, answered = 5, correct = 2),
                ),
                subtopicCoverage = listOf(
                    SubtopicCoverage(
                        topicId = TopicId,
                        subtopicId = SubtopicId,
                        attemptedQuestionCount = 4,
                        totalQuestionCount = 8,
                    ),
                ),
            ),
        )

        val below = index.forSubtopic(SubtopicId)
        val atMinimum = index.forSubtopic(OtherSubtopicId)
        assertEquals(4, below.answeredCount)
        assertFalse(below.hasAccuracyEvidence)
        assertEquals(5, atMinimum.answeredCount)
        assertTrue(atMinimum.hasAccuracyEvidence)
    }

    private fun topicPerformance(answered: Int, correct: Int) =
        TopicPerformance(
            topicId = TopicId,
            topicName = "Kotlin",
            answeredCount = answered,
            correctCount = correct,
            percentage = correct.toDouble() / answered * 100.0,
            isWeak = false,
        )

    private fun subtopicPerformance(subtopicId: String, answered: Int, correct: Int) =
        SubtopicPerformance(
            subtopicId = subtopicId,
            subtopicName = subtopicId,
            topicId = TopicId,
            topicName = "Kotlin",
            answeredCount = answered,
            correctCount = correct,
            percentage = correct.toDouble() / answered * 100.0,
            isWeak = false,
        )

    private fun snapshot(
        topics: List<TopicPerformance> = emptyList(),
        subtopics: List<SubtopicPerformance> = emptyList(),
        topicCoverage: List<TopicCoverage> = emptyList(),
        subtopicCoverage: List<SubtopicCoverage> = emptyList(),
    ) = LearningProgressSnapshot(
        completedAttemptCount = 0,
        answeredQuestionCount = 0,
        correctAnswerCount = 0,
        percentage = 0.0,
        topics = topics,
        subtopics = subtopics,
        weakAreas = emptyList(),
        coverage = CurriculumCoverage(attemptedQuestionCount = 0, totalQuestionCount = 0),
        topicCoverage = topicCoverage,
        subtopicCoverage = subtopicCoverage,
        recentPerformance = RecentPerformance(emptyList(), emptyList()),
    )

    private companion object {
        const val TopicId = "kotlin"
        const val SubtopicId = "kotlin_basics"
        const val OtherSubtopicId = "kotlin_generics"
    }
}
