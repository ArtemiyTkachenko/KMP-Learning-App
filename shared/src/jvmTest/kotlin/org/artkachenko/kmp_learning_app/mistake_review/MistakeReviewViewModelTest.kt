package org.artkachenko.kmp_learning_app.mistake_review

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
import kotlinx.coroutines.flow.StateFlow
import org.artkachenko.kmp_learning_app.assessment.history.testCacheScope
import org.artkachenko.kmp_learning_app.assessment.history.testHistoryStore
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

@OptIn(ExperimentalCoroutinesApi::class)
internal class MistakeReviewViewModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun anEmptyQueueBecomesTheEmptyState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val state = mistakeState(VmHistoryRepository(emptyList()))
        advanceUntilIdle()

        assertIs<MistakeReviewUiState.Empty>(state.value)
    }

    @Test
    fun aPopulatedQueueBecomesContentInServiceOrder() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = VmHistoryRepository(
            listOf(
                vmAttempt("newest", "2026-08-29T12:00:00Z", "q3", "q1"),
                vmAttempt("oldest", "2026-08-29T10:00:00Z", "q2"),
            ),
        )
        val state = mistakeState(repository)
        advanceUntilIdle()

        val content = assertIs<MistakeReviewUiState.Content>(state.value)
        // Recency ordering belongs to the service; the ViewModel must not re-sort it.
        assertEquals(listOf("q3", "q1", "q2"), content.mistakes.map { it.questionId })
    }

    @Test
    fun loadStartsInLoadingBeforeTheServiceCompletes() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = VmHistoryRepository(
            listOf(vmAttempt("a1", "2026-08-29T10:00:00Z", "q1")),
        )
        val state = mistakeState(repository)
        assertIs<MistakeReviewUiState.Loading>(state.value)

        advanceUntilIdle()
        assertIs<MistakeReviewUiState.Content>(state.value)
    }

    @Test
    fun serviceFailureSurfacesErrorAndRetrySucceeds() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = VmHistoryRepository(
            listOf(vmAttempt("a1", "2026-08-29T10:00:00Z", "q1")),
        )
        repository.failNextLoad = true
        val scope = testCacheScope()
        val store = testHistoryStore(repository, scope)
        val state = MistakeReviewStateHolder(vmService(repository), store, scope).state
        advanceUntilIdle()
        assertIs<MistakeReviewUiState.Error>(state.value)

        // A retry marks the shared history stale, which every screen derived from it follows.
        store.invalidate()
        advanceUntilIdle()
        val content = assertIs<MistakeReviewUiState.Content>(state.value)
        assertEquals(listOf("q1"), content.mistakes.map { it.questionId })
    }
    /**
     * The queue is derived by [MistakeReviewStateHolder], which outlives the ViewModel; the
     * ViewModel only republishes it, so the behaviour is exercised on the holder.
     */
    private fun mistakeState(repository: AssessmentRepository): StateFlow<MistakeReviewUiState> {
        val scope = testCacheScope()
        return MistakeReviewStateHolder(
            mistakeReviewService = vmService(repository),
            historyStore = testHistoryStore(repository, scope),
            scope = scope,
        ).state
    }
}

private fun vmService(repository: AssessmentRepository): MistakeReviewService =
    MistakeReviewService(
        assessmentRepository = repository,
        assessmentReviewLoader = AssessmentReviewLoader(VmCurriculumRepository),
    )

/** Every listed question is answered incorrectly, so each one reaches the queue. */
private fun vmAttempt(
    id: String,
    completedAt: String,
    vararg questionIds: String,
): TestAttempt =
    TestAttempt(
        id = id,
        config = AssessmentConfig.Mixed(questionIds.size),
        questionAttempts = questionIds.map {
            QuestionAttempt(it, QuestionAnswerState.Answered(setOf("${it}_b"), isCorrect = false))
        },
        status = AssessmentStatus.COMPLETED,
        startedAt = Instant.parse("2026-08-29T09:00:00Z"),
        completedAt = Instant.parse(completedAt),
        score = AssessmentScore(questionIds.size, 0),
    )

private class VmHistoryRepository(
    private val attempts: List<TestAttempt>,
) : AssessmentRepository {
    var failNextLoad = false

    override suspend fun save(attempt: TestAttempt) = Unit

    override suspend fun getById(attemptId: String): TestAttempt? =
        attempts.firstOrNull { it.id == attemptId }

    override suspend fun getCompletedAttempts(): List<TestAttempt> {
        if (failNextLoad) {
            failNextLoad = false
            error("History unavailable")
        }
        return attempts
    }
}

private object VmCurriculumRepository : CurriculumRepository {
    override suspend fun getActiveTopics(): List<Topic> = error("ACTIVE lookup must not be used.")
    override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestions(): List<Question> = error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsByTopicAndLevels(
        topicId: String,
        levels: Set<QuestionLevel>,
    ): List<Question> = error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsBySubtopicAndLevels(
        subtopicId: String,
        levels: Set<QuestionLevel>,
    ): List<Question> = error("ACTIVE lookup must not be used.")
    override suspend fun getTopicById(topicId: String): Topic? = error("Topic lookup is not needed.")
    override suspend fun getSubtopicById(subtopicId: String): Subtopic? =
        error("Subtopic lookup is not needed.")

    override suspend fun getQuestionById(questionId: String): Question? =
        Question(
            id = questionId,
            topicId = "kotlin",
            subtopicId = "coroutines",
            text = "Question $questionId",
            answers = listOf(
                AnswerOption("${questionId}_a", "Answer A"),
                AnswerOption("${questionId}_b", "Answer B"),
            ),
            selectionMode = AnswerSelectionMode.SINGLE,
            level = QuestionLevel.FOUNDATION,
            correctAnswerIds = listOf("${questionId}_a"),
            explanation = "Explanation",
            sources = listOf(SourceReference("Source", "https://example.com/$questionId")),
        )
}
