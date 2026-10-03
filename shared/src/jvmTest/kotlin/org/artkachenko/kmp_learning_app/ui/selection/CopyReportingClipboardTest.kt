package org.artkachenko.kmp_learning_app.ui.selection

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.NativeClipboard
import java.awt.datatransfer.StringSelection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlinx.coroutines.test.runTest

/**
 * The clipboard decorator on its own. On the JVM because a [ClipEntry] can only be built from a
 * platform's native entry; the decorator itself is common code.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal class CopyReportingClipboardTest {

    @Test
    fun aWriteReachesThePlatformAndIsThenReportedOnce() = runTest {
        val events = mutableListOf<String>()
        val platform = RecordingClipboard(events)
        val entry = ClipEntry(StringSelection("suspend"))

        CopyReportingClipboard(platform) { events += "reported" }.setClipEntry(entry)

        assertSame(entry, platform.written.single(), "the platform receives the entry it was given")
        // Reporting after writing, so the confirmation can never precede the clipboard write.
        assertEquals(listOf("written", "reported"), events)
    }

    @Test
    fun aFailedWriteIsNotReportedAndStillFails() = runTest {
        var reports = 0
        val platform = RecordingClipboard(failure = IllegalStateException("clipboard unavailable"))

        val thrown = assertFailsWith<IllegalStateException> {
            CopyReportingClipboard(platform) { reports++ }.setClipEntry(ClipEntry(StringSelection("x")))
        }

        assertEquals("clipboard unavailable", thrown.message)
        assertEquals(0, reports)
    }

    @Test
    fun clearingTheClipboardIsForwardedButNotReported() = runTest {
        var reports = 0
        val platform = RecordingClipboard()

        CopyReportingClipboard(platform) { reports++ }.setClipEntry(null)

        assertEquals(listOf<ClipEntry?>(null), platform.written)
        assertEquals(0, reports, "a clear removes text rather than copying any")
    }

    @Test
    fun readsAndTheNativeClipboardBelongToThePlatform() = runTest {
        var reports = 0
        val stored = ClipEntry(StringSelection("flow"))
        val platform = RecordingClipboard(stored = stored)
        val clipboard = CopyReportingClipboard(platform) { reports++ }

        assertSame(stored, clipboard.getClipEntry())
        assertSame(platform.nativeClipboard, clipboard.nativeClipboard)
        assertEquals(0, reports)
    }

    private class RecordingClipboard(
        private val events: MutableList<String> = mutableListOf(),
        private val failure: Throwable? = null,
        private val stored: ClipEntry? = null,
    ) : Clipboard {
        val written = mutableListOf<ClipEntry?>()

        override suspend fun getClipEntry(): ClipEntry? = stored

        override suspend fun setClipEntry(clipEntry: ClipEntry?) {
            failure?.let { throw it }
            written += clipEntry
            events += "written"
        }

        override val nativeClipboard: NativeClipboard = java.awt.datatransfer.Clipboard("test")
    }
}
