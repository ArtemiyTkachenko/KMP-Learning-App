package org.artkachenko.kmp_learning_app.progress

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
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
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService

@OptIn(ExperimentalCoroutinesApi::class)
internal class ProgressTopicViewModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun contentExposesTopicAggregateAndOnlyItsObservedSubtopics() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        // kotlin: 2/3 in coroutines plus 1/1 in basics. android is a different topic entirely.
        val context = topicTestContext(
            answers = listOf(
                answer("kotlin_co_1", true),
                answer("kotlin_co_2", false),
                answer("kotlin_co_3", false),
                answer("kotlin_basics_1", true),
                answer("android_1", true),
            ),
            questions = listOf(
                topicQuestion("kotlin_co_1", "kotlin", "coroutines"),
                topicQuestion("kotlin_co_2", "kotlin", "coroutines"),
                topicQuestion("kotlin_co_3", "kotlin", "coroutines"),
                topicQuestion("kotlin_basics_1", "kotlin", "basics"),
                topicQuestion("android_1", "android", "lifecycle"),
            ),
            topics = listOf(Topic("kotlin", "Kotlin"), Topic("android", "Android")),
            subtopics = listOf(
                Subtopic("coroutines", "kotlin", "Coroutines"),
                Subtopic("basics", "kotlin", "Basics"),
                Subtopic("lifecycle", "android", "Lifecycle"),
            ),
        )

        val viewModel = topicViewModel("kotlin", context)
        advanceUntilIdle()

        val content = assertIs<ProgressTopicUiState.Content>(viewModel.uiState.value)
        assertEquals("Kotlin", content.topicName)
        assertEquals(4, content.answeredCount)
        assertEquals(2, content.correctCount)
        assertEquals(50.0, content.percentage)
        assertEquals(listOf("basics", "coroutines"), content.subtopics.map { it.subtopicId })
        assertEquals(listOf("Basics", "Coroutines"), content.subtopics.map { it.subtopicName })
    }

    @Test
    fun weakSubtopicsUseTheDerivedPolicyFlagWithoutRecalculation() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val context = topicTestContext(
            // coroutines: 0/2 -> weak (>=2 observations below 70%). basics: 1/1 -> not weak.
            answers = listOf(
                answer("kotlin_co_1", false),
                answer("kotlin_co_2", false),
                answer("kotlin_basics_1", true),
            ),
            questions = listOf(
                topicQuestion("kotlin_co_1", "kotlin", "coroutines"),
                topicQuestion("kotlin_co_2", "kotlin", "coroutines"),
                topicQuestion("kotlin_basics_1", "kotlin", "basics"),
            ),
            topics = listOf(Topic("kotlin", "Kotlin")),
            subtopics = listOf(
                Subtopic("coroutines", "kotlin", "Coroutines"),
                Subtopic("basics", "kotlin", "Basics"),
            ),
        )

        val viewModel = topicViewModel("kotlin", context)
        advanceUntilIdle()

        val content = assertIs<ProgressTopicUiState.Content>(viewModel.uiState.value)
        val byId = content.subtopics.associateBy { it.subtopicId }
        assertEquals(false, byId.getValue("basics").isWeak)
        assertTrue(byId.getValue("coroutines").isWeak)
        assertEquals(0.0, byId.getValue("coroutines").percentage)
    }

    @Test
    fun deprecatedNamesRenderAndMissingMetadataFallsBackToNull() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val context = topicTestContext(
            answers = listOf(answer("q_old", false), answer("q_unknown", false)),
            questions = listOf(
                topicQuestion("q_old", "kotlin", "old_sub"),
                topicQuestion("q_unknown", "kotlin", "unknown_sub"),
            ),
            topics = listOf(Topic("kotlin", "Old Kotlin", ContentStatus.DEPRECATED)),
            // unknown_sub is deliberately absent from the curriculum.
            subtopics = listOf(Subtopic("old_sub", "kotlin", "Old coroutines", ContentStatus.DEPRECATED)),
        )

        val viewModel = topicViewModel("kotlin", context)
        advanceUntilIdle()

        val content = assertIs<ProgressTopicUiState.Content>(viewModel.uiState.value)
        assertEquals("Old Kotlin", content.topicName)
        val byId = content.subtopics.associateBy { it.subtopicId }
        assertEquals("Old coroutines", byId.getValue("old_sub").subtopicName)
        assertEquals(null, byId.getValue("unknown_sub").subtopicName)
    }

    @Test
    fun topicWithNoCompletedObservationsIsEmpty() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val context = topicTestContext(
            answers = listOf(answer("android_1", true)),
            questions = listOf(topicQuestion("android_1", "android", "lifecycle")),
            topics = listOf(Topic("android", "Android"), Topic("kotlin", "Kotlin")),
            subtopics = listOf(Subtopic("lifecycle", "android", "Lifecycle")),
        )

        val viewModel = topicViewModel("kotlin", context)
        advanceUntilIdle()

        // Kotlin exists in the curriculum but was never answered, so nothing is fabricated.
        assertIs<ProgressTopicUiState.Empty>(viewModel.uiState.value)
    }

    @Test
    fun loadFailureSurfacesErrorAndRetrySucceeds() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val context = topicTestContext(
            answers = listOf(answer("kotlin_1", true)),
            questions = listOf(topicQuestion("kotlin_1", "kotlin", "basics")),
            topics = listOf(Topic("kotlin", "Kotlin")),
            subtopics = listOf(Subtopic("basics", "kotlin", "Basics")),
        )
        context.repository.failNextLoad = true

        val viewModel = topicViewModel("kotlin", context)
        advanceUntilIdle()
        assertIs<ProgressTopicUiState.Error>(viewModel.uiState.value)

        viewModel.retry()
        advanceUntilIdle()
        assertIs<ProgressTopicUiState.Content>(viewModel.uiState.value)
    }
}

