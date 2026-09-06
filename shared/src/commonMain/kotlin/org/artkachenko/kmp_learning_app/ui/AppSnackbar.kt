package org.artkachenko.kmp_learning_app.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The shell's snackbar host, for the transient confirmations that belong to no single screen.
 *
 * The shell owns exactly one host so a message shows in the same place whatever produced it, and
 * so it sits above the navigation bar rather than behind it. Screens and shared components reach it
 * through this local instead of each hanging a `Scaffold` of its own, the same arrangement
 * `LocalAppContentMargin` uses for the measured content margin.
 *
 * Null when there is no shell above the caller — a preview, or a test that renders one component —
 * so a caller must treat the confirmation as optional rather than assume a host exists.
 */
internal val LocalAppSnackbarHostState = staticCompositionLocalOf<SnackbarHostState?> { null }
