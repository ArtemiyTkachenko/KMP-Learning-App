package org.artkachenko.kmp_learning_app.data.local.assessment.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "test_attempt")
internal data class TestAttemptEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "config_type")
    val configType: String,
    @ColumnInfo(name = "requested_question_count")
    val requestedQuestionCount: Int,
    @ColumnInfo(name = "scope_type")
    val scopeType: String?,
    @ColumnInfo(name = "scope_id")
    val scopeId: String?,
    val status: String,
    @ColumnInfo(name = "score_total_questions")
    val scoreTotalQuestions: Int?,
    @ColumnInfo(name = "score_correct_answers")
    val scoreCorrectAnswers: Int?,
    @ColumnInfo(name = "started_at_epoch_millis")
    val startedAtEpochMillis: Long,
    @ColumnInfo(name = "completed_at_epoch_millis")
    val completedAtEpochMillis: Long?,
)
