package org.artkachenko.kmp_learning_app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
public fun IosAppRoot() {
    val startupInitializer = remember { iosAppStartupInitializer() }
    AppRoot(startupInitializer)
}
