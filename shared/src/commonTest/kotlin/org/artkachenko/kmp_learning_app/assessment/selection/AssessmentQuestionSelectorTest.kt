package org.artkachenko.kmp_learning_app.assessment.selection

import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AssessmentQuestionSelectorTest {
    @Test
    fun focusedTopicSelectionUsesTopicRepositoryPathAndTakesRequestedCount() = runSelectorTest {
        repository.topicQuestions = mapOf(
            "android_ui" to questions("ui_question_a", "ui_question_b", "ui_question_c"),
        )

        val selected = selector().select(
            AssessmentConfig.Focused(
                scope = AssessmentScope.Topic("android_ui"),
                questionCount = 2,
            ),
        )

        assertEquals(listOf("topic:android_ui"), repository.calls)
        assertEquals(listOf("ui_question_a", "ui_question_b"), selected.map { it.id })
        assertUniqueQuestionIds(selected)
    }

    @Test
    fun focusedSubtopicSelectionUsesSubtopicRepositoryPath() = runSelectorTest {
        repository.subtopicQuestions = mapOf(
            "compose_state" to questions("compose_state_001", "compose_state_002"),
        )

        val selected = selector().select(
            AssessmentConfig.Focused(
                scope = AssessmentScope.Subtopic("compose_state"),
                questionCount = 2,
            ),
        )

        assertEquals(listOf("subtopic:compose_state"), repository.calls)
        assertEquals(listOf("compose_state_001", "compose_state_002"), selected.map { it.id })
    }

    @Test
    fun mixedSelectionUsesAllActiveRepositoryPath() = runSelectorTest {
        repository.activeQuestions = questions(
            "android_question",
            "kotlin_question",
            "testing_question",
        )

        val selected = selector().select(
            AssessmentConfig.Mixed(questionCount = 2),
        )

        assertEquals(listOf("all"), repository.calls)
        assertEquals(listOf("android_question", "kotlin_question"), selected.map { it.id })
    }

    @Test
    fun mixedFirstRoundCoversTopicsBeforeRepeatingOne() = runSelectorTest {
        repository.activeQuestions = mixedRoundFixture()

        val selected = selector().select(AssessmentConfig.Mixed(questionCount = 3))

        assertEquals(listOf("A1", "B1", "C1"), selected.map { it.id })
    }

    @Test
    fun mixedSecondRoundContinuesInTopicEncounterOrder() = runSelectorTest {
        repository.activeQuestions = mixedRoundFixture()

        val selected = selector().select(AssessmentConfig.Mixed(questionCount = 5))

        assertEquals(listOf("A1", "B1", "C1", "A2", "B2"), selected.map { it.id })
    }

    @Test
    fun mixedOversizedRequestReturnsAllUniqueQuestionsInCoverageOrder() = runSelectorTest {
        repository.activeQuestions = mixedRoundFixture()

        val selected = selector().select(AssessmentConfig.Mixed(questionCount = 10))

        assertEquals(listOf("A1", "B1", "C1", "A2", "B2", "A3"), selected.map { it.id })
        assertUniqueQuestionIds(selected)
    }

    @Test
    fun mixedRequestSmallerThanTopicCountUsesDistinctTopics() = runSelectorTest {
        repository.activeQuestions = listOf(
            question("A1", topicId = "A"),
            question("B1", topicId = "B"),
            question("C1", topicId = "C"),
            question("D1", topicId = "D"),
        )

        val selected = selector().select(AssessmentConfig.Mixed(questionCount = 2))

        assertEquals(listOf("A1", "B1"), selected.map { it.id })
        assertEquals(2, selected.map { it.topicId }.toSet().size)
    }

    @Test
    fun mixedSelectionSkipsExhaustedTopicsAcrossLaterRounds() = runSelectorTest {
        repository.activeQuestions = listOf(
            question("A1", topicId = "A"),
            question("B1", topicId = "B"),
            question("B2", topicId = "B"),
            question("B3", topicId = "B"),
            question("C1", topicId = "C"),
            question("C2", topicId = "C"),
        )

        val selected = selector().select(AssessmentConfig.Mixed(questionCount = 6))

        assertEquals(listOf("A1", "B1", "C1", "B2", "C2", "B3"), selected.map { it.id })
    }

    @Test
    fun requestedCountEqualToPoolReturnsWholeUniquePool() = runSelectorTest {
        repository.activeQuestions = questions("question_a", "question_b", "question_c")

        val selected = selector().select(AssessmentConfig.Mixed(questionCount = 3))

        assertEquals(listOf("question_a", "question_b", "question_c"), selected.map { it.id })
    }

    @Test
    fun requestedCountBelowPoolReturnsRequestedCount() = runSelectorTest {
        repository.activeQuestions = questions(
            "question_a",
            "question_b",
            "question_c",
            "question_d",
            "question_e",
        )

        val selected = selector().select(AssessmentConfig.Mixed(questionCount = 2))

        assertEquals(listOf("question_a", "question_b"), selected.map { it.id })
    }

    @Test
    fun requestedCountAbovePoolReturnsAvailableUniqueQuestions() = runSelectorTest {
        repository.activeQuestions = questions("question_a", "question_b", "question_c", "question_d")

        val selected = selector().select(AssessmentConfig.Mixed(questionCount = 10))

        assertEquals(listOf("question_a", "question_b", "question_c", "question_d"), selected.map { it.id })
    }

    @Test
    fun emptyEligiblePoolReturnsEmptyList() = runSelectorTest {
        repository.activeQuestions = emptyList()

        val selected = selector().select(AssessmentConfig.Mixed(questionCount = 10))

        assertEquals(emptyList(), selected)
    }

    @Test
    fun duplicateRepositoryResultsAreDeduplicatedByStableQuestionId() = runSelectorTest {
        repository.activeQuestions = listOf(
            question("question_a", text = "First copy"),
            question("question_b"),
            question("question_a", text = "Duplicate copy"),
        )

        val selected = selector().select(AssessmentConfig.Mixed(questionCount = 10))

        assertEquals(listOf("question_a", "question_b"), selected.map { it.id })
        assertEquals("First copy?", selected.first().text)
        assertUniqueQuestionIds(selected)
    }

    @Test
    fun randomizationIsInjectable() = runSelectorTest {
        repository.activeQuestions = listOf(
            question("A1", topicId = "A"),
            question("B1", topicId = "B"),
            question("A2", topicId = "A"),
            question("C1", topicId = "C"),
        )

        val selected = selector(
            randomize = { it.reversed() },
        ).select(AssessmentConfig.Mixed(questionCount = 4))

        assertEquals(listOf("C1", "A2", "B1", "A1"), selected.map { it.id })
    }

    @Test
    fun randomizerCannotIntroduceDuplicateSelectedQuestionIds() = runSelectorTest {
        repository.activeQuestions = questions("question_a", "question_b", "question_c")

        val selected = selector(
            randomize = { listOf(it[1], it[1], it[2], it[0]) },
        ).select(AssessmentConfig.Mixed(questionCount = 10))

        assertEquals(listOf("question_b", "question_c", "question_a"), selected.map { it.id })
    }

    private fun runSelectorTest(
        block: suspend SelectorTestScope.() -> Unit,
    ) {
        var outcome: Result<Unit>? = null
        block.startCoroutine(
            receiver = SelectorTestScope(),
            completion = object : Continuation<Unit> {
                override val context = EmptyCoroutineContext

                override fun resumeWith(result: Result<Unit>) {
                    outcome = result
                }
            },
        )
        outcome?.getOrThrow()
            ?: error("Selector test did not complete synchronously.")
    }

    private class SelectorTestScope {
        val repository = FakeCurriculumRepository()

        fun selector(
            randomize: (List<Question>) -> List<Question> = { it },
        ): AssessmentQuestionSelector =
            AssessmentQuestionSelector(
                curriculumRepository = repository,
                randomize = randomize,
            )
    }

    private class FakeCurriculumRepository : CurriculumRepository {
        var activeQuestions: List<Question> = emptyList()
        var topicQuestions: Map<String, List<Question>> = emptyMap()
        var subtopicQuestions: Map<String, List<Question>> = emptyMap()
        val calls = mutableListOf<String>()

        override suspend fun getActiveTopics(): List<Topic> =
            error("Not used by AssessmentQuestionSelector.")

        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> =
            error("Not used by AssessmentQuestionSelector.")

        override suspend fun getActiveQuestions(): List<Question> {
            calls += "all"
            return activeQuestions
        }

        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> {
            calls += "topic:$topicId"
            return topicQuestions[topicId].orEmpty()
        }

        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> {
            calls += "subtopic:$subtopicId"
            return subtopicQuestions[subtopicId].orEmpty()
        }

        override suspend fun getQuestionById(questionId: String): Question? =
            error("Not used by AssessmentQuestionSelector.")
    }

    private fun questions(vararg ids: String): List<Question> =
        ids.map { question(it) }

    private fun mixedRoundFixture(): List<Question> =
        listOf(
            question("A1", topicId = "A"),
            question("A2", topicId = "A"),
            question("B1", topicId = "B"),
            question("C1", topicId = "C"),
            question("B2", topicId = "B"),
            question("A3", topicId = "A"),
        )

    private fun question(
        id: String,
        topicId: String = "${id}_topic",
        text: String = id,
    ): Question =
        Question(
            id = id,
            topicId = topicId,
            subtopicId = "${id}_subtopic",
            text = "$text?",
            answers = listOf(
                AnswerOption("${id}_answer_a", "Answer A"),
                AnswerOption("${id}_answer_b", "Answer B"),
            ),
            correctAnswerIds = listOf("${id}_answer_a"),
            explanation = "$id explanation.",
            sources = listOf(
                SourceReference(
                    title = "$id source",
                    url = "https://example.com/$id",
                ),
            ),
            status = ContentStatus.ACTIVE,
        )

    private fun assertUniqueQuestionIds(questions: List<Question>) {
        assertEquals(
            questions.size,
            questions.map { it.id }.toSet().size,
        )
    }
}
