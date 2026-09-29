package org.artkachenko.kmp_learning_app.data.local.curriculum

import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.data.local.curriculum.repository.LocalCurriculumRepository
import org.koin.dsl.module

internal val curriculumDataModule = module {
    single {
        CurriculumImporter(
            database = get(),
        )
    }

    single {
        // Bound by its concrete type only. Application code asks for `CurriculumRepository`, which
        // `curriculumVisibilityModule` binds to the visibility decorator wrapping this.
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
