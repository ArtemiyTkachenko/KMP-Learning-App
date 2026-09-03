package org.artkachenko.kmp_learning_app.saved_questions

/** A learner-owned stable Question identity, independent of current curriculum availability. */
internal data class SavedQuestion(
    val questionId: String,
    val savedAtEpochMillis: Long,
)
