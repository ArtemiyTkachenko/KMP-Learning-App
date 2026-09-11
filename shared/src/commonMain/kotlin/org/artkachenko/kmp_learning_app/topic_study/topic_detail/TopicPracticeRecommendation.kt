package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.ui.LearningContextUiModel

/**
 * Which practice this Topic's evidence actually calls for.
 *
 * The Practice page used to give its largest control to the least informed action: "Start Practice"
 * was the only filled button on the page, and the two shortcuts that knew something about the
 * learner — practise the weak area, practise what you have not seen — sat above it as text buttons
 * inside a summary card. The page therefore recommended nothing and left the learner to notice the
 * useful options themselves.
 *
 * This is the smallest thing that fixes that: a total order over evidence the screen is already
 * displaying. It is not a scoring model, it has no weights, and it reads no repository — every input
 * is a figure already derived for the summary above the action.
 *
 * ## The order, and why
 *
 * 1. **Weak areas.** The domain's own verdict, already subject to `LearningProgressPolicy`'s
 *    evidence threshold. It is first because it names a demonstrated gap rather than an absence.
 * 2. **Unresolved mistakes.** Questions this learner has actually got wrong and not since got
 *    right. Concrete and finite, but narrower than a weak area: a handful of mistakes can exist in a
 *    Topic the learner is otherwise strong in.
 * 3. **Unseen questions.** Coverage rather than performance — material still ahead of them. Useful,
 *    and the weakest claim of the three, because nothing has gone wrong yet.
 * 4. **Nothing.** No evidence, so no recommendation is invented; ordinary practice leads instead.
 *
 * Every branch is explainable from what the page prints: the weak badge, the mistake count, and the
 * coverage counts are all on screen beside the action they justify.
 */
internal sealed interface TopicPracticeRecommendation {

    /** The [PracticeQuestionSource] the builder should open on when this recommendation is taken. */
    val source: PracticeQuestionSource

    data object WeakAreas : TopicPracticeRecommendation {
        override val source = PracticeQuestionSource.WEAK_AREAS
    }

    /** [count] is what the page states as the reason, and is never a promise about the run. */
    data class Mistakes(val count: Int) : TopicPracticeRecommendation {
        override val source = PracticeQuestionSource.UNRESOLVED_MISTAKES
    }

    data class Unseen(val count: Int) : TopicPracticeRecommendation {
        override val source = PracticeQuestionSource.UNSEEN
    }

    /** No evidence worth acting on. Ordinary practice over everything the Topic holds. */
    data object Everything : TopicPracticeRecommendation {
        override val source = PracticeQuestionSource.ALL
    }
}

/**
 * The recommendation this Topic's already-derived signals justify.
 *
 * [context] is `null` when analytics have not loaded or could not be derived, and
 * [unresolvedMistakeCount] is `null` when history is unknown for the same reason. Both degrade to
 * "no evidence" rather than to "no weakness, nothing to review": an unknown signal must never
 * produce a recommendation, and must never suppress one either — which is why they are asked
 * separately and a missing context still lets a known mistake count speak.
 *
 * Unseen deliberately requires something to have been attempted, matching [hasPartialCoverage] on
 * the shortcut it replaces. On a Topic nobody has touched, "unseen" is true of the entire bank, so
 * recommending it would promise a narrower run than ordinary practice while drawing from exactly the
 * same pool.
 */
internal fun topicPracticeRecommendation(
    context: LearningContextUiModel?,
    unresolvedMistakeCount: Int?,
): TopicPracticeRecommendation = when {
    context?.isWeak == true -> TopicPracticeRecommendation.WeakAreas
    unresolvedMistakeCount != null && unresolvedMistakeCount > 0 ->
        TopicPracticeRecommendation.Mistakes(unresolvedMistakeCount)
    context != null && context.attemptedQuestionCount > 0 && context.hasUnseenQuestions ->
        TopicPracticeRecommendation.Unseen(
            context.totalQuestionCount - context.attemptedQuestionCount,
        )
    else -> TopicPracticeRecommendation.Everything
}
