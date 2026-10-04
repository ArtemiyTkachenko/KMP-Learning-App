package org.artkachenko.kmp_learning_app.diagnostics

/**
 * Where technical failures go when the learner is only shown a generic recovery state.
 *
 * Reporting is observation, not control flow: it is synchronous, returns nothing, and never
 * decides whether the caller recovers. A report carries the original [Throwable] so its type,
 * stack trace and cause chain reach whoever reads the diagnostic.
 */
internal fun interface AppDiagnostics {
    fun reportError(context: String, throwable: Throwable)
}

/**
 * Local runtime diagnostics on the host's standard output: the console on Desktop and the web,
 * logcat on Android, and the Xcode console on iOS. One write per report keeps the context line
 * and its stack trace together.
 */
internal object ConsoleAppDiagnostics : AppDiagnostics {
    override fun reportError(context: String, throwable: Throwable) {
        println(renderDiagnosticReport(context, throwable))
    }
}

internal fun renderDiagnosticReport(context: String, throwable: Throwable): String =
    "$context\n${throwable.stackTraceToString()}"
