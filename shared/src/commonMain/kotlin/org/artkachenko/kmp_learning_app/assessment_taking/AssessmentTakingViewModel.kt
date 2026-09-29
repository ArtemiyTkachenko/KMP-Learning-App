package org.artkachenko.kmp_learning_app.assessment_taking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
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
import org.artkachenko.kmp_learning_app.assessment.session.CompleteAssessment
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibility
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibilityStateHolder

/**
 * Takes one persisted attempt, Question by Question.
 *
 * The learner's curriculum visibility is observed while this screen is alive, because an attempt
 * route can be parked in another area's stack while Settings hides a Topic. The loader decides
 * whether an attempt can be shown at all ([AssessmentSessionLoadResult.ContentUnavailable]); a
 * session already on screen is withdrawn the moment one of its Questions becomes hidden, and loaded
 * again when it is shown. Nothing about the attempt is written for either transition.
 */
internal class AssessmentTakingViewModel(
    private val attemptId: String,
    private val assessmentEngine: AssessmentEngine,
    private val assessmentRepository: AssessmentRepository,
    private val assessmentSessionLoader: AssessmentSessionLoader,
    private val completeAttempt: CompleteAssessment,
    private val visibilityStateHolder: CurriculumVisibilityStateHolder,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AssessmentTakingUiState>(AssessmentTakingUiState.Loading)
    val uiState: StateFlow<AssessmentTakingUiState> = _uiState.asStateFlow()

    private var session: AssessmentSession? = null
    private var currentQuestionIndex = 0
    private var pendingSelectedAnswerIds: Set<String> = emptySet()
    private var loadJob: Job? = null

    /** The visibility the current state was established under; see [observeVisibility]. */
    private var shownVisibility: CurriculumVisibility? = null

    init {
        loadAssessment()
        observeVisibility()
    }

    fun retry() {
        if (_uiState.value != AssessmentTakingUiState.Error) return
        loadAssessment()
    }

    fun selectAnswer(answerId: String) {
        val currentState = uiState.value as? AssessmentTakingUiState.Content ?: return
        if (currentState.isSubmitting || currentState.feedback != null) return

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
        val currentState = uiState.value as? AssessmentTakingUiState.Content ?: return
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
                // Withdrawn while the write was in flight: the answer is stored, but this session is
                // no longer the one on screen, so it must not bring hidden content back.
                if (session !== currentSession) return@onSuccess
                session = updatedSession
                if (isFormativePractice()) {
                    _uiState.value = currentState.copy(
                        selectedAnswerIds = pendingSelectedAnswerIds,
                        isSubmitting = false,
                        feedback = PracticeFeedback(
                            isCorrect = (updatedSession.attempt.questionAttempts[currentQuestionIndex]
                                .answerState as QuestionAnswerState.Answered).isCorrect,
                        ),
                    )
                } else if (currentQuestionIndex == updatedSession.questions.lastIndex) {
                    _uiState.value = AssessmentTakingUiState.ReadyToComplete(
                        attemptId = updatedSession.attempt.id,
                        totalQuestions = updatedSession.questions.size,
                    )
                } else {
                    currentQuestionIndex += 1
                    pendingSelectedAnswerIds = emptySet()
                    publishContent()
                }
            }.onFailure { failure ->
                if (failure is CancellationException) throw failure
                if (session !== currentSession) return@onFailure
                _uiState.value = currentState.copy(
                    isSubmitting = false,
                    submissionFailed = true,
                )
            }
        }
    }

    /** Moves forward only after the learner has seen formative feedback. */
    fun nextQuestion() {
        val currentState = uiState.value as? AssessmentTakingUiState.Content ?: return
        if (currentState.feedback == null) return
        val currentSession = session ?: return
        if (currentQuestionIndex == currentSession.questions.lastIndex) {
            _uiState.value = AssessmentTakingUiState.ReadyToComplete(
                attemptId = currentSession.attempt.id,
                totalQuestions = currentSession.questions.size,
            )
            // Practice has no decision left after its final feedback. Completing here preserves
            // the same persistence/history invalidation path as Interview while removing a
            // redundant "ready to finish" stop between learning and Results.
            completeAssessment()
        } else {
            currentQuestionIndex += 1
            pendingSelectedAnswerIds = emptySet()
            publishContent()
        }
    }

    fun completeAssessment() {
        val currentState = uiState.value as? AssessmentTakingUiState.ReadyToComplete ?: return
        if (currentState.isCompleting) return

        val originalSession = session ?: return
        _uiState.value = currentState.copy(
            isCompleting = true,
            completionFailed = false,
        )

        viewModelScope.launch {
            // Scoring, the durable write, and marking the shared history cache stale are one
            // operation rather than three statements here: the cache behind Progress, the mistake
            // queue, the interview record and the navigation badge must not be able to stay stale
            // because this coroutine was cancelled between the write and the invalidation. See
            // CompleteAssessment.
            runCatching { completeAttempt(originalSession) }.onSuccess { completedSession ->
                if (session !== originalSession) return@onSuccess
                session = completedSession
                _uiState.value = AssessmentTakingUiState.CompletionSucceeded(
                    attemptId = completedSession.attempt.id,
                )
            }.onFailure { failure ->
                if (failure is CancellationException) throw failure
                if (session !== originalSession) return@onFailure
                _uiState.value = AssessmentTakingUiState.ReadyToComplete(
                    attemptId = originalSession.attempt.id,
                    totalQuestions = originalSession.questions.size,
                    completionFailed = true,
                )
            }
        }
    }

    /**
     * Follows the learner's visibility without re-reading on the StateFlow's replayed value.
     *
     * A load in flight is restarted so it cannot land under the old visibility, and an unavailable
     * attempt is loaded again. A session on screen is judged from the Questions it already holds, so
     * hiding needs no read: if any is now hidden the session is dropped, never shortened.
     */
    private fun observeVisibility() {
        viewModelScope.launch {
            visibilityStateHolder.visibility.collect { visibility ->
                if (visibility == shownVisibility) return@collect
                when (_uiState.value) {
                    AssessmentTakingUiState.Loading,
                    AssessmentTakingUiState.Unavailable,
                    -> loadAssessment()

                    is AssessmentTakingUiState.Content,
                    is AssessmentTakingUiState.ReadyToComplete,
                    -> {
                        shownVisibility = visibility
                        val hidesSession = session?.questions
                            ?.any { !visibility.isTopicVisible(it.topicId) } == true
                        if (hidesSession) {
                            session = null
                            pendingSelectedAnswerIds = emptySet()
                            _uiState.value = AssessmentTakingUiState.Unavailable
                        }
                    }

                    else -> shownVisibility = visibility
                }
            }
        }
    }

    private fun loadAssessment() {
        loadJob?.cancel()
        _uiState.value = AssessmentTakingUiState.Loading
        session = null
        currentQuestionIndex = 0
        pendingSelectedAnswerIds = emptySet()
        val visibility = visibilityStateHolder.visibility.value
        shownVisibility = visibility

        loadJob = viewModelScope.launch {
            runCatching { loadExistingAttempt(attemptId, visibility) }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { failure ->
                if (failure is CancellationException) throw failure
                _uiState.value = AssessmentTakingUiState.Error
            }
        }
    }

    private suspend fun loadExistingAttempt(
        attemptId: String,
        visibility: CurriculumVisibility,
    ): AssessmentTakingUiState {
        val loadedSession = when (val result = assessmentSessionLoader.load(attemptId, visibility)) {
            is AssessmentSessionLoadResult.Loaded -> result.session
            AssessmentSessionLoadResult.NotInProgress -> {
                return AssessmentTakingUiState.CompletionSucceeded(attemptId)
            }
            // A valid stored attempt that cannot be shown right now, not a failure to retry.
            AssessmentSessionLoadResult.ContentUnavailable -> return AssessmentTakingUiState.Unavailable
            AssessmentSessionLoadResult.AttemptNotFound,
            is AssessmentSessionLoadResult.MissingQuestion ->
                error("Unable to load assessment attempt: $result")
        }
        session = loadedSession
        currentQuestionIndex = loadedSession.attempt.questionAttempts.indexOfFirst {
            it.answerState is QuestionAnswerState.Unanswered
        }
        if (currentQuestionIndex < 0) {
            return AssessmentTakingUiState.ReadyToComplete(
                attemptId = loadedSession.attempt.id,
                totalQuestions = loadedSession.questions.size,
            )
        }
        return loadedSession.toContentState()
    }

    private fun AssessmentSession.toContentState(): AssessmentTakingUiState.Content {
        val question = questions[currentQuestionIndex]
        return AssessmentTakingUiState.Content(
            attemptId = attempt.id,
            questionNumber = currentQuestionIndex + 1,
            totalQuestions = questions.size,
            question = question.toUiModel(),
            selectedAnswerIds = emptySet(),
            canSubmit = false,
            isSubmitting = false,
            submissionFailed = false,
            feedback = null,
        )
    }

    private fun publishContent() {
        val currentSession = session ?: return
        val question = currentSession.questions[currentQuestionIndex]
        _uiState.value = AssessmentTakingUiState.Content(
            attemptId = currentSession.attempt.id,
            questionNumber = currentQuestionIndex + 1,
            totalQuestions = currentSession.questions.size,
            question = question.toUiModel(),
            selectedAnswerIds = pendingSelectedAnswerIds,
            canSubmit = pendingSelectedAnswerIds.isNotEmpty(),
            isSubmitting = false,
            submissionFailed = false,
            feedback = null,
        )
    }

    private fun Question.toUiModel(): AssessmentQuestionUiModel =
        AssessmentQuestionUiModel(
            id = id,
            text = text,
            answers = answers,
            selectionMode = selectionMode,
            correctAnswerIds = correctAnswerIds,
            explanation = explanation,
        )

    private fun isFormativePractice(): Boolean =
        session?.attempt?.config is AssessmentConfig.Focused
}
