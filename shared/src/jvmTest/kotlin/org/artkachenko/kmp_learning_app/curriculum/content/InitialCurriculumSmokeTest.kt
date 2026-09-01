package org.artkachenko.kmp_learning_app.curriculum.content

import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
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
    fun bundledQuestionsHaveReviewedE1503LevelDistribution() = runTest {
        val initialCurriculum = BundledCurriculumSource.load()

        assertEquals(
            LevelDistribution(foundation = 238, applied = 147, advanced = 14),
            initialCurriculum.questions.levelDistribution(),
        )
        assertEquals(
            LevelDistribution(foundation = 205, applied = 141, advanced = 14),
            initialCurriculum.questions
                .filter { it.status == ContentStatus.ACTIVE }
                .levelDistribution(),
        )
        assertEquals(
            LevelDistribution(foundation = 33, applied = 6, advanced = 0),
            initialCurriculum.questions
                .filter { it.status == ContentStatus.DEPRECATED }
                .levelDistribution(),
        )
        assertEquals(
            mapOf(
                "android_platform" to LevelDistribution(13, 4, 0),
                "lifecycle_navigation" to LevelDistribution(19, 8, 0),
                "android_ui" to LevelDistribution(23, 8, 0),
                "kotlin_language" to LevelDistribution(25, 2, 0),
                "async_reactive" to LevelDistribution(24, 15, 4),
                "architecture" to LevelDistribution(11, 15, 0),
                "dependency_injection" to LevelDistribution(18, 9, 0),
                "local_data" to LevelDistribution(17, 4, 1),
                "networking" to LevelDistribution(12, 11, 1),
                "background_work" to LevelDistribution(14, 5, 1),
                "notifications" to LevelDistribution(7, 7, 0),
                "testing" to LevelDistribution(8, 16, 0),
                "performance" to LevelDistribution(14, 10, 0),
                "security" to LevelDistribution(9, 6, 3),
                "build_delivery" to LevelDistribution(12, 7, 0),
                "mobile_system_design" to LevelDistribution(1, 13, 4),
                "kmp" to LevelDistribution(11, 7, 0),
            ),
            initialCurriculum.questions
                .groupBy(Question::topicId)
                .mapValues { (_, questions) -> questions.levelDistribution() },
        )
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

private data class LevelDistribution(
    val foundation: Int,
    val applied: Int,
    val advanced: Int,
)

private fun List<Question>.levelDistribution() = LevelDistribution(
    foundation = count { it.level == QuestionLevel.FOUNDATION },
    applied = count { it.level == QuestionLevel.APPLIED },
    advanced = count { it.level == QuestionLevel.ADVANCED },
)
