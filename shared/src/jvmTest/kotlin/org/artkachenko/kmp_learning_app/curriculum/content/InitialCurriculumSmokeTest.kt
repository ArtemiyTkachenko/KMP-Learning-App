package org.artkachenko.kmp_learning_app.curriculum.content

import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
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
        assertEquals(399, initialCurriculum.questions.size)
        assertEquals(
            360,
            initialCurriculum.questions.count { it.status == ContentStatus.ACTIVE },
        )
        assertEquals(
            39,
            initialCurriculum.questions.count { it.status == ContentStatus.DEPRECATED },
        )
        assertEquals(353, initialCurriculum.questions.count { it.selectionMode == AnswerSelectionMode.SINGLE })
        assertEquals(46, initialCurriculum.questions.count { it.selectionMode == AnswerSelectionMode.MULTIPLE })
    }

    @Test
    fun bundledInitialQuestionDistributionMatchesCurrentTargets() = runTest {
        val initialCurriculum = BundledCurriculumSource.load()
        val countsByTopic = initialCurriculum.questions
            .filter { it.status == ContentStatus.ACTIVE }
            .groupingBy { it.topicId }
            .eachCount()

        assertEquals(
            mapOf(
                "android_platform" to 16,
                "lifecycle_navigation" to 23,
                "android_ui" to 26,
                "kotlin_language" to 25,
                "async_reactive" to 38,
                "architecture" to 22,
                "dependency_injection" to 23,
                "local_data" to 21,
                "networking" to 24,
                "background_work" to 19,
                "notifications" to 12,
                "testing" to 22,
                "performance" to 22,
                "security" to 17,
                "build_delivery" to 17,
                "mobile_system_design" to 17,
                "kmp" to 16,
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
    fun authoredMultipleQuestionsTellReaderToSelectAllThatApply() = runTest {
        val initialCurriculum = BundledCurriculumSource.load()
        val multipleQuestions = initialCurriculum.questions
            .filter { it.selectionMode == AnswerSelectionMode.MULTIPLE }

        assertTrue(multipleQuestions.isNotEmpty())
        assertTrue(
            multipleQuestions.all {
                it.text.contains("Select all that apply.")
            },
        )
    }
}
