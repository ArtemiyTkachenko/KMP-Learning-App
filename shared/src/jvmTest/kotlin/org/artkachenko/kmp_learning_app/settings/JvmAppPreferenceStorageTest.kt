package org.artkachenko.kmp_learning_app.settings

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibilityStateHolder
import org.koin.dsl.koinApplication

/**
 * The desktop host's actual durable storage.
 *
 * `AppearancePreferenceTest` covers the behaviour through an in-memory store, which is the right
 * place for the rules; this covers the one thing a fake cannot — that the JVM `actual` really
 * writes a file the next process can read, in the directory the desktop host already keeps its
 * database in. `user.home` is redirected exactly as `DesktopLocalDataPathTest` does it.
 */
internal class JvmAppPreferenceStorageTest {

    @Test
    fun anExplicitChoiceSurvivesAsAFileTheNextProcessReads() {
        withTemporaryHome { home ->
            val storage = koinApplication { modules(jvmAppearanceModule) }
                .koin
                .get<AppPreferenceStorage>()

            assertEquals(ThemePreference.System, ThemePreferenceStore(storage).read())

            AppearanceStateHolder(ThemePreferenceStore(storage)).setDarkTheme(true)

            val preferences = home.resolve(".kmp-learning-app/preferences.properties")
            assertTrue(preferences.isFile, "The desktop host should persist preferences to a file")
            assertTrue(
                preferences.readText().contains("${ThemePreferenceStore.Key}=dark"),
                "Expected the stored appearance token, got:\n${preferences.readText()}",
            )

            // A new graph over the same home, which is what a restart is.
            val restarted = koinApplication { modules(jvmAppearanceModule) }
                .koin
                .get<AppPreferenceStorage>()
            assertEquals(
                ThemePreference.Dark,
                AppearanceStateHolder(ThemePreferenceStore(restarted)).preference.value,
            )
        }
    }

    /**
     * The file is the application's preferences, not this preference's file: writing one key must
     * not drop another. Nothing else writes to it yet, which is exactly when this is easy to break.
     */
    @Test
    fun writingOnePreferenceLeavesTheOthersAlone() {
        withTemporaryHome {
            val storage = koinApplication { modules(jvmAppearanceModule) }
                .koin
                .get<AppPreferenceStorage>()

            storage.write("unrelated.key", "kept")
            ThemePreferenceStore(storage).write(ThemePreference.Light)

            assertEquals("kept", storage.read("unrelated.key"))
            assertEquals(ThemePreference.Light, ThemePreferenceStore(storage).read())
        }
    }

    /**
     * The KMP content preference shares the one preferences file with the appearance preference
     * and anything else stored there: it survives a restart, and neither preference erases the
     * other or an unrelated key.
     */
    @Test
    fun theKmpContentPreferenceSharesTheFileAndSurvivesARestart() {
        withTemporaryHome { home ->
            val storage = koinApplication { modules(jvmAppearanceModule) }
                .koin
                .get<AppPreferenceStorage>()
            storage.write("unrelated.key", "kept")
            AppearanceStateHolder(ThemePreferenceStore(storage)).setDarkTheme(true)

            CurriculumVisibilityStateHolder(KmpContentPreferenceStore(storage)).setIncludeKmpContent(true)

            val appDirectory = home.resolve(".kmp-learning-app")
            assertEquals(
                listOf("preferences.properties"),
                appDirectory.list()?.sorted(),
                "The preference must use the existing preferences file, not a second one",
            )
            val contents = appDirectory.resolve("preferences.properties").readText()
            assertTrue(contents.contains("${KmpContentPreferenceStore.Key}=on"), contents)
            assertTrue(contents.contains("${ThemePreferenceStore.Key}=dark"), contents)

            val restarted = koinApplication { modules(jvmAppearanceModule) }
                .koin
                .get<AppPreferenceStorage>()
            val holder = CurriculumVisibilityStateHolder(KmpContentPreferenceStore(restarted))
            assertTrue(holder.includeKmpContent.value)
            assertEquals(ThemePreference.Dark, ThemePreferenceStore(restarted).read())
            assertEquals("kept", restarted.read("unrelated.key"))

            // Turning it off is stored as a value, and still leaves the other keys alone.
            holder.setIncludeKmpContent(false)
            assertEquals(KmpContentPreferenceStore.OffToken, restarted.read(KmpContentPreferenceStore.Key))
            assertEquals(ThemePreference.Dark, ThemePreferenceStore(restarted).read())
            assertEquals("kept", restarted.read("unrelated.key"))
        }
    }

    @Test
    fun clearingTheOverrideRemovesItFromTheFile() {
        withTemporaryHome {
            val storage = koinApplication { modules(jvmAppearanceModule) }
                .koin
                .get<AppPreferenceStorage>()
            val store = ThemePreferenceStore(storage)
            store.write(ThemePreference.Dark)

            store.write(ThemePreference.System)

            assertNull(storage.read(ThemePreferenceStore.Key))
            assertEquals(ThemePreference.System, store.read())
        }
    }
}

private fun withTemporaryHome(block: (java.io.File) -> Unit) {
    val originalUserHome = System.getProperty("user.home")
    val temporaryHome = Files.createTempDirectory("kmp-learning-app-preferences-test").toFile()
    try {
        System.setProperty("user.home", temporaryHome.absolutePath)
        block(temporaryHome)
    } finally {
        System.setProperty("user.home", originalUserHome)
        temporaryHome.deleteRecursively()
    }
}
