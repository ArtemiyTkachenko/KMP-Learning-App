package org.artkachenko.kmp_learning_app.mistake_review

import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
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
    suspend fun load(): List<UnresolvedMistake> {
        // getCompletedAttempts() is contractually completed-only and already ordered newest first
        // (completedAt DESC, startedAt DESC, id ASC). The status filter mirrors the same defensive
        // check LearningProgressService applies; the order is consumed as-is and never re-sorted.
        val completedAttempts = assessmentRepository.getCompletedAttempts()
            .filter { it.status == AssessmentStatus.COMPLETED }

        val seenQuestionIds = mutableSetOf<String>()
        val candidates = mutableListOf<Candidate>()

        for (attempt in completedAttempts) {
            for (questionAttempt in attempt.questionAttempts) {
                // Newest attempt first means the FIRST occurrence seen is the latest one; every
                // older occurrence of the same Question is irrelevant to its current state.
                if (!seenQuestionIds.add(questionAttempt.questionId)) continue

                val answered = questionAttempt.answerState as QuestionAnswerState.Answered
                // Persisted correctness only. Re-deriving it from the current correctAnswerIds
                // would rewrite history whenever authored answers change.
                if (!answered.isCorrect) {
                    candidates += Candidate(attempt.id, questionAttempt)
                }
            }
        }

        // Review content is reconstructed only for unresolved candidates, never for every
        // historical occurrence.
        return candidates.map { candidate ->
            UnresolvedMistake(
                questionId = candidate.questionAttempt.questionId,
                sourceAttemptId = candidate.sourceAttemptId,
                reviewItem = assessmentReviewLoader.loadQuestion(candidate.questionAttempt),
            )
        }
    }
}

private data class Candidate(
    val sourceAttemptId: String,
    val questionAttempt: QuestionAttempt,
)
