package org.artkachenko.kmp_learning_app.assessment.session

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.math.abs
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

internal class AssessmentEngineTest {
    @Test
    fun startFocusedAssessmentCreatesInProgressSession() = runEngineTest {
        repository.topicQuestions = mapOf(
            "android_ui" to listOf(
                question("question_a"),
                question("question_b"),
            ),
        )
        val config = AssessmentConfig.Focused(
            scope = AssessmentScope.Topic("android_ui"),
            questionCount = 2,
        )

        val result = engine().start(config)

        val session = assertStarted(result)
        assertEquals("attempt-1", session.attempt.id)
        assertEquals(config, session.attempt.config)
        assertEquals(AssessmentStatus.IN_PROGRESS, session.attempt.status)
        assertEquals(StartedAt, session.attempt.startedAt)
        assertNull(session.attempt.completedAt)
        assertNull(session.attempt.score)
        assertEquals(listOf("question_a", "question_b"), session.questions.map { it.id })
        assertEquals(listOf("question_a", "question_b"), session.attempt.questionAttempts.map { it.questionId })
        assertEquals(
            listOf(QuestionAnswerState.Unanswered, QuestionAnswerState.Unanswered),
            session.attempt.questionAttempts.map { it.answerState },
        )
    }

    @Test
    fun startMixedAssessmentUsesSameEnginePath() = runEngineTest {
        repository.activeQuestions = listOf(
            question("android_question"),
            question("kotlin_question"),
        )
        val config = AssessmentConfig.Mixed(questionCount = 2)

        val result = engine().start(config)

        val session = assertStarted(result)
        assertEquals(config, session.attempt.config)
        assertEquals(listOf("android_question", "kotlin_question"), session.questions.map { it.id })
    }

    @Test
    fun everyStartedAssessmentUsesANewGeneratedAttemptId() = runEngineTest {
        repository.activeQuestions = listOf(question("question_a"))
        val engine = engine()

        val first = assertStarted(engine.start(AssessmentConfig.Mixed(questionCount = 1)))
        val second = assertStarted(engine.start(AssessmentConfig.Mixed(questionCount = 1)))

        assertEquals("attempt-1", first.attempt.id)
        assertEquals("attempt-2", second.attempt.id)
    }

    @Test
    fun startReturnsNoEligibleQuestionsWhenSelectorReturnsEmptyList() = runEngineTest {
        repository.activeQuestions = emptyList()

        val result = engine().start(AssessmentConfig.Mixed(questionCount = 10))

        assertEquals(AssessmentStartResult.NoEligibleQuestions, result)
    }

    @Test
    fun undersizedSelectionKeepsRequestedConfigButUsesActualQuestionAttempts() = runEngineTest {
        repository.activeQuestions = listOf(
            question("question_a"),
            question("question_b"),
            question("question_c"),
        )
        val config = AssessmentConfig.Mixed(questionCount = 10)

        val session = assertStarted(engine().start(config))

        assertEquals(10, session.attempt.config.questionCount)
        assertEquals(3, session.attempt.questionAttempts.size)
    }

    @Test
    fun correctSingleAnswerIsScoredCorrect() = runEngineTest {
        val session = sessionWith(question("single", correctAnswerIds = listOf("single_a")))

        val updated = engine().submitAnswer(session, "single", listOf("single_a"))

        assertAnswered(
            session = updated,
            questionId = "single",
            selectedAnswerIds = setOf("single_a"),
            isCorrect = true,
        )
    }

    @Test
    fun incorrectSingleAnswerIsScoredIncorrect() = runEngineTest {
        val session = sessionWith(question("single", correctAnswerIds = listOf("single_a")))

        val updated = engine().submitAnswer(session, "single", listOf("single_b"))

        assertAnswered(
            session = updated,
            questionId = "single",
            selectedAnswerIds = setOf("single_b"),
            isCorrect = false,
        )
    }

    @Test
    fun extraAnswerForSingleAnswerQuestionIsIncorrect() = runEngineTest {
        val session = sessionWith(question("single", correctAnswerIds = listOf("single_a")))

        val updated = engine().submitAnswer(session, "single", listOf("single_a", "single_b"))

        assertAnswered(
            session = updated,
            questionId = "single",
            selectedAnswerIds = setOf("single_a", "single_b"),
            isCorrect = false,
        )
    }

    @Test
    fun multipleCorrectAnswersRequireExactSet() = runEngineTest {
        val session = sessionWith(question("multi", correctAnswerIds = listOf("multi_a", "multi_c")))

        val updated = engine().submitAnswer(session, "multi", listOf("multi_a", "multi_c"))

        assertAnswered(
            session = updated,
            questionId = "multi",
            selectedAnswerIds = setOf("multi_a", "multi_c"),
            isCorrect = true,
        )
    }

