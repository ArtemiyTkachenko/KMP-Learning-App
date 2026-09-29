package org.artkachenko.kmp_learning_app.progress

internal sealed interface ProgressTopicUiState {
    data object Loading : ProgressTopicUiState

    /** The Topic is visible and has no observations yet. */
    data object Empty : ProgressTopicUiState

    /**
     * The Topic is hidden by the learner's curriculum visibility. A settled answer, distinct from
     * [Empty]: nothing is claimed about its history, which returns unchanged when it is shown.
     */
    data object Unavailable : ProgressTopicUiState

    data object Error : ProgressTopicUiState

    /**
     * The counts and [percentage] are all-time and occurrence-based; [coverage] describes the
     * current ACTIVE bank and counts each Question once. Both are mapped straight from the domain
     * snapshot, so the two can differ — which is the point of showing them together.
     */
    data class Content(
        val topicName: String?,
        val answeredCount: Int,
        val correctCount: Int,
        val percentage: Double,
        val isWeak: Boolean,
        /** `null` when this Topic has no ACTIVE questions to cover, not when it has none attempted. */
        val coverage: ProgressCoverageUiModel? = null,
        val subtopics: List<ProgressSubtopicUiModel>,
    ) : ProgressTopicUiState
}

internal data class ProgressSubtopicUiModel(
    val subtopicId: String,
    val subtopicName: String?,
    val answeredCount: Int,
    val correctCount: Int,
    val percentage: Double,
    val isWeak: Boolean,
    /** `null` when this Subtopic has no ACTIVE questions to cover. */
    val coverage: ProgressCoverageUiModel? = null,
)
