package org.artkachenko.kmp_learning_app.ui.selection

import androidx.compose.runtime.Composable

/**
 * Calls [onCopied] whenever text inside [content] is copied through a platform menu, and changes
 * nothing else about how those menus look or behave.
 *
 * This is an `expect` because a copy is offered through a different seam depending on the input
 * device, and neither seam covers the other:
 *
 * - Touch (Android, iOS, a touchscreen browser): the floating toolbar, reached through
 *   `LocalTextToolbar`, whose copy callback can be wrapped on its way past.
 * - Desktop: that toolbar is gated on `isInTouchMode`, which a mouse never satisfies, so nothing
 *   arrives there. The desktop context menu is reached through `LocalTextContextMenu` instead.
 *
 * An implementation must wrap [content] rather than merely observe it, because both seams are
 * composition locals that have to be provided above the `SelectionContainer` that reads them.
 */
@Composable
internal expect fun ReportSelectionCopies(
    onCopied: () -> Unit,
    content: @Composable () -> Unit,
)