    @Test
    fun multipleAnswerSubmissionOrderDoesNotMatter() = runEngineTest {
        val session = sessionWith(question("multi", correctAnswerIds = listOf("multi_a", "multi_c")))

        val updated = engine().submitAnswer(session, "multi", listOf("multi_c", "multi_a"))

        assertAnswered(
            session = updated,
            questionId = "multi",
            selectedAnswerIds = setOf("multi_a", "multi_c"),
            isCorrect = true,
        )
    }

    @Test
    fun missingOneCorrectAnswerIsIncorrect() = runEngineTest {
        val session = sessionWith(question("multi", correctAnswerIds = listOf("multi_a", "multi_c")))

        val updated = engine().submitAnswer(session, "multi", listOf("multi_a"))

        assertAnswered(
            session = updated,
            questionId = "multi",
            selectedAnswerIds = setOf("multi_a"),
            isCorrect = false,
        )
    }

    @Test
    fun incorrectAdditionalOptionIsIncorrect() = runEngineTest {
        val session = sessionWith(question("multi", correctAnswerIds = listOf("multi_a", "multi_c")))

        val updated = engine().submitAnswer(session, "multi", listOf("multi_a", "multi_b", "multi_c"))

        assertAnswered(
            session = updated,
            questionId = "multi",
            selectedAnswerIds = setOf("multi_a", "multi_b", "multi_c"),
            isCorrect = false,
        )
    }

    @Test
    fun duplicateSubmittedAnswerIdsAreNormalizedBeforeScoring() = runEngineTest {
        val session = sessionWith(question("multi", correctAnswerIds = listOf("multi_a", "multi_c")))

        val updated = engine().submitAnswer(session, "multi", listOf("multi_a", "multi_a", "multi_c"))

        assertAnswered(
            session = updated,
            questionId = "multi",
            selectedAnswerIds = setOf("multi_a", "multi_c"),
            isCorrect = true,
        )
    }

    @Test
    fun unknownAnswerIdIsRejectedAndOriginalSessionRemainsUnchanged() = runEngineTest {
        val session = sessionWith(question("single"))

        assertFailsWith<IllegalArgumentException> {
            engine().submitAnswer(session, "single", listOf("single_a", "unknown_answer"))
        }

        assertEquals(QuestionAnswerState.Unanswered, session.attempt.questionAttempts.single().answerState)
    }

    @Test
    fun emptyAnswerSelectionIsRejected() = runEngineTest {
        val session = sessionWith(question("single"))

        assertFailsWith<IllegalArgumentException> {
            engine().submitAnswer(session, "single", emptyList())
        }
    }

    @Test
    fun unknownQuestionIdIsRejected() = runEngineTest {
        val session = sessionWith(question("single"))

        assertFailsWith<IllegalArgumentException> {
            engine().submitAnswer(session, "missing_question", listOf("single_a"))
        }
    }

    @Test
    fun blankQuestionIdIsRejected() = runEngineTest {
        val session = sessionWith(question("single"))

        assertFailsWith<IllegalArgumentException> {
            engine().submitAnswer(session, " ", listOf("single_a"))
        }
    }

    @Test
    fun resubmissionIsRejectedAndOriginalAnsweredStateIsPreserved() = runEngineTest {
        val session = sessionWith(question("single", correctAnswerIds = listOf("single_a")))
        val answered = engine().submitAnswer(session, "single", listOf("single_a"))

        assertFailsWith<IllegalStateException> {
            engine().submitAnswer(answered, "single", listOf("single_b"))
        }

        assertAnswered(
            session = answered,
            questionId = "single",
            selectedAnswerIds = setOf("single_a"),
            isCorrect = true,
        )
    }

    @Test
    fun submittingOneAnswerLeavesOtherQuestionsUnchangedAndOrderStable() = runEngineTest {
        val session = sessionWith(
            question("question_a"),
            question("question_b"),
        )

        val updated = engine().submitAnswer(session, "question_b", listOf("question_b_a"))

        assertEquals(listOf("question_a", "question_b"), updated.attempt.questionAttempts.map { it.questionId })
        assertEquals(QuestionAnswerState.Unanswered, updated.attempt.questionAttempts[0].answerState)
        assertIs<QuestionAnswerState.Answered>(updated.attempt.questionAttempts[1].answerState)
    }

    @Test
    fun cannotCompleteWhenAnyQuestionIsUnanswered() = runEngineTest {
        val session = sessionWith(
            question("question_a"),
            question("question_b"),
        )
        val partiallyAnswered = engine().submitAnswer(session, "question_a", listOf("question_a_a"))

        assertFalse(engine().canComplete(partiallyAnswered))
        assertFailsWith<IllegalStateException> {
            engine().complete(partiallyAnswered)
        }
    }

