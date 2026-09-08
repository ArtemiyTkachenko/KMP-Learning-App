package org.artkachenko.kmp_learning_app.lesson_study

/**
 * One learner-owned fact: a Lesson was explicitly marked studied at a point in time.
 *
 * The stable Lesson ID is the only publisher-owned identity kept here. Title, summary, body,
 * authored order, owning Unit, and Subtopic mappings stay with the learning document and are
 * resolved at read time, so this record can never disagree with the Lesson the learner sees.
 * Nothing derived — a Unit or Topic count, percentage, or completion flag — belongs here either.
 */
internal data class StudiedLesson(
    val lessonId: String,
    val studiedAtEpochMillis: Long,
)
