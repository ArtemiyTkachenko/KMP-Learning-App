package org.artkachenko.kmp_learning_app.learning_progress

/**
 * The thresholds behind a Topic's readiness verdict for the learner's chosen [ReadinessTarget].
 *
 * Kept beside [LearningProgressPolicy] for the same reason [MistakeReviewPolicy] is: these are
 * product thresholds over completed history, applied by `ReadinessDerivation`, and nothing about
 * them is persisted. They are first values, expected to be tuned, so every one of them lives here
 * and nowhere else.
 */
internal object ReadinessPolicy {
    /**
     * Target-level answers a Topic needs before it gets any verdict at all.
     *
     * The weak-area minimum, reused rather than restated: "ready" is a verdict exactly as "weak" is,
     * and a verdict should rest on a pattern rather than on a few lucky or unlucky Questions. Below
     * it a Topic is `NOT_ENOUGH_EVIDENCE`, whatever its answers say.
     */
    const val MinimumAnsweredForVerdict: Int = LearningProgressPolicy.WeakAreaMinimumAnswered

    /**
     * How many of a Topic's most recent target-level answers the accuracy condition reads.
     *
     * Readiness asks "can I answer these now?", which all-time accuracy answers slowly: early
     * mistakes would hold a Topic back long after the learner had moved past them. Ten is twice the
     * evidence minimum, so one wrong answer moves the figure by ten points rather than twenty, and
     * still small enough that recent improvement shows within a session or two.
     */
    const val RecentWindow: Int = 10

    /**
     * Recent accuracy, in percent, a Topic needs to be `READY`.
     *
     * Well above the 70% weak-area line on purpose: "not weak" and "ready for an interview" are
     * different claims, and an interview rarely forgives one wrong answer in three.
     */
    const val ReadyRecentAccuracy: Double = 80.0

    /**
     * Share, in percent, of the Topic's current ACTIVE target-level Questions the learner must have
     * seen to be `READY`, each Question counted once.
     *
     * High recent accuracy over a small corner of a Topic is not readiness for the Topic. Sixty
     * rather than all of it, because finishing every Question in a bank is not what an interview
     * asks, and new content would otherwise keep taking `READY` away.
     */
    const val ReadyCoverage: Double = 60.0

    /**
     * Due mistakes at the target levels in the Topic that still allow `READY`.
     *
     * None: a due mistake is a Question the learner has got wrong and has not yet shown they can
     * answer a day later, which is precisely the gap readiness exists to surface. A mistake that is
     * scheduled but not yet due does not count — the learner cannot act on it today.
     */
    const val MaximumDueMistakesForReady: Int = 0
}
