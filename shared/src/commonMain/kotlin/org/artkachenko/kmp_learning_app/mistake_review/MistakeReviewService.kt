package org.artkachenko.kmp_learning_app.mistake_review

import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.history.UnresolvedMistakeDerivation
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader

/**
 * Derives the unresolved mistake queue from completed assessment history.
 *
 * A Question is unresolved when its MOST RECENT completed occurrence was incorrect, so answering it
 * correctly later resolves it automatically and nothing needs to be persisted. This is deliberately
 * different from the occurrence-based aggregation in `LearningProgressService`, which counts every
 * occurrence rather than only the latest one.
 */
internal class MistakeReviewService(
    private val assessmentRepository: AssessmentRepository,
    private val assessmentReviewLoader: AssessmentReviewLoader,
) {
    /**
     * [completedAttempts] lets a caller that already holds newest-first completed history reuse it,
     * exactly as [countUnresolved] does, so the shared cache is not re-read per screen.
     */
    suspend fun load(completedAttempts: List<TestAttempt>? = null): List<UnresolvedMistake> =
        unresolvedOccurrences(completedAttempts).map { occurrence ->
            UnresolvedMistake(
                questionId = occurrence.questionId,
                sourceAttemptId = occurrence.sourceAttemptId,
                // Review content is reconstructed only for unresolved candidates, never for every
                // historical occurrence.
                reviewItem = assessmentReviewLoader.loadQuestion(
                    attemptId = occurrence.sourceAttemptId,
                    questionAttempt = occurrence.questionAttempt,
                ),
            )
        }

    /**
     * How many Questions are unresolved, without reconstructing any review content.
     *
     * [completedAttempts] lets a caller that already holds newest-first completed history reuse it
     * rather than making the repository read it again — the progress dashboard would otherwise read
     * and rebuild the whole history a third time on every resume.
     */
    suspend fun countUnresolved(completedAttempts: List<TestAttempt>? = null): Int =
        unresolvedOccurrences(completedAttempts).size

    private suspend fun unresolvedOccurrences(
        completedAttempts: List<TestAttempt>? = null,
    ) = UnresolvedMistakeDerivation.derive(
        // getCompletedAttempts() is contractually completed-only and already ordered newest first
        // (completedAt DESC, startedAt DESC, id ASC). The shared derivation consumes that order as
        // given and defensively excludes any non-completed attempt supplied by a caller.
        completedAttempts ?: assessmentRepository.getCompletedAttempts(),
    )
}
