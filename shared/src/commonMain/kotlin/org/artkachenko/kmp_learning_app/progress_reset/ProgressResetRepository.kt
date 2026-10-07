package org.artkachenko.kmp_learning_app.progress_reset

/**
 * Deletes the learner's progress: every assessment attempt and every studied-Lesson mark.
 *
 * One operation rather than a delete on [org.artkachenko.kmp_learning_app.assessment.repository
 * .AssessmentRepository] beside one on [org.artkachenko.kmp_learning_app.lesson_study.repository
 * .LessonStudyRepository], because a reset is a single learner decision and must not be able to
 * land half-way: attempts gone while studied marks survive is a state the learner never chose.
 *
 * What is deliberately *not* touched is part of the contract: saved Questions are bookmarks the
 * learner curated rather than progress, preferences are settings, and the curriculum tables are
 * bundled content.
 */
internal interface ProgressResetRepository {
    /**
     * Deletes all attempts — completed and in progress, with their question occurrences and
     * selected answers — and all studied-Lesson records, atomically. A failure propagates and
     * deletes nothing.
     */
    suspend fun deleteLearnerProgress()
}
