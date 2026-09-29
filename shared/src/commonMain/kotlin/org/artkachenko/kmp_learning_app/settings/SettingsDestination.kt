package org.artkachenko.kmp_learning_app.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibilityStateHolder
import org.koin.compose.koinInject

/**
 * Binds the Settings screen to the application-scoped preferences it presents: appearance and
 * curriculum visibility.
 *
 * No ViewModel, deliberately. A ViewModel here would own nothing: there is no asynchronous load, no
 * derived state, and — the point of E13-06's ownership rule — neither preference may be owned by
 * anything whose lifetime ends when Settings leaves the back stack. The theme is applied above the
 * whole shell, and curriculum visibility is read by the repositories and every screen beneath this
 * one. This resolves the two app-scoped holders, shows what they say, and sends changes back to them.
 *
 * The theme switch shows the effective theme, resolved through the same function the application
 * theme uses, so what it displays is by construction what the learner is looking at. The Kotlin
 * Multiplatform switch has no effective state of its own: it shows the stored choice as it is.
 */
@Composable
internal fun SettingsDestination(
    onBack: () -> Unit,
    holder: AppearanceStateHolder = koinInject(),
    visibilityHolder: CurriculumVisibilityStateHolder = koinInject(),
) {
    val preference by holder.preference.collectAsState()
    val includeKmpContent by visibilityHolder.includeKmpContent.collectAsState()

    SettingsScreen(
        isDarkTheme = preference.resolveDarkTheme(isSystemInDarkTheme()),
        onDarkThemeChange = holder::setDarkTheme,
        includeKmpContent = includeKmpContent,
        onIncludeKmpContentChange = visibilityHolder::setIncludeKmpContent,
        onBack = onBack,
    )
}
