package org.artkachenko.kmp_learning_app.data.local.progress_reset

import androidx.room3.withWriteTransaction
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.progress_reset.ProgressResetRepository

/**
 * The attempt tables and `studied_lesson` live in the same [CurriculumDatabase], so the whole reset
 * is one write transaction and either all four tables are emptied or none is.
 *
 * Children are deleted before their parents because the attempt tables' foreign keys are
 * `NO_ACTION`, the same order [org.artkachenko.kmp_learning_app.data.local.assessment
 * .AssessmentAttemptStore.save] already deletes in. `saved_question`, the curriculum tables and the
 * preference store are not named here at all, which is what keeps them out of a reset.
 */
internal class LocalProgressResetRepository(
    private val database: CurriculumDatabase,
) : ProgressResetRepository {
    override suspend fun deleteLearnerProgress() {
        val attempts = database.assessmentAttemptDao()
        val studiedLessons = database.studiedLessonDao()

        database.withWriteTransaction {
            attempts.deleteAllSelectedAnswers()
            attempts.deleteAllQuestionAttempts()
            attempts.deleteAllTestAttempts()
            studiedLessons.deleteAll()
        }
    }
}
