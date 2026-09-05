package org.artkachenko.kmp_learning_app.topic_study.learning_lesson

import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningSection

/**
 * The Lesson destination's state.
 *
 * [NotFound] and [Error] stay distinct for the same reason as on the Unit overview: an identity
 * that names nothing current is settled, while a document that could not be read is worth retrying.
 */
internal sealed interface LearningLessonUiState {
    data object Loading : LearningLessonUiState

    /**
     * A current Lesson and everything the reader renders.
     *
     * [sections] and [sources] are the authored domain values, carried through rather than mapped
     * into a parallel UI hierarchy. `LearningSection` and `LearningBlock` were designed as
     * presentation-independent document structures — they carry no colour, spacing, or platform
     * type — so mirroring every block variant in a second hierarchy would duplicate the document
     * model without introducing a boundary. The rule that matters runs the other way: nothing
     * presentational may travel *into* those models, so every style decision lives in the renderer.
     *
     * [previousLesson] and [nextLesson] are presentation models rather than whole Lessons: the
     * controls need a stable ID to navigate by and a title to show, and handing the screen a
     * `LearningLesson` would give it a second Lesson's entire body to render by accident.
     */
    data class Content(
        val unitId: String,
        val lessonId: String,
        val title: String,
        val summary: String,
        val sections: List<LearningSection>,
        val sources: List<SourceReference>,
        val previousLesson: AdjacentLessonUiModel?,
        val nextLesson: AdjacentLessonUiModel?,
    ) : LearningLessonUiState

    /**
     * The Unit or Lesson is unknown or no longer current — or the two do not belong together. All
     * three are the same answer to the learner: this is not something they can read now.
     */
    data object NotFound : LearningLessonUiState

    /** The learning document could not be read. Retryable, unlike [NotFound]. */
    data object Error : LearningLessonUiState
}

/**
 * A sibling Lesson as its navigation control needs it.
 *
 * The [lessonId] is what travels when the control is activated. Position is deliberately absent:
 * an index would be a second, weaker identity that stops meaning the same thing the moment a Lesson
 * is retired, while the stable ID keeps meaning exactly one Lesson.
 */
internal data class AdjacentLessonUiModel(
    val lessonId: String,
    val title: String,
)
