package org.artkachenko.kmp_learning_app.saved_questions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.artkachenko.kmp_learning_app.saved_questions.repository.SavedQuestionRepository

/**
 * App-scoped saved-Question state, shared by every review surface.
 *
 * Focused results, Mixed results, and Mistake Review all present the same learner-owned saved
 * identities, so saving a Question on one surface has to be what the next surface shows. Deriving
 * that separately in three ViewModels would mean three caches of one truth; holding it here means
 * one, outliving any screen the navigation entry destroys.
 *
 * [repository] remains the persistent source of truth. Nothing is stored here that the database does
 * not already hold: this is the in-memory projection the UI observes, refreshed from the repository
 * after every mutation rather than assembled independently.
 */
internal class SavedQuestionStateHolder(
    private val repository: SavedQuestionRepository,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<SavedQuestionsState>(SavedQuestionsState.Loading)
    val state: StateFlow<SavedQuestionsState> = _state.asStateFlow()

    /** Held for the duration of a read so concurrent entries share one query, not one each. */
    private val reading = Mutex()

    /**
     * Reads saved state, unless a read is already running.
     *
     * Called when a review surface opens rather than once at startup: the read is a single indexed
     * lookup, and repeating it is what lets a surface recover from an earlier failed read instead of
     * leaving the save action unavailable for the rest of the session. Result content never waits on
     * it — the two states load independently.
     */
    fun refresh() {
        if (!reading.tryLock()) return
        scope.launch {
            try {
                runCatching { repository.getSavedQuestions() }.fold(
                    onSuccess = { saved ->
                        _state.update { current ->
                            when (current) {
                                // A mutation in flight keeps its pending marker across the refresh.
                                is SavedQuestionsState.Loaded -> current.copy(savedQuestions = saved)
                                else -> SavedQuestionsState.Loaded(saved)
                            }
                        }
                    },
                    // A failed re-read leaves an earlier successful one in place: previously known
                    // saved state is better than reporting that nothing is known.
                    onFailure = {
                        _state.update { current ->
                            if (current is SavedQuestionsState.Loaded) {
                                current
                            } else {
                                SavedQuestionsState.Error
                            }
                        }
                    },
                )
            } finally {
                reading.unlock()
            }
        }
    }

    /**
     * Saves an unsaved Question, or unsaves a saved one.
     *
     * Ignored while saved state is unknown, and while this Question already has a mutation in
     * flight, so repeated taps cannot launch competing toggles. The database's primary key remains
     * the final guarantee that a repeated save cannot create a second row; this only keeps what the
     * learner sees predictable.
     *
     * The visible state changes only after the write succeeds, and is then read back from the
     * repository, so a card never claims a saved state that was not persisted.
     */
    fun toggleSaved(questionId: String) {
        val loaded = _state.value as? SavedQuestionsState.Loaded ?: return
        if (questionId in loaded.pendingQuestionIds) return
        val save = questionId !in loaded.savedQuestionIds
        _state.update { current ->
            if (current is SavedQuestionsState.Loaded) {
                current.copy(pendingQuestionIds = current.pendingQuestionIds + questionId)
            } else {
                current
            }
        }
        scope.launch {
            runCatching {
                if (save) repository.save(questionId) else repository.unsave(questionId)
                repository.getSavedQuestions()
            }.fold(
                onSuccess = { saved -> settle(questionId) { it.copy(savedQuestions = saved) } },
                // The mutation failed, or the read after it did. Either way the last state read from
                // the repository stands rather than a guess at what the write would have made true.
                onFailure = { settle(questionId) { it } },
            )
        }
    }

    private fun settle(
        questionId: String,
        transform: (SavedQuestionsState.Loaded) -> SavedQuestionsState.Loaded,
    ) {
        _state.update { current ->
            if (current is SavedQuestionsState.Loaded) {
                transform(current).let { updated ->
                    updated.copy(pendingQuestionIds = updated.pendingQuestionIds - questionId)
                }
            } else {
                current
            }
        }
    }
}
