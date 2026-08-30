package org.artkachenko.kmp_learning_app.topic_study

import org.artkachenko.kmp_learning_app.AppShellViewModel
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSessionLoader
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingViewModel
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewViewModel
import org.artkachenko.kmp_learning_app.mixed_interview.InterviewStartViewModel
import org.artkachenko.kmp_learning_app.mixed_interview.MixedInterviewResultViewModel
import org.artkachenko.kmp_learning_app.progress.ProgressTopicViewModel
import org.artkachenko.kmp_learning_app.progress.ProgressViewModel
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultViewModel
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicDetailViewModel
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val topicStudyPresentationModule = module {
    single {
        AssessmentReviewLoader(
            curriculumRepository = get(),
        )
    }
    single {
        // AssessmentReviewLoader is already registered here, so the mistake queue joins the same
        // module rather than introducing another one or moving the loader across a boundary.
        MistakeReviewService(
            assessmentRepository = get(),
            assessmentReviewLoader = get(),
        )
    }
    viewModel {
        AppShellViewModel(
            mistakeReviewService = get(),
        )
    }
    viewModel {
        MistakeReviewViewModel(
            mistakeReviewService = get(),
        )
    }
    viewModel {
        InterviewStartViewModel(
            assessmentRepository = get(),
        )
    }
    viewModel {
        TopicBrowserViewModel(
            curriculumRepository = get(),
        )
    }
    viewModel {
        ProgressViewModel(
            learningProgressService = get(),
            assessmentRepository = get(),
            curriculumRepository = get(),
            mistakeReviewService = get(),
        )
    }
    viewModel { parameters ->
        ProgressTopicViewModel(
            topicId = parameters.get(),
            learningProgressService = get(),
        )
    }
    viewModel { parameters ->
        TopicDetailViewModel(
            topicId = parameters.get(),
            curriculumRepository = get(),
            learningProgressService = get(),
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
            assessmentRetakeService = get(),
        )
    }
}
