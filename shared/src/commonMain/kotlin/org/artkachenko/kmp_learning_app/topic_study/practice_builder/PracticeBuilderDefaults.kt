package org.artkachenko.kmp_learning_app.topic_study.practice_builder

/**
 * The practice length the builder opens on, unchanged from the one-tap focused practice that
 * preceded it: arriving and pressing Start must produce exactly the run the learner used to get.
 */
internal const val DefaultPracticeQuestionCount = 10

/**
 * The lengths offered, as a short fixed ladder rather than a free-entry field.
 *
 * A discrete choice keeps the count positive by construction, needs no validation or error state,
 * and stays a one-tap decision on a phone. The ladder brackets the default on both sides so
 * shortening a run is as easy as lengthening one.
 */
internal val PracticeQuestionCountOptions: List<Int> = listOf(5, 10, 15, 20)
