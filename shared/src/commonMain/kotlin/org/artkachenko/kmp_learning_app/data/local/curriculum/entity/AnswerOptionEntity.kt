package org.artkachenko.kmp_learning_app.data.local.curriculum.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey

@Entity(
    tableName = "answer_option",
    primaryKeys = ["question_id", "id"],
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
)
internal data class AnswerOptionEntity(
    @ColumnInfo(name = "question_id")
    val questionId: String,
    val id: String,
    val text: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
)
