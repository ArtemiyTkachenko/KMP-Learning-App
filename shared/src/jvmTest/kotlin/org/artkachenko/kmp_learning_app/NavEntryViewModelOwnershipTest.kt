package org.artkachenko.kmp_learning_app

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

/**
 * The ViewModel lifetime every screen relies on: one store per live back-stack entry.
 *
 * `App` installs the same two decorators, in the same order, and every destination resolves its
 * ViewModel against the store they give its entry. The journey tests fail when that decorator is
 * missing, but only as content that never appears; this pins the contract itself. Real [AppRoute]
 * values are the keys, so the content key is derived exactly as it is for the running app.
 *
 * Deliberately not pinned: two *equal* routes on one stack share a store (`CQ-DI-003`). That is
 * Navigation 3's content-key rule, and the navigation graph currently cannot produce it.
 */
@OptIn(ExperimentalTestApi::class)
internal class NavEntryViewModelOwnershipTest {

    @Test
    fun eachLiveEntryOwnsItsViewModelAndARemovedEntryClearsOnlyItsOwn() = runComposeUiTest {
        val backStack = mutableStateListOf<NavKey>(AppRoute.ProgressTopic("first"))
        val resolved = mutableMapOf<String, MutableList<EntryViewModel>>()
        val cleared = mutableListOf<String>()

        setContent {
            NavDisplay(
                backStack = backStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                onBack = { backStack.removeAt(backStack.lastIndex) },
                entryProvider = entryProvider {
                    entry<AppRoute.ProgressTopic> { route ->
                        val viewModel = viewModel { EntryViewModel(route.topicId, cleared) }
                        resolved.getOrPut(route.topicId) { mutableListOf() } += viewModel
                        BasicText("showing ${viewModel.topicId}")
                    }
                },
            )
        }
        waitForIdle()
        val first = resolved.getValue("first").last()

        // A second entry of the same destination, with the first still on the stack beneath it.
        backStack.add(AppRoute.ProgressTopic("second"))
        waitForIdle()
        onNodeWithText("showing second").assertIsDisplayed()
        val second = resolved.getValue("second").last()
        assertNotSame(first, second)
        assertEquals("second", second.topicId)
        assertTrue(cleared.isEmpty(), "Covering an entry must not clear its ViewModel: $cleared")

        // Back to the first: the same instance it had, and only the removed entry is cleared.
        backStack.removeAt(backStack.lastIndex)
        waitForIdle()
        onNodeWithText("showing first").assertIsDisplayed()
        assertTrue(resolved.getValue("first").all { it === first })
        assertEquals(listOf("second"), cleared)
    }
}

private class EntryViewModel(
    val topicId: String,
    private val cleared: MutableList<String>,
) : ViewModel() {
    override fun onCleared() {
        cleared += topicId
    }
}
