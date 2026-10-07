package org.artkachenko.kmp_learning_app

import org.artkachenko.kmp_learning_app.progress_reset.ResetLearnerProgress
import org.artkachenko.kmp_learning_app.settings.ProgressResetViewModel
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeService
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSessionLoader
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDataInitializer
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressStateHolder
import org.artkachenko.kmp_learning_app.lesson_study.repository.LessonStudyRepository
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewViewModel
import org.artkachenko.kmp_learning_app.mixed_interview.InterviewStartViewModel
import org.artkachenko.kmp_learning_app.mixed_interview.MixedInterviewResultViewModel
import org.artkachenko.kmp_learning_app.progress.ProgressTopicViewModel
import org.artkachenko.kmp_learning_app.progress.ProgressViewModel
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionContentResolver
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionStateHolder
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionsViewModel
import org.artkachenko.kmp_learning_app.saved_questions.repository.SavedQuestionRepository
import org.artkachenko.kmp_learning_app.settings.AppPreferenceStorage
import org.artkachenko.kmp_learning_app.settings.AppearanceStateHolder
import org.artkachenko.kmp_learning_app.settings.ThemePreferenceStore
import org.artkachenko.kmp_learning_app.settings.jvmAppearanceModule
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultViewModel
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonViewModel
import org.artkachenko.kmp_learning_app.topic_study.learning_unit.LearningUnitViewModel
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderTarget
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderViewModel
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicDetailViewModel
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserViewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibleCurriculumRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibleLearningContentRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibilityStateHolder
import org.artkachenko.kmp_learning_app.assessment.history.VisibleAssessmentHistory

