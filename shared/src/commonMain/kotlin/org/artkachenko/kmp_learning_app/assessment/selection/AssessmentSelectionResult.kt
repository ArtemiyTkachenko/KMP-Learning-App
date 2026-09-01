package org.artkachenko.kmp_learning_app.assessment.selection

import org.artkachenko.kmp_learning_app.curriculum.Question

/**
 * The outcome of resolving one [org.artkachenko.kmp_learning_app.assessment.AssessmentConfig]
 * into Questions.
 *
 * An empty `List<Question>` used to carry every failure at once: nothing eligible, nothing
 * requested, and — once targeted practice exists — a source that cannot be selected yet. Those
 * are different answers to the learner, and the difference is worth keeping at the boundary that
 * knows it. The taxonomy stays deliberately small: only reasons a caller would act on
 * differently, not one case per rule.
 */
internal sealed interface AssessmentSelectionResult {
    /** A runnable selection. Never empty — emptiness is a [NoContent] outcome, not a selection. */
    data class Selected(
        val questions: List<Question>,
    ) : AssessmentSelectionResult {
        init {
            require(questions.isNotEmpty()) {
                "Selected questions must not be empty."
            }
        }
    }

    /** A request that cannot produce an assessment. No attempt is created for any of these. */
    sealed interface NoContent : AssessmentSelectionResult {
        /** The scope, levels, and source are supported, but nothing matched them. */
        data object NoEligibleQuestions : NoContent

        /** No level was selected, so no Question can match. Never treated as "all levels". */
        data object NoLevelsSelected : NoContent

        /** The requested [org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource] has no policy yet. */
        data object SourceNotSupported : NoContent
    }
}
