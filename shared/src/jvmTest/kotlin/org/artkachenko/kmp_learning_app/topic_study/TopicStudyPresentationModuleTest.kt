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
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSessionLoader
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeService
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingLaunch
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingViewModel
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.mixed_interview.MixedInterviewResultViewModel
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultViewModel
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserViewModel
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicDetailViewModel
import org.koin.core.parameter.parametersOf
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
                    single {
                        AssessmentQuestionSelector(
                            curriculumRepository = get(),
                            randomize = { it },
                        )
                    }
                    single {
                        AssessmentEngine(
                            questionSelector = get(),
                            generateAttemptId = { "attempt" },
                        )
                    }
                    single<AssessmentRepository> { FakeAssessmentRepository() }
                    single { AssessmentSessionLoader(get(), get()) }
                    single { AssessmentRetakeService(get(), get()) }
                },
                topicStudyPresentationModule,
            )
        }

        try {
            assertIs<TopicBrowserViewModel>(app.koin.get<TopicBrowserViewModel>())
            assertIs<TopicDetailViewModel>(
                app.koin.get<TopicDetailViewModel> { parametersOf("topic") },
            )
            assertIs<AssessmentTakingViewModel>(
                app.koin.get<AssessmentTakingViewModel> {
                    parametersOf(
                        AssessmentTakingLaunch.New(AssessmentConfig.Focused(
                            scope = AssessmentScope.Topic("topic"),
                            questionCount = 1,
                        )),
                    )
                },
            )
            assertIs<AssessmentTakingViewModel>(
                app.koin.get<AssessmentTakingViewModel> {
                    parametersOf(
                        AssessmentTakingLaunch.New(AssessmentConfig.Mixed(questionCount = 1)),
                    )
                },
            )
            assertIs<AssessmentTakingViewModel>(
                app.koin.get<AssessmentTakingViewModel> {
                    parametersOf(AssessmentTakingLaunch.ExistingAttempt("attempt"))
                },
            )
            assertIs<AssessmentSessionLoader>(app.koin.get<AssessmentSessionLoader>())
            assertIs<AssessmentRetakeService>(app.koin.get<AssessmentRetakeService>())
            assertIs<AssessmentReviewLoader>(app.koin.get<AssessmentReviewLoader>())
            assertIs<FocusedResultViewModel>(
                app.koin.get<FocusedResultViewModel> { parametersOf("attempt") },
            )
            assertIs<MixedInterviewResultViewModel>(
                app.koin.get<MixedInterviewResultViewModel> { parametersOf("attempt") },
            )
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

        override suspend fun getTopicById(topicId: String): Topic? =
            error("Not used by TopicBrowserViewModel.")

        override suspend fun getQuestionById(questionId: String): Question? =
            error("Not used by TopicBrowserViewModel.")
    }

    private class FakeAssessmentRepository : AssessmentRepository {
        override suspend fun save(attempt: TestAttempt) = Unit

        override suspend fun getById(attemptId: String): TestAttempt? = null
    }
}
