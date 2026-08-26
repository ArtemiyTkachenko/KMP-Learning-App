package org.artkachenko.kmp_learning_app.data.local.assessment.repository

import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.data.local.assessment.AssessmentAttemptStore

internal class LocalAssessmentRepository(
    private val store: AssessmentAttemptStore,
) : AssessmentRepository {
    override suspend fun save(attempt: TestAttempt) {
        store.save(attempt)
    }

    override suspend fun getById(attemptId: String): TestAttempt? =
        store.getById(attemptId)
}
