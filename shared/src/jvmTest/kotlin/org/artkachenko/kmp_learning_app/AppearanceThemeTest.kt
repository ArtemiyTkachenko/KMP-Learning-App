package org.artkachenko.kmp_learning_app

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.settings.AppPreferenceStorage
import org.artkachenko.kmp_learning_app.settings.AppearanceStateHolder
import org.artkachenko.kmp_learning_app.settings.ThemePreference
import org.artkachenko.kmp_learning_app.settings.ThemePreferenceStore
import org.artkachenko.kmp_learning_app.ui.theme.AppDarkColorScheme
import org.artkachenko.kmp_learning_app.ui.theme.AppLightColorScheme
import org.artkachenko.kmp_learning_app.ui.theme.AppearanceTheme
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * The application's one theme decision, composed.
 *
 * `AppearancePreferenceTest` proves the rules about the preference; this proves the wiring: that
 * `AppRoot` and `App()` resolve the *same* decision, that an explicit choice reaches the whole
 * composition immediately rather than on the next restart, and that a composition with no
 * application graph — a preview, an isolated screen test — still themes correctly.
 *
 * Nothing here asserts what the system theme is. That would make the result depend on the machine
 * running it; the system path is covered deterministically by the pure resolver test.
 */
@OptIn(ExperimentalTestApi::class)
internal class AppearanceThemeTest {

    @Test
    fun aStoredDarkChoiceThemesTheApplicationDark() {
        withAppearanceGraph(ThemePreference.Dark) {
            runComposeUiTest {
                var scheme: ColorScheme? = null
                setContent { AppearanceTheme { scheme = MaterialTheme.colorScheme } }
                waitForIdle()

                assertSame(AppDarkColorScheme, assertNotNull(scheme))
            }
        }
    }

    @Test
    fun aStoredLightChoiceThemesTheApplicationLight() {
        withAppearanceGraph(ThemePreference.Light) {
            runComposeUiTest {
                var scheme: ColorScheme? = null
                setContent { AppearanceTheme { scheme = MaterialTheme.colorScheme } }
                waitForIdle()

                assertSame(AppLightColorScheme, assertNotNull(scheme))
            }
        }
    }

    /**
     * The immediacy requirement: changing the preference recolours the running composition. The
     * holder is the same instance Settings sends changes to, so this is the Settings switch's
     * effect on the whole application, without going through the screen.
     */
    @Test
    fun anExplicitChoiceRecoloursTheRunningApplication() {
        withAppearanceGraph(ThemePreference.Light) { holder ->
            runComposeUiTest {
                var scheme: ColorScheme? = null
                setContent { AppearanceTheme { scheme = MaterialTheme.colorScheme } }
                waitForIdle()
                assertSame(AppLightColorScheme, assertNotNull(scheme))

                holder.setDarkTheme(true)
                waitForIdle()
                assertSame(AppDarkColorScheme, assertNotNull(scheme))

                holder.setDarkTheme(false)
                waitForIdle()
                assertSame(AppLightColorScheme, assertNotNull(scheme))
            }
        }
    }

    /**
     * `AppRoot` themes its startup UI and `App()` themes itself again for direct composition, so
     * the two are nested in the running app. Both must land on the same scheme — the defect this
     * guards against is one of them following the preference while the other independently reads
     * the system, which is what the code did before E13-06.
     */
    @Test
    fun theOuterAndInnerThemeDecisionsAgree() {
        withAppearanceGraph(ThemePreference.Dark) {
            runComposeUiTest {
                var outer: ColorScheme? = null
                var inner: ColorScheme? = null
                setContent {
                    AppearanceTheme {
                        outer = MaterialTheme.colorScheme
                        AppearanceTheme { inner = MaterialTheme.colorScheme }
                    }
                }
                waitForIdle()

                assertSame(AppDarkColorScheme, assertNotNull(outer))
                assertSame(assertNotNull(outer), assertNotNull(inner))
            }
        }
    }

    /**
     * With no application graph the theme still resolves, which is what keeps previews and
     * isolated screen tests working: the preference is unknown, so the system value stands exactly
     * as it did before the preference existed.
     */
    @Test
    fun aCompositionWithoutAnApplicationGraphStillThemes() {
        synchronized(appIntegrationMainDispatcherLock) {
            stopKoin()
            runComposeUiTest {
                var scheme: ColorScheme? = null
                setContent { AppearanceTheme { scheme = MaterialTheme.colorScheme } }
                waitForIdle()

                val resolved = assertNotNull(scheme)
                assertTrue(
                    resolved === AppLightColorScheme || resolved === AppDarkColorScheme,
                    "Expected one of the app's own schemes, not a Material baseline",
                )
            }
        }
    }
}

/** In-memory durable storage, so a test can start the graph with a preference already stored. */
private class MapAppPreferenceStorage : AppPreferenceStorage {
    private val values = mutableMapOf<String, String>()

    override fun read(key: String): String? = values[key]

    override fun write(key: String, value: String?) {
        if (value == null) values.remove(key) else values[key] = value
    }
}

/**
 * Runs [block] with a global Koin holding one [AppearanceStateHolder] over [stored].
 *
 * `AppearanceTheme` looks the holder up in the global context, which is where every host puts it,
 * so these tests start one. The lock is the same one the other application-level tests take,
 * because the global context is shared state.
 */
private fun withAppearanceGraph(
    stored: ThemePreference,
    block: (AppearanceStateHolder) -> Unit,
) {
    synchronized(appIntegrationMainDispatcherLock) {
        stopKoin()
        val storage = MapAppPreferenceStorage()
        ThemePreferenceStore(storage).write(stored)
        val holder = AppearanceStateHolder(ThemePreferenceStore(storage))
        try {
            startKoin { modules(module { single { holder } }) }
            block(holder)
        } finally {
            stopKoin()
        }
    }
}
