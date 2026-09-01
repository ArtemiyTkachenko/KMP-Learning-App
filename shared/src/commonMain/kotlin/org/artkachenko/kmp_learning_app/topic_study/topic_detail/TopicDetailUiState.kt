package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.ui.LearningContextUiModel

internal sealed interface TopicDetailUiState {
    data object Loading : TopicDetailUiState

    data class Content(
        val topic: Topic,
        /** Authored ACTIVE questions in this Topic, which is what practice can draw from. */
        val topicQuestionCount: Int,
        val subtopics: List<SubtopicPracticeItem>,
        /**
         * Coverage and accuracy for the whole Topic, or `null` when analytics have not loaded or
         * could not be derived. Curriculum failure is an Error state; analytics failure is only an
         * absent summary, because starting practice must never depend on a statistic.
         */
        val learningContext: LearningContextUiModel? = null,
    ) : TopicDetailUiState

    data class NoQuestions(
        val topic: Topic,
    ) : TopicDetailUiState

    data object NotFound : TopicDetailUiState

    data object Error : TopicDetailUiState
}
