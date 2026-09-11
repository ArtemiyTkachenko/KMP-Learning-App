package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.lifecycle.ViewModel
import kotlin.time.Instant
import kotlinx.coroutines.flow.StateFlow
import org.artkachenko.kmp_learning_app.assessment.TestAttempt

/** The learner's mixed interview record. */
internal data class InterviewHistoryUiModel(
    val attemptCount: Int,
    val latest: InterviewAttemptUiModel,
    val best: InterviewAttemptUiModel,
)

/**
 * [completedAt] is the domain instant, never pre-formatted text: when a result reads as "Today" is a
 * question about the reader's zone and the reader's clock, so it is answered in the composable. See
 * `ui/time/TimestampText.kt`.
 */
internal data class InterviewAttemptUiModel(
    val attemptId: String,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val percentage: Double,
    val completedAt: Instant,
)

/**
 * Backs the interview start screen with what the learner has already scored.
 *
 * The screen was a call to action and nothing else, which left it mostly empty and gave a returning
 * learner no reason to look at it. The latest and best results say whether starting another
 * interview is worth it, and both open the result they came from.
 *
 * The record itself is held by [InterviewHistoryStateHolder], which outlives this ViewModel: the
 * navigation entry destroys the ViewModel on a tab switch, and rebuilding the record here meant the
 * card was absent for as long as the read took and then appeared under the learner.
 */
internal class InterviewStartViewModel(
    stateHolder: InterviewHistoryStateHolder,
) : ViewModel() {
    val history: StateFlow<InterviewHistoryUiState> = stateHolder.state
}

/**
 * Null when the attempt carries no score or no completion time, both of which a completed attempt
 * always does — the invariants live on `TestAttempt`. Returning null rather than asserting keeps one
 * malformed persisted row from taking the whole record away.
 */
internal fun toInterviewAttemptUiModel(attempt: TestAttempt): InterviewAttemptUiModel? {
    val score = attempt.score ?: return null
    val completedAt = attempt.completedAt ?: return null
    return InterviewAttemptUiModel(
        attemptId = attempt.id,
        correctAnswers = score.correctAnswers,
        totalQuestions = score.totalQuestions,
        percentage = score.percentage,
        completedAt = completedAt,
    )
}
