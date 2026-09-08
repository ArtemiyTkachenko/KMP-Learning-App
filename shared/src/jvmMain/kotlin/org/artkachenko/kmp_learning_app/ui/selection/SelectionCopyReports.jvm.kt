package org.artkachenko.kmp_learning_app.ui.selection

import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.LocalTextContextMenu
import androidx.compose.foundation.text.TextContextMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString

/**
 * The desktop implementation of [ReportSelectionCopies].
 *
 * The floating toolbar the touch platforms use is never shown here: `SelectionManager` gates it on
 * `isInTouchMode`, which is `!event.isMouseOrTouchPad()`, so a mouse selection produces no toolbar
 * call at all. What desktop offers instead is `LocalTextContextMenu`, through which both
 * `SelectionContainer` and every text field route their right-click menu, handing over a
 * [TextContextMenu.TextManager] that owns the copy action.
 *
 * Rendering stays with [TextContextMenu.Default], so the menu is the one the platform would have
 * drawn; only the manager passing through it is wrapped.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal actual fun ReportSelectionCopies(
    onCopied: () -> Unit,
    content: @Composable () -> Unit,
) {
    val currentOnCopied by rememberUpdatedState(onCopied)
    val contextMenu = remember { CopyReportingTextContextMenu { currentOnCopied() } }

    CompositionLocalProvider(LocalTextContextMenu provides contextMenu, content = content)
}

@OptIn(ExperimentalFoundationApi::class)
private class CopyReportingTextContextMenu(
    private val onCopied: () -> Unit,
) : TextContextMenu {

    @Composable
    override fun Area(
        textManager: TextContextMenu.TextManager,
        state: ContextMenuState,
        content: @Composable () -> Unit,
    ) {
        val reporting = remember(textManager) { CopyReportingTextManager(textManager, onCopied) }
        TextContextMenu.Default.Area(reporting, state, content)
    }
}

/**
 * The platform's text manager with one action wrapped.
 *
 * Every member is a `get()` rather than a stored value because the originals are: they are
 * recomputed from the live selection each time the menu is built, and holding on to one would
 * freeze a menu item in whatever state it had when the area was first composed.
 */
@OptIn(ExperimentalFoundationApi::class)
private class CopyReportingTextManager(
    private val delegate: TextContextMenu.TextManager,
    private val onCopied: () -> Unit,
) : TextContextMenu.TextManager {

    override val selectedText: AnnotatedString
        get() = delegate.selectedText

    override val cut: TextContextMenu.Action?
        get() = delegate.cut

    override val copy: TextContextMenu.Action?
        get() = delegate.copy?.let { action ->
            TextContextMenu.Action(
                enabled = action.enabled,
                execute = {
                    action.execute()
                    onCopied()
                },
            )
        }

    override val paste: TextContextMenu.Action?
        get() = delegate.paste

    override val selectAll: TextContextMenu.Action?
        get() = delegate.selectAll

    override fun selectWordAtPositionIfNotAlreadySelected(offset: Offset) {
        delegate.selectWordAtPositionIfNotAlreadySelected(offset)
    }
}
