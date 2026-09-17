package org.artkachenko.kmp_learning_app.settings

/**
 * The smallest durable key-value store the application needs, one method to read and one to write.
 *
 * One preference does not justify a settings framework or a Room migration, and the five configured
 * targets each already have a normal small key-value store of their own — `SharedPreferences`,
 * `NSUserDefaults`, a properties file beside the desktop database, `localStorage`. This interface
 * is the whole boundary between them and the rest of the app, which is also what makes the
 * preference testable without any of them: see `InMemoryAppPreferenceStorage` in the tests.
 *
 * Implementations must not throw. A store that cannot be read — a browser with site data blocked,
 * an unreadable file — reports absence, because failing to remember a preference is not a reason to
 * fail to start.
 */
internal interface AppPreferenceStorage {

    /** The stored value for [key], or null when nothing is stored or the store is unreadable. */
    fun read(key: String): String?

    /** Stores [value] under [key], or clears the key when [value] is null. */
    fun write(key: String, value: String?)
}
