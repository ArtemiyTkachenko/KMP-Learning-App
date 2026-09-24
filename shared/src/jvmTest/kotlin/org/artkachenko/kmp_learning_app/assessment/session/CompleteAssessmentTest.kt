package org.artkachenko.kmp_learning_app.assessment.session

import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant
import kotlin.test.AfterTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.history.testCacheScope
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

/**
 * Completion as one operation rather than two statements.
 *
 * The durable write and the history invalidation are the same domain event, and the rules below are
 * about the pair. They are stated here, over the real [AssessmentHistoryStore], rather than through
 * `AssessmentTakingViewModel`, because nothing about them is presentational: the taking ViewModel's
 * own suite keeps the state-machine rules — that a double tap persists once, that a failure stays
 * retryable — and this keeps the persistence contract those rules sit on.
 *
 * Invalidation is observed as what it actually is: one more read of the attempt table.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class CompleteAssessmentTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun completingScoresTheAttemptPersistsItAndMarksHistoryStale() = runCompletionTest {
        val repository = FakeAssessmentRepository()
        val store = historyStore(repository)
        val session = answeredSession()
        advanceUntilIdle()
        assertEquals(1, repository.reads, "The cache reads once eagerly before completion.")

        val completed = completeAssessment(repository, store)(session)
        advanceUntilIdle()

        assertEquals(AssessmentStatus.COMPLETED, completed.attempt.status)
        assertEquals(2, requireNotNull(completed.attempt.score).totalQuestions)
        assertEquals(1, requireNotNull(completed.attempt.score).correctAnswers)
        assertEquals(listOf(AssessmentStatus.COMPLETED), repository.saved.map { it.status })
        assertEquals(2, repository.reads, "Completion must mark the shared history stale.")
    }

    /**
     * The regression the operation exists for.
     *
     * A completed attempt is written inside one atomic transaction, but the step *after* that write
     * is reached by resuming a continuation — and a cancelled job throws at the resumption. The
     * learner backing out of the taking destination as the transaction commits is enough: the
     * attempt is `COMPLETED` in the database while the app-scoped cache still holds the list from
     * before it, so Progress, the mistake queue, the Mistakes badge, the interview record and
     * unseen-practice selection all silently omit an assessment that was finished, for the rest of
     * the process.
     *
     * The fake reproduces exactly that shape with no timing assumption: it records the write — the
     * commit — and only then observes the cancellation its own resumption would have observed.
     */
    @Test
    fun aWriteThatCommitsBeforeCancellationStillMarksHistoryStale() = runCompletionTest {
        val repository = FakeAssessmentRepository()
        val store = historyStore(repository)
        advanceUntilIdle()
        val completion = async {
            repository.afterSave = { coroutineContext.ensureActive() }
            repository.onSaved = { requireNotNull(coroutineContext[Job]).cancel() }
            completeAssessment(repository, store)(answeredSession())
        }
        advanceUntilIdle()

        assertEquals(
            listOf(AssessmentStatus.COMPLETED),
            repository.saved.map { it.status },
            "The write is expected to have committed before cancellation was observed.",
        )
        assertEquals(
            2,
            repository.reads,
            "A committed completion must mark the cache stale even when its caller is cancelled.",
        )
        assertFailsWith<CancellationException> { completion.await() }
    }

    /**
     * A write that never committed is invalidated too, and deliberately so: the operation cannot
     * tell a failure before the commit from one after it, and the two costs are not comparable. A
     * needless invalidation costs one re-read that returns the attempts already cached — which
     * [AssessmentHistoryStore.history] documents as a normal emission — while a missing one costs
     * correctness for the rest of the process.
     */
    @Test
    fun aFailedWriteStillMarksHistoryStale() = runCompletionTest {
        val repository = FakeAssessmentRepository()
        repository.saveFailure = IllegalStateException("Database unavailable")
        val store = historyStore(repository)
        advanceUntilIdle()

        assertFailsWith<IllegalStateException> {
            completeAssessment(repository, store)(answeredSession())
        }
        advanceUntilIdle()

        assertEquals(emptyList(), repository.saved)
        assertEquals(2, repository.reads)
    }

    /**
     * Duplicate invocation, which a retry after a failed completion is.
     *
     * The attempt keeps the identity it was started with and the write is a full replace, so running
     * completion twice over the same session rewrites one row rather than recording a second
     * occurrence. That is what keeps a retry from double-counting in every figure derived from
     * history.
     */
    @Test
    fun repeatedCompletionRewritesOneAttemptRatherThanAddingAnOccurrence() = runCompletionTest {
        val repository = FakeAssessmentRepository()
        val store = historyStore(repository)
        val session = answeredSession()
        val complete = completeAssessment(repository, store)
        advanceUntilIdle()

        val first = complete(session)
        val second = complete(session)
        advanceUntilIdle()

        assertEquals(listOf("attempt-1", "attempt-1"), repository.saved.map { it.id })
        assertEquals(first.attempt.score, second.attempt.score)
        assertEquals(first.attempt.questionAttempts, second.attempt.questionAttempts)
    }

    /**
     * A session that cannot be completed creates nothing, so there is nothing to invalidate either:
     * the guard runs before the write, and the cache is untouched.
     */
    @Test
    fun anUnfinishedSessionIsNeitherWrittenNorInvalidated() = runCompletionTest {
        val repository = FakeAssessmentRepository()
        val store = historyStore(repository)
        advanceUntilIdle()
        val readsBefore = repository.reads

        assertFailsWith<IllegalStateException> {
            completeAssessment(repository, store)(partlyAnsweredSession())
        }

        assertEquals(emptyList(), repository.saved)
        assertEquals(readsBefore, repository.reads)
    }

    /**
     * The real cache on the same app-scoped shape the running app gives it, so an invalidation
     * is observed as the read it actually causes. Its first, eager read is settled by the
     * caller before the operation under test runs, so a later count of reads is unambiguous.
     */
    private fun historyStore(repository: AssessmentRepository) =
        AssessmentHistoryStore(repository, testCacheScope())

    private fun completeAssessment(
        repository: AssessmentRepository,
        store: AssessmentHistoryStore,
    ) = CompleteAssessment(
        assessmentEngine = AssessmentEngine(
            questionSelector = AssessmentQuestionSelector(
                curriculumRepository = EmptyCurriculumRepository,
                completedHistory = { emptyList() },
                randomize = { it },
            ),
            generateAttemptId = { error("Completion must not start an assessment.") },
            now = { CompletedAt },
        ),
        assessmentRepository = repository,
        historyStore = store,
    )

    private fun answeredSession() = session(
        QuestionAttempt("q1", QuestionAnswerState.Answered(setOf("q1_a"), isCorrect = true)),
        QuestionAttempt("q2", QuestionAnswerState.Answered(setOf("q2_b"), isCorrect = false)),
    )

    private fun partlyAnsweredSession() = session(
        QuestionAttempt("q1", QuestionAnswerState.Answered(setOf("q1_a"), isCorrect = true)),
        QuestionAttempt("q2"),
    )

    private fun session(vararg questionAttempts: QuestionAttempt) = AssessmentSession(
        attempt = TestAttempt(
            id = "attempt-1",
            config = AssessmentConfig.Focused(AssessmentScope.Topic("topic"), questionCount = 2),
            questionAttempts = questionAttempts.toList(),
            status = AssessmentStatus.IN_PROGRESS,
            startedAt = StartedAt,
        ),
        questions = questionAttempts.map { question(it.questionId) },
    )

    private fun question(id: String) = Question(
        id = id,
        topicId = "topic",
        subtopicId = "subtopic",
        text = "Question $id?",
        answers = listOf(
            AnswerOption("${id}_a", "First answer for $id"),
            AnswerOption("${id}_b", "Second answer for $id"),
        ),
        correctAnswerIds = listOf("${id}_a"),
        selectionMode = AnswerSelectionMode.SINGLE,
        level = QuestionLevel.FOUNDATION,
        explanation = "Why the first answer for $id is the correct one.",
        sources = emptyList(),
    )

    private fun runCompletionTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }

    private companion object {
        val StartedAt: Instant = Instant.fromEpochMilliseconds(1_000)
        val CompletedAt: Instant = Instant.fromEpochMilliseconds(2_000)
    }
}

