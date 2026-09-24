package org.artkachenko.kmp_learning_app.assessment.retake

import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment.start.StartAssessment
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

/**
 * The retake state machine on its own, with no result screen around it.
 *
 * The Focused practice result and the Mixed interview result each held an identical copy of this,
 * under two different names, so every rule below had to be stated and proved twice through two
 * ViewModels and their fakes. It is one owner now: these are its rules, and the two ViewModel
 * suites keep only what is genuinely theirs — that they delegate to it, and that they refuse to
 * retake a result that has not loaded.
 *
 * The real [AssessmentRetakeService] is used rather than a stand-in, over fake repositories: the
 * outcomes this controller reacts to are produced by the actual creation pipeline, so a change that
 * stopped producing one of them would fail here rather than pass against a fiction.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class AssessmentRetakeControllerTest {

    @Test
    fun createdIsTerminalUntilTheSameIdentityIsConfirmedNavigated() = runTest {
        val fixture = RetakeFixture()
        fixture.curriculum.topicQuestions = mapOf("topic" to listOf(question("q1")))
        val controller = fixture.controller(this)
        val event = async { controller.createdAttempts.first() }

        controller.start()
        assertEquals(AssessmentRetakeState.Creating, controller.state.value)
        advanceUntilIdle()

        assertEquals(AssessmentRetakeCreated("retake-1"), event.await())
        assertEquals(AssessmentRetakeState.Created("retake-1"), controller.state.value)

        // The window CQ-BUG-003 described: persistence has finished, the buffered event has not
        // been consumed, and pressing again must not create a second durable attempt.
        controller.start()
        advanceUntilIdle()
        assertEquals(1, fixture.assessments.saveCalls)

        // A confirmation for some other retake must not release the guard protecting this one.
        controller.onCreatedAttemptHandled("retake-2")
        assertEquals(AssessmentRetakeState.Created("retake-1"), controller.state.value)

        controller.onCreatedAttemptHandled("retake-1")
        assertEquals(AssessmentRetakeState.Idle, controller.state.value)

        controller.start()
        advanceUntilIdle()
        assertEquals(2, fixture.assessments.saveCalls)
    }

    @Test
    fun duplicateStartsWhileCreationIsRunningProduceOneRetake() = runTest {
        val fixture = RetakeFixture()
        fixture.curriculum.topicQuestions = mapOf("topic" to listOf(question("q1")))
        fixture.curriculum.gate = CompletableDeferred()
        val controller = fixture.controller(this)

        controller.start()
        controller.start()
        runCurrent()

        assertEquals(AssessmentRetakeState.Creating, controller.state.value)
        assertEquals(1, fixture.curriculum.topicQuestionCalls)

        fixture.curriculum.gate?.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, fixture.assessments.saveCalls)
    }

    /**
     * Nothing durable was created, so the button itself is the retry. Each outcome is reported as
     * itself rather than collapsed into one failure: they mean different things to the learner, and
     * the two result screens word all three differently.
     */
    @Test
    fun everyFailureIsDistinctAndLeavesTheActionAvailableAgain() = runTest {
        val fixture = RetakeFixture()
        val controller = fixture.controller(this)

        // The source attempt is gone, so there is nothing to take again.
        fixture.assessments.attempts.remove(SourceAttemptId)
        controller.start()
        advanceUntilIdle()
        assertEquals(AssessmentRetakeState.SourceAttemptNotFound, controller.state.value)

        // The source is back, but its scope no longer holds an eligible Question.
        fixture.assessments.attempts[SourceAttemptId] = sourceAttempt()
        fixture.curriculum.topicQuestions = emptyMap()
        controller.start()
        advanceUntilIdle()
        assertEquals(AssessmentRetakeState.NoEligibleQuestions, controller.state.value)

        // The read itself fails, which is neither of the two answers above.
        fixture.curriculum.failure = IllegalStateException("curriculum unavailable")
        controller.start()
        advanceUntilIdle()
        assertEquals(AssessmentRetakeState.Error, controller.state.value)

        fixture.curriculum.failure = null
        fixture.curriculum.topicQuestions = mapOf("topic" to listOf(question("q1")))
        controller.start()
        advanceUntilIdle()
        assertEquals(AssessmentRetakeState.Created("retake-1"), controller.state.value)
        assertEquals(1, fixture.assessments.saveCalls)
    }

    @Test
    fun noFailedRetakeEverReachesNavigation() = runTest {
        val fixture = RetakeFixture()
        fixture.curriculum.topicQuestions = emptyMap()
        val controller = fixture.controller(this)
        val event = async { controller.createdAttempts.first() }

        controller.start()
        advanceUntilIdle()

        assertEquals(AssessmentRetakeState.NoEligibleQuestions, controller.state.value)
        assertFalse(event.isCompleted)
        event.cancel()
    }

    /**
     * Cancellation means the owning screen is going away, not that the retake failed. Reporting it
     * as [AssessmentRetakeState.Error] would claim a failure that never happened, on a surface that
     * is being torn down.
     */
    @Test
    fun cancellationDoesNotBecomeAFailedRetake() = runTest {
        val fixture = RetakeFixture()
        fixture.curriculum.failure = CancellationException("scope ending")
        val controller = fixture.controller(this)

        controller.start()
        advanceUntilIdle()

        assertEquals(AssessmentRetakeState.Creating, controller.state.value)
    }

    @Test
    fun theControllerRetakesTheAttemptItWasBuiltFor() = runTest {
        val fixture = RetakeFixture()
        fixture.curriculum.topicQuestions = mapOf("topic" to listOf(question("q1")))
        fixture.controller(this).start()
        advanceUntilIdle()

        assertEquals(listOf(SourceAttemptId), fixture.assessments.lookups)
    }

    private class RetakeFixture {
        val assessments = FakeAssessmentRepository().apply {
            attempts[SourceAttemptId] = sourceAttempt()
        }
        val curriculum = FakeCurriculumRepository()

        fun controller(scope: TestScope) = AssessmentRetakeController(
            sourceAttemptId = SourceAttemptId,
            retakeService = AssessmentRetakeService(
                assessmentRepository = assessments,
                startAssessment = StartAssessment(
                    assessmentRepository = assessments,
                    assessmentEngine = AssessmentEngine(
                        questionSelector = AssessmentQuestionSelector(
                            curriculumRepository = curriculum,
                            completedHistory = { emptyList() },
                            randomize = { it },
                        ),
                        generateAttemptId = { "retake-1" },
                        now = { Instant.fromEpochMilliseconds(2) },
                    ),
                ),
            ),
            scope = scope,
        )
    }

    private class FakeAssessmentRepository : AssessmentRepository {
        val attempts = mutableMapOf<String, TestAttempt>()
        val lookups = mutableListOf<String>()
        var saveCalls = 0

        override suspend fun save(attempt: TestAttempt) {
            saveCalls++
            attempts[attempt.id] = attempt
        }

        override suspend fun getById(attemptId: String): TestAttempt? {
            lookups += attemptId
            return attempts[attemptId]
        }

        override suspend fun getCompletedAttempts(): List<TestAttempt> = emptyList()
    }

    private class FakeCurriculumRepository : CurriculumRepository {
        var topicQuestions: Map<String, List<Question>> = emptyMap()
        var topicQuestionCalls = 0
        var failure: Throwable? = null

        /** Suspends the selection read so two starts can be made while one is genuinely running. */
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun getActiveQuestionsByTopicAndLevels(
            topicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> {
            topicQuestionCalls++
            gate?.await()
            failure?.let { throw it }
            return topicQuestions[topicId].orEmpty().filter { it.level in levels }
        }

        override suspend fun getActiveTopics(): List<Topic> = error("Not used.")
        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> = error("Not used.")
        override suspend fun getActiveQuestions(): List<Question> = error("Not used.")
        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
            error("Not used.")
        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
            error("Not used.")
        override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> =
            error("Not used.")
        override suspend fun getActiveQuestionsBySubtopicAndLevels(
            subtopicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = error("Not used.")
        override suspend fun getTopicById(topicId: String): Topic? = error("Not used.")
        override suspend fun getSubtopicById(subtopicId: String): Subtopic? = error("Not used.")
        override suspend fun getQuestionById(questionId: String): Question? = error("Not used.")
    }
}

private const val SourceAttemptId = "attempt-source"

private fun sourceAttempt(): TestAttempt = TestAttempt(
    id = SourceAttemptId,
    config = AssessmentConfig.Focused(AssessmentScope.Topic("topic"), questionCount = 1),
    questionAttempts = listOf(
        QuestionAttempt(
            questionId = "q1",
            answerState = QuestionAnswerState.Answered(setOf("q1_a"), isCorrect = true),
        ),
    ),
    status = AssessmentStatus.COMPLETED,
    startedAt = Instant.fromEpochMilliseconds(0),
    completedAt = Instant.fromEpochMilliseconds(1),
    score = AssessmentScore(totalQuestions = 1, correctAnswers = 1),
)

private fun question(id: String): Question = Question(
    id = id,
    topicId = "topic",
    subtopicId = "subtopic",
    text = "$id?",
    answers = listOf(AnswerOption("${id}_a", "A"), AnswerOption("${id}_b", "B")),
    selectionMode = AnswerSelectionMode.SINGLE,
    level = QuestionLevel.FOUNDATION,
    correctAnswerIds = listOf("${id}_a"),
    explanation = "$id explanation.",
    sources = listOf(SourceReference("Source", "https://example.invalid/$id")),
    status = ContentStatus.ACTIVE,
)
