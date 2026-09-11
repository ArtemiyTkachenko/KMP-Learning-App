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
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingContext
import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingResolver
import org.artkachenko.kmp_learning_app.guided_learning.LearningRecommendation
import org.artkachenko.kmp_learning_app.guided_learning.LearningRecommendationRationale
import org.artkachenko.kmp_learning_app.guided_learning.LearningRecommendationResolver
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.lesson_study.ContinueLearningOutcome
import org.artkachenko.kmp_learning_app.lesson_study.ContinueLearningPolicy
import org.artkachenko.kmp_learning_app.lesson_study.ContinueLearningTarget
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressState
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressStateHolder
import org.artkachenko.kmp_learning_app.ui.LearningContextIndex

/**
 * Topic discovery, enriched with what the learner has done with each Topic and what to do next.
 *
 * The inputs are held apart on purpose, because they fail and change independently:
 *
 * - the [catalog] is the primary capability and the only one that can produce Loading, Empty, or
 *   Error. Browsing, searching, and opening a Topic must keep working when analytics do not;
 * - the [query] belongs to the learner, so it lives outside both loads. A history refresh rebuilds
 *   the rows underneath an active search without disturbing what was typed;
 * - [learningContexts], [recommendedNext], and [continueStudying] are optional enrichment derived
 *   from the shared history cache. Until a derivation succeeds they stay null, and the screen
 *   simply omits them: unknown history is not empty history, and must never render as "not studied
 *   yet", as a shortcut into a context the learner does not have, or as advice for a learner whose
 *   history nobody could read;
 * - [learningUnitCounts] is optional enrichment from a different publisher-owned source. It answers
 *   "what is there to read here?" while the history enrichment answers "what has this learner
 *   done?", so the two are never mixed. The assessment curriculum stays the authoritative
 *   catalogue: unreadable learning content costs a Topic its availability marker and nothing else;
 * - [continueLearning] is a third kind of enrichment again, over the authored learning sequence and
 *   the learner's own study record. It shares the learning-content read above and observes the
 *   app-scoped study projection every other Learn surface observes, so it can disagree with neither.
 *   Either input being unknown costs the card and nothing else.
 *
 * The three guided surfaces answer different questions and are never combined: [continueStudying] is
 * recency ("take me back to what I was doing"), [recommendedNext] is learning priority ("what
 * should I do now?"), and [continueLearning] is authored sequence ("what should I read next?"). They
 * may point somewhere different, and none is suppressed, deduplicated, or re-decided because of
 * another. Only [continueLearning] is derived without touching assessment history at all.
 *
 * Each writer updates its own input and re-renders, rather than the state being combined
 * asynchronously, so a retry shows its spinner on the same frame it is requested.
 */
