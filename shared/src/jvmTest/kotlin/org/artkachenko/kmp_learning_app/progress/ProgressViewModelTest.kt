package org.artkachenko.kmp_learning_app.progress

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService

@OptIn(ExperimentalCoroutinesApi::class)
internal class ProgressViewModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun aDerivationMovesLoadingToEmptyOnASingleHistoryRead() = runTest {
        setMain(testScheduler)
        val context = TestContext()

        assertIs<ProgressUiState.Loading>(context.viewModel.uiState.value)
        assertEquals(0, context.assessment.getCompletedCalls)

        context.viewModel.refresh()
        advanceUntilIdle()

        assertIs<ProgressUiState.Empty>(context.viewModel.uiState.value)
        // One derivation is now one read: the snapshot and the history rows share it, where the
        // dashboard used to read the history once for each.
        assertEquals(1, context.assessment.getCompletedCalls)
    }

    @Test
    fun populatedContentUsesSnapshotTotalsAndPreservesHistoryOrder() = runTest {
        setMain(testScheduler)
        val observations = listOf(
            observations("new", 10, 7),
            observations("mid", 10, 7),
            observations("old", 10, 7),
        )
        val attempts = listOf(
            completedAttempt("new", AssessmentConfig.Mixed(10), observations[0]),
            completedAttempt("mid", AssessmentConfig.Mixed(10), observations[1]),
            completedAttempt("old", AssessmentConfig.Mixed(10), observations[2]),
        )
        val context = TestContext(
            attempts = attempts,
            questions = observations.flatten().map { question(it.first, "topic", "subtopic") },
            topics = listOf(Topic("topic", "Kotlin")),
            subtopics = listOf(Subtopic("subtopic", "topic", "Core")),
        )

        context.viewModel.refresh()
        advanceUntilIdle()

        val content = content(context.viewModel)
        assertEquals(3, content.completedAttemptCount)
        assertEquals(30, content.answeredQuestionCount)
        assertEquals(21, content.correctAnswerCount)
        assertEquals(70.0, content.percentage)
        assertEquals(listOf("new", "mid", "old"), content.history.map { it.attemptId })
        assertEquals(CompletedAssessmentType.MIXED, content.history.first().assessmentType)
        assertEquals(7, content.history.first().correctAnswers)
        assertEquals("2026-08-29T00:15:00Z", content.history.first().completedAtText)
    }

    @Test
    fun partialCoverageCarriesTheAttemptedCountTheActiveTotalAndThePercentage() = runTest {
        setMain(testScheduler)
        // 25 of the 100 ACTIVE questions have been seen, and a second attempt re-answers one of
        // them: coverage counts a stable Question ID once however often it was answered.
        val seen = List(25) { "q_$it" to (it % 2 == 0) }
        val context = TestContext(
            attempts = listOf(
                attemptAt("repeat", "2026-08-29T01:00:00Z", listOf(seen.first())),
                attemptAt("first", "2026-08-29T00:00:00Z", seen),
            ),
            questions = List(100) { question("q_$it", "topic", "subtopic") },
            topics = listOf(Topic("topic", "Kotlin")),
            subtopics = listOf(Subtopic("subtopic", "topic", "Core")),
        )

        context.viewModel.refresh()
        advanceUntilIdle()

        assertEquals(
            ProgressCoverageUiModel(
                attemptedQuestionCount = 25,
                totalQuestionCount = 100,
                percentage = 25.0,
            ),
            content(context.viewModel).coverage,
        )
    }

    @Test
    fun broadCoverageIsMappedWithoutRoundingAwayTheCounts() = runTest {
        setMain(testScheduler)
        val seen = List(90) { "q_$it" to true }
        val context = TestContext(
            attempts = listOf(attemptAt("attempt", "2026-08-29T00:00:00Z", seen)),
            questions = List(100) { question("q_$it", "topic", "subtopic") },
            topics = listOf(Topic("topic", "Kotlin")),
            subtopics = listOf(Subtopic("subtopic", "topic", "Core")),
        )

        context.viewModel.refresh()
        advanceUntilIdle()

        assertEquals(
            ProgressCoverageUiModel(90, 100, 90.0),
            content(context.viewModel).coverage,
        )
    }

    @Test
    fun recentPerformanceIsQuestionWeightedAndNotTheMeanOfItsAttempts() = runTest {
        setMain(testScheduler)
        // 1/1 and 10/20 is 11/21, not the 75% a mean of the two attempt percentages would give.
        val context = TestContext(
            attempts = listOf(
                attemptAt("small", "2026-08-29T10:00:00Z", listOf("s_0" to true)),
                attemptAt("large", "2026-08-29T09:00:00Z", List(20) { "l_$it" to (it < 10) }),
            ),
            questions = (List(20) { question("l_$it", "topic", "subtopic") } +
                question("s_0", "topic", "subtopic")),
            topics = listOf(Topic("topic", "Kotlin")),
            subtopics = listOf(Subtopic("subtopic", "topic", "Core")),
        )

        context.viewModel.refresh()
        advanceUntilIdle()

        val recent = assertNotNull(content(context.viewModel).recentPerformance)
        assertEquals(2, recent.attemptCount)
        assertEquals(21, recent.answeredQuestionCount)
        assertEquals(11, recent.correctAnswerCount)
        assertEquals(11.0 / 21.0 * 100.0, recent.percentage)
    }

    @Test
    fun theRecentWindowKeepsTheLatestFiveAttemptsOldestFirst() = runTest {
        setMain(testScheduler)
        // Six attempts, deliberately supplied newest-first, each with a distinguishable accuracy.
        val correctCounts = mapOf(
            "sixth" to 10, "fifth" to 9, "fourth" to 8, "third" to 7, "second" to 6, "first" to 5,
        )
        val hours = listOf("sixth", "fifth", "fourth", "third", "second", "first")
        val attempts = hours.mapIndexed { index, id ->
            attemptAt(
                id = id,
                completedAt = "2026-08-29T${(15 - index).toString().padStart(2, '0')}:00:00Z",
                answers = List(10) { "${id}_q_$it" to (it < correctCounts.getValue(id)) },
            )
        }
        val context = TestContext(
            attempts = attempts,
            questions = attempts.flatMap(TestAttempt::questionAttempts)
                .map { question(it.questionId, "topic", "subtopic") },
            topics = listOf(Topic("topic", "Kotlin")),
            subtopics = listOf(Subtopic("subtopic", "topic", "Core")),
        )

        context.viewModel.refresh()
        advanceUntilIdle()

        val content = content(context.viewModel)
        val trend = assertIs<ProgressRecentTrendUiModel.Available>(
            assertNotNull(content.recentPerformance).trend,
        )
        assertEquals(
            listOf("second", "third", "fourth", "fifth", "sixth"),
            trend.attempts.map { it.attemptId },
            "the series must reach presentation oldest -> newest",
        )
        assertEquals(listOf(60.0, 70.0, 80.0, 90.0, 100.0), trend.attempts.map { it.percentage })
        // The recent window is the latest five; the lifetime figures still count all six.
        assertEquals(6, content.completedAttemptCount)
        assertEquals(60, content.answeredQuestionCount)
        assertEquals(45, content.correctAnswerCount)
        assertEquals(6, content.history.size)
    }

    @Test
    fun aShortHistoryKeepsItsRecentSummaryAndReportsTheTrendAsUnavailable() = runTest {
        setMain(testScheduler)
        for (attemptCount in 1..2) {
            val attempts = List(attemptCount) { index ->
                attemptAt(
                    id = "attempt-$index",
                    completedAt = "2026-08-29T0$index:00:00Z",
                    answers = listOf("attempt-${index}_q" to true),
                )
            }
            val context = TestContext(
                attempts = attempts,
                questions = attempts.map { question("${it.id}_q", "topic", "subtopic") },
                topics = listOf(Topic("topic", "Kotlin")),
                subtopics = listOf(Subtopic("subtopic", "topic", "Core")),
            )

            context.viewModel.refresh()
            advanceUntilIdle()

            val recent = assertNotNull(content(context.viewModel).recentPerformance)
            assertEquals(attemptCount, recent.attemptCount)
            assertEquals(100.0, recent.percentage, "the summary is real evidence and must survive")
            assertEquals(
                ProgressRecentTrendUiModel.InsufficientHistory(requiredAttemptCount = 3),
                recent.trend,
            )
        }
    }

    @Test
    fun aThirdAttemptMakesTheTrendAvailable() = runTest {
        setMain(testScheduler)
        val attempts = List(3) { index ->
            attemptAt(
                id = "attempt-$index",
                completedAt = "2026-08-29T0$index:00:00Z",
                answers = listOf("attempt-${index}_q" to true),
            )
        }
        val context = TestContext(
            attempts = attempts,
            questions = attempts.map { question("${it.id}_q", "topic", "subtopic") },
            topics = listOf(Topic("topic", "Kotlin")),
            subtopics = listOf(Subtopic("subtopic", "topic", "Core")),
        )

        context.viewModel.refresh()
        advanceUntilIdle()

        val trend = assertIs<ProgressRecentTrendUiModel.Available>(
            assertNotNull(content(context.viewModel).recentPerformance).trend,
        )
        assertEquals(3, trend.attempts.size)
    }

    @Test
    fun coverageAndRecentPerformanceAddNoReadOfTheirOwn() = runTest {
        setMain(testScheduler)
        val answers = List(4) { "q_$it" to (it < 3) }
        val context = TestContext(
            attempts = listOf(completedAttempt("attempt", AssessmentConfig.Mixed(4), answers)),
            questions = answers.map { question(it.first, "topic", "subtopic") },
            topics = listOf(Topic("topic", "Kotlin")),
            subtopics = listOf(Subtopic("subtopic", "topic", "Core")),
        )

        advanceUntilIdle()

        val content = content(context.viewModel)
        // Both summaries come off the snapshot the derivation already produced, so the dashboard
        // still reads the history once and the ACTIVE bank once per derivation.
        assertEquals(ProgressCoverageUiModel(4, 4, 100.0), content.coverage)
        assertNotNull(content.recentPerformance)
        assertEquals(1, context.assessment.getCompletedCalls)
        assertEquals(1, context.curriculum.activeQuestionCalls)
    }

    @Test
    fun unresolvedMistakeCountComesFromTheMistakeQueuesLatestOccurrenceRule() = runTest {
        setMain(testScheduler)
        // q1 was wrong then right, so it is resolved; q2 is still wrong. Only q2 should count.
        val context = TestContext(
            attempts = listOf(
                completedAttempt("new", AssessmentConfig.Mixed(2), listOf("q1" to true, "q2" to false)),
                completedAttempt("old", AssessmentConfig.Mixed(1), listOf("q1" to false)),
            ),
            questions = listOf(
                question("q1", "topic", "subtopic"),
                question("q2", "topic", "subtopic"),
            ),
            topics = listOf(Topic("topic", "Kotlin")),
            subtopics = listOf(Subtopic("subtopic", "topic", "Core")),
        )

        context.viewModel.refresh()
        advanceUntilIdle()

        assertEquals(1, content(context.viewModel).unresolvedMistakeCount)
    }

    @Test
    fun weakAreasAndTopicPerformanceMapWithoutReorderingOrReclassification() = runTest {
        setMain(testScheduler)
        val answers = listOf("q1" to true, "q2" to false, "q3" to false)
        val context = TestContext(
            attempts = listOf(completedAttempt("attempt", AssessmentConfig.Mixed(3), answers)),
            questions = answers.map { question(it.first, "topic", "subtopic") },
            topics = listOf(Topic("topic", "Android platform")),
            subtopics = listOf(Subtopic("subtopic", "topic", "State and lifecycle")),
        )

        context.viewModel.refresh()
        advanceUntilIdle()

        val content = content(context.viewModel)
        assertEquals(listOf(WeakAreaType.SUBTOPIC, WeakAreaType.TOPIC), content.weakAreas.map { it.type })
        assertEquals("State and lifecycle", content.weakAreas[0].title)
        assertEquals("Android platform", content.weakAreas[0].subtitle)
        assertEquals(3, content.weakAreas[0].answeredCount)
        assertEquals(1, content.weakAreas[0].correctCount)
        assertEquals(1.0 / 3.0 * 100.0, content.weakAreas[0].percentage)
        assertEquals(
            ProgressTopicUiModel("topic", "Android platform", 3, 1, 1.0 / 3.0 * 100.0),
            content.topics.single(),
        )
    }

    @Test
    fun historyResolvesDeprecatedAndMissingFocusedScopesWithoutDroppingRows() = runTest {
        setMain(testScheduler)
        val attempts = listOf(
            historyAttempt("mixed", AssessmentConfig.Mixed(1)),
            historyAttempt("topic", AssessmentConfig.Focused(AssessmentScope.Topic("old-topic"), 1)),
            historyAttempt("subtopic", AssessmentConfig.Focused(AssessmentScope.Subtopic("old-sub"), 1)),
            historyAttempt("missing-topic", AssessmentConfig.Focused(AssessmentScope.Topic("missing"), 1)),
            historyAttempt("missing-sub", AssessmentConfig.Focused(AssessmentScope.Subtopic("missing"), 1)),
            historyAttempt("missing-parent", AssessmentConfig.Focused(AssessmentScope.Subtopic("orphan"), 1)),
        )
        val context = TestContext(
            attempts = attempts,
            questions = attempts.map { question("${it.id}-q", "stats", "stats-sub") },
            topics = listOf(
                Topic("stats", "Stats"),
                Topic("old-topic", "Old Kotlin", ContentStatus.DEPRECATED),
            ),
            subtopics = listOf(
                Subtopic("stats-sub", "stats", "Stats subtopic"),
                Subtopic("old-sub", "old-topic", "Old coroutines", ContentStatus.DEPRECATED),
                Subtopic("orphan", "missing-parent-topic", "Orphan subtopic"),
            ),
        )

        context.viewModel.refresh()
        advanceUntilIdle()

        val history = content(context.viewModel).history
        assertEquals(6, history.size)
        assertEquals(CompletedAssessmentType.MIXED, history[0].assessmentType)
        assertEquals(FocusedScopeUiModel.Topic("Old Kotlin"), history[1].focusedScope)
        assertEquals(
            FocusedScopeUiModel.Subtopic("Old Kotlin", "Old coroutines"),
            history[2].focusedScope,
        )
        assertEquals(FocusedScopeUiModel.Topic(null), history[3].focusedScope)
        assertEquals(FocusedScopeUiModel.Subtopic(null, null), history[4].focusedScope)
        assertEquals(
            FocusedScopeUiModel.Subtopic(null, "Orphan subtopic"),
            history[5].focusedScope,
        )
    }

    @Test
    fun historyMetadataLookupsAreCachedWithinRefresh() = runTest {
        setMain(testScheduler)
        val attempts = listOf(
            historyAttempt("topic-1", AssessmentConfig.Focused(AssessmentScope.Topic("topic"), 1)),
            historyAttempt("topic-2", AssessmentConfig.Focused(AssessmentScope.Topic("topic"), 1)),
            historyAttempt("sub-1", AssessmentConfig.Focused(AssessmentScope.Subtopic("sub"), 1)),
            historyAttempt("sub-2", AssessmentConfig.Focused(AssessmentScope.Subtopic("sub"), 1)),
        )
        val context = TestContext(
            attempts = attempts,
            questions = attempts.map { question("${it.id}-q", "topic", "sub") },
            topics = listOf(Topic("topic", "Topic")),
            subtopics = listOf(Subtopic("sub", "topic", "Subtopic")),
        )

        // The cache only re-derives when the history actually changes, so the second read has to
        // return something different from the first.
        context.assessment.onGetCompleted = { call ->
            if (call == 1) attempts.take(1) else attempts
        }

        advanceUntilIdle()
        // Clearing the counters after the first derivation leaves only the lookups the history
        // mapping's own per-derivation cache controls.
        context.curriculum.resetLookupCounts()
        context.viewModel.refresh()
        advanceUntilIdle()

        // Four attempts share one topic and one subtopic. Without per-derivation caching each
        // row would look them up again; two lookups is one from the progress snapshot and one
        // from the history mapping, not one per row.
        assertEquals(2, context.curriculum.topicLookupCalls.getValue("topic"))
        assertEquals(2, context.curriculum.subtopicLookupCalls.getValue("sub"))
    }

    @Test
    fun overallStatisticsComeFromTheSnapshotRatherThanTheHistoryRows() = runTest {
        setMain(testScheduler)
        val snapshotAttempts = listOf(
            completedAttempt("snapshot", AssessmentConfig.Mixed(10), observations("snap", 10, 7)),
        )
        val historyAttempts = listOf(
            completedAttempt("history-a", AssessmentConfig.Mixed(2), observations("hist_a", 2, 0)),
            completedAttempt("history-b", AssessmentConfig.Mixed(2), observations("hist_b", 2, 0)),
        )
        val context = TestContext(
            questions = (snapshotAttempts + historyAttempts)
                .flatMap(TestAttempt::questionAttempts)
                .map { question(it.questionId, "topic", "subtopic") },
            topics = listOf(Topic("topic", "Kotlin")),
            subtopics = listOf(Subtopic("subtopic", "topic", "Core")),
        )
        // The snapshot and the rows are now derived from one shared read rather than the
        // dashboard reading the history a second time, so they can no longer disagree - which is
        // the point. What is worth pinning is that the read happens once per derivation.
        context.assessment.onGetCompleted = { snapshotAttempts + historyAttempts }

        advanceUntilIdle()
        val readsAfterFirstDerivation = context.assessment.getCompletedCalls

        val content = content(context.viewModel)
        assertEquals(3, content.completedAttemptCount)
        assertEquals(listOf("snapshot", "history-a", "history-b"), content.history.map { it.attemptId })
        assertEquals(
            1,
            readsAfterFirstDerivation,
            "the dashboard must derive its totals and its rows from a single history read",
        )
    }

    @Test
    fun historySkipsAttemptsThatAreNotCompleted() = runTest {
        setMain(testScheduler)
        val completed = historyAttempt("completed", AssessmentConfig.Mixed(1))
        val inProgress = TestAttempt(
            id = "in-progress",
            config = AssessmentConfig.Mixed(1),
            questionAttempts = listOf(QuestionAttempt("in-progress-q", QuestionAnswerState.Unanswered)),
            status = AssessmentStatus.IN_PROGRESS,
            startedAt = Instant.parse("2026-08-29T00:00:00Z"),
        )
        val context = TestContext(
            attempts = listOf(inProgress, completed),
            questions = listOf(question("completed-q", "topic", "sub")),
            topics = listOf(Topic("topic", "Kotlin")),
            subtopics = listOf(Subtopic("sub", "topic", "Core")),
        )

        context.viewModel.refresh()
        advanceUntilIdle()

        // A contract violation must not take the whole dashboard down, and history must stay
        // consistent with the snapshot's completed attempt count.
        val content = content(context.viewModel)
        assertEquals(1, content.completedAttemptCount)
        assertEquals(listOf("completed"), content.history.map { it.attemptId })
    }

    @Test
    fun retryAfterFailureAndLaterRefreshReplacePreviousState() = runTest {
        setMain(testScheduler)
        val first = historyAttempt("first", AssessmentConfig.Mixed(1))
        val second = historyAttempt("second", AssessmentConfig.Mixed(1))
        val context = TestContext(
            attempts = listOf(first),
            questions = listOf(
                question("first-q", "topic", "sub"),
                question("second-q", "topic", "sub"),
            ),
        )
        context.assessment.failNextLoad = true

        context.viewModel.refresh()
        advanceUntilIdle()
        assertIs<ProgressUiState.Error>(context.viewModel.uiState.value)

        context.viewModel.refresh()
        advanceUntilIdle()
        assertEquals(listOf("first"), content(context.viewModel).history.map { it.attemptId })

        context.assessment.attempts = listOf(second, first)
        context.viewModel.refresh()
        advanceUntilIdle()
        assertEquals(listOf("second", "first"), content(context.viewModel).history.map { it.attemptId })
    }

    @Test
    fun aStaleReadCompletingLateDoesNotOverwriteTheNewerResult() = runTest {
        setMain(testScheduler)
        val stale = historyAttempt("stale", AssessmentConfig.Mixed(1))
        val fresh = historyAttempt("fresh", AssessmentConfig.Mixed(1))
        val firstLoadGate = CompletableDeferred<Unit>()
        val context = TestContext(
            attempts = listOf(fresh),
            questions = listOf(
                question("stale-q", "topic", "sub"),
                question("fresh-q", "topic", "sub"),
            ),
        )
        context.assessment.onGetCompleted = { call ->
            if (call == 1) {
                firstLoadGate.await()
                listOf(stale)
            } else {
                listOf(fresh)
            }
        }

        runCurrent()
        context.viewModel.refresh()
        firstLoadGate.complete(Unit)
        advanceUntilIdle()

        // A stale read completing after a newer one must not be what the screen is left showing.
        assertEquals(listOf("fresh"), content(context.viewModel).history.map { it.attemptId })
    }

    private fun setMain(testScheduler: kotlinx.coroutines.test.TestCoroutineScheduler) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    }
}

