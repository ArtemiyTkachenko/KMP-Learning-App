package org.artkachenko.kmp_learning_app.data.local.curriculum

import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.data.local.curriculum.repository.LocalCurriculumRepository
import org.koin.dsl.module

internal val curriculumDataModule = module {
    single {
        CurriculumImporter(
            database = get(),
        )
    }

    single<CurriculumRepository> {
        LocalCurriculumRepository(
            database = get(),
        )
    }

    single {
        CurriculumDataInitializer(
            importer = get(),
        )
    }
}
