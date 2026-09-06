package org.artkachenko.kmp_learning_app.ui.selection

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.selection_copied_snackbar
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.ui.LocalAppSnackbarHostState
import org.jetbrains.compose.resources.stringResource

/**
 * Selectable content whose copies are confirmed with a snackbar.
 *
 * The affordances are the platform's own — Android's floating toolbar, the desktop context menu,
 * Ctrl/Cmd-C everywhere — because a text selection is a place where people already know what their
 * system does, and a second menu of our own next to the real one is one menu too many. The only
 * thing added is the confirmation: a copy is otherwise silent, and a learner pulling a definition
 * out of an explanation has no way to tell whether it worked short of pasting it somewhere.
 *
 * Copies are noticed rather than performed: [ReportSelectionCopies] wraps the platform's own copy
 * action, so the clipboard still receives exactly what the platform's Copy would have put there.
 * A keyboard copy bypasses those menus and so passes unannounced.
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

    ReportSelectionCopies(onCopied = onCopied) {
        SelectionContainer(modifier = modifier, content = content)
    }
}
