package org.artkachenko.kmp_learning_app.ui.selection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.platform.asAwtTransferable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import java.awt.datatransfer.DataFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.AppNavigationScaffold
import org.artkachenko.kmp_learning_app.AppTopLevelDestination
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme

/**
 * Selecting and copying on desktop, driven with a real mouse.
 *
 * The desktop path is worth testing on its own because it shares no seam with the touch platforms:
 * `SelectionManager` shows its floating toolbar only in touch mode, so everything a mouse does here
 * goes through `LocalTextContextMenu` instead.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
internal class SelectionCopyDesktopTest {

    @Test
    fun copyingFromThePlatformMenuIsConfirmedWithASnackbar() = runComposeUiTest {
        val clipboard = RecordingClipboard()
        setContent { Shell(clipboard) }

        onNodeWithTag(TextTag).performMouseInput {
            moveTo(Offset(1f, centerY))
            press()
            // Several steps rather than one jump: a selection is extended by drag events, and one
            // move would leave the manager with a single sample of the gesture.
            moveTo(Offset(width / 2f, centerY))
            moveTo(Offset(width - 1f, centerY))
            release()
        }
        onNodeWithTag(TextTag).performMouseInput { rightClick(Offset(width / 2f, centerY)) }

        // The platform's own menu item, not one this app draws.
        onNodeWithText(PlatformCopyLabel).performClick()

        waitUntil(timeoutMillis = TimeoutMillis) { clipboard.copiedText != null }
        val copied = clipboard.copiedText
        assertTrue(
            copied != null && copied.isNotEmpty() && SelectableText.contains(copied),
            "expected part of the rendered text on the clipboard, was $copied",
        )
        onNodeWithText("Copied to clipboard").assertIsDisplayed()
    }

    @Test
    fun aSelectionOnItsOwnAddsNoPromptAndCopiesNothing() = runComposeUiTest {
        val clipboard = RecordingClipboard()
        setContent { Shell(clipboard) }

        onNodeWithTag(TextTag).performMouseInput {
            moveTo(Offset(1f, centerY))
            press()
            moveTo(Offset(width / 2f, centerY))
            moveTo(Offset(width - 1f, centerY))
            release()
        }
        mainClock.advanceTimeBy(TimeoutMillis)

        // Finishing a selection is not a request to do anything with it: no menu of the app's own
        // appears beside the platform's, and the clipboard is left alone until a copy is asked for.
        onNodeWithText(PlatformCopyLabel).assertDoesNotExist()
        assertEquals(null, clipboard.copiedText)
    }

    @androidx.compose.runtime.Composable
    private fun Shell(clipboard: Clipboard) {
        AppTheme {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                // Through the real shell, because the snackbar host lives on its Scaffold and is
                // reached through the composition local the shell provides.
                Box(Modifier.size(400.dp, 800.dp)) {
                    AppNavigationScaffold(
                        selected = AppTopLevelDestination.TOPICS,
                        onSelect = {},
                        showsNavigation = false,
                    ) {
                        SelectableContent {
                            Text(text = SelectableText, modifier = Modifier.testTag(TextTag))
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val TextTag = "selectable-text"
        const val SelectableText = "Coroutines suspend without blocking a thread"

        /** What Compose's own desktop context menu calls its copy item. */
        const val PlatformCopyLabel = "Copy"
        const val TimeoutMillis = 2_000L
    }

    /**
     * A clipboard that records instead of reaching the desktop session's own.
     *
     * `SelectionContainer` only installs its copy handler when the clipboard reports that writing
     * is supported, which on desktop is unconditionally true, so nothing more than these three
     * members is needed.
     */
    private class RecordingClipboard : Clipboard {
        private var entry: ClipEntry? = null

        val copiedText: String?
            get() = entry?.asAwtTransferable
                ?.takeIf { it.isDataFlavorSupported(DataFlavor.stringFlavor) }
                ?.getTransferData(DataFlavor.stringFlavor) as? String

        override suspend fun getClipEntry(): ClipEntry? = entry

        override suspend fun setClipEntry(clipEntry: ClipEntry?) {
            entry = clipEntry
        }

        override val nativeClipboard: NativeClipboard
            get() = java.awt.datatransfer.Clipboard("test")
    }
}
