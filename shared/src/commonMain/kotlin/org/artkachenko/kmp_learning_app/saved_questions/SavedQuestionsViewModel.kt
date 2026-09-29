package org.artkachenko.kmp_learning_app.saved_questions

import kotlin.coroutines.cancellation.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibility
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibilityStateHolder

/**
 * Presents the learner's saved Questions for review.
 *
 * The saved list itself is never read here: [savedQuestionStateHolder] is the app's one saved-state
 * projection, so a Question saved on a result screen appears in this list without a second
 * subscription to the repository, and a Question removed here disappears from the result screens
 * for the same reason.
 *
 * Content resolution is the only work this ViewModel adds, and it is kept strictly downstream of
 * saved state: the holder decides what is saved and in what order, [contentResolver] decides what
 * each identity currently resolves to, and neither answer is allowed to change the other.
 *
 * What is presented also depends on the learner's curriculum visibility, which changes while the
 * saved list does not. Resolved content is therefore keyed on both — the saved list *and* the
 * visibility it was resolved under — and a visibility change re-resolves the same saved list without
 * reading or writing saved state. When every saved Question is hidden the screen is
 * [SavedQuestionsUiState.Empty], even though the saved table is not.
 */
internal class SavedQuestionsViewModel(
    private val savedQuestionStateHolder: SavedQuestionStateHolder,
    private val contentResolver: SavedQuestionContentResolver,
    private val visibilityStateHolder: CurriculumVisibilityStateHolder,
) : ViewModel() {
    private val _uiState = MutableStateFlow<SavedQuestionsUiState>(SavedQuestionsUiState.Loading)
    val uiState: StateFlow<SavedQuestionsUiState> = _uiState.asStateFlow()

    private var resolution: Job? = null

    /**
     * The saved list the current content was resolved from.
     *
     * Held so a state emission that changed only [SavedQuestionsState.Loaded.pendingQuestionIds]
     * can update the actions without re-resolving and blanking the list: every Unsave tap produces
     * such an emission, and re-running resolution for each one would flash the screen back through
     * Loading. Cleared whenever resolution fails, so a retry always re-runs it.
     */
    private var resolvedFor: List<SavedQuestion>? = null

    /** The visibility [resolvedFor] was resolved under; the other half of the content's identity. */
    private var resolvedVisibility: CurriculumVisibility? = null

    /** The visibility the newest resolution was requested under, so the replayed value is ignored. */
    private var requestedVisibility: CurriculumVisibility? = null

    init {
        savedQuestionStateHolder.refresh()
        viewModelScope.launch {
            savedQuestionStateHolder.state.collect(::render)
        }
        viewModelScope.launch {
            // Re-rendering the current saved state is enough: a visibility that differs from the one
            // the content was resolved under no longer matches it, so render re-resolves.
            visibilityStateHolder.visibility.collect { visibility ->
                if (visibility != requestedVisibility) render(savedQuestionStateHolder.state.value)
            }
        }
    }

    /**
     * Re-reads saved state, and re-runs content resolution against the saved list already loaded.
     *
     * The second half is not redundant. When only the curriculum read failed, the holder's refresh
     * re-reads an identical saved list, which is an equal value that a `StateFlow` does not
     * re-emit — so nothing downstream would run again and Retry would appear to do nothing.
     */
    fun retry() {
        savedQuestionStateHolder.refresh()
        val loaded = savedQuestionStateHolder.state.value as? SavedQuestionsState.Loaded ?: return
        if (loaded.savedQuestions.isNotEmpty()) resolve(loaded)
    }

    /**
     * Removes a saved Question through the shared holder, which persists first and re-reads after.
     *
     * Guarded on the holder's own saved set so this screen's action can only ever unsave: toggling
     * an ID that is not currently saved would save it, which is not something a browsing surface
     * for already-saved Questions should be able to do. Missing content is deliberately removable —
     * a stale identity the learner cannot see must still be one they can get rid of.
     */
    fun removeSaved(questionId: String) {
        val loaded = savedQuestionStateHolder.state.value as? SavedQuestionsState.Loaded ?: return
        if (questionId !in loaded.savedQuestionIds) return
        savedQuestionStateHolder.toggleSaved(questionId)
    }

    private fun render(state: SavedQuestionsState) {
        when (state) {
            SavedQuestionsState.Loading ->
                if (_uiState.value !is SavedQuestionsUiState.Content ||
                    visibilityStateHolder.visibility.value != resolvedVisibility
                ) {
                    _uiState.value = SavedQuestionsUiState.Loading
                }

            // The holder reports Error only when it has never read saved state successfully, so
            // there is no previously known truth to keep showing.
            SavedQuestionsState.Error -> {
                resolution?.cancel()
                resolvedFor = null
                _uiState.value = SavedQuestionsUiState.Error
            }

            is SavedQuestionsState.Loaded -> when {
                state.savedQuestions.isEmpty() -> {
                    resolution?.cancel()
                    resolvedFor = emptyList()
                    _uiState.value = SavedQuestionsUiState.Empty
                }

                state.savedQuestions == resolvedFor &&
                    visibilityStateHolder.visibility.value == resolvedVisibility -> {
                    when (val current = _uiState.value) {
                        is SavedQuestionsUiState.Content ->
                            _uiState.value = current.copy(pendingQuestionIds = state.pendingQuestionIds)
                        // Settled: every saved Question resolved hidden under this visibility.
                        SavedQuestionsUiState.Empty -> Unit
                        else -> resolve(state)
                    }
                }

                else -> resolve(state)
            }
        }
    }

    /**
     * Keeps whatever content is already on screen while the new list resolves, to avoid a flash —
     * but only content resolved under the same visibility. Content from another visibility could
     * include Questions that are now hidden, so it gives way to Loading instead.
     */
    private fun resolve(state: SavedQuestionsState.Loaded) {
        resolution?.cancel()
        val visibility = visibilityStateHolder.visibility.value
        if (_uiState.value !is SavedQuestionsUiState.Content || visibility != resolvedVisibility) {
            _uiState.value = SavedQuestionsUiState.Loading
        }
        requestedVisibility = visibility
        resolution = viewModelScope.launch {
            try {
                val items = contentResolver.resolve(state.savedQuestions, visibility)
                resolvedFor = state.savedQuestions
                resolvedVisibility = visibility
                _uiState.value = if (items.isEmpty()) {
                    // Everything saved is hidden: nothing to browse, though nothing was removed.
                    SavedQuestionsUiState.Empty
                } else {
                    SavedQuestionsUiState.Content(
                        items = items,
                        pendingQuestionIds = state.pendingQuestionIds,
                    )
                }
            } catch (cancellation: CancellationException) {
                // This job was superseded — by a newer saved list, or by the list becoming empty.
                // Publishing anything here would let the replaced resolution have the last word
                // over the one that replaced it, so it publishes nothing at all.
                throw cancellation
            } catch (_: Exception) {
                // A curriculum read that failed is not evidence that the Questions are gone, so
                // this is an error with a retry rather than a list of missing placeholders.
                resolvedFor = null
                _uiState.value = SavedQuestionsUiState.Error
            }
        }
    }
}
