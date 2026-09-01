package org.artkachenko.kmp_learning_app.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.learning_progress.QuestionCoverage
import org.artkachenko.kmp_learning_app.learning_progress.SubtopicCoverage
import org.artkachenko.kmp_learning_app.learning_progress.SubtopicPerformance

internal class ProgressTopicViewModel(
    private val topicId: String,
    private val learningProgressService: LearningProgressService,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ProgressTopicUiState>(ProgressTopicUiState.Loading)
    val uiState: StateFlow<ProgressTopicUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        require(topicId.isNotBlank()) { "topicId must not be blank." }
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.value = ProgressTopicUiState.Loading
        loadJob = viewModelScope.launch {
            try {
                _uiState.value = loadState()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                _uiState.value = ProgressTopicUiState.Error
            }
        }
    }

    private suspend fun loadState(): ProgressTopicUiState {
        val snapshot = learningProgressService.load()
        // The snapshot already carries observation-based performance in a deterministic order.
        // Selecting from it keeps this screen consistent with the dashboard and avoids
        // recalculating anything from the current ACTIVE curriculum.
        val topic = snapshot.topics.firstOrNull { it.topicId == topicId }
            ?: return ProgressTopicUiState.Empty

        // This screen stays analytics-focused: only observed Subtopics get rows, and coverage joins
        // onto them by stable ID rather than adding every unseen Subtopic as a 0/N row. Browsing the
        // whole curriculum is what Topic Detail is for.
        val subtopicCoverage = snapshot.subtopicCoverage.associateBy(SubtopicCoverage::subtopicId)

        return ProgressTopicUiState.Content(
            topicName = topic.topicName,
            answeredCount = topic.answeredCount,
            correctCount = topic.correctCount,
            percentage = topic.percentage,
            isWeak = topic.isWeak,
            coverage = toUiModel(snapshot.topicCoverage.firstOrNull { it.topicId == topicId }),
            subtopics = snapshot.subtopics
                .filter { it.topicId == topicId }
                .map { toUiModel(it, subtopicCoverage[it.subtopicId]) },
        )
    }

    private fun toUiModel(
        subtopic: SubtopicPerformance,
        coverage: SubtopicCoverage?,
    ): ProgressSubtopicUiModel =
        ProgressSubtopicUiModel(
            subtopicId = subtopic.subtopicId,
            subtopicName = subtopic.subtopicName,
            answeredCount = subtopic.answeredCount,
            correctCount = subtopic.correctCount,
            percentage = subtopic.percentage,
            isWeak = subtopic.isWeak,
            coverage = toUiModel(coverage),
        )

    /**
     * `null` when there is nothing of the current bank in scope — a Topic answered entirely on
     * Questions that have since been retired has no coverage to report, which is a different
     * statement from having explored none of it. Historical performance above is untouched either
     * way: the two are separate figures, and zero coverage never erases an accuracy.
     */
    private fun toUiModel(coverage: QuestionCoverage?): ProgressCoverageUiModel? {
        if (coverage == null || coverage.totalQuestionCount == 0) return null
        return ProgressCoverageUiModel(
            attemptedQuestionCount = coverage.attemptedQuestionCount,
            totalQuestionCount = coverage.totalQuestionCount,
            percentage = coverage.percentage,
        )
    }
}
