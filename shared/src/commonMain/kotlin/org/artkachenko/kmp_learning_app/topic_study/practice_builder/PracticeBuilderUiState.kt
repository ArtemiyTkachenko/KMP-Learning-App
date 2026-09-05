package org.artkachenko.kmp_learning_app.topic_study.practice_builder

import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel

/**
 * Everything the Practice Builder renders, and nothing it would have to derive.
 *
 * The screen reads this and calls back; it never decides whether a level may be deselected, whether
 * a source may be chosen, or whether Start is allowed. Those are invariants, and an invariant a
 * Composable enforces is one that a second Composable can break.
 */
internal data class PracticeBuilderUiState(
    val scope: PracticeScopeUiModel,
    val questionCount: Int,
    val questionCountOptions: List<Int>,
    val levels: Set<QuestionLevel>,
    val source: PracticeQuestionSource,
    val sourceOptions: List<PracticeSourceOption>,
    val availability: PracticeAvailability,
) {
    val isStartEnabled: Boolean get() = availability is PracticeAvailability.Available
}

/**
 * The scope being practised.
 *
 * [name] is null until the content read resolves the stable ID, and stays null if the target no
 * longer exists. The kind is known from the route, so the screen can always say what is being
 * configured even before it can say which Topic.
 */
internal data class PracticeScopeUiModel(
    val kind: PracticeScopeKind,
    val name: String? = null,
)

/**
 * Which kind of thing the builder was opened on, taken from the route rather than from the derived
 * [org.artkachenko.kmp_learning_app.assessment.AssessmentScope]. A Learning Unit and a hand-picked
 * group of Subtopics produce the same assessment scope, and the learner chose one of them.
 */
internal enum class PracticeScopeKind {
    TOPIC,
    SUBTOPIC,
    LEARNING_UNIT,
}

/**
 * One question-source choice.
 *
 * [isAvailable] is a property of the selection policy, not of the learner's content: a source
 * whose policy has not been implemented yet cannot be chosen at all. Whether an available source
 * happens to have Questions right now is [PracticeAvailability], which is a separate answer — an
 * unseen source stays selectable after the learner has seen everything in scope, and reports no
 * content. All current sources have policies; keeping this distinction lets future source values be
 * represented before implementation without conflating that state with an empty eligible pool.
 */
internal data class PracticeSourceOption(
    val source: PracticeQuestionSource,
    val isAvailable: Boolean,
)

/** Whether the current configuration could actually run, checked before Start is ever pressed. */
internal sealed interface PracticeAvailability {
    /** The eligibility read is in flight. Start is withheld rather than guessed at. */
    data object Checking : PracticeAvailability

    data class Available(val eligibleQuestionCount: Int) : PracticeAvailability

    /** The scope, levels, and source are valid, but no ACTIVE Question matches them. */
    data object NoEligibleQuestions : PracticeAvailability

    /**
     * The target itself no longer names current material, so there is no scope to filter.
     *
     * Distinct from [NoEligibleQuestions], which the learner can resolve by widening levels or
     * changing the source: nothing on this screen can make a retired Unit practiceable, so the
     * controls stay visible but Start can never become enabled. Distinct from [Error] too — the
     * lookup succeeded and gave a settled answer, so offering Retry would promise a different
     * outcome from repeating it.
     */
    data object TargetUnavailable : PracticeAvailability

    /**
     * The target resolves and is current, but teaches no concept that can be assessed.
     *
     * Content validation should prevent this, and it is reported rather than assumed away because
     * the alternative at this boundary is constructing an empty scope and failing a domain
     * precondition in front of the learner.
     */
    data object NoPracticeableConcepts : PracticeAvailability

    /** The eligibility read failed. Distinct from "nothing matched", which is not an error. */
    data object Error : PracticeAvailability
}

/**
 * The builder's one outward effect. Navigation receives the finished configuration rather than any
 * selected content: the practice run is described, not carried.
 */
internal sealed interface PracticeBuilderEvent {
    data class StartPractice(val config: AssessmentConfig.Focused) : PracticeBuilderEvent
}
