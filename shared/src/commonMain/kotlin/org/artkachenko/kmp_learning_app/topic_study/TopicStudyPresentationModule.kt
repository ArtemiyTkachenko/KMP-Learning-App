package org.artkachenko.kmp_learning_app.topic_study

import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserViewModel
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicDetailViewModel
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSessionLoader
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingViewModel
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.mixed_interview.MixedInterviewResultViewModel
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val topicStudyPresentationModule = module {
    single {
        AssessmentReviewLoader(
            curriculumRepository = get(),
        )
    }
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
        AssessmentTakingViewModel(
            launch = parameters.get(),
            assessmentEngine = get(),
            assessmentRepository = get(),
            assessmentSessionLoader = get<AssessmentSessionLoader>(),
        )
    }
    viewModel { parameters ->
        FocusedResultViewModel(
            attemptId = parameters.get(),
            assessmentRepository = get(),
            assessmentReviewLoader = get(),
            assessmentRetakeService = get(),
        )
    }
    viewModel { parameters ->
        MixedInterviewResultViewModel(
            attemptId = parameters.get(),
            assessmentRepository = get(),
            curriculumRepository = get(),
            assessmentReviewLoader = get(),
        )
    }
}
