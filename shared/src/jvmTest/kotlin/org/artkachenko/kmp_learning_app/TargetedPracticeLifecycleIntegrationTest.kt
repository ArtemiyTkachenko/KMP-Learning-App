package org.artkachenko.kmp_learning_app

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
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
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.history.AppCoroutineScope
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.history.QuestionExposure
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeResult
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeService
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentSelectionResult
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingLaunch
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingUiState
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingViewModel
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
import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImportResult
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.learning_progress.SubtopicCoverage
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService
import org.artkachenko.kmp_learning_app.topic_study.focused_practice.toAssessmentConfig
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultUiState
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultViewModel
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeAvailability
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderEvent
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderUiState
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderViewModel
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.toPracticeRoute
import org.artkachenko.kmp_learning_app.data.local.saved_questions.savedQuestionDataModule
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.koin.core.Koin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * E16-06: targeted practice verified across the boundaries its own layers cannot see.
 *
 * Every source policy is already covered against fakes — `AssessmentQuestionSelectorTest` alone
 * holds the scope, level and history rules for all four — and persistence, migration, routing and
 * the builder's own invariants each have their own tests. What none of them can show is whether the
 * pieces still agree once a real database, the production Koin graph and the real completion
 * lifecycle are between them: whether a completed run actually reaches the coverage, Progress and
 * mistake state that the *next* practice request reads.
 *
 * So these tests deliberately own as little as possible. The database is real, the curriculum is
 * imported through the real importer, and everything below `PracticeBuilderViewModel` and
 * `AssessmentTakingViewModel` is resolved from the production modules. Only three things are
 * overridden, all for determinism rather than behaviour: the database instance, the engine's
 * attempt IDs and clock, and the selector's randomizer. In particular the selector keeps the
 * production `AssessmentHistoryStore` as its history, so a scenario passes only if the ordinary
 * completion invalidation is what carries a finished attempt into the following selection.
 *
 * The invariant under test throughout is that the source chooses *which* Questions are asked and
 * nothing else: one engine, one scoring, one persistence path, one result, one history.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class TargetedPracticeLifecycleIntegrationTest {
    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun customAllPracticeRunsThroughTheProductionGraphAndPersistsExactlyOneAttempt() = runPracticeTest {
        val builder = builder(AssessmentScope.Topic(TopicA))
        assertEquals(
            PracticeAvailability.Available(TopicAQuestionIds.size),
            builder.settled().availability,
        )

        // Every control the learner can touch, each one re-running the eligibility read. Preflight
        // asks the selector, never the engine, so none of this may leave an attempt behind.
        builder.selectQuestionCount(5)
        builder.toggleLevel(QuestionLevel.FOUNDATION)
        assertEquals(PracticeAvailability.Available(4), builder.settled().availability)
        builder.selectSource(PracticeQuestionSource.UNSEEN)
        assertEquals(PracticeAvailability.Available(4), builder.settled().availability)
        builder.selectSource(PracticeQuestionSource.ALL)
        builder.toggleLevel(QuestionLevel.FOUNDATION)
        // Six Questions are eligible again, but availability reports what the run will actually
        // ask, and the count is still the five chosen above.
        assertEquals(PracticeAvailability.Available(5), builder.settled().availability)
        assertEquals(0, attemptCount())

        val configured = builder.start()
        assertEquals(
            AssessmentConfig.Focused(
                scope = AssessmentScope.Topic(TopicA),
                questionCount = 5,
                levels = AllQuestionLevels,
                source = PracticeQuestionSource.ALL,
            ),
            configured,
        )

        // The builder hands navigation a configuration, not content, and the practice destination
        // rebuilds it from route fields. Losing a dimension here would silently widen the run.
        val fromRoute = assertIs<AppRoute.FocusedTopicPractice>(configured.toPracticeRoute())
            .toAssessmentConfig()
        assertEquals(configured, fromRoute)

        val taking = startTaking(fromRoute)
        val started = requireNotNull(assessmentRepository.getById(FirstAttemptId))
        assertEquals(1, attemptCount())
        assertEquals(AssessmentStatus.IN_PROGRESS, started.status)
        assertEquals(fromRoute, started.config)
        assertNull(started.score)
        val askedIds = started.questionAttempts.map { it.questionId }
        assertEquals(5, askedIds.size)
        assertEquals(askedIds.size, askedIds.toSet().size)
        assertTrue(askedIds.all { it in TopicAQuestionIds })
        assertTrue(started.questionAttempts.all { it.answerState == QuestionAnswerState.Unanswered })

        val completedId = taking.answerAllAndComplete(correctFor = setOf(askedIds.first()))
        assertEquals(FirstAttemptId, completedId)

        val completed = requireNotNull(assessmentRepository.getById(completedId))
        assertEquals(1, attemptCount())
        assertEquals(AssessmentStatus.COMPLETED, completed.status)
        assertEquals(AssessmentScore(totalQuestions = 5, correctAnswers = 1), completed.score)
        assertEquals(fromRoute, completed.config)
        assertEquals(listOf(completedId), assessmentRepository.getCompletedAttempts().map { it.id })
    }

    @Test
    fun levelFilteringSurvivesRealSelectionAndPersistence() = runPracticeTest {
        // One Question per level in this Subtopic, so the level filter is the only thing that can
        // produce these answers. Eligibility comes from the real level-aware repository reads.
        assertEquals(
            listOf(AdvancedQuestion),
            selectedIds(practiceOnA1(levels = setOf(QuestionLevel.ADVANCED))),
        )
        assertEquals(
            listOf(AppliedQuestion, AdvancedQuestion),
            selectedIds(
                practiceOnA1(levels = setOf(QuestionLevel.APPLIED, QuestionLevel.ADVANCED)),
            ),
        )

        val config = practiceOnA1(
            levels = setOf(QuestionLevel.APPLIED, QuestionLevel.ADVANCED),
            source = PracticeQuestionSource.UNSEEN,
        )
        val attemptId = runPractice(config, correctFor = setOf(AdvancedQuestion))

        // History has to describe the run the learner configured, not the all-levels ALL default a
        // pre-v6 row reconstructs as.
        val persisted = requireNotNull(assessmentRepository.getById(attemptId))
        assertEquals(config, persisted.config)
        val row = requireNotNull(database.assessmentAttemptDao().getTestAttemptById(attemptId))
        assertEquals("APPLIED,ADVANCED", row.practiceLevels)
        assertEquals("UNSEEN", row.practiceSource)
        assertEquals(SubtopicA1, row.scopeId)
    }

    @Test
    fun unseenCompletionUpdatesCoverageAndFutureEligibility() = runPracticeTest {
        assertEquals(0, coverageOfA1().attemptedQuestionCount)
        assertEquals(A1QuestionIds, selectedIds(practiceOnA1(source = PracticeQuestionSource.UNSEEN)))

        runPractice(
            practiceOnA1(levels = setOf(QuestionLevel.FOUNDATION)),
            correctFor = setOf(FoundationQuestion),
        )

        assertEquals(1, coverageOfA1().attemptedQuestionCount)
        assertEquals(3, coverageOfA1().totalQuestionCount)
        assertEquals(
            listOf(AppliedQuestion, AdvancedQuestion),
            selectedIds(practiceOnA1(source = PracticeQuestionSource.UNSEEN)),
        )

        // Completing unseen practice is the only thing that happens here: nothing marks a Question
        // seen, and no isSeen state exists to mark.
        runPractice(
            practiceOnA1(
                levels = setOf(QuestionLevel.APPLIED),
                source = PracticeQuestionSource.UNSEEN,
            ),
            correctFor = setOf(AppliedQuestion),
        )

        assertEquals(2, coverageOfA1().attemptedQuestionCount)
        assertEquals(
            listOf(AdvancedQuestion),
            selectedIds(practiceOnA1(source = PracticeQuestionSource.UNSEEN)),
        )
    }

    @Test
    fun coverageAndUnseenEligibilityStayTwoViewsOfOneHistory() = runPracticeTest {
        runPractice(
            practiceOnA1(levels = setOf(QuestionLevel.FOUNDATION)),
            correctFor = setOf(FoundationQuestion),
        )
        runPractice(
            practiceOnA1(levels = setOf(QuestionLevel.APPLIED)),
            correctFor = emptySet(),
        )

        val history = assessmentRepository.getCompletedAttempts()
        val activePool = curriculumRepository.getActiveQuestionsBySubtopic(SubtopicA1).map { it.id }
        val exposed = QuestionExposure.observedQuestionIds(history)
        val covered = activePool.filter { it in exposed }
        val unseen = selectedIds(practiceOnA1(source = PracticeQuestionSource.UNSEEN))

        // Progress counts what is inside the exposure set and unseen practice selects what is
        // outside it. If those two ever stopped being complements the learner would see a coverage
        // percentage that practice contradicts, so assert the partition rather than each side alone.
        assertEquals(activePool.toSet(), (covered + unseen).toSet())
        assertTrue(covered.none { it in unseen })
        assertEquals(covered.size, coverageOfA1().attemptedQuestionCount)
        assertEquals(activePool.size, coverageOfA1().totalQuestionCount)
    }

    @Test
    fun weakPracticeContributesToOrdinaryProgressStatistics() = runPracticeTest {
        // Two answered Questions at 0% makes this Subtopic weak while leaving its parent Topic
        // below the Topic evidence minimum, which is what keeps the healthy-parent case honest.
        runPractice(
            practiceOnA2(levels = setOf(QuestionLevel.FOUNDATION, QuestionLevel.APPLIED)),
            correctFor = emptySet(),
        )
        val before = progressService.load()
        assertEquals(1, before.completedAttemptCount)
        assertEquals(2, before.answeredQuestionCount)
        assertEquals(0, before.correctAnswerCount)

        // Scoped to the whole Topic: only the weak child contributes eligibility, and it contributes
        // its current ACTIVE content rather than only the Questions with history.
        val weakConfig = AssessmentConfig.Focused(
            scope = AssessmentScope.Topic(TopicA),
            questionCount = 10,
            source = PracticeQuestionSource.WEAK_AREAS,
        )
        assertEquals(A2QuestionIds, selectedIds(weakConfig))

        val attemptId = runPractice(weakConfig, correctFor = A2QuestionIds.toSet())

        val after = progressService.load()
        assertEquals(2, after.completedAttemptCount)
        assertEquals(5, after.answeredQuestionCount)
        assertEquals(3, after.correctAnswerCount)
        val subtopic = after.subtopics.single { it.subtopicId == SubtopicA2 }
        assertEquals(5, subtopic.answeredCount)
        assertEquals(3, subtopic.correctCount)
        val topic = after.topics.single { it.topicId == TopicA }
        assertEquals(5, topic.answeredCount)
        assertEquals(3, topic.correctCount)
        assertEquals(
            PracticeQuestionSource.WEAK_AREAS,
            assertIs<AssessmentConfig.Focused>(
                requireNotNull(assessmentRepository.getById(attemptId)).config,
            ).source,
        )
    }

    @Test
    fun aWeakAreaAttemptIsIndistinguishableFromAnyOtherCompletedAttemptToProgress() = runPracticeTest {
        runPractice(
            practiceOnA2(levels = setOf(QuestionLevel.FOUNDATION, QuestionLevel.APPLIED)),
            correctFor = emptySet(),
        )
        runPractice(
            AssessmentConfig.Focused(
                scope = AssessmentScope.Topic(TopicA),
                questionCount = 10,
                source = PracticeQuestionSource.WEAK_AREAS,
            ),
            correctFor = A2QuestionIds.toSet(),
        )

        val history = assessmentRepository.getCompletedAttempts()
        val asOrdinaryPractice = history.map { attempt ->
            when (val config = attempt.config) {
                is AssessmentConfig.Focused ->
                    attempt.copy(config = config.copy(source = PracticeQuestionSource.ALL))
                is AssessmentConfig.Mixed -> attempt
            }
        }

        // Selection is the only thing the source may influence. Rewriting it must therefore be
        // invisible to every statistic Progress derives from the same history.
        assertEquals(progressService.load(history), progressService.load(asOrdinaryPractice))
    }

    @Test
    fun mistakePracticeCorrectAnswerResolvesAndALaterMistakeReopensIt() = runPracticeTest {
        val mistakeConfig = practiceOnA1(source = PracticeQuestionSource.UNRESOLVED_MISTAKES)
        runPractice(
            practiceOnA1(levels = setOf(QuestionLevel.FOUNDATION)),
            correctFor = emptySet(),
        )

        assertEquals(listOf(FoundationQuestion), mistakeReviewService.load().map { it.questionId })
        assertEquals(1, mistakeReviewService.countUnresolved())
        assertEquals(listOf(FoundationQuestion), selectedIds(mistakeConfig))

        runPractice(mistakeConfig, correctFor = setOf(FoundationQuestion))

        // Nothing was resolved by hand. The newest completed occurrence is correct, so the same
        // derivation that feeds Mistake Review also stops offering it for practice.
        assertEquals(emptyList(), mistakeReviewService.load().map { it.questionId })
        assertEquals(0, mistakeReviewService.countUnresolved())
        assertIs<AssessmentSelectionResult.NoContent>(questionSelector.select(mistakeConfig))

        runPractice(
            practiceOnA1(levels = setOf(QuestionLevel.FOUNDATION)),
            correctFor = emptySet(),
        )

        assertEquals(listOf(FoundationQuestion), mistakeReviewService.load().map { it.questionId })
        assertEquals(1, mistakeReviewService.countUnresolved())
        assertEquals(listOf(FoundationQuestion), selectedIds(mistakeConfig))
    }

    @Test
    fun retakingATargetedRunRepeatsItsStoredConfigurationAgainstCurrentHistory() = runPracticeTest {
        val narrowed = practiceOnA1(levels = setOf(QuestionLevel.ADVANCED))
        val sourceId = runPractice(narrowed, correctFor = setOf(AdvancedQuestion))
        val sourceAttempt = requireNotNull(assessmentRepository.getById(sourceId))

        val retake = assertIs<AssessmentRetakeResult.Created>(
            retakeService.createRetake(sourceId),
        ).session.attempt

        assertNotEquals(sourceId, retake.id)
        assertEquals(narrowed, retake.config)
        assertEquals(AssessmentStatus.IN_PROGRESS, retake.status)
        assertNull(retake.score)
        assertEquals(listOf(AdvancedQuestion), retake.questionAttempts.map { it.questionId })
        assertEquals(sourceAttempt, assessmentRepository.getById(sourceId))

        // A retake re-runs the stored configuration rather than replaying stored Questions, so a
        // history-derived run legitimately finds a different pool the second time. Repeating an
        // unseen run that consumed its scope has nothing left to ask, and refusing creates nothing.
        val unseenId = runPractice(
            practiceOnA2(source = PracticeQuestionSource.UNSEEN),
            correctFor = A2QuestionIds.toSet(),
        )
        val attemptsBeforeUnseenRetake = attemptCount()
        assertEquals(
            AssessmentRetakeResult.NoEligibleQuestions,
            retakeService.createRetake(unseenId),
        )
        assertEquals(attemptsBeforeUnseenRetake, attemptCount())
    }

    @Test
    fun aTargetedAttemptReachesTheOrdinaryFocusedResult() = runPracticeTest {
        val attemptId = runPractice(
            practiceOnA1(
                levels = setOf(QuestionLevel.APPLIED, QuestionLevel.ADVANCED),
                source = PracticeQuestionSource.UNSEEN,
            ),
            correctFor = setOf(AdvancedQuestion),
        )

        val state = assertIs<FocusedResultUiState.Content>(
            resultViewModel(attemptId).uiState.await { it !is FocusedResultUiState.Loading },
        )

        val persisted = requireNotNull(assessmentRepository.getById(attemptId))
        assertEquals(attemptId, state.attemptId)
        assertEquals(persisted.score?.totalQuestions, state.totalQuestions)
        assertEquals(persisted.score?.correctAnswers, state.correctAnswers)
        assertEquals(persisted.score?.percentage, state.percentage)
        assertEquals(
            listOf(AppliedQuestion, AdvancedQuestion),
            state.questions.map { assertIs<ReviewQuestionItem.Available>(it).question.questionId },
        )
    }

    @Test
    fun mixedInterviewStaysOutsideTargetedPracticeSemantics() = runPracticeTest {
        val mixed = AssessmentConfig.Mixed(questionCount = 4)
        val selected = selectedQuestions(mixed)

        // Coverage-first across Topics, not a scoped level-aware read: the first round visits every
        // Topic before any Topic repeats, and level metadata does not narrow anything.
        assertEquals(4, selected.size)
        assertEquals(
            listOf(TopicA, TopicB),
            selected.take(2).map { it.topicId },
        )
        assertTrue(selected.any { it.level == QuestionLevel.FOUNDATION })
        assertTrue(selected.any { it.level != QuestionLevel.FOUNDATION })

        val attemptId = runPractice(mixed, correctFor = setOf(selected.first().id))

        val persisted = requireNotNull(assessmentRepository.getById(attemptId))
        assertEquals(mixed, persisted.config)
        val row = requireNotNull(database.assessmentAttemptDao().getTestAttemptById(attemptId))
        assertNull(row.practiceLevels)
        assertNull(row.practiceSource)
        assertNull(row.scopeType)
        assertNull(row.scopeId)

        // A Mixed attempt is ordinary completed history to everything downstream.
        val progress = progressService.load()
        assertEquals(1, progress.completedAttemptCount)
        assertEquals(4, progress.answeredQuestionCount)
        assertEquals(1, progress.correctAnswerCount)
        assertEquals(3, mistakeReviewService.countUnresolved())
        val retake = assertIs<AssessmentRetakeResult.Created>(
            retakeService.createRetake(attemptId),
        ).session.attempt
        assertEquals(mixed, retake.config)
        assertNotEquals(attemptId, retake.id)
    }
}