private class TestContext(
    attempts: List<TestAttempt> = emptyList(),
    questions: List<Question> = emptyList(),
    topics: List<Topic> = emptyList(),
    subtopics: List<Subtopic> = emptyList(),
) {
    val assessment = FakeAssessmentRepository(attempts)
    val curriculum = FakeCurriculumRepository(questions, topics, subtopics)
    val viewModel = run {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val store = AssessmentHistoryStore(assessment, scope)
        ProgressViewModel(
            historyStore = store,
            stateHolder = ProgressStateHolder(
                learningProgressService = LearningProgressService(assessment, curriculum),
                curriculumRepository = curriculum,
                mistakeReviewService = MistakeReviewService(assessment, AssessmentReviewLoader(curriculum)),
                historyStore = store,
                scope = scope,
            ),
        )
    }
}

private class FakeAssessmentRepository(
    var attempts: List<TestAttempt>,
) : AssessmentRepository {
    var getCompletedCalls = 0
    var failNextLoad = false
    var onGetCompleted: (suspend (Int) -> List<TestAttempt>)? = null

    override suspend fun save(attempt: TestAttempt) = Unit

    override suspend fun getById(attemptId: String): TestAttempt? =
        attempts.firstOrNull { it.id == attemptId }

    override suspend fun getCompletedAttempts(): List<TestAttempt> {
        getCompletedCalls += 1
        if (failNextLoad) {
            failNextLoad = false
            error("History unavailable")
        }
        return onGetCompleted?.invoke(getCompletedCalls) ?: attempts
    }
}

