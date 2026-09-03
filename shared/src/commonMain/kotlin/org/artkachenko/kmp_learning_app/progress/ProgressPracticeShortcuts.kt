package org.artkachenko.kmp_learning_app.progress

import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset

/**
 * The practice intent a weak-area row already stands for.
 *
 * Nothing is decided here. The row exists because `LearningProgressService` put it in the snapshot's
 * weak areas, so weakness is already established and is not re-tested against the accuracy figure
 * beside it; the row carries the stable ID, so the scope is already identified. All this does is
 * name the pair as the semantic payload EPIC-16 already understands.
 *
 * This is deliberately not a recommendation. `LearningRecommendationPolicy` chooses one globally
 * prioritised action; a row-level shortcut only offers the action for the exact scope the learner is
 * looking at, and several of them may be offered at once.
 *
 * The dashboard's aggregate figures — curriculum coverage and the unresolved-mistake count — get no
 * equivalent, and must not: neither identifies a Topic or Subtopic, and there is no global focused
 * scope to fall back on. Picking one for the learner would be hidden recommendation behaviour.
 */
internal fun WeakAreaUiModel.toPracticePreset(): PracticePreset =
    PracticePreset(
        scope = when (type) {
            WeakAreaType.TOPIC -> AssessmentScope.Topic(stableId)
            WeakAreaType.SUBTOPIC -> AssessmentScope.Subtopic(stableId)
        },
        source = PracticeQuestionSource.WEAK_AREAS,
    )
