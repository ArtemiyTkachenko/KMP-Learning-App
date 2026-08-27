package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.AppRoute
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope

internal class FocusedPracticeRouteTest {
    @Test
    fun topicConfigMapsToStableTopicRoute() {
        val route = AssessmentConfig.Focused(
            scope = AssessmentScope.Topic("topic_a"),
            questionCount = FocusedPracticeQuestionCount,
        ).toAppRoute()

        assertEquals(
            AppRoute.FocusedTopicPractice("topic_a", FocusedPracticeQuestionCount),
            route,
        )
    }

    @Test
    fun subtopicConfigMapsToStableSubtopicRoute() {
        val route = AssessmentConfig.Focused(
            scope = AssessmentScope.Subtopic("subtopic_a"),
            questionCount = FocusedPracticeQuestionCount,
        ).toAppRoute()

        assertEquals(
            AppRoute.FocusedSubtopicPractice("subtopic_a", FocusedPracticeQuestionCount),
            route,
        )
    }
}
