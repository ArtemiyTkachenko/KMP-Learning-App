package org.artkachenko.kmp_learning_app.progress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.artkachenko.kmp_learning_app.learning_progress.ReadinessTarget
import org.artkachenko.kmp_learning_app.settings.InMemoryAppPreferenceStorage

internal class ReadinessTargetPreferenceStoreTest {

    /** Absence is "not chosen yet", which Progress shows as a chooser rather than a guessed level. */
    @Test
    fun anEmptyStoreReadsAsNotChosen() {
        assertNull(ReadinessTargetPreferenceStore(InMemoryAppPreferenceStorage()).read())
    }

    @Test
    fun eachTokenReadsAsItsTarget() {
        val expected = mapOf(
            "junior" to ReadinessTarget.JUNIOR,
            "mid" to ReadinessTarget.MID_LEVEL,
            "senior" to ReadinessTarget.SENIOR,
        )
        expected.forEach { (token, target) ->
            val storage = InMemoryAppPreferenceStorage()
            storage.write("readiness.target", token)

            assertEquals(target, ReadinessTargetPreferenceStore(storage).read())
        }
    }

    /** A token from a later version, or an enum name, must not judge the learner against a level. */
    @Test
    fun anUnrecognisedTokenReadsAsNotChosen() {
        val storage = InMemoryAppPreferenceStorage()
        storage.write(ReadinessTargetPreferenceStore.Key, "SENIOR")

        assertNull(ReadinessTargetPreferenceStore(storage).read())
    }

    @Test
    fun everyTargetIsWrittenAsItsOwnExplicitToken() {
        val written = ReadinessTarget.entries.associateWith { target ->
            val storage = InMemoryAppPreferenceStorage()
            ReadinessTargetPreferenceStore(storage).write(target)
            storage.stored("readiness.target")
        }

        assertEquals(
            mapOf(
                ReadinessTarget.JUNIOR to "junior",
                ReadinessTarget.MID_LEVEL to "mid",
                ReadinessTarget.SENIOR to "senior",
            ),
            written,
        )
    }
}

internal class ReadinessTargetStateHolderTest {

    @Test
    fun aFreshInstallHasNoTarget() {
        val holder = ReadinessTargetStateHolder(ReadinessTargetPreferenceStore(InMemoryAppPreferenceStorage()))

        assertNull(holder.target.value)
    }

    @Test
    fun theStoredChoiceIsReadOnConstruction() {
        val storage = InMemoryAppPreferenceStorage()
        ReadinessTargetPreferenceStore(storage).write(ReadinessTarget.MID_LEVEL)

        val holder = ReadinessTargetStateHolder(ReadinessTargetPreferenceStore(storage))

        assertEquals(ReadinessTarget.MID_LEVEL, holder.target.value)
    }

    @Test
    fun choosingUpdatesTheStateAndPersistsSynchronously() {
        val storage = InMemoryAppPreferenceStorage()
        val holder = ReadinessTargetStateHolder(ReadinessTargetPreferenceStore(storage))

        holder.setTarget(ReadinessTarget.SENIOR)

        assertEquals(ReadinessTarget.SENIOR, holder.target.value)
        assertEquals("senior", storage.stored("readiness.target"))
        // A new process reads the same choice back.
        assertEquals(
            ReadinessTarget.SENIOR,
            ReadinessTargetStateHolder(ReadinessTargetPreferenceStore(storage)).target.value,
        )
    }
}
