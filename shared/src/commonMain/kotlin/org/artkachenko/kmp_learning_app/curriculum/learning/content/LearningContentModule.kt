package org.artkachenko.kmp_learning_app.curriculum.learning.content

import org.koin.dsl.module

/**
 * Learning content is bundled publisher-owned material with no database, platform binding,
 * or startup import, so it stands apart from `curriculumDataModule` rather than joining it.
 * The repository is a `single` because its loaded document is meant to be shared.
 */
internal val learningContentModule = module {
    single {
        // Bound by its concrete type only. Application code asks for `LearningContentRepository`,
        // which `curriculumVisibilityModule` binds to the visibility decorator wrapping this.
        BundledLearningContentRepository()
    }
}
