package org.artkachenko.kmp_learning_app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
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
 *
 * All four Material subsystems are supplied explicitly. Previously only `colorScheme` was, which
 * meant the type scale, the shape scale, and every component's motion were Material's defaults —
 * so screens patched weight at the call site, every card carried the same generic 12.dp corner,
 * and nothing in the app moved with intent.
 *
 * [MotionScheme.expressive] is the one line here with app-wide reach: it re-specs the motion of
 * every Material component — the navigation bar indicator, buttons, chips, switches, progress
 * indicators — onto spring physics rather than fixed-duration tweens. The app's own animations
 * read their values from [AppMotion], which states the same spring constants.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val semanticColors = if (darkTheme) AppDarkSemanticColors else AppLightSemanticColors

    CompositionLocalProvider(LocalAppSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) AppDarkColorScheme else AppLightColorScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = AppShapes,
            typography = AppTypography,
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
