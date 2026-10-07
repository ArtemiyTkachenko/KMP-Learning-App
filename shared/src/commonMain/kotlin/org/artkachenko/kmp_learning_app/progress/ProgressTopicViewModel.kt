package org.artkachenko.kmp_learning_app.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibility
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibilityStateHolder
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.learning_progress.QuestionCoverage
import org.artkachenko.kmp_learning_app.learning_progress.SubtopicCoverage
import org.artkachenko.kmp_learning_app.learning_progress.SubtopicPerformance

/**
 * One Topic's performance, selected from the visible progress snapshot.
 *
 * The snapshot already excludes hidden Topics, but selecting from it alone would make a hidden
 * Topic look like [ProgressTopicUiState.Empty] — "no observations yet" — which is a false statement
 * about a destination the learner cannot currently see. The route's Topic is therefore checked
 * against the learner's [CurriculumVisibility] first and a hidden one is
 * [ProgressTopicUiState.Unavailable]. Visibility is observed while this screen is alive, because a
 * Progress drill-down can be parked in its area's stack while Settings changes it.
 *
 * [readiness] is not part of [uiState]. It is the dashboard's own row for this Topic, read from the
 * live [ProgressStateHolder] state ([dashboardState]), so the block cannot disagree with the row the
 * learner tapped and follows every refresh the dashboard does — a completed assessment, a visibility
 * change, a mistake falling due, a new target — without a second readiness derivation. It is
 * separate because it exists even where [uiState] is `Empty`: a Topic with no answers yet still has
 * a readiness, `NOT_ENOUGH_EVIDENCE`, and still has something to do about it.
 */
internal class ProgressTopicViewModel(
    private val topicId: String,
    private val learningProgressService: LearningProgressService,
    private val visibilityStateHolder: CurriculumVisibilityStateHolder,
    dashboardState: StateFlow<ProgressUiState>,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ProgressTopicUiState>(ProgressTopicUiState.Loading)
    val uiState: StateFlow<ProgressTopicUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    /**
     * `null` while the dashboard has no readiness for this Topic: no target chosen, nothing at the
     * target's levels to measure, the Topic hidden, or the dashboard not loaded.
     */
    val readiness: StateFlow<ProgressTopicReadinessUiModel?> = dashboardState
        .map { state ->
            val readiness = (state as? ProgressUiState.Content)?.readiness
            (readiness as? ProgressReadinessUiModel.Assessed)?.topics?.firstOrNull { it.topicId == topicId }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The visibility the newest load was made under; the replayed startup value is ignored. */
    private var loadVisibility: CurriculumVisibility? = null

    init {
        require(topicId.isNotBlank()) { "topicId must not be blank." }
        load()
        viewModelScope.launch {
            visibilityStateHolder.visibility.collect { visibility ->
                if (visibility != loadVisibility) load()
            }
        }
    }

    fun retry() {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        val visibility = visibilityStateHolder.visibility.value
        loadVisibility = visibility
        // Settled without a read: the Topic ID is the route's, and visibility is decided by it alone.
        if (!visibility.isTopicVisible(topicId)) {
            _uiState.value = ProgressTopicUiState.Unavailable
            return
        }
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
