package org.artkachenko.kmp_learning_app.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.learning_progress.TopicPerformance
import org.artkachenko.kmp_learning_app.learning_progress.WeakArea

internal class ProgressViewModel(
    private val learningProgressService: LearningProgressService,
    private val assessmentRepository: AssessmentRepository,
    private val curriculumRepository: CurriculumRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ProgressUiState>(ProgressUiState.Loading)
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    fun refresh() {
        loadJob?.cancel()
        _uiState.value = ProgressUiState.Loading
        loadJob = viewModelScope.launch {
            try {
                _uiState.value = loadState()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                _uiState.value = ProgressUiState.Error
            }
        }
    }

    private suspend fun loadState(): ProgressUiState {
        val snapshot = learningProgressService.load()
        val completedAttempts = assessmentRepository.getCompletedAttempts()
        if (snapshot.completedAttemptCount == 0) return ProgressUiState.Empty

        return ProgressUiState.Content(
            completedAttemptCount = snapshot.completedAttemptCount,
            answeredQuestionCount = snapshot.answeredQuestionCount,
            correctAnswerCount = snapshot.correctAnswerCount,
            percentage = snapshot.percentage,
            weakAreas = snapshot.weakAreas.map(::toUiModel),
            topics = snapshot.topics.map(::toUiModel),
            history = mapHistory(completedAttempts),
        )
    }

    private fun toUiModel(area: WeakArea): WeakAreaUiModel =
        when (area) {
            is WeakArea.Topic -> WeakAreaUiModel(
                type = WeakAreaType.TOPIC,
                stableId = area.performance.topicId,
                title = area.performance.topicName,
                subtitle = null,
                answeredCount = area.answeredCount,
                correctCount = area.correctCount,
                percentage = area.percentage,
            )
            is WeakArea.Subtopic -> WeakAreaUiModel(
                type = WeakAreaType.SUBTOPIC,
                stableId = area.performance.subtopicId,
                title = area.performance.subtopicName,
                subtitle = area.performance.topicName,
                answeredCount = area.answeredCount,
                correctCount = area.correctCount,
                percentage = area.percentage,
            )
        }

    private fun toUiModel(topic: TopicPerformance): ProgressTopicUiModel =
        ProgressTopicUiModel(
            topicId = topic.topicId,
            topicName = topic.topicName,
            answeredCount = topic.answeredCount,
            correctCount = topic.correctCount,
            percentage = topic.percentage,
        )

    private suspend fun mapHistory(
        attempts: List<TestAttempt>,
    ): List<CompletedAttemptUiModel> {
        val topicsById = mutableMapOf<String, Topic?>()
        val subtopicsById = mutableMapOf<String, Subtopic?>()

        return attempts.map { attempt ->
            val score = requireNotNull(attempt.score)
            val completedAt = requireNotNull(attempt.completedAt)
            when (val config = attempt.config) {
                is AssessmentConfig.Mixed -> CompletedAttemptUiModel(
                    attemptId = attempt.id,
                    assessmentType = CompletedAssessmentType.MIXED,
                    focusedScope = null,
                    totalQuestions = score.totalQuestions,
                    correctAnswers = score.correctAnswers,
                    percentage = score.percentage,
                    completedAtText = completedAt.toString(),
                )
                is AssessmentConfig.Focused -> CompletedAttemptUiModel(
                    attemptId = attempt.id,
                    assessmentType = CompletedAssessmentType.FOCUSED,
                    focusedScope = when (val scope = config.scope) {
                        is AssessmentScope.Topic -> FocusedScopeUiModel.Topic(
                            topicName = topicsById.getOrLoad(scope.topicId) {
                                curriculumRepository.getTopicById(scope.topicId)
                            }?.name,
                        )
                        is AssessmentScope.Subtopic -> {
                            val subtopic = subtopicsById.getOrLoad(scope.subtopicId) {
                                curriculumRepository.getSubtopicById(scope.subtopicId)
                            }
                            FocusedScopeUiModel.Subtopic(
                                topicName = subtopic?.let {
                                    topicsById.getOrLoad(it.topicId) {
                                        curriculumRepository.getTopicById(it.topicId)
                                    }?.name
                                },
                                subtopicName = subtopic?.name,
                            )
                        }
                    },
                    totalQuestions = score.totalQuestions,
                    correctAnswers = score.correctAnswers,
                    percentage = score.percentage,
                    completedAtText = completedAt.toString(),
                )
            }
        }
    }
}

private suspend fun <K, V> MutableMap<K, V?>.getOrLoad(
    key: K,
    load: suspend () -> V?,
): V? {
    if (containsKey(key)) return this[key]
    return load().also { this[key] = it }
}
