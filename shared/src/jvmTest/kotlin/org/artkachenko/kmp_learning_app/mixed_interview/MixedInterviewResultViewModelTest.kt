package org.artkachenko.kmp_learning_app.mixed_interview

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

@OptIn(ExperimentalCoroutinesApi::class)
internal class MixedInterviewResultViewModelTest {
    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun completedMixedAttemptUsesPersistedScoreAndEncounterOrderedTopicPerformance() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val questions = listOf(
            question("b1", "topic-b"),
            question("a1", "topic-a"),
            question("b2", "topic-b"),
            question("c1", "topic-c"),
        )
        val curriculum = FakeCurriculumRepository(
            questions = questions,
            topics = listOf(
                Topic("topic-a", "Topic A"),
                Topic("topic-b", "Retired Topic B", ContentStatus.DEPRECATED),
            ),
        )
        val attempt = completedAttempt(
            questionAttempts = listOf(
                answered("b1", isCorrect = true),
                answered("a1", isCorrect = false),
                answered("b2", isCorrect = false),
                answered("c1", isCorrect = true),
                answered("missing", isCorrect = true),
            ),
            score = AssessmentScore(totalQuestions = 5, correctAnswers = 3),
        )

        val viewModel = viewModel(FakeAssessmentRepository(attempt), curriculum)
        advanceUntilIdle()

        val state = assertIs<MixedInterviewResultUiState.Content>(viewModel.uiState.value)
        assertEquals(5, state.totalQuestions)
        assertEquals(3, state.correctAnswers)
        assertEquals(60.0, state.percentage)
        assertEquals(listOf("b1", "a1", "b2", "c1", "missing"), state.questions.map {
            when (it) {
                is ReviewQuestionItem.Available -> it.question.questionId
                is ReviewQuestionItem.Missing -> it.questionId
            }
        })
        assertEquals(
            listOf(
                TopicPerformanceUiModel("topic-b", "Retired Topic B", 2, 1, 50.0),
                TopicPerformanceUiModel("topic-a", "Topic A", 1, 0, 0.0),
                TopicPerformanceUiModel("topic-c", null, 1, 1, 100.0),
            ),
            state.topicPerformance,
        )
        assertEquals(4, state.topicPerformance.sumOf { it.questionCount })
        assertEquals(4, state.questions.count { it is ReviewQuestionItem.Available })
        assertEquals(listOf("topic-b", "topic-a", "topic-c"), curriculum.topicLookups)
    }

    @Test
    fun missingInProgressAndWrongConfigAttemptsHaveExplicitStates() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val curriculum = FakeCurriculumRepository(emptyList(), emptyList())
        val repository = FakeAssessmentRepository(null)
        val viewModel = viewModel(repository, curriculum)
        advanceUntilIdle()
        assertIs<MixedInterviewResultUiState.AttemptNotFound>(viewModel.uiState.value)

        repository.attempt = inProgressAttempt()
        viewModel.retry()
        advanceUntilIdle()
        assertIs<MixedInterviewResultUiState.NotCompleted>(viewModel.uiState.value)

        repository.attempt = completedAttempt(
            questionAttempts = listOf(answered("q")),
            score = AssessmentScore(1, 1),
            config = AssessmentConfig.Focused(AssessmentScope.Topic("topic"), 1),
        )
        viewModel.retry()
        advanceUntilIdle()
        assertIs<MixedInterviewResultUiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun loadFailureCanBeRetried() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val question = question("q", "topic")
        val repository = FakeAssessmentRepository(
            completedAttempt(listOf(answered("q")), AssessmentScore(1, 1)),
            failNextLoad = true,
        )
        val curriculum = FakeCurriculumRepository(
            questions = listOf(question),
            topics = listOf(Topic("topic", "Topic")),
        )
        val viewModel = viewModel(repository, curriculum)
        advanceUntilIdle()
        assertIs<MixedInterviewResultUiState.Error>(viewModel.uiState.value)

        viewModel.retry()
        advanceUntilIdle()
        assertIs<MixedInterviewResultUiState.Content>(viewModel.uiState.value)
    }

    private fun viewModel(
        repository: FakeAssessmentRepository,
        curriculum: FakeCurriculumRepository,
    ) = MixedInterviewResultViewModel(
        attemptId = "attempt",
        assessmentRepository = repository,
        curriculumRepository = curriculum,
        assessmentReviewLoader = AssessmentReviewLoader(curriculum),
    )

    private fun inProgressAttempt() = TestAttempt(
        id = "attempt",
        config = AssessmentConfig.Mixed(1),
        questionAttempts = listOf(QuestionAttempt("q")),
        status = AssessmentStatus.IN_PROGRESS,
        startedAt = Instant.fromEpochMilliseconds(1),
    )

    private fun completedAttempt(
        questionAttempts: List<QuestionAttempt>,
        score: AssessmentScore,
        config: AssessmentConfig = AssessmentConfig.Mixed(questionAttempts.size),
    ) = TestAttempt(
        id = "attempt",
        config = config,
        questionAttempts = questionAttempts,
        status = AssessmentStatus.COMPLETED,
        startedAt = Instant.fromEpochMilliseconds(1),
        completedAt = Instant.fromEpochMilliseconds(2),
        score = score,
    )

    private fun answered(questionId: String, isCorrect: Boolean) =
        QuestionAttempt(
            questionId,
            QuestionAnswerState.Answered(setOf("a"), isCorrect),
        )

    private fun answered(questionId: String) = answered(questionId, isCorrect = true)

    private fun question(id: String, topicId: String) = Question(
        id = id,
        topicId = topicId,
        subtopicId = "subtopic",
        text = "Question $id",
        answers = listOf(AnswerOption("a", "Answer A"), AnswerOption("b", "Answer B")),
        correctAnswerIds = listOf("a"),
        explanation = "Explanation $id",
        sources = listOf(SourceReference("Source $id", "https://example.com/$id")),
    )

    private class FakeAssessmentRepository(
        var attempt: TestAttempt?,
        var failNextLoad: Boolean = false,
    ) : AssessmentRepository {
        override suspend fun save(attempt: TestAttempt) = Unit

        override suspend fun getById(attemptId: String): TestAttempt? {
            if (failNextLoad) {
                failNextLoad = false
                error("load failed")
            }
            return attempt
        }
    }

    private class FakeCurriculumRepository(
        private val questions: List<Question>,
        private val topics: List<Topic>,
    ) : CurriculumRepository {
        val topicLookups = mutableListOf<String>()

        override suspend fun getActiveTopics(): List<Topic> = error("Not used")
        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> = error("Not used")
        override suspend fun getActiveQuestions(): List<Question> = error("Not used")
        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> = error("Not used")
        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> = error("Not used")
        override suspend fun getTopicById(topicId: String): Topic? {
            topicLookups += topicId
            return topics.firstOrNull { it.id == topicId }
        }
        override suspend fun getQuestionById(questionId: String): Question? =
            questions.firstOrNull { it.id == questionId }
    }
}
