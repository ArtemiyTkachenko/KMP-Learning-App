package org.artkachenko.kmp_learning_app.ui.selection

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.selection_copied_snackbar
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.ui.LocalAppSnackbarHostState
import org.jetbrains.compose.resources.stringResource

/**
 * Selectable content whose copies are confirmed with a snackbar.
 *
 * The affordances are the platform's own — Android's selection menu, the iOS floating toolbar, the
 * desktop and browser context menus, the copy shortcut — because a text selection is a place where
 * people already know what their system does, and a second menu of our own next to the real one is
 * one menu too many. The only thing added is the confirmation: a copy is otherwise silent, and a
 * learner pulling a definition out of an explanation has no way to tell whether it worked short of
 * pasting it somewhere.
 *
 * Copies are noticed rather than performed. The selection writes its text through the [LocalClipboard]
 * it finds, so it is given a [CopyReportingClipboard] around the platform's: the clipboard receives
 * exactly what the platform's Copy would have put there, and the confirmation follows a successful
 * write however that copy was asked for. A copy that never reaches this clipboard — the browser's own
 * copy shortcut, which writes to the copy event instead — passes unannounced.
 *
 * [content] gets the platform's clipboard back, so a text field inside it cuts, copies, and pastes
 * without being mistaken for a copy from the selection.
 */
@Composable
internal fun SelectableContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val snackbarHostState = LocalAppSnackbarHostState.current
    val copiedMessage = stringResource(Res.string.selection_copied_snackbar)
    val scope = rememberCoroutineScope()

    val onCopied = remember(snackbarHostState, copiedMessage, scope) {
        {
            snackbarHostState?.let { host ->
                scope.launch {
                    // Repeated copies replace the message rather than queueing behind it.
                    host.currentSnackbarData?.dismiss()
                    host.showSnackbar(copiedMessage, duration = SnackbarDuration.Short)
                }
            }
            Unit
        }
    }

    val platformClipboard = LocalClipboard.current
    // The selection keeps whichever clipboard it was composed with, so the clipboard reads the
    // current callback through a holder rather than capturing the one it was built with.
    val currentOnCopied by rememberUpdatedState(onCopied)
    val reportingClipboard = remember(platformClipboard) {
        CopyReportingClipboard(platform = platformClipboard, onCopied = { currentOnCopied() })
    }

    // SelectionContainer reads the clipboard for its copy in its own body, before composing its
    // children, so the reporting one can stop at it.
    CompositionLocalProvider(LocalClipboard provides reportingClipboard) {
        SelectionContainer(modifier = modifier) {
            CompositionLocalProvider(LocalClipboard provides platformClipboard, content = content)
        }
    }
}
