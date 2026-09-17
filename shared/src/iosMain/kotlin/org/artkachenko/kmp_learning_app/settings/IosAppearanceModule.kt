package org.artkachenko.kmp_learning_app.settings

import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

/** iOS's own small key-value store, which is what `NSUserDefaults` is for. */
private class IosAppPreferenceStorage : AppPreferenceStorage {

    private val defaults = NSUserDefaults.standardUserDefaults

    override fun read(key: String): String? = defaults.stringForKey(key)

    override fun write(key: String, value: String?) {
        if (value == null) {
            defaults.removeObjectForKey(key)
        } else {
            defaults.setObject(value, forKey = key)
        }
    }
}

internal val iosAppearanceModule = module {
    single<AppPreferenceStorage> { IosAppPreferenceStorage() }
}
