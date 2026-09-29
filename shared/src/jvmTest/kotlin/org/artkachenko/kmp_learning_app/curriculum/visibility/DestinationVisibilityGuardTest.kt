package org.artkachenko.kmp_learning_app.curriculum.visibility

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.history.asCompletedHistory
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.lesson_study.studyProgressStateHolder
import org.artkachenko.kmp_learning_app.progress.ProgressTopicUiState
import org.artkachenko.kmp_learning_app.progress.ProgressTopicViewModel
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonUiState
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonViewModel
import org.artkachenko.kmp_learning_app.topic_study.learning_unit.LearningUnitUiState
import org.artkachenko.kmp_learning_app.topic_study.learning_unit.LearningUnitViewModel
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeAvailability
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderTarget
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderViewModel
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeTargetResolution
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeTargetResolver

/**
 * Destinations that resolve content through identity reads refuse known-hidden content on their own,
 * with no navigator involved — the case of a restored or direct route that pruning has not reached.
 * Each follows a live visibility change on the same ViewModel, both ways.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class DestinationVisibilityGuardTest {
    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun aKmpLearningUnitIsNotFoundWhileHiddenAndReturnsWhenShown() = guardTest {
        val holder = curriculumVisibilityStateHolder(includeKmpContent = true)
        val viewModel = LearningUnitViewModel(
            unitId = "unit_kmp",
            learningContentRepository = VisibilityGuardFixture.learningContent(),
            studyProgressStateHolder = studyProgressStateHolder(),
            visibilityStateHolder = holder,
        )
        advanceUntilIdle()
        assertEquals("unit_kmp", assertIs<LearningUnitUiState.Content>(viewModel.uiState.value).unitId)

        holder.setIncludeKmpContent(false)
        advanceUntilIdle()
        assertEquals(LearningUnitUiState.NotFound, viewModel.uiState.value)

        holder.setIncludeKmpContent(true)
        advanceUntilIdle()
        assertIs<LearningUnitUiState.Content>(viewModel.uiState.value)
    }

    @Test
    fun aDirectKmpUnitRouteUnderOffIsNotFoundAndACoreUnitIsUnaffected() = guardTest {
        val holder = curriculumVisibilityStateHolder(includeKmpContent = false)
        val content = VisibilityGuardFixture.learningContent()
        val kmp = LearningUnitViewModel("unit_kmp", content, studyProgressStateHolder(), holder)
        val android = LearningUnitViewModel("unit_android", content, studyProgressStateHolder(), holder)
        advanceUntilIdle()

        assertEquals(LearningUnitUiState.NotFound, kmp.uiState.value)
        assertIs<LearningUnitUiState.Content>(android.uiState.value)
        // One read each: the StateFlow's replayed value did not trigger a second load.
        assertEquals(listOf("unit_kmp", "unit_android"), content.unitReadIds)
    }

    @Test
    fun aKmpLessonIsNotFoundWhileItsUnitIsHidden() = guardTest {
        val holder = curriculumVisibilityStateHolder(includeKmpContent = true)
        val viewModel = LearningLessonViewModel(
            unitId = "unit_kmp",
            lessonId = "lesson_kmp_2",
            learningContentRepository = VisibilityGuardFixture.learningContent(),
            studyProgressStateHolder = studyProgressStateHolder(),
            visibilityStateHolder = holder,
        )
        advanceUntilIdle()
        val shown = assertIs<LearningLessonUiState.Content>(viewModel.uiState.value)
        assertEquals("lesson_kmp_1", shown.previousLesson?.lessonId)

        holder.setIncludeKmpContent(false)
        advanceUntilIdle()
        assertEquals(LearningLessonUiState.NotFound, viewModel.uiState.value)

        holder.setIncludeKmpContent(true)
        advanceUntilIdle()
        assertIs<LearningLessonUiState.Content>(viewModel.uiState.value)
    }

    @Test
    fun hiddenPracticeTargetsAreUnavailableByTheirOwningTopic() = guardTest {
        val visibility = VisibilityFixture.visibility(includeKmpContent = false)
        val resolver = PracticeTargetResolver(
            curriculumRepository = FixtureCurriculumRepository(),
            learningContentRepository = VisibilityGuardFixture.learningContent(),
            visibility = visibility,
        )

        listOf(
            PracticeBuilderTarget.Topic("kmp"),
            PracticeBuilderTarget.Subtopic("kmp_koin"),
            PracticeBuilderTarget.LearningUnit("unit_kmp"),
        ).forEach { target ->
            assertEquals(PracticeTargetResolution.Unavailable, resolver.resolve(target), "$target")
        }
        assertIs<PracticeTargetResolution.Resolved>(resolver.resolve(PracticeBuilderTarget.Topic("android")))
        assertIs<PracticeTargetResolution.Resolved>(resolver.resolve(PracticeBuilderTarget.LearningUnit("unit_android")))
        // A target whose display metadata does not resolve keeps its existing missing-label
        // behaviour: missing is not evidence of a hidden Topic.
        val unnamed = assertIs<PracticeTargetResolution.Resolved>(
            resolver.resolve(PracticeBuilderTarget.Subtopic("no_such_subtopic")),
        )
        assertEquals(null, unnamed.name)

        visibility.value = CurriculumVisibility.from(includeKmpContent = true)
        assertIs<PracticeTargetResolution.Resolved>(resolver.resolve(PracticeBuilderTarget.Topic("kmp")))
    }

    @Test
    fun aLivePracticeBuilderDropsItsKmpScopeWhenHiddenAndRegainsIt() = guardTest {
        val holder = curriculumVisibilityStateHolder(includeKmpContent = true)
        val curriculum = FixtureCurriculumRepository()
        val viewModel = PracticeBuilderViewModel(
            target = PracticeBuilderTarget.Topic("kmp"),
            targetResolver = PracticeTargetResolver(
                curriculumRepository = curriculum,
                learningContentRepository = VisibilityGuardFixture.learningContent(),
                visibility = holder.visibility,
            ),
            questionSelector = AssessmentQuestionSelector(
                curriculumRepository = VisibleCurriculumRepository(curriculum, holder.visibility),
                completedHistory = { emptyList() },
                randomize = { it },
            ),
            visibilityStateHolder = holder,
        )
        advanceUntilIdle()
        assertEquals(PracticeAvailability.Available(7), viewModel.uiState.value.availability)

        holder.setIncludeKmpContent(false)
        advanceUntilIdle()
        assertEquals(PracticeAvailability.TargetUnavailable, viewModel.uiState.value.availability)
        assertEquals(false, viewModel.uiState.value.isStartEnabled)

        holder.setIncludeKmpContent(true)
        advanceUntilIdle()
        assertEquals(PracticeAvailability.Available(7), viewModel.uiState.value.availability)
    }

    /** Hidden is not the same statement as "no observations": the screen says which it is. */
    @Test
    fun aHiddenProgressTopicIsUnavailableRatherThanEmpty() = guardTest {
        val holder = curriculumVisibilityStateHolder(includeKmpContent = false)
        val attempts = FixtureAssessmentRepository(completedAttempt("kmp", 1_000, "k1" to true))
        val curriculum = VisibleCurriculumRepository(FixtureCurriculumRepository(), holder.visibility)
        val service = LearningProgressService(attempts.asCompletedHistory(), curriculum)
        val kmp = ProgressTopicViewModel("kmp", service, holder)
        val compose = ProgressTopicViewModel("compose", service, holder)
        advanceUntilIdle()

        assertEquals(ProgressTopicUiState.Unavailable, kmp.uiState.value)
        // A visible Topic without observations is still Empty.
        assertEquals(ProgressTopicUiState.Empty, compose.uiState.value)

        holder.setIncludeKmpContent(true)
        advanceUntilIdle()
        assertEquals(1, assertIs<ProgressTopicUiState.Content>(kmp.uiState.value).answeredCount)

        holder.setIncludeKmpContent(false)
        advanceUntilIdle()
        assertEquals(ProgressTopicUiState.Unavailable, kmp.uiState.value)
    }

    private fun guardTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }
}
