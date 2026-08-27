package org.artkachenko.kmp_learning_app.topic_study

import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val topicStudyPresentationModule = module {
    viewModel {
        TopicBrowserViewModel(
            curriculumRepository = get(),
        )
    }
}