// region Test graph

/**
 * The production graph over a real database, with only determinism overridden.
 *
 * `AssessmentQuestionSelector` and `AssessmentEngine` are redeclared solely to inject an identity
 * randomizer, predictable attempt IDs, and a monotonic clock. They keep every production
 * collaborator, including the app-scoped [AssessmentHistoryStore] the selector reads history
 * through, so completion invalidation stays part of what these tests exercise.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private fun runPracticeTest(block: suspend PracticeGraph.() -> Unit) = runTest {
    Dispatchers.setMain(Dispatchers.Unconfined)
    val database = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    assertEquals(
        CurriculumImportResult.Imported,
        CurriculumImporter(database, loadCurriculum = { practiceFixture() }).importCurriculum(),
    )

    var attemptSequence = 0
    var clockSequence = 0L
    val app = koinApplication {
        modules(
            curriculumDataModule,
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
                            Instant.fromEpochMilliseconds(FixtureEpochMillis + clockSequence * 1_000)
                        },
                    )
                }
            },
        )
    }

    try {
        PracticeGraph(database, app.koin).block()
    } finally {
        // The history store's refresh is app-scoped and eagerly started, so it outlives every
        // screen by design and would outlive this test too. Cancelling that scope before closing
        // the database is what makes the teardown deterministic: closing underneath an in-flight
        // Room query throws into the global handler and fails whichever test runs next, while
        // leaving both open would hand the rest of the suite a live database and a live refresh
        // per scenario.
        app.koin.get<AppCoroutineScope>().cancel()
        app.close()
        database.close()
    }
}

private class PracticeGraph(
    val database: CurriculumDatabase,
    private val koin: Koin,
) {
    val curriculumRepository: CurriculumRepository get() = koin.get()
    val assessmentRepository: AssessmentRepository get() = koin.get()
    val questionSelector: AssessmentQuestionSelector get() = koin.get()
    val progressService: LearningProgressService get() = koin.get()
    val mistakeReviewService: MistakeReviewService get() = koin.get()
    val retakeService: AssessmentRetakeService get() = koin.get()

    fun builder(scope: AssessmentScope): PracticeBuilderViewModel = koin.get { parametersOf(scope) }

    fun resultViewModel(attemptId: String): FocusedResultViewModel =
        koin.get { parametersOf(attemptId) }

    private fun takingViewModel(config: AssessmentConfig): AssessmentTakingViewModel =
        koin.get { parametersOf(AssessmentTakingLaunch.New(config)) }

    suspend fun attemptCount(): Int = database.assessmentAttemptDao().countTestAttempts()

    suspend fun selectedQuestions(config: AssessmentConfig): List<Question> =
        when (val selection = questionSelector.select(config)) {
            is AssessmentSelectionResult.Selected -> selection.questions
            is AssessmentSelectionResult.NoContent -> emptyList()
        }

    suspend fun selectedIds(config: AssessmentConfig): List<String> =
        selectedQuestions(config).map { it.id }

    suspend fun coverageOfA1(): SubtopicCoverage =
        progressService.load().subtopicCoverage.single { it.subtopicId == SubtopicA1 }

    /** Starts a run the way the practice destination does, and waits for its first question. */
    suspend fun startTaking(config: AssessmentConfig): AssessmentTakingViewModel {
        val viewModel = takingViewModel(config)
        assertIs<AssessmentTakingUiState.Content>(viewModel.awaitQuestion(questionNumber = 1))
        return viewModel
    }

    suspend fun runPractice(
        config: AssessmentConfig,
        correctFor: Set<String>,
    ): String = startTaking(config).answerAllAndComplete(correctFor)
}

