package org.artkachenko.kmp_learning_app.data.local.saved_questions.repository

import kotlin.time.Clock
import kotlin.time.Instant
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.saved_questions.entity.SavedQuestionEntity
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestion
import org.artkachenko.kmp_learning_app.saved_questions.repository.SavedQuestionRepository

internal class LocalSavedQuestionRepository(
    private val database: CurriculumDatabase,
    private val now: () -> Instant = { Clock.System.now() },
) : SavedQuestionRepository {
    override suspend fun save(questionId: String) {
        database.savedQuestionDao().insert(
            SavedQuestionEntity(
                questionId = questionId,
                savedAtEpochMillis = now().toEpochMilliseconds(),
            ),
        )
    }

    override suspend fun unsave(questionId: String) {
        database.savedQuestionDao().deleteByQuestionId(questionId)
    }

    override suspend fun isSaved(questionId: String): Boolean =
        database.savedQuestionDao().getByQuestionId(questionId) != null

    override suspend fun getSavedQuestions(): List<SavedQuestion> =
        database.savedQuestionDao().getAll().map { savedQuestion ->
            SavedQuestion(
                questionId = savedQuestion.questionId,
                savedAtEpochMillis = savedQuestion.savedAtEpochMillis,
            )
        }
}
