package org.artkachenko.kmp_learning_app.guided_learning

import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.learning_progress.SubtopicCoverage
import org.artkachenko.kmp_learning_app.learning_progress.TopicCoverage
import org.artkachenko.kmp_learning_app.learning_progress.WeakArea

/**
 * Already-derived facts needed to choose one next learning action.
 *
 * Sources of truth are deliberately explicit:
 *
 * - [completedAttemptCount], [weakAreas], [topicCoverage], and [subtopicCoverage] come from
 *   `LearningProgressSnapshot`;
 * - [unresolvedMistakeCount] comes from `MistakeReviewService.countUnresolved`, which delegates to
 *   `UnresolvedMistakeDerivation`;
 * - [recentStudyContext] is the configuration kind and stable scope of the newest completed
 *   history entry. Completed history is already newest first; callers must not include an
 *   in-progress attempt or presentation navigation state.
 *
 * The policy consumes these facts as given. It does not recalculate weakness thresholds, exposure,
 * unresolved-mistake lifecycle, or history ordering.
 */
internal data class LearningRecommendationInputs(
    val completedAttemptCount: Int,
    val unresolvedMistakeCount: Int,
    val weakAreas: List<WeakArea>,
    val topicCoverage: List<TopicCoverage>,
    val subtopicCoverage: List<SubtopicCoverage>,
    val recentStudyContext: RecentStudyContext?,
) {
    init {
        require(completedAttemptCount >= 0) {
            "completedAttemptCount must not be negative."
        }
        require(unresolvedMistakeCount >= 0) {
            "unresolvedMistakeCount must not be negative."
        }
    }
}

/** Stable completed-study context, used only to break an otherwise tied coverage decision. */
internal sealed interface RecentStudyContext {
    data class Focused(
        val scope: AssessmentScope,
    ) : RecentStudyContext

    /** Mixed history cannot identify one Topic and therefore never changes a coverage decision. */
    data object Mixed : RecentStudyContext
}

/**
 * Semantic intent for opening EPIC-16 practice.
 *
 * Question count and levels stay absent so the Practice Builder remains authoritative for its
 * established defaults and lets the learner inspect or edit them before starting.
 */
internal data class PracticePreset(
    val scope: AssessmentScope,
    val source: PracticeQuestionSource,
)

/** A destination concept, independent from Navigation 3 and presentation models. */
internal sealed interface LearningRecommendationTarget {
    data object Topics : LearningRecommendationTarget

    data object MistakeReview : LearningRecommendationTarget

    data class Practice(
        val preset: PracticePreset,
    ) : LearningRecommendationTarget
}

/**
 * Typed evidence for localized user-visible copy. Presentation never has to infer why a target won.
 */
internal sealed interface LearningRecommendationRationale {
    data object NewUser : LearningRecommendationRationale

    data class UnresolvedMistakes(
        val count: Int,
    ) : LearningRecommendationRationale

    data class WeakArea(
        val scope: AssessmentScope,
        val areaName: String?,
    ) : LearningRecommendationRationale

    data class UnseenCoverage(
        val topicId: String,
        val unseenQuestionCount: Int,
    ) : LearningRecommendationRationale
}

/** At most one useful next action and the exact fact that justified it. */
internal data class LearningRecommendation(
    val target: LearningRecommendationTarget,
    val rationale: LearningRecommendationRationale,
)
