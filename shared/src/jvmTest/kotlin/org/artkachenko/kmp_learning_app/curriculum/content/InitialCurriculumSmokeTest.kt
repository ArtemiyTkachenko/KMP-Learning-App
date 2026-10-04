package org.artkachenko.kmp_learning_app.curriculum.content

import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.validation.CurriculumValidator
import org.artkachenko.kmp_learning_app.curriculum.validation.renderForDiagnostics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The shipped bank's shape, pinned in one place on purpose.
 *
 * The exact counts below are not consequences that happen to be frozen: they are the change
 * detector the authoring workflow relies on (`docs/content/question-bank-coverage.md`, "Pinned
 * count tests"), so a content batch has to restate the taxonomy, status and selection-mode split,
 * level distribution and per-Topic ACTIVE counts it intends. A total that is only the sum of two
 * figures already pinned here is not pinned again, and tests elsewhere that import or read the
 * bundle derive their expectations from the loaded document instead of repeating these.
 */
internal class InitialCurriculumSmokeTest {
    @Test
    fun bundledInitialCurriculumHasExpectedTopicTaxonomyAndQuestionCount() = runTest {
        val initialCurriculum = BundledCurriculumSource.load()

        assertEquals(17, initialCurriculum.topics.size)
        assertEquals(361, initialCurriculum.subtopics.size)
        // ACTIVE and DEPRECATED are the only statuses, so together they are the total.
        assertEquals(
            439,
            initialCurriculum.questions.count { it.status == ContentStatus.ACTIVE },
        )
        assertEquals(
            41,
            initialCurriculum.questions.count { it.status == ContentStatus.DEPRECATED },
        )
        assertEquals(433, initialCurriculum.questions.count { it.selectionMode == AnswerSelectionMode.SINGLE })
        assertEquals(47, initialCurriculum.questions.count { it.selectionMode == AnswerSelectionMode.MULTIPLE })
    }

    @Test
    fun bundledQuestionsHaveReviewedLevelDistribution() = runTest {
        val initialCurriculum = BundledCurriculumSource.load()

        // The all-status distribution is the sum of these two and is not pinned separately.
        assertEquals(
            LevelDistribution(foundation = 204, applied = 207, advanced = 28),
            initialCurriculum.questions
                .filter { it.status == ContentStatus.ACTIVE }
                .levelDistribution(),
        )
        assertEquals(
            LevelDistribution(foundation = 34, applied = 7, advanced = 0),
            initialCurriculum.questions
                .filter { it.status == ContentStatus.DEPRECATED }
                .levelDistribution(),
        )
        assertEquals(
            mapOf(
                "android_platform" to LevelDistribution(13, 4, 0),
                "lifecycle_navigation" to LevelDistribution(19, 8, 0),
                "android_ui" to LevelDistribution(24, 26, 5),
                "kotlin_language" to LevelDistribution(25, 2, 0),
                "async_reactive" to LevelDistribution(23, 29, 10),
                "architecture" to LevelDistribution(10, 32, 2),
                "dependency_injection" to LevelDistribution(18, 24, 0),
                "local_data" to LevelDistribution(16, 4, 1),
                "networking" to LevelDistribution(12, 10, 1),
                "background_work" to LevelDistribution(14, 5, 1),
                "notifications" to LevelDistribution(7, 7, 0),
                "testing" to LevelDistribution(8, 16, 0),
                "performance" to LevelDistribution(14, 10, 0),
                "security" to LevelDistribution(9, 6, 3),
                "build_delivery" to LevelDistribution(12, 7, 0),
                "mobile_system_design" to LevelDistribution(1, 13, 4),
                "kmp" to LevelDistribution(13, 11, 1),
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
                "android_ui" to 49,
                "kotlin_language" to 25,
                "async_reactive" to 57,
                "architecture" to 40,
                "dependency_injection" to 38,
                "local_data" to 20,
                "networking" to 23,
                "background_work" to 19,
                "notifications" to 12,
                "testing" to 21,
                "performance" to 22,
                "security" to 17,
                "build_delivery" to 17,
                "mobile_system_design" to 17,
                "kmp" to 23,
            ),
            countsByTopic,
        )
    }

    @Test
    fun bundledInitialCurriculumPassesStructuralValidation() = runTest {
        val initialCurriculum = BundledCurriculumSource.load()

        val errors = CurriculumValidator().validate(initialCurriculum)

        // The validator already names the rule and the entity for every defect; a bare isEmpty()
        // would reduce all of that to "Expected value to be true" in CI.
        assertTrue(
            errors.isEmpty(),
            "Bundled curriculum failed validation with ${errors.renderForDiagnostics()}",
        )
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
