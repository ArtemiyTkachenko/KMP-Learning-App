package org.artkachenko.kmp_learning_app.guided_learning

import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.learning_progress.SubtopicCoverage
import org.artkachenko.kmp_learning_app.learning_progress.TopicCoverage
import org.artkachenko.kmp_learning_app.learning_progress.WeakArea

/**
 * Deterministic product policy for the single most useful next learning action.
 *
 * This is an ordered decision tree, not a score. Priority is:
 *
 * 1. no usable ACTIVE curriculum -> no recommendation;
 * 2. no completed attempts -> browse Topics;
 * 3. unresolved mistakes -> Mistake Review;
 * 4. first currently usable weak area in the established `WeakArea` order -> weak-area practice;
 * 5. remaining unseen ACTIVE questions -> Topic-scoped unseen practice;
 * 6. otherwise -> no recommendation.
 *
 * Coverage ties are resolved by lowest exact coverage ratio, then a matching recent focused
 * context, then more unseen Questions, then stable Topic ID. No randomness, time, or aggregate
 * ranking score participates.
 */
internal object LearningRecommendationPolicy {
    fun recommend(inputs: LearningRecommendationInputs): LearningRecommendation? {
        if (!inputs.hasUsableActiveCurriculum()) return null

        if (inputs.completedAttemptCount == 0) {
            return LearningRecommendation(
                target = LearningRecommendationTarget.Topics,
                rationale = LearningRecommendationRationale.NewUser,
            )
        }

        if (inputs.unresolvedMistakeCount > 0) {
            return LearningRecommendation(
                target = LearningRecommendationTarget.MistakeReview,
                rationale = LearningRecommendationRationale.UnresolvedMistakes(
                    count = inputs.unresolvedMistakeCount,
                ),
            )
        }

        inputs.firstUsableWeakArea()?.let { return it }

        return inputs.unseenCoverageRecommendation()
    }
}

private fun LearningRecommendationInputs.hasUsableActiveCurriculum(): Boolean =
    topicCoverage.any { it.totalQuestionCount > 0 }

/**
 * `LearningPerformanceDerivation` already orders weak areas by severity, evidence, and stable ID.
 * Preserve that product meaning and skip only identities with no current ACTIVE content.
 */
private fun LearningRecommendationInputs.firstUsableWeakArea(): LearningRecommendation? {
    for (area in weakAreas) {
        val scope = area.usableScope(topicCoverage, subtopicCoverage) ?: continue
        val areaName = when (area) {
            is WeakArea.Topic -> area.performance.topicName
            is WeakArea.Subtopic -> area.performance.subtopicName
        }
        val preset = PracticePreset(
            scope = scope,
            source = PracticeQuestionSource.WEAK_AREAS,
        )
        return LearningRecommendation(
            target = LearningRecommendationTarget.Practice(preset),
            rationale = LearningRecommendationRationale.WeakArea(
                scope = scope,
                areaName = areaName,
            ),
        )
    }
    return null
}

private fun WeakArea.usableScope(
    topicCoverage: List<TopicCoverage>,
    subtopicCoverage: List<SubtopicCoverage>,
): AssessmentScope? =
    when (this) {
        is WeakArea.Topic -> performance.topicId
            .takeIf { topicId ->
                topicCoverage.any {
                    it.topicId == topicId && it.totalQuestionCount > 0
                }
            }
            ?.let(AssessmentScope::Topic)

        is WeakArea.Subtopic -> performance.subtopicId
            .takeIf { subtopicId ->
                subtopicCoverage.any {
                    it.topicId == performance.topicId &&
                        it.subtopicId == subtopicId &&
                        it.totalQuestionCount > 0
                }
            }
            ?.let(AssessmentScope::Subtopic)
    }

private fun LearningRecommendationInputs.unseenCoverageRecommendation(): LearningRecommendation? {
    val candidates = topicCoverage.filter {
        it.totalQuestionCount > 0 && it.attemptedQuestionCount < it.totalQuestionCount
    }
    val leastCovered = candidates.minWithOrNull(CoverageRatioComparator) ?: return null
    val tiedForLowestCoverage = candidates.filter {
        CoverageRatioComparator.compare(it, leastCovered) == 0
    }

    val recentTopicId = recentStudyContext.currentTopicId(subtopicCoverage)
    val selected = tiedForLowestCoverage.firstOrNull { it.topicId == recentTopicId }
        ?: tiedForLowestCoverage.minWithOrNull(
            compareByDescending<TopicCoverage> { it.unseenQuestionCount }
                .thenBy(TopicCoverage::topicId),
        )
        ?: return null

    val preset = PracticePreset(
        scope = AssessmentScope.Topic(selected.topicId),
        source = PracticeQuestionSource.UNSEEN,
    )
    return LearningRecommendation(
        target = LearningRecommendationTarget.Practice(preset),
        rationale = LearningRecommendationRationale.UnseenCoverage(
            topicId = selected.topicId,
            unseenQuestionCount = selected.unseenQuestionCount,
        ),
    )
}

private val TopicCoverage.unseenQuestionCount: Int
    get() = totalQuestionCount - attemptedQuestionCount

/** Compares ratios exactly so floating-point rounding cannot decide a recommendation. */
private val CoverageRatioComparator = Comparator<TopicCoverage> { left, right ->
    (left.attemptedQuestionCount.toLong() * right.totalQuestionCount)
        .compareTo(right.attemptedQuestionCount.toLong() * left.totalQuestionCount)
}

private fun RecentStudyContext?.currentTopicId(
    subtopicCoverage: List<SubtopicCoverage>,
): String? =
    when (this) {
        is RecentStudyContext.Focused -> when (val scope = scope) {
            is AssessmentScope.Topic -> scope.topicId
            is AssessmentScope.Subtopic -> subtopicCoverage
                .asSequence()
                .filter {
                    it.subtopicId == scope.subtopicId && it.totalQuestionCount > 0
                }
                .map(SubtopicCoverage::topicId)
                .minOrNull()
        }

        RecentStudyContext.Mixed,
        null,
        -> null
    }
