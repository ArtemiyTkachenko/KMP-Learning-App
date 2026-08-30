package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService

internal class TopicDetailViewModel(
    private val topicId: String,
    private val curriculumRepository: CurriculumRepository,
    private val learningProgressService: LearningProgressService,
) : ViewModel() {
    private val _uiState = MutableStateFlow<TopicDetailUiState>(TopicDetailUiState.Loading)
    val uiState: StateFlow<TopicDetailUiState> = _uiState.asStateFlow()

    init {
        require(topicId.isNotBlank()) {
            "topicId must not be blank."
        }
        loadTopic()
    }

    fun retry() {
        loadTopic()
    }

    fun topicPracticeConfig(): AssessmentConfig.Focused? =
        (uiState.value as? TopicDetailUiState.Content)?.let { state ->
            AssessmentConfig.Focused(
                scope = AssessmentScope.Topic(state.topic.id),
                questionCount = FocusedPracticeQuestionCount,
            )
        }

    fun subtopicPracticeConfig(subtopicId: String): AssessmentConfig.Focused? =
        (uiState.value as? TopicDetailUiState.Content)
            ?.subtopics
            ?.firstOrNull { it.subtopic.id == subtopicId }
            ?.let { item ->
                AssessmentConfig.Focused(
                    scope = AssessmentScope.Subtopic(item.subtopic.id),
                    questionCount = FocusedPracticeQuestionCount,
                )
            }

    private fun loadTopic() {
        _uiState.value = TopicDetailUiState.Loading
        viewModelScope.launch {
            runCatching {
                val topic = curriculumRepository.getActiveTopics()
                    .firstOrNull { it.id == topicId }
                    ?: return@runCatching TopicDetailUiState.NotFound

                // Study and progress were separate views of the same topic: this screen listed
                // what is available to practise without ever saying how the learner is doing on
                // it. The snapshot is consumed as-is, never recomputed here.
                val snapshot = learningProgressService.load()
                val topicAccuracy = snapshot.topics
                    .firstOrNull { it.topicId == topic.id }
                    ?.percentage
                val subtopicAccuracy = snapshot.subtopics
                    .filter { it.topicId == topic.id }
                    .associate { it.subtopicId to it.percentage }

                val subtopics = curriculumRepository.getActiveSubtopics(topic.id)
                val questions = curriculumRepository.getActiveQuestionsByTopic(topic.id)
                val questionCounts = questions.groupingBy { it.subtopicId }.eachCount()
                val populatedSubtopics = subtopics.mapNotNull { subtopic ->
                    questionCounts[subtopic.id]
                        ?.takeIf { it > 0 }
                        ?.let { count ->
                            SubtopicPracticeItem(
                                subtopic = subtopic,
                                questionCount = count,
                                accuracyPercentage = subtopicAccuracy[subtopic.id],
                            )
                        }
                }

                if (questions.isEmpty()) {
                    TopicDetailUiState.NoQuestions(topic)
                } else {
                    TopicDetailUiState.Content(
                        topic = topic,
                        topicQuestionCount = questions.size,
                        subtopics = populatedSubtopics,
                        accuracyPercentage = topicAccuracy,
                    )
                }
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure {
                _uiState.value = TopicDetailUiState.Error
            }
        }
    }
}
