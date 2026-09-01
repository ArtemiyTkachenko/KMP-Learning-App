package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistory
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.ui.LearningContextIndex

/**
 * One Topic's practice surface, enriched with what the learner has done with it.
 *
 * Curriculum and analytics are loaded separately and on purpose: the Topic, its Subtopics, and the
 * practice actions are the reason this screen exists, so a history that is still loading — or that
 * failed — leaves them fully usable and only removes the learning summary. The alternative, waiting
 * for both, would let an optional statistic block starting practice.
 */
internal class TopicDetailViewModel(
    private val topicId: String,
    private val curriculumRepository: CurriculumRepository,
    private val learningProgressService: LearningProgressService,
    private val historyStore: AssessmentHistoryStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow<TopicDetailUiState>(TopicDetailUiState.Loading)
    val uiState: StateFlow<TopicDetailUiState> = _uiState.asStateFlow()

    private var curriculum: TopicCurriculum = TopicCurriculum.Loading
    private var learningContexts: LearningContextIndex? = null

    init {
        require(topicId.isNotBlank()) {
            "topicId must not be blank."
        }
        observeLearningContext()
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

    /**
     * Follows the app-scoped history cache rather than reading completed attempts again, so
     * finishing an assessment refreshes this Topic's context through the same invalidation every
     * other consumer uses — without reloading the curriculum underneath it.
     */
    private fun observeLearningContext() {
        viewModelScope.launch {
            historyStore.history.collect { history ->
                learningContexts = when (history) {
                    // Unknown history, not empty history: with nothing derived the summary is
                    // omitted rather than announcing that the Topic has never been studied.
                    AssessmentHistory.Loading, AssessmentHistory.Failed -> null
                    is AssessmentHistory.Loaded -> runCatching {
                        LearningContextIndex(learningProgressService.load(history.attempts))
                    }.getOrNull()
                }
                render()
            }
        }
    }

    private fun loadTopic() {
        curriculum = TopicCurriculum.Loading
        render()
        viewModelScope.launch {
            curriculum = runCatching { readCurriculum() }.getOrElse { TopicCurriculum.Error }
            render()
        }
    }

    private suspend fun readCurriculum(): TopicCurriculum {
        val topic = curriculumRepository.getActiveTopics()
            .firstOrNull { it.id == topicId }
            ?: return TopicCurriculum.NotFound

        val subtopics = curriculumRepository.getActiveSubtopics(topic.id)
        val questions = curriculumRepository.getActiveQuestionsByTopic(topic.id)
        if (questions.isEmpty()) return TopicCurriculum.NoQuestions(topic)

        val questionCounts = questions.groupingBy { it.subtopicId }.eachCount()
        return TopicCurriculum.Loaded(
            topic = topic,
            questionCount = questions.size,
            // Only Subtopics that can actually be practised become rows.
            subtopics = subtopics.mapNotNull { subtopic ->
                questionCounts[subtopic.id]
                    ?.takeIf { it > 0 }
                    ?.let { count -> subtopic to count }
            },
        )
    }

    private fun render() {
        val contexts = learningContexts
        _uiState.value = when (val curriculum = curriculum) {
            TopicCurriculum.Loading -> TopicDetailUiState.Loading
            TopicCurriculum.Error -> TopicDetailUiState.Error
            TopicCurriculum.NotFound -> TopicDetailUiState.NotFound
            is TopicCurriculum.NoQuestions -> TopicDetailUiState.NoQuestions(curriculum.topic)
            is TopicCurriculum.Loaded -> TopicDetailUiState.Content(
                topic = curriculum.topic,
                topicQuestionCount = curriculum.questionCount,
                subtopics = curriculum.subtopics.map { (subtopic, questionCount) ->
                    SubtopicPracticeItem(
                        subtopic = subtopic,
                        questionCount = questionCount,
                        // Joined by stable Subtopic ID against the one derivation above.
                        learningContext = contexts?.forSubtopic(subtopic.id),
                    )
                },
                learningContext = contexts?.forTopic(curriculum.topic.id),
            )
        }
    }
}

/** The curriculum half of the screen, which alone decides whether the Topic can be practised. */
private sealed interface TopicCurriculum {
    data object Loading : TopicCurriculum

    data object Error : TopicCurriculum

    data object NotFound : TopicCurriculum

    data class NoQuestions(val topic: Topic) : TopicCurriculum

    data class Loaded(
        val topic: Topic,
        val questionCount: Int,
        val subtopics: List<Pair<Subtopic, Int>>,
    ) : TopicCurriculum
}
