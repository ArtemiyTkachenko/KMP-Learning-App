package org.artkachenko.kmp_learning_app.data.local.lesson_study

import org.artkachenko.kmp_learning_app.data.local.lesson_study.repository.LocalLessonStudyRepository
import org.artkachenko.kmp_learning_app.lesson_study.repository.LessonStudyRepository
import org.koin.dsl.module

/**
 * Learner-owned study state, kept separate from `learningContentModule`, which owns the
 * publisher-authored learning document.
 */
internal val lessonStudyDataModule = module {
    single<LessonStudyRepository> {
        LocalLessonStudyRepository(
            database = get(),
        )
    }
}
