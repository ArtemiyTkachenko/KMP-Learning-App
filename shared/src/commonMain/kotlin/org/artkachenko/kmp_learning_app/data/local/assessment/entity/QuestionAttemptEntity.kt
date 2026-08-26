package org.artkachenko.kmp_learning_app.data.local.assessment.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionEntity

@Entity(
    tableName = "question_attempt",
    primaryKeys = ["test_attempt_id", "question_id"],
    foreignKeys = [
        ForeignKey(
            entity = TestAttemptEntity::class,
            parentColumns = ["id"],
            childColumns = ["test_attempt_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["question_id"]),
    ],
)
internal data class QuestionAttemptEntity(
    @ColumnInfo(name = "test_attempt_id")
    val testAttemptId: String,
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    @ColumnInfo(name = "is_correct")
    val isCorrect: Boolean?,
)
