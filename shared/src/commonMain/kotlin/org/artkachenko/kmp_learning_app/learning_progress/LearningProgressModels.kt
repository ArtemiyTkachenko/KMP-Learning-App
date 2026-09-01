package org.artkachenko.kmp_learning_app.learning_progress

import kotlin.time.Instant

internal data class LearningProgressSnapshot(
    val completedAttemptCount: Int,
    val answeredQuestionCount: Int,
    val correctAnswerCount: Int,
    val percentage: Double,
    val topics: List<TopicPerformance>,
    val subtopics: List<SubtopicPerformance>,
    val weakAreas: List<WeakArea>,
    val coverage: CurriculumCoverage,
    val topicCoverage: List<TopicCoverage>,
    val subtopicCoverage: List<SubtopicCoverage>,
    val recentPerformance: RecentPerformance,
)

/**
 * How much of the CURRENT ACTIVE curriculum the learner has encountered.
 *
 * Coverage is deliberately separate from the performance models above: performance is
 * occurrence-based (answering the same Question five times counts five times), while coverage
 * counts each stable Question ID at most once. Coverage also spans scopes with no history at all,
 * which [TopicPerformance] cannot express, because "never attempted" and "attempted badly" are
 * different learning signals.
 */
internal sealed interface QuestionCoverage {
    val attemptedQuestionCount: Int
    val totalQuestionCount: Int

    /**
     * `null` when there is no ACTIVE curriculum in scope: 0/0 means "nothing to cover", which is
     * not the same statement as "0% covered", and presentation should be free to say so.
     */
    val percentage: Double?
        get() =
            if (totalQuestionCount == 0) {
                null
            } else {
                attemptedQuestionCount.toDouble() / totalQuestionCount * 100.0
            }
}

internal data class CurriculumCoverage(
    override val attemptedQuestionCount: Int,
    override val totalQuestionCount: Int,
) : QuestionCoverage

internal data class TopicCoverage(
    val topicId: String,
    override val attemptedQuestionCount: Int,
    override val totalQuestionCount: Int,
) : QuestionCoverage

internal data class SubtopicCoverage(
    val topicId: String,
    val subtopicId: String,
    override val attemptedQuestionCount: Int,
    override val totalQuestionCount: Int,
) : QuestionCoverage

/**
 * Performance across the latest completed assessments, as defined by [RecentPerformancePolicy].
 *
 * Held as the two ordered series rather than as pre-computed totals so that the summary can only
 * ever describe the whole window: [answerSeries] is capped for presentation, and a summary stored
 * alongside it could silently start describing the truncated tail instead. Everything a caller reads
 * as a total is therefore derived from [attemptSeries], which is never truncated.
 */
internal data class RecentPerformance(
    /** The window, oldest -> newest, so a chart reads past -> present. */
    val attemptSeries: List<RecentAttemptPerformance>,
    /**
     * Individual answer outcomes from the same window, oldest -> newest, capped at
     * [RecentPerformancePolicy.MaxRecentAnswerOutcomes].
     *
     * Ordering is between attempts by completion time and within an attempt by stored assessment
     * sequence. `QuestionAttempt` records no answer timestamp, so this is a sequence, not a
     * wall-clock ordering of when each answer was given.
     */
    val answerSeries: List<RecentAnswerOutcome>,
) {
    val attemptCount: Int get() = attemptSeries.size

    val answeredQuestionCount: Int
        get() = attemptSeries.sumOf(RecentAttemptPerformance::answeredQuestionCount)

    val correctAnswerCount: Int
        get() = attemptSeries.sumOf(RecentAttemptPerformance::correctAnswerCount)

    /**
     * Question-weighted accuracy across the window: correct answers over answered questions, never
     * the mean of the attempt percentages, because attempts contain different numbers of questions.
     *
     * `null` rather than 0.0 when there is no recent evidence at all: a learner who has completed
     * nothing has not scored 0%, and presentation must be free to say so.
     */
    val percentage: Double?
        get() =
            if (answeredQuestionCount == 0) {
                null
            } else {
                correctAnswerCount.toDouble() / answeredQuestionCount * 100.0
            }

    val trendAvailability: RecentTrendAvailability
        get() = RecentPerformancePolicy.trendAvailability(attemptCount)
}

/**
 * One completed assessment in the recent window. Deliberately not a `TestAttempt`: presentation of a
 * trend needs the shape of the result, not the questions, answers, or configuration behind it.
 */
internal data class RecentAttemptPerformance(
    val attemptId: String,
    val completedAt: Instant,
    val answeredQuestionCount: Int,
    val correctAnswerCount: Int,
) {
    init {
        // A completed attempt always has at least one answered question, so the accuracy below is
        // always defined; anything else means the window was built from a malformed attempt.
        require(answeredQuestionCount > 0) {
            "answeredQuestionCount must be greater than zero."
        }
        require(correctAnswerCount in 0..answeredQuestionCount) {
            "correctAnswerCount must be between zero and answeredQuestionCount."
        }
    }

    val percentage: Double
        get() = correctAnswerCount.toDouble() / answeredQuestionCount * 100.0
}

/**
 * One answered question inside the recent window, carrying only what a trend needs. Question text,
 * selected answers, and curriculum metadata are deliberately absent: this is performance history,
 * and reloading content for it would make the derivation depend on the current question bank.
 */
internal data class RecentAnswerOutcome(
    val attemptId: String,
    val questionId: String,
    val isCorrect: Boolean,
)

/**
 * Whether the recent window holds enough completed assessments to present as a trajectory.
 *
 * This states only whether a trend is presentable, never which direction it points: the attempt
 * percentages are exposed raw so the learner reads the actual numbers instead of a derived score.
 */
internal sealed interface RecentTrendAvailability {
    data class InsufficientHistory(
        val attemptCount: Int,
        val requiredAttemptCount: Int,
    ) : RecentTrendAvailability

    data object Available : RecentTrendAvailability
}

internal data class TopicPerformance(
    val topicId: String,
    val topicName: String?,
    val answeredCount: Int,
    val correctCount: Int,
    val percentage: Double,
    val isWeak: Boolean,
)

internal data class SubtopicPerformance(
    val subtopicId: String,
    val subtopicName: String?,
    val topicId: String,
    val topicName: String?,
    val answeredCount: Int,
    val correctCount: Int,
    val percentage: Double,
    val isWeak: Boolean,
)

internal sealed interface WeakArea {
    val answeredCount: Int
    val correctCount: Int
    val percentage: Double

    data class Topic(
        val performance: TopicPerformance,
    ) : WeakArea {
        override val answeredCount: Int = performance.answeredCount
        override val correctCount: Int = performance.correctCount
        override val percentage: Double = performance.percentage
    }

    data class Subtopic(
        val performance: SubtopicPerformance,
    ) : WeakArea {
        override val answeredCount: Int = performance.answeredCount
        override val correctCount: Int = performance.correctCount
        override val percentage: Double = performance.percentage
    }
}