/**
 * Every runtime host installs [sharedApplicationModules] plus exactly two platform bindings — one
 * `CurriculumDatabase` and one `AppPreferenceStorage` — under strict override, then composes
 * [AppRoot]. These tests build the graph the same way, with an in-memory database and the JVM
 * preference module standing in for the platform half, so they pin that contract without needing
 * a device, simulator, or browser: if a shared module stops providing something the product graph
 * needs, or two modules define the same type, every host would fail, and this fails first instead.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal class SharedHostStartupTest {
    @Test
    fun sharedHostModulesResolveTheWholeProductGraph() {
        // Koin resolution is synchronous, so this needs no test scheduler.
        //
        // Main is set to Unconfined and deliberately NOT reset afterwards. Resolving these
        // ViewModels starts viewModelScope work that resumes on Room's executor, so tearing
        // Main down here would make those late resumptions throw
        // "Dispatchers.Main was accessed ... after Dispatchers.resetMain()" into the global
        // handler, which then fails whichever unrelated test runs next. Later test classes set
        // their own Main, so leaving it as Unconfined is harmless.
        Dispatchers.setMain(Dispatchers.Unconfined)
        val database = inMemoryDatabase()
        val app = koinApplication {
            // As every host does, so a duplicate definition fails here instead of replacing one.
            strictOverride()
            modules(sharedApplicationModules())
            // The two platform bindings: the database and the preference store.
            modules(module { single<CurriculumDatabase> { database } }, jvmAppearanceModule)
        }

        try {
            val koin = app.koin

            assertIs<CurriculumImporter>(koin.get<CurriculumImporter>())
            assertIs<CurriculumDataInitializer>(koin.get<CurriculumDataInitializer>())
            // The interfaces resolve to the visibility decorators, never to the raw sources.
            assertIs<VisibleCurriculumRepository>(koin.get<CurriculumRepository>())
            assertIs<VisibleLearningContentRepository>(koin.get<LearningContentRepository>())
            assertIs<CurriculumVisibilityStateHolder>(koin.get<CurriculumVisibilityStateHolder>())
            assertIs<VisibleAssessmentHistory>(koin.get<VisibleAssessmentHistory>())
            assertIs<AssessmentRepository>(koin.get<AssessmentRepository>())
            assertIs<AssessmentQuestionSelector>(koin.get<AssessmentQuestionSelector>())
            assertIs<AssessmentEngine>(koin.get<AssessmentEngine>())
            assertIs<AssessmentSessionLoader>(koin.get<AssessmentSessionLoader>())
            assertIs<AssessmentRetakeService>(koin.get<AssessmentRetakeService>())
            assertIs<AssessmentReviewLoader>(koin.get<AssessmentReviewLoader>())
            assertIs<LearningProgressService>(koin.get<LearningProgressService>())
            assertIs<MistakeReviewService>(koin.get<MistakeReviewService>())

            assertIs<TopicBrowserViewModel>(koin.get<TopicBrowserViewModel>())
            assertIs<TopicDetailViewModel>(koin.get<TopicDetailViewModel> { parametersOf("topic") })
            assertIs<ProgressViewModel>(koin.get<ProgressViewModel>())
            assertIs<ProgressTopicViewModel>(
                koin.get<ProgressTopicViewModel> { parametersOf("topic") },
            )
            assertIs<MistakeReviewViewModel>(koin.get<MistakeReviewViewModel>())
            // One completed-history cache. Every history-derived surface in the app — this badge,
            // Progress, the mistake queue, the interview record, and the Practice Builder's
            // preflight through AssessmentQuestionSelector — reads this one instance, so it must
            // stay a `single`: as a `factory` each would cache its own history, and they would
            // drift apart after an attempt completes. A second definition of it is caught
            // separately, by strict override when the graph is built.
            assertEquals(
                koin.get<AssessmentHistoryStore>(),
                koin.get<AssessmentHistoryStore>(),
            )
            // The shell badge and the interview record are the two ViewModels no other graph check
            // resolves; both derive from the history cache above rather than reading it again.
            assertIs<AppShellViewModel>(koin.get<AppShellViewModel>())
            assertIs<InterviewStartViewModel>(koin.get<InterviewStartViewModel>())
            // The Practice Builder now stands between choosing a scope and taking an assessment,
            // so every targeted practice run starts here. It is safe to resolve where assessment
            // taking is not: its eligibility read goes to the selection boundary, which reads
            // content rather than creating an attempt.
            assertIs<PracticeBuilderViewModel>(
                koin.get<PracticeBuilderViewModel> {
                    parametersOf(PracticeBuilderTarget.Topic("topic"))
                },
            )
            assertIs<FocusedResultViewModel>(
                koin.get<FocusedResultViewModel> { parametersOf("attempt") },
            )
            assertIs<MixedInterviewResultViewModel>(
                koin.get<MixedInterviewResultViewModel> { parametersOf("attempt") },
            )
            // Saved Questions spans both shared modules: the repository comes from the data module
            // and everything above it from the presentation module. Resolving the browsing
            // destination's ViewModel is what proves a host gets that whole chain rather than the
            // review surfaces alone.
            assertIs<SavedQuestionRepository>(koin.get<SavedQuestionRepository>())
            // Learner-owned Lesson study state is bound in its own data module rather than in
            // learningContentModule, which owns the publisher document, so a host that installed
            // only the content module would fail here.
            assertIs<LessonStudyRepository>(koin.get<LessonStudyRepository>())
            assertIs<SavedQuestionContentResolver>(koin.get<SavedQuestionContentResolver>())
            assertIs<SavedQuestionsViewModel>(koin.get<SavedQuestionsViewModel>())
            // An app-scoped holder: the review surfaces and the browser share saved state by
            // sharing this instance, so a `factory` here would silently break that.
            assertEquals(
                koin.get<SavedQuestionStateHolder>(),
                koin.get<SavedQuestionStateHolder>(),
            )
            // Study state spans both shared modules the same way: the repository comes from
            // `lessonStudyDataModule` and the app-scoped projection from the presentation module.
            // A `single`, because the entire reason it exists is that the Lesson reader, the Unit
            // overview, and Topic Detail are alive at once and must agree.
            assertEquals(
                koin.get<StudyProgressStateHolder>(),
                koin.get<StudyProgressStateHolder>(),
            )
            // The chain the three Learn ViewModels resolve through, end to end.
            assertIs<LearningUnitViewModel>(
                koin.get<LearningUnitViewModel> { parametersOf("unit") },
            )
            assertIs<LearningLessonViewModel>(
                koin.get<LearningLessonViewModel> { parametersOf("unit", "lesson") },
            )
            // Resetting progress spans tables of two data modules and invalidates holders from the
            // presentation module, so it is resolved here end to end, through Settings' ViewModel.
            assertIs<ResetLearnerProgress>(koin.get<ResetLearnerProgress>())
            assertIs<ProgressResetViewModel>(koin.get<ProgressResetViewModel>())
            // The appearance preference spans the same two-module split: the platform key-value
            // store comes from the host's module and everything above it from the shared one.
            assertIs<AppPreferenceStorage>(koin.get<AppPreferenceStorage>())
            assertIs<ThemePreferenceStore>(koin.get<ThemePreferenceStore>())
            // A `single`, because it is the application's theme: a second instance would mean the
            // startup screens and the shell could disagree about light or dark.
            assertEquals(
                koin.get<AppearanceStateHolder>(),
                koin.get<AppearanceStateHolder>(),
            )

            // AssessmentTakingViewModel is deliberately not resolved here: it starts a real
            // assessment from its initializer, which needs seeded curriculum content rather
            // than the empty database this graph check uses. TopicStudyPresentationModuleTest
            // covers it with fakes shaped for that.
        } finally {
            // The database is intentionally left open. Room runs queries on its own executor,
            // so closing here can pull the database out from under in-flight ViewModel work and
            // throw into the global handler, failing whichever test runs next. An in-memory
            // database is released with the JVM anyway.
            app.close()
        }
    }

    @Test
    fun appRootEntersTopicBrowserAfterInitializationSucceeds() {
        // Mirrors the host lifecycle: start Koin, then compose AppRoot with the host's
        // initializer. Hosts resolve ViewModels through the global Koin the same way.
        synchronized(appIntegrationMainDispatcherLock) {
            stopKoin()
            Dispatchers.setMain(Dispatchers.Unconfined)
            try {
                runComposeUiTest {
                    // Deliberately not closed: ViewModel coroutines from the disposed
                    // composition can still be settling, and closing the database under them
                    // throws into the global handler, which fails the next test to run.
                    // An in-memory database is released with the JVM anyway.
                    val created = inMemoryDatabase()
                    val koin = startKoin {
                        strictOverride()
                        modules(sharedApplicationModules())
                        modules(
                            module { single<CurriculumDatabase> { created } },
                            jvmAppearanceModule,
                        )
                    }.koin

                    setContent {
                        AppRoot(koin.get<CurriculumDataInitializer>())
                    }

                    // Ready replaces the startup UI with the real App graph, whose Topic
                    // Browser resolves its ViewModel through Koin.
                    waitUntil(timeoutMillis = 30_000) {
                        onAllNodesWithText("Pick a topic to study or practise.")
                            .fetchSemanticsNodes()
                            .isNotEmpty()
                    }
                    // The navigation bar is part of the shell a host reaches on startup.
                    onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.TOPICS))
                        .assertIsDisplayed()

                    // Topics come from the bundled curriculum imported by the same initializer
                    // every host runs, so a host reaching Ready reaches real content.
                    waitUntil(timeoutMillis = 30_000) {
                        onAllNodesWithText("Android Platform & Application Model")
                            .fetchSemanticsNodes()
                            .isNotEmpty()
                    }
                    onNodeWithText("Android Platform & Application Model").assertIsDisplayed()
                }
            } finally {
                stopKoin()
                Dispatchers.resetMain()
            }
        }
    }
}

private fun inMemoryDatabase(): CurriculumDatabase =
    Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
