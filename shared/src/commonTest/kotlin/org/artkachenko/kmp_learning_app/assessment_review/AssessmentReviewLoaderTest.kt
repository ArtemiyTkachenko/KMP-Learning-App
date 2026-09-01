package org.artkachenko.kmp_learning_app.assessment_review

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

internal class AssessmentReviewLoaderTest {
    @Test
    fun mapsHistoricalReviewInAttemptOrder() = runTest {
        val questions = listOf(
            question("q1", topicId = "compose"),
            question("q2", topicId = "kotlin"),
            question("q3", topicId = "coroutines"),
        )
        val attempt = completedAttempt(
            listOf(
                answered("q3", selectedIds = setOf("b"), isCorrect = false),
                answered("q1", selectedIds = setOf("a", "b"), isCorrect = false),
                answered("q2", selectedIds = setOf("a", "c"), isCorrect = true),
            ),
            correctAnswers = 1,
        )

        val items = AssessmentReviewLoader(FakeCurriculumRepository(questions))
            .loadQuestions(attempt)

        assertEquals(listOf("q3", "q1", "q2"), items.map { available(it).questionId })
        val first = available(items.first())
        assertEquals("coroutines", first.topicId)
        assertFalse(first.isCorrect)
        // Every authored answer is present. Their order is derived from the attempt id rather
        // than the authored order, so it is asserted as a set here and pinned in AnswerOrderTest.
        assertEquals(setOf("a", "b", "c"), first.answers.map { it.id }.toSet())
        assertTrue(first.answers.first { it.id == "b" }.wasSelected)
        assertFalse(first.answers.first { it.id == "a" }.wasSelected)
        assertTrue(first.answers.first { it.id == "a" }.isCorrectAnswer)
        assertTrue(first.answers.first { it.id == "c" }.isCorrectAnswer)
        assertEquals("Explanation q3", first.explanation)
        assertEquals(listOf("Source B", "Source A"), first.sources.map { it.title })
    }

    @Test
    fun deprecatedQuestionRemainsAvailable() = runTest {
        val question = question("deprecated", status = ContentStatus.DEPRECATED)
        val items = AssessmentReviewLoader(FakeCurriculumRepository(listOf(question)))
            .loadQuestions(completedAttempt(listOf(answered("deprecated")), correctAnswers = 1))

        assertEquals("deprecated", available(items.single()).questionId)
    }

    @Test
    fun missingQuestionIsExplicitAndDoesNotStopSubsequentReview() = runTest {
        val items = AssessmentReviewLoader(FakeCurriculumRepository(listOf(question("available"))))
            .loadQuestions(
                completedAttempt(
                    listOf(answered("missing"), answered("available")),
                    correctAnswers = 1,
                ),
            )

        assertEquals("missing", assertIs<ReviewQuestionItem.Missing>(items.first()).questionId)
        assertEquals("available", available(items[1]).questionId)
    }

    @Test
    fun singleOccurrenceMapsSelectedAnswersAndPersistedCorrectness() = runTest {
        val loader = AssessmentReviewLoader(FakeCurriculumRepository(listOf(question("q1"))))

        val item = loader.loadQuestion("attempt_1", answered("q1", selectedIds = setOf("b"), isCorrect = false))

        val question = available(item)
        assertEquals("q1", question.questionId)
        assertEquals("Question q1", question.text)
        assertFalse(question.isCorrect)
        assertTrue(question.answers.first { it.id == "b" }.wasSelected)
        assertFalse(question.answers.first { it.id == "a" }.wasSelected)
        assertTrue(question.answers.first { it.id == "a" }.isCorrectAnswer)
    }

    @Test
    fun singleOccurrencePreservesPersistedCorrectnessWhenItDisagreesWithAuthoredAnswers() = runTest {
        val loader = AssessmentReviewLoader(FakeCurriculumRepository(listOf(question("q1"))))

        // Selected the currently-correct answers but was persisted as incorrect: history wins.
        val item = loader.loadQuestion("attempt_1", answered("q1", selectedIds = setOf("a", "c"), isCorrect = false))

        assertFalse(available(item).isCorrect)
    }

    @Test
    fun singleOccurrenceOfAnUnknownQuestionIsMissing() = runTest {
        val loader = AssessmentReviewLoader(FakeCurriculumRepository(emptyList()))

        val item = loader.loadQuestion("attempt_1", answered("gone"))

        assertEquals(ReviewQuestionItem.Missing("gone"), item)
    }

    private fun available(item: ReviewQuestionItem): ReviewQuestionUiModel =
        assertIs<ReviewQuestionItem.Available>(item).question

    private fun answered(
        questionId: String,
        selectedIds: Set<String> = setOf("a", "c"),
        isCorrect: Boolean = true,
    ) = QuestionAttempt(
        questionId,
        QuestionAnswerState.Answered(selectedIds, isCorrect),
    )

    private fun completedAttempt(
        questionAttempts: List<QuestionAttempt>,
        correctAnswers: Int,
    ) = TestAttempt(
        id = "attempt",
        config = AssessmentConfig.Mixed(questionAttempts.size),
        questionAttempts = questionAttempts,
        status = AssessmentStatus.COMPLETED,
        startedAt = Instant.fromEpochMilliseconds(1),
        completedAt = Instant.fromEpochMilliseconds(2),
        score = AssessmentScore(questionAttempts.size, correctAnswers),
    )

    private fun question(
        id: String,
        topicId: String = "topic",
        status: ContentStatus = ContentStatus.ACTIVE,
    ) = Question(
        id = id,
        topicId = topicId,
        subtopicId = "subtopic",
        text = "Question $id",
        answers = listOf(
            AnswerOption("a", "Answer A"),
            AnswerOption("b", "Answer B"),
            AnswerOption("c", "Answer C"),
        ),
        selectionMode = AnswerSelectionMode.MULTIPLE,
        level = QuestionLevel.FOUNDATION,
        correctAnswerIds = listOf("a", "c"),
        explanation = "Explanation $id",
        sources = listOf(
            SourceReference("Source B", "https://example.com/b"),
            SourceReference("Source A", "https://example.com/a"),
        ),
        status = status,
    )

    private class FakeCurriculumRepository(
        private val questions: List<Question>,
    ) : CurriculumRepository {
        override suspend fun getActiveTopics(): List<Topic> = error("Not used")
        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> = error("Not used")
        override suspend fun getActiveQuestions(): List<Question> = error("Not used")
        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> = error("Not used")
        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> = error("Not used")
        override suspend fun getTopicById(topicId: String): Topic? = error("Not used")
        override suspend fun getSubtopicById(subtopicId: String): Subtopic? = null
        override suspend fun getQuestionById(questionId: String): Question? =
            questions.firstOrNull { it.id == questionId }
    }
}
