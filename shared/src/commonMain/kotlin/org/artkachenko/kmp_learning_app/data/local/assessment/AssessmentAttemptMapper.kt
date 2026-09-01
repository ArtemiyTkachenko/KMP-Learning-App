package org.artkachenko.kmp_learning_app.data.local.assessment

import kotlin.time.Instant
import org.artkachenko.kmp_learning_app.assessment.AllQuestionLevels
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.inAuthoredOrder
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.data.local.assessment.entity.QuestionAttemptEntity
import org.artkachenko.kmp_learning_app.data.local.assessment.entity.QuestionAttemptSelectedAnswerEntity
import org.artkachenko.kmp_learning_app.data.local.assessment.entity.TestAttemptEntity

private const val ConfigTypeFocused = "FOCUSED"
private const val ConfigTypeMixed = "MIXED"
private const val ScopeTypeTopic = "TOPIC"
private const val ScopeTypeSubtopic = "SUBTOPIC"
private const val PracticeLevelSeparator = ","

internal data class AssessmentAttemptPersistenceSnapshot(
    val testAttempt: TestAttemptEntity,
    val questionAttempts: List<QuestionAttemptEntity>,
    val selectedAnswers: List<QuestionAttemptSelectedAnswerEntity>,
)

internal fun TestAttempt.toPersistenceSnapshot(): AssessmentAttemptPersistenceSnapshot =
    AssessmentAttemptPersistenceSnapshot(
        testAttempt = toEntity(),
        questionAttempts = questionAttempts.mapIndexed { index, questionAttempt ->
            questionAttempt.toEntity(
                testAttemptId = id,
                sortOrder = index,
            )
        },
        selectedAnswers = questionAttempts.flatMap { questionAttempt ->
            questionAttempt.toSelectedAnswerEntities(testAttemptId = id)
        },
    )

internal fun toDomainTestAttempt(
    attempt: TestAttemptEntity,
    questionAttempts: List<QuestionAttemptEntity>,
    selectedAnswers: List<QuestionAttemptSelectedAnswerEntity>,
): TestAttempt {
    val selectedAnswerIdsByQuestion =
        selectedAnswers.groupBy { it.questionId }
            .mapValues { (_, answers) -> answers.map { it.answerId }.toSet() }

    return TestAttempt(
        id = attempt.id,
        config = attempt.toDomainConfig(),
        questionAttempts = questionAttempts.map { questionAttempt ->
            questionAttempt.toDomain(
                selectedAnswerIds = selectedAnswerIdsByQuestion[questionAttempt.questionId].orEmpty(),
            )
        },
        status = attempt.toDomainStatus(),
        startedAt = Instant.fromEpochMilliseconds(attempt.startedAtEpochMillis),
        completedAt = attempt.completedAtEpochMillis?.let(Instant::fromEpochMilliseconds),
        score = attempt.toDomainScore(),
    )
}

private fun TestAttempt.toEntity(): TestAttemptEntity {
    val configFields = config.toPersistenceConfigFields()
    return TestAttemptEntity(
        id = id,
        configType = configFields.configType,
        requestedQuestionCount = config.questionCount,
        scopeType = configFields.scopeType,
        scopeId = configFields.scopeId,
        practiceLevels = configFields.practiceLevels,
        practiceSource = configFields.practiceSource,
        status = status.name,
        scoreTotalQuestions = score?.totalQuestions,
        scoreCorrectAnswers = score?.correctAnswers,
        startedAtEpochMillis = startedAt.toEpochMilliseconds(),
        completedAtEpochMillis = completedAt?.toEpochMilliseconds(),
    )
}

/**
 * The attempt record stores what was practised: type, scope, requested count, and — since the
 * Practice Builder can produce them — the practised levels and question source.
 *
 * Those two were deliberately left out while no UI could narrow a run, so every stored FOCUSED
 * attempt genuinely was an all-levels
 * [org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource.ALL] request. That stopped
 * being true once a learner could practise, say, only ADVANCED Questions: history would describe
 * the attempt as something they never ran, and retake — which re-runs the reconstructed config —
 * would widen back to the whole scope.
 *
 * MIXED keeps writing nulls because it has no level or source dimension at all, which is also what
 * every pre-EPIC-16 row holds after the v6 migration; [toDomainConfig] reads a null on a FOCUSED
 * row back as the historical all-levels `ALL` semantics.
 */
private fun AssessmentConfig.toPersistenceConfigFields(): PersistenceConfigFields =
    when (this) {
        is AssessmentConfig.Focused -> {
            val scopeFields = scope.toPersistenceScopeFields()
            PersistenceConfigFields(
                configType = ConfigTypeFocused,
                scopeType = scopeFields.scopeType,
                scopeId = scopeFields.scopeId,
                practiceLevels = levels.toPersistedPracticeLevels(),
                practiceSource = source.name,
            )
        }
        is AssessmentConfig.Mixed ->
            PersistenceConfigFields(
                configType = ConfigTypeMixed,
                scopeType = null,
                scopeId = null,
                practiceLevels = null,
                practiceSource = null,
            )
    }

