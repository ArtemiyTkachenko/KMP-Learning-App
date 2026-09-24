package org.artkachenko.kmp_learning_app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import org.artkachenko.kmp_learning_app.settings.AppearanceStateHolder
import org.artkachenko.kmp_learning_app.settings.ThemePreference
import org.artkachenko.kmp_learning_app.settings.resolveDarkTheme
import org.koin.mp.KoinPlatform

/**
 * [AppTheme] under the learner's saved appearance: the application's single theme decision.
 *
 * `AppRoot` and `App()` both call this, which is what makes them one decision rather than two.
 * Before the preference existed each independently defaulted to `isSystemInDarkTheme()`, so they
 * agreed by coincidence; had only one been taught about the preference, the startup screens and the
 * app itself would have disagreed. Calling this twice is harmless — the inner call resolves the
 * same holder to the same value, and `AppTheme` is documented as safe to nest.
 *
 * The holder is resolved from the running application graph, and its absence is a supported case:
 * a preview or a unit test that composes a screen directly has no graph, and then the system value
 * stands exactly as it did before. That is why this is a lookup that may fail rather than
 * `koinInject()`, which would throw.
 *
 * The holder is a lazy Koin `single`, so the first call here is what constructs it and performs
 * its one synchronous storage read, inside the `remember` below. That read completes within this
 * composition, which is why the theme is settled before the first frame and no startup step waits
 * on storage. Every later call, and every recomposition, only observes the value.
 */
@Composable
internal fun AppearanceTheme(content: @Composable () -> Unit) {
    // Remembered, so which branch below runs is fixed for the life of this composition.
    val holder = remember { KoinPlatform.getKoinOrNull()?.getOrNull<AppearanceStateHolder>() }
    val preference = if (holder == null) {
        ThemePreference.System
    } else {
        holder.preference.collectAsState().value
    }

    AppTheme(
        darkTheme = preference.resolveDarkTheme(isSystemInDarkTheme()),
        content = content,
    )
}
