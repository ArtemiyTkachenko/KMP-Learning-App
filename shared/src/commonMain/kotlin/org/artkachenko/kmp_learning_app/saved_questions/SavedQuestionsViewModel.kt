package org.artkachenko.kmp_learning_app.saved_questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
 */
internal class SavedQuestionsViewModel(
    private val savedQuestionStateHolder: SavedQuestionStateHolder,
    private val contentResolver: SavedQuestionContentResolver,
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

    init {
        savedQuestionStateHolder.refresh()
        viewModelScope.launch {
            savedQuestionStateHolder.state.collect(::render)
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
                if (_uiState.value !is SavedQuestionsUiState.Content) {
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

                state.savedQuestions == resolvedFor -> {
                    val current = _uiState.value
                    if (current is SavedQuestionsUiState.Content) {
                        _uiState.value = current.copy(pendingQuestionIds = state.pendingQuestionIds)
                    } else {
                        resolve(state)
                    }
                }

                else -> resolve(state)
            }
        }
    }

    /** Keeps whatever content is already on screen while the new list resolves, to avoid a flash. */
    private fun resolve(state: SavedQuestionsState.Loaded) {
        resolution?.cancel()
        if (_uiState.value !is SavedQuestionsUiState.Content) {
            _uiState.value = SavedQuestionsUiState.Loading
        }
        resolution = viewModelScope.launch {
            runCatching { contentResolver.resolve(state.savedQuestions) }.fold(
                onSuccess = { items ->
                    resolvedFor = state.savedQuestions
                    _uiState.value = SavedQuestionsUiState.Content(
                        items = items,
                        pendingQuestionIds = state.pendingQuestionIds,
                    )
                },
                // A curriculum read that failed is not evidence that the Questions are gone, so
                // this is an error with a retry rather than a list of missing placeholders.
                onFailure = {
                    resolvedFor = null
                    _uiState.value = SavedQuestionsUiState.Error
                },
            )
        }
    }
}
