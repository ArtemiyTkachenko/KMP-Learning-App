package org.artkachenko.kmp_learning_app.data.local.saved_questions.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity

@Entity(
    tableName = "saved_question",
    primaryKeys = ["question_id"],
)
internal data class SavedQuestionEntity(
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "saved_at_epoch_millis")
    val savedAtEpochMillis: Long,
)
