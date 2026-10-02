package org.artkachenko.kmp_learning_app

import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDataInitializer
import org.artkachenko.kmp_learning_app.data.local.curriculum.webCurriculumDataModule
import org.artkachenko.kmp_learning_app.settings.webAppearanceModule
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

public fun startWebLocalDataGraph() {
    if (KoinPlatform.getKoinOrNull() != null) return

    startKoin {
        // A second definition of a type fails here rather than silently replacing the first.
        strictOverride()
        modules(sharedApplicationModules())
        modules(webCurriculumDataModule, webAppearanceModule)
    }
}

internal fun webAppStartupInitializer(): AppStartupInitializer =
    KoinPlatform.getKoin().get<CurriculumDataInitializer>()
