package org.artkachenko.kmp_learning_app.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Durable storage standing in for `SharedPreferences`, `NSUserDefaults`, a properties file and
 * `localStorage`.
 *
 * The preference is tested through the same [AppPreferenceStorage] boundary every host implements,
 * so the behaviour below is what each of them produces without needing a device, a simulator or a
 * browser. [reopen] is the interesting part: it hands the *same* stored contents to a new store, so
 * a test can distinguish "the holder remembers" from "the value was persisted".
 */
private class InMemoryAppPreferenceStorage(
    private val values: MutableMap<String, String> = mutableMapOf(),
) : AppPreferenceStorage {

    override fun read(key: String): String? = values[key]

    override fun write(key: String, value: String?) {
        if (value == null) values.remove(key) else values[key] = value
    }

    /** A second store over the same durable contents, as restarting the app would produce. */
    fun reopen(): InMemoryAppPreferenceStorage = InMemoryAppPreferenceStorage(values)

    fun stored(key: String): String? = values[key]
}

internal class ThemePreferenceResolutionTest {

    @Test
    fun withNoOverrideTheSystemDecides() {
        assertTrue(ThemePreference.System.resolveDarkTheme(systemInDarkTheme = true))
        assertFalse(ThemePreference.System.resolveDarkTheme(systemInDarkTheme = false))
    }

    /**
     * The compatibility rule this feature turns on: once the learner has chosen, the operating
     * system no longer gets a vote. Both system values are exercised for each choice, because a
     * resolver that ignored its argument only in one direction would still pass a single case.
     */
    @Test
    fun anExplicitChoiceOutranksTheSystemInBothDirections() {
        assertFalse(ThemePreference.Light.resolveDarkTheme(systemInDarkTheme = true))
        assertFalse(ThemePreference.Light.resolveDarkTheme(systemInDarkTheme = false))
        assertTrue(ThemePreference.Dark.resolveDarkTheme(systemInDarkTheme = true))
        assertTrue(ThemePreference.Dark.resolveDarkTheme(systemInDarkTheme = false))
    }
}

internal class ThemePreferenceStoreTest {

    @Test
    fun anEmptyStoreReportsNoOverride() {
        val store = ThemePreferenceStore(InMemoryAppPreferenceStorage())

        assertEquals(ThemePreference.System, store.read())
    }

    @Test
    fun anExplicitChoiceRoundTripsThroughStorage() {
        val storage = InMemoryAppPreferenceStorage()
        val store = ThemePreferenceStore(storage)

        store.write(ThemePreference.Light)
        assertEquals(ThemePreference.Light, store.read())
        assertEquals(ThemePreferenceStore.LightToken, storage.stored(ThemePreferenceStore.Key))

        store.write(ThemePreference.Dark)
        assertEquals(ThemePreference.Dark, store.read())
        assertEquals(ThemePreferenceStore.DarkToken, storage.stored(ThemePreferenceStore.Key))
    }

    /** Following the system is the absence of an override, so it clears the key rather than adding a third token. */
    @Test
    fun returningToTheSystemThemeClearsTheStoredKey() {
        val storage = InMemoryAppPreferenceStorage()
        val store = ThemePreferenceStore(storage)
        store.write(ThemePreference.Dark)

        store.write(ThemePreference.System)

        assertNull(storage.stored(ThemePreferenceStore.Key))
        assertEquals(ThemePreference.System, store.read())
    }

    /**
     * A value this version does not recognise — written by a later one, or corrupted — must not
     * stop the app starting. It reads as no override, so the system theme applies.
     */
    @Test
    fun anUnrecognisedStoredValueReadsAsNoOverride() {
        val storage = InMemoryAppPreferenceStorage()
        storage.write(ThemePreferenceStore.Key, "midnight")

        assertEquals(ThemePreference.System, ThemePreferenceStore(storage).read())
    }
}

internal class AppearanceStateHolderTest {

    @Test
    fun aHolderOverEmptyStorageReportsSystemBehaviour() {
        val holder = AppearanceStateHolder(ThemePreferenceStore(InMemoryAppPreferenceStorage()))

        assertEquals(ThemePreference.System, holder.preference.value)
    }

