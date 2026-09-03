package org.artkachenko.kmp_learning_app.saved_questions

/**
 * What a review surface currently knows about the learner's saved Questions.
 *
 * [Loading] and [Error] are deliberately distinct from an empty [Loaded]: a surface that cannot read
 * saved state does not know a Question is unsaved, so it offers no save affordance rather than
 * showing every Question as confidently unsaved.
 */
internal sealed interface SavedQuestionsState {
    data object Loading : SavedQuestionsState

    /**
     * [savedQuestions] is the repository's own list, in its own order, because that ordering is the
     * saved-state answer a browsing surface needs. [pendingQuestionIds] names the Questions whose
     * save or unsave is currently being persisted; only those Questions lose their action, so a
     * mutation on one card never disables another.
     */
    data class Loaded(
        val savedQuestions: List<SavedQuestion>,
        val pendingQuestionIds: Set<String> = emptySet(),
    ) : SavedQuestionsState {
        /** Membership only, derived for per-card rendering; [savedQuestions] stays canonical. */
        val savedQuestionIds: Set<String> =
            savedQuestions.mapTo(mutableSetOf()) { it.questionId }
    }

    data object Error : SavedQuestionsState
}
