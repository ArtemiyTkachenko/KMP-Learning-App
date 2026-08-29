package org.artkachenko.kmp_learning_app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * The single theme entry point for every host.
 *
 * Hosts and previews call this instead of `MaterialTheme { }` so light/dark selection and the
 * product's semantic colours are decided in one place. It is safe to nest: `AppRoot` themes its
 * startup UI and `App()` themes itself for direct test and preview composition, and both resolve
 * to the same values.
 */
@Composable
internal fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val semanticColors = if (darkTheme) AppDarkSemanticColors else AppLightSemanticColors

    CompositionLocalProvider(LocalAppSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) AppDarkColorScheme else AppLightColorScheme,
            content = content,
        )
    }
}

/** Mirrors `MaterialTheme.colorScheme` for the roles Material 3 does not define. */
internal object AppThemeExtras {
    val semanticColors: AppSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppSemanticColors.current
}
