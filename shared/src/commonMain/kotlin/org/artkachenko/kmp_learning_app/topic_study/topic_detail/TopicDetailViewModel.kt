package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistory
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.ui.LearningContextIndex

/**
 * One Topic's study and practice surface, enriched with what the learner has done with it.
 *
 * Three inputs, held apart because they fail and change independently:
 *
 * - the [curriculum] is the primary capability and the only one that can produce Loading, NotFound,
 *   or Error. It decides whether the Topic exists — and, through its question count alone, whether
 *   the Topic can be practised;
 * - [learningUnits] is authored study material from a different publisher-owned source. It has its
 *   own loading and failure states inside [TopicDetailUiState.Content] because an unreadable
 *   learning document must not take away a practiceable Topic, and a Topic that is still resolving
 *   its Units must stay fully practiceable while it does;
 * - [learningContexts] is optional analytics derived from the shared history cache. Until a
 *   derivation succeeds it stays null and the screen omits the summary: unknown history is not
 *   empty history, and an optional statistic must never block starting practice.
 *
 * Each writer updates its own input and re-renders rather than the state being awaited together, so
 * the Topic becomes visible and practiceable the moment the curriculum lands.
 */
internal class TopicDetailViewModel(
    private val topicId: String,
    private val curriculumRepository: CurriculumRepository,
    private val learningContentRepository: LearningContentRepository,
    private val learningProgressService: LearningProgressService,
    private val historyStore: AssessmentHistoryStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow<TopicDetailUiState>(TopicDetailUiState.Loading)
    val uiState: StateFlow<TopicDetailUiState> = _uiState.asStateFlow()

    private var curriculum: TopicCurriculum = TopicCurriculum.Loading
    private var learningUnits: TopicLearningUnitsUiState = TopicLearningUnitsUiState.Loading
    /** Identifies the newest load, so a slower earlier one cannot write over it. */
    private var loadGeneration: Int = 0
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

    /**
     * The scope a practice action configures, or null when this screen has nothing to practise.
     *
     * These used to return a whole `AssessmentConfig.Focused` built from a fixed question count,
     * because practice started immediately. The Practice Builder owns the count, levels, and
     * source now, so this screen contributes only the stable scope it is showing — and still
     * refuses to hand out a Subtopic it does not list.
     *
     * The question-count guard replaces what the removed `NoQuestions` state used to enforce:
     * `Content` now means the Topic exists, so an empty Topic reaches this function and must still
     * refuse to produce a startable scope.
     */
    fun topicPracticeScope(): AssessmentScope? =
        (uiState.value as? TopicDetailUiState.Content)
            ?.takeIf { it.topicQuestionCount > 0 }
            ?.let { state -> AssessmentScope.Topic(state.topic.id) }

    fun subtopicPracticeScope(subtopicId: String): AssessmentScope? =
        (uiState.value as? TopicDetailUiState.Content)
            ?.subtopics
            ?.firstOrNull { it.subtopic.id == subtopicId }
            ?.let { item -> AssessmentScope.Subtopic(item.subtopic.id) }

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

    /**
     * Loads the curriculum, then the Topic's study material in the same coroutine.
     *
     * Sequential rather than parallel, and rendering in between: the curriculum is what decides
     * Loading, NotFound, and Error, so it is published the moment it arrives and practice becomes
     * available while the study section is still unknown. Learning content is loaded only once a
     * Topic has actually resolved, so a malformed learning document naming a retired Topic cannot
     * produce orphaned Units on a screen that should be NotFound.
     */
    private fun loadTopic() {
        val generation = ++loadGeneration
        curriculum = TopicCurriculum.Loading
        // The previous read described a Topic that is being loaded again, so study material drops
        // back to unknown rather than being shown against whatever arrives next.
        learningUnits = TopicLearningUnitsUiState.Loading
        render()
        viewModelScope.launch {
            val curriculum = runCatching { readCurriculum() }.getOrElse { TopicCurriculum.Error }
            if (generation != loadGeneration) return@launch
            this@TopicDetailViewModel.curriculum = curriculum
            render()
            if (curriculum is TopicCurriculum.Loaded) {
                loadLearningUnits(curriculum.topic.id, generation)
            }
        }
    }

    /**
     * Reads ACTIVE Units for this Topic by stable Topic ID, in the order the repository returns
     * them: that order is authored pedagogical order and is never re-sorted here.
     *
     * A failure becomes [TopicLearningUnitsUiState.Unavailable] rather than an empty list or a
     * screen-level Error, so the learner is told study material could not be read instead of being
     * told there is none — and keeps every practice action either way.
     */
    private suspend fun loadLearningUnits(topicId: String, generation: Int) {
        val units = runCatching {
            TopicLearningUnitsUiState.Available(
                learningContentRepository.getActiveUnitsByTopic(topicId).toLearningUnitItems(),
            )
        }.getOrElse { TopicLearningUnitsUiState.Unavailable }
        if (generation != loadGeneration) return
        learningUnits = units
        render()
    }

    /**
     * The Topic and what can be practised in it. A Topic with no ACTIVE Questions is still a loaded
     * Topic: whether it can be practised is a count, not a different kind of screen.
     */
    private suspend fun readCurriculum(): TopicCurriculum {
        val topic = curriculumRepository.getActiveTopics()
            .firstOrNull { it.id == topicId }
            ?: return TopicCurriculum.NotFound

        val subtopics = curriculumRepository.getActiveSubtopics(topic.id)
        val questions = curriculumRepository.getActiveQuestionsByTopic(topic.id)
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
                learningUnits = learningUnits,
                learningContext = contexts?.forTopic(curriculum.topic.id),
            )
        }
    }
}

/** The curriculum half of the screen, which alone decides whether the Topic exists. */
private sealed interface TopicCurriculum {
    data object Loading : TopicCurriculum

    data object Error : TopicCurriculum

    data object NotFound : TopicCurriculum

    data class Loaded(
        val topic: Topic,
        val questionCount: Int,
        val subtopics: List<Pair<Subtopic, Int>>,
    ) : TopicCurriculum
}
