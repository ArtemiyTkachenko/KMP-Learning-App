package org.artkachenko.kmp_learning_app.settings

/**
 * Durable storage standing in for `SharedPreferences`, `NSUserDefaults`, a properties file and
 * `localStorage`.
 *
 * The preference is tested through the same [AppPreferenceStorage] boundary every host implements,
 * so the behaviour the preference tests pin is what each of them produces without needing a device, a simulator or a
 * browser. [reopen] is the interesting part: it hands the *same* stored contents to a new store, so
 * a test can distinguish "the holder remembers" from "the value was persisted".
 */
internal class InMemoryAppPreferenceStorage(
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
