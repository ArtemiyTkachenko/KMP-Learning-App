package org.artkachenko.kmp_learning_app.guided_learning

/**
 * Where continuing recent study sends the learner.
 *
 * Both cases address content by stable curriculum identity, and neither can name a stored attempt.
 * That is the whole distinction this feature rests on: Continue Studying returns to a *learning
 * context*, it does not resume, retake, or reopen an assessment. There is deliberately no variant
 * carrying an attempt ID, so no consumer can express one.
 */
internal sealed interface ContinueStudyingTarget {
    /** The existing Topic detail, optionally opened at one of its Subtopics. */
    data class Topic(
        val topicId: String,
        val subtopicId: String? = null,
    ) : ContinueStudyingTarget

    /**
     * The existing Practice Builder, opened on a remembered intent the learner can still edit.
     *
     * A preset, not a run: [PracticePreset] carries scope and source only, so the builder applies
     * its own question-count and level defaults and re-runs its normal preflight against current
     * content.
     */
    data class Practice(
        val preset: PracticePreset,
    ) : ContinueStudyingTarget
}

/**
 * One resolved continue-studying context: where to go, named as the curriculum names it *now*.
 *
 * History supplies the stable IDs in [target]; every label is read back from current curriculum, so
 * a renamed Topic appears renamed here without anything being migrated. Persisted display names are
 * never used, and this model carries no user-visible sentence of its own — the practice intent stays
 * typed inside [target] so presentation owns the copy and its localization.
 */
internal data class ContinueStudyingContext(
    val target: ContinueStudyingTarget,
    /** Current name of the Topic or Subtopic being returned to. */
    val scopeName: String,
    /** Current name of the parent Topic, present only when [scopeName] names a Subtopic. */
    val parentTopicName: String? = null,
)