private fun Set<QuestionLevel>.toPersistedPracticeLevels(): String =
    inAuthoredOrder().joinToString(PracticeLevelSeparator) { it.name }

private fun AssessmentScope.toPersistenceScopeFields(): PersistenceScopeFields =
    when (this) {
        is AssessmentScope.Topic ->
            PersistenceScopeFields(
                scopeType = ScopeTypeTopic,
                scopeId = topicId,
            )
        is AssessmentScope.Subtopic ->
            PersistenceScopeFields(
                scopeType = ScopeTypeSubtopic,
                scopeId = subtopicId,
            )
    }

private fun QuestionAttempt.toEntity(
    testAttemptId: String,
    sortOrder: Int,
): QuestionAttemptEntity =
    QuestionAttemptEntity(
        testAttemptId = testAttemptId,
        questionId = questionId,
        sortOrder = sortOrder,
        isCorrect = when (val state = answerState) {
            QuestionAnswerState.Unanswered -> null
            is QuestionAnswerState.Answered -> state.isCorrect
        },
    )

private fun QuestionAttempt.toSelectedAnswerEntities(
    testAttemptId: String,
): List<QuestionAttemptSelectedAnswerEntity> =
    when (val state = answerState) {
        QuestionAnswerState.Unanswered -> emptyList()
        is QuestionAnswerState.Answered ->
            state.selectedAnswerIds.map { answerId ->
                QuestionAttemptSelectedAnswerEntity(
                    testAttemptId = testAttemptId,
                    questionId = questionId,
                    answerId = answerId,
                )
            }
    }

private fun TestAttemptEntity.toDomainConfig(): AssessmentConfig =
    when (configType) {
        ConfigTypeMixed -> {
            require(scopeType == null && scopeId == null) {
                "MIXED assessment config must not have scope fields."
            }
            require(practiceLevels == null && practiceSource == null) {
                "MIXED assessment config must not have practice selection fields."
            }
            AssessmentConfig.Mixed(questionCount = requestedQuestionCount)
        }
        ConfigTypeFocused -> {
            require(scopeType != null && scopeId != null) {
                "FOCUSED assessment config must have scope fields."
            }
            AssessmentConfig.Focused(
                scope = when (scopeType) {
                    ScopeTypeTopic -> AssessmentScope.Topic(scopeId)
                    ScopeTypeSubtopic -> AssessmentScope.Subtopic(scopeId)
                    else -> error("Unknown focused assessment scope type: $scopeType.")
                },
                questionCount = requestedQuestionCount,
                // A row written before v6 has no recorded selection, and every such attempt was an
                // all-levels ALL run. Defaulting rather than failing is what keeps that history
                // readable, and it is the only place the legacy semantics are reconstructed.
                levels = practiceLevels?.toDomainPracticeLevels() ?: AllQuestionLevels,
                source = practiceSource?.let(PracticeQuestionSource::valueOf)
                    ?: PracticeQuestionSource.ALL,
            )
        }
        else -> error("Unknown assessment config type: $configType.")
    }

private fun String.toDomainPracticeLevels(): Set<QuestionLevel> =
    if (isEmpty()) {
        emptySet()
    } else {
        split(PracticeLevelSeparator).map(QuestionLevel::valueOf).toSet()
    }

private fun QuestionAttemptEntity.toDomain(
    selectedAnswerIds: Set<String>,
): QuestionAttempt =
    QuestionAttempt(
        questionId = questionId,
        answerState = if (isCorrect == null) {
            require(selectedAnswerIds.isEmpty()) {
                "Unanswered question attempts must not have selected answers."
            }
            QuestionAnswerState.Unanswered
        } else {
            require(selectedAnswerIds.isNotEmpty()) {
                "Answered question attempts must have selected answers."
            }
            QuestionAnswerState.Answered(
                selectedAnswerIds = selectedAnswerIds,
                isCorrect = isCorrect,
            )
        },
    )

private fun TestAttemptEntity.toDomainStatus(): AssessmentStatus =
    AssessmentStatus.valueOf(status)

private fun TestAttemptEntity.toDomainScore(): AssessmentScore? =
    when (toDomainStatus()) {
        AssessmentStatus.IN_PROGRESS -> {
            require(scoreTotalQuestions == null && scoreCorrectAnswers == null) {
                "IN_PROGRESS attempts must not have score fields."
            }
            require(completedAtEpochMillis == null) {
                "IN_PROGRESS attempts must not have completed timestamp."
            }
            null
        }
        AssessmentStatus.COMPLETED -> {
            require(scoreTotalQuestions != null && scoreCorrectAnswers != null) {
                "COMPLETED attempts must have score fields."
            }
            require(completedAtEpochMillis != null) {
                "COMPLETED attempts must have completed timestamp."
            }
            AssessmentScore(
                totalQuestions = scoreTotalQuestions,
                correctAnswers = scoreCorrectAnswers,
            )
        }
    }

private data class PersistenceConfigFields(
    val configType: String,
    val scopeType: String?,
    val scopeId: String?,
    val practiceLevels: String?,
    val practiceSource: String?,
)

private data class PersistenceScopeFields(
    val scopeType: String,
    val scopeId: String,
)
