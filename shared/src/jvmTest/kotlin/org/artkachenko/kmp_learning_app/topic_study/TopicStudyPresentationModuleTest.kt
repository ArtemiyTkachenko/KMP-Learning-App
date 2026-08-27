package org.artkachenko.kmp_learning_app.topic_study

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserViewModel
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
internal class TopicStudyPresentationModuleTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun presentationModuleResolvesTopicBrowserViewModel() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val app = koinApplication {
            modules(
                module {
                    single<CurriculumRepository> {
                        FakeCurriculumRepository()
                    }
                },
                topicStudyPresentationModule,
            )
        }

        try {
            assertIs<TopicBrowserViewModel>(app.koin.get<TopicBrowserViewModel>())
            advanceUntilIdle()
        } finally {
            app.close()
        }
    }

    private class FakeCurriculumRepository : CurriculumRepository {
        override suspend fun getActiveTopics(): List<Topic> =
            listOf(Topic("topic", "Topic"))

        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> =
            error("Not used by TopicBrowserViewModel.")

        override suspend fun getActiveQuestions(): List<Question> =
            error("Not used by TopicBrowserViewModel.")

        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
            error("Not used by TopicBrowserViewModel.")

        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
            error("Not used by TopicBrowserViewModel.")

        override suspend fun getQuestionById(questionId: String): Question? =
            error("Not used by TopicBrowserViewModel.")
    }
}
