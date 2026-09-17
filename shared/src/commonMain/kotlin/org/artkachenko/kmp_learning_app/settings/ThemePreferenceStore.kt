package org.artkachenko.kmp_learning_app.settings

/**
 * Reads and writes the appearance override, and owns its storage key and encoding.
 *
 * The key and the two tokens live here, in common code, rather than in each platform store: five
 * copies of a string constant are five chances for one host to forget what the others remember.
 * Each platform implements [AppPreferenceStorage] and nothing else.
 *
 * An unrecognised stored value — a token written by a future version, or corrupted data — reads as
 * [ThemePreference.System] rather than throwing, so the app starts under the system theme instead
 * of not starting.
 */
internal class ThemePreferenceStore(private val storage: AppPreferenceStorage) {

    fun read(): ThemePreference = when (storage.read(Key)) {
        LightToken -> ThemePreference.Light
        DarkToken -> ThemePreference.Dark
        else -> ThemePreference.System
    }

    /**
     * Persists an explicit choice.
     *
     * [ThemePreference.System] clears the key, because following the operating system *is* the
     * absence of an override. The effective theme is never written: storing "dark, because the
     * phone was dark that evening" would turn a system default into a choice the learner never
     * made, and would then survive them switching the phone back.
     */
    fun write(preference: ThemePreference) {
        storage.write(
            Key,
            when (preference) {
                ThemePreference.System -> null
                ThemePreference.Light -> LightToken
                ThemePreference.Dark -> DarkToken
            },
        )
    }

    internal companion object {
        /**
         * The storage key, named for what it holds rather than for the screen that changes it.
         *
         * It is part of the app's durable contract with an installed copy of itself: renaming it
         * silently discards every learner's saved appearance, so it stays as it is.
         */
        const val Key: String = "appearance.theme"

        const val LightToken: String = "light"
        const val DarkToken: String = "dark"
    }
}
