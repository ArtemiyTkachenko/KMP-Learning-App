package org.artkachenko.kmp_learning_app.data.local.lesson_study.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity

/**
 * Deliberately has no foreign key: Learning Lessons are bundled publisher documents rather than
 * Room rows, and a learner's claim is allowed to outlive the material it was made about.
 */
@Entity(
    tableName = "studied_lesson",
    primaryKeys = ["lesson_id"],
)
internal data class StudiedLessonEntity(
    @ColumnInfo(name = "lesson_id")
    val lessonId: String,
    @ColumnInfo(name = "studied_at_epoch_millis")
    val studiedAtEpochMillis: Long,
)
