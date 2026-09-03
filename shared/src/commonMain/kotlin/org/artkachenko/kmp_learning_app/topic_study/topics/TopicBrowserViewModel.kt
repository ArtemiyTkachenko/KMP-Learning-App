package org.artkachenko.kmp_learning_app.topic_study.topics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistory
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingContext
import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingResolver
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.ui.LearningContextIndex

/**
 * Topic discovery, enriched with what the learner has done with each Topic.
 *
 * The three inputs are held apart on purpose, because they fail and change independently:
 *
 * - the [catalog] is the primary capability and the only one that can produce Loading, Empty, or
 *   Error. Browsing, searching, and opening a Topic must keep working when analytics do not;
 * - the [query] belongs to the learner, so it lives outside both loads. A history refresh rebuilds
 *   the rows underneath an active search without disturbing what was typed;
 * - [learningContexts] and [continueStudying] are optional enrichment derived from the shared
 *   history cache. Until a derivation succeeds they stay null, and the screen simply omits them:
 *   unknown history is not empty history, and must never render as "not studied yet" or as a
 *   shortcut into a context the learner does not have.
 *
 * Each writer updates its own input and re-renders, rather than the state being combined
 * asynchronously, so a retry shows its spinner on the same frame it is requested.
 */
internal class TopicBrowserViewModel(
    private val curriculumRepository: CurriculumRepository,
    private val learningProgressService: LearningProgressService,
    private val historyStore: AssessmentHistoryStore,
    private val continueStudyingResolver: ContinueStudyingResolver,
) : ViewModel() {
    private val _uiState = MutableStateFlow<TopicBrowserUiState>(TopicBrowserUiState.Loading)
    val uiState: StateFlow<TopicBrowserUiState> = _uiState.asStateFlow()

    private var catalog: TopicCatalog = TopicCatalog.Loading
    private var query: String = ""
    private var learningContexts: LearningContextIndex? = null
    private var continueStudying: ContinueStudyingContext? = null

    init {
        observeLearningContext()
        loadCatalog()
    }

    fun retry() {
        loadCatalog()
    }

    fun onSearchQueryChange(query: String) {
        // In-memory only: typing filters the catalog that is already loaded and never touches the
        // repository, the history cache, or the coverage derivation.
        this.query = query
        render()
    }

    /**
     * Follows the app-scoped history cache rather than reading completed attempts again, so a newly
     * completed assessment refreshes this screen's learning context and its Continue Studying
     * shortcut through the same invalidation every other consumer uses, without a restart or a
     * manual retry.
     *
     * Both derivations read the same emission, in one sequential collector: `collect` processes an
     * emission to completion before taking the next, so two history refreshes cannot interleave and
     * an older derivation cannot land on top of a newer one. There is no second history collector
     * and no independent `getCompletedAttempts` read for Continue Studying.
     */
    private fun observeLearningContext() {
        viewModelScope.launch {
            historyStore.history.collect { history ->
                val attempts = (history as? AssessmentHistory.Loaded)?.attempts
                // A failed derivation is treated exactly like history that has not arrived: the
                // catalog stays browsable and loses only its decoration. Neither derivation can
                // turn this screen into an Error, and neither can hide the other's result.
                learningContexts = attempts?.derivedOrNull {
                    LearningContextIndex(learningProgressService.load(it))
                }
                continueStudying = attempts?.derivedOrNull {
                    continueStudyingResolver.resolve(it)
                }
                render()
            }
        }
    }

    private suspend fun <T> List<TestAttempt>.derivedOrNull(
        derive: suspend (List<TestAttempt>) -> T?,
    ): T? = runCatching { derive(this) }.getOrNull()

    private fun loadCatalog() {
        catalog = TopicCatalog.Loading
        render()
        viewModelScope.launch {
            catalog = runCatching { readCatalog() }.getOrElse { TopicCatalog.Error }
            render()
        }
    }

    private suspend fun readCatalog(): TopicCatalog {
        val topics = curriculumRepository.getActiveTopics()
        if (topics.isEmpty()) return TopicCatalog.Empty
        return TopicCatalog.Loaded(
            topics = topics,
            searchableSubtopics = topics.flatMap { topic ->
                curriculumRepository.getActiveSubtopics(topic.id).map { subtopic ->
                    SubtopicSearchResult(
                        subtopicId = subtopic.id,
                        subtopicName = subtopic.name,
                        parentTopicId = topic.id,
                        parentTopicName = topic.name,
                    )
                }
            },
        )
    }

    private fun render() {
        _uiState.value = when (val catalog = catalog) {
            TopicCatalog.Loading -> TopicBrowserUiState.Loading
            TopicCatalog.Empty -> TopicBrowserUiState.Empty
            TopicCatalog.Error -> TopicBrowserUiState.Error
            is TopicCatalog.Loaded -> catalog.toContent(query, learningContexts, continueStudying)
        }
    }
}

/** The catalog half of the screen, kept separate from the query and from learning context. */
private sealed interface TopicCatalog {
    data object Loading : TopicCatalog

    data object Empty : TopicCatalog

    data object Error : TopicCatalog

    data class Loaded(
        val topics: List<Topic>,
        val searchableSubtopics: List<SubtopicSearchResult>,
    ) : TopicCatalog
}

private fun TopicCatalog.Loaded.toContent(
    query: String,
    learningContexts: LearningContextIndex?,
    continueStudying: ContinueStudyingContext?,
): TopicBrowserUiState.Content {
    val items = topics.map { topic ->
        TopicBrowserItemUiModel(
            topicId = topic.id,
            topicName = topic.name,
            // Joined by stable Topic ID against one derivation, never per row.
            learningContext = learningContexts?.forTopic(topic.id),
        )
    }
    val tokens = query.searchTokens()
    if (tokens.isEmpty()) {
        return TopicBrowserUiState.Content(
            topics = items,
            searchableSubtopics = searchableSubtopics,
            query = query,
            // The shortcut belongs to browsing, so it is attached here and nowhere else: an active
            // query keeps the screen on what was asked for rather than adding an unrelated card.
            continueStudying = continueStudying,
        )
    }
    return TopicBrowserUiState.Content(
        topics = items,
        searchableSubtopics = searchableSubtopics,
        query = query,
        // Matching reads Topic and Subtopic names only. Learning context is display metadata, so a
        // query of "weak" or "76%" still finds curriculum by name or nothing at all.
        topicMatches = items.filter { it.topicName.matchesAll(tokens) },
        subtopicMatches = searchableSubtopics.filter { it.subtopicName.matchesAll(tokens) },
    )
}

private fun String.searchTokens(): List<String> =
    trim()
        .lowercase()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }

private fun String.matchesAll(tokens: List<String>): Boolean {
    val candidate = lowercase()
    return tokens.all(candidate::contains)
}
