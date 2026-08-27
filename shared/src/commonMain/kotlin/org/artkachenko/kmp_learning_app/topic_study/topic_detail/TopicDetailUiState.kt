package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import org.artkachenko.kmp_learning_app.curriculum.Topic

internal sealed interface TopicDetailUiState {
    data object Loading : TopicDetailUiState

    data class Content(
        val topic: Topic,
        val topicQuestionCount: Int,
        val subtopics: List<SubtopicPracticeItem>,
    ) : TopicDetailUiState

    data class NoQuestions(
        val topic: Topic,
    ) : TopicDetailUiState

    data object NotFound : TopicDetailUiState

    data object Error : TopicDetailUiState
}
