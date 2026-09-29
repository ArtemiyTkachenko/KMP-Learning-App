package org.artkachenko.kmp_learning_app.curriculum.visibility

import org.artkachenko.kmp_learning_app.assessment.history.AppCoroutineScope
import org.artkachenko.kmp_learning_app.assessment.history.VisibleAssessmentHistory
import org.artkachenko.kmp_learning_app.curriculum.learning.content.BundledLearningContentRepository
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.data.local.curriculum.repository.LocalCurriculumRepository
import org.artkachenko.kmp_learning_app.settings.KmpContentPreferenceStore
import org.koin.dsl.module

/**
 * The curriculum visibility boundary: the preference, its app-scoped state, and every seam where it
 * applies.
 *
 * The data modules bind the raw sources by their concrete types only — `LocalCurriculumRepository`
 * in `curriculumDataModule`, `BundledLearningContentRepository` in `learningContentModule`, and
 * `AssessmentHistoryStore` in `assessmentDataModule`. The interfaces application code asks for,
 * [CurriculumRepository] and [LearningContentRepository], are bound only here, to the visibility
 * decorators wrapping those raw sources, so nothing can obtain unfiltered eligibility through an
 * interface by accident, and a graph without this module fails to resolve them instead of silently
 * showing hidden content.
 *
 * Like `appearanceModule`, it needs the host's `AppPreferenceStorage` and adds no platform code.
 */
internal val curriculumVisibilityModule = module {
    single { KmpContentPreferenceStore(storage = get()) }
    single {
        // App-scoped: both decorators, the history projection, the Settings switch and the screens
        // that re-read on a change must all observe one visibility, and it has to outlive every
        // destination.
        CurriculumVisibilityStateHolder(store = get())
    }
    single<CurriculumRepository> {
        VisibleCurriculumRepository(
            delegate = get<LocalCurriculumRepository>(),
            visibility = get<CurriculumVisibilityStateHolder>().visibility,
        )
    }
    single<LearningContentRepository> {
        VisibleLearningContentRepository(
            delegate = get<BundledLearningContentRepository>(),
            visibility = get<CurriculumVisibilityStateHolder>().visibility,
        )
    }
    single {
        VisibleAssessmentHistory(
            rawHistory = get(),
            // The decorated repository: its historical resolver passes through unfiltered, which
            // is what the projection needs to classify hidden Questions.
            curriculumRepository = get(),
            visibility = get<CurriculumVisibilityStateHolder>().visibility,
            scope = get<AppCoroutineScope>(),
        )
    }
}
