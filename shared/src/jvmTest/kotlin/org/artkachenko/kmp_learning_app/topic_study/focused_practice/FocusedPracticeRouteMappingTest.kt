package org.artkachenko.kmp_learning_app.topic_study.focused_practice

import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.AppRoute
import org.artkachenko.kmp_learning_app.assessment.AllQuestionLevels
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel

internal class FocusedPracticeRouteMappingTest {
    @Test
    fun topicRouteReconstructsFocusedTopicConfig() {
        val config = AppRoute.FocusedTopicPractice(
            topicId = "topic_a",
            questionCount = 10,
            levels = listOf(QuestionLevel.APPLIED, QuestionLevel.ADVANCED),
            source = PracticeQuestionSource.ALL,
        ).toAssessmentConfig()

        assertEquals(AssessmentScope.Topic("topic_a"), config.scope)
        assertEquals(10, config.questionCount)
        assertEquals(setOf(QuestionLevel.APPLIED, QuestionLevel.ADVANCED), config.levels)
        assertEquals(PracticeQuestionSource.ALL, config.source)
    }

    @Test
    fun subtopicRouteReconstructsFocusedSubtopicConfig() {
        val config = AppRoute.FocusedSubtopicPractice(
            subtopicId = "subtopic_a",
            questionCount = 6,
            levels = listOf(QuestionLevel.FOUNDATION),
            source = PracticeQuestionSource.ALL,
        ).toAssessmentConfig()

        assertEquals(AssessmentScope.Subtopic("subtopic_a"), config.scope)
        assertEquals(6, config.questionCount)
        assertEquals(setOf(QuestionLevel.FOUNDATION), config.levels)
        assertEquals(PracticeQuestionSource.ALL, config.source)
    }

    /**
     * The route carries the selection rather than relying on the constructor defaults, so a
     * narrowed run cannot arrive at assessment taking widened back to every level.
     */
    @Test
    fun aNarrowedRouteDoesNotReconstructAsAllLevels() {
        val config = AppRoute.FocusedTopicPractice(
            topicId = "topic_a",
            questionCount = 10,
            levels = listOf(QuestionLevel.ADVANCED),
            source = PracticeQuestionSource.ALL,
        ).toAssessmentConfig()

        assertEquals(setOf(QuestionLevel.ADVANCED), config.levels)
        assertEquals(AllQuestionLevels.size, 3)
    }

    @Test
    fun existingPracticeRouteCarriesOnlyStableAttemptId() {
        val route = AppRoute.FocusedPracticeAttempt("retake-1")

        assertEquals("retake-1", route.attemptId)
    }
}
