package org.artkachenko.kmp_learning_app.assessment.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference

internal class AnswerOrderTest {
    @Test
    fun theSameAttemptAlwaysOrdersAQuestionTheSameWay() {
        val question = question("q1")

        val orders = List(20) { question.withAnswersOrderedFor("attempt_1").answerIds() }

        assertEquals(1, orders.toSet().size, "order must not change between calls: $orders")
    }

    @Test
    fun differentAttemptsOrderTheSameQuestionDifferently() {
        val question = question("q1")

        // Any single pair could coincide by chance, so this asserts the property across a range:
        // a fixed order would collapse to one distinct arrangement.
        val orders = List(25) { index -> question.withAnswersOrderedFor("attempt_$index").answerIds() }

        assertTrue(orders.toSet().size > 1, "every attempt produced the same order: ${orders.first()}")
    }

    @Test
    fun differentQuestionsInOneAttemptAreOrderedIndependently() {
        val orders = List(25) { index ->
            question("q$index").withAnswersOrderedFor("attempt_1").answerIds()
        }

        assertTrue(orders.toSet().size > 1, "every question produced the same order: ${orders.first()}")
    }

    @Test
    fun everyAnswerSurvivesReordering() {
        val question = question("q1")

        repeat(25) { index ->
            val reordered = question.withAnswersOrderedFor("attempt_$index")

            assertEquals(question.answers.toSet(), reordered.answers.toSet())
            assertEquals(question.answers.size, reordered.answers.size)
        }
    }

    @Test
    fun reorderingChangesNothingElseAboutTheQuestion() {
        val question = question("q1")

        val reordered = question.withAnswersOrderedFor("attempt_1")

        // Scoring reads correctAnswerIds and the engine validates against answer ids, so both have
        // to come through untouched for an attempt taken before this existed to stay correct.
        assertEquals(question.correctAnswerIds, reordered.correctAnswerIds)
        assertEquals(question.copy(answers = emptyList()), reordered.copy(answers = emptyList()))
    }

    private fun question(id: String) = Question(
        id = id,
        topicId = "topic",
        subtopicId = "subtopic",
        text = "Question $id?",
        answers = listOf(
            AnswerOption("${id}_a", "Answer A"),
            AnswerOption("${id}_b", "Answer B"),
            AnswerOption("${id}_c", "Answer C"),
            AnswerOption("${id}_d", "Answer D"),
        ),
        selectionMode = AnswerSelectionMode.SINGLE,
        level = QuestionLevel.FOUNDATION,
        correctAnswerIds = listOf("${id}_a"),
        explanation = "Explanation $id",
        sources = listOf(SourceReference("Source", "https://example.com/$id")),
    )

    private fun Question.answerIds(): List<String> = answers.map { it.id }
}