/**
 * Counts reads so an invalidation is observable as what it is — one more read of the attempt table —
 * and lets a test decide what happens around a write.
 */
private class FakeAssessmentRepository : AssessmentRepository {
    val saved = mutableListOf<TestAttempt>()
    var reads = 0
    var saveFailure: Throwable? = null

    /** Runs after the write is recorded, standing in for the commit having landed. */
    var onSaved: suspend () -> Unit = {}

    /** Runs after [onSaved], standing in for what resuming this write's continuation observes. */
    var afterSave: suspend () -> Unit = {}

    override suspend fun save(attempt: TestAttempt) {
        saveFailure?.let { throw it }
        saved += attempt
        onSaved()
        afterSave()
    }

    override suspend fun getById(attemptId: String): TestAttempt? =
        saved.lastOrNull { it.id == attemptId }

    override suspend fun getCompletedAttempts(): List<TestAttempt> {
        reads += 1
        return saved.filter { it.status == AssessmentStatus.COMPLETED }
    }
}

private object EmptyCurriculumRepository : CurriculumRepository {
    override suspend fun getActiveTopics(): List<Topic> = emptyList()
    override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> = emptyList()
    override suspend fun getActiveQuestions(): List<Question> = emptyList()
    override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> = emptyList()
    override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> = emptyList()
    override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> = emptyList()
    override suspend fun getActiveQuestionsByTopicAndLevels(
        topicId: String,
        levels: Set<QuestionLevel>,
    ): List<Question> = emptyList()
    override suspend fun getActiveQuestionsBySubtopicAndLevels(
        subtopicId: String,
        levels: Set<QuestionLevel>,
    ): List<Question> = emptyList()
    override suspend fun getTopicById(topicId: String): Topic? = null
    override suspend fun getSubtopicById(subtopicId: String): Subtopic? = null
    override suspend fun getQuestionById(questionId: String): Question? = null
}