internal class TopicBrowserViewModel(
    private val curriculumRepository: CurriculumRepository,
    private val learningContentRepository: LearningContentRepository,
    private val learningProgressService: LearningProgressService,
    private val historyStore: AssessmentHistoryStore,
    private val continueStudyingResolver: ContinueStudyingResolver,
    private val learningRecommendationResolver: LearningRecommendationResolver,
    private val studyProgressStateHolder: StudyProgressStateHolder,
) : ViewModel() {
    private val _uiState = MutableStateFlow<TopicBrowserUiState>(TopicBrowserUiState.Loading)
    val uiState: StateFlow<TopicBrowserUiState> = _uiState.asStateFlow()

    private var catalog: TopicCatalog = TopicCatalog.Loading
    /** Identifies the newest catalogue request, so a slower earlier one cannot write over it. */
    private var catalogGeneration: Int = 0
    private var query: String = ""
    /**
     * Active Learning Units per Topic, or `null` while unknown. `null` and an entry of `0` are
     * deliberately different answers, and the whole map is `null` rather than partially filled
     * because one read either produced availability for the loaded catalogue or produced none.
     */
    private var learningUnitCounts: Map<String, Int>? = null
    /**
     * ACTIVE Units in global authored order, or `null` while unknown.
     *
     * Held as domain Units rather than as a resolved next Lesson so a study-state change re-derives
     * the answer without reading learning content again — the same reason Topic Detail keeps its own
     * ACTIVE Unit list beside its row models.
     */
    private var activeLearningUnits: List<LearningUnit>? = null
    private var studyState: StudyProgressState = StudyProgressState.Loading
    private var learningContexts: LearningContextIndex? = null
    private var continueStudying: ContinueStudyingContext? = null
    private var recommendedNext: LearningRecommendation? = null

    init {
        observeLearningContext()
        observeStudyState()
        loadCatalog()
    }

    fun retry() {
        // The study record is re-read too, so a learner who retries after a failed read recovers the
        // Continue Learning card as well as the catalogue. Content and study state stay independent
        // reads; this only triggers both.
        studyProgressStateHolder.refresh()
        loadCatalog()
    }

    /**
     * Follows the app-scoped study projection rather than reading the study table for itself.
     *
     * This screen usually stays alive underneath Topic Detail, the Unit overview, and the Lesson
     * reader, so a Lesson marked three destinations deeper has to reach it on the way back without
     * anything being reloaded. Observing the one holder is what makes that true, and is also what
     * stops Continue Learning from disagreeing with the studied indicators shown on those screens.
     */
    private fun observeStudyState() {
        studyProgressStateHolder.refresh()
        viewModelScope.launch {
            studyProgressStateHolder.state.collect { state ->
                studyState = state
                render()
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        // In-memory only: typing filters the catalog that is already loaded and never touches the
        // repository, the history cache, or the coverage derivation.
        this.query = query
        render()
    }

    /**
     * Follows the app-scoped history cache rather than reading completed attempts again, so a newly
     * completed assessment refreshes this screen's learning context, its recommendation, and its
     * Continue Studying shortcut through the same invalidation every other consumer uses, without a
     * restart, a manual retry, or anything cached between emissions.
     *
     * All three derivations read the same emission, in one sequential collector: `collect`
     * processes an emission to completion before taking the next, so two history refreshes cannot
     * interleave and an older derivation cannot land on top of a newer one. There is no second
     * history collector and no independent `getCompletedAttempts` read.
     *
     * `attempts` is non-null only for `Loaded`, which is the distinction the recommendation rests
     * on: a loaded empty history is a learner with no completed study and may produce the
     * deterministic new-user recommendation, while Loading or Failed history is simply unknown and
     * must never be presented as either.
     */
    private fun observeLearningContext() {
        viewModelScope.launch {
            historyStore.history.collect { history ->
                val attempts = (history as? AssessmentHistory.Loaded)?.attempts
                // A failed derivation is treated exactly like history that has not arrived: the
                // catalog stays browsable and loses only its decoration. No derivation can turn
                // this screen into an Error, and none can hide another's result.
                val progress = attempts?.derivedOrNull { learningProgressService.load(it) }
                val nextLearningContexts = progress?.let(::LearningContextIndex)
                // The one progress derivation above is reused rather than a second load being
                // issued for the recommendation: Topic rows and the recommendation describe the
                // same history, so they must not be able to disagree about it either.
                val nextRecommendedNext = if (attempts != null && progress != null) {
                    attempts.derivedOrNull {
                        learningRecommendationResolver.resolve(it, progress)
                    }
                } else {
                    null
                }
                val nextContinueStudying = attempts?.derivedOrNull {
                    continueStudyingResolver.resolve(it)
                }
                // Publish the three answers together. Catalogue, search, and study-state work can
                // render while either resolver above is suspended; assigning earlier would expose
                // topic progress from the new history beside cards from the previous one.
                learningContexts = nextLearningContexts
                recommendedNext = nextRecommendedNext
                continueStudying = nextContinueStudying
                render()
            }
        }
    }

    private suspend fun <T> List<TestAttempt>.derivedOrNull(
        derive: suspend (List<TestAttempt>) -> T?,
    ): T? = runCatching { derive(this) }.getOrNull()

    /**
     * Loads the catalogue, then enriches it with learning availability in the same coroutine.
     *
     * Sequential rather than parallel, and rendering in between: the catalogue is what decides
     * Loading, Empty, and Error, so it is published the moment it arrives and the rows become
     * browsable while availability is still unknown. Learning content never gets a loading state,
     * an error state, or a say in whether the screen can be used.
     *
     * [catalogGeneration] exists because `retry()` can be pressed while a load is still running.
     * Only the newest request may write, so a slower earlier load — catalogue or enrichment —
     * cannot land on top of a newer one.
     */
    private fun loadCatalog() {
        val generation = ++catalogGeneration
        catalog = TopicCatalog.Loading
        // The previous catalogue's availability describes Topics that are being reloaded, so it is
        // dropped back to unknown rather than shown against whatever arrives next. The authored
        // sequence goes with it: both describe the same document read.
        learningUnitCounts = null
        activeLearningUnits = null
        render()
        viewModelScope.launch {
            val catalog = runCatching { readCatalog() }.getOrElse { TopicCatalog.Error }
            if (generation != catalogGeneration) return@launch
            this@TopicBrowserViewModel.catalog = catalog
            render()
            if (catalog is TopicCatalog.Loaded) {
                loadLearningContent(catalog.topics, generation)
            }
        }
    }

    /**
     * Reads what the learning document says, in one pass: ACTIVE Unit counts per Topic for the
     * availability markers, and the ACTIVE Units in global authored order for Continue Learning.
     *
     * Both come from the same cached document through the same boundary, and both are published
     * together, so an availability marker and the next-Lesson answer can never describe different
     * reads. A failure leaves both unknown and returns quietly: there is no error state and no retry
     * of its own, because the learner loses two decorations and no capability. The repository caches
     * one validated document, so every lookup here is a map read over one load.
     */
    private suspend fun loadLearningContent(topics: List<Topic>, generation: Int) {
        val content = runCatching {
            LearningContentEnrichment(
                unitCounts = topics.associate { topic ->
                    topic.id to learningContentRepository.getActiveUnitsByTopic(topic.id).size
                },
                activeUnits = learningContentRepository.getActiveUnits(),
            )
        }.getOrNull() ?: return
        if (generation != catalogGeneration) return
        learningUnitCounts = content.unitCounts
        activeLearningUnits = content.activeUnits
        render()
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
            is TopicCatalog.Loaded -> catalog.toContent(
                query = query,
                learningUnitCounts = learningUnitCounts,
                learningContexts = learningContexts,
                continueStudying = continueStudying,
                recommendedNext = recommendedNext,
                continueLearning = continueLearning(),
            )
        }
    }

    /**
     * Resolves the next Lesson from the two current inputs, or nothing when either is unknown.
     *
     * Unknown content and an unknown or unreadable study record both return `null` rather than being
     * treated as an empty document or an empty studied set — an unreadable study record would
     * otherwise present the very first Lesson of the curriculum as "next" to a learner who has read
     * half of it, which is the fabrication the study-progress contract exists to forbid.
     *
     * Derived on every render rather than cached, from state this screen already holds: the walk is
     * a pass over a list already in memory, and caching it would be a fourth place study state could
     * go stale.
     */
    private fun continueLearning(): ContinueLearningUiModel? {
        val units = activeLearningUnits ?: return null
        val studiedLessonIds = (studyState as? StudyProgressState.Loaded)
            ?.studiedLessonIds
            ?: return null

        return when (val outcome = ContinueLearningPolicy.resolve(units, studiedLessonIds)) {
            is ContinueLearningOutcome.Next -> units.toUiModel(outcome.target)
            // Worth saying: the learner has finished everything currently published.
            ContinueLearningOutcome.Complete -> ContinueLearningUiModel.Complete
            // Not worth saying: there is nothing to study, and no card can change that.
            ContinueLearningOutcome.Empty -> null
        }
    }
}

/** The two answers one read of the learning document gives this screen. */
private class LearningContentEnrichment(
    val unitCounts: Map<String, Int>,
    val activeUnits: List<LearningUnit>,
)

/**
 * Names the chosen Lesson and its Unit from the same list the policy walked.
 *
 * The lookup cannot fail — the target was produced from these very Units — but it is expressed as a
 * lookup rather than as a `require`, so a future policy that ever returned a foreign ID would cost
 * the card instead of crashing the catalogue.
 */
private fun List<LearningUnit>.toUiModel(target: ContinueLearningTarget): ContinueLearningUiModel? {
    val unit = firstOrNull { it.id == target.unitId } ?: return null
    val lesson = unit.lessons.firstOrNull { it.id == target.lessonId } ?: return null
    return ContinueLearningUiModel.Next(
        target = target,
        lessonTitle = lesson.title,
        unitTitle = unit.title,
    )
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
    learningUnitCounts: Map<String, Int>?,
    learningContexts: LearningContextIndex?,
    continueStudying: ContinueStudyingContext?,
    recommendedNext: LearningRecommendation?,
    continueLearning: ContinueLearningUiModel?,
): TopicBrowserUiState.Content {
    val items = topics.map { topic ->
        TopicBrowserItemUiModel(
            topicId = topic.id,
            topicName = topic.name,
            // Joined by stable Topic ID against one derivation, never per row.
            learningContext = learningContexts?.forTopic(topic.id),
            // Absent until the whole map is known, so an unreadable learning curriculum leaves the
            // row saying nothing rather than claiming the Topic has no study material.
            learningUnitCount = learningUnitCounts?.get(topic.id),
        )
    }
    val tokens = query.searchTokens()
    if (tokens.isEmpty()) {
        return TopicBrowserUiState.Content(
            topics = items,
            searchableSubtopics = searchableSubtopics,
            query = query,
            // All three guided surfaces belong to browsing, so they are attached here and nowhere
            // else: an active query keeps the screen on what was asked for rather than adding
            // unrelated cards. None is a search result, and none is filtered by the query text.
            continueStudying = continueStudying,
            recommendedNext = recommendedNext?.let { toUiModel(it) },
            continueLearning = continueLearning,
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

/**
 * Carries the policy's decision through unchanged, resolving only the display name its unseen
 * rationale cannot carry.
 *
 * The name is read from the catalogue this screen already holds, by stable Topic ID, so a renamed
 * Topic reads correctly with nothing stored or migrated — and a Topic the catalogue no longer lists
 * simply leaves the name absent rather than withholding the recommendation the policy made.
 */
private fun TopicCatalog.Loaded.toUiModel(
    recommendation: LearningRecommendation,
): RecommendedNextUiModel =
    RecommendedNextUiModel(
        target = recommendation.target,
        rationale = recommendation.rationale,
        topicName = (recommendation.rationale as? LearningRecommendationRationale.UnseenCoverage)
            ?.let { rationale -> topics.firstOrNull { it.id == rationale.topicId }?.name },
    )

private fun String.searchTokens(): List<String> =
    trim()
        .lowercase()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }

private fun String.matchesAll(tokens: List<String>): Boolean {
    val candidate = lowercase()
    return tokens.all(candidate::contains)
}
