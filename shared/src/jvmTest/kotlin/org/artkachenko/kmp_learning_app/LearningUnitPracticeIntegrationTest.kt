package org.artkachenko.kmp_learning_app

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.artkachenko.kmp_learning_app.assessment.AllQuestionLevels
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment.history.AppCoroutineScope
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentSelectionResult
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingLaunch
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingUiState
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingViewModel
import org.artkachenko.kmp_learning_app.curriculum.content.BundledCurriculumSource
import org.artkachenko.kmp_learning_app.curriculum.learning.content.learningContentModule
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImportResult
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.data.local.saved_questions.savedQuestionDataModule
import org.artkachenko.kmp_learning_app.topic_study.focused_practice.toAssessmentConfig
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeAvailability
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderEvent
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderTarget
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderUiState
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderViewModel
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeScopeKind
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.toPracticeRoute
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.koin.core.Koin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * E21-06: the shipped Learning Unit practised through the production graph.
 *
 * Every layer already has its own proof — the derivation in `PracticeTargetResolverTest`, the
 * builder's states in `PracticeBuilderViewModelTest`, the multi-Subtopic selection and persistence
 * in the E21-05 suites. What none of them can show is whether the *authored* Unit, the *authored*
 * Question bank, and the real selection and persistence path still agree: whether pressing
 * "Practice this unit" on the Unit that actually ships reaches Questions that actually exist.
 *
 * So neither side is a fixture here. The learning document is the bundled one and the curriculum is
 * imported from the bundled one through the real importer, which makes this test sensitive to
 * content: retiring a Lesson, re-pointing a primary concept, or deprecating the Questions behind
 * one is meant to fail here rather than pass silently.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class LearningUnitPracticeIntegrationTest {
    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /**
     * The whole runtime flow in one pass: the route's Unit ID becomes the current Unit, its ACTIVE
     * Lessons' primary concepts become the scope, and the ordinary focused engine runs it.
     */
    @Test
    fun theShippedComposeUnitPractisesTheConceptsItTeachesThroughTheExistingEngine() =
        runUnitPracticeTest {
            val builder = builder(PracticeBuilderTarget.LearningUnit(ComposeUnitId))
            val settled = builder.settled()

            // The title comes from the resolved Unit, not from the route and not from a concept
            // count: the learner is told what they are about to practise.
            assertEquals(PracticeScopeKind.LEARNING_UNIT, settled.scope.kind)
            assertEquals(ComposeUnitTitle, settled.scope.name)
            val available = assertIs<PracticeAvailability.Available>(settled.availability)
            assertTrue(
                available.eligibleQuestionCount > 0,
                "The shipped Unit reached no authored Questions.",
            )
            // Preflight reads eligibility; it must never create an attempt to find out.
            assertEquals(0, attemptCount())

            val config = builder.start()

            // The authoring rules, asserted against the content that ships: `compose_fundamentals`
            // is primary in two Lessons and appears once; the supporting concepts those Lessons
            // lean on — Views, recomposition, architecture UDF, state hoisting — are absent.
            assertEquals(
                AssessmentConfig.Focused(
                    scope = AssessmentScope.Subtopics(
                        setOf("compose_fundamentals", "compose_udf"),
                    ),
                    questionCount = settled.questionCount,
                    levels = AllQuestionLevels,
                    source = PracticeQuestionSource.ALL,
                ),
                config,
            )

            // The structural claim the acceptance criterion makes: the derived scope really does
            // reach the ACTIVE Questions authored against those Subtopics.
            val selected = selectedQuestions(config)
            assertTrue(selected.isNotEmpty(), "The Unit scope selected no Questions.")
            selected.forEach { question ->
                assertContains(
                    setOf("compose_fundamentals", "compose_udf"),
                    question.subtopicId,
                    "Selected ${question.id} outside the Unit's primary concepts.",
                )
            }

            // The configured run survives the back stack unchanged, exactly as Topic and Subtopic
            // runs do, and arrives at assessment taking as a plain multi-Subtopic scope.
            val route = assertIs<AppRoute.FocusedSubtopicsPractice>(config.toPracticeRoute())
            assertEquals(config, route.toAssessmentConfig())

            val attemptId = runPractice(route.toAssessmentConfig())

            // One ordinary attempt, through the ordinary engine, scored and completed.
            assertEquals(1, attemptCount())
            val stored = assertNotNull(assessmentRepository.getById(attemptId))
            assertEquals(AssessmentStatus.COMPLETED, stored.status)
            assertEquals(config, stored.config)
        }

    /**
     * Assessment history records the assessment, not the study material that suggested it.
     *
     * The stored row keeps the concepts the run actually asked about, so a later re-authoring of
     * the Unit cannot retroactively change what a finished attempt was — and nothing in the row
     * names the Unit, which is what keeps the assessment domain independent of learning content.
     */
    @Test
    fun aFinishedUnitRunPersistsItsConceptsAndNotTheUnitItCameFrom() = runUnitPracticeTest {
        val config = builder(PracticeBuilderTarget.LearningUnit(ComposeUnitId)).also { it.settled() }
            .start()

        val attemptId = runPractice(config)

        val row = assertNotNull(database.assessmentAttemptDao().getTestAttemptById(attemptId))
        assertFalse(
            row.scopeId.orEmpty().contains(ComposeUnitId),
            "The persisted scope named the Learning Unit: ${row.scopeId}.",
        )
        assertFalse(
            row.scopeId.orEmpty().contains(ComposeUnitTitle),
            "The persisted scope carried a presentation label: ${row.scopeId}.",
        )
        // Reconstruction is generic and complete: the attempt reopens as the multi-Subtopic run it
        // was, with no learning content consulted.
        val reconstructed = assertNotNull(assessmentRepository.getById(attemptId)).config
        assertEquals(
            AssessmentScope.Subtopics(setOf("compose_fundamentals", "compose_udf")),
            assertIs<AssessmentConfig.Focused>(reconstructed).scope,
        )
    }

    /** A stale route that names no current Unit fails safely rather than practising something else. */
    @Test
    fun aRouteNamingNoCurrentUnitStartsNothing() = runUnitPracticeTest {
        val builder = builder(PracticeBuilderTarget.LearningUnit("unit_that_was_retired"))

        val settled = builder.settled()
        builder.startPractice()

        assertEquals(PracticeAvailability.TargetUnavailable, settled.availability)
        assertFalse(settled.isStartEnabled)
        assertEquals(0, attemptCount())
    }
}

