package org.artkachenko.kmp_learning_app.curriculum.learning.content

import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository
import org.koin.dsl.module

/**
 * Learning content is bundled publisher-owned material with no database, platform binding,
 * or startup import, so it stands apart from `curriculumDataModule` rather than joining it.
 * The repository is a `single` because its loaded document is meant to be shared.
 */
internal val learningContentModule = module {
    single<LearningContentRepository> {
        BundledLearningContentRepository()
    }
}
