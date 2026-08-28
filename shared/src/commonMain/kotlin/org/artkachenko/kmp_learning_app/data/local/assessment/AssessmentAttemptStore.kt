package org.artkachenko.kmp_learning_app.data.local.assessment

import androidx.room3.withReadTransaction
import androidx.room3.withWriteTransaction
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase

internal class AssessmentAttemptStore(
    private val database: CurriculumDatabase,
) {
    suspend fun save(attempt: TestAttempt) {
        val snapshot = attempt.toPersistenceSnapshot()
        val dao = database.assessmentAttemptDao()

        database.withWriteTransaction {
            dao.upsertTestAttempt(snapshot.testAttempt)
            dao.deleteSelectedAnswersForAttempt(attempt.id)
            dao.deleteQuestionAttemptsForAttempt(attempt.id)
            dao.upsertQuestionAttempts(snapshot.questionAttempts)
            dao.upsertSelectedAnswers(snapshot.selectedAnswers)
        }
    }

    suspend fun getById(attemptId: String): TestAttempt? {
        val dao = database.assessmentAttemptDao()

        return database.withReadTransaction {
            val attempt = dao.getTestAttemptById(attemptId) ?: return@withReadTransaction null
            toDomainTestAttempt(
                attempt = attempt,
                questionAttempts = dao.getQuestionAttemptsForAttempt(attemptId),
                selectedAnswers = dao.getSelectedAnswersForAttempt(attemptId),
            )
        }
    }

    suspend fun getCompletedAttempts(): List<TestAttempt> {
        val dao = database.assessmentAttemptDao()

        return database.withReadTransaction {
            val attempts = dao.getCompletedTestAttempts(AssessmentStatus.COMPLETED.name)
            if (attempts.isEmpty()) return@withReadTransaction emptyList()

            val attemptIds = attempts.map { it.id }
            val questionAttemptsByAttempt = dao.getQuestionAttemptsForAttempts(attemptIds)
                .groupBy { it.testAttemptId }
            val selectedAnswersByAttempt = dao.getSelectedAnswersForAttempts(attemptIds)
                .groupBy { it.testAttemptId }

            attempts.map { attempt ->
                toDomainTestAttempt(
                    attempt = attempt,
                    questionAttempts = questionAttemptsByAttempt[attempt.id].orEmpty(),
                    selectedAnswers = selectedAnswersByAttempt[attempt.id].orEmpty(),
                )
            }
        }
    }
}
