package org.artkachenko.kmp_learning_app.settings

/**
 * Reads and writes whether Kotlin Multiplatform content is included, and owns its storage key and
 * encoding.
 *
 * Absence and any unrecognised token read as excluded, which is the fresh-install default.
 *
 * Unlike [ThemePreferenceStore], both choices are written explicitly and absence is never used to
 * mean "off". The theme's absent key *is* a choice — follow the system — whereas here absence only
 * means "never chosen". If the product default later becomes "included", a learner who deliberately
 * turned the content off must keep it off, and that is only possible if their choice was stored.
 */
internal class KmpContentPreferenceStore(private val storage: AppPreferenceStorage) {

    fun read(): Boolean = storage.read(Key) == OnToken

    fun write(includeKmpContent: Boolean) {
        storage.write(Key, if (includeKmpContent) OnToken else OffToken)
    }

    internal companion object {
        /**
         * The storage key, named for what it holds. Like the theme key, it is part of the app's
         * durable contract with an installed copy of itself and must not be renamed.
         */
        const val Key: String = "content.include_kmp"

        const val OnToken: String = "on"
        const val OffToken: String = "off"
    }
}
