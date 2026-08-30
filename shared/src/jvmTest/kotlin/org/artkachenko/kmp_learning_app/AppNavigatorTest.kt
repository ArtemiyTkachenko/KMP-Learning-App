package org.artkachenko.kmp_learning_app

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class AppNavigatorTest {
    private fun navigator(): AppNavigator =
        AppNavigator(
            AppTopLevelDestination.entries.associateWith { mutableListOf<NavKey>(it.route) },
        )

    @Test
    fun eachAreaKeepsItsOwnDetailWhenTheLearnerSwitchesAway() {
        val navigator = navigator()

        navigator.push(AppRoute.Topic("lifecycle"))
        assertEquals(AppRoute.Topic("lifecycle"), navigator.currentRoute)

        navigator.select(AppTopLevelDestination.PROGRESS)
        assertEquals(AppRoute.Progress, navigator.currentRoute)

        // Returning to Topics resumes the topic that was open, rather than dropping the learner
        // back at the list as a single shared stack did.
        navigator.select(AppTopLevelDestination.TOPICS)
        assertEquals(AppRoute.Topic("lifecycle"), navigator.currentRoute)
    }

    @Test
    fun reselectingTheCurrentAreaReturnsItToItsRoot() {
        val navigator = navigator()
        navigator.push(AppRoute.Topic("lifecycle"))

        navigator.select(AppTopLevelDestination.TOPICS)

        assertEquals(AppRoute.Topics, navigator.currentRoute)
        assertEquals(1, navigator.backStack.size)
    }

    @Test
    fun backLeavesTheDetailThenTheAreaThenGivesUp() {
        val navigator = navigator()
        navigator.select(AppTopLevelDestination.PROGRESS)
        navigator.push(AppRoute.ProgressTopic("kotlin"))

        assertTrue(navigator.popBack())
        assertEquals(AppRoute.Progress, navigator.currentRoute)

        // From a secondary area back returns to the start area; without this the navigation
        // control was the only way out of Progress, Interview, or Mistakes.
        assertTrue(navigator.popBack())
        assertEquals(AppTopLevelDestination.TOPICS, navigator.area)

        // Nothing left to consume, so the host closes the app.
        assertFalse(navigator.popBack())
    }

    @Test
    fun backOutOfAnAreaLeavesThatAreaWhereItWas() {
        val navigator = navigator()
        navigator.select(AppTopLevelDestination.PROGRESS)
        navigator.push(AppRoute.ProgressTopic("kotlin"))

        navigator.select(AppTopLevelDestination.TOPICS)
        navigator.select(AppTopLevelDestination.PROGRESS)

        assertEquals(AppRoute.ProgressTopic("kotlin"), navigator.currentRoute)
    }

    @Test
    fun replacingTheTopKeepsTheEntryBelowIt() {
        val navigator = navigator()
        navigator.select(AppTopLevelDestination.INTERVIEW)
        navigator.push(AppRoute.MixedInterview(questionCount = 20))

        navigator.replaceTop(AppRoute.MixedInterviewAttempt("attempt"))

        assertEquals(
            listOf<NavKey>(AppRoute.Interview, AppRoute.MixedInterviewAttempt("attempt")),
            navigator.backStack,
        )
    }

    @Test
    fun browsingScreensKeepAreaNavigationAndAssessmentScreensHideIt() {
        listOf(
            AppRoute.Topics,
            AppRoute.Interview,
            AppRoute.Progress,
            AppRoute.MistakeReview,
            AppRoute.Topic("t"),
            AppRoute.ProgressTopic("t"),
        ).forEach { assertTrue(it.showsAreaNavigation(), "$it should keep area navigation") }

        listOf(
            AppRoute.MixedInterview(20),
            AppRoute.MixedInterviewAttempt("a"),
            AppRoute.MixedInterviewResult("a"),
            AppRoute.FocusedTopicPractice("t", 10),
            AppRoute.FocusedSubtopicPractice("s", 10),
            AppRoute.FocusedPracticeAttempt("a"),
            AppRoute.FocusedPracticeResult("a"),
        ).forEach { assertFalse(it.showsAreaNavigation(), "$it should hide area navigation") }
    }
}
