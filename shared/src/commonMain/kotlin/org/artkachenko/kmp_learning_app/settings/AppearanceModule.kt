package org.artkachenko.kmp_learning_app.settings

import org.koin.dsl.module

/**
 * The appearance preference's shared bindings.
 *
 * Every host installs this alongside its own platform module, which is the one that supplies
 * [AppPreferenceStorage] — the same split `curriculumDataModule` and the per-platform
 * `CurriculumDatabase` modules already use.
 */
internal val appearanceModule = module {
    single { ThemePreferenceStore(storage = get()) }
    single {
        // App-scoped because the theme is applied above the navigation shell: the preference has
        // to outlive the Settings entry that changes it.
        AppearanceStateHolder(store = get())
    }
}
