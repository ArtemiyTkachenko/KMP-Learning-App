package org.artkachenko.kmp_learning_app

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class PlaceholderDetailUiState(
    val itemId: String,
    val interactionCount: Int = 0,
)

internal class PlaceholderDetailViewModel(itemId: String) : ViewModel() {
    private val _uiState = MutableStateFlow(PlaceholderDetailUiState(itemId = itemId))
    val uiState: StateFlow<PlaceholderDetailUiState> = _uiState.asStateFlow()

    fun recordInteraction() {
        _uiState.update { state ->
            state.copy(interactionCount = state.interactionCount + 1)
        }
    }
}
