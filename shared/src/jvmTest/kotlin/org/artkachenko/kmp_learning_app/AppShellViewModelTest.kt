package org.artkachenko.kmp_learning_app

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.history.testCacheScope
import org.artkachenko.kmp_learning_app.assessment.history.testHistoryStore
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService

/**
 * The navigation badge: the one number the shell shows from every area, and the only state the
 * navigation control has of its own.
 *
 * Two rules are worth stating separately from the Mistakes screen's. The count comes from the
 * shared history cache rather than from a read the shell makes, so it moves when an assessment
 * completes rather than when the learner happens to switch areas. And a history that is loading or
 * unreadable badges *nothing* — a badge is decoration, and interrupting navigation over it would
 * cost the learner the ability to move around the app to report a fact they cannot act on. Both
 * were decided deliberately; neither had a test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class AppShellViewModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * The count is the mistake queue's own rule applied to the shared history — the queue's
     * semantics are proven in `MistakeReviewServiceTest`; what this pins is that the badge shows
     * that number and not, say, the number of incorrect answers ever given.
     */
    @Test
    fun theBadgeCountsTheUnresolvedMistakesInTheSharedHistory() = runShellTest {
        val repository = FakeAssessmentRepository(
            listOf(
                completedAttempt(
                    id = "attempt_1",
                    completedAtSeconds = 60,
                    answers = listOf("q_a" to false, "q_b" to false, "q_c" to true),
                ),
            ),
        )
        val viewModel = shellViewModel(repository)

        advanceUntilIdle()

        assertEquals(2, viewModel.unresolvedMistakeCount.value)
    }

    /** Before the first read settles there is nothing to say, and no badge says it. */
    @Test
    fun aHistoryThatHasNotLoadedBadgesNothing() = runShellTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeAssessmentRepository(
            listOf(
                completedAttempt(
                    id = "attempt_1",
                    completedAtSeconds = 60,
                    answers = listOf("q_a" to false),
                ),
            ),
        )
        repository.beforeRead = { gate.await() }
        val viewModel = shellViewModel(repository)

        advanceUntilIdle()
        assertEquals(0, viewModel.unresolvedMistakeCount.value)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, viewModel.unresolvedMistakeCount.value)
    }

    /**
     * An unreadable attempt table is indistinguishable from nothing unresolved, on purpose: there
     * is no navigation-level surface on which the failure could be reported without interrupting
     * navigation itself. What must not happen is the shell failing, or badging a number it cannot
     * stand behind.
     */
    @Test
    fun anUnreadableHistoryBadgesNothingRatherThanInterruptingNavigation() = runShellTest {
        val repository = FakeAssessmentRepository(
            listOf(
                completedAttempt(
                    id = "attempt_1",
                    completedAtSeconds = 60,
                    answers = listOf("q_a" to false, "q_b" to false),
                ),
            ),
        )
        repository.failure = IllegalStateException("Database unavailable")
        val viewModel = shellViewModel(repository)

        advanceUntilIdle()

        assertEquals(0, viewModel.unresolvedMistakeCount.value)
    }

    /**
     * Why the badge is derived from the shared cache rather than counted on navigation.
     *
     * Answering `q_a` correctly in a later assessment resolves it, and the badge has to follow that
     * while the shell stays alive — the learner is looking at the navigation bar when the result
     * screen appears. One completion is one invalidation is one further read: the shell contributes
     * no read of its own, which is the property that kept this count off the Progress dashboard.
     */
    @Test
    fun completingAnAssessmentMovesTheBadgeOnTheSameReadEveryScreenUses() = runShellTest {
        val repository = FakeAssessmentRepository(
            listOf(
                completedAttempt(
                    id = "attempt_1",
                    completedAtSeconds = 60,
                    answers = listOf("q_a" to false, "q_b" to false),
                ),
            ),
        )
        val store = testHistoryStore(repository, testCacheScope())
        val viewModel = shellViewModel(repository, store)
        advanceUntilIdle()
        assertEquals(2, viewModel.unresolvedMistakeCount.value)
        assertEquals(1, repository.reads)

        // Newest first, exactly as the repository returns completed history.
        repository.attempts = listOf(
            completedAttempt(
                id = "attempt_2",
                completedAtSeconds = 120,
                answers = listOf("q_a" to true),
            ),
        ) + repository.attempts
        store.invalidate()
        advanceUntilIdle()

        assertEquals(1, viewModel.unresolvedMistakeCount.value)
        assertEquals(2, repository.reads)
    }

    /**
     * Navigation 3 rebuilds the shell's ViewModel when the host recomposes it, and the cache is
     * app-scoped precisely so that costs nothing. The replacement badges the cached count off the
     * replayed history rather than starting a read and showing no badge until it lands.
     */
    @Test
    fun aRebuiltShellBadgesTheCachedCountWithoutReadingHistoryAgain() = runShellTest {
        val repository = FakeAssessmentRepository(
            listOf(
                completedAttempt(
                    id = "attempt_1",
                    completedAtSeconds = 60,
                    answers = listOf("q_a" to false, "q_b" to false),
                ),
            ),
        )
        val store = testHistoryStore(repository, testCacheScope())
        shellViewModel(repository, store)
        advanceUntilIdle()

        val rebuilt = shellViewModel(repository, store)
        advanceUntilIdle()

        assertEquals(2, rebuilt.unresolvedMistakeCount.value)
        assertEquals(1, repository.reads, "A rebuilt shell must not start a read of its own.")
    }

    private fun TestScope.shellViewModel(
        repository: AssessmentRepository,
        store: AssessmentHistoryStore = testHistoryStore(repository, testCacheScope()),
    ): AppShellViewModel =
        AppShellViewModel(
            mistakeReviewService = MistakeReviewService(
                assessmentRepository = repository,
                // The badge counts occurrences and never reconstructs review content, so a
                // repository that refuses every read is the assertion: reaching for a Question
                // here would fail the test rather than quietly cost a curriculum round trip.
                assessmentReviewLoader = AssessmentReviewLoader(UnreadCurriculumRepository),
            ),
            historyStore = store,
        )

    private fun runShellTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }

    private fun completedAttempt(
        id: String,
        completedAtSeconds: Long,
        answers: List<Pair<String, Boolean>>,
    ): TestAttempt =
        TestAttempt(
            id = id,
            config = AssessmentConfig.Mixed(questionCount = answers.size),
            questionAttempts = answers.map { (questionId, isCorrect) ->
                QuestionAttempt(
                    questionId = questionId,
                    answerState = QuestionAnswerState.Answered(
                        selectedAnswerIds = setOf("${questionId}_answer"),
                        isCorrect = isCorrect,
                    ),
                )
            },
            status = AssessmentStatus.COMPLETED,
            startedAt = Instant.fromEpochSeconds(completedAtSeconds - 60),
            completedAt = Instant.fromEpochSeconds(completedAtSeconds),
            score = AssessmentScore(
                totalQuestions = answers.size,
                correctAnswers = answers.count { it.second },
            ),
        )

    private class FakeAssessmentRepository(
        var attempts: List<TestAttempt>,
    ) : AssessmentRepository {
        var reads = 0
        var failure: Throwable? = null
        var beforeRead: suspend () -> Unit = {}

        override suspend fun save(attempt: TestAttempt) = error("The shell writes nothing.")

        override suspend fun getById(attemptId: String): TestAttempt? =
            error("The shell reads no individual attempt.")

        override suspend fun getCompletedAttempts(): List<TestAttempt> {
            val result = attempts
            reads++
            beforeRead()
            failure?.let { throw it }
            return result
        }
    }

    private object UnreadCurriculumRepository : CurriculumRepository {
        override suspend fun getActiveTopics(): List<Topic> = refuse()
        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> = refuse()
        override suspend fun getActiveQuestions(): List<Question> = refuse()
        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> = refuse()
        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
            refuse()

        override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> =
            refuse()

        override suspend fun getActiveQuestionsByTopicAndLevels(
            topicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = refuse()

        override suspend fun getActiveQuestionsBySubtopicAndLevels(
            subtopicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = refuse()

        override suspend fun getTopicById(topicId: String): Topic? = refuse()
        override suspend fun getSubtopicById(subtopicId: String): Subtopic? = refuse()
        override suspend fun getQuestionsByIds(
            questionIds: Collection<String>,
        ): Map<String, Question> = refuse()

        private fun refuse(): Nothing = error("The navigation badge reads no curriculum content.")
    }
}
