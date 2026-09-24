package org.artkachenko.kmp_learning_app.topic_study.focused_result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeController
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeCreated
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeService
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeState
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.assessment_review.isAvailableFor
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionStateHolder
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionsState

internal class FocusedResultViewModel(
    private val attemptId: String,
    private val assessmentRepository: AssessmentRepository,
    private val assessmentReviewLoader: AssessmentReviewLoader,
    assessmentRetakeService: AssessmentRetakeService,
    private val savedQuestionStateHolder: SavedQuestionStateHolder,
) : ViewModel() {
    private val _uiState = MutableStateFlow<FocusedResultUiState>(FocusedResultUiState.Loading)
    val uiState: StateFlow<FocusedResultUiState> = _uiState.asStateFlow()

    /**
     * The shared retake state machine, scoped to this screen's lifetime.
     *
     * Constructed here rather than injected because it is per-attempt and per-screen: it exists for
     * as long as this result is open, and its work belongs to [viewModelScope] so leaving the
     * destination cancels a retake still in flight.
     */
    private val retake = AssessmentRetakeController(
        sourceAttemptId = attemptId,
        retakeService = assessmentRetakeService,
        scope = viewModelScope,
    )

    /** Observed beside [uiState]; see [FocusedResultUiState] for why it is not part of it. */
    val retakeState: StateFlow<AssessmentRetakeState> = retake.state
    val retakeEvents: Flow<AssessmentRetakeCreated> = retake.createdAttempts

    /**
     * A second, independent state stream: the result is never held back waiting for saved state, and
     * a saved-state failure never becomes [FocusedResultUiState.Error].
     */
    val savedQuestions: StateFlow<SavedQuestionsState> = savedQuestionStateHolder.state

    init {
        require(attemptId.isNotBlank()) { "attemptId must not be blank." }
        load()
        savedQuestionStateHolder.refresh()
    }

    fun retry() {
        if (_uiState.value != FocusedResultUiState.Error) return
        load()
        savedQuestionStateHolder.refresh()
    }

    /**
     * Saves or unsaves a Question of this result.
     *
     * Ignores an ID this result does not currently show as available content, so the mutation
     * boundary cannot persist a Question the learner has no way to review — the state already knows
     * which items resolved, so no curriculum lookup is needed to check.
     */
    fun toggleSaved(questionId: String) {
        val content = uiState.value as? FocusedResultUiState.Content ?: return
        if (content.questions.none { it.isAvailableFor(questionId) }) return
        savedQuestionStateHolder.toggleSaved(questionId)
    }

    /**
     * There has to be a result to practise again before one can be asked for; everything after that
     * check is the shared retake state machine's.
     */
    fun repeatPractice() {
        if (uiState.value !is FocusedResultUiState.Content) return
        retake.start()
    }

    fun onRetakeEventHandled(attemptId: String) = retake.onCreatedAttemptHandled(attemptId)

    private fun load() {
        _uiState.value = FocusedResultUiState.Loading
        viewModelScope.launch {
            runCatching {
                val attempt = assessmentRepository.getById(attemptId)
                    ?: return@runCatching FocusedResultUiState.AttemptNotFound
                if (attempt.status != AssessmentStatus.COMPLETED) {
                    return@runCatching FocusedResultUiState.NotCompleted
                }
                val score = requireNotNull(attempt.score)
                val questions = assessmentReviewLoader.loadQuestions(attempt)
                FocusedResultUiState.Content(
                    attemptId = attempt.id,
                    totalQuestions = score.totalQuestions,
                    correctAnswers = score.correctAnswers,
                    percentage = score.percentage,
                    questions = questions,
                )
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { failure ->
                if (failure is CancellationException) throw failure
                _uiState.value = FocusedResultUiState.Error
            }
        }
    }
}