    @Test
    fun aHolderRestoresAStoredLightChoice() {
        val storage = InMemoryAppPreferenceStorage()
        ThemePreferenceStore(storage).write(ThemePreference.Light)

        val holder = AppearanceStateHolder(ThemePreferenceStore(storage))

        assertEquals(ThemePreference.Light, holder.preference.value)
        assertFalse(holder.preference.value.resolveDarkTheme(systemInDarkTheme = true))
    }

    @Test
    fun aHolderRestoresAStoredDarkChoice() {
        val storage = InMemoryAppPreferenceStorage()
        ThemePreferenceStore(storage).write(ThemePreference.Dark)

        val holder = AppearanceStateHolder(ThemePreferenceStore(storage))

        assertEquals(ThemePreference.Dark, holder.preference.value)
        assertTrue(holder.preference.value.resolveDarkTheme(systemInDarkTheme = false))
    }

    @Test
    fun turningTheSwitchOnRecordsAnExplicitDarkChoice() {
        val storage = InMemoryAppPreferenceStorage()
        val holder = AppearanceStateHolder(ThemePreferenceStore(storage))

        holder.setDarkTheme(true)

        assertEquals(ThemePreference.Dark, holder.preference.value)
        assertEquals(ThemePreferenceStore.DarkToken, storage.stored(ThemePreferenceStore.Key))
    }

    @Test
    fun turningTheSwitchOffRecordsAnExplicitLightChoice() {
        val storage = InMemoryAppPreferenceStorage()
        ThemePreferenceStore(storage).write(ThemePreference.Dark)
        val holder = AppearanceStateHolder(ThemePreferenceStore(storage))

        holder.setDarkTheme(false)

        assertEquals(ThemePreference.Light, holder.preference.value)
        assertEquals(ThemePreferenceStore.LightToken, storage.stored(ThemePreferenceStore.Key))
    }

    /**
     * A learner whose system is dark sees the switch on, and turning it off must persist Light
     * rather than nothing: the point of the feature is that the app stops following the system.
     */
    @Test
    fun turningTheSwitchOffUnderADarkSystemStillPersistsAnOverride() {
        val storage = InMemoryAppPreferenceStorage()
        val holder = AppearanceStateHolder(ThemePreferenceStore(storage))
        assertTrue(holder.preference.value.resolveDarkTheme(systemInDarkTheme = true))

        holder.setDarkTheme(false)

        // The system is still dark; the app is not.
        assertFalse(holder.preference.value.resolveDarkTheme(systemInDarkTheme = true))
        assertEquals(ThemePreferenceStore.LightToken, storage.stored(ThemePreferenceStore.Key))
    }

    /** Restart: a new holder over the same durable storage restores the explicit choice. */
    @Test
    fun reconstructingTheHolderPreservesAnExplicitChoice() {
        val storage = InMemoryAppPreferenceStorage()
        AppearanceStateHolder(ThemePreferenceStore(storage)).setDarkTheme(true)

        val restarted = AppearanceStateHolder(ThemePreferenceStore(storage.reopen()))

        assertEquals(ThemePreference.Dark, restarted.preference.value)
    }

    /**
     * Nothing writes the effective theme. A learner who never opens Settings under a dark system
     * must still be following the system afterwards, so that switching the phone to light switches
     * the app with it.
     */
    @Test
    fun theEffectiveSystemThemeIsNeverPersistedAsAChoice() {
        val storage = InMemoryAppPreferenceStorage()
        val holder = AppearanceStateHolder(ThemePreferenceStore(storage))

        // Observing the effective theme, repeatedly, under a dark system.
        repeat(3) { holder.preference.value.resolveDarkTheme(systemInDarkTheme = true) }

        assertNull(storage.stored(ThemePreferenceStore.Key))
        val restarted = AppearanceStateHolder(ThemePreferenceStore(storage.reopen()))
        assertEquals(ThemePreference.System, restarted.preference.value)
    }
}
