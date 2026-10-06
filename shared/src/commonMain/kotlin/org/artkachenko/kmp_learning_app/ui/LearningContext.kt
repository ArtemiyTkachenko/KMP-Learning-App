package org.artkachenko.kmp_learning_app.ui

import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressPolicy
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
 *
 * Accuracy itself then has three states of its own, told apart by [accuracyPercentage] and
 * [answeredCount] together:
 *
 * - **never answered** — [accuracyPercentage] is `null`, and a surface shows no accuracy at all;
 * - **answered, below the evidence minimum** — [accuracyPercentage] is set but
 *   [hasAccuracyEvidence] is false. A surface states the evidence instead ("2 answered", "Not
 *   enough data") and draws no percentage, accuracy colour, ring or meter: two answers do not
 *   measure a scope, and a "100%" from one of them is a claim the learner never earned;
 * - **at or above the minimum** — [hasAccuracyEvidence] is true and [accuracyPercentage] is the
 *   figure to show.
 */
internal data class LearningContextUiModel(
    val attemptedQuestionCount: Int,
    val totalQuestionCount: Int,
    /** `null` when this scope holds no ACTIVE questions at all: 0/0 is not 0% covered. */
    val coveragePercentage: Double?,
    /** All-time occurrence-based accuracy, or `null` when history holds no answer for this scope. */
    val accuracyPercentage: Double?,
    /**
     * Every answer recorded in this scope across completed history — the occurrence count behind
     * [accuracyPercentage], and the evidence [hasAccuracyEvidence] is judged on. Not
     * [attemptedQuestionCount], which counts unique current Questions for coverage: a Question
     * answered three times is three answers here and one attempted Question there.
     */
    val answeredCount: Int,
    /**
     * The domain's weak-area verdict, copied verbatim. Presentation never re-derives it from
     * [accuracyPercentage]. Below the evidence minimum the policy never calls a scope weak, so such a
     * scope shows neither a figure nor a badge.
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

    /**
     * Whether [accuracyPercentage] rests on enough answers to be shown as a figure. False both when
     * the scope was never answered and when it was answered too few times; the policy owns the
     * minimum, so no surface restates it.
     */
    val hasAccuracyEvidence: Boolean
        get() = accuracyPercentage != null &&
            LearningProgressPolicy.hasAccuracyEvidence(answeredCount)

    /** Whether there is a current bank to describe, so a surface can omit an empty "0 of 0". */
    val hasCoverageScope: Boolean
        get() = totalQuestionCount > 0

    /**
     * Whether the coverage already derived above leaves current questions the learner has not met.
     *
     * This restates the two counts and nothing else. It is an *observation* used to decide whether
     * offering unseen practice makes sense, never a selection rule: which stable Question IDs are
     * actually unseen stays entirely with the practice selector, which re-derives them from
     * completed history at the moment practice is configured.
     */
    val hasUnseenQuestions: Boolean
        get() = attemptedQuestionCount < totalQuestionCount
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
                Performance(it.percentage, it.answeredCount, it.isWeak)
            },
        )

    fun forSubtopic(subtopicId: String): LearningContextUiModel =
        learningContext(
            coverage = subtopicCoverage[subtopicId],
            performance = subtopicPerformance[subtopicId]?.let {
                Performance(it.percentage, it.answeredCount, it.isWeak)
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
            answeredCount = performance?.answeredCount ?: 0,
            isWeak = performance?.isWeak == true,
        )

    /** The two performance models carry the same fields under different types. */
    private data class Performance(
        val percentage: Double,
        val answeredCount: Int,
        val isWeak: Boolean,
    )
}
