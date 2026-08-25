package org.artkachenko.kmp_learning_app.data.local.curriculum.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "topic")
internal data class TopicEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val status: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
)
