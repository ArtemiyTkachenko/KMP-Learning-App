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
    /**
     * The practised `QuestionLevel` names, comma separated in enum order, or null.
     *
     * Null carries a meaning the mapper depends on: either a MIXED attempt, which has no level
     * dimension, or a FOCUSED attempt written before targeted practice existed, which was
     * unavoidably an all-levels run. A closed three-value set read only alongside its attempt does
     * not earn a join table; the ordering is normalised on write so equal selections compare equal.
     */
    @ColumnInfo(name = "practice_levels")
    val practiceLevels: String?,
    /** The `PracticeQuestionSource` name, or null for MIXED and for pre-EPIC-16 FOCUSED rows. */
    @ColumnInfo(name = "practice_source")
    val practiceSource: String?,
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
