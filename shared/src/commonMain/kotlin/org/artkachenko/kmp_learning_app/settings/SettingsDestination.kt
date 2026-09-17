package org.artkachenko.kmp_learning_app.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.koinInject

/**
 * Binds the Settings screen to the application-scoped appearance preference.
 *
 * No ViewModel, deliberately. A ViewModel here would own nothing: there is no asynchronous load, no
 * derived state, and — the point of E13-06's ownership rule — the preference must *not* be owned by
 * anything whose lifetime ends when Settings leaves the back stack. This resolves the app-scoped
 * holder, shows what it says, and sends changes back to it.
 *
 * The switch shows the effective theme, resolved through the same function the application theme
 * uses, so what it displays is by construction what the learner is looking at.
 */
@Composable
internal fun SettingsDestination(
    onBack: () -> Unit,
    holder: AppearanceStateHolder = koinInject(),
) {
    val preference by holder.preference.collectAsState()

    SettingsScreen(
        isDarkTheme = preference.resolveDarkTheme(isSystemInDarkTheme()),
        onDarkThemeChange = holder::setDarkTheme,
        onBack = onBack,
    )
}
