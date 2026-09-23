package org.artkachenko.kmp_learning_app.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The application's appearance preference, owned for the lifetime of the application.
 *
 * Deliberately not a Settings-screen ViewModel. The theme is applied above the whole navigation
 * shell, so the thing that holds it has to outlive any single entry in the back stack; a holder
 * scoped to Settings would be destroyed the moment Settings was popped, taking the app's theme with
 * it. It is a Koin `single`, so there is exactly one and every observer sees the same value.
 *
 * The stored preference is read once, synchronously, in the constructor. A Koin `single` is lazy,
 * so that happens when the holder is first resolved rather than while the host builds its graph: in
 * the running app the first resolution is `AppearanceTheme`'s `remember` block, on the first
 * composition. What matters is that the read is synchronous rather than asynchronous — it completes
 * within that composition, so the effective theme is settled before the first frame and the app
 * cannot open light and then flip to dark. It is one key-value read per process on every host, and
 * `AppRoot` has no startup step waiting on it.
 */
internal class AppearanceStateHolder(private val store: ThemePreferenceStore) {

    private val _preference = MutableStateFlow(store.read())

    /** The current preference. [resolveDarkTheme] turns it into the effective theme. */
    val preference: StateFlow<ThemePreference> = _preference.asStateFlow()

    /**
     * Records an explicit choice and persists it.
     *
     * The write is not dispatched: it is one small key-value write, and doing it here means the
     * preference is durable by the time the switch has finished moving, rather than depending on a
     * coroutine that a closing app may not have run. The observable state is updated first, so the
     * UI follows the learner's action immediately.
     */
    fun setDarkTheme(enabled: Boolean) {
        val chosen = if (enabled) ThemePreference.Dark else ThemePreference.Light
        if (_preference.value == chosen) return
        _preference.value = chosen
        store.write(chosen)
    }
}
