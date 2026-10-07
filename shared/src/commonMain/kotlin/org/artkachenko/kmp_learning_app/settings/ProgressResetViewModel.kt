package org.artkachenko.kmp_learning_app.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Where the Settings screen's reset confirmation is. */
internal sealed interface ProgressResetUiState {
    /** No dialog. */
    data object Idle : ProgressResetUiState

    /** The confirmation is open. [failed] when the previous confirm did not delete anything. */
    data class Confirming(val failed: Boolean = false) : ProgressResetUiState

    /** The delete is running; the dialog stays open and cannot be dismissed. */
    data object Resetting : ProgressResetUiState
}

/** A reset that completed. One-shot: it navigates and confirms, and must not replay. */
internal data object ProgressResetCompleted

/**
 * The one piece of Settings that owns asynchronous state: confirming and running a progress reset.
 *
 * The two preferences on the screen still come straight from their app-scoped holders; this holds
 * only the dialog, whether the reset is running, and its outcome. A failure keeps the dialog open
 * with an error so Reset is the retry, and the data is untouched because the delete is one
 * transaction. Success closes the dialog and emits [ProgressResetCompleted] once, for the
 * destination to reset navigation and show the confirmation.
 */
internal class ProgressResetViewModel(
    private val resetLearnerProgress: suspend () -> Unit,
) : ViewModel() {
    private val _state = MutableStateFlow<ProgressResetUiState>(ProgressResetUiState.Idle)
    val state: StateFlow<ProgressResetUiState> = _state.asStateFlow()

    private val _events = Channel<ProgressResetCompleted>(Channel.BUFFERED)
    val events: Flow<ProgressResetCompleted> = _events.receiveAsFlow()

    fun requestReset() {
        if (_state.value == ProgressResetUiState.Idle) _state.value = ProgressResetUiState.Confirming()
    }

    /** Cancel, or a dismissal outside the dialog. Ignored while the reset is running. */
    fun dismiss() {
        if (_state.value is ProgressResetUiState.Confirming) _state.value = ProgressResetUiState.Idle
    }

    fun confirmReset() {
        if (_state.value !is ProgressResetUiState.Confirming) return
        _state.value = ProgressResetUiState.Resetting
        viewModelScope.launch {
            try {
                resetLearnerProgress()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _state.value = ProgressResetUiState.Confirming(failed = true)
                return@launch
            }
            _state.value = ProgressResetUiState.Idle
            _events.send(ProgressResetCompleted)
        }
    }
}
