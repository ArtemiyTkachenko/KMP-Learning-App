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
 * The order is *derived* from the attempt and Question ids rather than stored: it is
 * deterministic for a given `(attemptId, questionId)` and answer set. That is what makes it agree
 * wherever it is recomputed — resuming an in-progress attempt, re-rendering after a configuration
 * change or process death, and reviewing the attempt afterwards — while a second attempt at the
 * same Question orders it differently. Storing it would mean a schema change and a migration for a
 * value that can simply be recomputed.
 *
 * Review therefore reproduces the arrangement the learner answered only while the Question's
 * AnswerOption identity set is unchanged. The question-bank identity gate guarantees that for every
 * revision accepted under a stable Question id. An attempt from before that gate may resolve
 * against a changed set — current options plus a retired one it selected — and the same seed then
 * shuffles a different list, so its exact original arrangement is not promised.
 *
 * Nothing downstream depends on the order. [AssessmentEngine] validates and scores a submission as
 * sets, and an attempt records the answer ids that were selected and its own verdict, so the
 * selected answers, the recorded correctness, and the selected options' text stay readable whatever
 * order review shows them in.
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
