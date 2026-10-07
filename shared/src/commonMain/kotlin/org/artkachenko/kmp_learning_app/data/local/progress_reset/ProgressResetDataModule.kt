package org.artkachenko.kmp_learning_app.data.local.progress_reset

import org.artkachenko.kmp_learning_app.progress_reset.ProgressResetRepository
import org.artkachenko.kmp_learning_app.progress_reset.ResetLearnerProgress
import org.koin.dsl.module

/**
 * Resetting learner progress. Its own module because the delete spans two data modules' tables —
 * `assessmentDataModule`'s attempts and `lessonStudyDataModule`'s studied Lessons — in one
 * transaction, and belongs to neither.
 */
internal val progressResetDataModule = module {
    single<ProgressResetRepository> {
        LocalProgressResetRepository(
            database = get(),
        )
    }
    single {
        // Like CompleteAssessment, it owns the write and every invalidation that write requires,
        // so no caller can delete progress and forget to tell the caches.
        ResetLearnerProgress(
            repository = get(),
            historyStore = get(),
            studyProgress = get(),
        )
    }
}
