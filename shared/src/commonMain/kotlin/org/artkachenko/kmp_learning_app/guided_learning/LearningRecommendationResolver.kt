package org.artkachenko.kmp_learning_app.guided_learning

import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressSnapshot

/**
 * How many Questions are currently unresolved, for completed history the caller already holds.
 *
 * Deliberately one number and one input: the recommendation needs the count and nothing else, so it
 * cannot reach review content, the repository, or the shared history cache through this. The
 * production implementation is `MistakeReviewService::countUnresolved`, which owns the
 * latest-occurrence semantics; this interface exists so the recommendation domain does not have to
 * depend on the Mistake Review feature to ask for its own input, and so a failing count can be
 * exercised without a malformed persisted attempt.
 */
internal fun interface UnresolvedMistakeCounter {
    suspend fun countUnresolved(completedAttempts: List<TestAttempt>): Int
}

/**
 * Assembles the already-derived facts one next action needs, and hands them to the policy.
 *
 * This class makes no product decision. Precedence, weak-area ordering, coverage tie-breaking, and
 * the new-user and no-curriculum cases all belong to [LearningRecommendationPolicy], which is the
 * only place a recommendation is chosen. Everything here is fact-gathering:
 *
 * - the learning-progress facts are read off a [LearningProgressSnapshot] the caller has already
 *   derived, rather than by loading one. One history emission therefore produces one progress
 *   derivation, shared with whatever else the caller enriches from it;
 * - the unresolved-mistake count is asked for with that same completed history, so the shared cache
 *   is not read again;
 * - recent study context comes from [toRecentStudyContext], the one definition guided learning has
 *   of what a stored attempt says about recent study.
 *
 * A failing count propagates rather than being read as zero. Zero unresolved mistakes is a decision
 * the policy acts on — it falls through to weak areas and then to coverage — so substituting it for
 * an unknown count would recommend practice on the strength of a fact nobody established.
 */
internal class LearningRecommendationResolver(
    private val unresolvedMistakeCounter: UnresolvedMistakeCounter,
) {
    /**
     * @param completedAttempts completed history, newest first, exactly as the caller received it
     * from `AssessmentHistoryStore`. Not re-sorted: newest-first is the repository's contract.
     * @param progress the snapshot derived from that same [completedAttempts].
     */
    suspend fun resolve(
        completedAttempts: List<TestAttempt>,
        progress: LearningProgressSnapshot,
    ): LearningRecommendation? {
        val inputs = LearningRecommendationInputs(
            completedAttemptCount = progress.completedAttemptCount,
            unresolvedMistakeCount = unresolvedMistakeCounter.countUnresolved(completedAttempts),
            weakAreas = progress.weakAreas,
            topicCoverage = progress.topicCoverage,
            subtopicCoverage = progress.subtopicCoverage,
            // The newest entry that describes completed study, which for the normal completed-only
            // history is simply the first. firstNotNullOfOrNull rather than first() so an
            // IN_PROGRESS attempt supplied by a lower-level caller is stepped over instead of
            // erasing a real recent context — without reordering valid completed history.
            recentStudyContext = completedAttempts.firstNotNullOfOrNull(
                TestAttempt::toRecentStudyContext,
            ),
        )

        return LearningRecommendationPolicy.recommend(inputs)
    }
}
