package org.artkachenko.kmp_learning_app.lesson_study

import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit

/**
 * Turns the current publisher-owned learning hierarchy plus the learner's studied Lesson identities
 * into current study progress. Pure: it reads no repository, no clock, and no assessment history, so
 * the same content and the same identities always produce the same result.
 *
 * This is deliberately not part of `LearningProgressService`, which answers a different question
 * from a different fact — measured performance on completed assessments. Studied is a claim the
 * learner makes about reading; practised is evidence of practice. Sharing a derivation between them
 * would let one be manufactured from the other.
 *
 * Studied state arrives as a `Set` of Lesson IDs rather than as persisted records, which is what
 * makes the three lifecycle rules fall out of the arithmetic instead of needing special cases: an
 * orphan identity is simply never encountered in current content, a repeated identity cannot weight
 * anything, and persistence's newest-first row order — a boundary convenience, not curriculum
 * order — cannot reach the output, whose order comes entirely from authored content.
 */
internal object StudyProgressDerivation {
    /**
     * Progress over [unit]'s current ACTIVE Lessons, in authored order.
     *
     * The denominator is read from the current Lessons and never from the studied set: counting the
     * records a learner happens to hold is how a partially studied Unit would report 100%.
     */
    fun deriveUnit(
        unit: LearningUnit,
        studiedLessonIds: Set<String>,
    ): LearningUnitStudyProgress {
        val lessons = unit.lessons
            .filter { it.status == ContentStatus.ACTIVE }
            .map { lesson ->
                LessonStudyProgress(
                    lessonId = lesson.id,
                    isStudied = lesson.id in studiedLessonIds,
                )
            }

        return LearningUnitStudyProgress(
            unitId = unit.id,
            lessons = lessons,
            summary = summaryOf(lessons),
        )
    }

    /**
     * Progress over the ACTIVE Units in [units] whose home Topic is [topicId], in the authored order
     * they arrive in.
     *
     * Home Topic is `LearningUnit.topicId` alone. A Lesson's primary and supporting Subtopic
     * mappings may name Subtopics owned by other Topics — cross-Topic bridging is an authoring rule
     * — and they say what a Lesson teaches, not where the learner's browsing progress lives, so they
     * take no part in this.
     *
     * Callers normally pass `LearningContentRepository.getActiveUnitsByTopic`, which already
     * satisfies both filters. They are applied anyway because this function also accepts an
     * arbitrary Unit list, and a DEPRECATED or foreign Unit slipping into a Topic aggregate would be
     * silent rather than obvious.
     */
    fun deriveTopic(
        topicId: String,
        units: List<LearningUnit>,
        studiedLessonIds: Set<String>,
    ): TopicStudyProgress {
        val unitProgress = units
            .filter { it.status == ContentStatus.ACTIVE && it.topicId == topicId }
            .map { unit -> deriveUnit(unit, studiedLessonIds) }

        // Flattening the per-Unit Lessons is what makes the Topic aggregate lesson-weighted: a Unit
        // of ten Lessons must not carry the same weight as a Unit of two, which averaging their
        // completion percentages would do. An empty Unit contributes no Lesson and so neither
        // dilutes nor inflates the Topic.
        return TopicStudyProgress(
            topicId = topicId,
            units = unitProgress,
            summary = summaryOf(unitProgress.flatMap(LearningUnitStudyProgress::lessons)),
        )
    }

    private fun summaryOf(lessons: List<LessonStudyProgress>): StudyProgressSummary =
        StudyProgressSummary.of(
            studiedCount = lessons.count(LessonStudyProgress::isStudied),
            totalCount = lessons.size,
        )
}
