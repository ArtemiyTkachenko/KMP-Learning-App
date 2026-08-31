package org.artkachenko.kmp_learning_app.data.local.curriculum.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "question",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topic_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = SubtopicEntity::class,
            parentColumns = ["topic_id", "id"],
            childColumns = ["topic_id", "subtopic_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["topic_id", "status"]),
        Index(value = ["subtopic_id", "status"]),
        Index(value = ["topic_id", "subtopic_id"]),
    ],
)
internal data class QuestionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "topic_id")
    val topicId: String,
    @ColumnInfo(name = "subtopic_id")
    val subtopicId: String,
    val text: String,
    @ColumnInfo(name = "selection_mode")
    val selectionMode: String,
    val explanation: String,
    val status: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
)
