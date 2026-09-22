package org.artkachenko.kmp_learning_app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Desktop-named entry point kept so `desktopApp` keeps a host-specific root.
 *
 * Startup loading, failure, and retry state lives in [AppRoot] because Android
 * needs identical behavior; this only supplies the desktop initializer.
 */
@Composable
public fun DesktopAppRoot() {
    val startupInitializer = remember { desktopAppStartupInitializer() }
    AppRoot(startupInitializer)
}
