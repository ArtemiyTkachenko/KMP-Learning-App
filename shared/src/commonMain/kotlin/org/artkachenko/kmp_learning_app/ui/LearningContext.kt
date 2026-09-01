package org.artkachenko.kmp_learning_app.ui

import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressSnapshot
import org.artkachenko.kmp_learning_app.learning_progress.QuestionCoverage
import org.artkachenko.kmp_learning_app.learning_progress.SubtopicCoverage
import org.artkachenko.kmp_learning_app.learning_progress.SubtopicPerformance
import org.artkachenko.kmp_learning_app.learning_progress.TopicCoverage
import org.artkachenko.kmp_learning_app.learning_progress.TopicPerformance

/**
 * What a learner has done with one Topic or Subtopic, as the study surfaces need to say it.
 *
 * The two halves are joined here but never merged, because neither is derivable from the other:
 * coverage counts each stable Question ID once against the CURRENT ACTIVE bank, while accuracy
 * counts every occurrence across all completed history. A scope can legitimately hold real
 * historical accuracy beside zero current coverage — that is what it looks like after the Questions
 * it was answered on were retired — so the app deliberately has no single combined score.
 *
 * Three states have to stay distinguishable, and the nullability below is how:
 *
 * - a `null` [accuracyPercentage] means loaded history holds no answer for this scope. It is not
 *   0%: "never answered" and "answered and got none right" are different things to tell a learner;
 * - an absent [LearningContextUiModel] — wherever a surface holds a nullable one — means analytics
 *   have not loaded or could not be derived, so nothing about the learner is known yet. That is not
 *   the same statement as an empty history, and must never be presented as one;
 * - [isUnstudied] is the only combination that justifies saying so.
 */
internal data class LearningContextUiModel(
    val attemptedQuestionCount: Int,
    val totalQuestionCount: Int,
    /** `null` when this scope holds no ACTIVE questions at all: 0/0 is not 0% covered. */
    val coveragePercentage: Double?,
    /** All-time occurrence-based accuracy, or `null` when history holds no answer for this scope. */
    val accuracyPercentage: Double?,
    /**
     * The domain's weak-area verdict, copied verbatim. Presentation never re-derives it from
     * [accuracyPercentage]: a low percentage from a single answer is visibly low without being weak,
     * because the evidence threshold in the policy has not been met.
     */
    val isWeak: Boolean,
) {
    /**
     * True only when loaded history proves there is nothing to report: no answer ever recorded for
     * this scope, and none of its current questions encountered. Everything else has something to
     * say instead — including zero coverage beside real historical accuracy.
     */
    val isUnstudied: Boolean
        get() = accuracyPercentage == null && attemptedQuestionCount == 0

    /** Whether there is a current bank to describe, so a surface can omit an empty "0 of 0". */
    val hasCoverageScope: Boolean
        get() = totalQuestionCount > 0
}

/**
 * One derivation of a [LearningProgressSnapshot], indexed by stable ID for in-memory lookup.
 *
 * Screens join against this rather than searching the snapshot's lists per row, so enriching a list
 * of Topics costs one derivation and no repository read per card. Scopes the snapshot never
 * mentions still resolve, to an empty context rather than to nothing, because a Topic with neither
 * current questions nor history is a legitimate thing to browse.
 */
internal class LearningContextIndex(snapshot: LearningProgressSnapshot) {
    private val topicCoverage = snapshot.topicCoverage.associateBy(TopicCoverage::topicId)
    private val topicPerformance = snapshot.topics.associateBy(TopicPerformance::topicId)
    private val subtopicCoverage = snapshot.subtopicCoverage.associateBy(SubtopicCoverage::subtopicId)
    private val subtopicPerformance =
        snapshot.subtopics.associateBy(SubtopicPerformance::subtopicId)

    fun forTopic(topicId: String): LearningContextUiModel =
        learningContext(
            coverage = topicCoverage[topicId],
            performance = topicPerformance[topicId]?.let {
                Performance(it.percentage, it.isWeak)
            },
        )

    fun forSubtopic(subtopicId: String): LearningContextUiModel =
        learningContext(
            coverage = subtopicCoverage[subtopicId],
            performance = subtopicPerformance[subtopicId]?.let {
                Performance(it.percentage, it.isWeak)
            },
        )

    private fun learningContext(
        coverage: QuestionCoverage?,
        performance: Performance?,
    ): LearningContextUiModel =
        LearningContextUiModel(
            attemptedQuestionCount = coverage?.attemptedQuestionCount ?: 0,
            totalQuestionCount = coverage?.totalQuestionCount ?: 0,
            coveragePercentage = coverage?.percentage,
            accuracyPercentage = performance?.percentage,
            isWeak = performance?.isWeak == true,
        )

    /** The two performance models carry the same pair of fields under different names. */
    private data class Performance(
        val percentage: Double,
        val isWeak: Boolean,
    )
}
