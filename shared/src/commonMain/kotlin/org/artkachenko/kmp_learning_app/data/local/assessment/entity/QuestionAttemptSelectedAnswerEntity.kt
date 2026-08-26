package org.artkachenko.kmp_learning_app.data.local.assessment.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.AnswerOptionEntity

@Entity(
    tableName = "question_attempt_selected_answer",
    primaryKeys = ["test_attempt_id", "question_id", "answer_id"],
    foreignKeys = [
        ForeignKey(
            entity = QuestionAttemptEntity::class,
            parentColumns = ["test_attempt_id", "question_id"],
            childColumns = ["test_attempt_id", "question_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = AnswerOptionEntity::class,
            parentColumns = ["question_id", "id"],
            childColumns = ["question_id", "answer_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["question_id", "answer_id"]),
    ],
)
internal data class QuestionAttemptSelectedAnswerEntity(
    @ColumnInfo(name = "test_attempt_id")
    val testAttemptId: String,
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "answer_id")
    val answerId: String,
)
