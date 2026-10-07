package org.artkachenko.kmp_learning_app.progress

import org.artkachenko.kmp_learning_app.learning_progress.ReadinessTarget
import org.artkachenko.kmp_learning_app.settings.AppPreferenceStorage

/**
 * Reads and writes the interview level the learner is preparing for, and owns its storage key and
 * encoding.
 *
 * Follows [org.artkachenko.kmp_learning_app.settings.KmpContentPreferenceStore]: every choice is
 * written as its own explicit token, and absence only means "not chosen yet" — which here is a
 * state the UI shows, a chooser rather than a guessed target. An unrecognised token, from a later
 * version or corrupted data, reads as not chosen, because judging a learner against a level they
 * never picked would be a false verdict.
 *
 * The tokens are spelled out rather than taken from the enum names, so renaming a
 * [ReadinessTarget] constant can never silently forget a stored choice.
 */
internal class ReadinessTargetPreferenceStore(private val storage: AppPreferenceStorage) {

    fun read(): ReadinessTarget? =
        when (storage.read(Key)) {
            JuniorToken -> ReadinessTarget.JUNIOR
            MidLevelToken -> ReadinessTarget.MID_LEVEL
            SeniorToken -> ReadinessTarget.SENIOR
            else -> null
        }

    fun write(target: ReadinessTarget) {
        storage.write(
            Key,
            when (target) {
                ReadinessTarget.JUNIOR -> JuniorToken
                ReadinessTarget.MID_LEVEL -> MidLevelToken
                ReadinessTarget.SENIOR -> SeniorToken
            },
        )
    }

    internal companion object {
        /**
         * The storage key, named for what it holds. Part of the app's durable contract with an
         * installed copy of itself, like the theme and content keys, and must not be renamed.
         */
        const val Key: String = "readiness.target"

        const val JuniorToken: String = "junior"
        const val MidLevelToken: String = "mid"
        const val SeniorToken: String = "senior"
    }
}
