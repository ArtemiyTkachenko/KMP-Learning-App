package org.artkachenko.kmp_learning_app.ui.selection

import androidx.compose.runtime.Composable

/**
 * Wraps the floating-toolbar seam, which a browser never shows because `isInTouchMode` is always
 * false there; the right-click menu copies without it, so no copy is reported here today. See the
 * common declaration.
 */
@Composable
internal actual fun ReportSelectionCopies(
    onCopied: () -> Unit,
    content: @Composable () -> Unit,
) {
    ReportCopiesThroughTextToolbar(onCopied = onCopied, content = content)
}
