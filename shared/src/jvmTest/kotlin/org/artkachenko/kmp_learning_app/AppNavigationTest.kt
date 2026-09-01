package org.artkachenko.kmp_learning_app

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertSame
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel

internal class AppNavigationTest {
    @Test
    fun topicRoutesCarryStableIdentityWithOptionalSubtopicTarget() {
        assertEquals(
            AppRoute.Topic(topicId = "topic_id", subtopicId = null),
            AppRoute.Topic(topicId = "topic_id"),
        )
        assertEquals(
            AppRoute.Topic(topicId = "topic_id", subtopicId = "subtopic_id"),
            AppRoute.Topic("topic_id", "subtopic_id"),
        )
    }

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
            AppRoute.PracticeBuilderTopic("topic"),
            focusedTopicPractice(),
        )

        backStack.replaceTopWith(AppRoute.FocusedPracticeAttempt("focused-attempt"))

        // The builder stays on the stack, so backing out of a practice run returns to the setup
        // the learner configured rather than all the way to the Topic.
        assertEquals(
            listOf(
                AppRoute.Topics,
                AppRoute.Topic("topic"),
                AppRoute.PracticeBuilderTopic("topic"),
                AppRoute.FocusedPracticeAttempt("focused-attempt"),
            ),
            backStack,
        )
    }

    @Test
    fun topicPracticeOpensTheBuilderBeforeAnyAssessment() {
        val backStack = mutableListOf<AppRoute>(AppRoute.Topics, AppRoute.Topic("topic"))

        backStack.add(AppRoute.PracticeBuilderTopic("topic"))
        backStack.add(focusedTopicPractice())

        assertEquals(
            listOf(
                AppRoute.Topics,
                AppRoute.Topic("topic"),
                AppRoute.PracticeBuilderTopic("topic"),
                focusedTopicPractice(),
            ),
            backStack,
        )
    }

    @Test
    fun practiceBuilderRoutesCarryOnlyStableScopeIdentity() {
        assertEquals(AppRoute.PracticeBuilderTopic("topic_stable"), AppRoute.PracticeBuilderTopic("topic_stable"))
        assertEquals(
            AppRoute.PracticeBuilderSubtopic("subtopic_stable"),
            AppRoute.PracticeBuilderSubtopic("subtopic_stable"),
        )
    }

    private fun focusedTopicPractice(): AppRoute.FocusedTopicPractice =
        AppRoute.FocusedTopicPractice(
            topicId = "topic",
            questionCount = 10,
            levels = listOf(QuestionLevel.ADVANCED),
            source = PracticeQuestionSource.ALL,
        )

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
