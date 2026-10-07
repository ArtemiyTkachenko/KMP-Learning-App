package org.artkachenko.kmp_learning_app.mistake_review

import kotlin.time.Clock
import kotlin.time.Instant
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.history.CompletedAssessmentHistory
import org.artkachenko.kmp_learning_app.assessment.history.MistakeScheduleDerivation
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.assessment_review.ReviewOccurrence

/**
 * Derives the unresolved mistake queue from completed assessment history.
 *
 * Which Questions are unresolved, and which of them are due, is [MistakeScheduleDerivation]'s
 * answer: a mistake leaves the queue only after correct answers spread over the review ladder, and
 * nothing needs to be persisted. This is deliberately different from the occurrence-based
 * aggregation in `LearningProgressService`, which counts every occurrence.
 *
 * History comes from the caller or from [completedHistory], which the application binds to the
 * visible history projection. A hidden Topic's answers are therefore absent before the schedule is
 * replayed, so they neither appear as mistakes nor move a visible Question along its ladder — and
 * this service needs no visibility logic of its own.
 *
 * [now] is injected, as `AssessmentEngine` does, so tests decide which entries are due.
 */
internal class MistakeReviewService(
    private val completedHistory: CompletedAssessmentHistory,
    private val assessmentReviewLoader: AssessmentReviewLoader,
    private val now: () -> Instant = { Clock.System.now() },
) {
    /**
     * [completedAttempts] lets a caller that already holds newest-first completed history reuse it,
     * exactly as [countUnresolved] does, so the shared cache is not re-read per screen.
     */
    suspend fun load(completedAttempts: List<TestAttempt>? = null): List<UnresolvedMistake> {
        // Review content is reconstructed only for scheduled candidates, never for every
        // historical occurrence — and for all of them in one historical read rather than one per
        // mistake, which is what the loader's occurrence-list entry point exists for. The queue
        // keeps the derivation's order: the loader returns one item per occurrence, in order.
        val scheduled = schedule(completedAttempts)
        val evaluatedAt = now()
        val reviewItems = assessmentReviewLoader.loadQuestions(
            scheduled.map {
                ReviewOccurrence(it.latestMistake.sourceAttemptId, it.latestMistake.questionAttempt)
            },
        )
        return scheduled.zip(reviewItems) { mistake, reviewItem ->
            UnresolvedMistake(
                questionId = mistake.questionId,
                sourceAttemptId = mistake.latestMistake.sourceAttemptId,
                reviewItem = reviewItem,
                dueFrom = mistake.dueFrom,
                isDue = mistake.isDue(evaluatedAt),
            )
        }
    }

    /**
     * How many Questions are unresolved — due or coming up — without reconstructing any review
     * content.
     *
     * [completedAttempts] lets a caller that already holds newest-first completed history reuse it
     * rather than reading it again — the progress dashboard would otherwise read and rebuild the
     * whole history a third time on every resume.
     */
    suspend fun countUnresolved(completedAttempts: List<TestAttempt>? = null): Int =
        schedule(completedAttempts).size

    /**
     * How many unresolved Questions are due now: the prompts to act today. An entry that is coming
     * up cannot advance yet, so it is left out.
     */
    suspend fun countDue(completedAttempts: List<TestAttempt>? = null): Int {
        val evaluatedAt = now()
        return schedule(completedAttempts).count { it.isDue(evaluatedAt) }
    }

    private suspend fun schedule(
        completedAttempts: List<TestAttempt>?,
    ) = MistakeScheduleDerivation.derive(
        // Completed history is contractually completed-only and ordered newest first. The shared
        // derivation re-orders by completion time itself and excludes any non-completed attempt a
        // caller supplies.
        completedAttempts ?: completedHistory.completedAttempts(),
    )
}
