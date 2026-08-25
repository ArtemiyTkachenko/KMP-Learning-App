package org.artkachenko.kmp_learning_app.data.local.curriculum.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey

@Entity(
    tableName = "question_source",
    primaryKeys = ["question_id", "url"],
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
)
internal data class QuestionSourceEntity(
    @ColumnInfo(name = "question_id")
    val questionId: String,
    val url: String,
    val title: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
)
