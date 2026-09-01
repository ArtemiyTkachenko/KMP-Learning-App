package org.artkachenko.kmp_learning_app.assessment.retake

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Instant
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
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

internal class AssessmentRetakeServiceTest {
    @Test
    fun retakeCreatesNewPersistedAttemptFromFocusedSourceWithFreshSelection() = runRetakeTest {
        val source = completedSourceAttempt(
            config = AssessmentConfig.Focused(
                scope = AssessmentScope.Topic("topic"),
                questionCount = 2,
            ),
            questionIds = listOf("question_a", "question_b"),
        )
        assessmentRepository.attempts[source.id] = source
        curriculumRepository.topicQuestions = mapOf(
            "topic" to listOf(
                question("question_b"),
                question("question_c"),
            ),
        )

        val result = service().createRetake(source.id)

        val created = assertIs<AssessmentRetakeResult.Created>(result).session
        assertEquals("retake-1", created.attempt.id)
        assertEquals(source.config, created.attempt.config)
        assertEquals(listOf("question_b", "question_c"), created.attempt.questionAttempts.map { it.questionId })
        assertEquals(listOf("question_b", "question_c"), created.questions.map { it.id })
        assertEquals(
            listOf(QuestionAnswerState.Unanswered, QuestionAnswerState.Unanswered),
            created.attempt.questionAttempts.map { it.answerState },
        )
        assertEquals(AssessmentStatus.IN_PROGRESS, created.attempt.status)
        assertNull(created.attempt.score)
        assertNull(created.attempt.completedAt)
        assertEquals(RetakeStartedAt, created.attempt.startedAt)
        assertEquals(created.attempt, assessmentRepository.getById("retake-1"))
        assertEquals(source, assessmentRepository.getById(source.id))
    }

    @Test
    fun mixedRetakePreservesConfigCreatesCleanAttemptAndUsesBalancedSelectionWithOverlap() = runRetakeTest {
        val source = completedSourceAttempt(
            config = AssessmentConfig.Mixed(questionCount = 3),
            questionIds = listOf("a1", "b1", "c1"),
        )
        assessmentRepository.attempts[source.id] = source
        curriculumRepository.activeQuestions = listOf(
            question("a1", topicId = "topic-a"),
            question("a2", topicId = "topic-a"),
            question("b1", topicId = "topic-b"),
            question("b2", topicId = "topic-b"),
            question("c1", topicId = "topic-c"),
        )

        val result = service().createRetake(source.id)

        val created = assertIs<AssessmentRetakeResult.Created>(result).session
        assertEquals("retake-1", created.attempt.id)
        assertEquals(source.config, created.attempt.config)
        assertEquals(listOf("a1", "b1", "c1"), created.attempt.questionAttempts.map { it.questionId })
        assertEquals(AssessmentStatus.IN_PROGRESS, created.attempt.status)
        assertEquals(null, created.attempt.score)
        assertEquals(null, created.attempt.completedAt)
        assertEquals(
            listOf(QuestionAnswerState.Unanswered, QuestionAnswerState.Unanswered, QuestionAnswerState.Unanswered),
            created.attempt.questionAttempts.map { it.answerState },
        )
        assertEquals(source, assessmentRepository.getById(source.id))
        assertEquals(created.attempt, assessmentRepository.getById(created.attempt.id))
    }

    @Test
    fun sourceNotFoundReturnsResultWithoutStartingOrSaving() = runRetakeTest {
        val result = service().createRetake("missing")

        assertEquals(AssessmentRetakeResult.SourceAttemptNotFound, result)
        assertEquals(0, assessmentRepository.saveCalls)
        assertEquals(0, curriculumRepository.activeQuestionCalls)
        assertEquals(0, curriculumRepository.topicQuestionCalls)
        assertEquals(0, curriculumRepository.subtopicQuestionCalls)
    }

    @Test
    fun blankSourceIdIsRejected() = runRetakeTest {
        assertFailsWith<IllegalArgumentException> {
            service().createRetake(" ")
        }
    }

    @Test
    fun inProgressSourceCannotBeRetaken() = runRetakeTest {
        assessmentRepository.attempts["in_progress"] = TestAttempt(
            id = "in_progress",
            config = AssessmentConfig.Mixed(questionCount = 1),
            questionAttempts = listOf(QuestionAttempt("question_a")),
            status = AssessmentStatus.IN_PROGRESS,
            startedAt = SourceStartedAt,
        )

        assertFailsWith<IllegalStateException> {
            service().createRetake("in_progress")
        }
    }

    @Test
    fun noEligibleQuestionsReturnsResultAndDoesNotSaveRetake() = runRetakeTest {
        val source = completedSourceAttempt(
            config = AssessmentConfig.Focused(
                scope = AssessmentScope.Subtopic("subtopic"),
                questionCount = 2,
            ),
            questionIds = listOf("question_a"),
        )
        assessmentRepository.attempts[source.id] = source
        curriculumRepository.subtopicQuestions = emptyMap()

        val result = service().createRetake(source.id)

        assertEquals(AssessmentRetakeResult.NoEligibleQuestions, result)
        assertEquals(0, assessmentRepository.saveCalls)
        assertEquals(source, assessmentRepository.getById(source.id))
    }

    @Test
    fun mixedRetakeWithoutActiveQuestionsDoesNotPersist() = runRetakeTest {
        val source = completedSourceAttempt(
            config = AssessmentConfig.Mixed(questionCount = 3),
            questionIds = listOf("question_a"),
        )
        assessmentRepository.attempts[source.id] = source

        val result = service().createRetake(source.id)

        assertEquals(AssessmentRetakeResult.NoEligibleQuestions, result)
        assertEquals(0, assessmentRepository.saveCalls)
        assertEquals(source, assessmentRepository.getById(source.id))
    }

