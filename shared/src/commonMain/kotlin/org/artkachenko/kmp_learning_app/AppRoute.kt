package org.artkachenko.kmp_learning_app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel

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
        val subtopicId: String? = null,
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

    /**
     * The Practice Builder, scoped by the stable ID it was opened from.
     *
     * Only the ID travels: the Topic or Subtopic name is resolved from the curriculum on arrival,
     * so a renamed Topic cannot be shown under a stale label saved into the back stack.
     */
    @Serializable
    data class PracticeBuilderTopic(
        val topicId: String,
    ) : AppRoute

    @Serializable
    data class PracticeBuilderSubtopic(
        val subtopicId: String,
    ) : AppRoute

    /**
     * A configured practice run.
     *
     * Every dimension the builder exposes is carried as a typed field, because the destination
     * rebuilds `AssessmentConfig.Focused` from the route and a missing dimension would silently
     * become its default — practising all levels when the learner asked for one. Content is still
     * addressed only by stable ID; no Question, answer, or curriculum text passes through here.
     */
    @Serializable
    data class FocusedTopicPractice(
        val topicId: String,
        val questionCount: Int,
        val levels: List<QuestionLevel>,
        val source: PracticeQuestionSource,
    ) : AppRoute

    @Serializable
    data class FocusedSubtopicPractice(
        val subtopicId: String,
        val questionCount: Int,
        val levels: List<QuestionLevel>,
        val source: PracticeQuestionSource,
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