private class FakeCurriculumRepository(
    private val questions: List<Question>,
    topics: List<Topic>,
    subtopics: List<Subtopic>,
) : CurriculumRepository {
    private val questionsById = questions.associateBy(Question::id)
    private val topicsById = topics.associateBy(Topic::id)
    private val subtopicsById = subtopics.associateBy(Subtopic::id)
    val topicLookupCalls = mutableMapOf<String, Int>()
    val subtopicLookupCalls = mutableMapOf<String, Int>()
    var activeQuestionCalls = 0

    fun resetLookupCounts() {
        topicLookupCalls.clear()
        subtopicLookupCalls.clear()
    }

    override suspend fun getActiveTopics(): List<Topic> = error("ACTIVE lookup must not be used.")
    override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> =
        error("ACTIVE lookup must not be used.")

    /** LearningProgressService reads the ACTIVE bank once per load to derive curriculum coverage. */
    override suspend fun getActiveQuestions(): List<Question> {
        activeQuestionCalls += 1
        return questions
    }

    override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
        error("ACTIVE lookup must not be used.")

    override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> =
        error("Level-filtered lookup must not be used.")

    override suspend fun getActiveQuestionsByTopicAndLevels(
        topicId: String,
        levels: Set<QuestionLevel>,
    ): List<Question> = error("Level-filtered lookup must not be used.")

    override suspend fun getActiveQuestionsBySubtopicAndLevels(
        subtopicId: String,
        levels: Set<QuestionLevel>,
    ): List<Question> = error("Level-filtered lookup must not be used.")

    override suspend fun getTopicById(topicId: String): Topic? {
        topicLookupCalls[topicId] = topicLookupCalls.getOrElse(topicId) { 0 } + 1
        return topicsById[topicId]
    }

    override suspend fun getSubtopicById(subtopicId: String): Subtopic? {
        subtopicLookupCalls[subtopicId] = subtopicLookupCalls.getOrElse(subtopicId) { 0 } + 1
        return subtopicsById[subtopicId]
    }

    override suspend fun getQuestionById(questionId: String): Question? = questionsById[questionId]
}

