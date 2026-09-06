package org.artkachenko.kmp_learning_app

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertSame
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingTarget
import org.artkachenko.kmp_learning_app.guided_learning.LearningRecommendationTarget
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.toPracticeBuilderRoute

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

    /**
     * A contextual shortcut pushes the builder onto the area the learner is already in, and takes
     * the same route the identical preset takes from Recommended Next. The builder never learns
     * which surface a preset came from, so one preset means one thing everywhere.
     */
    @Test
    fun aContextualShortcutPushesTheBuilderOntoTheAreaItWasOfferedIn() {
        val preset = PracticePreset(
            AssessmentScope.Subtopic("coroutines"),
            PracticeQuestionSource.UNRESOLVED_MISTAKES,
        )
        val navigator = navigator()
        navigator.select(AppTopLevelDestination.MISTAKES)

        navigator.push(preset.toPracticeBuilderRoute())

        assertEquals(AppTopLevelDestination.MISTAKES, navigator.area)
        assertEquals(
            AppRoute.PracticeBuilderSubtopic("coroutines", PracticeQuestionSource.UNRESOLVED_MISTAKES),
            navigator.currentRoute,
        )
        // Identical payload, identical destination, whichever surface offered it.
        assertEquals(
            preset.toPracticeBuilderRoute(),
            LearningRecommendationTarget.Practice(preset).toAppRoute(),
        )
    }

    /**
     * The same rule that binds Recommended Next binds every contextual shortcut: a preset opens a
     * configuration screen, never a run, a stored attempt, or a result.
     */
    @Test
    fun noContextualShortcutPresetCanStartOrReopenAnAssessment() {
        val presets = PracticeQuestionSource.entries.flatMap { source ->
            listOf(
                PracticePreset(AssessmentScope.Topic("kotlin"), source),
                PracticePreset(AssessmentScope.Subtopic("coroutines"), source),
            )
        }

        presets.map(PracticePreset::toPracticeBuilderRoute).forEach { route ->
            assertTrue(
                route is AppRoute.PracticeBuilderTopic ||
                    route is AppRoute.PracticeBuilderSubtopic,
                "$route is not the Practice Builder",
            )
            assertFalse(route is AppRoute.FocusedTopicPractice, "$route starts an assessment")
            assertFalse(route is AppRoute.FocusedSubtopicPractice, "$route starts an assessment")
            assertFalse(route is AppRoute.FocusedPracticeAttempt, "$route resumes an attempt")
            assertFalse(route is AppRoute.FocusedPracticeResult, "$route reopens a result")
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

    /**
     * The structural rule for EPIC-18: Saved Questions completes the study workflow without adding
     * a fifth place to navigate to. It is a detail of Topics, reached the way Topic detail is.
     */
    @Test
    fun savedQuestionsIsATopicsDetailAndNotAFifthArea() {
        assertEquals(
            listOf(
                AppTopLevelDestination.TOPICS,
                AppTopLevelDestination.INTERVIEW,
                AppTopLevelDestination.PROGRESS,
                AppTopLevelDestination.MISTAKES,
            ),
            AppTopLevelDestination.entries,
        )
        assertNull(AppTopLevelDestination.forRoute(AppRoute.SavedQuestions))

        val navigator = navigator()
        navigator.push(AppRoute.SavedQuestions)

        assertEquals(AppTopLevelDestination.TOPICS, navigator.area)
        assertEquals(AppRoute.SavedQuestions, navigator.currentRoute)
    }

    @Test
    fun savedQuestionsKeepsAreaNavigationAndBackReturnsToTopics() {
        // Browsing, not an assessment in progress: the learner can leave for another area in one
        // move, exactly as they can from Topic detail or the Practice Builder.
        assertTrue(AppRoute.SavedQuestions.showsAreaNavigation())

        val navigator = navigator()
        navigator.push(AppRoute.SavedQuestions)
        navigator.popBack()

        assertEquals(AppRoute.Topics, navigator.currentRoute)
        assertEquals(AppTopLevelDestination.TOPICS, navigator.area)
    }

    @Test
    fun switchingAreasFromSavedQuestionsLeavesItWhereItWas() {
        val navigator = navigator()
        navigator.push(AppRoute.SavedQuestions)

        navigator.select(AppTopLevelDestination.PROGRESS)
        assertEquals(AppRoute.Progress, navigator.currentRoute)

        // Each area keeps its own stack, so Topics is still on Saved Questions when returned to.
        navigator.select(AppTopLevelDestination.TOPICS)
        assertEquals(AppRoute.SavedQuestions, navigator.currentRoute)
    }

    @Test
    fun theSavedQuestionsRouteCarriesNoSavedState() {
        // Which Questions are saved is read from the shared saved state on arrival. The route is a
        // destination, not a snapshot: no question IDs, order, or content travels in the stack.
        val route: AppRoute = AppRoute.SavedQuestions

        assertSame(AppRoute.SavedQuestions, route)
    }

    /**
     * The Learn stack EPIC-21 builds: catalogue, Topic, Unit, Lesson. Each step is a push onto the
     * area's existing stack, so no new area appears and back unwinds it one destination at a time.
     */
    @Test
    fun learningRoutesExtendTheLearnStackAndBackUnwindsItToTheOriginatingTopic() {
        val navigator = navigator()

        navigator.push(AppRoute.Topic("android_ui"))
        navigator.push(AppRoute.LearningUnit("unit_thinking_in_compose"))
        navigator.push(
            AppRoute.LearningLesson(
                unitId = "unit_thinking_in_compose",
                lessonId = "lesson_declarative_ui",
            ),
        )

        assertEquals(AppTopLevelDestination.TOPICS, navigator.area)
        assertEquals(
            listOf<NavKey>(
                AppRoute.Topics,
                AppRoute.Topic("android_ui"),
                AppRoute.LearningUnit("unit_thinking_in_compose"),
                AppRoute.LearningLesson("unit_thinking_in_compose", "lesson_declarative_ui"),
            ),
            navigator.backStack.toList(),
        )

        assertTrue(navigator.popBack())
        assertEquals(AppRoute.LearningUnit("unit_thinking_in_compose"), navigator.currentRoute)
        assertTrue(navigator.popBack())
        assertEquals(AppRoute.Topic("android_ui"), navigator.currentRoute)
    }

    @Test
    fun learningRoutesAreLearnDetailsAndNotAFifthArea() {
        assertNull(AppTopLevelDestination.forRoute(AppRoute.LearningUnit("unit")))
        assertNull(AppTopLevelDestination.forRoute(AppRoute.LearningLesson("unit", "lesson")))
    }

    @Test
    fun learningRoutesKeepAreaNavigationVisible() {
        // Reading is browsing, not an assessment in progress: leaving for another area costs the
        // learner nothing, exactly as on Topic detail.
        assertTrue(AppRoute.LearningUnit("unit").showsAreaNavigation())
        assertTrue(AppRoute.LearningLesson("unit", "lesson").showsAreaNavigation())
    }

    @Test
    fun switchingAreasFromALessonLeavesTheLearningStackWhereItWas() {
        val navigator = navigator()
        navigator.push(AppRoute.Topic("android_ui"))
        navigator.push(AppRoute.LearningUnit("unit_thinking_in_compose"))
        navigator.push(
            AppRoute.LearningLesson("unit_thinking_in_compose", "lesson_declarative_ui"),
        )

        navigator.select(AppTopLevelDestination.PROGRESS)
        assertEquals(AppRoute.Progress, navigator.currentRoute)

        navigator.select(AppTopLevelDestination.TOPICS)
        assertEquals(
            AppRoute.LearningLesson("unit_thinking_in_compose", "lesson_declarative_ui"),
            navigator.currentRoute,
        )
    }

    /**
     * E21-07: Learn's own stack and another area's, both holding detail at the same time.
     *
     * `switchingAreasFromALessonLeavesTheLearningStackWhereItWas` shows Learn surviving a trip to
     * an empty Progress, and `AppNavigatorTest.backOutOfAnAreaLeavesThatAreaWhereItWas` shows
     * Progress surviving on its own. Neither can catch a shared stack that happens to look right
     * while only one area is deep, which is exactly what reading a Lesson and then opening a
     * Progress detail would expose.
     */
    @Test
    fun learnAndProgressEachKeepTheirOwnStackWhileBothHoldDetail() {
        val navigator = navigator()
        navigator.push(AppRoute.Topic("android_ui"))
        navigator.push(AppRoute.LearningUnit("unit_thinking_in_compose"))
        navigator.push(
            AppRoute.LearningLesson("unit_thinking_in_compose", "lesson_declarative_ui"),
        )

        navigator.select(AppTopLevelDestination.PROGRESS)
        navigator.push(AppRoute.ProgressTopic("android_ui"))

        // Progress got its own entry rather than inheriting the reader's.
        assertEquals(
            listOf<NavKey>(AppRoute.Progress, AppRoute.ProgressTopic("android_ui")),
            navigator.backStack.toList(),
        )

        navigator.select(AppTopLevelDestination.TOPICS)
        assertEquals(
            listOf<NavKey>(
                AppRoute.Topics,
                AppRoute.Topic("android_ui"),
                AppRoute.LearningUnit("unit_thinking_in_compose"),
                AppRoute.LearningLesson("unit_thinking_in_compose", "lesson_declarative_ui"),
            ),
            navigator.backStack.toList(),
        )

        // And Progress is still where the learner left it, not rewound by Learn's own depth.
        navigator.select(AppTopLevelDestination.PROGRESS)
        assertEquals(AppRoute.ProgressTopic("android_ui"), navigator.currentRoute)
    }

    /** The existing reselect policy, which learning detail must not become an exception to. */
    @Test
    fun reselectingLearnFromALessonReturnsToTheCatalogueRoot() {
        val navigator = navigator()
        navigator.push(AppRoute.Topic("android_ui"))
        navigator.push(AppRoute.LearningUnit("unit_thinking_in_compose"))
        navigator.push(
            AppRoute.LearningLesson("unit_thinking_in_compose", "lesson_declarative_ui"),
        )

        navigator.select(AppTopLevelDestination.TOPICS)

        assertEquals(AppRoute.Topics, navigator.currentRoute)
    }

    /**
     * Identity only. Title, summary, Lesson prose, and Sources are resolved from
     * `LearningContentRepository` on arrival, so nothing publisher-owned is serialized into the
     * back stack where a later edit could leave it stale.
     */
    @Test
    fun learningRoutesCarryStableIdentitiesOnly() {
        val unit = AppRoute.LearningUnit(unitId = "unit_thinking_in_compose")
        assertEquals("unit_thinking_in_compose", unit.unitId)
        assertEquals(AppRoute.LearningUnit("unit_thinking_in_compose"), unit)

        val lesson = AppRoute.LearningLesson(
            unitId = "unit_thinking_in_compose",
            lessonId = "lesson_declarative_ui",
        )
        assertEquals("unit_thinking_in_compose", lesson.unitId)
        assertEquals("lesson_declarative_ui", lesson.lessonId)
        // The parent is part of the identity: the same Lesson under another Unit is another route.
        assertNotEquals(
            lesson,
            AppRoute.LearningLesson("unit_other", "lesson_declarative_ui"),
        )
    }

    /**
     * Practice opened from study is an ordinary detail push onto the Learn stack, from either
     * reading surface, so back returns to the screen the learner left rather than to the Topic.
     */
    @Test
    fun practisingAUnitPushesTheBuilderOntoTheLearnStackFromBothReadingSurfaces() {
        val fromUnit = navigator()
        fromUnit.push(AppRoute.Topic("android_ui"))
        fromUnit.push(AppRoute.LearningUnit("unit_thinking_in_compose"))
        fromUnit.push(AppRoute.PracticeBuilderLearningUnit("unit_thinking_in_compose"))

        assertEquals(AppTopLevelDestination.TOPICS, fromUnit.area)
        assertTrue(fromUnit.popBack())
        assertEquals(AppRoute.LearningUnit("unit_thinking_in_compose"), fromUnit.currentRoute)

        val fromLesson = navigator()
        fromLesson.push(AppRoute.Topic("android_ui"))
        fromLesson.push(AppRoute.LearningUnit("unit_thinking_in_compose"))
        fromLesson.push(
            AppRoute.LearningLesson("unit_thinking_in_compose", "lesson_declarative_ui"),
        )
        fromLesson.push(AppRoute.PracticeBuilderLearningUnit("unit_thinking_in_compose"))

        assertTrue(fromLesson.popBack())
        assertEquals(
            AppRoute.LearningLesson("unit_thinking_in_compose", "lesson_declarative_ui"),
            fromLesson.currentRoute,
        )
    }

    /**
     * Identity only, and nothing derived from it: the concepts the Unit teaches are resolved on
     * arrival, so no set of Subtopic IDs and no Unit title is serialized into the back stack.
     */
    @Test
    fun theUnitBuilderRouteCarriesTheStableUnitIdOnly() {
        val route = AppRoute.PracticeBuilderLearningUnit(unitId = "unit_thinking_in_compose")

        assertEquals("unit_thinking_in_compose", route.unitId)
        assertEquals(AppRoute.PracticeBuilderLearningUnit("unit_thinking_in_compose"), route)
        assertNotEquals(route, AppRoute.PracticeBuilderLearningUnit("unit_other"))
    }

    /**
     * Setting practice up is still browsing, and taking it still is not — the Unit entry inherits
     * both halves of the existing rule rather than introducing a third behaviour.
     */
    @Test
    fun unitPracticeFollowsTheExistingAreaNavigationRule() {
        assertNull(AppTopLevelDestination.forRoute(AppRoute.PracticeBuilderLearningUnit("unit")))
        assertTrue(AppRoute.PracticeBuilderLearningUnit("unit").showsAreaNavigation())
        assertFalse(
            AppRoute.FocusedSubtopicsPractice(
                subtopicIds = listOf("subtopic_a", "subtopic_b"),
                questionCount = 10,
                levels = QuestionLevel.entries,
                source = PracticeQuestionSource.ALL,
            ).showsAreaNavigation(),
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
