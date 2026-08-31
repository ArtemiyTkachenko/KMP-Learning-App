package org.artkachenko.kmp_learning_app.assessment.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

internal class AssessmentSessionLoaderTest {
    @Test
    fun loadsInProgressAttemptInPersistedQuestionOrder() = runTest {
        val repository = FakeAssessmentRepository(attempt(listOf("q3", "q1", "q2")))
        val result = AssessmentSessionLoader(
            repository,
            FakeCurriculumRepository(listOf(question("q1"), question("q2"), question("q3"))),
        ).load("attempt")

        val loaded = assertIs<AssessmentSessionLoadResult.Loaded>(result)
        assertEquals(listOf("q3", "q1", "q2"), loaded.session.questions.map { it.id })
    }

    @Test
    fun rejectsMissingAndCompletedAttempts() = runTest {
        val repository = FakeAssessmentRepository(null)
        val loader = AssessmentSessionLoader(repository, FakeCurriculumRepository())
        assertIs<AssessmentSessionLoadResult.AttemptNotFound>(loader.load("missing"))

        repository.attempt = attempt(listOf("q"), status = AssessmentStatus.COMPLETED)
        assertIs<AssessmentSessionLoadResult.NotInProgress>(loader.load("attempt"))
    }

    @Test
    fun missingQuestionIsExplicitAndDeprecatedQuestionLoads() = runTest {
        val curriculum = FakeCurriculumRepository(listOf(question("q", ContentStatus.DEPRECATED)))
        val missing = AssessmentSessionLoader(
            FakeAssessmentRepository(attempt(listOf("missing", "q"))), curriculum,
        ).load("attempt")
        assertEquals("missing", assertIs<AssessmentSessionLoadResult.MissingQuestion>(missing).questionId)

        val loaded = AssessmentSessionLoader(
            FakeAssessmentRepository(attempt(listOf("q"))), curriculum,
        ).load("attempt")
        assertIs<AssessmentSessionLoadResult.Loaded>(loaded)
    }

    private fun attempt(ids: List<String>, status: AssessmentStatus = AssessmentStatus.IN_PROGRESS) = TestAttempt(
        id = "attempt",
        config = AssessmentConfig.Focused(AssessmentScope.Topic("topic"), 10),
        questionAttempts = ids.map {
            QuestionAttempt(
                it,
                if (status == AssessmentStatus.COMPLETED) {
                    QuestionAnswerState.Answered(setOf("a"), isCorrect = false)
                } else {
                    QuestionAnswerState.Unanswered
                },
            )
        },
        status = status,
        startedAt = Instant.fromEpochMilliseconds(1),
        completedAt = if (status == AssessmentStatus.COMPLETED) Instant.fromEpochMilliseconds(2) else null,
        score = if (status == AssessmentStatus.COMPLETED) org.artkachenko.kmp_learning_app.assessment.AssessmentScore(ids.size, 0) else null,
    )

    private fun question(id: String, status: ContentStatus = ContentStatus.ACTIVE) = Question(
        id, "topic", "subtopic", "Question", listOf(AnswerOption("a", "A")), AnswerSelectionMode.SINGLE, listOf("a"),
        "Explanation", listOf(SourceReference("Source", "url")), status,
    )

    private class FakeAssessmentRepository(var attempt: TestAttempt?) : AssessmentRepository {
        override suspend fun save(attempt: TestAttempt) { this.attempt = attempt }
        override suspend fun getById(attemptId: String): TestAttempt? = attempt
        override suspend fun getCompletedAttempts(): List<TestAttempt> = emptyList()
    }

    private class FakeCurriculumRepository(
        private val questions: List<Question> = emptyList(),
    ) : CurriculumRepository {
        override suspend fun getActiveTopics(): List<Topic> = emptyList()
        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> = emptyList()
        override suspend fun getActiveQuestions(): List<Question> = emptyList()
        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> = emptyList()
        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> = emptyList()
        override suspend fun getTopicById(topicId: String): Topic? = null
        override suspend fun getSubtopicById(subtopicId: String): Subtopic? = null
        override suspend fun getQuestionById(questionId: String): Question? = questions.firstOrNull { it.id == questionId }
    }
}
