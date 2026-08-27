package org.artkachenko.kmp_learning_app.topic_study.focused_practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSession
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSessionLoadResult
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSessionLoader
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentStartResult
import org.artkachenko.kmp_learning_app.curriculum.Question

internal class FocusedPracticeViewModel(
    private val launch: FocusedPracticeLaunch,
    private val assessmentEngine: AssessmentEngine,
    private val assessmentRepository: AssessmentRepository,
    private val assessmentSessionLoader: AssessmentSessionLoader,
) : ViewModel() {
    private val _uiState = MutableStateFlow<FocusedPracticeUiState>(FocusedPracticeUiState.Loading)
    val uiState: StateFlow<FocusedPracticeUiState> = _uiState.asStateFlow()

    private var session: AssessmentSession? = null
    private var currentQuestionIndex = 0
    private var pendingSelectedAnswerIds: Set<String> = emptySet()

    init {
        startAssessment()
    }

    fun retry() {
        startAssessment()
    }

    fun selectAnswer(answerId: String) {
        val currentState = uiState.value as? FocusedPracticeUiState.Content ?: return
        if (currentState.isSubmitting) return

        val question = session?.questions?.getOrNull(currentQuestionIndex) ?: return
        if (question.answers.none { it.id == answerId }) return

        pendingSelectedAnswerIds = when (currentState.question.selectionMode) {
            AnswerSelectionMode.SINGLE -> setOf(answerId)
            AnswerSelectionMode.MULTIPLE -> pendingSelectedAnswerIds.toMutableSet().let { selectedIds ->
                if (!selectedIds.add(answerId)) selectedIds.remove(answerId)
                selectedIds
            }
        }
        publishContent()
    }

    fun submitAnswer() {
        val currentState = uiState.value as? FocusedPracticeUiState.Content ?: return
        if (currentState.isSubmitting || pendingSelectedAnswerIds.isEmpty()) return

        val currentSession = session ?: return
        val currentQuestion = currentSession.questions.getOrNull(currentQuestionIndex) ?: return
        _uiState.value = currentState.copy(
            isSubmitting = true,
            submissionFailed = false,
        )

        viewModelScope.launch {
            runCatching {
                val updatedSession = assessmentEngine.submitAnswer(
                    session = currentSession,
                    questionId = currentQuestion.id,
                    selectedAnswerIds = pendingSelectedAnswerIds,
                )
                assessmentRepository.save(updatedSession.attempt)
                updatedSession
            }.onSuccess { updatedSession ->
                session = updatedSession
                if (currentQuestionIndex == updatedSession.questions.lastIndex) {
                    _uiState.value = FocusedPracticeUiState.ReadyToComplete(
                        attemptId = updatedSession.attempt.id,
                        totalQuestions = updatedSession.questions.size,
                    )
                } else {
                    currentQuestionIndex += 1
                    pendingSelectedAnswerIds = emptySet()
                    publishContent()
                }
            }.onFailure {
                _uiState.value = currentState.copy(
                    isSubmitting = false,
                    submissionFailed = true,
                )
            }
        }
    }

    fun completeAssessment() {
        val currentState = uiState.value as? FocusedPracticeUiState.ReadyToComplete ?: return
        if (currentState.isCompleting) return

        val originalSession = session ?: return
        _uiState.value = currentState.copy(
            isCompleting = true,
            completionFailed = false,
        )

        viewModelScope.launch {
            runCatching {
                val completedSession = assessmentEngine.complete(originalSession)
                assessmentRepository.save(completedSession.attempt)
                completedSession
            }.onSuccess { completedSession ->
                session = completedSession
                _uiState.value = FocusedPracticeUiState.CompletionSucceeded(
                    attemptId = completedSession.attempt.id,
                )
            }.onFailure {
                _uiState.value = FocusedPracticeUiState.ReadyToComplete(
                    attemptId = originalSession.attempt.id,
                    totalQuestions = originalSession.questions.size,
                    completionFailed = true,
                )
            }
        }
    }

    private fun startAssessment() {
        _uiState.value = FocusedPracticeUiState.Loading
        session = null
        currentQuestionIndex = 0
        pendingSelectedAnswerIds = emptySet()

        viewModelScope.launch {
            runCatching {
                when (val requestedLaunch = launch) {
                    is FocusedPracticeLaunch.New -> startNewAssessment(requestedLaunch.config)
                    is FocusedPracticeLaunch.ExistingAttempt -> loadExistingAttempt(requestedLaunch.attemptId)
                }
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure {
                _uiState.value = FocusedPracticeUiState.Error
            }
        }
    }

    private suspend fun startNewAssessment(config: AssessmentConfig.Focused): FocusedPracticeUiState =
        when (val result = assessmentEngine.start(config)) {
            AssessmentStartResult.NoEligibleQuestions -> FocusedPracticeUiState.NoQuestions
            is AssessmentStartResult.Started -> {
                assessmentRepository.save(result.session.attempt)
                session = result.session
                currentQuestionIndex = 0
                result.session.toContentState()
            }
        }

    private suspend fun loadExistingAttempt(attemptId: String): FocusedPracticeUiState {
        val result = assessmentSessionLoader.load(attemptId)
        val loadedSession = (result as? AssessmentSessionLoadResult.Loaded)?.session
            ?: error("Unable to load focused practice attempt: $result")
        check(loadedSession.attempt.config is AssessmentConfig.Focused) {
            "Focused practice requires a focused assessment attempt."
        }
        session = loadedSession
        currentQuestionIndex = loadedSession.attempt.questionAttempts.indexOfFirst {
            it.answerState is QuestionAnswerState.Unanswered
        }
        if (currentQuestionIndex < 0) {
            return FocusedPracticeUiState.ReadyToComplete(
                attemptId = loadedSession.attempt.id,
                totalQuestions = loadedSession.questions.size,
            )
        }
        return loadedSession.toContentState()
    }

    private fun AssessmentSession.toContentState(): FocusedPracticeUiState.Content {
        val question = questions[currentQuestionIndex]
        return FocusedPracticeUiState.Content(
            attemptId = attempt.id,
            questionNumber = currentQuestionIndex + 1,
            totalQuestions = questions.size,
            question = question.toUiModel(),
            selectedAnswerIds = emptySet(),
            canSubmit = false,
            isSubmitting = false,
            submissionFailed = false,
        )
    }

    private fun publishContent() {
        val currentSession = session ?: return
        val question = currentSession.questions[currentQuestionIndex]
        _uiState.value = FocusedPracticeUiState.Content(
            attemptId = currentSession.attempt.id,
            questionNumber = currentQuestionIndex + 1,
            totalQuestions = currentSession.questions.size,
            question = question.toUiModel(),
            selectedAnswerIds = pendingSelectedAnswerIds,
            canSubmit = pendingSelectedAnswerIds.isNotEmpty(),
            isSubmitting = false,
            submissionFailed = false,
        )
    }

    private fun Question.toUiModel(): FocusedQuestionUiModel =
        FocusedQuestionUiModel(
            id = id,
            text = text,
            answers = answers,
            selectionMode = if (correctAnswerIds.size == 1) {
                AnswerSelectionMode.SINGLE
            } else {
                AnswerSelectionMode.MULTIPLE
            },
        )
}
