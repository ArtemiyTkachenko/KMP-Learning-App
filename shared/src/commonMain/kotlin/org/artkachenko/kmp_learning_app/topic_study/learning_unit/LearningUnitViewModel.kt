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
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository

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
 */
internal class LearningUnitViewModel(
    private val unitId: String,
    private val learningContentRepository: LearningContentRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<LearningUnitUiState>(LearningUnitUiState.Loading)
    val uiState: StateFlow<LearningUnitUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        require(unitId.isNotBlank()) { "unitId must not be blank." }
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.value = LearningUnitUiState.Loading
        loadJob = viewModelScope.launch {
            try {
                _uiState.value = loadState()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                // The document could not be read, which is a different statement from this Unit not
                // existing: the learner is offered Retry rather than being told the Unit is gone.
                _uiState.value = LearningUnitUiState.Error
            }
        }
    }

    /**
     * A Unit with no current Lessons still renders as [LearningUnitUiState.Content].
     *
     * Content authoring should not produce one, but an ACTIVE Unit whose Lessons have all been
     * retired is a coherent document — and an overview that crashed or claimed the Unit did not
     * exist would misreport it. The screen says so in words instead.
     */
    private suspend fun loadState(): LearningUnitUiState {
        val unit = learningContentRepository.getUnitById(unitId)
            ?.takeIf { it.status == ContentStatus.ACTIVE }
            ?: return LearningUnitUiState.NotFound

        return LearningUnitUiState.Content(
            unitId = unit.id,
            title = unit.title,
            summary = unit.summary,
            lessons = unit.toActiveLessonItems(),
        )
    }
}
