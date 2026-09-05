package org.artkachenko.kmp_learning_app.topic_study

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.learning.content.learningContentModule
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.history.AppCoroutineScope
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSessionLoader
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeService
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingLaunch
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingViewModel
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.mixed_interview.MixedInterviewResultViewModel
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewViewModel
import org.artkachenko.kmp_learning_app.progress.ProgressTopicViewModel
import org.artkachenko.kmp_learning_app.progress.ProgressViewModel
import org.artkachenko.kmp_learning_app.saved_questions.FakeSavedQuestionRepository
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionContentResolver
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionStateHolder
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionsViewModel
import org.artkachenko.kmp_learning_app.saved_questions.repository.SavedQuestionRepository
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultViewModel
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderViewModel
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
                            completedHistory = { emptyList() },
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
                    single { LearningProgressService(get(), get()) }
                    // The presentation module now depends on the app-scoped history cache; the
                    // real one is declared alongside the assessment data module.
                    single { AppCoroutineScope() }
                    single { AssessmentHistoryStore(get(), get<AppCoroutineScope>()) }
                    // Saved Questions are provided by the data module in production; the review
                    // ViewModels resolve the app-scoped holder built on that repository.
                    single<SavedQuestionRepository> { FakeSavedQuestionRepository() }
                },
                // The real E20 module rather than another fake: the Topic Browser must resolve the
                // same LearningContentRepository singleton the hosts already register, through its
                // interface, and this is what proves the two modules compose.
                learningContentModule,
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
            // The Practice Builder is reached both ways: from content with a scope alone, and from
            // a guided-learning or contextual preset that also names the source it opens on. The
            // module reads that second parameter as optional, so both entries have to resolve and
            // the preset one has to arrive on its own source rather than on the ALL default.
            assertEquals(
                PracticeQuestionSource.ALL,
                app.koin.get<PracticeBuilderViewModel> {
                    parametersOf(AssessmentScope.Topic("topic"))
                }.uiState.value.source,
            )
            assertEquals(
                PracticeQuestionSource.WEAK_AREAS,
                app.koin.get<PracticeBuilderViewModel> {
                    parametersOf(
                        AssessmentScope.Subtopic("subtopic"),
                        PracticeQuestionSource.WEAK_AREAS,
                    )
                }.uiState.value.source,
            )
            assertIs<AssessmentSessionLoader>(app.koin.get<AssessmentSessionLoader>())
            assertIs<AssessmentRetakeService>(app.koin.get<AssessmentRetakeService>())
            assertIs<AssessmentReviewLoader>(app.koin.get<AssessmentReviewLoader>())
            assertIs<CurriculumRepository>(app.koin.get<CurriculumRepository>())
            assertIs<LearningContentRepository>(app.koin.get<LearningContentRepository>())
            assertIs<AssessmentRepository>(app.koin.get<AssessmentRepository>())
            assertIs<AssessmentQuestionSelector>(app.koin.get<AssessmentQuestionSelector>())
            assertIs<AssessmentEngine>(app.koin.get<AssessmentEngine>())
            assertIs<FocusedResultViewModel>(
                app.koin.get<FocusedResultViewModel> { parametersOf("attempt") },
            )
            assertIs<MixedInterviewResultViewModel>(
                app.koin.get<MixedInterviewResultViewModel> { parametersOf("attempt") },
            )
            assertIs<ProgressViewModel>(app.koin.get<ProgressViewModel>())
            assertIs<ProgressTopicViewModel>(
                app.koin.get<ProgressTopicViewModel> { parametersOf("topic") },
            )
            assertIs<MistakeReviewService>(app.koin.get<MistakeReviewService>())
            assertIs<MistakeReviewViewModel>(app.koin.get<MistakeReviewViewModel>())
            // One holder for the whole app: every review surface must observe the same instance.
            assertEquals(
                app.koin.get<SavedQuestionStateHolder>(),
                app.koin.get<SavedQuestionStateHolder>(),
            )
            assertIs<SavedQuestionContentResolver>(app.koin.get<SavedQuestionContentResolver>())
            assertIs<SavedQuestionsViewModel>(app.koin.get<SavedQuestionsViewModel>())
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

        override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> =
            error("Not used by TopicBrowserViewModel.")

        override suspend fun getActiveQuestionsByTopicAndLevels(
            topicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = error("Not used by TopicBrowserViewModel.")

        override suspend fun getActiveQuestionsBySubtopicAndLevels(
            subtopicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = error("Not used by TopicBrowserViewModel.")

        override suspend fun getTopicById(topicId: String): Topic? =
            error("Not used by TopicBrowserViewModel.")

        override suspend fun getSubtopicById(subtopicId: String): Subtopic? =
            error("Not used by TopicBrowserViewModel.")

        override suspend fun getQuestionById(questionId: String): Question? =
            error("Not used by TopicBrowserViewModel.")
    }

    private class FakeAssessmentRepository : AssessmentRepository {
        override suspend fun save(attempt: TestAttempt) = Unit

        override suspend fun getById(attemptId: String): TestAttempt? = null

        override suspend fun getCompletedAttempts(): List<TestAttempt> = emptyList()
    }
}
