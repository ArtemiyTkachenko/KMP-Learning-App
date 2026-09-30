package org.artkachenko.kmp_learning_app

import android.app.Application
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDataInitializer
import org.artkachenko.kmp_learning_app.data.local.curriculum.androidCurriculumDataModule
import org.artkachenko.kmp_learning_app.settings.androidAppearanceModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

public fun startAndroidLocalDataGraph(application: Application) {
    if (GlobalContext.getOrNull() != null) return

    startKoin {
        // A second definition of a type fails here rather than silently replacing the first.
        strictOverride()
        androidContext(application.applicationContext)
        modules(sharedApplicationModules())
        modules(androidCurriculumDataModule, androidAppearanceModule)
    }
}

public fun androidAppStartupInitializer(): AppStartupInitializer =
    GlobalContext.get().get<CurriculumDataInitializer>()
