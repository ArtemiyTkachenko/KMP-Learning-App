package org.artkachenko.kmp_learning_app.data.local.curriculum

import org.koin.dsl.module

internal val webCurriculumDataModule = module {
    single {
        createWebCurriculumDatabase()
    }
}
