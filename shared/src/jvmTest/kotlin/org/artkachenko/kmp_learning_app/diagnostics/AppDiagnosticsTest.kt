package org.artkachenko.kmp_learning_app.diagnostics

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

internal class AppDiagnosticsTest {
    @Test
    fun consoleReportLeadsWithTheContextAndKeepsTheStackTraceAndCause() {
        val failure = IllegalStateException("database failed", RuntimeException("disk I/O"))

        val report = renderDiagnosticReport("Application startup initialization failed.", failure)

        assertEquals("Application startup initialization failed.", report.lineSequence().first())
        assertContains(report, "IllegalStateException: database failed")
        assertContains(report, "Caused by: java.lang.RuntimeException: disk I/O")
        assertContains(report, "at org.artkachenko.kmp_learning_app.diagnostics.AppDiagnosticsTest")
    }
}
