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
        assertEquals(180, initialCurriculum.questions.size)
    }

    @Test
    fun bundledInitialQuestionDistributionMatchesE0604Targets() = runTest {
        val initialCurriculum = BundledCurriculumSource.load()
        val countsByTopic = initialCurriculum.questions
            .groupingBy { it.topicId }
            .eachCount()

        assertEquals(
            mapOf(
                "android_platform" to 11,
                "lifecycle_navigation" to 13,
                "android_ui" to 18,
                "kotlin_language" to 16,
                "async_reactive" to 16,
                "architecture" to 12,
                "dependency_injection" to 8,
                "local_data" to 11,
                "networking" to 11,
                "background_work" to 8,
                "notifications" to 6,
                "testing" to 11,
                "performance" to 9,
                "security" to 7,
                "build_delivery" to 7,
                "mobile_system_design" to 8,
                "kmp" to 8,
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
