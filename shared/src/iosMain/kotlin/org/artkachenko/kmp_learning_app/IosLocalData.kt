package org.artkachenko.kmp_learning_app

import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDataInitializer
import org.artkachenko.kmp_learning_app.data.local.curriculum.iosCurriculumDataModule
import org.artkachenko.kmp_learning_app.settings.iosAppearanceModule
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

internal fun startIosLocalDataGraph() {
    if (KoinPlatform.getKoinOrNull() != null) return

    startKoin {
        // A second definition of a type fails here rather than silently replacing the first.
        strictOverride()
        modules(sharedApplicationModules())
        modules(iosCurriculumDataModule, iosAppearanceModule)
    }
}

internal fun iosAppStartupInitializer(): AppStartupInitializer =
    KoinPlatform.getKoin().get<CurriculumDataInitializer>()