    @Test
    fun eachRetakeFromSameSourceIsIndependent() = runRetakeTest {
        val source = completedSourceAttempt(
            config = AssessmentConfig.Mixed(questionCount = 1),
            questionIds = listOf("question_a"),
        )
        assessmentRepository.attempts[source.id] = source
        curriculumRepository.activeQuestions = listOf(question("question_b"))

        val first = assertIs<AssessmentRetakeResult.Created>(
            service().createRetake(source.id),
        ).session
        val second = assertIs<AssessmentRetakeResult.Created>(
            service().createRetake(source.id),
        ).session

        assertEquals("retake-1", first.attempt.id)
        assertEquals("retake-2", second.attempt.id)
        assertEquals(first.attempt, assessmentRepository.getById("retake-1"))
        assertEquals(second.attempt, assessmentRepository.getById("retake-2"))
        assertEquals(source, assessmentRepository.getById(source.id))
    }

    private fun runRetakeTest(
        block: suspend RetakeTestScope.() -> Unit,
    ) {
        var outcome: Result<Unit>? = null
        block.startCoroutine(
            receiver = RetakeTestScope(),
            completion = object : Continuation<Unit> {
                override val context = EmptyCoroutineContext

                override fun resumeWith(result: Result<Unit>) {
                    outcome = result
                }
            },
        )
        outcome?.getOrThrow()
            ?: error("Retake test did not complete synchronously.")
    }

    private class RetakeTestScope {
        val assessmentRepository = FakeAssessmentRepository()
        val curriculumRepository = FakeCurriculumRepository()
        private var nextAttemptNumber = 1

        fun service(): AssessmentRetakeService =
            AssessmentRetakeService(
                assessmentRepository = assessmentRepository,
                assessmentEngine = AssessmentEngine(
                    questionSelector = AssessmentQuestionSelector(
                        curriculumRepository = curriculumRepository,
                        randomize = { it },
                    ),
                    generateAttemptId = { "retake-${nextAttemptNumber++}" },
                    now = { RetakeStartedAt },
                ),
            )
    }

    private class FakeAssessmentRepository : AssessmentRepository {
        val attempts = mutableMapOf<String, TestAttempt>()
        var saveCalls = 0

        override suspend fun save(attempt: TestAttempt) {
            saveCalls++
            attempts[attempt.id] = attempt
        }

        override suspend fun getById(attemptId: String): TestAttempt? =
            attempts[attemptId]

        override suspend fun getCompletedAttempts(): List<TestAttempt> = emptyList()
    }

    private class FakeCurriculumRepository : CurriculumRepository {
        var activeQuestions: List<Question> = emptyList()
        var topicQuestions: Map<String, List<Question>> = emptyMap()
        var subtopicQuestions: Map<String, List<Question>> = emptyMap()
        var activeQuestionCalls = 0
        var topicQuestionCalls = 0
        var subtopicQuestionCalls = 0

        override suspend fun getActiveTopics(): List<Topic> =
            error("Not used by retake tests.")

        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> =
            error("Not used by retake tests.")

        override suspend fun getActiveQuestions(): List<Question> {
            activeQuestionCalls++
            return activeQuestions
        }

        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> {
            topicQuestionCalls++
            return topicQuestions[topicId].orEmpty()
        }

        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> {
            subtopicQuestionCalls++
            return subtopicQuestions[subtopicId].orEmpty()
        }

        override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> =
            error("Not used by retake tests.")

        override suspend fun getActiveQuestionsByTopicAndLevels(
            topicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = error("Not used by retake tests.")

        override suspend fun getActiveQuestionsBySubtopicAndLevels(
            subtopicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = error("Not used by retake tests.")

        override suspend fun getTopicById(topicId: String): Topic? =
            error("Not used by retake tests.")

        override suspend fun getSubtopicById(subtopicId: String): Subtopic? =
            error("Not used by retake tests.")

        override suspend fun getQuestionById(questionId: String): Question? =
            error("Not used by retake tests.")
    }

    private fun completedSourceAttempt(
        config: AssessmentConfig,
        questionIds: List<String>,
    ): TestAttempt =
        TestAttempt(
            id = "attempt-source",
            config = config,
            questionAttempts = questionIds.map { questionId ->
                QuestionAttempt(
                    questionId = questionId,
                    answerState = QuestionAnswerState.Answered(
                        selectedAnswerIds = setOf("${questionId}_a"),
                        isCorrect = true,
                    ),
                )
            },
            status = AssessmentStatus.COMPLETED,
            startedAt = SourceStartedAt,
            completedAt = SourceCompletedAt,
            score = AssessmentScore(
                totalQuestions = questionIds.size,
                correctAnswers = questionIds.size,
            ),
        )

    private fun question(id: String, topicId: String = "topic"): Question =
        Question(
            id = id,
            topicId = topicId,
            subtopicId = "subtopic",
            text = "$id?",
            answers = listOf(
                AnswerOption(id = "${id}_a", text = "A"),
                AnswerOption(id = "${id}_b", text = "B"),
            ),
            selectionMode = AnswerSelectionMode.SINGLE,
            level = QuestionLevel.FOUNDATION,
            correctAnswerIds = listOf("${id}_a"),
            explanation = "$id explanation.",
            sources = listOf(
                SourceReference(
                    title = "$id source",
                    url = "https://example.com/$id",
                ),
            ),
            status = ContentStatus.ACTIVE,
        )
}

private val SourceStartedAt = Instant.fromEpochMilliseconds(1_700_000_000_000)
private val SourceCompletedAt = Instant.fromEpochMilliseconds(1_700_000_060_000)
private val RetakeStartedAt = Instant.fromEpochMilliseconds(1_700_000_120_000)
