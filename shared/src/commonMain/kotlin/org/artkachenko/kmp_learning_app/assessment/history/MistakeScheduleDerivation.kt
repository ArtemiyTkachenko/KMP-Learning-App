package org.artkachenko.kmp_learning_app.assessment.history

import kotlin.time.Instant
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.learning_progress.MistakeReviewPolicy

/**
 * The single definition of which Questions are unresolved mistakes, and when each is next due.
 *
 * Each Question's completed occurrences are replayed oldest first, by the attempt's `completedAt`,
 * through the [MistakeReviewPolicy] ladder:
 *
 * - its first incorrect occurrence puts it on the schedule; a Question never answered wrong is
 *   never scheduled;
 * - any incorrect occurrence puts it back at stage 0, due one ladder step later;
 * - a correct occurrence advances it only when [MistakeReviewPolicy.counts] — on or after the due
 *   time, less the grace — and the last step's counted answer resolves it;
 * - a correct occurrence before that is recorded in history but changes nothing here, which is
 *   what keeps an immediate re-practice, after reading the explanation, from resolving anything;
 * - a resolved Question answered wrong again re-enters at stage 0.
 *
 * Pure: the output depends only on [derive]'s attempts, and the current time enters only through
 * [ScheduledMistake.isDue]. Nothing is persisted, and current curriculum content is deliberately
 * absent because historical state must survive answer-key changes, deprecation, and missing
 * content. Correctness is the persisted one, never re-graded.
 */
internal object MistakeScheduleDerivation {
    /**
     * Every scheduled (unresolved) Question, soonest due first; Questions due at the same instant
     * are ordered newest mistake first.
     *
     * [attempts] may arrive in any order — the repository's newest first included — and
     * non-completed attempts are ignored. Attempts completed at the same instant are replayed in the
     * reverse of the given order, which for the repository's newest-first order is oldest first.
     */
    fun derive(attempts: List<TestAttempt>): List<ScheduledMistake> {
        val chronological = attempts
            .filter { it.status == AssessmentStatus.COMPLETED }
            .asReversed()
            // Stable, so equal completion times keep the reversed input order.
            .sortedBy { it.completedAt }

        val schedules = linkedMapOf<String, ScheduleState>()
        for (attempt in chronological) {
            val answeredAt = checkNotNull(attempt.completedAt)
            for (questionAttempt in attempt.questionAttempts) {
                val answered = questionAttempt.answerState as QuestionAnswerState.Answered
                val questionId = questionAttempt.questionId
                val current = schedules[questionId]
                if (!answered.isCorrect) {
                    schedules[questionId] = ScheduleState(
                        stage = 0,
                        dueAt = answeredAt + MistakeReviewPolicy.Ladder[0],
                        latestMistake = MistakeOccurrence(attempt.id, answeredAt, questionAttempt),
                    )
                    continue
                }
                if (current == null || !MistakeReviewPolicy.counts(answeredAt, current.dueAt)) continue
                val stage = current.stage + 1
                if (stage == MistakeReviewPolicy.ResolveAfterCountedCorrect) {
                    schedules.remove(questionId)
                } else {
                    schedules[questionId] = current.copy(
                        stage = stage,
                        dueAt = answeredAt + MistakeReviewPolicy.Ladder[stage],
                    )
                }
            }
        }

        return schedules.entries
            .map { (questionId, state) ->
                ScheduledMistake(questionId, state.stage, state.dueAt, state.latestMistake)
            }
            // Newest mistake first, then soonest due first; the stable sort keeps the first order
            // among Questions due at the same instant.
            .sortedByDescending { it.latestMistake.answeredAt }
            .sortedBy { it.dueAt }
    }

    private data class ScheduleState(
        val stage: Int,
        val dueAt: Instant,
        val latestMistake: MistakeOccurrence,
    )
}

/**
 * One unresolved mistake's place on the review ladder.
 *
 * [stage] is the number of counted correct answers since the latest mistake, and [dueAt] the
 * ladder's due time for the next one. [latestMistake] is the most recent *incorrect* occurrence —
 * the answer a review card shows — rather than a later early correct answer that counted for
 * nothing.
 */
internal data class ScheduledMistake(
    val questionId: String,
    val stage: Int,
    val dueAt: Instant,
    val latestMistake: MistakeOccurrence,
) {
    /** The earliest moment a correct answer counts, which is when the entry is presented as due. */
    val dueFrom: Instant get() = dueAt - MistakeReviewPolicy.Grace

    /** Whether a correct answer at [now] would advance this schedule. */
    fun isDue(now: Instant): Boolean = MistakeReviewPolicy.counts(now, dueAt)
}

/** One persisted incorrect occurrence of a stable Question ID. */
internal data class MistakeOccurrence(
    val sourceAttemptId: String,
    val answeredAt: Instant,
    val questionAttempt: QuestionAttempt,
)
