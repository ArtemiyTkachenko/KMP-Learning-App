package org.artkachenko.kmp_learning_app

import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDataInitializer
import org.artkachenko.kmp_learning_app.data.local.curriculum.jvmCurriculumDataModule
import org.artkachenko.kmp_learning_app.settings.jvmAppearanceModule
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

public fun startDesktopLocalDataGraph() {
    if (GlobalContext.getOrNull() != null) return

    startKoin {
        // A second definition of a type fails here rather than silently replacing the first.
        strictOverride()
        modules(sharedApplicationModules())
        modules(jvmCurriculumDataModule, jvmAppearanceModule)
    }
}

public fun desktopAppStartupInitializer(): AppStartupInitializer =
    GlobalContext.get().get<CurriculumDataInitializer>()
