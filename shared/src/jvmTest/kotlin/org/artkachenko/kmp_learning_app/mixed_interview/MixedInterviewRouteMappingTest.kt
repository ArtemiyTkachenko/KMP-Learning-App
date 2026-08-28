package org.artkachenko.kmp_learning_app.mixed_interview

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.artkachenko.kmp_learning_app.AppRoute
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingLaunch

internal class MixedInterviewRouteMappingTest {
    @Test
    fun startRouteUsesTheMvpQuestionCountAsPrimitiveData() {
        val route = mixedInterviewStartRoute()

        assertEquals(20, route.questionCount)
        assertEquals(MixedInterviewDefaults.QuestionCount, route.questionCount)
    }

    @Test
    fun routeReconstructsMixedAssessmentConfig() {
        val route = AppRoute.MixedInterview(
            questionCount = MixedInterviewDefaults.QuestionCount,
        )

        val config = route.toAssessmentConfig()

        assertIs<AssessmentConfig.Mixed>(config)
        assertEquals(route.questionCount, config.questionCount)
    }

    @Test
    fun resultRouteCarriesOnlyStableAttemptIdentity() {
        val route = AppRoute.MixedInterviewResult(attemptId = "attempt-mixed")

        assertEquals("attempt-mixed", route.attemptId)
    }

    @Test
    fun persistedAttemptRouteLoadsExistingAssessmentByStableIdentity() {
        val route = AppRoute.MixedInterviewAttempt(attemptId = "attempt-mixed")

        assertEquals(
            AssessmentTakingLaunch.ExistingAttempt("attempt-mixed"),
            route.toAssessmentTakingLaunch(),
        )
    }
}
