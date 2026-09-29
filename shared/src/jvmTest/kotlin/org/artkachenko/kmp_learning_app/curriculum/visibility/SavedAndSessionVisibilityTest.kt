package org.artkachenko.kmp_learning_app.curriculum.visibility

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSessionLoadResult
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSessionLoader
import org.artkachenko.kmp_learning_app.assessment.session.CompleteAssessment
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingUiState
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingViewModel
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.saved_questions.FakeSavedQuestionRepository
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestion
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionContentResolver
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionItem
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionsState
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionsUiState
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionsViewModel
import org.artkachenko.kmp_learning_app.saved_questions.savedQuestionStateHolder

/**
 * Saved Questions and in-progress attempts under curriculum visibility. Both keep their stored
 * records exactly as they are: saved identities are presented through the visibility, and an
 * in-progress attempt is shown whole or not at all.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SavedAndSessionVisibilityTest {
    private val hidden = CurriculumVisibility.from(includeKmpContent = false)
    private val shown = CurriculumVisibility.from(includeKmpContent = true)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun theResolverOmitsHiddenQuestionsAndKeepsMissingAndDeprecatedOnes() = runTest {
        val retired = question("a_old", VisibilityFixture.androidLifecycle).copy(status = ContentStatus.DEPRECATED)
        val resolver = SavedQuestionContentResolver(
            FixtureCurriculumRepository(questions = VisibilityFixture.questions + retired),
        )
        val saved = listOf("k1", "a_old", "gone", "a1").mapIndexed { index, id -> SavedQuestion(id, 100L - index) }

        val hiddenItems = resolver.resolve(saved, hidden)
        // The KMP Question is left out rather than shown as Missing; the unresolved one stays
        // Missing because its Topic cannot be classified; a deprecated visible one stays reviewable.
        assertEquals(listOf("a_old", "gone", "a1"), hiddenItems.map { it.questionId })
        assertIs<SavedQuestionItem.Available>(hiddenItems[0])
        assertIs<SavedQuestionItem.Missing>(hiddenItems[1])

        assertEquals(listOf("k1", "a_old", "gone", "a1"), resolver.resolve(saved, shown).map { it.questionId })
    }

    @Test
    fun savedQuestionsFollowVisibilityWithoutTouchingSavedState() = savedTest {
        val repository = FakeSavedQuestionRepository(listOf(SavedQuestion("k1", 200), SavedQuestion("a1", 100)))
        val savedState = savedQuestionStateHolder(repository)
        val holder = curriculumVisibilityStateHolder(includeKmpContent = false)
        val viewModel = SavedQuestionsViewModel(savedState, SavedQuestionContentResolver(FixtureCurriculumRepository()), holder)
        advanceUntilIdle()
        val readsAfterOpening = repository.readCalls

        assertEquals(listOf("a1"), content(viewModel).items.map { it.questionId })
        // The raw learner-owned state still holds both identities.
        assertEquals(setOf("k1", "a1"), assertIs<SavedQuestionsState.Loaded>(savedState.state.value).savedQuestionIds)

        holder.setIncludeKmpContent(true)
        advanceUntilIdle()
        assertEquals(listOf("k1", "a1"), content(viewModel).items.map { it.questionId })

        holder.setIncludeKmpContent(false)
        advanceUntilIdle()
        assertEquals(listOf("a1"), content(viewModel).items.map { it.questionId })

        // The transitions re-resolved content only: no saved-table read, save or removal.
        assertEquals(readsAfterOpening, repository.readCalls)
        assertEquals(emptyList(), repository.saveCalls)
        assertEquals(emptyList(), repository.unsaveCalls)
    }

    @Test
    fun onlyHiddenSavedQuestionsIsEmptyAndShowingThemRestoresTheList() = savedTest {
        val repository = FakeSavedQuestionRepository(listOf(SavedQuestion("k1", 200), SavedQuestion("k2", 100)))
        val holder = curriculumVisibilityStateHolder(includeKmpContent = false)
        val viewModel = SavedQuestionsViewModel(
            savedQuestionStateHolder(repository),
            SavedQuestionContentResolver(FixtureCurriculumRepository()),
            holder,
        )
        advanceUntilIdle()
        assertEquals(SavedQuestionsUiState.Empty, viewModel.uiState.value)

        holder.setIncludeKmpContent(true)
        advanceUntilIdle()
        assertEquals(listOf("k1", "k2"), content(viewModel).items.map { it.questionId })
    }

    @Test
    fun pendingRemovalOfAVisibleItemIsStillReportedWhileKmpIsHidden() = savedTest {
        val repository = FakeSavedQuestionRepository(listOf(SavedQuestion("k1", 200), SavedQuestion("a1", 100)))
        val viewModel = SavedQuestionsViewModel(
            savedQuestionStateHolder(repository),
            SavedQuestionContentResolver(FixtureCurriculumRepository()),
            curriculumVisibilityStateHolder(includeKmpContent = false),
        )
        advanceUntilIdle()

        val gate = CompletableDeferred<Unit>()
        repository.unsaveGate = gate
        viewModel.removeSaved("a1")
        advanceUntilIdle()
        assertEquals(setOf("a1"), content(viewModel).pendingQuestionIds)

        gate.complete(Unit)
        advanceUntilIdle()
        // Only the visible Question was removed; the hidden one stays saved, and nothing is left.
        assertEquals(listOf("a1"), repository.unsaveCalls)
        assertEquals(SavedQuestionsUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun theLoaderRefusesAnyInProgressAttemptWithAHiddenQuestion() = runTest {
        val attempts = FixtureAssessmentRepository(
            inProgressAttempt("core", "a1", "c1"),
            inProgressAttempt("kmp", "k1", "k2"),
            inProgressAttempt("mixed", "a1", "k1"),
            inProgressAttempt("unresolved", "a1", "gone"),
        )
        val before = attempts.snapshot()
        val visibility = VisibilityFixture.visibility(includeKmpContent = false)
        val loader = AssessmentSessionLoader(attempts, FixtureCurriculumRepository(), visibility)

        assertIs<AssessmentSessionLoadResult.Loaded>(loader.load("core"))
        assertEquals(AssessmentSessionLoadResult.ContentUnavailable, loader.load("kmp"))
        // Never shortened: one hidden Question makes the whole attempt unavailable.
        assertEquals(AssessmentSessionLoadResult.ContentUnavailable, loader.load("mixed"))
        // Missing metadata is still MissingQuestion, not a hidden Topic.
        assertEquals(AssessmentSessionLoadResult.MissingQuestion("gone"), loader.load("unresolved"))

        visibility.value = shown
        val kmp = assertIs<AssessmentSessionLoadResult.Loaded>(loader.load("kmp"))
        assertEquals(listOf("k1", "k2"), kmp.session.questions.map { it.id })
        val mixed = assertIs<AssessmentSessionLoadResult.Loaded>(loader.load("mixed"))
        assertEquals(2, mixed.session.questions.size)

        assertEquals(before, attempts.snapshot())
        assertEquals(0, attempts.saves)
    }

    @Test
    fun takingAHiddenAttemptIsUnavailableAndResumesUnchangedWhenShown() = savedTest {
        val attempts = FixtureAssessmentRepository(inProgressAttempt("mixed", "a1", "k1"))
        val holder = curriculumVisibilityStateHolder(includeKmpContent = false)
        val viewModel = takingViewModel("mixed", attempts, holder)
        advanceUntilIdle()
        assertEquals(AssessmentTakingUiState.Unavailable, viewModel.uiState.value)
        // Not a failure, so Retry does nothing.
        viewModel.retry()
        advanceUntilIdle()
        assertEquals(AssessmentTakingUiState.Unavailable, viewModel.uiState.value)

        holder.setIncludeKmpContent(true)
        advanceUntilIdle()
        val content = assertIs<AssessmentTakingUiState.Content>(viewModel.uiState.value)
        assertEquals(1 to 2, content.questionNumber to content.totalQuestions)
        assertEquals(0, attempts.saves)
    }

    /** A session on screen is withdrawn the moment one of its Questions becomes hidden. */
    @Test
    fun aLiveSessionIsWithdrawnWhenItsContentIsHidden() = savedTest {
        val attempts = FixtureAssessmentRepository(inProgressAttempt("kmp", "k1", "k2"))
        val holder = curriculumVisibilityStateHolder(includeKmpContent = true)
        val viewModel = takingViewModel("kmp", attempts, holder)
        advanceUntilIdle()
        assertIs<AssessmentTakingUiState.Content>(viewModel.uiState.value)

        holder.setIncludeKmpContent(false)
        advanceUntilIdle()
        assertEquals(AssessmentTakingUiState.Unavailable, viewModel.uiState.value)

        holder.setIncludeKmpContent(true)
        advanceUntilIdle()
        assertIs<AssessmentTakingUiState.Content>(viewModel.uiState.value)
        assertEquals(0, attempts.saves)
    }

    @Test
    fun aCoreSessionIsUnaffectedByHidingKmp() = savedTest {
        val attempts = FixtureAssessmentRepository(inProgressAttempt("core", "a1", "c1"))
        val holder = curriculumVisibilityStateHolder(includeKmpContent = true)
        val viewModel = takingViewModel("core", attempts, holder)
        advanceUntilIdle()
        viewModel.selectAnswer("a1_a")
        val selected = assertIs<AssessmentTakingUiState.Content>(viewModel.uiState.value)

        holder.setIncludeKmpContent(false)
        advanceUntilIdle()

        // The same state, the learner's unsubmitted selection included: nothing was reloaded.
        assertEquals(selected, viewModel.uiState.value)
        assertEquals(listOf("core"), attempts.getByIdCalls)
    }

    private fun TestScope.takingViewModel(
        attemptId: String,
        attempts: FixtureAssessmentRepository,
        holder: CurriculumVisibilityStateHolder,
    ): AssessmentTakingViewModel {
        val curriculum = FixtureCurriculumRepository()
        val engine = AssessmentEngine(
            questionSelector = AssessmentQuestionSelector(
                curriculumRepository = VisibleCurriculumRepository(curriculum, holder.visibility),
                completedHistory = { emptyList() },
                randomize = { it },
            ),
            generateAttemptId = { error("start must not be called") },
            now = { Instant.fromEpochMilliseconds(2_000) },
        )
        return AssessmentTakingViewModel(
            attemptId = attemptId,
            assessmentEngine = engine,
            assessmentRepository = attempts,
            assessmentSessionLoader = AssessmentSessionLoader(attempts, curriculum, holder.visibility),
            completeAttempt = CompleteAssessment(
                assessmentEngine = engine,
                assessmentRepository = attempts,
                historyStore = AssessmentHistoryStore(attempts, CoroutineScope(SupervisorJob())),
            ),
            visibilityStateHolder = holder,
        )
    }

    private fun content(viewModel: SavedQuestionsViewModel) =
        assertIs<SavedQuestionsUiState.Content>(viewModel.uiState.value)

    private fun savedTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }
}
