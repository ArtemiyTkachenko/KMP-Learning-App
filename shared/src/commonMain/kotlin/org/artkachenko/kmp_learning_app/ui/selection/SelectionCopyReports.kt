package org.artkachenko.kmp_learning_app.ui.selection

import androidx.compose.runtime.Composable

/**
 * Calls [onCopied] whenever text inside [content] is copied through a platform menu, and changes
 * nothing else about how those menus look or behave.
 *
 * This is an `expect` because each platform's selection menu reaches its Copy through a different
 * seam, and with the Compose versions currently resolved only two of them pass through a seam this
 * app can wrap:
 *
 * - Desktop: the right-click menu, through `LocalTextContextMenu`. Reported, and pinned by
 *   `SelectionCopyDesktopTest`.
 * - iOS: the floating toolbar, through `LocalTextToolbar`, because Compose Multiplatform keeps
 *   `ComposeFoundationFlags.isNewContextMenuEnabled` off. Reported, by source inspection.
 * - Android: AndroidX foundation 1.11 turns that flag on, so the toolbar is an `ActionMode` built by
 *   the new context-menu provider and its Copy calls the selection's own copy directly.
 *   `LocalTextToolbar` is never asked, so nothing is reported — confirmed on an emulator.
 * - Web: `isInTouchMode` is always false, so the toolbar is never shown, and the right-click menu
 *   calls the selection's copy directly. Nothing is reported.
 *
 * The gap is recorded as `CQ-KMP-003` in `docs/quality/code-quality-audit.md`. The copy itself is
 * unaffected everywhere; only the confirmation is missing.
 *
 * An implementation must wrap [content] rather than merely observe it, because both seams are
 * composition locals that have to be provided above the `SelectionContainer` that reads them.
 */
@Composable
internal expect fun ReportSelectionCopies(
    onCopied: () -> Unit,
    content: @Composable () -> Unit,
)
