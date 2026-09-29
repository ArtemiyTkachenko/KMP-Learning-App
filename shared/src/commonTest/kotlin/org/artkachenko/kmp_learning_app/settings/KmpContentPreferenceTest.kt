package org.artkachenko.kmp_learning_app.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibility
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibilityStateHolder

internal class KmpContentPreferenceStoreTest {

    @Test
    fun anEmptyStoreReadsAsExcluded() {
        assertFalse(KmpContentPreferenceStore(InMemoryAppPreferenceStorage()).read())
    }

    @Test
    fun theOnTokenReadsAsIncluded() {
        val storage = InMemoryAppPreferenceStorage()
        storage.write(KmpContentPreferenceStore.Key, "on")

        assertTrue(KmpContentPreferenceStore(storage).read())
    }

    @Test
    fun theOffTokenReadsAsExcluded() {
        val storage = InMemoryAppPreferenceStorage()
        storage.write(KmpContentPreferenceStore.Key, "off")

        assertFalse(KmpContentPreferenceStore(storage).read())
    }

    /** A token from a later version, or corrupted data, must not include optional content. */
    @Test
    fun anUnrecognisedTokenReadsAsExcluded() {
        val storage = InMemoryAppPreferenceStorage()
        storage.write(KmpContentPreferenceStore.Key, "true")

        assertFalse(KmpContentPreferenceStore(storage).read())
    }

    @Test
    fun includingWritesTheOnToken() {
        val storage = InMemoryAppPreferenceStorage()

        KmpContentPreferenceStore(storage).write(true)

        assertEquals("on", storage.stored("content.include_kmp"))
    }

    /**
     * The deliberate difference from the theme store: OFF is a stored choice, not an absent key, so
     * a future change of the default cannot reinterpret it.
     */
    @Test
    fun excludingWritesTheOffTokenRatherThanClearingTheKey() {
        val storage = InMemoryAppPreferenceStorage()
        KmpContentPreferenceStore(storage).write(true)

        KmpContentPreferenceStore(storage).write(false)

        assertEquals("off", storage.stored("content.include_kmp"))
    }
}

internal class CurriculumVisibilityStateHolderTest {

    @Test
    fun aFreshInstallStartsWithKmpContentHidden() {
        val holder = CurriculumVisibilityStateHolder(KmpContentPreferenceStore(InMemoryAppPreferenceStorage()))

        assertFalse(holder.includeKmpContent.value)
        assertFalse(holder.visibility.value.isTopicVisible("kmp"))
        assertTrue(holder.visibility.value.isTopicVisible("android_fundamentals"))
    }

    @Test
    fun includingUpdatesTheStateAndPersistsSynchronously() {
        val storage = InMemoryAppPreferenceStorage()
        val holder = CurriculumVisibilityStateHolder(KmpContentPreferenceStore(storage))

        holder.setIncludeKmpContent(true)

        assertTrue(holder.includeKmpContent.value)
        assertTrue(holder.visibility.value.isTopicVisible("kmp"))
        assertEquals("on", storage.stored(KmpContentPreferenceStore.Key))
    }

    @Test
    fun excludingUpdatesTheStateAndPersistsSynchronously() {
        val storage = InMemoryAppPreferenceStorage()
        storage.write(KmpContentPreferenceStore.Key, "on")
        val holder = CurriculumVisibilityStateHolder(KmpContentPreferenceStore(storage))

        holder.setIncludeKmpContent(false)

        assertFalse(holder.includeKmpContent.value)
        assertFalse(holder.visibility.value.isTopicVisible("kmp"))
        assertEquals("off", storage.stored(KmpContentPreferenceStore.Key))
    }

    /**
     * Choosing OFF on a fresh install changes no state, but it is still a choice and must be
     * stored, or a later default of ON would silently override it.
     */
    @Test
    fun explicitlyChoosingTheDefaultIsStillPersisted() {
        val storage = InMemoryAppPreferenceStorage()
        val holder = CurriculumVisibilityStateHolder(KmpContentPreferenceStore(storage))

        holder.setIncludeKmpContent(false)

        assertEquals("off", storage.stored(KmpContentPreferenceStore.Key))
    }

    @Test
    fun aHolderOverTheSameStorageRestoresTheChoice() {
        val storage = InMemoryAppPreferenceStorage()
        CurriculumVisibilityStateHolder(KmpContentPreferenceStore(storage)).setIncludeKmpContent(true)

        val restarted = CurriculumVisibilityStateHolder(KmpContentPreferenceStore(storage.reopen()))

        assertTrue(restarted.includeKmpContent.value)
        assertEquals(CurriculumVisibility.from(includeKmpContent = true), restarted.visibility.value)
    }

    @Test
    fun visibilityFollowsThePreferenceBothWays() {
        val holder = CurriculumVisibilityStateHolder(KmpContentPreferenceStore(InMemoryAppPreferenceStorage()))

        holder.setIncludeKmpContent(true)
        assertTrue(holder.visibility.value.isTopicVisible("kmp"))

        holder.setIncludeKmpContent(false)
        assertFalse(holder.visibility.value.isTopicVisible("kmp"))
    }
}
