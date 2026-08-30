package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository

/** The learner's mixed interview record, or null before they have finished one. */
internal data class InterviewHistoryUiModel(
    val attemptCount: Int,
    val latest: InterviewAttemptUiModel,
    val best: InterviewAttemptUiModel,
)

internal data class InterviewAttemptUiModel(
    val attemptId: String,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val percentage: Double,
)

/**
 * Backs the interview start screen with what the learner has already scored.
 *
 * The screen was a call to action and nothing else, which left it mostly empty and gave a returning
 * learner no reason to look at it. The latest and best results say whether starting another
 * interview is worth it, and both open the result they came from.
 */
internal class InterviewStartViewModel(
    private val assessmentRepository: AssessmentRepository,
) : ViewModel() {
    private val _history = MutableStateFlow<InterviewHistoryUiModel?>(null)
    val history: StateFlow<InterviewHistoryUiModel?> = _history.asStateFlow()
    private var loadJob: Job? = null

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                _history.value = loadHistory()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                // The interview can still be started without its history, so a failed read leaves
                // the screen in its first-run shape rather than blocking the only action on it.
                _history.value = null
            }
        }
    }

    private suspend fun loadHistory(): InterviewHistoryUiModel? {
        val attempts = assessmentRepository.getCompletedAttempts()
            .filter { it.status == AssessmentStatus.COMPLETED && it.config is AssessmentConfig.Mixed }
            .mapNotNull(::toUiModel)
        if (attempts.isEmpty()) return null

        // getCompletedAttempts is ordered newest first, which the progress history relies on too.
        return InterviewHistoryUiModel(
            attemptCount = attempts.size,
            latest = attempts.first(),
            best = attempts.maxBy { it.percentage },
        )
    }

    private fun toUiModel(attempt: TestAttempt): InterviewAttemptUiModel? {
        val score = attempt.score ?: return null
        return InterviewAttemptUiModel(
            attemptId = attempt.id,
            correctAnswers = score.correctAnswers,
            totalQuestions = score.totalQuestions,
            percentage = score.percentage,
        )
    }
}
