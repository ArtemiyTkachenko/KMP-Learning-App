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
 * [name] is null until the curriculum read resolves the stable ID, and stays null if the scope no
 * longer exists. The kind is known from the route, so the screen can always say what is being
 * configured even before it can say which Topic.
 */
internal data class PracticeScopeUiModel(
    val kind: PracticeScopeKind,
    val name: String? = null,
)

internal enum class PracticeScopeKind {
    TOPIC,
    SUBTOPIC,
}

/**
 * One question-source choice.
 *
 * [isAvailable] is a property of the selection policy, not of the learner's content: a source
 * whose policy has not been implemented yet cannot be chosen at all. Whether an available source
 * happens to have Questions right now is [PracticeAvailability], which is a separate answer — an
 * unseen source stays selectable after the learner has seen everything in scope, and reports no
 * content. E16-05 turns the remaining Mistakes source available by implementing its policy, with no
 * change to this screen.
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
