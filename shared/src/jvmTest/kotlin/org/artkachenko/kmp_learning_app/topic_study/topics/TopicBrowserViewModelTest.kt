package org.artkachenko.kmp_learning_app.topic_study.topics

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
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
    fun loadsTopicAndSubtopicCatalogInRepositoryOrder() = runViewModelTest {
        val topics = listOf(
            Topic("topic_b", "Topic B"),
            Topic("topic_a", "Topic A"),
        )
        val repository = FakeCurriculumRepository(
            topicResults = resultsOf(topics),
            subtopicResults = mutableMapOf(
                "topic_b" to resultsOf(
                    listOf(
                        Subtopic("subtopic_b2", "topic_b", "B Two"),
                        Subtopic("subtopic_b1", "topic_b", "B One"),
                    ),
                ),
                "topic_a" to resultsOf(
                    listOf(Subtopic("subtopic_a", "topic_a", "A One")),
                ),
            ),
        )
        val viewModel = TopicBrowserViewModel(repository)

        assertEquals(TopicBrowserUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertEquals(topics, state.topics)
        assertEquals(
            listOf("subtopic_b2", "subtopic_b1", "subtopic_a"),
            state.searchableSubtopics.map(SubtopicSearchResult::subtopicId),
        )
        assertEquals(listOf("topic_b", "topic_a"), repository.subtopicReadTopicIds)

        viewModel.onSearchQueryChange("b")
        val search = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertEquals(
            listOf("subtopic_b2", "subtopic_b1"),
            search.subtopicMatches.map(SubtopicSearchResult::subtopicId),
        )
    }

    @Test
    fun topicMatchingIsCaseInsensitiveAndPreservesTopicOrder() = runViewModelTest {
        val viewModel = loadedViewModel()

        listOf("compose", "COMPOSE", "Compose").forEach { query ->
            viewModel.onSearchQueryChange(query)
            val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
            assertEquals(
                listOf("compose", "compose_architecture"),
                state.topicMatches.map(TopicSearchResult::topicId),
            )
        }
    }

    @Test
    fun subtopicMatchIncludesParentTopicContext() = runViewModelTest {
        val viewModel = loadedViewModel()

        viewModel.onSearchQueryChange("viewmodel")

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertTrue(state.topicMatches.isEmpty())
        assertEquals(
            listOf(
                SubtopicSearchResult(
                    subtopicId = "viewmodel_lifecycle",
                    subtopicName = "ViewModel lifecycle",
                    parentTopicId = "architecture",
                    parentTopicName = "Lifecycle, State & Navigation",
                ),
            ),
            state.subtopicMatches,
        )
    }

    @Test
    fun oneQueryCanReturnTopicAndSubtopicGroups() = runViewModelTest {
        val viewModel = loadedViewModel()

        viewModel.onSearchQueryChange("compose")

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertEquals(2, state.topicMatches.size)
        assertEquals(
            listOf("compose_runtime"),
            state.subtopicMatches.map(SubtopicSearchResult::subtopicId),
        )
    }

    @Test
    fun multiWordMatchingTrimsSplitsAndRequiresEveryToken() = runViewModelTest {
        val viewModel = loadedViewModel()

        viewModel.onSearchQueryChange("  VIEW   model  ")

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertEquals(
            listOf("viewmodel_lifecycle"),
            state.subtopicMatches.map(SubtopicSearchResult::subtopicId),
        )
    }

    @Test
    fun blankAndClearedQueriesRestoreNormalBrowsingWithoutExpandingSubtopics() = runViewModelTest {
        val viewModel = loadedViewModel()
        val originalTopics = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value).topics

        viewModel.onSearchQueryChange("compose")
        assertTrue(
            assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value).topicMatches.isNotEmpty(),
        )

        viewModel.onSearchQueryChange("   ")
        val whitespace = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertEquals(originalTopics, whitespace.topics)
        assertTrue(whitespace.topicMatches.isEmpty())
        assertTrue(whitespace.subtopicMatches.isEmpty())

        viewModel.onSearchQueryChange("")
        val cleared = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertEquals(originalTopics, cleared.topics)
        assertTrue(cleared.topicMatches.isEmpty())
        assertTrue(cleared.subtopicMatches.isEmpty())
    }

    @Test
    fun nonBlankQueryCanProduceExplicitlyEmptyMatches() = runViewModelTest {
        val viewModel = loadedViewModel()

        viewModel.onSearchQueryChange("not in this curriculum")

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertEquals("not in this curriculum", state.query)
        assertTrue(state.topicMatches.isEmpty())
        assertTrue(state.subtopicMatches.isEmpty())
    }

    @Test
    fun typingFiltersLoadedCatalogWithoutRepositoryOrQuestionReads() = runViewModelTest {
        val repository = catalogRepository()
        val viewModel = TopicBrowserViewModel(repository)
        advanceUntilIdle()

        viewModel.onSearchQueryChange("c")
        viewModel.onSearchQueryChange("co")
        viewModel.onSearchQueryChange("compose")
        viewModel.onSearchQueryChange("")

        assertEquals(1, repository.topicReadCount)
        assertEquals(3, repository.subtopicReadTopicIds.size)
        assertEquals(0, repository.questionReadCount)
    }

    @Test
    fun emptyTopicCatalogDoesNotLoadSubtopics() = runViewModelTest {
        val repository = FakeCurriculumRepository(topicResults = resultsOf(emptyList()))
        val viewModel = TopicBrowserViewModel(repository)

        advanceUntilIdle()

        assertEquals(TopicBrowserUiState.Empty, viewModel.uiState.value)
        assertTrue(repository.subtopicReadTopicIds.isEmpty())
    }

    @Test
    fun anyCatalogLoadFailureBecomesErrorAndRetryCanSucceed() = runViewModelTest {
        val topic = Topic("topic_a", "Topic A")
        val repository = FakeCurriculumRepository(
            topicResults = resultsOf(listOf(topic), listOf(topic)),
            subtopicResults = mutableMapOf(
                topic.id to ArrayDeque(
                    listOf(
                        Result.failure(IllegalStateException("subtopics unavailable")),
                        Result.success(listOf(Subtopic("subtopic_a", topic.id, "Subtopic A"))),
                    ),
                ),
            ),
        )
        val viewModel = TopicBrowserViewModel(repository)

        advanceUntilIdle()
        assertEquals(TopicBrowserUiState.Error, viewModel.uiState.value)

        viewModel.retry()
        assertEquals(TopicBrowserUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertEquals(listOf("subtopic_a"), state.searchableSubtopics.map { it.subtopicId })
    }

    private suspend fun TestScope.loadedViewModel(): TopicBrowserViewModel =
        TopicBrowserViewModel(catalogRepository()).also { advanceUntilIdle() }

    private fun runViewModelTest(
        block: suspend TestScope.() -> Unit,
    ) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }

    private class FakeCurriculumRepository(
        private val topicResults: ArrayDeque<Result<List<Topic>>>,
        private val subtopicResults: MutableMap<String, ArrayDeque<Result<List<Subtopic>>>> =
            mutableMapOf(),
    ) : CurriculumRepository {
        var topicReadCount: Int = 0
            private set
        val subtopicReadTopicIds = mutableListOf<String>()
        var questionReadCount: Int = 0
            private set

        override suspend fun getActiveTopics(): List<Topic> {
            topicReadCount += 1
            return topicResults.removeFirst().getOrThrow()
        }

        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> {
            subtopicReadTopicIds += topicId
            return subtopicResults[topicId]?.removeFirst()?.getOrThrow().orEmpty()
        }

        override suspend fun getActiveQuestions(): List<Question> {
            questionReadCount += 1
            return emptyList()
        }

        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> {
            questionReadCount += 1
            return emptyList()
        }

        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> {
            questionReadCount += 1
            return emptyList()
        }

        override suspend fun getTopicById(topicId: String): Topic? =
            error("Not used by TopicBrowserViewModel.")

        override suspend fun getSubtopicById(subtopicId: String): Subtopic? =
            error("Not used by TopicBrowserViewModel.")

        override suspend fun getQuestionById(questionId: String): Question? {
            questionReadCount += 1
            return null
        }
    }

    private companion object {
        fun catalogRepository() = FakeCurriculumRepository(
            topicResults = resultsOf(
                listOf(
                    Topic("compose", "Compose UI"),
                    Topic("compose_architecture", "Compose Architecture"),
                    Topic("architecture", "Lifecycle, State & Navigation"),
                ),
            ),
            subtopicResults = mutableMapOf(
                "compose" to resultsOf(
                    listOf(Subtopic("compose_runtime", "compose", "Compose runtime")),
                ),
                "compose_architecture" to resultsOf(emptyList()),
                "architecture" to resultsOf(
                    listOf(
                        Subtopic(
                            "viewmodel_lifecycle",
                            "architecture",
                            "ViewModel lifecycle",
                        ),
                    ),
                ),
            ),
        )

        fun <T> resultsOf(vararg values: T): ArrayDeque<Result<T>> =
            ArrayDeque(values.map(Result.Companion::success))
    }
}
