package org.artkachenko.kmp_learning_app.saved_questions

import org.artkachenko.kmp_learning_app.assessment_review.ReviewSourceUiModel
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibility

/**
 * Turns saved identities into reviewable content, one stable-ID lookup at a time.
 *
 * Saved state and Question content are separate truths with separate owners:
 * [SavedQuestionStateHolder] says which IDs are saved and in what order, and the curriculum says
 * what those IDs currently resolve to. Keeping the resolution here rather than in the ViewModel is
 * what makes the three outcomes — ACTIVE, DEPRECATED, and gone — testable on their own.
 *
 * The lookup is deliberately [CurriculumRepository.getQuestionsByIds], the historical resolver, and
 * never an ACTIVE listing: a saved identity is learner-owned and outlives the Question's place in
 * the current catalogue, so a DEPRECATED Question stays reviewable exactly like an ACTIVE one.
 *
 * It is also where the learner's [CurriculumVisibility] applies to saved content. The saved
 * identities themselves are never filtered — [SavedQuestionStateHolder] keeps every one, and nothing
 * is unsaved — but a resolved Question whose Topic is hidden is left out of what is presented, and
 * returns as soon as that Topic is visible again.
 */
internal class SavedQuestionContentResolver(
    private val curriculumRepository: CurriculumRepository,
) {
    /**
     * Resolves [savedQuestions] in the order given, which is the repository's own saved order.
     *
     * Nothing is re-sorted, filtered, or grouped by resolution outcome: a missing Question keeps
     * its position among the ones around it, because the order the learner saved things in is the
     * order they browse them in.
     *
     * A null lookup is a [SavedQuestionItem.Missing]; a failing lookup is a thrown exception, and
     * the caller decides what an unreadable curriculum means. The two must not be conflated — a
     * database failure is not evidence that a Question no longer exists.
     *
     * A Question that resolves under a Topic [visibility] hides is omitted. An unresolved ID stays
     * [SavedQuestionItem.Missing]: its Topic cannot be classified, so it is not known to be hidden.
     * Status plays no part — a DEPRECATED Question of a visible Topic stays.
     */
    suspend fun resolve(
        savedQuestions: List<SavedQuestion>,
        visibility: CurriculumVisibility,
    ): List<SavedQuestionItem> {
        // Nothing saved means nothing to resolve, and the curriculum is not read for it.
        if (savedQuestions.isEmpty()) return emptyList()
        // The whole saved list is resolved in one read rather than one per identity. Every unsave
        // changes the saved list and so re-runs this for everything still on it, which is what
        // makes the per-id cost repeat rather than being paid once when the screen opens.
        val questionsById = curriculumRepository.getQuestionsByIds(
            savedQuestions.mapTo(mutableSetOf(), SavedQuestion::questionId),
        )
        return savedQuestions.mapNotNull { saved ->
            val question = questionsById[saved.questionId]
                ?: return@mapNotNull SavedQuestionItem.Missing(saved)
            if (!visibility.isTopicVisible(question.topicId)) return@mapNotNull null
            SavedQuestionItem.Available(saved, question.toSavedQuestionContent())
        }
    }
}

private fun Question.toSavedQuestionContent(): SavedQuestionContentUiModel =
    SavedQuestionContentUiModel(
        questionId = id,
        text = text,
        // Authored order, not the per-attempt shuffle assessment review replays: there is no
        // attempt here whose arrangement could be reproduced.
        answers = answers.map { answer ->
            SavedQuestionAnswerUiModel(
                id = answer.id,
                text = answer.text,
                isCorrectAnswer = answer.id in correctAnswerIds,
            )
        },
        explanation = explanation,
        sources = sources.map { ReviewSourceUiModel(it.title, it.url) },
    )
