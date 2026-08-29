package org.artkachenko.kmp_learning_app.learning_progress

internal data class LearningProgressSnapshot(
    val completedAttemptCount: Int,
    val answeredQuestionCount: Int,
    val correctAnswerCount: Int,
    val percentage: Double,
    val topics: List<TopicPerformance>,
    val subtopics: List<SubtopicPerformance>,
    val weakAreas: List<WeakArea>,
)

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
