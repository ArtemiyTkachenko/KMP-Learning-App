package org.artkachenko.kmp_learning_app.progress

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistory
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.learning_progress.CurriculumCoverage
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.learning_progress.RecentPerformance
import org.artkachenko.kmp_learning_app.learning_progress.RecentTrendAvailability
import org.artkachenko.kmp_learning_app.learning_progress.TopicPerformance
import org.artkachenko.kmp_learning_app.learning_progress.WeakArea
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService

/**
 * App-scoped progress dashboard state, derived from the shared history cache.
 *
 * This holds the derived state rather than the ViewModel doing so, because the navigation entry
 * destroys the ViewModel on a tab switch: the dashboard was rebuilt from nothing on every visit and
 * showed a spinner over figures the app had loaded moments earlier. Keeping the last value here
 * means a returning screen renders it on its first frame while a re-read runs behind it.
 */
internal class ProgressStateHolder(
    private val learningProgressService: LearningProgressService,
    private val curriculumRepository: CurriculumRepository,
    private val mistakeReviewService: MistakeReviewService,
    historyStore: AssessmentHistoryStore,
    scope: CoroutineScope,
) {
    val state: StateFlow<ProgressUiState> = historyStore.history
        .map { history ->
            when (history) {
                AssessmentHistory.Loading -> ProgressUiState.Loading
                AssessmentHistory.Failed -> ProgressUiState.Error
                is AssessmentHistory.Loaded -> runCatching { loadState(history.attempts) }
                    .getOrElse { ProgressUiState.Error }
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, ProgressUiState.Loading)

    private suspend fun loadState(completedAttempts: List<TestAttempt>): ProgressUiState {
        // Reuses the history the cache already holds, so one derivation is one read.
        val snapshot = learningProgressService.load(completedAttempts)
        if (snapshot.completedAttemptCount == 0) return ProgressUiState.Empty
        // Reuses the mistake queue's own latest-occurrence rule instead of re-deriving it here,
        // and asks only for the size so no review content is reconstructed. Handing over the
        // history already loaded above keeps this refresh to two repository reads, not three.
        val unresolvedMistakeCount = mistakeReviewService.countUnresolved(completedAttempts)

        return ProgressUiState.Content(
            completedAttemptCount = snapshot.completedAttemptCount,
            answeredQuestionCount = snapshot.answeredQuestionCount,
            correctAnswerCount = snapshot.correctAnswerCount,
            percentage = snapshot.percentage,
            // Both of these are read straight off the snapshot the derivation above already
            // produced. Neither the window nor the ACTIVE denominator is recomputed here, so
            // presentation cannot disagree with the domain and adds no repository read of its own.
            coverage = toUiModel(snapshot.coverage),
            recentPerformance = toUiModel(snapshot.recentPerformance),
            unresolvedMistakeCount = unresolvedMistakeCount,
            weakAreas = snapshot.weakAreas.map(::toUiModel),
            topics = snapshot.topics.map(::toUiModel),
            history = mapHistory(completedAttempts),
        )
    }

    private fun toUiModel(coverage: CurriculumCoverage): ProgressCoverageUiModel =
        ProgressCoverageUiModel(
            attemptedQuestionCount = coverage.attemptedQuestionCount,
            totalQuestionCount = coverage.totalQuestionCount,
            percentage = coverage.percentage,
        )

    /**
     * `null` when the window holds no answered question at all, so the dashboard omits the surface
     * instead of claiming a recent 0%.
     */
    private fun toUiModel(recent: RecentPerformance): ProgressRecentPerformanceUiModel? {
        val percentage = recent.percentage ?: return null
        return ProgressRecentPerformanceUiModel(
            attemptCount = recent.attemptCount,
            answeredQuestionCount = recent.answeredQuestionCount,
            correctAnswerCount = recent.correctAnswerCount,
            percentage = percentage,
            trend = when (val availability = recent.trendAvailability) {
                is RecentTrendAvailability.InsufficientHistory ->
                    ProgressRecentTrendUiModel.InsufficientHistory(availability.requiredAttemptCount)
                RecentTrendAvailability.Available -> ProgressRecentTrendUiModel.Available(
                    // Kept in the domain's oldest -> newest order so the chart reads past -> present.
                    attempts = recent.attemptSeries.map {
                        ProgressRecentAttemptUiModel(
                            attemptId = it.attemptId,
                            percentage = it.percentage,
                        )
                    },
                )
            },
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
        // LearningProgressService applies the same defensive filter before deriving its
        // statistics. Mirroring it keeps the history consistent with completedAttemptCount
        // and prevents one malformed row from turning the whole dashboard into Error.
        val completedAttempts = attempts.filter { it.status == AssessmentStatus.COMPLETED }

        return completedAttempts.map { attempt ->
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

