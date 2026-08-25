package org.artkachenko.kmp_learning_app.data.local.curriculum.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey

@Entity(
    tableName = "question_correct_answer",
    primaryKeys = ["question_id", "answer_id"],
    foreignKeys = [
        ForeignKey(
            entity = AnswerOptionEntity::class,
            parentColumns = ["question_id", "id"],
            childColumns = ["question_id", "answer_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
)
internal data class QuestionCorrectAnswerEntity(
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "answer_id")
    val answerId: String,
)
