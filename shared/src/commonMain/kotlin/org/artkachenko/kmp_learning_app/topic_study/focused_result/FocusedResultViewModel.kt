package org.artkachenko.kmp_learning_app.topic_study.focused_result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeResult
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeService
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader

internal class FocusedResultViewModel(
    private val attemptId: String,
    private val assessmentRepository: AssessmentRepository,
    private val assessmentReviewLoader: AssessmentReviewLoader,
    private val assessmentRetakeService: AssessmentRetakeService,
) : ViewModel() {
    private val _uiState = MutableStateFlow<FocusedResultUiState>(FocusedResultUiState.Loading)
    val uiState: StateFlow<FocusedResultUiState> = _uiState.asStateFlow()
    private val _events = Channel<FocusedResultEvent>(Channel.BUFFERED)
    val events: Flow<FocusedResultEvent> = _events.receiveAsFlow()

    init {
        require(attemptId.isNotBlank()) { "attemptId must not be blank." }
        load()
    }

    fun retry() {
        load()
    }

    fun repeatPractice() {
        val currentState = uiState.value as? FocusedResultUiState.Content ?: return
        if (currentState.repeatPracticeState == RepeatPracticeState.Creating) return
        _uiState.value = currentState.copy(repeatPracticeState = RepeatPracticeState.Creating)
        viewModelScope.launch {
            runCatching { assessmentRetakeService.createRetake(attemptId) }
                .onSuccess { result ->
                    when (result) {
                        is AssessmentRetakeResult.Created -> {
                            _uiState.value = currentState.copy(repeatPracticeState = RepeatPracticeState.Idle)
                            _events.send(FocusedResultEvent.RetakeCreated(result.session.attempt.id))
                        }
                        AssessmentRetakeResult.SourceAttemptNotFound ->
                            setRepeatState(RepeatPracticeState.SourceAttemptNotFound)
                        AssessmentRetakeResult.NoEligibleQuestions ->
                            setRepeatState(RepeatPracticeState.NoEligibleQuestions)
                    }
                }
                .onFailure { setRepeatState(RepeatPracticeState.Error) }
        }
    }

    private fun setRepeatState(state: RepeatPracticeState) {
        val currentState = uiState.value as? FocusedResultUiState.Content ?: return
        _uiState.value = currentState.copy(repeatPracticeState = state)
    }

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
            }.onFailure {
                _uiState.value = FocusedResultUiState.Error
            }
        }
    }
}
