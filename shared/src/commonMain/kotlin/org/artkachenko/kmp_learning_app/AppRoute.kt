package org.artkachenko.kmp_learning_app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface AppRoute : NavKey {
    @Serializable
    data object Topics : AppRoute

    @Serializable
    data object Interview : AppRoute

    @Serializable
    data object Progress : AppRoute

    @Serializable
    data class ProgressTopic(
        val topicId: String,
    ) : AppRoute

    @Serializable
    data object MistakeReview : AppRoute

    @Serializable
    data class Topic(
        val topicId: String,
    ) : AppRoute

    @Serializable
    data class MixedInterview(
        val questionCount: Int,
    ) : AppRoute

    @Serializable
    data class MixedInterviewAttempt(
        val attemptId: String,
    ) : AppRoute

    @Serializable
    data class MixedInterviewResult(
        val attemptId: String,
    ) : AppRoute

    @Serializable
    data class FocusedTopicPractice(
        val topicId: String,
        val questionCount: Int,
    ) : AppRoute

    @Serializable
    data class FocusedSubtopicPractice(
        val subtopicId: String,
        val questionCount: Int,
    ) : AppRoute

    @Serializable
    data class FocusedPracticeResult(
        val attemptId: String,
    ) : AppRoute

    @Serializable
    data class FocusedPracticeAttempt(
        val attemptId: String,
    ) : AppRoute
}
