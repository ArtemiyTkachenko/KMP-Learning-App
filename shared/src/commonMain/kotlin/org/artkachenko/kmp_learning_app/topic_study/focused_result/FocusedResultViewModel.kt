package org.artkachenko.kmp_learning_app.topic_study.focused_result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

internal class FocusedResultViewModel(
    private val attemptId: String,
    private val assessmentRepository: AssessmentRepository,
    private val curriculumRepository: CurriculumRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<FocusedResultUiState>(FocusedResultUiState.Loading)
    val uiState: StateFlow<FocusedResultUiState> = _uiState.asStateFlow()

    init {
        require(attemptId.isNotBlank()) { "attemptId must not be blank." }
        load()
    }

    fun retry() {
        load()
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
                val questions = attempt.questionAttempts.map { questionAttempt ->
                    val question = curriculumRepository.getQuestionById(questionAttempt.questionId)
                    if (question == null) {
                        ReviewQuestionItem.Missing(questionAttempt.questionId)
                    } else {
                        val answerState = questionAttempt.answerState as? QuestionAnswerState.Answered
                            ?: error("Completed attempt contains an unanswered question.")
                        ReviewQuestionItem.Available(
                            ReviewQuestionUiModel(
                                questionId = question.id,
                                text = question.text,
                                isCorrect = answerState.isCorrect,
                                answers = question.answers.map { answer ->
                                    ReviewAnswerUiModel(
                                        id = answer.id,
                                        text = answer.text,
                                        wasSelected = answer.id in answerState.selectedAnswerIds,
                                        isCorrectAnswer = answer.id in question.correctAnswerIds,
                                    )
                                },
                                explanation = question.explanation,
                                sources = question.sources.map { source ->
                                    ReviewSourceUiModel(source.title, source.url)
                                },
                            ),
                        )
                    }
                }
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
