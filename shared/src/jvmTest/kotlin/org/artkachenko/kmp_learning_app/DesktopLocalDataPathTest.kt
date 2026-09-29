package org.artkachenko.kmp_learning_app

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin

internal class DesktopLocalDataPathTest {
    @Test
    fun desktopStartupBridgeCreatesPersistentDatabaseAndInitializesCurriculum() = runTest {
        val originalUserHome = System.getProperty(UserHomeProperty)
        val tempHome = Files.createTempDirectory("kmp-learning-app-desktop-test").toFile()

        try {
            System.setProperty(UserHomeProperty, tempHome.absolutePath)

            startDesktopLocalDataGraph()
            desktopAppStartupInitializer().initialize()

            val repository = GlobalContext.get().get<CurriculumRepository>()
            // A fresh install hides the optional Kotlin Multiplatform Topic: 16 of the 17 imported
            // Topics are visible, and the hidden one still resolves by ID for stored history.
            val activeTopics = repository.getActiveTopics()
            assertEquals(16, activeTopics.size)
            assertTrue(activeTopics.none { it.id == "kmp" })
            assertNotNull(repository.getTopicById("kmp"))
            assertTrue(
                tempHome.resolve(".kmp-learning-app/curriculum.db").exists(),
                "Desktop runtime should create a persistent Room database under the user's app directory.",
            )
        } finally {
            stopKoin()
            System.setProperty(UserHomeProperty, originalUserHome)
            tempHome.deleteRecursively()
        }
    }

    private companion object {
        const val UserHomeProperty = "user.home"
    }
}