    @Test
    fun allAnsweredInProgressSessionIsReadyButNotAutomaticallyCompleted() = runEngineTest {
        val session = answerAll(
            sessionWith(
                question("question_a"),
                question("question_b"),
            ),
        )

        assertTrue(engine().canComplete(session))
        assertEquals(AssessmentStatus.IN_PROGRESS, session.attempt.status)
        assertNull(session.attempt.score)
    }

    @Test
    fun completeAssessmentCalculatesScoreFromAnsweredQuestionAttempts() = runEngineTest {
        val session = sessionWith(
            question("question_a", correctAnswerIds = listOf("question_a_a")),
            question("question_b", correctAnswerIds = listOf("question_b_a")),
            question("question_c", correctAnswerIds = listOf("question_c_a")),
        ).let {
            engine().submitAnswer(it, "question_a", listOf("question_a_a"))
        }.let {
            engine().submitAnswer(it, "question_b", listOf("question_b_b"))
        }.let {
            engine().submitAnswer(it, "question_c", listOf("question_c_a"))
        }

        val completed = engine().complete(session)

        assertEquals(AssessmentStatus.COMPLETED, completed.attempt.status)
        assertEquals(StartedAt, completed.attempt.completedAt)
        assertEquals(3, completed.attempt.score?.totalQuestions)
        assertEquals(2, completed.attempt.score?.correctAnswers)
        assertTrue(abs(completed.attempt.score!!.percentage - 66.6666) < 0.001)
        assertEquals(session.attempt.questionAttempts, completed.attempt.questionAttempts)
        assertFalse(engine().canComplete(completed))
    }

    @Test
    fun completionUsesActualSelectedQuestionCountNotRequestedMaximum() = runEngineTest {
        val session = answerAll(
            sessionWith(
                question("question_a"),
                question("question_b"),
                config = AssessmentConfig.Mixed(questionCount = 10),
            ),
        )

        val completed = engine().complete(session)

        assertEquals(10, completed.attempt.config.questionCount)
        assertEquals(2, completed.attempt.score?.totalQuestions)
    }

    @Test
    fun completeTwiceFails() = runEngineTest {
        val completed = engine().complete(
            answerAll(sessionWith(question("question_a"))),
        )

        assertFailsWith<IllegalStateException> {
            engine().complete(completed)
        }
    }

    @Test
    fun completionRejectsClockGoingBeforeStartedAt() = runEngineTest {
        val clockThatMovesBack =
            engine(now = { Instant.fromEpochMilliseconds(StartedAt.toEpochMilliseconds() - 1_000) })

        assertFailsWith<IllegalArgumentException> {
            clockThatMovesBack.complete(
                answerAll(sessionWith(question("question_a"))),
            )
        }
    }

    @Test
    fun submitAfterCompletionFails() = runEngineTest {
        val completed = engine().complete(
            answerAll(sessionWith(question("question_a"))),
        )

        assertFailsWith<IllegalStateException> {
            engine().submitAnswer(completed, "question_a", listOf("question_a_a"))
        }
    }

    @Test
    fun sessionRequiresQuestionAndAttemptOrderAlignment() {
        val questions = listOf(
            question("question_a"),
            question("question_b"),
        )

        assertFailsWith<IllegalArgumentException> {
            AssessmentSession(
                attempt = TestAttempt(
                    id = "attempt-1",
                    config = AssessmentConfig.Mixed(questionCount = 2),
                    questionAttempts = listOf(
                        QuestionAttempt("question_b"),
                        QuestionAttempt("question_a"),
                    ),
                    status = AssessmentStatus.IN_PROGRESS,
                    startedAt = StartedAt,
                ),
                questions = questions,
            )
        }
    }

