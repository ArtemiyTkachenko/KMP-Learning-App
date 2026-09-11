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

/**
 * Configures one targeted practice run, then hands the finished configuration to navigation.
 *
 * The builder sits between choosing something to practise and taking an assessment, and it owns
 * every rule about what a runnable configuration is: the defaults it opens on, the last-level
 * protection, which sources may be chosen, and whether the current combination has any content.
 * Assessment taking is unchanged behind it — this class produces an `AssessmentConfig.Focused` and
 * nothing else, so there is still one engine and one result flow.
 *
 * It is opened on a [PracticeBuilderTarget] rather than on an `AssessmentScope`, because the two
 * stopped being the same thing once a Learning Unit could be practised: a Unit's scope is the set
 * of concepts its current Lessons teach, which exists only after [PracticeTargetResolver] has read
 * it. The scope is therefore held as state that arrives, and Start stays withheld until it does —
 * a builder that guessed a scope while resolving could preflight, and then run, an assessment the
 * learner never asked for.
 *
 * Eligibility is read through [AssessmentQuestionSelector], the same boundary the engine selects
 * with, and deliberately never through `AssessmentEngine.start`: starting persists an attempt, and
 * a screen that checks whether practice is possible must not create practice as a side effect.
 */
internal class PracticeBuilderViewModel(
    private val target: PracticeBuilderTarget,
    private val targetResolver: PracticeTargetResolver,
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
    private var resolveJob: Job? = null

    /**
     * The assessment scope, once the target has resolved into one. Null while resolving and for a
     * target that never becomes practiceable, which is exactly when there is no configuration to
     * preflight or start.
     */
    private var scope: AssessmentScope? = null

    init {
        resolveTarget()
    }

    fun selectQuestionCount(questionCount: Int) {
        if (questionCount !in _uiState.value.questionCountOptions) return
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
        val scope = scope ?: return
        if (!state.isStartEnabled) return
        viewModelScope.launch {
            _events.send(PracticeBuilderEvent.StartPractice(state.toAssessmentConfig(scope)))
        }
    }

    /**
     * Retries whichever read failed. A target that never resolved is re-resolved, because the
     * eligibility read it would otherwise repeat has no scope to run against.
     */
    fun retryAvailability() {
        if (scope == null) resolveTarget() else refreshAvailability()
    }

    private fun PracticeBuilderUiState.toAssessmentConfig(
        scope: AssessmentScope,
    ): AssessmentConfig.Focused =
        AssessmentConfig.Focused(
            scope = scope,
            questionCount = questionCount,
            levels = levels,
            source = source,
        )

    private fun initialState(): PracticeBuilderUiState =
        PracticeBuilderUiState(
            scope = PracticeScopeUiModel(
                // From the target, so the screen can say what is being configured before the
                // content read has said which one.
                kind = when (target) {
                    is PracticeBuilderTarget.Topic -> PracticeScopeKind.TOPIC
                    is PracticeBuilderTarget.Subtopic -> PracticeScopeKind.SUBTOPIC
                    is PracticeBuilderTarget.LearningUnit -> PracticeScopeKind.LEARNING_UNIT
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
     * Resolves the target from its stable ID rather than from anything carried through navigation,
     * so both the heading and the concepts practised follow current content instead of a snapshot
     * frozen into the back stack.
     *
     * Only a resolved scope goes on to the eligibility read. The three other outcomes are terminal
     * for this screen: the content is gone, it teaches nothing assessable, or the document could
     * not be read at all — and only the last of those is worth retrying, which is why it reports
     * as [PracticeAvailability.Error] rather than as one of the settled states.
     */
    private fun resolveTarget() {
        resolveJob?.cancel()
        availabilityJob?.cancel()
        scope = null
        _uiState.update { it.copy(availability = PracticeAvailability.Checking) }
        resolveJob = viewModelScope.launch {
            val resolution = try {
                targetResolver.resolve(target)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (@Suppress("TooGenericExceptionCaught") failure: Throwable) {
                null
            }
            when (resolution) {
                is PracticeTargetResolution.Resolved -> {
                    scope = resolution.scope
                    _uiState.update { it.copy(scope = it.scope.copy(name = resolution.name)) }
                    refreshAvailability()
                }

                PracticeTargetResolution.Unavailable -> updateAvailability(
                    PracticeAvailability.TargetUnavailable,
                )

                PracticeTargetResolution.NoPracticeableConcepts -> updateAvailability(
                    PracticeAvailability.NoPracticeableConcepts,
                )

                null -> updateAvailability(PracticeAvailability.Error)
            }
        }
    }

    private fun refreshAvailability() {
        val scope = scope ?: return
        availabilityJob?.cancel()
        _uiState.update { it.copy(availability = PracticeAvailability.Checking) }
        availabilityJob = viewModelScope.launch {
            val availability = try {
                // Ask the selector for a deliberately large run to discover the actual eligible
                // pool. The normal selector remains the single selection boundary; the builder
                // merely uses that answer to avoid promising an impossible session length.
                when (val selection = questionSelector.select(currentConfig(scope, Int.MAX_VALUE))) {
                    is AssessmentSelectionResult.Selected -> {
                        val available = selection.questions.size
                        val presets = PracticeQuestionCountOptions.filter { it <= available }
                        // Preserve familiar presets and append the exact pool size whenever the
                        // pool falls between them. It is the only truthful way to offer all six,
                        // eight, or seventeen matching Questions without calling that option ten.
                        val options = when {
                            available == 0 -> emptyList()
                            available in presets -> presets
                            else -> presets + available
                        }
                        val selected = _uiState.value.questionCount
                            .takeIf { it in options }
                            ?: options.lastOrNull()
                            ?: DefaultPracticeQuestionCount
                        _uiState.update {
                            it.copy(questionCount = selected, questionCountOptions = options)
                        }
                        PracticeAvailability.Available(available)
                    }
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
            updateAvailability(availability)
        }
    }

    private fun updateAvailability(availability: PracticeAvailability) {
        _uiState.update { it.copy(availability = availability) }
    }

    private fun currentConfig(scope: AssessmentScope, questionCount: Int = _uiState.value.questionCount): AssessmentConfig.Focused =
        _uiState.value.toAssessmentConfig(scope).copy(questionCount = questionCount)
}
