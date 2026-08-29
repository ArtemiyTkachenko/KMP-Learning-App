package org.artkachenko.kmp_learning_app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

internal class AppNavigationTest {
    @Test
    fun persistedMixedAttemptReplacesConfigEntry() {
        val backStack = mutableListOf<AppRoute>(
            AppRoute.Topics,
            AppRoute.MixedInterview(questionCount = 20),
        )

        backStack.replaceTopWith(AppRoute.MixedInterviewAttempt("mixed-attempt"))

        assertEquals(
            listOf(AppRoute.Topics, AppRoute.MixedInterviewAttempt("mixed-attempt")),
            backStack,
        )
    }

    @Test
    fun persistedFocusedAttemptReplacesConfigEntry() {
        val backStack = mutableListOf<AppRoute>(
            AppRoute.Topics,
            AppRoute.Topic("topic"),
            AppRoute.FocusedTopicPractice("topic", questionCount = 10),
        )

        backStack.replaceTopWith(AppRoute.FocusedPracticeAttempt("focused-attempt"))

        assertEquals(
            listOf(
                AppRoute.Topics,
                AppRoute.Topic("topic"),
                AppRoute.FocusedPracticeAttempt("focused-attempt"),
            ),
            backStack,
        )
    }

    @Test
    fun completionReplacesOnlyPersistedAttemptEntry() {
        val backStack = mutableListOf<AppRoute>(
            AppRoute.Topics,
            AppRoute.MixedInterviewAttempt("mixed-attempt"),
        )

        backStack.replaceTopWith(AppRoute.MixedInterviewResult("mixed-attempt"))

        assertEquals(
            listOf(AppRoute.Topics, AppRoute.MixedInterviewResult("mixed-attempt")),
            backStack,
        )
    }

    @Test
    fun mixedRetakePushesStableAttemptAndCompletionPreservesSourceResult() {
        val backStack = mutableListOf<AppRoute>(
            AppRoute.Topics,
            AppRoute.MixedInterviewResult("source"),
        )

        backStack.add(AppRoute.MixedInterviewAttempt("retake"))

        assertEquals(
            listOf(
                AppRoute.Topics,
                AppRoute.MixedInterviewResult("source"),
                AppRoute.MixedInterviewAttempt("retake"),
            ),
            backStack,
        )

        backStack.replaceTopWith(AppRoute.MixedInterviewResult("retake"))

        assertEquals(
            listOf(
                AppRoute.Topics,
                AppRoute.MixedInterviewResult("source"),
                AppRoute.MixedInterviewResult("retake"),
            ),
            backStack,
        )
    }

    @Test
    fun mistakeReviewPushesFromProgressAndPopsBackToIt() {
        val backStack = mutableListOf<AppRoute>(AppRoute.Topics, AppRoute.Progress)

        backStack.add(AppRoute.MistakeReview)
        assertEquals(
            listOf(AppRoute.Topics, AppRoute.Progress, AppRoute.MistakeReview),
            backStack,
        )

        backStack.removeAt(backStack.lastIndex)
        assertEquals(AppRoute.Progress, backStack.last())
    }

    @Test
    fun mistakeReviewRouteCarriesNoDerivedData() {
        // The queue is derived from complete history, so the route needs no arguments and must
        // never carry question IDs, attempts, or review models.
        val route: AppRoute = AppRoute.MistakeReview

        assertSame(AppRoute.MistakeReview, route)
    }

    @Test
    fun progressTopicDrillDownPushesStableTopicIdAndPopsBackToProgress() {
        val backStack = mutableListOf<AppRoute>(AppRoute.Topics, AppRoute.Progress)

        backStack.add(AppRoute.ProgressTopic("topic_kotlin"))
        assertEquals(
            listOf(AppRoute.Topics, AppRoute.Progress, AppRoute.ProgressTopic("topic_kotlin")),
            backStack,
        )

        backStack.removeAt(backStack.lastIndex)
        assertEquals(AppRoute.Progress, backStack.last())
    }

    @Test
    fun progressTopicRouteCarriesOnlyStableTopicIdentity() {
        val route = AppRoute.ProgressTopic(topicId = "topic_stable_id")

        assertEquals("topic_stable_id", route.topicId)
        assertEquals(AppRoute.ProgressTopic("topic_stable_id"), route)
    }

    @Test
    fun progressAndHistoricalResultsPushStableRoutesAndPopBackToProgress() {
        val backStack = mutableListOf<AppRoute>(AppRoute.Topics)

        backStack.add(AppRoute.Progress)
        assertEquals(listOf(AppRoute.Topics, AppRoute.Progress), backStack)

        backStack.add(AppRoute.MixedInterviewResult("mixed-history"))
        assertEquals(
            listOf(
                AppRoute.Topics,
                AppRoute.Progress,
                AppRoute.MixedInterviewResult("mixed-history"),
            ),
            backStack,
        )
        backStack.removeAt(backStack.lastIndex)
        assertEquals(AppRoute.Progress, backStack.last())

        backStack.add(AppRoute.FocusedPracticeResult("focused-history"))
        assertEquals(
            listOf(
                AppRoute.Topics,
                AppRoute.Progress,
                AppRoute.FocusedPracticeResult("focused-history"),
            ),
            backStack,
        )
    }
}
