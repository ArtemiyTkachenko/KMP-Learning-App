package org.artkachenko.kmp_learning_app.learning_progress

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * The spaced-review ladder a mistake climbs before it counts as resolved.
 *
 * Kept beside [LearningProgressPolicy] because both are product thresholds over completed history,
 * not storage rules: `MistakeScheduleDerivation` applies these values and nothing is persisted.
 */
internal object MistakeReviewPolicy {
    /**
     * The gap before each review, indexed by the number of counted correct answers so far.
     *
     * Growing gaps are the point: a correct answer one day later shows recall overnight, and the
     * later steps show it lasting past the week the learner was cramming in. Fixed rather than
     * adaptive, so a learner can predict when an entry comes back.
     */
    val Ladder: List<Duration> = listOf(1.days, 3.days, 7.days)

    /**
     * Counted correct answers needed to resolve a mistake. One per [Ladder] step, so the last step's
     * correct answer is the one that resolves it.
     */
    val ResolveAfterCountedCorrect: Int = Ladder.size

    /**
     * How early a review may happen and still count.
     *
     * The ladder is elapsed time, not calendar days, so without slack a learner who studies at 21:00
     * and again at 19:00 the next day would be two hours short of "a day later". Four hours keeps a
     * normal evening routine counting while still ruling out a same-session or same-morning repeat.
     */
    val Grace: Duration = 4.hours

    /**
     * Whether a correct answer at [answeredAt] advances a schedule that is due at [dueAt].
     *
     * The same predicate decides whether an entry is presented as due, so "Due now" always means
     * "a correct answer now would count".
     */
    fun counts(answeredAt: Instant, dueAt: Instant): Boolean = answeredAt >= dueAt - Grace
}
