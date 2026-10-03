package org.artkachenko.kmp_learning_app.ui.selection

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.NativeClipboard

/**
 * A [Clipboard] that is the platform's own in every respect except that it says when something
 * was written to it.
 *
 * Reads and the native clipboard are forwarded untouched, and a write is the platform's write: the
 * entry is handed over exactly as received, and [onCopied] runs only once that write has returned.
 * A write that throws is never reported, and neither is clearing the clipboard with `null`, which
 * removes text rather than copying any.
 */
internal class CopyReportingClipboard(
    private val platform: Clipboard,
    private val onCopied: () -> Unit,
) : Clipboard {

    override suspend fun getClipEntry(): ClipEntry? = platform.getClipEntry()

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        platform.setClipEntry(clipEntry)
        if (clipEntry != null) onCopied()
    }

    // Experimental on the web targets only; forwarding it adds no dependence of our own on its shape.
    @OptIn(ExperimentalComposeUiApi::class)
    override val nativeClipboard: NativeClipboard
        get() = platform.nativeClipboard
}
