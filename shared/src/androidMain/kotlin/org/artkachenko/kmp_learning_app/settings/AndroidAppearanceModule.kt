package org.artkachenko.kmp_learning_app.settings

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private const val PreferencesName = "org.artkachenko.kmp_learning_app.preferences"

/** Android's own small key-value store, which is what `SharedPreferences` is for. */
private class AndroidAppPreferenceStorage(context: Context) : AppPreferenceStorage {

    private val preferences =
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)

    override fun read(key: String): String? = preferences.getString(key, null)

    override fun write(key: String, value: String?) {
        preferences.edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }
}

internal val androidAppearanceModule = module {
    single<AppPreferenceStorage> {
        AndroidAppPreferenceStorage(androidContext())
    }
}
