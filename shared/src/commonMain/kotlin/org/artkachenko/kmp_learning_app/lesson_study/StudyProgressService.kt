package org.artkachenko.kmp_learning_app.lesson_study

import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import org.artkachenko.kmp_learning_app.lesson_study.repository.LessonStudyRepository

/**
 * The IO half of study progress: reads the learner's study facts once per snapshot and hands them to
 * [StudyProgressDerivation] as identities.
 *
 * Publisher content is supplied by the caller rather than read here, because the Learn surfaces
 * already own their content reads and must keep the two failures apart: an unreadable learning
 * document is a content error, while unreadable study state costs the learner only the studied
 * indicator over content that still reads perfectly well. Injecting `LearningContentRepository` here
 * would duplicate those reads and merge the failures.
 */
internal class StudyProgressService(
    private val lessonStudyRepository: LessonStudyRepository,
) {
    suspend fun deriveUnit(unit: LearningUnit): LearningUnitStudyProgress =
        StudyProgressDerivation.deriveUnit(unit, studiedLessonIds())

    suspend fun deriveTopic(
        topicId: String,
        units: List<LearningUnit>,
    ): TopicStudyProgress = StudyProgressDerivation.deriveTopic(topicId, units, studiedLessonIds())

    /**
     * Whether a study fact exists for this stable Lesson ID.
     *
     * This answers persistence's question, not the curriculum's: it stays true for a DEPRECATED or
     * unresolvable Lesson, because a stored claim is never reinterpreted by content lifecycle.
     * Whether such a Lesson counts toward current progress is decided by the content-aware
     * derivation above.
     */
    suspend fun isLessonStudied(lessonId: String): Boolean = lessonStudyRepository.isStudied(lessonId)

    /**
     * One read per snapshot, collapsed to identities: a Unit and its Topic must not disagree because
     * they sampled study state at different moments, and duplicates or row order must not survive
     * into the derivation.
     */
    private suspend fun studiedLessonIds(): Set<String> =
        lessonStudyRepository.getStudiedLessons().mapTo(mutableSetOf(), StudiedLesson::lessonId)
}
