package org.artkachenko.kmp_learning_app.data.local.saved_questions

import org.artkachenko.kmp_learning_app.data.local.saved_questions.repository.LocalSavedQuestionRepository
import org.artkachenko.kmp_learning_app.saved_questions.repository.SavedQuestionRepository
import org.koin.dsl.module

internal val savedQuestionDataModule = module {
    single<SavedQuestionRepository> {
        LocalSavedQuestionRepository(
            database = get(),
        )
    }
}
