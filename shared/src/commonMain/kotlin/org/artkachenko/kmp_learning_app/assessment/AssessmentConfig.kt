package org.artkachenko.kmp_learning_app.assessment

import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel

/**
 * Every authored interview depth.
 *
 * This is the default practice selection and the pre-E16 focused-practice behaviour: a Topic or
 * Subtopic practised without narrowing to a level. "Any level" is expressed by naming all of them,
 * never by an empty set, which matches the `CurriculumRepository` level-filter contract.
 */
internal val AllQuestionLevels: Set<QuestionLevel> = QuestionLevel.entries.toSet()

internal sealed interface AssessmentConfig {
    val questionCount: Int

    /**
     * One targeted practice request: a scope, how many Questions to ask, which levels count as
     * eligible, and which source the eligible Questions are drawn from.
     *
     * The four dimensions are deliberately one typed configuration rather than a separate
     * assessment type per practice kind, so a new source policy extends selection instead of
     * forking the engine, the session lifecycle, and scoring.
     *
     * [levels] may be empty because a practice request is also built from user input and from
     * navigation, and a non-runnable request has to be reportable rather than fatal. Selection —
     * not this constructor — turns an empty selection into an explicit no-content outcome, so
     * every non-runnable practice request is refused in one place. [questionCount] is different:
     * a non-positive count is a programming error with no user-facing meaning, so it stays a
     * construction-time invariant.
     */
    data class Focused(
        val scope: AssessmentScope,
        override val questionCount: Int,
        val levels: Set<QuestionLevel> = AllQuestionLevels,
        val source: PracticeQuestionSource = PracticeQuestionSource.ALL,
    ) : AssessmentConfig {
        init {
            requirePositiveQuestionCount(questionCount)
        }
    }

    /**
     * Mixed Android Interview: a cross-topic run with no scope, no level narrowing, and no
     * question source. It is intentionally not a targeted practice request — coverage-first
     * selection across every ACTIVE Question is the product behaviour — so it does not inherit
     * the practice dimensions above.
     */
    data class Mixed(
        override val questionCount: Int,
    ) : AssessmentConfig {
        init {
            requirePositiveQuestionCount(questionCount)
        }
    }
}

private fun requirePositiveQuestionCount(questionCount: Int) {
    require(questionCount > 0) {
        "questionCount must be greater than zero."
    }
}
