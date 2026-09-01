package org.artkachenko.kmp_learning_app.learning_progress

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
