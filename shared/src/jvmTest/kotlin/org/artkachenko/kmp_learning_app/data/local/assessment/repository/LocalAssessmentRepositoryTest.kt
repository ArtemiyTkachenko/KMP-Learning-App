package org.artkachenko.kmp_learning_app.data.local.assessment.repository

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.data.local.assessment.AssessmentAttemptStore
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.AnswerOptionEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.SubtopicEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.TopicEntity

internal class LocalAssessmentRepositoryTest {
    @Test
    fun saveAndGetRoundTripReturnsDomainAttempt() = runTest {
        withTestDatabase { database ->
            insertFixtureCurriculum(database)
            val repository = localRepository(database)
            val attempt = completedAttempt(
                id = "attempt_round_trip",
                config = AssessmentConfig.Focused(
                    scope = AssessmentScope.Topic("topic"),
                    questionCount = 2,
                ),
            )

            repository.save(attempt)

            assertEquals(attempt, repository.getById("attempt_round_trip"))
        }
    }

    @Test
    fun missingAttemptReturnsNull() = runTest {
        withTestDatabase { database ->
            assertNull(localRepository(database).getById("missing"))
        }
    }

    @Test
    fun savingUpdatedInProgressAttemptDoesNotDuplicateChildren() = runTest {
        withTestDatabase { database ->
            insertFixtureCurriculum(database)
            val repository = localRepository(database)
            val original = TestAttempt(
                id = "attempt_update",
                config = AssessmentConfig.Mixed(questionCount = 2),
                questionAttempts = listOf(
                    QuestionAttempt("question_a"),
                    QuestionAttempt("question_b"),
                ),
                status = AssessmentStatus.IN_PROGRESS,
                startedAt = StartedAt,
            )
            val updated = original.copy(
                questionAttempts = listOf(
                    answeredAttempt("question_a", "question_a_a"),
                    QuestionAttempt("question_b"),
                ),
            )

            repository.save(original)
            repository.save(updated)

            assertEquals(updated, repository.getById("attempt_update"))
            assertEquals(1, database.assessmentAttemptDao().countTestAttempts())
            assertEquals(2, database.assessmentAttemptDao().countQuestionAttempts())
            assertEquals(1, database.assessmentAttemptDao().countSelectedAnswers())
        }
    }

    @Test
    fun savingCompletedUpdatePreservesScoreTimestampAndSelectedAnswers() = runTest {
        withTestDatabase { database ->
            insertFixtureCurriculum(database)
            val repository = localRepository(database)
            val inProgress = TestAttempt(
                id = "attempt_complete",
                config = AssessmentConfig.Focused(
                    scope = AssessmentScope.Subtopic("subtopic"),
                    questionCount = 2,
                ),
                questionAttempts = listOf(
                    QuestionAttempt("question_a"),
                    QuestionAttempt("question_b"),
                ),
                status = AssessmentStatus.IN_PROGRESS,
                startedAt = StartedAt,
            )
            val completed = inProgress.copy(
                questionAttempts = listOf(
                    answeredAttempt("question_a", "question_a_a"),
                    answeredAttempt("question_b", "question_b_b", isCorrect = false),
                ),
                status = AssessmentStatus.COMPLETED,
                completedAt = CompletedAt,
                score = AssessmentScore(totalQuestions = 2, correctAnswers = 1),
            )

            repository.save(inProgress)
            repository.save(completed)

            assertEquals(completed, repository.getById("attempt_complete"))
            assertEquals(1, database.assessmentAttemptDao().countTestAttempts())
            assertEquals(2, database.assessmentAttemptDao().countQuestionAttempts())
            assertEquals(2, database.assessmentAttemptDao().countSelectedAnswers())
        }
    }

    @Test
    fun completedHistoryDelegatesToStoreAndExcludesInProgressAttempts() = runTest {
        withTestDatabase { database ->
            insertFixtureCurriculum(database)
            val repository = localRepository(database)
            val completed = completedAttempt(
                id = "completed",
                config = AssessmentConfig.Mixed(questionCount = 2),
            )
            val inProgress = TestAttempt(
                id = "in_progress",
                config = AssessmentConfig.Mixed(questionCount = 1),
                questionAttempts = listOf(QuestionAttempt("question_a")),
                status = AssessmentStatus.IN_PROGRESS,
                startedAt = StartedAt,
            )

            repository.save(inProgress)
            repository.save(completed)

            assertEquals(listOf(completed), repository.getCompletedAttempts())
            assertEquals(inProgress, repository.getById(inProgress.id))
        }
    }

    private fun localRepository(database: CurriculumDatabase): LocalAssessmentRepository =
        LocalAssessmentRepository(
            store = AssessmentAttemptStore(database),
        )

    private suspend fun withTestDatabase(
        block: suspend (CurriculumDatabase) -> Unit,
    ) {
        val database = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        try {
            block(database)
        } finally {
            database.close()
        }
    }

    private suspend fun insertFixtureCurriculum(database: CurriculumDatabase) {
        val dao = database.curriculumDao()
        dao.upsertTopics(listOf(TopicEntity("topic", "Topic", "ACTIVE", sortOrder = 0)))
        dao.upsertSubtopics(listOf(SubtopicEntity("subtopic", "topic", "Subtopic", "ACTIVE", sortOrder = 0)))
        dao.upsertQuestions(
            listOf(
                QuestionEntity("question_a", "topic", "subtopic", "Question A?", "SINGLE", "Explanation A.", "ACTIVE", sortOrder = 0),
                QuestionEntity("question_b", "topic", "subtopic", "Question B?", "SINGLE", "Explanation B.", "ACTIVE", sortOrder = 1),
            ),
        )
        dao.upsertAnswerOptions(
            listOf(
                AnswerOptionEntity("question_a", "question_a_a", "A", sortOrder = 0),
                AnswerOptionEntity("question_a", "question_a_b", "B", sortOrder = 1),
                AnswerOptionEntity("question_b", "question_b_a", "A", sortOrder = 0),
                AnswerOptionEntity("question_b", "question_b_b", "B", sortOrder = 1),
            ),
        )
    }

    private fun completedAttempt(
        id: String,
        config: AssessmentConfig,
    ): TestAttempt =
        TestAttempt(
            id = id,
            config = config,
            questionAttempts = listOf(
                answeredAttempt("question_a", "question_a_a"),
                answeredAttempt("question_b", "question_b_a", "question_b_b", isCorrect = false),
            ),
            status = AssessmentStatus.COMPLETED,
            startedAt = StartedAt,
            completedAt = CompletedAt,
            score = AssessmentScore(totalQuestions = 2, correctAnswers = 1),
        )

    private fun answeredAttempt(
        questionId: String,
        vararg selectedAnswerIds: String,
        isCorrect: Boolean = true,
    ): QuestionAttempt =
        QuestionAttempt(
            questionId = questionId,
            answerState = QuestionAnswerState.Answered(
                selectedAnswerIds = selectedAnswerIds.toSet(),
                isCorrect = isCorrect,
            ),
        )
}

private val StartedAt = Instant.fromEpochMilliseconds(1_700_000_000_000)
private val CompletedAt = Instant.fromEpochMilliseconds(1_700_000_060_000)
