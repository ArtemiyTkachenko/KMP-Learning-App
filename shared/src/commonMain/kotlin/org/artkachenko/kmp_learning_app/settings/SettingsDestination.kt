package org.artkachenko.kmp_learning_app.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.settings_reset_done
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibilityStateHolder
import org.artkachenko.kmp_learning_app.ui.LocalAppSnackbarHostState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Binds the Settings screen to the application-scoped preferences it presents — appearance and
 * curriculum visibility — and to the one piece of state it owns, the progress reset.
 *
 * The preferences take no ViewModel, deliberately: there is no asynchronous load and no derived
 * state, and — the point of E13-06's ownership rule — neither preference may be owned by anything
 * whose lifetime ends when Settings leaves the back stack. The theme is applied above the whole
 * shell, and curriculum visibility is read by the repositories and every screen beneath this one.
 * This resolves the two app-scoped holders, shows what they say, and sends changes back to them.
 * The reset is different — a confirmation, a running delete and an outcome — so it alone has
 * [ProgressResetViewModel].
 *
 * The theme switch shows the effective theme, resolved through the same function the application
 * theme uses, so what it displays is by construction what the learner is looking at. The Kotlin
 * Multiplatform switch has no effective state of its own: it shows the stored choice as it is.
 *
 * [onProgressReset] runs once a reset has deleted the data, before the confirmation shows: every
 * attempt route on any back stack now names a deleted attempt, so the shell resets navigation.
 */
@Composable
internal fun SettingsDestination(
    onBack: () -> Unit,
    onProgressReset: () -> Unit,
    holder: AppearanceStateHolder = koinInject(),
    visibilityHolder: CurriculumVisibilityStateHolder = koinInject(),
    progressResetViewModel: ProgressResetViewModel = koinViewModel(),
) {
    val preference by holder.preference.collectAsState()
    val includeKmpContent by visibilityHolder.includeKmpContent.collectAsState()
    val progressReset by progressResetViewModel.state.collectAsStateWithLifecycle()
    val currentOnProgressReset by rememberUpdatedState(onProgressReset)
    val snackbarHostState = LocalAppSnackbarHostState.current
    val resetDone = stringResource(Res.string.settings_reset_done)

    LaunchedEffect(progressResetViewModel) {
        progressResetViewModel.events.collect {
            currentOnProgressReset()
            // Optional, as for every user of the shell's host: there is none in a preview or test.
            snackbarHostState?.showSnackbar(resetDone, duration = SnackbarDuration.Short)
        }
    }

    SettingsScreen(
        isDarkTheme = preference.resolveDarkTheme(isSystemInDarkTheme()),
        onDarkThemeChange = holder::setDarkTheme,
        includeKmpContent = includeKmpContent,
        onIncludeKmpContentChange = visibilityHolder::setIncludeKmpContent,
        progressReset = progressReset,
        onResetProgress = progressResetViewModel::requestReset,
        onConfirmReset = progressResetViewModel::confirmReset,
        onDismissReset = progressResetViewModel::dismiss,
        onBack = onBack,
    )
}
