package org.artkachenko.kmp_learning_app

import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibleCurriculumRepository
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDataInitializer
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.settings.AppearanceStateHolder
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform

/**
 * The web host's own graph start, in a real browser, on both web targets.
 *
 * `SharedHostStartupTest` proves the shared modules on the JVM with JVM platform bindings. This
 * is the half only a browser can run: `startWebLocalDataGraph` installs the web database and
 * preference bindings beside them, and everything `webApp`'s `main` reaches before composing —
 * the startup initializer, the appearance holder the theme reads synchronously, and the
 * visibility decorator over the preference store — resolves. Koin resolves at runtime, so a
 * missing web binding would compile and pass every other check.
 *
 * Nothing is imported and the database is never opened, so the production `curriculum.db` that
 * `WebCurriculumDatabaseTest` opens is left alone; the database object is still closed, and the
 * global graph stopped, so no later test sees either.
 */
internal class WebLocalDataGraphTest {

    @Test
    fun theWebGraphResolvesWhatTheHostReachesBeforeComposing() {
        stopKoin()
        try {
            startWebLocalDataGraph()
            val koin = KoinPlatform.getKoin()

            assertIs<CurriculumDataInitializer>(webAppStartupInitializer())
            assertIs<AppearanceStateHolder>(koin.get<AppearanceStateHolder>())
            assertIs<VisibleCurriculumRepository>(koin.get<CurriculumRepository>())

            // A second start is the no-op every host relies on, not a second graph.
            startWebLocalDataGraph()
            assertSame(koin, KoinPlatform.getKoin())
        } finally {
            KoinPlatform.getKoinOrNull()?.getOrNull<CurriculumDatabase>()?.close()
            stopKoin()
        }
    }
}
