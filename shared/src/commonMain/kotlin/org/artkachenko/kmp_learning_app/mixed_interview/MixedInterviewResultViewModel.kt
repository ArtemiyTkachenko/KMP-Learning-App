package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeResult
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeService
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

internal class MixedInterviewResultViewModel(
    private val attemptId: String,
    private val assessmentRepository: AssessmentRepository,
    private val curriculumRepository: CurriculumRepository,
    private val assessmentReviewLoader: AssessmentReviewLoader,
    private val assessmentRetakeService: AssessmentRetakeService,
) : ViewModel() {
    private val _uiState = MutableStateFlow<MixedInterviewResultUiState>(
        MixedInterviewResultUiState.Loading,
    )
    val uiState: StateFlow<MixedInterviewResultUiState> = _uiState.asStateFlow()
    private val _events = Channel<MixedInterviewResultEvent>(Channel.BUFFERED)
    val events: Flow<MixedInterviewResultEvent> = _events.receiveAsFlow()

    init {
        require(attemptId.isNotBlank()) { "attemptId must not be blank." }
        load()
    }

    fun retry() {
        load()
    }

    fun repeatInterview() {
        val currentState = uiState.value as? MixedInterviewResultUiState.Content ?: return
        if (currentState.repeatInterviewState == RepeatInterviewState.Creating) return
        _uiState.value = currentState.copy(repeatInterviewState = RepeatInterviewState.Creating)
        viewModelScope.launch {
            runCatching { assessmentRetakeService.createRetake(attemptId) }
                .onSuccess { result ->
                    when (result) {
                        is AssessmentRetakeResult.Created -> {
                            setRepeatState(RepeatInterviewState.Idle)
                            _events.send(
                                MixedInterviewResultEvent.RetakeCreated(
                                    result.session.attempt.id,
                                ),
                            )
                        }
                        AssessmentRetakeResult.SourceAttemptNotFound ->
                            setRepeatState(RepeatInterviewState.SourceAttemptNotFound)
                        AssessmentRetakeResult.NoEligibleQuestions ->
                            setRepeatState(RepeatInterviewState.NoEligibleQuestions)
                    }
                }
                .onFailure { setRepeatState(RepeatInterviewState.Error) }
        }
    }

    private fun setRepeatState(state: RepeatInterviewState) {
        val currentState = uiState.value as? MixedInterviewResultUiState.Content ?: return
        _uiState.value = currentState.copy(repeatInterviewState = state)
    }

    private fun load() {
        _uiState.value = MixedInterviewResultUiState.Loading
        viewModelScope.launch {
            runCatching { loadResult() }
                .onSuccess { _uiState.value = it }
                .onFailure { _uiState.value = MixedInterviewResultUiState.Error }
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
            topicPerformance = loadTopicPerformance(questions),
            questions = questions,
        )
    }

    private suspend fun loadTopicPerformance(
        questions: List<ReviewQuestionItem>,
    ): List<TopicPerformanceUiModel> {
        val countsByTopic = linkedMapOf<String, TopicCounts>()
        questions.forEach { item ->
            val question = (item as? ReviewQuestionItem.Available)?.question ?: return@forEach
            val counts = countsByTopic.getOrPut(question.topicId) { TopicCounts() }
            counts.questionCount += 1
            if (question.isCorrect) counts.correctCount += 1
        }

        return countsByTopic.map { (topicId, counts) ->
            TopicPerformanceUiModel(
                topicId = topicId,
                topicName = curriculumRepository.getTopicById(topicId)?.name,
                questionCount = counts.questionCount,
                correctCount = counts.correctCount,
                percentage = counts.correctCount.toDouble() / counts.questionCount * 100.0,
            )
        }
    }

    private class TopicCounts(
        var questionCount: Int = 0,
        var correctCount: Int = 0,
    )
}
