package org.artkachenko.kmp_learning_app.assessment.history

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository

/**
 * The suspending read the store exposes to question selection, which is the one consumer that
 * cannot express "not loaded yet" or "failed" as a state of its own.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class AssessmentHistoryStoreTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * The distinction the whole read exists for: history that has not loaded is not history that
     * is empty. Answering early would report every Question as unseen, most likely right after
     * launch, and let a practice run start on that answer.
     */
    @Test
    fun completedAttemptsWaitsForTheFirstReadRatherThanAnsweringWithNoHistory() = runStoreTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeAssessmentRepository(listOf(completedAttempt("question_a")))
        repository.beforeRead = { gate.await() }
        val store = testHistoryStore(repository, testCacheScope())

        val pending = async { store.completedAttempts() }
        advanceUntilIdle()
        assertTrue(pending.isActive, "An unread history must not answer as an empty one.")

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("question_a"), pending.await().questionIds())
    }

    @Test
    fun completedAttemptsFailsWhenHistoryCannotBeRead() = runStoreTest {
        val repository = FakeAssessmentRepository(emptyList())
        repository.failure = IllegalStateException("Database unavailable")
        val store = testHistoryStore(repository, testCacheScope())

        assertFailsWith<AssessmentHistoryUnavailableException> { store.completedAttempts() }
    }

    @Test
    fun completedAttemptsRetriesAfterTheCurrentGenerationFailed() = runStoreTest {
        val repository = FakeAssessmentRepository(listOf(completedAttempt("question_a")))
        repository.failure = IllegalStateException("Database unavailable")
        val store = testHistoryStore(repository, testCacheScope())
        assertFailsWith<AssessmentHistoryUnavailableException> { store.completedAttempts() }

        repository.failure = null

        assertEquals(listOf("question_a"), store.completedAttempts().questionIds())
        assertEquals(2, repository.reads)
    }

    /** Served from the same cache the screens read, so a repeated preflight costs no query. */
    @Test
    fun repeatedReadsAreAnsweredFromTheCachedHistory() = runStoreTest {
        val repository = FakeAssessmentRepository(listOf(completedAttempt("question_a")))
        val store = testHistoryStore(repository, testCacheScope())

        store.completedAttempts()
        store.completedAttempts()
        advanceUntilIdle()

        assertEquals(1, repository.reads)
    }

    /** Selection waits for the invalidated generation rather than consuming the prior cache. */
    @Test
    fun invalidationMakesTheNextReadSeeNewlyCompletedAttempts() = runStoreTest {
        val repository = FakeAssessmentRepository(listOf(completedAttempt("question_a")))
        val store = testHistoryStore(repository, testCacheScope())
        assertEquals(listOf("question_a"), store.completedAttempts().questionIds())

        val gate = CompletableDeferred<Unit>()
        repository.attempts = listOf(completedAttempt("question_a"), completedAttempt("question_b"))
        repository.beforeRead = { gate.await() }
        store.invalidate()

        val pending = async { store.completedAttempts() }
        advanceUntilIdle()

        assertTrue(pending.isActive, "Invalidated history must wait for its refreshed generation.")
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("question_a", "question_b"), pending.await().questionIds())
    }

    @Test
    fun failedInvalidationIsNotReturnedAsStaleHistoryToSelection() = runStoreTest {
        val repository = FakeAssessmentRepository(listOf(completedAttempt("question_a")))
        val store = testHistoryStore(repository, testCacheScope())
        assertEquals(listOf("question_a"), store.completedAttempts().questionIds())

        repository.failure = IllegalStateException("Database unavailable")
        store.invalidate()

        assertFailsWith<AssessmentHistoryUnavailableException> { store.completedAttempts() }
        advanceUntilIdle()

        val displayed = assertIs<AssessmentHistory.Loaded>(store.history.value)
        assertEquals(listOf("question_a"), displayed.attempts.questionIds())
    }

    private fun runStoreTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }

    private fun List<TestAttempt>.questionIds(): List<String> =
        flatMap { attempt -> attempt.questionAttempts.map { it.questionId } }

    private fun completedAttempt(questionId: String): TestAttempt =
        TestAttempt(
            id = "attempt_$questionId",
            config = AssessmentConfig.Mixed(questionCount = 1),
            questionAttempts = listOf(
                QuestionAttempt(
                    questionId = questionId,
                    answerState = QuestionAnswerState.Answered(
                        selectedAnswerIds = setOf("${questionId}_answer_a"),
                        isCorrect = true,
                    ),
                ),
            ),
            status = AssessmentStatus.COMPLETED,
            startedAt = Instant.fromEpochSeconds(0),
            completedAt = Instant.fromEpochSeconds(60),
            score = AssessmentScore(totalQuestions = 1, correctAnswers = 1),
        )

    private class FakeAssessmentRepository(
        var attempts: List<TestAttempt>,
    ) : AssessmentRepository {
        var reads = 0
        var failure: Throwable? = null
        var beforeRead: suspend () -> Unit = {}

        override suspend fun save(attempt: TestAttempt) =
            error("Not used by AssessmentHistoryStore.")

        override suspend fun getById(attemptId: String): TestAttempt? =
            error("Not used by AssessmentHistoryStore.")

        override suspend fun getCompletedAttempts(): List<TestAttempt> {
            beforeRead()
            reads++
            failure?.let { throw it }
            return attempts
        }
    }
}
