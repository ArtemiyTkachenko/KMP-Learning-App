package org.artkachenko.kmp_learning_app

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSessionLoader
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibility
import org.artkachenko.kmp_learning_app.curriculum.visibility.FixtureAssessmentRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.FixtureCurriculumRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibilityFixture
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibilityGuardFixture
import org.artkachenko.kmp_learning_app.curriculum.visibility.completedAttempt
import org.artkachenko.kmp_learning_app.curriculum.visibility.inProgressAttempt
import org.artkachenko.kmp_learning_app.topic_study.FakeLearningContentRepository

/**
 * Route classification and back-stack pruning, driven without composition over the
 * [VisibilityFixture] curriculum: `a*` Questions are Android, `k*` Questions are KMP.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class AppRouteVisibilityTest {
    private val hidden = CurriculumVisibility.from(includeKmpContent = false)
    private val shown = CurriculumVisibility.from(includeKmpContent = true)

    private val attempts = FixtureAssessmentRepository(
        completedAttempt("android_result", 1_000, "a1" to true),
        completedAttempt("kmp_result", 2_000, "k1" to true, "k2" to false),
        // Audit D-2: one core answer and one KMP answer.
        completedAttempt("mixed_result", 3_000, "a1" to true, "k1" to false),
        // A Question the curriculum no longer holds, beside a KMP one.
        completedAttempt("unresolved_result", 4_000, "retired" to true, "k1" to true),
        inProgressAttempt("android_attempt", "a1", "a2"),
        inProgressAttempt("kmp_attempt", "k1", "k2"),
        inProgressAttempt("mixed_attempt", "a1", "k1"),
        inProgressAttempt("unresolved_attempt", "a1", "retired"),
    )
    private val curriculum = FixtureCurriculumRepository()
    private val resolver = AppRouteVisibilityResolver(
        curriculumRepository = curriculum,
        learningContentRepository = VisibilityGuardFixture.learningContent(),
        assessmentRepository = attempts,
        assessmentSessionLoader = AssessmentSessionLoader(
            attempts,
            curriculum,
            MutableStateFlow(hidden),
        ),
    )

    @Test
    fun routesThatIdentifyNoSingleTopicAreAlwaysVisible() = runTest {
        listOf(
            AppRoute.Topics,
            AppRoute.Interview,
            AppRoute.Progress,
            AppRoute.MistakeReview,
            AppRoute.SavedQuestions,
            AppRoute.Settings,
        ).forEach { route ->
            assertEquals(AppRouteVisibility.Visible, resolver.classify(route, hidden), "$route")
        }
    }

    @Test
    fun topicSubtopicAndUnitRoutesFollowTheirOwningTopic() = runTest {
        val kmpRoutes = listOf(
            AppRoute.Topic("kmp"),
            AppRoute.Topic("kmp", subtopicId = "kmp_koin"),
            AppRoute.ProgressTopic("kmp"),
            AppRoute.PracticeBuilderTopic("kmp"),
            AppRoute.PracticeBuilderSubtopic("kmp_expect_actual"),
            AppRoute.LearningUnit("unit_kmp"),
            // Owned through the Unit it carries.
            AppRoute.LearningLesson("unit_kmp", "lesson_kmp_2"),
            AppRoute.PracticeBuilderLearningUnit("unit_kmp"),
        )
        val androidRoutes = listOf(
            AppRoute.Topic("android"),
            AppRoute.ProgressTopic("android"),
            AppRoute.PracticeBuilderTopic("android"),
            AppRoute.PracticeBuilderSubtopic("android_lifecycle"),
            AppRoute.LearningUnit("unit_android"),
            AppRoute.LearningLesson("unit_android", "lesson_android_1"),
            AppRoute.PracticeBuilderLearningUnit("unit_android"),
        )

        kmpRoutes.forEach {
            assertEquals(AppRouteVisibility.KnownHidden, resolver.classify(it, hidden), "$it")
            assertEquals(AppRouteVisibility.Visible, resolver.classify(it, shown), "$it")
        }
        androidRoutes.forEach {
            assertEquals(AppRouteVisibility.Visible, resolver.classify(it, hidden), "$it")
        }
    }

    @Test
    fun anOwnerThatCannotBeEstablishedIsUnknownRatherThanHidden() = runTest {
        listOf(
            AppRoute.PracticeBuilderSubtopic("no_such_subtopic"),
            AppRoute.LearningUnit("no_such_unit"),
            AppRoute.LearningLesson("no_such_unit", "lesson_kmp_1"),
            AppRoute.FocusedPracticeResult("no_such_attempt"),
            AppRoute.MixedInterviewAttempt("no_such_attempt"),
            // An in-progress attempt is not a result, and the result screen says so itself.
            AppRoute.MixedInterviewResult("kmp_attempt"),
        ).forEach { route ->
            assertEquals(AppRouteVisibility.Unknown, resolver.classify(route, hidden), "$route")
        }
    }

    @Test
    fun aFailedReadIsUnknown() = runTest {
        val failing = AppRouteVisibilityResolver(
            curriculumRepository = curriculum,
            learningContentRepository = FakeLearningContentRepository(failuresRemaining = Int.MAX_VALUE),
            assessmentRepository = attempts,
            assessmentSessionLoader = AssessmentSessionLoader(attempts, curriculum, MutableStateFlow(hidden)),
        )

        assertEquals(AppRouteVisibility.Unknown, failing.classify(AppRoute.LearningUnit("unit_kmp"), hidden))
    }

    /**
     * A completed result is hidden only when the history projection leaves nothing of it, so a
     * partially visible result stays, and an unresolved Question keeps a result visible exactly as
     * it keeps an attempt in projected history.
     */
    @Test
    fun completedResultsFollowTheHistoryProjection() = runTest {
        assertEquals(AppRouteVisibility.KnownHidden, resolver.classify(AppRoute.FocusedPracticeResult("kmp_result"), hidden))
        assertEquals(AppRouteVisibility.KnownHidden, resolver.classify(AppRoute.MixedInterviewResult("kmp_result"), hidden))
        assertEquals(AppRouteVisibility.Visible, resolver.classify(AppRoute.MixedInterviewResult("mixed_result"), hidden))
        assertEquals(AppRouteVisibility.Visible, resolver.classify(AppRoute.FocusedPracticeResult("android_result"), hidden))
        assertEquals(AppRouteVisibility.Visible, resolver.classify(AppRoute.MixedInterviewResult("unresolved_result"), hidden))
        assertEquals(AppRouteVisibility.Visible, resolver.classify(AppRoute.FocusedPracticeResult("kmp_result"), shown))
    }

    /** An in-progress attempt is never shortened, so one hidden Question hides the whole attempt. */
    @Test
    fun inProgressAttemptsAreHiddenByAnyResolvedHiddenQuestion() = runTest {
        assertEquals(AppRouteVisibility.KnownHidden, resolver.classify(AppRoute.FocusedPracticeAttempt("kmp_attempt"), hidden))
        assertEquals(AppRouteVisibility.KnownHidden, resolver.classify(AppRoute.MixedInterviewAttempt("mixed_attempt"), hidden))
        assertEquals(AppRouteVisibility.Visible, resolver.classify(AppRoute.MixedInterviewAttempt("android_attempt"), hidden))
        // Missing metadata is not evidence of a hidden Topic.
        assertEquals(AppRouteVisibility.Unknown, resolver.classify(AppRoute.MixedInterviewAttempt("unresolved_attempt"), hidden))
        assertEquals(AppRouteVisibility.Visible, resolver.classify(AppRoute.MixedInterviewAttempt("mixed_attempt"), shown))
        // A finished attempt route is judged as the result it hands over to.
        assertEquals(AppRouteVisibility.KnownHidden, resolver.classify(AppRoute.FocusedPracticeAttempt("kmp_result"), hidden))
        assertEquals(AppRouteVisibility.Visible, resolver.classify(AppRoute.MixedInterviewAttempt("mixed_result"), hidden))
    }

    @Test
    fun nothingIsReadWhileNothingIsHidden() = runTest {
        resolver.classify(AppRoute.MixedInterviewResult("mixed_result"), shown)
        resolver.classify(AppRoute.MixedInterviewAttempt("mixed_attempt"), shown)

        assertEquals(emptyList(), attempts.getByIdCalls)
    }

    @Test
    fun aKmpLearnFlowIsPrunedBackToTheTopicsRoot() = runTest {
        val navigator = navigator()
        navigator.push(AppRoute.Topic("kmp"))
        navigator.push(AppRoute.LearningUnit("unit_kmp"))
        navigator.push(AppRoute.LearningLesson("unit_kmp", "lesson_kmp_1"))
        navigator.push(AppRoute.PracticeBuilderLearningUnit("unit_kmp"))

        prune(navigator, MutableStateFlow(hidden))

        assertEquals(listOf<NavKey>(AppRoute.Topics), navigator.backStack)
    }

    /** Validation covers every area's stack, not only the one on screen. */
    @Test
    fun aParkedProgressDrillDownIsPrunedWhileAnotherAreaIsShown() = runTest {
        val navigator = navigator()
        navigator.select(AppTopLevelDestination.PROGRESS)
        navigator.push(AppRoute.ProgressTopic("kmp"))
        navigator.select(AppTopLevelDestination.TOPICS)
        navigator.push(AppRoute.Settings)
        val visibility = MutableStateFlow(shown)
        val pruning = launch { pruneRoutesHiddenBy(visibility, navigator, resolver) }
        advanceUntilIdle()

        visibility.value = hidden
        advanceUntilIdle()
        pruning.cancel()

        assertEquals(listOf<NavKey>(AppRoute.Topics, AppRoute.Settings), navigator.backStack)
        navigator.select(AppTopLevelDestination.PROGRESS)
        assertEquals(listOf<NavKey>(AppRoute.Progress), navigator.backStack)
    }

    @Test
    fun kmpPracticeBuilderRoutesAreRemovedAndCorePracticeStays() = runTest {
        val navigator = navigator()
        navigator.select(AppTopLevelDestination.MISTAKES)
        navigator.push(AppRoute.PracticeBuilderSubtopic("kmp_expect_actual"))
        navigator.select(AppTopLevelDestination.PROGRESS)
        navigator.push(AppRoute.PracticeBuilderTopic("kmp"))
        navigator.select(AppTopLevelDestination.INTERVIEW)
        navigator.push(AppRoute.PracticeBuilderSubtopic("android_lifecycle"))

        prune(navigator, MutableStateFlow(hidden))

        assertEquals(listOf<NavKey>(AppRoute.Interview, AppRoute.PracticeBuilderSubtopic("android_lifecycle")), navigator.backStack)
        navigator.select(AppTopLevelDestination.MISTAKES)
        assertEquals(listOf<NavKey>(AppRoute.MistakeReview), navigator.backStack)
        navigator.select(AppTopLevelDestination.PROGRESS)
        assertEquals(listOf<NavKey>(AppRoute.Progress), navigator.backStack)
    }

    @Test
    fun aKmpOnlyResultIsRemovedAndAPartiallyVisibleMixedResultStays() = runTest {
        val navigator = navigator()
        navigator.select(AppTopLevelDestination.PROGRESS)
        navigator.push(AppRoute.FocusedPracticeResult("kmp_result"))
        navigator.select(AppTopLevelDestination.INTERVIEW)
        navigator.push(AppRoute.MixedInterviewResult("mixed_result"))

        prune(navigator, MutableStateFlow(hidden))

        assertEquals(listOf<NavKey>(AppRoute.Interview, AppRoute.MixedInterviewResult("mixed_result")), navigator.backStack)
        navigator.select(AppTopLevelDestination.PROGRESS)
        assertEquals(listOf<NavKey>(AppRoute.Progress), navigator.backStack)
    }

    /** An unclassifiable route reaches its own NotFound instead of being deleted. */
    @Test
    fun unknownRoutesAreNotPruned() = runTest {
        val navigator = navigator()
        navigator.push(AppRoute.LearningUnit("no_such_unit"))

        prune(navigator, MutableStateFlow(hidden))

        assertEquals(listOf<NavKey>(AppRoute.Topics, AppRoute.LearningUnit("no_such_unit")), navigator.backStack)
    }

    @Test
    fun settingsStaysOpenAboveAPrunedChainAndIsRebasedOnTheRoot() = runTest {
        val navigator = navigator()
        navigator.push(AppRoute.Topic("kmp"))
        navigator.push(AppRoute.LearningUnit("unit_kmp"))
        navigator.push(AppRoute.Settings)

        prune(navigator, MutableStateFlow(hidden))

        assertEquals(listOf<NavKey>(AppRoute.Topics, AppRoute.Settings), navigator.backStack)
        navigator.popBack()
        assertEquals(AppRoute.Topics, navigator.currentRoute)
    }

    @Test
    fun showingContentAgainDoesNotRecreatePrunedRoutes() = runTest {
        val navigator = navigator()
        navigator.push(AppRoute.Topic("kmp"))
        navigator.push(AppRoute.Settings)
        val visibility = MutableStateFlow(hidden)
        val pruning = launch { pruneRoutesHiddenBy(visibility, navigator, resolver) }
        advanceUntilIdle()

        visibility.value = shown
        advanceUntilIdle()
        pruning.cancel()

        assertEquals(listOf<NavKey>(AppRoute.Topics, AppRoute.Settings), navigator.backStack)
    }

    /**
     * A pass still classifying when the visibility changes again is cancelled, so a result computed
     * under OFF cannot prune a stack that is now valid under ON.
     */
    @Test
    fun aPassSupersededByANewerVisibilityPrunesNothing() = runTest {
        val gate = CompletableDeferred<Unit>()
        val gatedResolver = AppRouteVisibilityResolver(
            curriculumRepository = curriculum,
            learningContentRepository = VisibilityGuardFixture.learningContent().apply {
                beforeRead = { gate.await() }
            },
            assessmentRepository = attempts,
            assessmentSessionLoader = AssessmentSessionLoader(attempts, curriculum, MutableStateFlow(hidden)),
        )
        val navigator = navigator()
        navigator.push(AppRoute.LearningUnit("unit_kmp"))
        val visibility = MutableStateFlow(hidden)
        val pruning = launch { pruneRoutesHiddenBy(visibility, navigator, gatedResolver) }
        advanceUntilIdle()

        visibility.value = shown
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()
        pruning.cancel()

        assertEquals(listOf<NavKey>(AppRoute.Topics, AppRoute.LearningUnit("unit_kmp")), navigator.backStack)
    }

    /** Pruning is navigation state only: no attempt is written, cancelled, or rewritten. */
    @Test
    fun pruningWritesNothing() = runTest {
        val before = attempts.snapshot()
        val navigator = navigator()
        navigator.push(AppRoute.FocusedPracticeAttempt("kmp_attempt"))
        navigator.select(AppTopLevelDestination.INTERVIEW)
        navigator.push(AppRoute.MixedInterviewResult("kmp_result"))

        prune(navigator, MutableStateFlow(hidden))

        assertEquals(0, attempts.saves)
        assertEquals(before, attempts.snapshot())
        assertEquals(AssessmentStatus.IN_PROGRESS, attempts.snapshot().getValue("kmp_attempt").status)
    }

    private fun TestScope.prune(navigator: AppNavigator, visibility: MutableStateFlow<CurriculumVisibility>) {
        val pruning = launch { pruneRoutesHiddenBy(visibility, navigator, resolver) }
        advanceUntilIdle()
        pruning.cancel()
    }

    private fun navigator(): AppNavigator =
        AppNavigator(AppTopLevelDestination.entries.associateWith { mutableListOf<NavKey>(it.route) })
}
