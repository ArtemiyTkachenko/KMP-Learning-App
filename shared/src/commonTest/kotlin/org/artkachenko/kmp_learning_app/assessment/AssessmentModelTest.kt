package org.artkachenko.kmp_learning_app.assessment

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class AssessmentModelTest {
    @Test
    fun focusedTopicConfigIsRepresentable() {
        val config = AssessmentConfig.Focused(
            scope = AssessmentScope.Topic("android_ui"),
            questionCount = 10,
        )

        assertIs<AssessmentScope.Topic>(config.scope)
        assertEquals(10, config.questionCount)
    }

    @Test
    fun focusedSubtopicConfigIsRepresentable() {
        val config = AssessmentConfig.Focused(
            scope = AssessmentScope.Subtopic("compose_state"),
            questionCount = 5,
        )

        assertIs<AssessmentScope.Subtopic>(config.scope)
        assertEquals(5, config.questionCount)
    }

    @Test
    fun mixedConfigIsRepresentable() {
        val config = AssessmentConfig.Mixed(questionCount = 20)

        assertEquals(20, config.questionCount)
    }

    @Test
    fun questionCountMustBePositive() {
        AssessmentConfig.Mixed(questionCount = 1)

        assertFailsWith<IllegalArgumentException> {
            AssessmentConfig.Mixed(questionCount = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            AssessmentConfig.Focused(
                scope = AssessmentScope.Topic("android_ui"),
                questionCount = -1,
            )
        }
    }

    @Test
    fun focusedScopeIdsMustNotBeBlank() {
        assertFailsWith<IllegalArgumentException> {
            AssessmentScope.Topic(" ")
        }
        assertFailsWith<IllegalArgumentException> {
            AssessmentScope.Subtopic("")
        }
    }

    @Test
    fun unansweredStateIsRepresentable() {
        assertEquals(QuestionAnswerState.Unanswered, QuestionAnswerState.Unanswered)
    }

    @Test
    fun answeredStateSupportsOneAnswerId() {
        val state = QuestionAnswerState.Answered(
            selectedAnswerIds = setOf("answer_a"),
            isCorrect = true,
        )

        assertEquals(setOf("answer_a"), state.selectedAnswerIds)
        assertTrue(state.isCorrect)
    }

    @Test
    fun answeredStateSupportsMultipleAnswerIds() {
        val state = QuestionAnswerState.Answered(
            selectedAnswerIds = setOf("answer_a", "answer_c"),
            isCorrect = true,
        )

        assertEquals(setOf("answer_a", "answer_c"), state.selectedAnswerIds)
    }

    @Test
    fun duplicateAnswerSelectionsCollapseThroughSetSemantics() {
        val state = QuestionAnswerState.Answered(
            selectedAnswerIds = setOf("answer_a", "answer_a"),
            isCorrect = true,
        )

        assertEquals(setOf("answer_a"), state.selectedAnswerIds)
    }

    @Test
    fun answeredStateRequiresNonblankSelections() {
        assertFailsWith<IllegalArgumentException> {
            QuestionAnswerState.Answered(
                selectedAnswerIds = emptySet(),
                isCorrect = false,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            QuestionAnswerState.Answered(
                selectedAnswerIds = setOf("answer_a", " "),
                isCorrect = false,
            )
        }
    }

    @Test
    fun questionAttemptDefaultsToUnanswered() {
        val attempt = QuestionAttempt(questionId = "question_001")

        assertEquals("question_001", attempt.questionId)
        assertEquals(QuestionAnswerState.Unanswered, attempt.answerState)
    }

    @Test
    fun answeredQuestionAttemptIsRepresentable() {
        val answerState = QuestionAnswerState.Answered(
            selectedAnswerIds = setOf("answer_a"),
            isCorrect = false,
        )
        val attempt = QuestionAttempt(
            questionId = "question_001",
            answerState = answerState,
        )

        assertEquals(answerState, attempt.answerState)
    }

    @Test
    fun questionAttemptRequiresQuestionId() {
        assertFailsWith<IllegalArgumentException> {
            QuestionAttempt(questionId = "")
        }
    }

    @Test
    fun assessmentScoreCalculatesPercentage() {
        val score = AssessmentScore(
            totalQuestions = 4,
            correctAnswers = 3,
        )

        assertEquals(4, score.totalQuestions)
        assertEquals(3, score.correctAnswers)
        assertTrue(abs(score.percentage - 75.0) < 0.0001)
    }

    @Test
    fun assessmentScoreRequiresPossibleCounts() {
        assertFailsWith<IllegalArgumentException> {
            AssessmentScore(totalQuestions = 0, correctAnswers = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            AssessmentScore(totalQuestions = 3, correctAnswers = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            AssessmentScore(totalQuestions = 3, correctAnswers = 4)
        }
    }

    @Test
    fun validInProgressAttemptCanHaveUnansweredAndAnsweredQuestionsWithoutScore() {
        val attempt = TestAttempt(
            id = "attempt_001",
            config = AssessmentConfig.Mixed(questionCount = 2),
            questionAttempts = listOf(
                QuestionAttempt("question_001"),
                answeredAttempt("question_002"),
            ),
            status = AssessmentStatus.IN_PROGRESS,
        )

        assertEquals(AssessmentStatus.IN_PROGRESS, attempt.status)
        assertNull(attempt.score)
    }

    @Test
    fun testAttemptRequiresStableIdentityAndQuestions() {
        assertFailsWith<IllegalArgumentException> {
            TestAttempt(
                id = " ",
                config = AssessmentConfig.Mixed(questionCount = 1),
                questionAttempts = listOf(QuestionAttempt("question_001")),
                status = AssessmentStatus.IN_PROGRESS,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            TestAttempt(
                id = "attempt_001",
                config = AssessmentConfig.Mixed(questionCount = 1),
                questionAttempts = emptyList(),
                status = AssessmentStatus.IN_PROGRESS,
            )
        }
    }

    @Test
    fun testAttemptQuestionIdsMustBeUnique() {
        assertFailsWith<IllegalArgumentException> {
            TestAttempt(
                id = "attempt_001",
                config = AssessmentConfig.Mixed(questionCount = 2),
                questionAttempts = listOf(
                    QuestionAttempt("question_001"),
                    QuestionAttempt("question_001"),
                ),
                status = AssessmentStatus.IN_PROGRESS,
            )
        }
    }

    @Test
    fun inProgressAttemptMustNotHaveScore() {
        assertFailsWith<IllegalArgumentException> {
            TestAttempt(
                id = "attempt_001",
                config = AssessmentConfig.Mixed(questionCount = 1),
                questionAttempts = listOf(answeredAttempt("question_001")),
                status = AssessmentStatus.IN_PROGRESS,
                score = AssessmentScore(totalQuestions = 1, correctAnswers = 1),
            )
        }
    }

    @Test
    fun completedAttemptRequiresAnsweredQuestionsAndScore() {
        assertFailsWith<IllegalArgumentException> {
            TestAttempt(
                id = "attempt_001",
                config = AssessmentConfig.Mixed(questionCount = 1),
                questionAttempts = listOf(QuestionAttempt("question_001")),
                status = AssessmentStatus.COMPLETED,
                score = AssessmentScore(totalQuestions = 1, correctAnswers = 0),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            TestAttempt(
                id = "attempt_001",
                config = AssessmentConfig.Mixed(questionCount = 1),
                questionAttempts = listOf(answeredAttempt("question_001")),
                status = AssessmentStatus.COMPLETED,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            TestAttempt(
                id = "attempt_001",
                config = AssessmentConfig.Mixed(questionCount = 1),
                questionAttempts = listOf(answeredAttempt("question_001")),
                status = AssessmentStatus.COMPLETED,
                score = AssessmentScore(totalQuestions = 2, correctAnswers = 1),
            )
        }
    }

    @Test
    fun validCompletedAttemptIsRepresentable() {
        val attempt = TestAttempt(
            id = "attempt_001",
            config = AssessmentConfig.Mixed(questionCount = 2),
            questionAttempts = listOf(
                answeredAttempt("question_001"),
                answeredAttempt("question_002", isCorrect = false),
            ),
            status = AssessmentStatus.COMPLETED,
            score = AssessmentScore(totalQuestions = 2, correctAnswers = 1),
        )

        assertEquals(AssessmentStatus.COMPLETED, attempt.status)
        assertEquals(1, attempt.score?.correctAnswers)
    }

    @Test
    fun representativeModelsHaveValueEquality() {
        assertEquals(
            AssessmentConfig.Focused(AssessmentScope.Topic("android_ui"), questionCount = 5),
            AssessmentConfig.Focused(AssessmentScope.Topic("android_ui"), questionCount = 5),
        )
        assertEquals(
            QuestionAttempt(
                questionId = "question_001",
                answerState = QuestionAnswerState.Answered(setOf("answer_a"), isCorrect = true),
            ),
            QuestionAttempt(
                questionId = "question_001",
                answerState = QuestionAnswerState.Answered(setOf("answer_a"), isCorrect = true),
            ),
        )
        assertEquals(
            AssessmentScore(totalQuestions = 2, correctAnswers = 1),
            AssessmentScore(totalQuestions = 2, correctAnswers = 1),
        )
        assertEquals(
            completedAttempt(),
            completedAttempt(),
        )
    }

    private fun answeredAttempt(
        questionId: String,
        isCorrect: Boolean = true,
    ): QuestionAttempt =
        QuestionAttempt(
            questionId = questionId,
            answerState = QuestionAnswerState.Answered(
                selectedAnswerIds = setOf("${questionId}_answer_a"),
                isCorrect = isCorrect,
            ),
        )

    private fun completedAttempt(): TestAttempt =
        TestAttempt(
            id = "attempt_001",
            config = AssessmentConfig.Focused(
                scope = AssessmentScope.Subtopic("compose_state"),
                questionCount = 1,
            ),
            questionAttempts = listOf(answeredAttempt("question_001")),
            status = AssessmentStatus.COMPLETED,
            score = AssessmentScore(totalQuestions = 1, correctAnswers = 1),
        )
}
