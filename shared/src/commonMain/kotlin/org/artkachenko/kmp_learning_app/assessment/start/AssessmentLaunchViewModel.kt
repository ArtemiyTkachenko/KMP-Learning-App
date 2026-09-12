package org.artkachenko.kmp_learning_app.assessment.start

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
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig

internal class AssessmentLaunchViewModel(
    private val startAssessment: StartAssessment,
) : ViewModel() {
    private val _state = MutableStateFlow<AssessmentLaunchState>(AssessmentLaunchState.Idle)
    val state: StateFlow<AssessmentLaunchState> = _state.asStateFlow()

    private val _events = Channel<AssessmentLaunchEvent>(Channel.BUFFERED)
    val events: Flow<AssessmentLaunchEvent> = _events.receiveAsFlow()

    fun start(config: AssessmentConfig) {
        if (_state.value !is AssessmentLaunchState.Idle) return
        launch(config)
    }

    fun retry() {
        val failed = _state.value as? AssessmentLaunchState.Failed ?: return
        launch(failed.config)
    }

    fun dismissFailure() {
        if (_state.value is AssessmentLaunchState.Failed) {
            _state.value = AssessmentLaunchState.Idle
        }
    }

    private fun launch(config: AssessmentConfig) {
        _state.value = AssessmentLaunchState.Launching
        viewModelScope.launch {
            try {
                when (val result = startAssessment(config)) {
                    is StartAssessmentResult.Created -> {
                        _state.value = AssessmentLaunchState.Idle
                        _events.send(AssessmentLaunchEvent.Created(result.attemptId))
                    }
                    StartAssessmentResult.NoEligibleQuestions -> {
                        _state.value = AssessmentLaunchState.Failed(
                            config = config,
                            reason = AssessmentLaunchFailure.NoEligibleQuestions,
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (@Suppress("TooGenericExceptionCaught") failure: Throwable) {
                _state.value = AssessmentLaunchState.Failed(
                    config = config,
                    reason = AssessmentLaunchFailure.Unexpected,
                )
            }
        }
    }
}

internal sealed interface AssessmentLaunchState {
    data object Idle : AssessmentLaunchState
    data object Launching : AssessmentLaunchState
    data class Failed(
        val config: AssessmentConfig,
        val reason: AssessmentLaunchFailure,
    ) : AssessmentLaunchState
}

internal enum class AssessmentLaunchFailure {
    NoEligibleQuestions,
    Unexpected,
}

internal sealed interface AssessmentLaunchEvent {
    data class Created(
        val attemptId: String,
    ) : AssessmentLaunchEvent
}
