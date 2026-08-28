package org.artkachenko.kmp_learning_app.assessment.repository

import org.artkachenko.kmp_learning_app.assessment.TestAttempt

internal interface AssessmentRepository {
    suspend fun save(attempt: TestAttempt)

    suspend fun getById(attemptId: String): TestAttempt?

    suspend fun getCompletedAttempts(): List<TestAttempt>
}
