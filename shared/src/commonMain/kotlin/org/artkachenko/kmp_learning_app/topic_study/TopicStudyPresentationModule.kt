package org.artkachenko.kmp_learning_app.topic_study

import org.artkachenko.kmp_learning_app.AppShellViewModel
import org.artkachenko.kmp_learning_app.assessment.history.AppCoroutineScope
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewStateHolder
import org.artkachenko.kmp_learning_app.mixed_interview.InterviewHistoryStateHolder
import org.artkachenko.kmp_learning_app.progress.ProgressStateHolder
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSessionLoader
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingViewModel
import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingResolver
import org.artkachenko.kmp_learning_app.guided_learning.LearningRecommendationResolver
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewViewModel
import org.artkachenko.kmp_learning_app.mixed_interview.InterviewStartViewModel
import org.artkachenko.kmp_learning_app.mixed_interview.MixedInterviewResultViewModel
import org.artkachenko.kmp_learning_app.progress.ProgressTopicViewModel
import org.artkachenko.kmp_learning_app.progress.ProgressViewModel
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultViewModel
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderViewModel
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
    single {
        ContinueStudyingResolver(
            curriculumRepository = get(),
        )
    }
    single {
        // The count only, taken from the same completed history the caller already holds: the
        // recommendation never loads the mistake queue's review content to find out how many
        // Questions are unresolved.
        val mistakeReviewService = get<MistakeReviewService>()
        LearningRecommendationResolver(
            unresolvedMistakeCounter = { completedAttempts ->
                mistakeReviewService.countUnresolved(completedAttempts)
            },
        )
    }
    single {
        InterviewHistoryStateHolder(
            historyStore = get(),
            scope = get<AppCoroutineScope>(),
        )
    }
    single {
        MistakeReviewStateHolder(
            mistakeReviewService = get(),
            historyStore = get(),
            scope = get<AppCoroutineScope>(),
        )
    }
    single {
        ProgressStateHolder(
            learningProgressService = get(),
            curriculumRepository = get(),
            mistakeReviewService = get(),
            historyStore = get(),
            scope = get<AppCoroutineScope>(),
        )
    }
    viewModel {
        AppShellViewModel(
            mistakeReviewService = get(),
            historyStore = get(),
        )
    }
    viewModel {
        MistakeReviewViewModel(
            historyStore = get(),
            stateHolder = get(),
        )
    }
    viewModel {
        InterviewStartViewModel(
            stateHolder = get(),
        )
    }
    viewModel {
        // The shared history cache, not another read of its own: Topic learning context, the
        // recommendation, and the continue shortcut all refresh from the same invalidation as
        // Progress and the mistake queue.
        TopicBrowserViewModel(
            curriculumRepository = get(),
            learningProgressService = get(),
            historyStore = get(),
            continueStudyingResolver = get(),
            learningRecommendationResolver = get(),
        )
    }
    viewModel {
        ProgressViewModel(
            historyStore = get(),
            stateHolder = get(),
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
            historyStore = get(),
        )
    }
    viewModel { parameters ->
        // The selection boundary, not the engine: the builder reads eligibility before starting
        // practice and must never create an attempt to find out whether one is possible.
        PracticeBuilderViewModel(
            scope = parameters.get(),
            curriculumRepository = get(),
            questionSelector = get(),
            // Optional: opening the builder from content passes a scope alone and keeps the
            // builder's own ALL default, while a preset-carrying entry supplies the source.
            initialSource = parameters.getOrNull() ?: PracticeQuestionSource.ALL,
        )
    }
    viewModel { parameters ->
        AssessmentTakingViewModel(
            launch = parameters.get(),
            assessmentEngine = get(),
            assessmentRepository = get(),
            assessmentSessionLoader = get<AssessmentSessionLoader>(),
            historyStore = get(),
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