private class TopicTestContext(
    val repository: TopicHistoryRepository,
    val curriculum: TopicCurriculumRepository,
)

private fun topicTestContext(
    answers: List<Pair<String, Boolean>>,
    questions: List<Question>,
    topics: List<Topic>,
    subtopics: List<Subtopic>,
): TopicTestContext {
    val attempt = TestAttempt(
        id = "attempt",
        config = AssessmentConfig.Mixed(answers.size),
        questionAttempts = answers.map { (questionId, isCorrect) ->
            QuestionAttempt(
                questionId,
                QuestionAnswerState.Answered(setOf("${questionId}_answer"), isCorrect),
            )
        },
        status = AssessmentStatus.COMPLETED,
        startedAt = Instant.parse("2026-08-29T00:00:00Z"),
        completedAt = Instant.parse("2026-08-29T00:15:00Z"),
        score = AssessmentScore(answers.size, answers.count { it.second }),
    )
    return TopicTestContext(
        TopicHistoryRepository(listOf(attempt)),
        TopicCurriculumRepository(questions, topics, subtopics),
    )
}

private fun topicViewModel(
    topicId: String,
    context: TopicTestContext,
): ProgressTopicViewModel =
    ProgressTopicViewModel(
        topicId = topicId,
        learningProgressService = LearningProgressService(context.repository, context.curriculum),
    )

private fun answer(
    questionId: String,
    isCorrect: Boolean,
): Pair<String, Boolean> = questionId to isCorrect

private class TopicHistoryRepository(
    private val attempts: List<TestAttempt>,
) : AssessmentRepository {
    var failNextLoad = false

    override suspend fun save(attempt: TestAttempt) = Unit

    override suspend fun getById(attemptId: String): TestAttempt? =
        attempts.firstOrNull { it.id == attemptId }

    override suspend fun getCompletedAttempts(): List<TestAttempt> {
        if (failNextLoad) {
            failNextLoad = false
            error("History unavailable")
        }
        return attempts
    }
}

private class TopicCurriculumRepository(
    questions: List<Question>,
    topics: List<Topic>,
    subtopics: List<Subtopic>,
) : CurriculumRepository {
    private val questionsById = questions.associateBy(Question::id)
    private val topicsById = topics.associateBy(Topic::id)
    private val subtopicsById = subtopics.associateBy(Subtopic::id)

    override suspend fun getActiveTopics(): List<Topic> = error("ACTIVE lookup must not be used.")
    override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestions(): List<Question> = error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
        error("ACTIVE lookup must not be used.")

    override suspend fun getTopicById(topicId: String): Topic? = topicsById[topicId]
    override suspend fun getSubtopicById(subtopicId: String): Subtopic? = subtopicsById[subtopicId]
    override suspend fun getQuestionById(questionId: String): Question? = questionsById[questionId]
}

private fun topicQuestion(
    id: String,
    topicId: String,
    subtopicId: String,
): Question =
    Question(
        id = id,
        topicId = topicId,
        subtopicId = subtopicId,
        text = "Question $id",
        answers = listOf(AnswerOption("${id}_a", "Answer A"), AnswerOption("${id}_b", "Answer B")),
        correctAnswerIds = listOf("${id}_a"),
        explanation = "Explanation",
        sources = listOf(SourceReference("Source", "https://example.com/$id")),
    )
