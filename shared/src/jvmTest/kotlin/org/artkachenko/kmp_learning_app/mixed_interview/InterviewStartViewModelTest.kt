package org.artkachenko.kmp_learning_app.mixed_interview

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import kotlinx.coroutines.flow.StateFlow
import org.artkachenko.kmp_learning_app.assessment.history.testCacheScope
import org.artkachenko.kmp_learning_app.assessment.history.testHistoryStore
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository

internal class InterviewStartViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun aLearnerWithNoInterviewsHasNoRecord() = runTest(dispatcher) {
        val state = interviewState(RecordingRepository(emptyList()))
        testScheduler.advanceUntilIdle()

        assertIs<InterviewHistoryUiState.Empty>(state.value)
    }

    @Test
    fun focusedPracticeDoesNotCountAsAnInterview() = runTest(dispatcher) {
        val state = interviewState(RecordingRepository(listOf(focusedAttempt("focused", correct = 9, total = 10))),
        )
        testScheduler.advanceUntilIdle()

        assertIs<InterviewHistoryUiState.Empty>(state.value)
    }

    @Test
    fun theLatestAndBestInterviewsAreReported() = runTest(dispatcher) {
        // Newest first, matching the repository's own ordering: the most recent interview went
        // worse than an earlier one, so latest and best have to be different attempts.
        val state = interviewState(
            RecordingRepository(
                listOf(
                    mixedAttempt("newest", correct = 5, total = 20),
                    mixedAttempt("best", correct = 18, total = 20),
                    mixedAttempt("oldest", correct = 10, total = 20),
                ),
            ),
        )
        testScheduler.advanceUntilIdle()

        val history = assertIs<InterviewHistoryUiState.Content>(state.value).history
        assertEquals(3, history.attemptCount)
        assertEquals("newest", history.latest.attemptId)
        assertEquals(5, history.latest.correctAnswers)
        assertEquals("best", history.best.attemptId)
        assertEquals(90.0, history.best.percentage)
    }

    @Test
    fun aSingleInterviewIsBothTheLatestAndTheBest() = runTest(dispatcher) {
        val state = interviewState(
            RecordingRepository(listOf(mixedAttempt("only", correct = 7, total = 20))),
        )
        testScheduler.advanceUntilIdle()

        val history = assertIs<InterviewHistoryUiState.Content>(state.value).history
        assertEquals("only", history.latest.attemptId)
        assertEquals("only", history.best.attemptId)
    }

    @Test
    fun aFailedReadLeavesTheScreenStartable() = runTest(dispatcher) {
        val state = interviewState(FailingRepository)
        testScheduler.advanceUntilIdle()

        assertIs<InterviewHistoryUiState.Empty>(state.value)
    }

    /**
     * The record is derived by [InterviewHistoryStateHolder], which outlives the ViewModel; the
     * ViewModel only republishes it, so the behaviour is exercised on the holder.
     */
    private fun interviewState(repository: AssessmentRepository): StateFlow<InterviewHistoryUiState> {
        val scope = testCacheScope()
        return InterviewHistoryStateHolder(testHistoryStore(repository, scope), scope).state
    }

}

private fun mixedAttempt(id: String, correct: Int, total: Int): TestAttempt =
    attempt(id, AssessmentConfig.Mixed(questionCount = total), correct, total)

private fun focusedAttempt(id: String, correct: Int, total: Int): TestAttempt =
    attempt(
        id,
        AssessmentConfig.Focused(
            scope = AssessmentScope.Topic(topicId = "topic"),
            questionCount = total,
        ),
        correct,
        total,
    )

private fun attempt(
    id: String,
    config: AssessmentConfig,
    correct: Int,
    total: Int,
): TestAttempt =
    TestAttempt(
        id = id,
        config = config,
        status = AssessmentStatus.COMPLETED,
        // TestAttempt requires a completed attempt's score to match its answered questions,
        // so the fixture builds one question per point in the score.
        questionAttempts = List(total) { index ->
            QuestionAttempt(
                questionId = "$id-q$index",
                answerState = QuestionAnswerState.Answered(
                    selectedAnswerIds = setOf("answer"),
                    isCorrect = index < correct,
                ),
            )
        },
        score = AssessmentScore(totalQuestions = total, correctAnswers = correct),
        startedAt = Instant.fromEpochMilliseconds(0),
        completedAt = Instant.fromEpochMilliseconds(1),
    )

private class RecordingRepository(
    private val attempts: List<TestAttempt>,
) : AssessmentRepository {
    override suspend fun save(attempt: TestAttempt) = Unit

    override suspend fun getById(attemptId: String): TestAttempt? =
        attempts.firstOrNull { it.id == attemptId }

    override suspend fun getCompletedAttempts(): List<TestAttempt> = attempts
}

private object FailingRepository : AssessmentRepository {
    override suspend fun save(attempt: TestAttempt) = Unit

    override suspend fun getById(attemptId: String): TestAttempt? = null

    override suspend fun getCompletedAttempts(): List<TestAttempt> =
        throw IllegalStateException("database unavailable")
}
