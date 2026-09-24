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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
 * The two surfaces of the store: the suspending read question selection uses, which is the one
 * consumer that cannot express "not loaded yet" or "failed" as a state of its own, and the
 * observable history every screen derives from.
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
    fun ownerCancellationDoesNotPublishFailedHistory() = runStoreTest {
        val repository = FakeAssessmentRepository(emptyList())
        repository.beforeRead = { CompletableDeferred<Unit>().await() }
        val owner = testCacheScope()
        val store = testHistoryStore(repository, owner)
        runCurrent()

        owner.cancel()
        advanceUntilIdle()

        assertIs<AssessmentHistory.Loading>(store.history.first())
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

    @Test
    fun concurrentOneShotCallersCoalesceOntoOneRetryGeneration() = runStoreTest {
        val repository = FakeAssessmentRepository(listOf(completedAttempt("question_a")))
        repository.failure = IllegalStateException("Database unavailable")
        val store = testHistoryStore(repository, testCacheScope())
        assertFailsWith<AssessmentHistoryUnavailableException> { store.completedAttempts() }
        repository.failure = null
        val retryGate = CompletableDeferred<Unit>()
        repository.beforeRead = { retryGate.await() }

        val first = async { store.completedAttempts() }
        val second = async { store.completedAttempts() }
        runCurrent()
        assertTrue(first.isActive)
        assertTrue(second.isActive)

        retryGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(first.await(), second.await())
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
    fun invalidationDuringAnActiveReadKeepsStaleObserverDataButBlocksOneShotCallers() = runStoreTest {
        val firstReadGate = CompletableDeferred<Unit>()
        val secondReadGate = CompletableDeferred<Unit>()
        val repository = FakeAssessmentRepository(listOf(completedAttempt("question_a")))
        repository.beforeRead = {
            when (repository.reads) {
                1 -> firstReadGate.await()
                2 -> secondReadGate.await()
            }
        }
        val store = testHistoryStore(repository, testCacheScope())
        runCurrent()

        repository.attempts = listOf(completedAttempt("question_a"), completedAttempt("question_b"))
        store.invalidate()
        val oneShot = async { store.completedAttempts() }
        firstReadGate.complete(Unit)
        runCurrent()

        assertEquals(
            listOf("question_a"),
            assertIs<AssessmentHistory.Loaded>(store.history.first()).attempts.questionIds(),
        )
        assertTrue(oneShot.isActive)

        secondReadGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("question_a", "question_b"), oneShot.await().questionIds())
        assertEquals(
            listOf("question_a", "question_b"),
            assertIs<AssessmentHistory.Loaded>(store.history.first()).attempts.questionIds(),
        )
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

        val displayed = assertIs<AssessmentHistory.Loaded>(store.history.first())
        assertEquals(listOf("question_a"), displayed.attempts.questionIds())
    }

    /**
     * The contract every screen's Retry rests on: one invalidation is one emission, even when the
     * re-read produces attempts equal to the ones already cached.
     *
     * A `StateFlow` would drop that emission, which is why four consumers each kept a private
     * counter to manufacture one. Consumers derive from this value over content that can be
     * unavailable while the attempt table reads perfectly well, so "the history was refreshed" has
     * to reach them whether or not the history changed.
     */
    @Test
    fun invalidationEmitsAgainEvenWhenTheAttemptsAreUnchanged() = runStoreTest {
        val repository = FakeAssessmentRepository(listOf(completedAttempt("question_a")))
        val store = testHistoryStore(repository, testCacheScope())
        val observerScope = testCacheScope()
        val observed = mutableListOf<AssessmentHistory>()
        val observer = observerScope.launch { store.history.toList(observed) }
        advanceUntilIdle()
        assertEquals(
            listOf<AssessmentHistory>(AssessmentHistory.Loaded(repository.attempts)),
            observed.settled(),
        )

        store.invalidate()
        advanceUntilIdle()
        observer.cancel()

        assertEquals(
            List<AssessmentHistory>(2) { AssessmentHistory.Loaded(repository.attempts) },
            observed.settled(),
            "An unchanged re-read must still announce that the history was refreshed.",
        )
        assertEquals(2, repository.reads)
    }

    /**
     * A re-read that fails keeps the attempts already read — and still announces itself, so a
     * consumer whose own derivation failed over those attempts derives again rather than waiting
     * for a history change that may never come.
     */
    @Test
    fun aFailedReReadEmitsAgainAndKeepsTheAttemptsAlreadyRead() = runStoreTest {
        val repository = FakeAssessmentRepository(listOf(completedAttempt("question_a")))
        val store = testHistoryStore(repository, testCacheScope())
        val observerScope = testCacheScope()
        val observed = mutableListOf<AssessmentHistory>()
        val observer = observerScope.launch { store.history.toList(observed) }
        advanceUntilIdle()

        repository.failure = IllegalStateException("Database unavailable")
        store.invalidate()
        advanceUntilIdle()
        observer.cancel()

        assertEquals(2, observed.settled().size, "A failed refresh is still a refresh.")
        assertEquals(
            listOf("question_a"),
            assertIs<AssessmentHistory.Loaded>(observed.last()).attempts.questionIds(),
        )
    }

    /** Nothing has been read yet, so a late subscriber is told that rather than "no history". */
    @Test
    fun aSubscriberBeforeTheFirstReadSettlesSeesLoading() = runStoreTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeAssessmentRepository(listOf(completedAttempt("question_a")))
        repository.beforeRead = { gate.await() }
        val store = testHistoryStore(repository, testCacheScope())
        runCurrent()

        assertIs<AssessmentHistory.Loading>(store.history.first())

        gate.complete(Unit)
        advanceUntilIdle()

        assertIs<AssessmentHistory.Loaded>(store.history.first())
    }

    /** A screen re-entering composition renders the cached history rather than a spinner. */
    @Test
    fun aLateSubscriberReplaysTheCachedHistory() = runStoreTest {
        val repository = FakeAssessmentRepository(listOf(completedAttempt("question_a")))
        val store = testHistoryStore(repository, testCacheScope())
        advanceUntilIdle()

        val observerScope = testCacheScope()
        val observed = mutableListOf<AssessmentHistory>()
        val observer = observerScope.launch { store.history.toList(observed) }
        advanceUntilIdle()
        observer.cancel()

        assertEquals(
            listOf<AssessmentHistory>(AssessmentHistory.Loaded(repository.attempts)),
            observed,
        )
        assertEquals(1, repository.reads, "Subscribing must not start a read of its own.")
    }

    /**
     * Drops the pre-read [AssessmentHistory.Loading] the shared flow seeds itself with, so a count
     * of announcements is a count of settled refreshes rather than of subscription timing.
     */
    private fun List<AssessmentHistory>.settled(): List<AssessmentHistory> =
        filterNot { it is AssessmentHistory.Loading }

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
            val result = attempts
            reads++
            beforeRead()
            failure?.let { throw it }
            return result
        }
    }
}
