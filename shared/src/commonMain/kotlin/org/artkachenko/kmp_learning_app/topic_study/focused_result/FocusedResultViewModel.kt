package org.artkachenko.kmp_learning_app.topic_study.focused_result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.history.VisibleHistoryProjection
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeController
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeCreated
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeService
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeState
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.assessment_review.isAvailableFor
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibility
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibilityStateHolder
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionStateHolder
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionsState

/**
 * One completed practice attempt, shown as the learner's current curriculum visibility allows.
 *
 * The stored attempt is projected through [VisibleHistoryProjection] — the rule Progress, interview
 * history, mistakes and recommendations already read — so this result cannot disagree with them: the
 * score is recomputed from persisted correctness over the visible answers, and only visible Questions
 * are reviewed. An attempt with nothing visible is [FocusedResultUiState.Unavailable], not
 * `AttemptNotFound`; it still exists and returns unchanged when its content is shown. Visibility is
 * observed while this screen is alive, because a result can be parked in another area's stack while
 * Settings changes it.
 */
internal class FocusedResultViewModel(
    private val attemptId: String,
    private val assessmentRepository: AssessmentRepository,
    private val curriculumRepository: CurriculumRepository,
    private val assessmentReviewLoader: AssessmentReviewLoader,
    assessmentRetakeService: AssessmentRetakeService,
    private val savedQuestionStateHolder: SavedQuestionStateHolder,
    private val visibilityStateHolder: CurriculumVisibilityStateHolder,
) : ViewModel() {
    private val _uiState = MutableStateFlow<FocusedResultUiState>(FocusedResultUiState.Loading)
    val uiState: StateFlow<FocusedResultUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    /** The visibility the newest load was made under; the replayed startup value is ignored. */
    private var loadVisibility: CurriculumVisibility? = null

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
        viewModelScope.launch {
            visibilityStateHolder.visibility.collect { visibility ->
                if (visibility != loadVisibility) load()
            }
        }
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

    /**
     * Reads the stored attempt and projects it under the current visibility. [loadJob] is cancelled
     * first, so a projection made under an older visibility can never overwrite a newer one.
     */
    private fun load() {
        loadJob?.cancel()
        _uiState.value = FocusedResultUiState.Loading
        val visibility = visibilityStateHolder.visibility.value
        loadVisibility = visibility
        loadJob = viewModelScope.launch {
            runCatching {
                val attempt = assessmentRepository.getById(attemptId)
                    ?: return@runCatching FocusedResultUiState.AttemptNotFound
                if (attempt.status != AssessmentStatus.COMPLETED) {
                    return@runCatching FocusedResultUiState.NotCompleted
                }
                val visible = VisibleHistoryProjection
                    .visibleAttempts(listOf(attempt), visibility, curriculumRepository)
                    .singleOrNull()
                    ?: return@runCatching FocusedResultUiState.Unavailable
                val score = requireNotNull(visible.score)
                val questions = assessmentReviewLoader.loadQuestions(visible)
                FocusedResultUiState.Content(
                    attemptId = attempt.id,
                    totalQuestions = score.totalQuestions,
                    correctAnswers = score.correctAnswers,
                    percentage = score.percentage,
                    questions = questions,
                    hiddenQuestionCount = attempt.questionAttempts.size - visible.questionAttempts.size,
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
