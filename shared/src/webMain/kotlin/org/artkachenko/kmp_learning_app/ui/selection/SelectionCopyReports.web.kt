package org.artkachenko.kmp_learning_app.ui.selection

import androidx.compose.runtime.Composable

/** Copies here are offered by the floating toolbar, so wrapping that is all it takes. */
@Composable
internal actual fun ReportSelectionCopies(
    onCopied: () -> Unit,
    content: @Composable () -> Unit,
) {
    ReportCopiesThroughTextToolbar(onCopied = onCopied, content = content)
}
