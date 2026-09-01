package org.artkachenko.kmp_learning_app.progress

internal sealed interface ProgressUiState {
    data object Loading : ProgressUiState

    data object Empty : ProgressUiState

    data object Error : ProgressUiState

    /**
     * The dashboard answers three separate questions, and the fields below are deliberately not
     * interchangeable: [percentage] and the counts beside it are all-time, [coverage] describes the
     * current ACTIVE question bank, and [recentPerformance] describes only the latest few
     * assessments. Each is mapped straight from the domain snapshot so none can drift into another.
     */
    data class Content(
        val completedAttemptCount: Int,
        val answeredQuestionCount: Int,
        val correctAnswerCount: Int,
        val percentage: Double,
        val coverage: ProgressCoverageUiModel,
        val recentPerformance: ProgressRecentPerformanceUiModel?,
        val unresolvedMistakeCount: Int,
        val weakAreas: List<WeakAreaUiModel>,
        val topics: List<ProgressTopicUiModel>,
        val history: List<CompletedAttemptUiModel>,
    ) : ProgressUiState
}

/**
 * How much of the CURRENT ACTIVE question bank the learner has encountered.
 *
 * The raw counts travel alongside the percentage because the percentage alone is not interpretable:
 * a learner needs the denominator to understand why coverage and accuracy can differ so widely, and
 * because the denominator is the current bank, publishing new questions legitimately lowers it.
 */
internal data class ProgressCoverageUiModel(
    val attemptedQuestionCount: Int,
    val totalQuestionCount: Int,
    /** `null` when there is no ACTIVE curriculum at all: 0/0 is not 0% covered. */
    val percentage: Double?,
)

/**
 * Performance across the latest completed assessments, kept apart from the all-time figures above.
 *
 * [percentage] is the domain's question-weighted accuracy for the whole window, never the mean of
 * the attempt percentages in [ProgressRecentTrendUiModel.Available].
 */
internal data class ProgressRecentPerformanceUiModel(
    val attemptCount: Int,
    val answeredQuestionCount: Int,
    val correctAnswerCount: Int,
    val percentage: Double,
    val trend: ProgressRecentTrendUiModel,
)

/**
 * Whether the recent window can be drawn as a trajectory.
 *
 * The attempt series lives inside [Available] rather than beside this flag so that a chart cannot be
 * rendered from a window the domain considers too short to be a trend.
 */
internal sealed interface ProgressRecentTrendUiModel {
    data class InsufficientHistory(
        val requiredAttemptCount: Int,
    ) : ProgressRecentTrendUiModel

    /** Attempts oldest -> newest, in exactly the order the domain produced them. */
    data class Available(
        val attempts: List<ProgressRecentAttemptUiModel>,
    ) : ProgressRecentTrendUiModel
}

internal data class ProgressRecentAttemptUiModel(
    val attemptId: String,
    val percentage: Double,
)

internal enum class WeakAreaType {
    TOPIC,
    SUBTOPIC,
}

internal data class WeakAreaUiModel(
    val type: WeakAreaType,
    val stableId: String,
    val title: String?,
    val subtitle: String?,
    val answeredCount: Int,
    val correctCount: Int,
    val percentage: Double,
)

internal data class ProgressTopicUiModel(
    val topicId: String,
    val topicName: String?,
    val answeredCount: Int,
    val correctCount: Int,
    val percentage: Double,
)

internal enum class CompletedAssessmentType {
    FOCUSED,
    MIXED,
}

internal sealed interface FocusedScopeUiModel {
    data class Topic(
        val topicName: String?,
    ) : FocusedScopeUiModel

    data class Subtopic(
        val topicName: String?,
        val subtopicName: String?,
    ) : FocusedScopeUiModel
}

internal data class CompletedAttemptUiModel(
    val attemptId: String,
    val assessmentType: CompletedAssessmentType,
    val focusedScope: FocusedScopeUiModel?,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val percentage: Double,
    val completedAtText: String,
)
