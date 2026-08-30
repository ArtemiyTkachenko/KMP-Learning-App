package org.artkachenko.kmp_learning_app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeService
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSessionLoader
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDataInitializer
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewViewModel
import org.artkachenko.kmp_learning_app.mixed_interview.MixedInterviewResultViewModel
import org.artkachenko.kmp_learning_app.progress.ProgressTopicViewModel
import org.artkachenko.kmp_learning_app.progress.ProgressViewModel
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultViewModel
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicDetailViewModel
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserViewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * Every runtime host installs the same three shared modules plus exactly one platform
 * `CurriculumDatabase` module, then composes [AppRoot]. These tests pin that contract
 * without needing a device, simulator, or browser: if a shared module stops providing
 * something the product graph needs, every host would fail at its first ViewModel
 * resolution, and this fails first instead.
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
            modules(
                // The only binding a platform host adds on top of the shared modules.
                module { single<CurriculumDatabase> { database } },
                curriculumDataModule,
                assessmentDataModule,
                topicStudyPresentationModule,
            )
        }

        try {
            val koin = app.koin

            assertIs<CurriculumImporter>(koin.get<CurriculumImporter>())
            assertIs<CurriculumDataInitializer>(koin.get<CurriculumDataInitializer>())
            assertIs<CurriculumRepository>(koin.get<CurriculumRepository>())
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
            assertIs<FocusedResultViewModel>(
                koin.get<FocusedResultViewModel> { parametersOf("attempt") },
            )
            assertIs<MixedInterviewResultViewModel>(
                koin.get<MixedInterviewResultViewModel> { parametersOf("attempt") },
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
                        modules(
                            module { single<CurriculumDatabase> { created } },
                            curriculumDataModule,
                            assessmentDataModule,
                            topicStudyPresentationModule,
                        )
                    }.koin

                    setContent {
                        AppRoot { koin.get<CurriculumDataInitializer>().initialize() }
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
