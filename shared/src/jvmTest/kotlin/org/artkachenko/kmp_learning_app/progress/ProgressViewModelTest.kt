package org.artkachenko.kmp_learning_app.progress

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
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
    fun refreshMovesLoadingToEmptyWithoutAutomaticInitLoad() = runTest {
        setMain(testScheduler)
        val context = TestContext()

        assertIs<ProgressUiState.Loading>(context.viewModel.uiState.value)
        assertEquals(0, context.assessment.getCompletedCalls)

        context.viewModel.refresh()
        advanceUntilIdle()

        assertIs<ProgressUiState.Empty>(context.viewModel.uiState.value)
        assertEquals(2, context.assessment.getCompletedCalls)
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

        // LearningProgressService resolves curriculum metadata during its own load, which is
        // not what this test is about. The ViewModel reads the history second, so clearing the
        // counters there leaves only the lookups the ViewModel's per-refresh cache controls.
        context.assessment.onGetCompleted = { call ->
            if (call == 2) context.curriculum.resetLookupCounts()
            attempts
        }

        context.viewModel.refresh()
        advanceUntilIdle()

        assertEquals(1, context.curriculum.topicLookupCalls.getValue("topic"))
        assertEquals(1, context.curriculum.subtopicLookupCalls.getValue("sub"))
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
        // Deliberately let the snapshot and the history rows disagree: the service reads the
        // history first, the ViewModel reads it again for the rows. Summing the rows would
        // report 2 attempts, 4 answered, 0 correct, 0% instead of the snapshot's values.
        context.assessment.onGetCompleted = { call ->
            if (call == 1) snapshotAttempts else historyAttempts
        }

        context.viewModel.refresh()
        advanceUntilIdle()

        val content = content(context.viewModel)
        assertEquals(1, content.completedAttemptCount)
        assertEquals(10, content.answeredQuestionCount)
        assertEquals(7, content.correctAnswerCount)
        assertEquals(70.0, content.percentage)
        assertEquals(listOf("history-a", "history-b"), content.history.map { it.attemptId })
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
    fun newerRefreshCancelsSuspendedLoadSoStaleResultCannotOverwriteIt() = runTest {
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

        context.viewModel.refresh()
        runCurrent()
        context.viewModel.refresh()
        advanceUntilIdle()
        assertEquals(listOf("fresh"), content(context.viewModel).history.map { it.attemptId })

        firstLoadGate.complete(Unit)
        advanceUntilIdle()
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
    val viewModel = ProgressViewModel(
        learningProgressService = LearningProgressService(assessment, curriculum),
        assessmentRepository = assessment,
        curriculumRepository = curriculum,
        mistakeReviewService = MistakeReviewService(assessment, AssessmentReviewLoader(curriculum)),
    )
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
    questions: List<Question>,
    topics: List<Topic>,
    subtopics: List<Subtopic>,
) : CurriculumRepository {
    private val questionsById = questions.associateBy(Question::id)
    private val topicsById = topics.associateBy(Topic::id)
    private val subtopicsById = subtopics.associateBy(Subtopic::id)
    val topicLookupCalls = mutableMapOf<String, Int>()
    val subtopicLookupCalls = mutableMapOf<String, Int>()

    fun resetLookupCounts() {
        topicLookupCalls.clear()
        subtopicLookupCalls.clear()
    }

    override suspend fun getActiveTopics(): List<Topic> = error("ACTIVE lookup must not be used.")
    override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestions(): List<Question> = error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
        error("ACTIVE lookup must not be used.")

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
        correctAnswerIds = listOf("${id}_a"),
        explanation = "Explanation",
        sources = listOf(SourceReference("Source", "https://example.com/$id")),
    )

private fun content(viewModel: ProgressViewModel): ProgressUiState.Content =
    assertIs<ProgressUiState.Content>(viewModel.uiState.value)
