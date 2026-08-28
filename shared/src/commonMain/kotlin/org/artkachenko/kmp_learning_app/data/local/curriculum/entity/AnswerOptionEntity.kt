package org.artkachenko.kmp_learning_app.data.local.curriculum.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus

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
    /**
     * DEPRECATED marks an option the bundled curriculum no longer authors but that a
     * historical attempt still selected, so it cannot be deleted without breaking the
     * foreign key from question_attempt_selected_answer. Active curriculum queries
     * exclude these; getQuestionById keeps them so past attempts remain reviewable.
     */
    @ColumnInfo(name = "status")
    val status: String = ContentStatus.ACTIVE.name,
)
