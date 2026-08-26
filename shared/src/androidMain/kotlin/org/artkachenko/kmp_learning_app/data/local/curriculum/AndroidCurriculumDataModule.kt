package org.artkachenko.kmp_learning_app.data.local.curriculum

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

internal val androidCurriculumDataModule = module {
    single {
        createCurriculumDatabase(
            context = androidContext(),
        )
    }
}
