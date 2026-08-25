package org.artkachenko.kmp_learning_app.curriculum.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class InitialCurriculumSmokeTest {
    @Test
    fun initialCurriculumHasExpectedTopicTaxonomyAndQuestionCount() {
        assertEquals(17, initialCurriculum.topics.size)
        assertEquals(361, initialCurriculum.subtopics.size)
        assertEquals(90, initialCurriculum.questions.size)
    }

    @Test
    fun initialQuestionDistributionMatchesE0604Targets() {
        val countsByTopic = initialCurriculum.questions
            .groupingBy { it.topicId }
            .eachCount()

        assertEquals(
            mapOf(
                "android_platform" to 6,
                "lifecycle_navigation" to 7,
                "android_ui" to 10,
                "kotlin_language" to 10,
                "async_reactive" to 10,
                "architecture" to 7,
                "dependency_injection" to 4,
                "local_data" to 5,
                "networking" to 5,
                "background_work" to 4,
                "notifications" to 3,
                "testing" to 5,
                "performance" to 4,
                "security" to 3,
                "build_delivery" to 3,
                "mobile_system_design" to 2,
                "kmp" to 2,
            ),
            countsByTopic,
        )
    }

    @Test
    fun initialQuestionsUseKnownCurriculumLocationsAndContentShape() {
        val topicIds = initialCurriculum.topics.map { it.id }.toSet()
        val subtopicsById = initialCurriculum.subtopics.associateBy { it.id }
        val questionIds = initialCurriculum.questions.map { it.id }

        assertEquals(questionIds.size, questionIds.toSet().size)

        initialCurriculum.questions.forEach { question ->
            assertTrue(question.topicId in topicIds, "Unknown topicId: ${question.topicId}")
            assertEquals(question.topicId, subtopicsById.getValue(question.subtopicId).topicId)
            assertTrue(question.answers.size >= 2, "Not enough answers: ${question.id}")
            assertTrue(question.correctAnswerIds.isNotEmpty(), "No correct answer: ${question.id}")
            assertTrue(question.explanation.isNotBlank(), "Blank explanation: ${question.id}")
            assertTrue(question.sources.isNotEmpty(), "No source: ${question.id}")
            assertTrue(question.sources.all { it.title.isNotBlank() && it.url.isNotBlank() })

            val answerIds = question.answers.map { it.id }
            assertEquals(answerIds.size, answerIds.toSet().size, "Duplicate answer ID: ${question.id}")
            assertTrue(
                question.correctAnswerIds.all { it in answerIds },
                "Correct answer does not reference an answer option: ${question.id}",
            )
        }
    }

    @Test
    fun multipleCorrectAnswerQuestionsTellReaderToSelectAllThatApply() {
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

