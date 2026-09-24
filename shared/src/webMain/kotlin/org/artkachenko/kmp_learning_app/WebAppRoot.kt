package org.artkachenko.kmp_learning_app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
public fun WebAppRoot() {
    val startupInitializer = remember { webAppStartupInitializer() }
    AppRoot(startupInitializer)
}
