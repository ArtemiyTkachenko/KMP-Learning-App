package org.artkachenko.kmp_learning_app.saved_questions

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.artkachenko.kmp_learning_app.appIntegrationMainDispatcherLock
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeService
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.data.local.assessment.AssessmentAttemptStore
import org.artkachenko.kmp_learning_app.data.local.assessment.repository.LocalAssessmentRepository
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImportResult
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.data.local.curriculum.repository.LocalCurriculumRepository
import org.artkachenko.kmp_learning_app.data.local.saved_questions.repository.LocalSavedQuestionRepository
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewStateHolder
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewUiState
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewViewModel
import org.artkachenko.kmp_learning_app.mixed_interview.MixedInterviewResultUiState
import org.artkachenko.kmp_learning_app.mixed_interview.MixedInterviewResultViewModel
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultUiState
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultViewModel

/**
 * The Saved Questions lifecycle across the boundaries no single-layer test owns: a real curriculum
 * re-import, a real restart, and the four surfaces that present one saved state.
 *
 * What is deliberately *not* repeated here: `LocalSavedQuestionRepositoryTest` owns the persistence
 * contract (duplicate-safe insert, timestamp stability, ordering, reconstruction),
 * `SavedQuestionContentResolverTest` owns the three resolution outcomes, and
 * `SavedQuestionCaptureIntegrationTest` owns the holder-to-table mutation path. These tests compose
 * those proven pieces and assert only what emerges from putting them together.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SavedQuestionLifecycleIntegrationTest {
    /**
     * Curriculum truth and learner-owned saved identity are separate stores with separate owners.
     *
     * A re-import rewrites what a Question *is* — its text, its explanation, its lifecycle status —
     * and must not touch which Questions the learner saved, when they saved them, or the order the
     * saved list comes back in. This passes because `CurriculumImporter` has no knowledge of
     * `saved_question` at all; were reconciliation ever added to the import, the timestamps
     * asserted below would be the first thing to move.
     */
    @Test
    fun curriculumReimportPreservesSavedIdentityAndHistoricalResolution() = runTest {
        withTestDatabase { database ->
            importCurriculum(database, initialCurriculum())
            var savedAt = 1_000L
            val savedRepository = LocalSavedQuestionRepository(database) {
                Instant.fromEpochMilliseconds(savedAt)
            }
            val curriculumRepository: CurriculumRepository = LocalCurriculumRepository(database)
            val contentResolver = SavedQuestionContentResolver(curriculumRepository)

            // Saved at distinct times, so the newest-first order is a real answer rather than the
            // identifier tie-break, and a re-import that touched it would be visible.
            savedRepository.save("q_active")
            savedAt = 2_000
            savedRepository.save("q_lifecycle")
            savedAt = 3_000
            // In no curriculum at all: an identity the learner saved from a bundle that has since
            // stopped shipping the Question.
            savedRepository.save("q_missing")

            val savedBefore = savedRepository.getSavedQuestions()
            assertEquals(
                listOf("q_missing", "q_lifecycle", "q_active"),
                savedBefore.map { it.questionId },
            )

            importCurriculum(database, reimportedCurriculum())

            // Identity, timestamps, and order, compared as whole records: a re-import is not a Save
            // action, so nothing here may be refreshed, reordered, pruned, or re-created.
            assertEquals(savedBefore, savedRepository.getSavedQuestions())
            assertEquals(3, database.savedQuestionDao().count())

            val resolved = contentResolver.resolve(savedBefore)
            assertEquals(
                savedBefore.map { it.questionId },
                resolved.map { it.questionId },
            )

            // The curriculum did change, and the saved identity resolves to what it now says.
            val active = assertIs<SavedQuestionItem.Available>(resolved[2])
            assertEquals("Updated active question?", active.question.text)
            assertEquals(
                ContentStatus.ACTIVE,
                curriculumRepository.getQuestionById("q_active")?.status,
            )

            // DEPRECATED is a curriculum lifecycle answer, not a saved-state one: the Question left
            // the ACTIVE catalogue and stays exactly as reviewable as an ACTIVE one.
            assertIs<SavedQuestionItem.Available>(resolved[1])
            assertEquals(
                ContentStatus.DEPRECATED,
                curriculumRepository.getQuestionById("q_lifecycle")?.status,
            )
            assertTrue(curriculumRepository.getActiveQuestions().none { it.id == "q_lifecycle" })

            // Import is not garbage collection. The stale identity survives, so removing it stays
            // the learner's decision to make on the Saved Questions screen.
            assertIs<SavedQuestionItem.Missing>(resolved[0])
            assertNull(curriculumRepository.getQuestionById("q_missing"))
            assertTrue(savedRepository.isSaved("q_missing"))
        }
    }

    /**
     * A restart, then browsing.
     *
     * `LocalSavedQuestionRepositoryTest` proves an identity survives reconstruction at the
     * repository. What this adds is the rest of the path a returning learner takes: a reconstructed
     * database, rebuilt repositories, a fresh holder, and the browsing screen resolving all three
     * lifecycle outcomes in the saved order the previous session left behind.
     */
    @Test
    fun savedQuestionsRemainBrowsableAfterDatabaseAndRepositoryReconstruction() =
        withMainDispatcher {
            val directory = Files.createTempDirectory("saved-question-lifecycle-test")
            val databasePath = directory.resolve("curriculum.db").toString()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

            try {
                val firstSession = openDatabase(databasePath)
                try {
                    importCurriculum(firstSession, reimportedCurriculum())
                    var savedAt = 1_000L
                    val repository = LocalSavedQuestionRepository(firstSession) {
                        Instant.fromEpochMilliseconds(savedAt)
                    }
                    repository.save("q_active")
                    savedAt = 2_000
                    repository.save("q_lifecycle")
                    savedAt = 3_000
                    repository.save("q_missing")
                } finally {
                    firstSession.close()
                }

                val secondSession = openDatabase(databasePath)
                try {
                    val browser = SavedQuestionsViewModel(
                        savedQuestionStateHolder = SavedQuestionStateHolder(
                            repository = LocalSavedQuestionRepository(secondSession),
                            scope = scope,
                        ),
                        contentResolver = SavedQuestionContentResolver(
                            LocalCurriculumRepository(secondSession),
                        ),
                    )

                    val content = assertIs<SavedQuestionsUiState.Content>(
                        browser.uiState.await { it is SavedQuestionsUiState.Content },
                    )
                    assertEquals(
                        listOf("q_missing", "q_lifecycle", "q_active"),
                        content.items.map { it.questionId },
                    )
                    assertIs<SavedQuestionItem.Missing>(content.items[0])
                    // The DEPRECATED Question comes back on the same branch as the ACTIVE one.
                    assertIs<SavedQuestionItem.Available>(content.items[1])
                    assertIs<SavedQuestionItem.Available>(content.items[2])
                } finally {
                    secondSession.close()
                }
            } finally {
                scope.cancel()
                directory.toFile().deleteRecursively()
            }
        }

    /**
     * One holder, four consumers.
     *
     * Each surface's own ViewModel test proves it can save and unsave against a repository of its
     * own. That is a weaker claim than this one: here the Focused result, the Mixed result, Mistake
     * Review, and the browsing destination are built over a single [SavedQuestionStateHolder] on a
     * single real table, and a mutation made through one of them has to be what the other three
     * report — none of them re-reading the saved table for itself.
     *
     * The unsave deliberately goes through a different surface than the save, so a pass cannot be
     * explained by one surface reading back its own write.
     */
    @Test
    fun sharedSavedStateFlowsAcrossReviewAndBrowsingSurfaces() =
        withMainDispatcher {
            withTestDatabase { database ->
                val surfaces = reviewSurfaces(database)
                try {
                    // Every surface must show q1 as available review content before it may mutate:
                    // the E18-02 guard only lets a screen save what it is currently showing.
                    val focusedContent = assertIs<FocusedResultUiState.Content>(
                        surfaces.focused.uiState.await { it is FocusedResultUiState.Content },
                    )
                    assertTrue(focusedContent.questions.any { it.isAvailable("q1") })
                    val mixedContent = assertIs<MixedInterviewResultUiState.Content>(
                        surfaces.mixed.uiState.await { it is MixedInterviewResultUiState.Content },
                    )
                    assertTrue(mixedContent.questions.any { it.isAvailable("q1") })
                    val mistakeContent = assertIs<MistakeReviewUiState.Content>(
                        surfaces.mistakes.uiState.await { it is MistakeReviewUiState.Content },
                    )
                    assertTrue(mistakeContent.mistakes.any { it.reviewItem.isAvailable("q1") })
                    surfaces.awaitSaved(emptySet())

                    surfaces.focused.toggleSaved("q1")

                    surfaces.awaitSaved(setOf("q1"))
                    assertEquals(
                        listOf("q1"),
                        surfaces.savedRepository.getSavedQuestions().map { it.questionId },
                    )
                    assertEquals(1, database.savedQuestionDao().count())
                    val browsed = assertIs<SavedQuestionsUiState.Content>(
                        surfaces.browser.uiState.await { it is SavedQuestionsUiState.Content },
                    )
                    assertEquals(listOf("q1"), browsed.items.map { it.questionId })

                    // Saved state is learner intent about a Question, orthogonal to how it was
                    // answered. Nothing derived from assessment history may have moved: not the
                    // persisted attempts, not the reviewed results, not the mistake queue.
                    assertEquals(
                        surfaces.completedAttempts,
                        surfaces.assessmentRepository.getCompletedAttempts(),
                    )
                    assertEquals(
                        focusedContent,
                        assertIs<FocusedResultUiState.Content>(surfaces.focused.uiState.value),
                    )
                    assertEquals(
                        mistakeContent.mistakes,
                        assertIs<MistakeReviewUiState.Content>(surfaces.mistakes.uiState.value).mistakes,
                    )

                    // Unsaved from Mistake Review, observed everywhere — including the browser,
                    // whose list empties because the only identity it was showing is gone.
                    surfaces.mistakes.toggleSaved("q1")

                    surfaces.awaitSaved(emptySet())
                    assertEquals(0, database.savedQuestionDao().count())
                    surfaces.browser.uiState.await { it is SavedQuestionsUiState.Empty }
                } finally {
                    surfaces.close()
                }
            }
        }

    /** Every review and browsing surface, over one holder and one real saved-Question table. */
    private suspend fun reviewSurfaces(database: CurriculumDatabase): ReviewSurfaces {
        importCurriculum(database, reviewCurriculum())
        val curriculumRepository: CurriculumRepository = LocalCurriculumRepository(database)
        val assessmentRepository: AssessmentRepository =
            LocalAssessmentRepository(AssessmentAttemptStore(database))
        assessmentRepository.save(
            completedAttempt(
                id = "focused_attempt",
                config = AssessmentConfig.Focused(
                    scope = AssessmentScope.Topic("topic"),
                    questionCount = 1,
                ),
            ),
        )
        assessmentRepository.save(
            completedAttempt(
                id = "mixed_attempt",
                config = AssessmentConfig.Mixed(questionCount = 1),
            ),
        )

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val savedRepository = LocalSavedQuestionRepository(database)
        val holder = SavedQuestionStateHolder(savedRepository, scope)
        val reviewLoader = AssessmentReviewLoader(curriculumRepository)
        val retakeService = AssessmentRetakeService(
            assessmentRepository = assessmentRepository,
            assessmentEngine = AssessmentEngine(
                questionSelector = AssessmentQuestionSelector(
                    curriculumRepository = curriculumRepository,
                    completedHistory = { assessmentRepository.getCompletedAttempts() },
                    randomize = { it },
                ),
                generateAttemptId = { "retake_attempt" },
            ),
        )
        val historyStore = AssessmentHistoryStore(assessmentRepository, scope)

        return ReviewSurfaces(
            scope = scope,
            savedRepository = savedRepository,
            assessmentRepository = assessmentRepository,
            completedAttempts = assessmentRepository.getCompletedAttempts(),
            focused = FocusedResultViewModel(
                attemptId = "focused_attempt",
                assessmentRepository = assessmentRepository,
                assessmentReviewLoader = reviewLoader,
                assessmentRetakeService = retakeService,
                savedQuestionStateHolder = holder,
            ),
            mixed = MixedInterviewResultViewModel(
                attemptId = "mixed_attempt",
                assessmentRepository = assessmentRepository,
                curriculumRepository = curriculumRepository,
                assessmentReviewLoader = reviewLoader,
                assessmentRetakeService = retakeService,
                savedQuestionStateHolder = holder,
            ),
            mistakes = MistakeReviewViewModel(
                historyStore = historyStore,
                stateHolder = MistakeReviewStateHolder(
                    mistakeReviewService = MistakeReviewService(
                        assessmentRepository = assessmentRepository,
                        assessmentReviewLoader = reviewLoader,
                    ),
                    historyStore = historyStore,
                    scope = scope,
                ),
                savedQuestionStateHolder = holder,
            ),
            browser = SavedQuestionsViewModel(
                savedQuestionStateHolder = holder,
                contentResolver = SavedQuestionContentResolver(curriculumRepository),
            ),
        )
    }

    private class ReviewSurfaces(
        private val scope: CoroutineScope,
        val savedRepository: LocalSavedQuestionRepository,
        val assessmentRepository: AssessmentRepository,
        val completedAttempts: List<TestAttempt>,
        val focused: FocusedResultViewModel,
        val mixed: MixedInterviewResultViewModel,
        val mistakes: MistakeReviewViewModel,
        val browser: SavedQuestionsViewModel,
    ) {
        /** The claim under test: all three review surfaces settle on the same saved set. */
        suspend fun awaitSaved(expected: Set<String>) {
            listOf(focused.savedQuestions, mixed.savedQuestions, mistakes.savedQuestions)
                .forEach { savedState ->
                    savedState.await { current ->
                        current is SavedQuestionsState.Loaded &&
                            current.pendingQuestionIds.isEmpty() &&
                            current.savedQuestionIds == expected
                    }
                }
        }

        fun close() = scope.cancel()
    }

    private suspend fun importCurriculum(database: CurriculumDatabase, curriculum: Curriculum) {
        assertIs<CurriculumImportResult.Imported>(
            CurriculumImporter(database, loadCurriculum = { curriculum }).importCurriculum(),
        )
    }

    private suspend fun withTestDatabase(block: suspend (CurriculumDatabase) -> Unit) {
        val database = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        try {
            block(database)
        } finally {
            database.close()
        }
    }

    private fun openDatabase(path: String): CurriculumDatabase =
        Room.databaseBuilder<CurriculumDatabase>(name = path)
            .setDriver(BundledSQLiteDriver())
            .build()

    /**
     * `viewModelScope` dispatches on Main, and these are the production ViewModels, so Main has to
     * exist. Unconfined rather than a test dispatcher, and `runBlocking` rather than `runTest`,
     * because the work being awaited is Room's: it runs on Room's own executor and would never be
     * advanced by a test scheduler. The shared lock is held for the same reason the other
     * Main-replacing integration tests hold it — Main is global state.
     */
    private fun withMainDispatcher(block: suspend () -> Unit) {
        synchronized(appIntegrationMainDispatcherLock) {
            Dispatchers.setMain(Dispatchers.Unconfined)
            try {
                runBlocking { block() }
            } finally {
                Dispatchers.resetMain()
            }
        }
    }

    /** `q_active` and `q_lifecycle` both ACTIVE; `q_missing` is in no curriculum at all. */
    private fun initialCurriculum(): Curriculum =
        curriculumOf(
            question("q_active", "Active question?", ContentStatus.ACTIVE),
            question("q_lifecycle", "Lifecycle question?", ContentStatus.ACTIVE),
        )

    /** The same bundle a release later: `q_active` reworded, `q_lifecycle` retired. */
    private fun reimportedCurriculum(): Curriculum =
        curriculumOf(
            question("q_active", "Updated active question?", ContentStatus.ACTIVE),
            question("q_lifecycle", "Lifecycle question?", ContentStatus.DEPRECATED),
        )

    private fun reviewCurriculum(): Curriculum =
        curriculumOf(
            question("q1", "First question?", ContentStatus.ACTIVE),
            question("q2", "Second question?", ContentStatus.ACTIVE),
        )

    private fun curriculumOf(vararg questions: Question): Curriculum =
        Curriculum(
            topics = listOf(Topic("topic", "Topic")),
            subtopics = listOf(Subtopic("subtopic", "topic", "Subtopic")),
            questions = questions.toList(),
        )

    private fun question(id: String, text: String, status: ContentStatus): Question =
        Question(
            id = id,
            topicId = "topic",
            subtopicId = "subtopic",
            text = text,
            answers = listOf(
                AnswerOption("${id}_a", "Answer A"),
                AnswerOption("${id}_b", "Answer B"),
            ),
            selectionMode = AnswerSelectionMode.SINGLE,
            level = QuestionLevel.FOUNDATION,
            correctAnswerIds = listOf("${id}_a"),
            explanation = "$id explanation.",
            sources = listOf(SourceReference("$id source", "https://example.com/$id")),
            status = status,
        )

    /**
     * One completed attempt over `q1`, answered incorrectly so the same Question also reaches the
     * mistake queue — which is what puts it in front of all three review surfaces at once.
     */
    private fun completedAttempt(id: String, config: AssessmentConfig): TestAttempt =
        TestAttempt(
            id = id,
            config = config,
            questionAttempts = listOf(
                QuestionAttempt(
                    questionId = "q1",
                    answerState = QuestionAnswerState.Answered(setOf("q1_b"), isCorrect = false),
                ),
            ),
            status = AssessmentStatus.COMPLETED,
            startedAt = Instant.fromEpochMilliseconds(1_000),
            completedAt = Instant.fromEpochMilliseconds(2_000),
            score = AssessmentScore(totalQuestions = 1, correctAnswers = 0),
        )
}

private fun ReviewQuestionItem.isAvailable(questionId: String): Boolean =
    this is ReviewQuestionItem.Available && question.questionId == questionId

/**
 * Room runs its queries on its own executor rather than the test scheduler, so settled state is
 * awaited rather than advanced to.
 */
private suspend fun <T> Flow<T>.await(predicate: (T) -> Boolean): T =
    withContext(Dispatchers.Default) {
        withTimeout(10_000) { first(predicate) }
    }