// endregion

// region Assessment taking

/**
 * Answers every question and completes, through the ViewModel that owns persistence.
 *
 * Driving the real state holder rather than the engine is the point: the attempt is written on
 * start, updated per answer, and completion is what invalidates the shared history.
 */
private suspend fun AssessmentTakingViewModel.answerAllAndComplete(correctFor: Set<String>): String {
    var questionNumber = 1
    while (true) {
        val state = awaitQuestion(questionNumber)
        if (state is AssessmentTakingUiState.ReadyToComplete) break
        val content = assertIs<AssessmentTakingUiState.Content>(state)
        val questionId = content.question.id
        val answerId = if (questionId in correctFor) {
            correctAnswerId(questionId)
        } else {
            incorrectAnswerId(questionId)
        }
        selectAnswer(answerId)
        submitAnswer()
        questionNumber += 1
    }

    completeAssessment()
    return assertIs<AssessmentTakingUiState.CompletionSucceeded>(
        uiState.await { it is AssessmentTakingUiState.CompletionSucceeded },
    ).attemptId
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

/** Waits for the builder's eligibility read to settle after a control change. */
private suspend fun PracticeBuilderViewModel.settled(): PracticeBuilderUiState =
    uiState.await { it.availability !is PracticeAvailability.Checking }

private suspend fun PracticeBuilderViewModel.start(): AssessmentConfig.Focused {
    startPractice()
    val event = withContext(Dispatchers.Default) {
        withTimeout(AwaitTimeoutMillis) { events.first() }
    }
    return assertIs<PracticeBuilderEvent.StartPractice>(event).config
}

/**
 * Real Room work runs on Room's own executor, so waiting has to leave the test scheduler's virtual
 * time — the same escape `MixedInterviewResultIntegrationTest` uses.
 */
private suspend fun <T> StateFlow<T>.await(predicate: (T) -> Boolean): T =
    withContext(Dispatchers.Default) {
        withTimeout(AwaitTimeoutMillis) { first(predicate) }
    }

// endregion

// region Fixture

private const val AwaitTimeoutMillis = 5_000L
private const val FixtureEpochMillis = 1_700_000_000_000L
private const val FirstAttemptId = "attempt-1"

private const val TopicA = "topic_a"
private const val TopicB = "topic_b"
private const val SubtopicA1 = "subtopic_a1"
private const val SubtopicA2 = "subtopic_a2"
private const val SubtopicB1 = "subtopic_b1"

private const val FoundationQuestion = "q_a1_foundation"
private const val AppliedQuestion = "q_a1_applied"
private const val AdvancedQuestion = "q_a1_advanced"

private val A1QuestionIds = listOf(FoundationQuestion, AppliedQuestion, AdvancedQuestion)
private val A2QuestionIds = listOf("q_a2_foundation", "q_a2_applied", "q_a2_advanced")
private val TopicAQuestionIds = A1QuestionIds + A2QuestionIds

private fun practiceOnA1(
    levels: Set<QuestionLevel> = AllQuestionLevels,
    source: PracticeQuestionSource = PracticeQuestionSource.ALL,
) = AssessmentConfig.Focused(
    scope = AssessmentScope.Subtopic(SubtopicA1),
    questionCount = 10,
    levels = levels,
    source = source,
)

private fun practiceOnA2(
    levels: Set<QuestionLevel> = AllQuestionLevels,
    source: PracticeQuestionSource = PracticeQuestionSource.ALL,
) = AssessmentConfig.Focused(
    scope = AssessmentScope.Subtopic(SubtopicA2),
    questionCount = 10,
    levels = levels,
    source = source,
)

private fun correctAnswerId(questionId: String) = "${questionId}_a"

private fun incorrectAnswerId(questionId: String) = "${questionId}_b"

/**
 * One Question per level in each Subtopic, so a level filter, a scope filter, and a history filter
 * each produce a distinguishable answer without needing a large curriculum.
 */
private fun practiceFixture() = Curriculum(
    topics = listOf(
        Topic(TopicA, "Topic A"),
        Topic(TopicB, "Topic B"),
    ),
    subtopics = listOf(
        Subtopic(SubtopicA1, TopicA, "Subtopic A1"),
        Subtopic(SubtopicA2, TopicA, "Subtopic A2"),
        Subtopic(SubtopicB1, TopicB, "Subtopic B1"),
    ),
    questions = listOf(
        fixtureQuestion(FoundationQuestion, TopicA, SubtopicA1, QuestionLevel.FOUNDATION),
        fixtureQuestion(AppliedQuestion, TopicA, SubtopicA1, QuestionLevel.APPLIED),
        fixtureQuestion(AdvancedQuestion, TopicA, SubtopicA1, QuestionLevel.ADVANCED),
        fixtureQuestion("q_a2_foundation", TopicA, SubtopicA2, QuestionLevel.FOUNDATION),
        fixtureQuestion("q_a2_applied", TopicA, SubtopicA2, QuestionLevel.APPLIED),
        fixtureQuestion("q_a2_advanced", TopicA, SubtopicA2, QuestionLevel.ADVANCED),
        fixtureQuestion("q_b1_foundation", TopicB, SubtopicB1, QuestionLevel.FOUNDATION),
        fixtureQuestion("q_b1_applied", TopicB, SubtopicB1, QuestionLevel.APPLIED),
    ),
)

private fun fixtureQuestion(
    id: String,
    topicId: String,
    subtopicId: String,
    level: QuestionLevel,
) = Question(
    id = id,
    topicId = topicId,
    subtopicId = subtopicId,
    text = "Question $id",
    answers = listOf(
        AnswerOption(correctAnswerId(id), "Correct"),
        AnswerOption(incorrectAnswerId(id), "Incorrect"),
    ),
    selectionMode = AnswerSelectionMode.SINGLE,
    level = level,
    correctAnswerIds = listOf(correctAnswerId(id)),
    explanation = "Explanation for $id",
    sources = listOf(SourceReference("Source $id", "https://example.com/$id")),
    status = ContentStatus.ACTIVE,
)

// endregion
