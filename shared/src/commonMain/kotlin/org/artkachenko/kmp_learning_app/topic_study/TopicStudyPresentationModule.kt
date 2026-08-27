package org.artkachenko.kmp_learning_app.topic_study

import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserViewModel
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicDetailViewModel
import org.artkachenko.kmp_learning_app.topic_study.focused_practice.FocusedPracticeViewModel
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val topicStudyPresentationModule = module {
    viewModel {
        TopicBrowserViewModel(
            curriculumRepository = get(),
        )
    }
    viewModel { parameters ->
        TopicDetailViewModel(
            topicId = parameters.get(),
            curriculumRepository = get(),
        )
    }
    viewModel { parameters ->
        FocusedPracticeViewModel(
            config = parameters.get(),
            assessmentEngine = get(),
            assessmentRepository = get(),
        )
    }
    viewModel { parameters ->
        FocusedResultViewModel(
            attemptId = parameters.get(),
            assessmentRepository = get(),
            curriculumRepository = get(),
        )
    }
}
