package org.artkachenko.kmp_learning_app.topic_study.topics

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

@OptIn(ExperimentalCoroutinesApi::class)
internal class TopicBrowserViewModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsContentInRepositoryOrder() = runViewModelTest {
        val topics = listOf(
            Topic("topic_b", "Topic B"),
            Topic("topic_a", "Topic A"),
            Topic("topic_c", "Topic C"),
        )
        val viewModel = TopicBrowserViewModel(
            curriculumRepository = FakeCurriculumRepository(
                topicResults = ArrayDeque(listOf(Result.success(topics))),
            ),
        )

        assertEquals(TopicBrowserUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertEquals(topics, state.topics)
    }

    @Test
    fun emptyRepositoryResultBecomesEmptyState() = runViewModelTest {
        val viewModel = TopicBrowserViewModel(
            curriculumRepository = FakeCurriculumRepository(
                topicResults = ArrayDeque(listOf(Result.success(emptyList()))),
            ),
        )

        advanceUntilIdle()

        assertEquals(TopicBrowserUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun repositoryFailureBecomesGenericErrorState() = runViewModelTest {
        val viewModel = TopicBrowserViewModel(
            curriculumRepository = FakeCurriculumRepository(
                topicResults = ArrayDeque(listOf(Result.failure(IllegalStateException("database unavailable")))),
            ),
        )

        advanceUntilIdle()

        assertEquals(TopicBrowserUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun retryMovesThroughLoadingAndCanReachContent() = runViewModelTest {
        val topics = listOf(Topic("topic_a", "Topic A"))
        val viewModel = TopicBrowserViewModel(
            curriculumRepository = FakeCurriculumRepository(
                topicResults = ArrayDeque(
                    listOf(
                        Result.failure(IllegalStateException("first load failed")),
                        Result.success(topics),
                    ),
                ),
            ),
        )

        advanceUntilIdle()
        assertEquals(TopicBrowserUiState.Error, viewModel.uiState.value)

        viewModel.retry()

        assertEquals(TopicBrowserUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()
        assertEquals(TopicBrowserUiState.Content(topics), viewModel.uiState.value)
    }

    private fun runViewModelTest(
        block: suspend TestScope.() -> Unit,
    ) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }

    private class FakeCurriculumRepository(
        private val topicResults: ArrayDeque<Result<List<Topic>>>,
    ) : CurriculumRepository {
        override suspend fun getActiveTopics(): List<Topic> =
            topicResults.removeFirst().getOrThrow()

        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> =
            error("Not used by TopicBrowserViewModel.")

        override suspend fun getActiveQuestions(): List<Question> =
            error("Not used by TopicBrowserViewModel.")

        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
            error("Not used by TopicBrowserViewModel.")

        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
            error("Not used by TopicBrowserViewModel.")

        override suspend fun getTopicById(topicId: String): Topic? =
            error("Not used by TopicBrowserViewModel.")

        override suspend fun getQuestionById(questionId: String): Question? =
            error("Not used by TopicBrowserViewModel.")
    }
}
