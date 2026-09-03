package org.artkachenko.kmp_learning_app.saved_questions.repository

import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestion

/**
 * Stores saved Question identity only; content remains owned by CurriculumRepository.
 *
 * Saving an existing id is a no-op, including preserving its original timestamp. A saved id may
 * resolve through `CurriculumRepository.getQuestionById` to ACTIVE or DEPRECATED content, or to
 * null when that content is no longer available. None of those outcomes changes saved state.
 */
internal interface SavedQuestionRepository {
    suspend fun save(questionId: String)

    suspend fun unsave(questionId: String)

    suspend fun isSaved(questionId: String): Boolean

    /** Most recently newly saved first, with stable Question id as the tie-breaker. */
    suspend fun getSavedQuestions(): List<SavedQuestion>
}
