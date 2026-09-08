package org.artkachenko.kmp_learning_app.lesson_study

import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit

/**
 * Answers "what current Lesson should I study next?" from current ACTIVE learning content and the
 * learner's studied Lesson identities, and from nothing else.
 *
 * This is not Continue Studying. That surface answers "where was I working, and how do I get back?"
 * out of completed assessment history; this one answers a question about reading, out of the
 * authored curriculum. The two are separately named, separately derived, and allowed to point
 * somewhere different, as `docs/architecture/study-progress.md` specifies.
 *
 * It lives beside [StudyProgressDerivation] rather than in `guided_learning` on purpose. Everything
 * in this package is pure over the learning document plus studied identities, and nothing in it can
 * reach a `TestAttempt`; keeping the policy here makes "Continue Learning never reads assessment
 * history" a property of where the code sits rather than a rule someone has to remember.
 *
 * Pure and positional: no clock, no repository, no attempt history, no weakness, coverage, or
 * navigation history, and no persistence. The same content and the same identities always give the
 * same answer, which is what lets the whole policy be tested without a database or a back stack.
 */
internal object ContinueLearningPolicy {
    /**
     * Walks [units] in the order given, then each Unit's Lessons in the order given, and returns the
     * first ACTIVE Lesson whose ID is absent from [studiedLessonIds].
     *
     * @param units ACTIVE Units in global authored order, as `LearningContentRepository.getActiveUnits`
     * provides them. List position *is* the curriculum's pedagogical sequence, so nothing here
     * sorts, groups by Topic, or re-orders: doing any of those would answer a positional question
     * with an order the author never wrote. DEPRECATED Units are filtered anyway, because an
     * arbitrary list is accepted and retired material silently becoming "next" would be invisible.
     *
     * @param studiedLessonIds membership only. An identity naming a Lesson that no longer resolves
     * is simply never encountered by the walk, which is how orphaned records are ignored without a
     * special case, and a duplicate identity cannot weight anything.
     *
     * Fully studied Units are skipped as a consequence of the walk rather than by a rule of their
     * own: they contain no remaining unstudied Lesson, so the walk passes straight through them.
     */
    fun resolve(
        units: List<LearningUnit>,
        studiedLessonIds: Set<String>,
    ): ContinueLearningOutcome {
        // Distinguishes the two exhausted outcomes. It can only be trusted once the walk has run to
        // the end, which is exactly when it is read: an early return means an unstudied Lesson was
        // found and neither exhausted outcome applies.
        var eligibleLessonExists = false

        for (unit in units) {
            if (unit.status != ContentStatus.ACTIVE) continue
            for (lesson in unit.lessons) {
                if (lesson.status != ContentStatus.ACTIVE) continue
                eligibleLessonExists = true
                if (lesson.id !in studiedLessonIds) {
                    return ContinueLearningOutcome.Next(
                        ContinueLearningTarget(unitId = unit.id, lessonId = lesson.id),
                    )
                }
            }
        }

        return if (eligibleLessonExists) {
            ContinueLearningOutcome.Complete
        } else {
            ContinueLearningOutcome.Empty
        }
    }
}
