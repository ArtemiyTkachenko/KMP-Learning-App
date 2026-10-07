package org.artkachenko.kmp_learning_app.learning_progress

import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel

/**
 * The interview level a learner is preparing for, and which Question levels count towards it.
 *
 * Each target includes every level below it: a Senior interview still asks Foundation questions.
 */
internal enum class ReadinessTarget(val levels: Set<QuestionLevel>) {
    JUNIOR(setOf(QuestionLevel.FOUNDATION)),
    MID_LEVEL(setOf(QuestionLevel.FOUNDATION, QuestionLevel.APPLIED)),
    SENIOR(setOf(QuestionLevel.FOUNDATION, QuestionLevel.APPLIED, QuestionLevel.ADVANCED)),
}

/**
 * One Topic's readiness verdict. There are exactly three, and there is deliberately no score: a
 * number would rank Topics against each other on evidence that is not comparable between them.
 */
internal enum class ReadinessStatus {
    READY,
    GETTING_THERE,
    NOT_ENOUGH_EVIDENCE,
}

/**
 * Something standing between a Topic and `READY`, carrying the figures needed to say so.
 *
 * The thresholds travel inside each gap so presentation states the aim the derivation actually
 * applied, rather than reading [ReadinessPolicy] a second time.
 */
internal sealed interface ReadinessGap {
    /** Fewer than [ReadinessPolicy.MinimumAnsweredForVerdict] target-level answers. */
    data class MoreAnswersNeeded(val remaining: Int) : ReadinessGap

    /** Scheduled mistakes at the target levels that are due now. */
    data class DueMistakes(val count: Int) : ReadinessGap

    data class RecentAccuracyBelow(
        val percentage: Double,
        val required: Double,
    ) : ReadinessGap

    data class CoverageBelow(
        val percentage: Double,
        val required: Double,
    ) : ReadinessGap
}

/**
 * A Topic's readiness for one target, and the evidence it rests on.
 *
 * [answeredCount] counts every target-level answer in the Topic's history; the recent figures count
 * only the newest [ReadinessPolicy.RecentWindow] of them. Coverage counts current ACTIVE target-level
 * Questions, each once. [gaps] is empty exactly when [status] is [ReadinessStatus.READY], and is
 * ordered most actionable first.
 */
internal data class TopicReadiness(
    val topicId: String,
    val status: ReadinessStatus,
    val answeredCount: Int,
    val coveredQuestionCount: Int,
    val targetQuestionCount: Int,
    val recentAnsweredCount: Int,
    val recentCorrectCount: Int,
    val dueMistakeCount: Int,
    /**
     * The target counts Advanced Questions but this Topic has none ACTIVE, so it was judged on its
     * Foundation and Applied Questions alone — which the learner should be told.
     */
    val advancedUnavailable: Boolean,
    val gaps: List<ReadinessGap>,
) {
    /** Never 0/0: a Topic with no target-level Questions is omitted rather than represented. */
    val coveragePercentage: Double
        get() = coveredQuestionCount.toDouble() / targetQuestionCount * 100.0

    /** `null` when nothing has been answered at the target levels, which is not 0% accuracy. */
    val recentAccuracy: Double?
        get() =
            if (recentAnsweredCount == 0) {
                null
            } else {
                recentCorrectCount.toDouble() / recentAnsweredCount * 100.0
            }
}

/**
 * Readiness for every visible Topic that has something to measure at [target]'s levels.
 *
 * There is no overall score, only a count: "4 of 15 Topics ready" says what it counts, while an
 * average of fifteen verdicts would say nothing any one of them supports.
 */
internal data class ReadinessSnapshot(
    val target: ReadinessTarget,
    val topics: List<TopicReadiness>,
) {
    val readyTopicCount: Int get() = topics.count { it.status == ReadinessStatus.READY }
}