private fun completedAttempt(
    id: String,
    config: AssessmentConfig,
    answers: List<Pair<String, Boolean>>,
): TestAttempt =
    TestAttempt(
        id = id,
        config = config,
        questionAttempts = answers.map { (questionId, isCorrect) ->
            QuestionAttempt(
                questionId,
                QuestionAnswerState.Answered(setOf("${questionId}_answer"), isCorrect),
            )
        },
        status = AssessmentStatus.COMPLETED,
        startedAt = Instant.parse("2026-08-29T00:00:00Z"),
        completedAt = Instant.parse("2026-08-29T00:15:00Z"),
        score = AssessmentScore(answers.size, answers.count { it.second }),
    )

private fun historyAttempt(
    id: String,
    config: AssessmentConfig,
): TestAttempt = completedAttempt(id, config, listOf("${id}-q" to true))

/**
 * A completed attempt with an explicit completion time, for the tests that care which attempts fall
 * inside the recent window and in what order they reach presentation.
 */
private fun attemptAt(
    id: String,
    completedAt: String,
    answers: List<Pair<String, Boolean>>,
): TestAttempt =
    TestAttempt(
        id = id,
        config = AssessmentConfig.Mixed(answers.size),
        questionAttempts = answers.map { (questionId, isCorrect) ->
            QuestionAttempt(
                questionId,
                QuestionAnswerState.Answered(setOf("${questionId}_answer"), isCorrect),
            )
        },
        status = AssessmentStatus.COMPLETED,
        startedAt = Instant.parse(completedAt),
        completedAt = Instant.parse(completedAt),
        score = AssessmentScore(answers.size, answers.count { it.second }),
    )

private fun observations(
    prefix: String,
    count: Int,
    correctCount: Int,
): List<Pair<String, Boolean>> =
    List(count) { index -> "${prefix}_q_$index" to (index < correctCount) }

private fun question(
    id: String,
    topicId: String,
    subtopicId: String,
): Question =
    Question(
        id = id,
        topicId = topicId,
        subtopicId = subtopicId,
        text = "Question $id",
        answers = listOf(
            AnswerOption("${id}_a", "Answer A"),
            AnswerOption("${id}_b", "Answer B"),
        ),
        selectionMode = AnswerSelectionMode.SINGLE,
        level = QuestionLevel.FOUNDATION,
        correctAnswerIds = listOf("${id}_a"),
        explanation = "Explanation",
        sources = listOf(SourceReference("Source", "https://example.com/$id")),
    )

private fun content(viewModel: ProgressViewModel): ProgressUiState.Content =
    assertIs<ProgressUiState.Content>(viewModel.uiState.value)
