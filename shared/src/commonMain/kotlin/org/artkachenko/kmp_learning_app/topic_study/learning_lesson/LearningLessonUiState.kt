package org.artkachenko.kmp_learning_app.topic_study.learning_lesson

/**
 * The Lesson destination's state.
 *
 * Deliberately small. E21-03 owns the Lesson's identity, its parent relationship, and its place in
 * the back stack; E21-04 owns the structured document body. The authored Lesson's sections and
 * Sources are therefore not carried here even though they were resolved on the way in — state that
 * nothing renders is a contract this issue has not earned yet, and E21-04 will add it alongside the
 * renderer that needs it.
 *
 * [NotFound] and [Error] stay distinct for the same reason as on the Unit overview: an identity
 * that names nothing current is settled, while a document that could not be read is worth retrying.
 */
internal sealed interface LearningLessonUiState {
    data object Loading : LearningLessonUiState

    data class Content(
        val unitId: String,
        val lessonId: String,
        val title: String,
        val summary: String,
    ) : LearningLessonUiState

    /**
     * The Unit or Lesson is unknown or no longer current — or the two do not belong together. All
     * three are the same answer to the learner: this is not something they can read now.
     */
    data object NotFound : LearningLessonUiState

    /** The learning document could not be read. Retryable, unlike [NotFound]. */
    data object Error : LearningLessonUiState
}