/**
 * Boots the production graph over an in-memory database holding the *bundled* curriculum.
 *
 * Only determinism is overridden: the database instance, the engine's attempt IDs and clock, and
 * the selector's randomizer. Everything the assertions depend on — which Questions exist, which
 * concepts the Unit teaches — is shipped content.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private fun runUnitPracticeTest(block: suspend UnitPracticeGraph.() -> Unit) = runTest {
    Dispatchers.setMain(Dispatchers.Unconfined)
    val database = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    assertEquals(
        CurriculumImportResult.Imported,
        CurriculumImporter(database, loadCurriculum = { BundledCurriculumSource.load() })
            .importCurriculum(),
    )

    var attemptSequence = 0
    var clockSequence = 0L
    val app = koinApplication {
        modules(
            curriculumDataModule,
            learningContentModule,
            assessmentDataModule,
            savedQuestionDataModule,
            topicStudyPresentationModule,
            module {
                single<CurriculumDatabase> { database }
                single {
                    AssessmentQuestionSelector(
                        curriculumRepository = get(),
                        completedHistory = get<AssessmentHistoryStore>(),
                        performanceDerivation = get(),
                        randomize = { it },
                    )
                }
                single {
                    AssessmentEngine(
                        questionSelector = get(),
                        generateAttemptId = { "attempt-${++attemptSequence}" },
                        now = {
                            clockSequence += 1
                            Instant.fromEpochMilliseconds(EpochMillis + clockSequence * 1_000)
                        },
                    )
                }
            },
        )
    }

    try {
        UnitPracticeGraph(database, app.koin).block()
    } finally {
        // The history store's refresh is app-scoped and outlives every screen by design, so it is
        // cancelled before the database closes underneath an in-flight query.
        app.koin.get<AppCoroutineScope>().cancel()
        app.close()
        database.close()
    }
}

private class UnitPracticeGraph(
    val database: CurriculumDatabase,
    private val koin: Koin,
) {
    val assessmentRepository: AssessmentRepository get() = koin.get()
    private val questionSelector: AssessmentQuestionSelector get() = koin.get()
    private val curriculumRepository: CurriculumRepository get() = koin.get()

    fun builder(target: PracticeBuilderTarget): PracticeBuilderViewModel =
        koin.get { parametersOf(target) }

    suspend fun attemptCount(): Int = database.assessmentAttemptDao().countTestAttempts()

    suspend fun selectedQuestions(config: AssessmentConfig) =
        when (val selection = questionSelector.select(config)) {
            is AssessmentSelectionResult.Selected -> selection.questions
            is AssessmentSelectionResult.NoContent -> emptyList()
        }

    /** Answers every question correctly and completes, through the ViewModel that owns persistence. */
    suspend fun runPractice(config: AssessmentConfig): String {
        val viewModel: AssessmentTakingViewModel =
            koin.get { parametersOf(AssessmentTakingLaunch.New(config)) }
        var questionNumber = 1
        while (true) {
            val state = viewModel.awaitQuestion(questionNumber)
            if (state is AssessmentTakingUiState.ReadyToComplete) break
            val content = assertIs<AssessmentTakingUiState.Content>(state)
            // The authored key, read from the curriculum: this suite runs on real Questions, so
            // no naming convention can stand in for the correct answer.
            val question = assertNotNull(curriculumRepository.getQuestionById(content.question.id))
            question.correctAnswerIds.forEach(viewModel::selectAnswer)
            viewModel.submitAnswer()
            questionNumber += 1
        }
        viewModel.completeAssessment()
        return assertIs<AssessmentTakingUiState.CompletionSucceeded>(
            viewModel.uiState.await { it is AssessmentTakingUiState.CompletionSucceeded },
        ).attemptId
    }
}

