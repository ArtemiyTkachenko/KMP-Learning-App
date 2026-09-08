package org.artkachenko.kmp_learning_app.lesson_study

/**
 * What the Learn surfaces currently know about the learner's studied Lessons.
 *
 * [Loading] and [Error] are deliberately distinct from an empty [Loaded], because "study state
 * could not be read" and "nothing has been studied" are different statements about the learner. A
 * surface that cannot read study state does not know a Lesson is unstudied, so it withholds the
 * indicator and the mark control rather than announcing every Lesson as confidently unstudied.
 */
internal sealed interface StudyProgressState {
    data object Loading : StudyProgressState

    /**
     * [studiedLessons] is the repository's own list in its own order — a persistence convenience
     * that no derivation reads, kept because it is what was actually read back.
     * [pendingLessonIds] names the Lessons whose mark or unmark is currently being persisted; only
     * those Lessons lose their action, so a write on one Lesson never disables another.
     */
    data class Loaded(
        val studiedLessons: List<StudiedLesson>,
        val pendingLessonIds: Set<String> = emptySet(),
    ) : StudyProgressState {
        /**
         * Membership only, which is the single shape [StudyProgressDerivation] accepts: row order
         * and recorded times are boundary detail that must not reach current progress.
         */
        val studiedLessonIds: Set<String> =
            studiedLessons.mapTo(mutableSetOf(), StudiedLesson::lessonId)
    }

    data object Error : StudyProgressState
}
