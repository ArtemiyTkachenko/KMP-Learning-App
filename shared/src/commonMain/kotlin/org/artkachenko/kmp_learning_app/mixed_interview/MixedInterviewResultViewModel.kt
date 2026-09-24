package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeController
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeCreated
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeService
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeState
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.assessment_review.isAvailableFor
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionStateHolder
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionsState

internal class MixedInterviewResultViewModel(
    private val attemptId: String,
    private val assessmentRepository: AssessmentRepository,
    private val curriculumRepository: CurriculumRepository,
    private val assessmentReviewLoader: AssessmentReviewLoader,
    assessmentRetakeService: AssessmentRetakeService,
    private val savedQuestionStateHolder: SavedQuestionStateHolder,
) : ViewModel() {
    private val _uiState = MutableStateFlow<MixedInterviewResultUiState>(
        MixedInterviewResultUiState.Loading,
    )
    val uiState: StateFlow<MixedInterviewResultUiState> = _uiState.asStateFlow()

    /** The same retake state machine the Focused result uses, over the same service. */
    private val retake = AssessmentRetakeController(
        sourceAttemptId = attemptId,
        retakeService = assessmentRetakeService,
        scope = viewModelScope,
    )

    val retakeState: StateFlow<AssessmentRetakeState> = retake.state
    val retakeEvents: Flow<AssessmentRetakeCreated> = retake.createdAttempts

    /**
     * The same app-scoped saved state the Focused result and Mistake Review observe, so a Question
     * saved on one of them is already saved here. Score, topic performance, and retake are derived
     * exactly as before and owe nothing to it.
     */
    val savedQuestions: StateFlow<SavedQuestionsState> = savedQuestionStateHolder.state

    init {
        require(attemptId.isNotBlank()) { "attemptId must not be blank." }
        load()
        savedQuestionStateHolder.refresh()
    }

    fun retry() {
        if (_uiState.value != MixedInterviewResultUiState.Error) return
        load()
        savedQuestionStateHolder.refresh()
    }

    /** Ignores any ID this result does not currently show as available review content. */
    fun toggleSaved(questionId: String) {
        val content = uiState.value as? MixedInterviewResultUiState.Content ?: return
        if (content.questions.none { it.isAvailableFor(questionId) }) return
        savedQuestionStateHolder.toggleSaved(questionId)
    }

    /** As on the Focused result: this decides whether there is anything to repeat, nothing more. */
    fun repeatInterview() {
        if (uiState.value !is MixedInterviewResultUiState.Content) return
        retake.start()
    }

    fun onRetakeEventHandled(attemptId: String) = retake.onCreatedAttemptHandled(attemptId)

    private fun load() {
        _uiState.value = MixedInterviewResultUiState.Loading
        viewModelScope.launch {
            runCatching { loadResult() }
                .onSuccess { _uiState.value = it }
                .onFailure { failure ->
                    if (failure is CancellationException) throw failure
                    _uiState.value = MixedInterviewResultUiState.Error
                }
        }
    }

    private suspend fun loadResult(): MixedInterviewResultUiState {
        val attempt = assessmentRepository.getById(attemptId)
            ?: return MixedInterviewResultUiState.AttemptNotFound
        if (attempt.status != AssessmentStatus.COMPLETED) {
            return MixedInterviewResultUiState.NotCompleted
        }
        if (attempt.config !is AssessmentConfig.Mixed) {
            return MixedInterviewResultUiState.Error
        }

        val score = requireNotNull(attempt.score)
        val questions = assessmentReviewLoader.loadQuestions(attempt)
        return MixedInterviewResultUiState.Content(
            attemptId = attempt.id,
            totalQuestions = score.totalQuestions,
            correctAnswers = score.correctAnswers,
            percentage = score.percentage,
            topicPerformance = nameTopics(questions),
            questions = questions,
        )
    }

    /**
     * Names the Topics of the already-counted breakdown.
     *
     * The counting is [topicAnswerCounts] and needs nothing from here; this adds the one thing it
     * cannot know, at one lookup per distinct Topic rather than per Question.
     */
    private suspend fun nameTopics(
        questions: List<ReviewQuestionItem>,
    ): List<TopicPerformanceUiModel> = questions.topicAnswerCounts().map { counts ->
        TopicPerformanceUiModel(
            topicId = counts.topicId,
            topicName = curriculumRepository.getTopicById(counts.topicId)?.name,
            questionCount = counts.questionCount,
            correctCount = counts.correctCount,
            percentage = counts.percentage,
        )
    }
}
