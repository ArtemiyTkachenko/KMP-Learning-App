package org.artkachenko.kmp_learning_app.data.local.curriculum.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "subtopic",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topic_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["topic_id", "id"], unique = true),
        Index(value = ["topic_id", "status"]),
    ],
)
internal data class SubtopicEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "topic_id")
    val topicId: String,
    val name: String,
    val status: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
)
