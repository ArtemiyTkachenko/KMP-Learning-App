package org.artkachenko.kmp_learning_app.learning_progress

import org.artkachenko.kmp_learning_app.assessment.TestAttempt

/**
 * What "recent" means for derived learning statistics.
 *
 * All-time accuracy answers "how have I performed across my complete history?", and it becomes
 * steadily less responsive as that history grows: a learner who started at 45% and now scores 80-90%
 * can still read 58% lifetime. Recent performance answers the separate question "how have I been
 * performing lately?" and deliberately does not replace or reweight the all-time figures.
 *
 * ## Why a count window rather than a date window
 *
 * "The last 7 days" behaves badly for a local study app: an intensive user produces dozens of
 * observations in one evening while an occasional user produces none for a fortnight and would see
 * an empty dashboard despite having history. A bounded count of completed assessments always
 * describes real evidence, whenever it happened.
 *
 * ## Why five
 *
 * Five is recent enough to move when the learner improves or regresses, large enough that one bad
 * evening does not define it, small enough to explain in one sentence ("your last five
 * assessments"), and a natural size for one compact trend series. It is a product policy, not a
 * tuning parameter, so it is intentionally not configurable.
 */
internal object RecentPerformancePolicy {
    /** How many completed assessments define "recent". */
    const val MaxRecentAttempts = 5

    /**
     * Below this many attempts there is no trajectory worth presenting: one attempt is a single
     * observation, and two describe a single change that is as likely to be noise as a trend.
     */
    const val MinimumTrendAttempts = 3

    /**
     * Upper bound on the per-answer series. The series exists so presentation can show a fine
     * grained recent history without loading attempts again; it is not the source of the recent
     * summary, which always uses every answer in the window.
     */
    const val MaxRecentAnswerOutcomes = 50

    /**
     * Matches the persistence ordering in `AssessmentAttemptDao` (`completed_at DESC, started_at
     * DESC, id ASC`) so the window is the same set of attempts however the history reached us.
     */
    private val newestFirst: Comparator<TestAttempt> =
        compareByDescending<TestAttempt> { requireNotNull(it.completedAt) }
            .thenByDescending(TestAttempt::startedAt)
            .thenBy(TestAttempt::id)

    /**
     * The latest [MaxRecentAttempts] completed attempts, returned oldest -> newest so a chart reads
     * past -> present without presentation having to reverse domain data.
     *
     * The input is sorted here rather than trusted, and copied rather than sorted in place: the
     * caller's list may come from the repository, from the shared history cache, or from a test
     * fake, and only the first of those guarantees the persistence ordering.
     */
    fun recentWindow(completedAttempts: List<TestAttempt>): List<TestAttempt> =
        completedAttempts
            .sortedWith(newestFirst)
            .take(MaxRecentAttempts)
            .reversed()

    fun trendAvailability(attemptCount: Int): RecentTrendAvailability =
        if (attemptCount >= MinimumTrendAttempts) {
            RecentTrendAvailability.Available
        } else {
            RecentTrendAvailability.InsufficientHistory(
                attemptCount = attemptCount,
                requiredAttemptCount = MinimumTrendAttempts,
            )
        }
}
