package org.artkachenko.kmp_learning_app.assessment.session

import kotlin.random.Random
import org.artkachenko.kmp_learning_app.curriculum.Question

/**
 * Returns this Question with its answers in the presentation order for [attemptId].
 *
 * Answers are stored in one order and were shown in that order, so a learner who saw a Question
 * before could recall the position of the correct option instead of reading the options. Ordering
 * per attempt removes that cue.
 *
 * The order is *derived* from the attempt and Question ids rather than stored, which is what makes
 * it stable everywhere it has to agree: resuming an in-progress attempt, re-rendering after a
 * configuration change or process death, and reviewing the attempt afterwards all produce the same
 * order, while a second attempt at the same Question orders it differently. Storing it would mean a
 * schema change and a migration for a value that can simply be recomputed.
 *
 * Nothing downstream depends on the order. [AssessmentEngine] validates and scores a submission as
 * sets, and an attempt records the answer ids that were selected, so attempts taken before this
 * existed stay readable and correctly scored.
 */
internal fun Question.withAnswersOrderedFor(attemptId: String): Question =
    copy(answers = answers.shuffled(Random(answerOrderSeed(attemptId, id))))

/**
 * A deterministic seed for one attempt/Question pair.
 *
 * Computed here rather than from `String.hashCode()` because that algorithm is a platform detail:
 * this app runs the same attempt data on Android, desktop, web, and iOS, and the order has to agree
 * on all of them. The separator keeps ids that concatenate to the same text ("ab" + "c" and
 * "a" + "bc") from seeding identically.
 */
private fun answerOrderSeed(attemptId: String, questionId: String): Int {
    var seed = SEED_OFFSET
    for (character in attemptId) seed = seed * SEED_FACTOR + character.code
    seed = seed * SEED_FACTOR + SEED_SEPARATOR
    for (character in questionId) seed = seed * SEED_FACTOR + character.code
    return seed
}

private const val SEED_OFFSET = 17
private const val SEED_FACTOR = 31
private const val SEED_SEPARATOR = 0x1F
