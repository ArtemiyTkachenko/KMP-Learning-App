package org.artkachenko.kmp_learning_app

import kotlin.test.Test
import kotlin.test.assertEquals

internal class PlaceholderDetailViewModelTest {
    @Test
    fun initialStateContainsRouteArgument() {
        val viewModel = PlaceholderDetailViewModel(itemId = "placeholder-123")

        assertEquals(
            PlaceholderDetailUiState(itemId = "placeholder-123"),
            viewModel.uiState.value,
        )
    }

    @Test
    fun recordInteractionIncrementsInteractionCount() {
        val viewModel = PlaceholderDetailViewModel(itemId = "placeholder-123")

        viewModel.recordInteraction()
        viewModel.recordInteraction()

        assertEquals(
            PlaceholderDetailUiState(
                itemId = "placeholder-123",
                interactionCount = 2,
            ),
            viewModel.uiState.value,
        )
    }
}
