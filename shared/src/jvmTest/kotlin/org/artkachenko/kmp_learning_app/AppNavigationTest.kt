package org.artkachenko.kmp_learning_app

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertSame
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingTarget
import org.artkachenko.kmp_learning_app.guided_learning.LearningRecommendationTarget
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset

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

    @Test
    fun continuingATopicContextReachesTheExistingTopicDetailRoute() {
        assertEquals(
            AppRoute.Topic(topicId = "kotlin", subtopicId = null),
            ContinueStudyingTarget.Topic("kotlin").toAppRoute(),
        )
    }

    @Test
    fun continuingASubtopicContextReachesTheSameTopicRouteOpenedAtTheSubtopic() {
        assertEquals(
            AppRoute.Topic(topicId = "kotlin", subtopicId = "coroutines"),
            ContinueStudyingTarget.Topic("kotlin", "coroutines").toAppRoute(),
        )
    }

    @Test
    fun continuingTargetedPracticeReachesTheBuilderCarryingItsSource() {
        assertEquals(
            AppRoute.PracticeBuilderTopic("kotlin", PracticeQuestionSource.WEAK_AREAS),
            ContinueStudyingTarget.Practice(
                PracticePreset(
                    scope = AssessmentScope.Topic("kotlin"),
                    source = PracticeQuestionSource.WEAK_AREAS,
                ),
            ).toAppRoute(),
        )
        assertEquals(
            AppRoute.PracticeBuilderSubtopic("coroutines", PracticeQuestionSource.UNSEEN),
            ContinueStudyingTarget.Practice(
                PracticePreset(
                    scope = AssessmentScope.Subtopic("coroutines"),
                    source = PracticeQuestionSource.UNSEEN,
                ),
            ).toAppRoute(),
        )
    }

    /**
     * The rule this whole feature depends on: continuing recent study returns to a *learning
     * context*, so no Continue target may ever produce a route that reopens a stored attempt.
     */
    @Test
    fun noContinueTargetCanReachAnExistingAttempt() {
        val routes = listOf(
            ContinueStudyingTarget.Topic("kotlin"),
            ContinueStudyingTarget.Topic("kotlin", "coroutines"),
            ContinueStudyingTarget.Practice(
                PracticePreset(AssessmentScope.Topic("kotlin"), PracticeQuestionSource.UNSEEN),
            ),
            ContinueStudyingTarget.Practice(
                PracticePreset(
                    AssessmentScope.Subtopic("coroutines"),
                    PracticeQuestionSource.UNRESOLVED_MISTAKES,
                ),
            ),
        ).map(ContinueStudyingTarget::toAppRoute)

        routes.forEach { route ->
            assertFalse(route is AppRoute.FocusedPracticeAttempt, "$route resumes an attempt")
            assertFalse(route is AppRoute.MixedInterviewAttempt, "$route resumes an attempt")
            // Nor a finished one, and nor a run configured to start immediately.
            assertFalse(route is AppRoute.FocusedPracticeResult, "$route reopens a result")
            assertFalse(route is AppRoute.MixedInterviewResult, "$route reopens a result")
            assertFalse(route is AppRoute.FocusedTopicPractice, "$route starts an assessment")
            assertFalse(route is AppRoute.FocusedSubtopicPractice, "$route starts an assessment")
            assertFalse(route is AppRoute.MixedInterview, "$route starts an assessment")
        }
    }

    @Test
    fun aNewLearnerRecommendationSelectsTopicsWithoutChoosingATopic() {
        assertEquals(AppRoute.Topics, LearningRecommendationTarget.Topics.toAppRoute())

        val navigator = navigator()
        navigator.push(AppRoute.Topic("kotlin"))
        navigator.openRecommendation(LearningRecommendationTarget.Topics)

        // The Topics area itself, returned to its root. No Topic is picked for the learner: the
        // acceptance criterion is deterministic starting guidance, not automatic selection.
        assertEquals(AppTopLevelDestination.TOPICS, navigator.area)
        assertEquals(AppRoute.Topics, navigator.currentRoute)
    }

    @Test
    fun aMistakeRecommendationReachesTheExistingMistakeReviewArea() {
        assertEquals(AppRoute.MistakeReview, LearningRecommendationTarget.MistakeReview.toAppRoute())

        val navigator = navigator()
        navigator.openRecommendation(LearningRecommendationTarget.MistakeReview)

        // The existing capability and its own area, rather than the same route pushed onto the
        // Topics stack — and deliberately not UNRESOLVED_MISTAKES practice.
        assertEquals(AppTopLevelDestination.MISTAKES, navigator.area)
        assertEquals(AppRoute.MistakeReview, navigator.currentRoute)
    }

    @Test
    fun aPracticeRecommendationReachesTheBuilderCarryingItsScopeAndSource() {
        assertEquals(
            AppRoute.PracticeBuilderSubtopic("coroutines", PracticeQuestionSource.WEAK_AREAS),
            recommendedPractice(
                AssessmentScope.Subtopic("coroutines"),
                PracticeQuestionSource.WEAK_AREAS,
            ).toAppRoute(),
        )
        assertEquals(
            AppRoute.PracticeBuilderTopic("kotlin", PracticeQuestionSource.UNSEEN),
            recommendedPractice(
                AssessmentScope.Topic("kotlin"),
                PracticeQuestionSource.UNSEEN,
            ).toAppRoute(),
        )

        // Pushed onto the current area, because the builder is a detail rather than an area root.
        val navigator = navigator()
        navigator.openRecommendation(
            recommendedPractice(AssessmentScope.Topic("kotlin"), PracticeQuestionSource.UNSEEN),
        )
        assertEquals(AppTopLevelDestination.TOPICS, navigator.area)
        assertEquals(
            AppRoute.PracticeBuilderTopic("kotlin", PracticeQuestionSource.UNSEEN),
            navigator.currentRoute,
        )
    }

    /**
     * The rule Recommended Next rests on: it selects a product capability the learner can inspect
     * and edit, never a running or stored assessment.
     */
    @Test
    fun noRecommendationTargetCanStartOrReopenAnAssessment() {
        val routes = listOf(
            LearningRecommendationTarget.Topics,
            LearningRecommendationTarget.MistakeReview,
            recommendedPractice(AssessmentScope.Topic("kotlin"), PracticeQuestionSource.UNSEEN),
            recommendedPractice(
                AssessmentScope.Subtopic("coroutines"),
                PracticeQuestionSource.WEAK_AREAS,
            ),
        ).map(LearningRecommendationTarget::toAppRoute)

        routes.forEach { route ->
            assertFalse(route is AppRoute.FocusedTopicPractice, "$route starts an assessment")
            assertFalse(route is AppRoute.FocusedSubtopicPractice, "$route starts an assessment")
            assertFalse(route is AppRoute.MixedInterview, "$route starts an assessment")
            assertFalse(route is AppRoute.FocusedPracticeAttempt, "$route resumes an attempt")
            assertFalse(route is AppRoute.MixedInterviewAttempt, "$route resumes an attempt")
            assertFalse(route is AppRoute.FocusedPracticeResult, "$route reopens a result")
            assertFalse(route is AppRoute.MixedInterviewResult, "$route reopens a result")
        }
    }

    private fun recommendedPractice(
        scope: AssessmentScope,
        source: PracticeQuestionSource,
    ): LearningRecommendationTarget.Practice =
        LearningRecommendationTarget.Practice(PracticePreset(scope, source))

    private fun navigator(): AppNavigator =
        AppNavigator(
            AppTopLevelDestination.entries.associateWith { mutableListOf<NavKey>(it.route) },
        )

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
