package org.artkachenko.kmp_learning_app.topic_study.focused_practice

import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.AppRoute
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope

internal class FocusedPracticeRouteMappingTest {
    @Test
    fun topicRouteReconstructsFocusedTopicConfig() {
        val config = AppRoute.FocusedTopicPractice("topic_a", 10).toAssessmentConfig()

        assertEquals(AssessmentScope.Topic("topic_a"), config.scope)
        assertEquals(10, config.questionCount)
    }

    @Test
    fun subtopicRouteReconstructsFocusedSubtopicConfig() {
        val config = AppRoute.FocusedSubtopicPractice("subtopic_a", 6).toAssessmentConfig()

        assertEquals(AssessmentScope.Subtopic("subtopic_a"), config.scope)
        assertEquals(6, config.questionCount)
    }

    @Test
    fun existingPracticeRouteCarriesOnlyStableAttemptId() {
        val route = AppRoute.FocusedPracticeAttempt("retake-1")

        assertEquals("retake-1", route.attemptId)
    }
}
