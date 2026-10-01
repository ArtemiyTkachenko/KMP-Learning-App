package org.artkachenko.kmp_learning_app.ui.selection

import androidx.compose.runtime.Composable

/**
 * Wraps the floating-toolbar seam, which AndroidX foundation 1.11's new context menu no longer
 * calls for a selection, so no copy is reported here today. See the common declaration.
 */
@Composable
internal actual fun ReportSelectionCopies(
    onCopied: () -> Unit,
    content: @Composable () -> Unit,
) {
    ReportCopiesThroughTextToolbar(onCopied = onCopied, content = content)
}
