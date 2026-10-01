package org.artkachenko.kmp_learning_app.settings

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.koin.dsl.koinApplication

/**
 * The browser store against real `localStorage`, on both web targets.
 *
 * The rules for what a stored token means are tested through an in-memory store in `commonTest`;
 * this checks the part a fake cannot — that the `js()` bridge returns exactly what was stored, or
 * null when nothing is, and that clearing one key leaves another alone. A browser with storage
 * blocked cannot be produced here, so the catch branches are covered by inspection only.
 */
internal class WebAppPreferenceStorageTest {

    private val storage = koinApplication { modules(webAppearanceModule) }
        .koin
        .get<AppPreferenceStorage>()

    @AfterTest
    fun clearKeys() {
        listOf(ThemePreferenceStore.Key, KmpContentPreferenceStore.Key, EmptyKey)
            .forEach { storage.write(it, null) }
    }

    @Test
    fun aValueReadsBackAndANeverWrittenKeyIsAbsent() {
        assertNull(storage.read(ThemePreferenceStore.Key))

        storage.write(ThemePreferenceStore.Key, ThemePreferenceStore.DarkToken)

        assertEquals(ThemePreferenceStore.DarkToken, storage.read(ThemePreferenceStore.Key))
    }

    @Test
    fun clearingOneKeyLeavesTheOtherStored() {
        storage.write(ThemePreferenceStore.Key, ThemePreferenceStore.LightToken)
        storage.write(KmpContentPreferenceStore.Key, KmpContentPreferenceStore.OnToken)

        storage.write(ThemePreferenceStore.Key, null)

        assertNull(storage.read(ThemePreferenceStore.Key))
        assertEquals(KmpContentPreferenceStore.OnToken, storage.read(KmpContentPreferenceStore.Key))
    }

    @Test
    fun anEmptyValueIsStoredRatherThanReadAsAbsence() {
        storage.write(EmptyKey, "")

        assertEquals("", storage.read(EmptyKey))
    }

    private companion object {
        const val EmptyKey = "test.empty_value"
    }
}
