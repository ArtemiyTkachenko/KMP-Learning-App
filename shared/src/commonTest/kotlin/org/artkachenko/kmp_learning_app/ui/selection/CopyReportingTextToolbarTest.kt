package org.artkachenko.kmp_learning_app.ui.selection

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

internal class CopyReportingTextToolbarTest {

    @Test
    fun theMenuReachesThePlatformUnchanged() {
        val platform = RecordingTextToolbar()
        val paste = {}
        val cut = {}
        val selectAll = {}
        var copies = 0

        CopyReportingTextToolbar(platform) { copies++ }.showMenu(
            rect = Rect(0f, 0f, 4f, 4f),
            onCopyRequested = {},
            onPasteRequested = paste,
            onCutRequested = cut,
            onSelectAllRequested = selectAll,
        )

        // The platform draws the menu; only the copy callback is a different object.
        assertEquals(Rect(0f, 0f, 4f, 4f), platform.rect)
        assertSame(paste, platform.onPaste)
        assertSame(cut, platform.onCut)
        assertSame(selectAll, platform.onSelectAll)
        assertEquals(0, copies, "nothing is reported until the learner picks Copy")
    }

    @Test
    fun theWrappedCopyStillCopiesAndThenReports() {
        val platform = RecordingTextToolbar()
        val order = mutableListOf<String>()

        CopyReportingTextToolbar(platform) { order += "reported" }.showMenu(
            rect = Rect.Zero,
            onCopyRequested = { order += "copied" },
        )
        assertNotNull(platform.onCopy).invoke()

        // Reporting after copying, so the confirmation can never precede the clipboard write.
        assertEquals(listOf("copied", "reported"), order)
    }

    @Test
    fun aMenuWithNoCopyIsForwardedWithNone() {
        val platform = RecordingTextToolbar()

        CopyReportingTextToolbar(platform) {}.showMenu(
            rect = Rect.Zero,
            onPasteRequested = {},
        )

        assertNull(platform.onCopy, "adding a Copy item the platform did not offer would be wrong")
    }

    @Test
    fun statusAndHideBelongToThePlatform() {
        val platform = RecordingTextToolbar()
        val toolbar = CopyReportingTextToolbar(platform) {}

        toolbar.showMenu(rect = Rect.Zero, onCopyRequested = {})
        assertEquals(TextToolbarStatus.Shown, toolbar.status)

        toolbar.hide()
        assertEquals(TextToolbarStatus.Hidden, toolbar.status)
    }

    private class RecordingTextToolbar : TextToolbar {
        var rect: Rect? = null
            private set
        var onCopy: (() -> Unit)? = null
            private set
        var onPaste: (() -> Unit)? = null
            private set
        var onCut: (() -> Unit)? = null
            private set
        var onSelectAll: (() -> Unit)? = null
            private set

        override var status: TextToolbarStatus = TextToolbarStatus.Hidden
            private set

        override fun showMenu(
            rect: Rect,
            onCopyRequested: (() -> Unit)?,
            onPasteRequested: (() -> Unit)?,
            onCutRequested: (() -> Unit)?,
            onSelectAllRequested: (() -> Unit)?,
        ) {
            this.rect = rect
            onCopy = onCopyRequested
            onPaste = onPasteRequested
            onCut = onCutRequested
            onSelectAll = onSelectAllRequested
            status = TextToolbarStatus.Shown
        }

        override fun hide() {
            status = TextToolbarStatus.Hidden
        }
    }
}
