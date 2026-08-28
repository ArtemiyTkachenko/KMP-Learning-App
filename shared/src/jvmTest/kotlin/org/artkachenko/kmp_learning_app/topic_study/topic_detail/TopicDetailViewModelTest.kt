package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

@OptIn(ExperimentalCoroutinesApi::class)
internal class TopicDetailViewModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsTopicByStableIdAndPreservesSubtopicOrderAndCounts() = runViewModelTest {
        val topic = Topic("topic_a", "Topic A")
        val subtopics = listOf(
            Subtopic("subtopic_b", topic.id, "Subtopic B"),
            Subtopic("subtopic_a", topic.id, "Subtopic A"),
            Subtopic("subtopic_c", topic.id, "Subtopic C"),
        )
        val repository = FakeCurriculumRepository(
            topics = listOf(topic),
            subtopics = subtopics,
            questions = listOf(
                question("q1", topic.id, "subtopic_b"),
                question("q2", topic.id, "subtopic_b"),
                question("q3", topic.id, "subtopic_c"),
            ),
        )
        val viewModel = TopicDetailViewModel("topic_a", repository)

        advanceUntilIdle()

        val state = assertIs<TopicDetailUiState.Content>(viewModel.uiState.value)
        assertEquals(topic, state.topic)
        assertEquals(3, state.topicQuestionCount)
        assertEquals(listOf("subtopic_b", "subtopic_c"), state.subtopics.map { it.subtopic.id })
        assertEquals(listOf(2, 1), state.subtopics.map { it.questionCount })
        assertEquals(1, repository.topicQuestionCalls)
        assertEquals(0, repository.subtopicQuestionCalls)
    }

    @Test
    fun missingTopicDoesNotQueryChildren() = runViewModelTest {
        val repository = FakeCurriculumRepository(topics = emptyList())
        val viewModel = TopicDetailViewModel("missing", repository)

        advanceUntilIdle()

        assertEquals(TopicDetailUiState.NotFound, viewModel.uiState.value)
        assertEquals(0, repository.subtopicCalls)
        assertEquals(0, repository.topicQuestionCalls)
    }

    @Test
    fun noQuestionsProducesUnavailablePracticeState() = runViewModelTest {
        val topic = Topic("topic_a", "Topic A")
        val viewModel = TopicDetailViewModel(
            topicId = topic.id,
            curriculumRepository = FakeCurriculumRepository(topics = listOf(topic)),
        )

        advanceUntilIdle()

        val state = assertIs<TopicDetailUiState.NoQuestions>(viewModel.uiState.value)
        assertEquals(topic, state.topic)
        assertNull(viewModel.topicPracticeConfig())
        assertNull(viewModel.subtopicPracticeConfig("unknown"))
    }

    @Test
    fun buildsTopicAndPopulatedSubtopicConfigs() = runViewModelTest {
        val topic = Topic("topic_a", "Topic A")
        val subtopic = Subtopic("subtopic_a", topic.id, "Subtopic A")
        val viewModel = TopicDetailViewModel(
            topicId = topic.id,
            curriculumRepository = FakeCurriculumRepository(
                topics = listOf(topic),
                subtopics = listOf(subtopic),
                questions = listOf(question("q1", topic.id, subtopic.id)),
            ),
        )

        advanceUntilIdle()

        assertEquals(
            AssessmentScope.Topic(topic.id),
            viewModel.topicPracticeConfig()?.scope,
        )
        assertEquals(
            FocusedPracticeQuestionCount,
            viewModel.topicPracticeConfig()?.questionCount,
        )
        assertEquals(
            AssessmentScope.Subtopic(subtopic.id),
            viewModel.subtopicPracticeConfig(subtopic.id)?.scope,
        )
        assertNull(viewModel.subtopicPracticeConfig("empty-or-unknown"))
    }

    @Test
    fun retryCanRecoverFromFailure() = runViewModelTest {
        val topic = Topic("topic_a", "Topic A")
        val repository = FakeCurriculumRepository(
            topics = listOf(topic),
            failuresRemaining = 1,
            questions = listOf(question("q1", topic.id, "subtopic_a")),
        )
        val viewModel = TopicDetailViewModel(topic.id, repository)

        advanceUntilIdle()
        assertEquals(TopicDetailUiState.Error, viewModel.uiState.value)

        viewModel.retry()
        assertEquals(TopicDetailUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertIs<TopicDetailUiState.Content>(viewModel.uiState.value)
    }

    private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }

    private fun question(id: String, topicId: String, subtopicId: String) = Question(
        id = id,
        topicId = topicId,
        subtopicId = subtopicId,
        text = "Question $id",
        answers = listOf(AnswerOption("answer_a", "Answer A")),
        correctAnswerIds = listOf("answer_a"),
        explanation = "Explanation",
        sources = listOf(SourceReference("Source", "https://example.com")),
    )

    private class FakeCurriculumRepository(
        private val topics: List<Topic>,
        private val subtopics: List<Subtopic> = emptyList(),
        private val questions: List<Question> = emptyList(),
        private var failuresRemaining: Int = 0,
    ) : CurriculumRepository {
        var subtopicCalls = 0
            private set
        var topicQuestionCalls = 0
            private set
        var subtopicQuestionCalls = 0
            private set

        override suspend fun getActiveTopics(): List<Topic> {
            if (failuresRemaining > 0) {
                failuresRemaining -= 1
                error("load failed")
            }
            return topics
        }

        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> {
            subtopicCalls += 1
            return subtopics
        }

        override suspend fun getActiveQuestions(): List<Question> = error("Not used.")

        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> {
            topicQuestionCalls += 1
            return questions
        }

        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> {
            subtopicQuestionCalls += 1
            error("N+1 query must not be used.")
        }

        override suspend fun getTopicById(topicId: String): Topic? =
            topics.firstOrNull { it.id == topicId }

        override suspend fun getSubtopicById(subtopicId: String): Subtopic? =
            subtopics.firstOrNull { it.id == subtopicId }

        override suspend fun getQuestionById(questionId: String): Question? = error("Not used.")
    }
}