    @Test
    fun sessionRequiresQuestionsToBeNonemptyAndUnique() {
        assertFailsWith<IllegalArgumentException> {
            AssessmentSession(
                attempt = TestAttempt(
                    id = "attempt-1",
                    config = AssessmentConfig.Mixed(questionCount = 1),
                    questionAttempts = listOf(QuestionAttempt("question_a")),
                    status = AssessmentStatus.IN_PROGRESS,
                    startedAt = StartedAt,
                ),
                questions = emptyList(),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            AssessmentSession(
                attempt = TestAttempt(
                    id = "attempt-1",
                    config = AssessmentConfig.Mixed(questionCount = 2),
                    questionAttempts = listOf(
                        QuestionAttempt("question_a"),
                        QuestionAttempt("question_b"),
                    ),
                    status = AssessmentStatus.IN_PROGRESS,
                    startedAt = StartedAt,
                ),
                questions = listOf(
                    question("question_a"),
                    question("question_a"),
                ),
            )
        }
    }

    @Test
    fun submittingAndCompletingDoesNotMutateCurriculumQuestions() = runEngineTest {
        val questionA = question("question_a")
        val questionB = question("question_b")
        val session = sessionWith(questionA, questionB)
        val originalQuestions = session.questions

        val completed = engine().complete(answerAll(session))

        assertEquals(originalQuestions, session.questions)
        assertEquals(originalQuestions, completed.questions)
        assertEquals(questionA, originalQuestions[0])
        assertEquals(questionB, originalQuestions[1])
    }

    private fun runEngineTest(
        block: suspend EngineTestScope.() -> Unit,
    ) {
        var outcome: Result<Unit>? = null
        block.startCoroutine(
            receiver = EngineTestScope(),
            completion = object : Continuation<Unit> {
                override val context = EmptyCoroutineContext

                override fun resumeWith(result: Result<Unit>) {
                    outcome = result
                }
            },
        )
        outcome?.getOrThrow()
            ?: error("Engine test did not complete synchronously.")
    }

    private class EngineTestScope {
        val repository = FakeCurriculumRepository()
        private var nextAttemptNumber = 1

        fun engine(
            now: () -> Instant = { StartedAt },
        ): AssessmentEngine =
            AssessmentEngine(
                questionSelector = AssessmentQuestionSelector(
                    curriculumRepository = repository,
                    randomize = { it },
                ),
                generateAttemptId = { "attempt-${nextAttemptNumber++}" },
                now = now,
            )

        fun sessionWith(
            vararg questions: Question,
            config: AssessmentConfig = AssessmentConfig.Mixed(questionCount = questions.size),
        ): AssessmentSession {
            val questionList = questions.toList()
            return AssessmentSession(
                attempt = TestAttempt(
                    id = "attempt-1",
                    config = config,
                    questionAttempts = questionList.map {
                        QuestionAttempt(questionId = it.id)
                    },
                    status = AssessmentStatus.IN_PROGRESS,
                    startedAt = StartedAt,
                ),
                questions = questionList,
            )
        }

        fun answerAll(session: AssessmentSession): AssessmentSession {
            var updated = session
            session.questions.forEach { question ->
                updated = engine().submitAnswer(
                    session = updated,
                    questionId = question.id,
                    selectedAnswerIds = question.correctAnswerIds,
                )
            }
            return updated
        }
    }

    private class FakeCurriculumRepository : CurriculumRepository {
        var activeQuestions: List<Question> = emptyList()
        var topicQuestions: Map<String, List<Question>> = emptyMap()
        var subtopicQuestions: Map<String, List<Question>> = emptyMap()

        override suspend fun getActiveTopics(): List<Topic> =
            error("Not used by AssessmentEngine.")

        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> =
            error("Not used by AssessmentEngine.")

        override suspend fun getActiveQuestions(): List<Question> =
            activeQuestions

        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
            topicQuestions[topicId].orEmpty()

        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
            subtopicQuestions[subtopicId].orEmpty()

        override suspend fun getTopicById(topicId: String): Topic? =
            error("Not used by AssessmentEngine.")

        override suspend fun getQuestionById(questionId: String): Question? =
            error("Not used by AssessmentEngine.")
    }

    private fun assertStarted(result: AssessmentStartResult): AssessmentSession =
        assertIs<AssessmentStartResult.Started>(result).session

    private fun assertAnswered(
        session: AssessmentSession,
        questionId: String,
        selectedAnswerIds: Set<String>,
        isCorrect: Boolean,
    ) {
        val answerState = session.attempt.questionAttempts
            .single { it.questionId == questionId }
            .answerState
        assertIs<QuestionAnswerState.Answered>(answerState)
        assertEquals(selectedAnswerIds, answerState.selectedAnswerIds)
        assertEquals(isCorrect, answerState.isCorrect)
    }

    private fun question(
        id: String,
        correctAnswerIds: List<String> = listOf("${id}_a"),
    ): Question =
        Question(
            id = id,
            topicId = "${id}_topic",
            subtopicId = "${id}_subtopic",
            text = "$id?",
            answers = listOf(
                AnswerOption("${id}_a", "Answer A"),
                AnswerOption("${id}_b", "Answer B"),
                AnswerOption("${id}_c", "Answer C"),
            ),
            correctAnswerIds = correctAnswerIds,
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

private val StartedAt = Instant.fromEpochMilliseconds(1_700_000_000_000)
