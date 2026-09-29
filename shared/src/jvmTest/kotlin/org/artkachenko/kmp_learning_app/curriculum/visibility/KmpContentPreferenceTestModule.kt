package org.artkachenko.kmp_learning_app.curriculum.visibility

import org.artkachenko.kmp_learning_app.settings.AppPreferenceStorage
import org.artkachenko.kmp_learning_app.settings.InMemoryAppPreferenceStorage
import org.artkachenko.kmp_learning_app.settings.KmpContentPreferenceStore
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The host's `AppPreferenceStorage` for a test graph that installs `curriculumVisibilityModule`
 * without a platform preference module, pre-set to an explicit KMP content choice.
 *
 * Explicit rather than defaulted, so each graph states which curriculum it runs against: the
 * production default (`false`), or the full curriculum for a test whose subject is KMP content.
 */
internal fun kmpContentPreferenceTestModule(includeKmpContent: Boolean): Module = module {
    single<AppPreferenceStorage> {
        InMemoryAppPreferenceStorage().also { KmpContentPreferenceStore(it).write(includeKmpContent) }
    }
}

/**
 * A production [CurriculumVisibilityStateHolder] over in-memory storage, starting from an explicit
 * choice, for a ViewModel test that needs the holder without a Koin graph.
 */
internal fun curriculumVisibilityStateHolder(
    includeKmpContent: Boolean = false,
): CurriculumVisibilityStateHolder =
    CurriculumVisibilityStateHolder(
        KmpContentPreferenceStore(
            InMemoryAppPreferenceStorage().also { KmpContentPreferenceStore(it).write(includeKmpContent) },
        ),
    )
