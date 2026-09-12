package org.artkachenko.kmp_learning_app.topic_study

import org.artkachenko.kmp_learning_app.AppShellViewModel
import org.artkachenko.kmp_learning_app.assessment.history.AppCoroutineScope
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewStateHolder
import org.artkachenko.kmp_learning_app.mixed_interview.InterviewHistoryStateHolder
import org.artkachenko.kmp_learning_app.progress.ProgressStateHolder
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSessionLoader
import org.artkachenko.kmp_learning_app.assessment.start.AssessmentLaunchViewModel
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingViewModel
import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingResolver
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressStateHolder
import org.artkachenko.kmp_learning_app.guided_learning.LearningRecommendationResolver
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewViewModel
import org.artkachenko.kmp_learning_app.mixed_interview.InterviewStartViewModel
import org.artkachenko.kmp_learning_app.mixed_interview.MixedInterviewResultViewModel
import org.artkachenko.kmp_learning_app.progress.ProgressTopicViewModel
import org.artkachenko.kmp_learning_app.progress.ProgressViewModel
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionContentResolver
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionStateHolder
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionsViewModel
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultViewModel
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonViewModel
import org.artkachenko.kmp_learning_app.topic_study.learning_unit.LearningUnitViewModel
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderViewModel
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeTargetResolver
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
            learningContentRepository = get(),
        )
    }
    single {
        // One holder for the whole app, alongside the other app-scoped state: the three review
        // surfaces present the same saved identities, so they must observe the same state rather
        // than each caching the saved table for itself.
        SavedQuestionStateHolder(
            repository = get(),
            scope = get<AppCoroutineScope>(),
        )
    }
    single {
        // Content resolution only. Which Questions are saved stays the holder's answer above, and
        // this resolves each of those identities through the historical stable-ID lookup.
        SavedQuestionContentResolver(
            curriculumRepository = get(),
        )
    }
    single {
        // App-scoped for the reason the Learn stack demands it: Topic Detail, the Unit overview,
        // and the Lesson reader are alive at the same time and show the same learner-owned truth,
        // so marking a Lesson in the reader has to be what the two screens underneath show on the
        // way back. It lives here rather than in `lessonStudyDataModule`, which owns only the
        // Room-backed repository, and not in `learningContentModule`, which owns publisher content.
        StudyProgressStateHolder(
            repository = get(),
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
        AssessmentLaunchViewModel(startAssessment = get())
    }
    viewModel {
        MistakeReviewViewModel(
            historyStore = get(),
            stateHolder = get(),
            savedQuestionStateHolder = get(),
        )
    }
    viewModel {
        // The same app-scoped holder the three review surfaces observe, so browsing shows what
        // they saved and removing here is what they see next. No second read of the saved table.
        SavedQuestionsViewModel(
            savedQuestionStateHolder = get(),
            contentResolver = get(),
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
            // The E20 singleton from `learningContentModule`, through its interface: learning
            // availability is optional enrichment here, so presentation consumes the same
            // repository contract every other Learn surface will rather than reading the bundled
            // document for itself.
            learningContentRepository = get(),
            learningProgressService = get(),
            historyStore = get(),
            continueStudyingResolver = get(),
            learningRecommendationResolver = get(),
            // The same app-scoped projection the three Learn destinations observe. Continue Learning
            // has to agree with the studied indicators shown deeper in the stack, and this screen is
            // usually still alive beneath them when a Lesson is marked.
            studyProgressStateHolder = get(),
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
            // The same E20 singleton the Topic Browser resolves, through its interface: the
            // availability marker on the catalogue row and the Units listed here must come from
            // one validated document rather than from two reads that could disagree.
            learningContentRepository = get(),
            learningProgressService = get(),
            historyStore = get(),
            studyProgressStateHolder = get(),
        )
    }
    viewModel { parameters ->
        // The same E20 singleton every other Learn surface resolves. Learning content is this
        // destination's primary capability rather than enrichment, so an unreadable document is a
        // screen-level error here — but it is still the one validated document, not a second read.
        LearningUnitViewModel(
            unitId = parameters.get(),
            learningContentRepository = get(),
            // The same app-scoped holder the reader below mutates, so a Lesson marked one level
            // deeper reaches this overview without it being rebuilt.
            studyProgressStateHolder = get(),
        )
    }
    viewModel { parameters ->
        // Read by position rather than by type: both identities are Strings, so a type-based
        // lookup cannot say which is which, and silently resolving both to the Unit ID would make
        // every Lesson unavailable. The destination passes the Unit first, as the route declares.
        LearningLessonViewModel(
            unitId = parameters.get(0),
            lessonId = parameters.get(1),
            learningContentRepository = get(),
            studyProgressStateHolder = get(),
        )
    }
    single {
        // The one crossing from learning content into assessment configuration, and the only place
        // a Learning Unit becomes a set of Subtopic IDs. It is a `single` beside the other
        // resolvers because it holds no state: both repositories it reads are already app-scoped.
        PracticeTargetResolver(
            curriculumRepository = get(),
            learningContentRepository = get(),
        )
    }
    viewModel { parameters ->
        // The selection boundary, not the engine: the builder reads eligibility before starting
        // practice and must never create an attempt to find out whether one is possible.
        PracticeBuilderViewModel(
            target = parameters.get(),
            targetResolver = get(),
            questionSelector = get(),
            // Optional: opening the builder from content passes a scope alone and keeps the
            // builder's own ALL default, while a preset-carrying entry supplies the source.
            initialSource = parameters.getOrNull() ?: PracticeQuestionSource.ALL,
        )
    }
    viewModel { parameters ->
        AssessmentTakingViewModel(
            attemptId = parameters.get(),
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
            savedQuestionStateHolder = get(),
        )
    }
    viewModel { parameters ->
        MixedInterviewResultViewModel(
            attemptId = parameters.get(),
            assessmentRepository = get(),
            curriculumRepository = get(),
            assessmentReviewLoader = get(),
            assessmentRetakeService = get(),
            savedQuestionStateHolder = get(),
        )
    }
}
