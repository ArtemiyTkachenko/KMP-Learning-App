package org.artkachenko.kmp_learning_app.topic_study.learning_lesson

import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningSection
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressUiState

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
     *
     * [studyState] is nested inside Content rather than being a fourth screen state, because the
     * learner's study record and the learning document fail independently: an unreadable record
     * costs the studied indicator and the mark control, while the Lesson itself still reads
     * perfectly well. Promoting it would turn a missing indicator into a page the learner cannot
     * read at all.
     */
    data class Content(
        val unitId: String,
        val lessonId: String,
        val title: String,
        val summary: String,
        /**
         * Where this Lesson sits in the curriculum, resolved from the Unit it was opened through.
         *
         * A learner inside the reader could previously see the Lesson title and nothing else: the
         * Topic -> Unit -> Lesson hierarchy they navigated down became invisible the moment they
         * arrived, so "am I near the end of this Unit?" could only be answered by going back. It is
         * a presentation model rather than the `LearningUnit` itself, for the same reason the
         * neighbours are: the reader needs a name and a position, and handing it the parent Unit
         * would give it every sibling Lesson's body by accident.
         *
         * Nullable so the reader degrades rather than fabricates: a Lesson opened without a
         * resolvable parent sequence simply loses the orientation line. The ViewModel always
         * supplies one, because it resolves the Lesson *through* its Unit and therefore always has
         * the sequence in hand.
         */
        val placement: LessonPlacementUiModel?,
        val sections: List<LearningSection>,
        val sources: List<SourceReference>,
        val previousLesson: AdjacentLessonUiModel?,
        val nextLesson: AdjacentLessonUiModel?,
        val studyState: StudyProgressUiState<LessonStudyUiModel> = StudyProgressUiState.Loading,
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
 * Which Unit a Lesson is being read in, and where it falls in that Unit's authored sequence.
 *
 * [position] and [lessonCount] count ACTIVE Lessons only, which is the same list previous/next step
 * through: a retired Lesson is not a waypoint, so it must not be counted in a total the learner is
 * measuring their progress against either. Both are 1-based as displayed.
 */
internal data class LessonPlacementUiModel(
    val unitTitle: String,
    val position: Int,
    val lessonCount: Int,
) {
    /**
     * Whether the position is worth printing. "Lesson 1 of 1" states that there is no sequence,
     * which the absence of the phrase says more quietly; the Unit title is still shown, because
     * knowing which Unit you are reading is useful however many Lessons it holds.
     */
    val hasSequence: Boolean
        get() = lessonCount > 1
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

/**
 * The learner's persisted study record for the Lesson on screen, and whether a change to it is
 * still being written.
 *
 * [isStudied] is only ever the last value read back from persistence. Nothing else on this screen
 * may produce it: not scroll position, not the reading meter, not having pressed Next, and not
 * assessment history. [isPending] is true only for this Lesson's own write, which is what lets the
 * control refuse a second tap while still showing the value that is actually stored.
 */
internal data class LessonStudyUiModel(
    val isStudied: Boolean,
    val isPending: Boolean,
)
