package org.artkachenko.kmp_learning_app.topic_study.practice_builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.assessment.AllQuestionLevels
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentSelectionResult
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

/**
 * Configures one targeted practice run, then hands the finished configuration to navigation.
 *
 * The builder sits between choosing a Topic or Subtopic and taking an assessment, and it owns every
 * rule about what a runnable configuration is: the defaults it opens on, the last-level protection,
 * which sources may be chosen, and whether the current combination has any content. Assessment
 * taking is unchanged behind it — this class produces an `AssessmentConfig.Focused` and nothing
 * else, so there is still one engine and one result flow.
 *
 * Eligibility is read through [AssessmentQuestionSelector], the same boundary the engine selects
 * with, and deliberately never through `AssessmentEngine.start`: starting persists an attempt, and
 * a screen that checks whether practice is possible must not create practice as a side effect.
 */
internal class PracticeBuilderViewModel(
    private val scope: AssessmentScope,
    private val curriculumRepository: CurriculumRepository,
    private val questionSelector: AssessmentQuestionSelector,
    /**
     * Which source the builder opens on. `ALL` is the entry from Topic Detail and stays the
     * default; a caller that already knows the practice intent — a remembered targeted run —
     * supplies it so the learner arrives on the setup they meant rather than on a reset one.
     *
     * It seeds the initial state and nothing more: availability, editing, and Start are unchanged,
     * so an arriving preset is inspected and preflighted exactly like a hand-made selection.
     */
    private val initialSource: PracticeQuestionSource = PracticeQuestionSource.ALL,
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<PracticeBuilderUiState> = _uiState.asStateFlow()

    private val _events = Channel<PracticeBuilderEvent>(Channel.BUFFERED)
    val events: Flow<PracticeBuilderEvent> = _events.receiveAsFlow()

    /** Held so a superseded eligibility read cannot land after the one that replaced it. */
    private var availabilityJob: Job? = null

    init {
        resolveScopeName()
        refreshAvailability()
    }

    /**
     * Count does not change which Questions are eligible, only how many of them are asked, so this
     * deliberately does not re-run the eligibility read. Selection takes what exists when fewer
     * Questions are eligible than requested, which is the pre-existing focused-practice contract.
     */
    fun selectQuestionCount(questionCount: Int) {
        if (questionCount !in PracticeQuestionCountOptions) return
        _uiState.update { it.copy(questionCount = questionCount) }
    }

    /**
     * Toggles one level, except that the last selected level cannot be removed.
     *
     * An empty selection is representable in the domain and is explicitly non-runnable there, so
     * the guard is not about avoiding a crash: it is about never leaving the learner on a screen
     * whose Start button has become impossible for a reason they did not intend.
     */
    fun toggleLevel(level: QuestionLevel) {
        val current = _uiState.value.levels
        val updated = if (level in current) {
            if (current.size == 1) return
            current - level
        } else {
            current + level
        }
        _uiState.update { it.copy(levels = updated) }
        refreshAvailability()
    }

    /** A source with no selection policy is not selectable, and never silently becomes ALL. */
    fun selectSource(source: PracticeQuestionSource) {
        if (!questionSelector.isSourceSupported(source)) return
        if (_uiState.value.source == source) return
        _uiState.update { it.copy(source = source) }
        refreshAvailability()
    }

    /**
     * Start is a request to navigate, not to run: the attempt is created by assessment taking from
     * the configuration this emits, so backing out of the builder leaves no history behind.
     */
    fun startPractice() {
        val state = _uiState.value
        if (!state.isStartEnabled) return
        viewModelScope.launch {
            _events.send(PracticeBuilderEvent.StartPractice(state.toAssessmentConfig()))
        }
    }

    fun retryAvailability() {
        refreshAvailability()
    }

    private fun PracticeBuilderUiState.toAssessmentConfig(): AssessmentConfig.Focused =
        AssessmentConfig.Focused(
            scope = this@PracticeBuilderViewModel.scope,
            questionCount = questionCount,
            levels = levels,
            source = source,
        )

    private fun initialState(): PracticeBuilderUiState =
        PracticeBuilderUiState(
            scope = PracticeScopeUiModel(
                kind = when (scope) {
                    is AssessmentScope.Topic -> PracticeScopeKind.TOPIC
                    is AssessmentScope.Subtopic -> PracticeScopeKind.SUBTOPIC
                },
            ),
            questionCount = DefaultPracticeQuestionCount,
            questionCountOptions = PracticeQuestionCountOptions,
            // Opening on every level reproduces the run the old one-tap entry started, so the
            // builder costs a returning learner one extra tap and no decisions.
            levels = AllQuestionLevels,
            // A source with no selection policy is refused here for the same reason selectSource
            // refuses it: an unselectable source must not become the state the screen opens on.
            source = initialSource.takeIf(questionSelector::isSourceSupported)
                ?: PracticeQuestionSource.ALL,
            sourceOptions = PracticeQuestionSource.entries.map { source ->
                PracticeSourceOption(
                    source = source,
                    isAvailable = questionSelector.isSourceSupported(source),
                )
            },
            availability = PracticeAvailability.Checking,
        )

    /**
     * The name is looked up from the stable ID rather than carried through navigation, so the
     * heading follows the curriculum instead of a label frozen into the back stack. A scope that
     * cannot be resolved leaves the name absent; whether practice can run is answered separately
     * by the eligibility read, which is the question Start actually depends on.
     */
    private fun resolveScopeName() {
        viewModelScope.launch {
            val name = runCatching {
                when (val scope = scope) {
                    is AssessmentScope.Topic ->
                        curriculumRepository.getTopicById(scope.topicId)?.name
                    is AssessmentScope.Subtopic ->
                        curriculumRepository.getSubtopicById(scope.subtopicId)?.name
                }
            }.getOrNull()
            _uiState.update { it.copy(scope = it.scope.copy(name = name)) }
        }
    }

    private fun refreshAvailability() {
        availabilityJob?.cancel()
        _uiState.update { it.copy(availability = PracticeAvailability.Checking) }
        availabilityJob = viewModelScope.launch {
            val availability = try {
                when (val selection = questionSelector.select(currentConfig())) {
                    is AssessmentSelectionResult.Selected ->
                        PracticeAvailability.Available(selection.questions.size)
                    // Every no-content reason is the same answer here: this configuration has
                    // nothing to ask. The typed reasons stay useful at the selection boundary,
                    // but the builder's own invariants already rule out the two it could
                    // otherwise report.
                    is AssessmentSelectionResult.NoContent ->
                        PracticeAvailability.NoEligibleQuestions
                }
            } catch (cancellation: CancellationException) {
                // Rethrown rather than folded into Error: this read was superseded by a newer
                // selection, and reporting a failure for it would overwrite the newer answer.
                throw cancellation
            } catch (@Suppress("TooGenericExceptionCaught") failure: Throwable) {
                PracticeAvailability.Error
            }
            _uiState.update { it.copy(availability = availability) }
        }
    }

    private fun currentConfig(): AssessmentConfig.Focused = _uiState.value.toAssessmentConfig()
}
