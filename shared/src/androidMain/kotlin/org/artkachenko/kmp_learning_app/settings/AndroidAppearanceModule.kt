package org.artkachenko.kmp_learning_app.settings

import android.content.Context
import android.content.SharedPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private const val PreferencesName = "org.artkachenko.kmp_learning_app.preferences"

/**
 * Android's own small key-value store, which is what `SharedPreferences` is for.
 *
 * Every call is guarded, because [AppPreferenceStorage] requires implementations not to throw and
 * this one is read synchronously while `AppearanceTheme` composes — above the startup error screen,
 * so a thrown exception would be a crash on the first frame rather than a forgotten preference.
 * `getSharedPreferences` throws when the preferences directory is unavailable, which is the normal
 * state of a credential-protected context before the device is unlocked, and `getString` throws if
 * the key ever holds a value of another type. Both mean "no preference stored", which is the same
 * answer the JVM, iOS, and web implementations give when their own store cannot be read.
 */
private class AndroidAppPreferenceStorage(private val context: Context) : AppPreferenceStorage {

    private val preferences: SharedPreferences? by lazy {
        runCatching {
            context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        }.getOrNull()
    }

    override fun read(key: String): String? = runCatching {
        preferences?.getString(key, null)
    }.getOrNull()

    override fun write(key: String, value: String?) {
        runCatching {
            preferences?.edit()?.apply {
                if (value == null) remove(key) else putString(key, value)
            }?.apply()
        }
    }
}

internal val androidAppearanceModule = module {
    single<AppPreferenceStorage> {
        AndroidAppPreferenceStorage(androidContext())
    }
}
