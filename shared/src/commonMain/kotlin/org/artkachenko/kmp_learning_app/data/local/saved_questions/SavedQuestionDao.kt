package org.artkachenko.kmp_learning_app.data.local.saved_questions

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import org.artkachenko.kmp_learning_app.data.local.saved_questions.entity.SavedQuestionEntity

@Dao
internal interface SavedQuestionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(savedQuestion: SavedQuestionEntity)

    @Query("DELETE FROM saved_question WHERE question_id = :questionId")
    suspend fun deleteByQuestionId(questionId: String)

    @Query("SELECT * FROM saved_question WHERE question_id = :questionId")
    suspend fun getByQuestionId(questionId: String): SavedQuestionEntity?

    @Query(
        """
        SELECT *
        FROM saved_question
        ORDER BY saved_at_epoch_millis DESC, question_id ASC
        """,
    )
    suspend fun getAll(): List<SavedQuestionEntity>

    @Query("SELECT COUNT(*) FROM saved_question")
    suspend fun count(): Int
}
