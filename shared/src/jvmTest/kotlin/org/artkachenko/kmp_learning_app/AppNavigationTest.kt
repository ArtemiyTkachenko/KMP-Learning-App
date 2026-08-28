package org.artkachenko.kmp_learning_app

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
