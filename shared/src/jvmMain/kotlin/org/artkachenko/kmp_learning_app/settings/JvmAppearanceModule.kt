package org.artkachenko.kmp_learning_app.settings

import java.io.File
import java.util.Properties
import org.koin.dsl.module

/**
 * A properties file in the directory the desktop host already keeps its database in.
 *
 * `~/.kmp-learning-app` is where `createJvmCurriculumDatabase` puts `curriculum.db`, so the
 * preference lives beside the data it belongs to rather than inventing a second location. The
 * directory name keeps the old spelling deliberately: it addresses an existing installation's
 * files, and renaming it would orphan them.
 *
 * `java.util.prefs` was the alternative and is worse here — on a desktop JVM it writes into a
 * platform-specific registry or a hidden per-user tree, which is invisible to anyone debugging the
 * app and unrelated to where the rest of its state lives.
 */
private const val AppDirectoryName = ".kmp-learning-app"
private const val PreferencesFileName = "preferences.properties"

private class JvmAppPreferenceStorage : AppPreferenceStorage {

    private val file: File
        get() = File(File(System.getProperty("user.home"), AppDirectoryName), PreferencesFileName)

    override fun read(key: String): String? = runCatching {
        val source = file
        if (!source.isFile) return null
        Properties().apply { source.inputStream().use { load(it) } }.getProperty(key)
    }.getOrNull()

    override fun write(key: String, value: String?) {
        runCatching {
            val target = file
            target.parentFile?.mkdirs()
            // Read-modify-write rather than overwrite: this file is the application's preferences,
            // not this preference's file, so a second one added later must not erase the first.
            val properties = Properties().apply {
                if (target.isFile) target.inputStream().use { load(it) }
            }
            if (value == null) properties.remove(key) else properties.setProperty(key, value)
            target.outputStream().use { properties.store(it, "Android Engineering Lab preferences") }
        }
    }
}

internal val jvmAppearanceModule = module {
    single<AppPreferenceStorage> { JvmAppPreferenceStorage() }
}
