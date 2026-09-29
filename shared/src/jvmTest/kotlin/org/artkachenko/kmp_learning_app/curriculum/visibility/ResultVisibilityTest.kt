package org.artkachenko.kmp_learning_app.curriculum.visibility

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeService
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment.start.StartAssessment
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.mixed_interview.MixedInterviewResultUiState
import org.artkachenko.kmp_learning_app.mixed_interview.MixedInterviewResultViewModel
import org.artkachenko.kmp_learning_app.saved_questions.savedQuestionStateHolder
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultUiState
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultViewModel

/**
 * Completed results are projected through the same [VisibleHistoryProjection][org.artkachenko.kmp_learning_app.assessment.history.VisibleHistoryProjection]
 * every history surface reads, on the one stored attempt, and follow a visibility change live.
 * `a*` Questions are Android, `c*` Compose, `k*` Kotlin Multiplatform.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ResultVisibilityTest {
    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** Audit decision D-2: Android correct and KMP wrong is 1/2 stored, 1/1 hidden, 1/2 shown. */
    @Test
    fun aMixedResultIsProjectedWhileKmpIsHiddenAndRestoredWhenShown() = resultTest {
        val stored = completedAttempt("mixed", 1_000, "a1" to true, "k1" to false)
        val attempts = FixtureAssessmentRepository(stored)
        val holder = curriculumVisibilityStateHolder(includeKmpContent = false)
        val viewModel = mixedViewModel("mixed", attempts, holder)
        advanceUntilIdle()

        val hidden = assertIs<MixedInterviewResultUiState.Content>(viewModel.uiState.value)
        assertEquals(1 to 1, hidden.correctAnswers to hidden.totalQuestions)
        assertEquals(100.0, hidden.percentage)
        assertEquals(listOf("a1"), hidden.questions.ids())
        assertEquals(1, hidden.hiddenQuestionCount)
        assertEquals(listOf("android"), hidden.topicPerformance.map { it.topicId })

        holder.setIncludeKmpContent(true)
        advanceUntilIdle()
        val shown = assertIs<MixedInterviewResultUiState.Content>(viewModel.uiState.value)
        assertEquals(1 to 2, shown.correctAnswers to shown.totalQuestions)
        assertEquals(listOf("a1", "k1"), shown.questions.ids())
        assertEquals(0, shown.hiddenQuestionCount)
        assertEquals(listOf("android", "kmp"), shown.topicPerformance.map { it.topicId })

        assertEquals(stored, attempts.snapshot().getValue("mixed"))
        assertEquals(0, attempts.saves)
    }

    /**
     * Persisted correctness decides the projected score, the remaining Questions keep their stored
     * order, and only visible Topics get a breakdown row.
     */
    @Test
    fun aMixedProjectionKeepsOrderAndPersistedCorrectness() = resultTest {
        val attempts = FixtureAssessmentRepository(
            completedAttempt(
                "mixed", 1_000,
                "k1" to true, "c1" to false, "a2" to true, "k2" to true, "a1" to false,
            ),
        )
        val viewModel = mixedViewModel("mixed", attempts, curriculumVisibilityStateHolder(false))
        advanceUntilIdle()

        val content = assertIs<MixedInterviewResultUiState.Content>(viewModel.uiState.value)
        assertEquals(listOf("c1", "a2", "a1"), content.questions.ids())
        assertEquals(1 to 3, content.correctAnswers to content.totalQuestions)
        assertEquals(2, content.hiddenQuestionCount)
        assertEquals(setOf("compose", "android"), content.topicPerformance.map { it.topicId }.toSet())
    }

    @Test
    fun aMixedResultWithNothingVisibleIsUnavailableNotMissing() = resultTest {
        val attempts = FixtureAssessmentRepository(completedAttempt("kmp", 1_000, "k1" to true))
        val holder = curriculumVisibilityStateHolder(includeKmpContent = false)
        val viewModel = mixedViewModel("kmp", attempts, holder)
        advanceUntilIdle()
        assertEquals(MixedInterviewResultUiState.Unavailable, viewModel.uiState.value)

        holder.setIncludeKmpContent(true)
        advanceUntilIdle()
        assertIs<MixedInterviewResultUiState.Content>(viewModel.uiState.value)
    }

    /** Retake is unchanged: the stored config, selected against what is eligible now. */
    @Test
    fun aPartiallyHiddenMixedResultRetakesOnlyVisibleQuestions() = resultTest {
        val attempts = FixtureAssessmentRepository(completedAttempt("mixed", 1_000, "a1" to true, "k1" to false))
        val viewModel = mixedViewModel("mixed", attempts, curriculumVisibilityStateHolder(false))
        advanceUntilIdle()

        viewModel.repeatInterview()
        val created = viewModel.retakeEvents.first()
        advanceUntilIdle()

        val retake = attempts.snapshot().getValue(created.attemptId)
        assertEquals(AssessmentConfig.Mixed(questionCount = 2), retake.config)
        assertTrue(retake.questionAttempts.none { it.questionId.startsWith("k") })
    }

    @Test
    fun aFocusedResultWithNothingVisibleIsUnavailableAndOffersNoRetake() = resultTest {
        val attempts = FixtureAssessmentRepository(
            completedAttempt("kmp", 1_000, "k1" to true, "k2" to false, config = focusedOn(VisibilityFixture.kmp, 2)),
        )
        val holder = curriculumVisibilityStateHolder(includeKmpContent = false)
        val viewModel = focusedViewModel("kmp", attempts, holder)
        advanceUntilIdle()
        assertEquals(FocusedResultUiState.Unavailable, viewModel.uiState.value)

        viewModel.repeatPractice()
        advanceUntilIdle()
        assertEquals(1, attempts.snapshot().size)

        holder.setIncludeKmpContent(true)
        advanceUntilIdle()
        val shown = assertIs<FocusedResultUiState.Content>(viewModel.uiState.value)
        assertEquals(1 to 2, shown.correctAnswers to shown.totalQuestions)
        assertEquals(0, attempts.saves)
    }

    /**
     * A historical Focused attempt whose Questions were later split across the boundary — possible
     * after a re-homing migration — shows its visible part, with the hidden count explaining the
     * difference from the stored total.
     */
    @Test
    fun aPartiallyVisibleHistoricalFocusedResultIsProjected() = resultTest {
        val stored = completedAttempt(
            "focused", 1_000,
            "a1" to false, "k1" to true, "a2" to true,
            config = AssessmentConfig.Focused(AssessmentScope.Subtopic("android_lifecycle"), questionCount = 3),
        )
        val attempts = FixtureAssessmentRepository(stored)
        val holder = curriculumVisibilityStateHolder(includeKmpContent = false)
        val viewModel = focusedViewModel("focused", attempts, holder)
        advanceUntilIdle()

        val hidden = assertIs<FocusedResultUiState.Content>(viewModel.uiState.value)
        assertEquals("focused", hidden.attemptId)
        assertEquals(1 to 2, hidden.correctAnswers to hidden.totalQuestions)
        assertEquals(listOf("a1", "a2"), hidden.questions.ids())
        assertEquals(1, hidden.hiddenQuestionCount)

        holder.setIncludeKmpContent(true)
        advanceUntilIdle()
        val shown = assertIs<FocusedResultUiState.Content>(viewModel.uiState.value)
        assertEquals(2 to 3, shown.correctAnswers to shown.totalQuestions)
        assertEquals(listOf("a1", "k1", "a2"), shown.questions.ids())
        assertEquals(stored, attempts.snapshot().getValue("focused"))
    }

    /** The StateFlow's replayed value does not become a second startup read. */
    @Test
    fun aResultReadsItsAttemptOnceAtStartup() = resultTest {
        val attempts = FixtureAssessmentRepository(completedAttempt("mixed", 1_000, "a1" to true))
        mixedViewModel("mixed", attempts, curriculumVisibilityStateHolder(false))
        focusedViewModel("mixed", attempts, curriculumVisibilityStateHolder(false))
        advanceUntilIdle()

        assertEquals(listOf("mixed", "mixed"), attempts.getByIdCalls)
    }

    private fun List<ReviewQuestionItem>.ids(): List<String> = map {
        when (it) {
            is ReviewQuestionItem.Available -> it.question.questionId
            is ReviewQuestionItem.Missing -> it.questionId
        }
    }

    private fun TestScope.mixedViewModel(
        attemptId: String,
        attempts: FixtureAssessmentRepository,
        holder: CurriculumVisibilityStateHolder,
    ): MixedInterviewResultViewModel {
        val raw = FixtureCurriculumRepository()
        return MixedInterviewResultViewModel(
            attemptId = attemptId,
            assessmentRepository = attempts,
            curriculumRepository = raw,
            assessmentReviewLoader = AssessmentReviewLoader(raw),
            assessmentRetakeService = retakeService(attempts, holder),
            savedQuestionStateHolder = savedQuestionStateHolder(),
            visibilityStateHolder = holder,
        )
    }

    private fun TestScope.focusedViewModel(
        attemptId: String,
        attempts: FixtureAssessmentRepository,
        holder: CurriculumVisibilityStateHolder,
    ): FocusedResultViewModel {
        val raw = FixtureCurriculumRepository()
        return FocusedResultViewModel(
            attemptId = attemptId,
            assessmentRepository = attempts,
            curriculumRepository = raw,
            assessmentReviewLoader = AssessmentReviewLoader(raw),
            assessmentRetakeService = retakeService(attempts, holder),
            savedQuestionStateHolder = savedQuestionStateHolder(),
            visibilityStateHolder = holder,
        )
    }

    /** Selection reads through the visible decorator, as the production graph binds it. */
    private fun retakeService(
        attempts: FixtureAssessmentRepository,
        holder: CurriculumVisibilityStateHolder,
    ): AssessmentRetakeService =
        AssessmentRetakeService(
            assessmentRepository = attempts,
            startAssessment = StartAssessment(
                assessmentRepository = attempts,
                assessmentEngine = AssessmentEngine(
                    questionSelector = AssessmentQuestionSelector(
                        curriculumRepository = VisibleCurriculumRepository(FixtureCurriculumRepository(), holder.visibility),
                        completedHistory = { emptyList() },
                        randomize = { it },
                    ),
                    generateAttemptId = { "retake" },
                    now = { Instant.fromEpochMilliseconds(5_000) },
                ),
            ),
        )

    private fun resultTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }
}
