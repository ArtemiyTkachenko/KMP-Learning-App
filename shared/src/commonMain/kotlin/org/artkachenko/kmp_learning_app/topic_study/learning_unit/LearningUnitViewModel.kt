package org.artkachenko.kmp_learning_app.topic_study.learning_unit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibility
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibilityStateHolder
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressDerivation
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressState
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressStateHolder
import org.artkachenko.kmp_learning_app.lesson_study.toUiState

/**
 * One Learning Unit, resolved from its stable ID on arrival.
 *
 * `getUnitById` answers a *historical identity* question: it resolves a Unit whatever its status,
 * so a stored reference to retired material stays readable. Normal study browsing asks a different
 * question — what should be studied now — so this destination requires
 * [ContentStatus.ACTIVE] on top of that lookup. The repository is not the place to enforce it:
 * filtering direct lookups there would take stable resolution away from everything else that
 * depends on it. Current browsing eligibility is presentation's rule, and this is where it lives.
 *
 * The Unit's home Topic is not re-read. The route was pushed from that Topic and the learning
 * document is validated against the Curriculum before it ever loads, so confirming the Topic exists
 * would be a second lookup that cannot fail differently — and the overview has no use for its name.
 *
 * Its home Topic's *visibility* is checked, because the identity lookup resolves hidden Units too: a
 * Unit whose `topicId` the learner's [CurriculumVisibility] hides is the existing
 * [LearningUnitUiState.NotFound], so a restored or parked route cannot show it. Visibility is
 * observed while this screen is alive, and a change reloads through the same load path.
 *
 * Study progress is derived here rather than read here. The overview keeps the resolved
 * [LearningUnit] and re-runs the pure [StudyProgressDerivation] whenever the app-scoped
 * [studyProgressStateHolder] publishes a new studied set, so a Lesson marked in the reader above
 * updates this still-alive screen on the way back without the publisher document being fetched
 * again. Publisher content and learner state have independent lifecycles, and reloading the Unit to
 * recount a number nobody re-authored would confuse the two.
 */
internal class LearningUnitViewModel(
    private val unitId: String,
    private val learningContentRepository: LearningContentRepository,
    private val studyProgressStateHolder: StudyProgressStateHolder,
    private val visibilityStateHolder: CurriculumVisibilityStateHolder,
) : ViewModel() {
    private val _uiState = MutableStateFlow<LearningUnitUiState>(LearningUnitUiState.Loading)
    val uiState: StateFlow<LearningUnitUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    /** The visibility the newest load was made under; see [observeVisibility]. */
    private var loadVisibility: CurriculumVisibility? = null

    /** The document half, plus the resolved Unit study progress is derived from. */
    private var content: LearningUnitUiState = LearningUnitUiState.Loading
    private var unit: LearningUnit? = null
    private var studyState: StudyProgressState = StudyProgressState.Loading

    init {
        require(unitId.isNotBlank()) { "unitId must not be blank." }
        observeStudyState()
        load()
        observeVisibility()
    }

    fun retry() {
        studyProgressStateHolder.refresh()
        load()
    }

    private fun observeStudyState() {
        studyProgressStateHolder.refresh()
        viewModelScope.launch {
            studyProgressStateHolder.state.collect { state ->
                studyState = state
                render()
            }
        }
    }

    /**
     * Reloads when the learner shows or hides optional content. The StateFlow hands a new collector
     * the visibility [load] has just requested under, so comparing against [loadVisibility] keeps
     * it from becoming a second startup read; [load] cancels a read made under the old visibility.
     */
    private fun observeVisibility() {
        viewModelScope.launch {
            visibilityStateHolder.visibility.collect { visibility ->
                if (visibility != loadVisibility) load()
            }
        }
    }

    private fun load() {
        loadJob?.cancel()
        content = LearningUnitUiState.Loading
        unit = null
        render()
        val visibility = visibilityStateHolder.visibility.value
        loadVisibility = visibility
        loadJob = viewModelScope.launch {
            content = try {
                loadState(visibility)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                // The document could not be read, which is a different statement from this Unit not
                // existing: the learner is offered Retry rather than being told the Unit is gone.
                unit = null
                LearningUnitUiState.Error
            }
            render()
        }
    }

    /**
     * The document decides which page this is; study state only annotates the one page that has a
     * Unit on it, so an unreadable study record can never produce Loading, NotFound, or Error.
     */
    private fun render() {
        val unit = unit
        _uiState.value = when (val content = content) {
            is LearningUnitUiState.Content -> if (unit == null) {
                content
            } else {
                content.copy(
                    studyProgress = studyState.toUiState { loaded ->
                        StudyProgressDerivation.deriveUnit(unit, loaded.studiedLessonIds)
                    },
                )
            }
            else -> content
        }
    }

    /**
     * A Unit with no current Lessons still renders as [LearningUnitUiState.Content].
     *
     * Content authoring should not produce one, but an ACTIVE Unit whose Lessons have all been
     * retired is a coherent document — and an overview that crashed or claimed the Unit did not
     * exist would misreport it. The screen says so in words instead.
     */
    private suspend fun loadState(visibility: CurriculumVisibility): LearningUnitUiState {
        val unit = learningContentRepository.getUnitById(unitId)
            ?.takeIf { it.status == ContentStatus.ACTIVE }
            // A hidden Unit is not current study material for this learner, which is exactly what
            // NotFound already says; no separate state tells them why.
            ?.takeIf { visibility.isTopicVisible(it.topicId) }
            ?: run {
                this.unit = null
                return LearningUnitUiState.NotFound
            }

        // Retained so a study-state emission re-derives progress from the Unit already read. The
        // derivation needs the authored Lessons and their statuses, which the row models
        // deliberately do not carry.
        this.unit = unit
        return LearningUnitUiState.Content(
            unitId = unit.id,
            title = unit.title,
            summary = unit.summary,
            lessons = unit.toActiveLessonItems(),
        )
    }
}
