package org.artkachenko.kmp_learning_app.ui.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

/**
 * The touch implementation of [ReportSelectionCopies], shared by every platform whose selections
 * are made with a finger.
 */
@Composable
internal fun ReportCopiesThroughTextToolbar(
    onCopied: () -> Unit,
    content: @Composable () -> Unit,
) {
    val platformToolbar = LocalTextToolbar.current
    // The toolbar outlives individual callback instances, so it reads the current one through a
    // holder rather than capturing whichever it was built with.
    val currentOnCopied by rememberUpdatedState(onCopied)
    val toolbar = remember(platformToolbar) {
        CopyReportingTextToolbar(platform = platformToolbar, onCopied = { currentOnCopied() })
    }

    CompositionLocalProvider(LocalTextToolbar provides toolbar, content = content)
}

/**
 * A [TextToolbar] that is the platform's own in every respect except that it says when its copy
 * action ran.
 *
 * Every call is forwarded untouched, including the menu an editable text field asks for, so the
 * toolbar the learner sees is the one the platform would have shown. Only the copy callback is
 * wrapped, and it still does the platform's copying before reporting.
 */
internal class CopyReportingTextToolbar(
    private val platform: TextToolbar,
    private val onCopied: () -> Unit,
) : TextToolbar {

    override val status: TextToolbarStatus
        get() = platform.status

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        platform.showMenu(
            rect = rect,
            onCopyRequested = reporting(onCopyRequested),
            onPasteRequested = onPasteRequested,
            onCutRequested = onCutRequested,
            onSelectAllRequested = onSelectAllRequested,
        )
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
        onAutofillRequested: (() -> Unit)?,
    ) {
        platform.showMenu(
            rect = rect,
            onCopyRequested = reporting(onCopyRequested),
            onPasteRequested = onPasteRequested,
            onCutRequested = onCutRequested,
            onSelectAllRequested = onSelectAllRequested,
            onAutofillRequested = onAutofillRequested,
        )
    }

    override fun hide() {
        platform.hide()
    }

    private fun reporting(copy: (() -> Unit)?): (() -> Unit)? =
        if (copy == null) null else { { copy(); onCopied() } }
}
