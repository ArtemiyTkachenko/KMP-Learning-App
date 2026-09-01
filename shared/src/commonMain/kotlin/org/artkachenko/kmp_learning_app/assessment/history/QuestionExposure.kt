package org.artkachenko.kmp_learning_app.assessment.history

import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.TestAttempt

/**
 * The single definition of "the learner has already been shown this Question".
 *
 * Curriculum coverage and unseen practice are one concept read in opposite directions — coverage
 * counts the Questions inside this set, unseen practice selects the ones outside it — so both
 * derive it here instead of each folding over history in its own way. Two implementations would
 * agree today and drift the first time one of them decided that, say, an abandoned attempt counts,
 * and the learner would then see a Progress percentage that unseen practice contradicts.
 *
 * Exposure is:
 *
 * - keyed by stable Question ID, never by text, Topic, or Subtopic, so re-authoring a Question does
 *   not make it unseen again;
 * - a set, so a Question answered in five attempts is exposed exactly as much as one answered once;
 * - independent of correctness — this describes what was asked, not how it went;
 * - completed-history only, enforced here rather than trusted from the caller, because an
 *   IN_PROGRESS attempt can still be abandoned and is not evidence the learner was assessed on
 *   anything.
 *
 * Whether an exposed ID still exists in the curriculum is deliberately not asked. Callers intersect
 * this with whatever current ACTIVE content they care about, which is what lets a retired Question
 * stay in history without affecting anything selectable today.
 */
internal object QuestionExposure {
    fun observedQuestionIds(attempts: List<TestAttempt>): Set<String> =
        attempts
            .asSequence()
            .filter { it.status == AssessmentStatus.COMPLETED }
            .flatMap { it.questionAttempts.asSequence() }
            .mapTo(mutableSetOf()) { it.questionId }
}
