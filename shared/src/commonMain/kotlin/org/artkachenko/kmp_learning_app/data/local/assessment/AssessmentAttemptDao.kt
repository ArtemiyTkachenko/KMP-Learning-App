package org.artkachenko.kmp_learning_app.data.local.assessment

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import org.artkachenko.kmp_learning_app.data.local.assessment.entity.QuestionAttemptEntity
import org.artkachenko.kmp_learning_app.data.local.assessment.entity.QuestionAttemptSelectedAnswerEntity
import org.artkachenko.kmp_learning_app.data.local.assessment.entity.TestAttemptEntity

@Dao
internal interface AssessmentAttemptDao {
    @Upsert
    suspend fun upsertTestAttempt(attempt: TestAttemptEntity)

    @Upsert
    suspend fun upsertQuestionAttempts(questionAttempts: List<QuestionAttemptEntity>)

    @Upsert
    suspend fun upsertSelectedAnswers(selectedAnswers: List<QuestionAttemptSelectedAnswerEntity>)

    @Query("DELETE FROM question_attempt_selected_answer WHERE test_attempt_id = :attemptId")
    suspend fun deleteSelectedAnswersForAttempt(attemptId: String)

    @Query("DELETE FROM question_attempt WHERE test_attempt_id = :attemptId")
    suspend fun deleteQuestionAttemptsForAttempt(attemptId: String)

    @Query("SELECT * FROM test_attempt WHERE id = :id")
    suspend fun getTestAttemptById(id: String): TestAttemptEntity?

    @Query(
        """
        SELECT *
        FROM test_attempt
        WHERE status = :completedStatus
        ORDER BY completed_at_epoch_millis DESC, started_at_epoch_millis DESC, id ASC
        """,
    )
    suspend fun getCompletedTestAttempts(completedStatus: String): List<TestAttemptEntity>

    @Query("SELECT * FROM question_attempt WHERE test_attempt_id = :attemptId ORDER BY sort_order")
    suspend fun getQuestionAttemptsForAttempt(attemptId: String): List<QuestionAttemptEntity>

    @Query(
        """
        SELECT *
        FROM question_attempt
        WHERE test_attempt_id IN (:attemptIds)
        ORDER BY test_attempt_id, sort_order
        """,
    )
    suspend fun getQuestionAttemptsForAttempts(attemptIds: List<String>): List<QuestionAttemptEntity>

    @Query(
        """
        SELECT *
        FROM question_attempt_selected_answer
        WHERE test_attempt_id = :attemptId
        ORDER BY question_id, answer_id
        """,
    )
    suspend fun getSelectedAnswersForAttempt(attemptId: String): List<QuestionAttemptSelectedAnswerEntity>

    @Query(
        """
        SELECT *
        FROM question_attempt_selected_answer
        WHERE test_attempt_id IN (:attemptIds)
        ORDER BY test_attempt_id, question_id, answer_id
        """,
    )
    suspend fun getSelectedAnswersForAttempts(
        attemptIds: List<String>,
    ): List<QuestionAttemptSelectedAnswerEntity>

    @Query("SELECT COUNT(*) FROM test_attempt")
    suspend fun countTestAttempts(): Int

    @Query("SELECT COUNT(*) FROM question_attempt")
    suspend fun countQuestionAttempts(): Int

    @Query("SELECT COUNT(*) FROM question_attempt_selected_answer")
    suspend fun countSelectedAnswers(): Int
}