private suspend fun AssessmentTakingViewModel.awaitQuestion(
    questionNumber: Int,
): AssessmentTakingUiState = uiState.await { state ->
    when (state) {
        is AssessmentTakingUiState.Content ->
            !state.isSubmitting && state.questionNumber == questionNumber
        is AssessmentTakingUiState.ReadyToComplete -> !state.isCompleting
        AssessmentTakingUiState.NoQuestions,
        AssessmentTakingUiState.Error,
        -> error("Assessment taking reached $state instead of question $questionNumber.")
        else -> false
    }
}

private suspend fun PracticeBuilderViewModel.settled(): PracticeBuilderUiState =
    uiState.await { it.availability !is PracticeAvailability.Checking }

private suspend fun PracticeBuilderViewModel.start(): AssessmentConfig.Focused {
    startPractice()
    val event = withContext(Dispatchers.Default) {
        withTimeout(AwaitTimeoutMillis) { events.first() }
    }
    return assertIs<PracticeBuilderEvent.StartPractice>(event).config
}

/** Real Room work runs on Room's own executor, so waiting has to leave virtual time. */
private suspend fun <T> StateFlow<T>.await(predicate: (T) -> Boolean): T =
    withContext(Dispatchers.Default) {
        withTimeout(AwaitTimeoutMillis) { first(predicate) }
    }

private const val AwaitTimeoutMillis = 10_000L
private const val EpochMillis = 1_700_000_000_000L

/** The shipped Unit, not a fixture: see the class comment. */
private const val ComposeUnitId = "unit_thinking_in_compose"
private const val ComposeUnitTitle = "Thinking in Compose"
