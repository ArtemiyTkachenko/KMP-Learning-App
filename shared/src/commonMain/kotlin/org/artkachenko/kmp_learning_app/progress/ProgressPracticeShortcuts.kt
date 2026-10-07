package org.artkachenko.kmp_learning_app.progress

import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset
import org.artkachenko.kmp_learning_app.learning_progress.ReadinessGap

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

/**
 * The practice intent a readiness gap obviously stands for, or `null` for due mistakes, whose
 * destination is the Mistakes area rather than a practice preset.
 *
 * As with weak areas, nothing is decided here: the derivation already named the gap and the Topic.
 * Too few answers and low recent accuracy both open ordinary practice on the Topic; low coverage
 * opens it on unseen Questions, which is exactly what raises coverage.
 *
 * The preset also carries the readiness target's [levels], so the builder opens on exactly the
 * Questions the gap is measured over: an unseen Advanced Question answered for a Junior target would
 * leave Junior coverage where it was. The learner can still widen them in the builder.
 */
internal fun ReadinessGap.toPracticePreset(
    topicId: String,
    levels: Set<QuestionLevel>,
): PracticePreset? =
    when (this) {
        is ReadinessGap.DueMistakes -> null
        is ReadinessGap.MoreAnswersNeeded, is ReadinessGap.RecentAccuracyBelow -> PracticePreset(
            scope = AssessmentScope.Topic(topicId),
            source = PracticeQuestionSource.ALL,
            levels = levels,
        )
        is ReadinessGap.CoverageBelow -> PracticePreset(
            scope = AssessmentScope.Topic(topicId),
            source = PracticeQuestionSource.UNSEEN,
            levels = levels,
        )
    }
