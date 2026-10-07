package org.artkachenko.kmp_learning_app.data.local.progress_reset

import androidx.room3.Room
import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
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
import org.artkachenko.kmp_learning_app.data.local.lesson_study.repository.LocalLessonStudyRepository
import org.artkachenko.kmp_learning_app.data.local.saved_questions.repository.LocalSavedQuestionRepository

/**
 * One row per line of the reset table: what a reset deletes, what it keeps, and that a failure part
 * of the way through deletes nothing.
 */
internal class LocalProgressResetRepositoryTest {

    @Test
    fun resetDeletesEveryAttemptWithItsChildrenAndEveryStudiedLesson() = runTest {
        withSeededDatabase { database ->
            LocalProgressResetRepository(database).deleteLearnerProgress()

            val attempts = database.assessmentAttemptDao()
            assertEquals(0, attempts.countTestAttempts())
            assertEquals(0, attempts.countQuestionAttempts())
            assertEquals(0, attempts.countSelectedAnswers())
            assertEquals(0, database.studiedLessonDao().count())
        }
    }

    /** In-progress attempts are progress too: a reset must not leave an unfinished run to resume. */
    @Test
    fun resetDeletesInProgressAttemptsAsWellAsCompletedOnes() = runTest {
        withSeededDatabase { database ->
            val store = AssessmentAttemptStore(database)

            LocalProgressResetRepository(database).deleteLearnerProgress()

            assertEquals(null, store.getById(InProgress.id))
            assertEquals(null, store.getById(Completed.id))
            assertEquals(emptyList(), store.getCompletedAttempts())
        }
    }

    @Test
    fun resetKeepsSavedQuestionsAndEveryCurriculumTable() = runTest {
        withSeededDatabase { database ->
            val curriculum = database.curriculumDao()
            val before = curriculumCounts(database)

            LocalProgressResetRepository(database).deleteLearnerProgress()

            assertEquals(listOf("question_b"), LocalSavedQuestionRepository(database).getSavedQuestions().map { it.questionId })
            assertEquals(before, curriculumCounts(database))
            assertEquals(1, curriculum.countTopics())
        }
    }

    @Test
    fun resetOfEmptyProgressSucceedsAndIsRepeatable() = runTest {
        withTestDatabase { database ->
            val repository = LocalProgressResetRepository(database)

            repository.deleteLearnerProgress()
            repository.deleteLearnerProgress()

            assertEquals(0, database.assessmentAttemptDao().countTestAttempts())
            assertEquals(0, database.studiedLessonDao().count())
        }
    }

    /**
     * The studied-Lesson delete runs last, so failing it is the case that proves the attempt deletes
     * before it are rolled back: progress is never left half reset.
     */
    @Test
    fun aFailurePartWayThroughDeletesNothing() = runTest {
        withSeededDatabase { database ->
            database.useWriterConnection { connection ->
                connection.executeSQL(
                    "CREATE TRIGGER refuse_studied_delete BEFORE DELETE ON studied_lesson " +
                        "BEGIN SELECT RAISE(ABORT, 'forced failure'); END",
                )
            }

            assertFails { LocalProgressResetRepository(database).deleteLearnerProgress() }

            val attempts = database.assessmentAttemptDao()
            assertEquals(2, attempts.countTestAttempts())
            assertEquals(3, attempts.countQuestionAttempts())
            assertEquals(4, attempts.countSelectedAnswers())
            assertEquals(2, database.studiedLessonDao().count())
        }
    }

    private suspend fun curriculumCounts(database: CurriculumDatabase): List<Int> {
        val dao = database.curriculumDao()
        return listOf(
            dao.countTopics(),
            dao.countSubtopics(),
            dao.countQuestions(),
            dao.countAnswerOptions(),
            dao.countCorrectAnswers(),
            dao.countQuestionSources(),
        )
    }

    /**
     * Two attempts — one completed, one in progress — with three occurrences and four selected
     * answers between them, two studied Lessons, and one saved Question.
     */
    private suspend fun withSeededDatabase(block: suspend (CurriculumDatabase) -> Unit) {
        withTestDatabase { database ->
            insertFixtureCurriculum(database)
            val store = AssessmentAttemptStore(database)
            store.save(Completed)
            store.save(InProgress)
            val study = LocalLessonStudyRepository(database, now = { StartedAt })
            study.markStudied("lesson_a")
            study.markStudied("lesson_b")
            LocalSavedQuestionRepository(database, now = { StartedAt }).save("question_b")
            block(database)
        }
    }

    private suspend fun withTestDatabase(block: suspend (CurriculumDatabase) -> Unit) {
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
                QuestionEntity("question_a", "topic", "subtopic", "Question A?", "SINGLE", "FOUNDATION", "Explanation A.", "ACTIVE", sortOrder = 0),
                QuestionEntity("question_b", "topic", "subtopic", "Question B?", "SINGLE", "FOUNDATION", "Explanation B.", "ACTIVE", sortOrder = 1),
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
}

private val StartedAt = Instant.fromEpochMilliseconds(1_700_000_000_000)
private val CompletedAt = Instant.fromEpochMilliseconds(1_700_000_060_000)

private fun answered(questionId: String, vararg answerIds: String, isCorrect: Boolean) =
    QuestionAttempt(
        questionId = questionId,
        answerState = QuestionAnswerState.Answered(answerIds.toSet(), isCorrect = isCorrect),
    )

private val Completed = TestAttempt(
    id = "completed",
    config = AssessmentConfig.Mixed(questionCount = 2),
    questionAttempts = listOf(
        answered("question_a", "question_a_a", isCorrect = true),
        answered("question_b", "question_b_a", "question_b_b", isCorrect = false),
    ),
    status = AssessmentStatus.COMPLETED,
    startedAt = StartedAt,
    completedAt = CompletedAt,
    score = AssessmentScore(totalQuestions = 2, correctAnswers = 1),
)

private val InProgress = TestAttempt(
    id = "in_progress",
    config = AssessmentConfig.Mixed(questionCount = 1),
    questionAttempts = listOf(answered("question_a", "question_a_b", isCorrect = false)),
    status = AssessmentStatus.IN_PROGRESS,
    startedAt = StartedAt,
)
