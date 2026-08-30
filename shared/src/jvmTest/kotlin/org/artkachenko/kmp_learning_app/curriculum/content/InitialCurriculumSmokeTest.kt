package org.artkachenko.kmp_learning_app.curriculum.content

import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.curriculum.validation.CurriculumValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class InitialCurriculumSmokeTest {
    @Test
    fun bundledInitialCurriculumHasExpectedTopicTaxonomyAndQuestionCount() = runTest {
        val initialCurriculum = BundledCurriculumSource.load()

        assertEquals(17, initialCurriculum.topics.size)
        assertEquals(361, initialCurriculum.subtopics.size)
        assertEquals(270, initialCurriculum.questions.size)
    }

    @Test
    fun bundledInitialQuestionDistributionMatchesCurrentTargets() = runTest {
        val initialCurriculum = BundledCurriculumSource.load()
        val countsByTopic = initialCurriculum.questions
            .groupingBy { it.topicId }
            .eachCount()

        assertEquals(
            mapOf(
                "android_platform" to 15,
                "lifecycle_navigation" to 20,
                "android_ui" to 25,
                "kotlin_language" to 22,
                "async_reactive" to 28,
                "architecture" to 18,
                "dependency_injection" to 16,
                "local_data" to 15,
                "networking" to 15,
                "background_work" to 11,
                "notifications" to 8,
                "testing" to 16,
                "performance" to 15,
                "security" to 10,
                "build_delivery" to 13,
                "mobile_system_design" to 12,
                "kmp" to 11,
            ),
            countsByTopic,
        )
    }

    @Test
    fun bundledInitialCurriculumPassesStructuralValidation() = runTest {
        val initialCurriculum = BundledCurriculumSource.load()

        assertTrue(CurriculumValidator().validate(initialCurriculum).isEmpty())
    }

    @Test
    fun bundledInitialQuestionsPreserveE0604ContentShape() = runTest {
        val initialCurriculum = BundledCurriculumSource.load()

        initialCurriculum.questions.forEach { question ->
            assertTrue(question.answers.size >= 2, "Not enough answers: ${question.id}")
            assertTrue(question.correctAnswerIds.isNotEmpty(), "No correct answer: ${question.id}")
            assertTrue(question.explanation.isNotBlank(), "Blank explanation: ${question.id}")
            assertTrue(question.sources.isNotEmpty(), "No source: ${question.id}")

            val answerIds = question.answers.map { it.id }
            assertEquals(answerIds.size, answerIds.toSet().size, "Duplicate answer ID: ${question.id}")
            assertTrue(
                question.correctAnswerIds.all { it in answerIds },
                "Correct answer does not reference an answer option: ${question.id}",
            )
        }
    }

    @Test
    fun multipleCorrectAnswerQuestionsTellReaderToSelectAllThatApply() = runTest {
        val initialCurriculum = BundledCurriculumSource.load()
        val multipleCorrectAnswerQuestions = initialCurriculum.questions
            .filter { it.correctAnswerIds.size > 1 }

        assertTrue(multipleCorrectAnswerQuestions.isNotEmpty())
        assertTrue(
            multipleCorrectAnswerQuestions.all {
                it.text.contains("Select all that apply.")
            },
        )
    }
}
