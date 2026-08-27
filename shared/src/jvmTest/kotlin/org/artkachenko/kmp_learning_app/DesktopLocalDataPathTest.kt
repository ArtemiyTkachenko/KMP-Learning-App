package org.artkachenko.kmp_learning_app

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
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
            initializeDesktopLocalData()

            val repository = GlobalContext.get().get<CurriculumRepository>()
            assertEquals(17, repository.getActiveTopics().size)
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
