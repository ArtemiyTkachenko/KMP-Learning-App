package org.artkachenko.kmp_learning_app

import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentStartResult
import org.artkachenko.kmp_learning_app.assessment.session.CompleteAssessment
import org.artkachenko.kmp_learning_app.curriculum.content.BundledCurriculumSource
import org.artkachenko.kmp_learning_app.curriculum.learning.content.learningContentModule
import org.artkachenko.kmp_learning_app.curriculum.visibility.curriculumVisibilityModule
import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.data.local.lesson_study.lessonStudyDataModule
import org.artkachenko.kmp_learning_app.data.local.progress_reset.progressResetDataModule
import org.artkachenko.kmp_learning_app.data.local.saved_questions.savedQuestionDataModule
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressState
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressStateHolder
import org.artkachenko.kmp_learning_app.lesson_study.repository.LessonStudyRepository
import org.artkachenko.kmp_learning_app.progress.ProgressContentTag
import org.artkachenko.kmp_learning_app.saved_questions.repository.SavedQuestionRepository
import org.artkachenko.kmp_learning_app.settings.AppPreferenceStorage
import org.artkachenko.kmp_learning_app.settings.KmpContentPreferenceStore
import org.artkachenko.kmp_learning_app.settings.SettingsResetConfirmTag
import org.artkachenko.kmp_learning_app.settings.SettingsResetDialogTag
import org.artkachenko.kmp_learning_app.settings.SettingsResetProgressTag
import org.artkachenko.kmp_learning_app.settings.ThemePreference
import org.artkachenko.kmp_learning_app.settings.ThemePreferenceStore
import org.artkachenko.kmp_learning_app.settings.appearanceModule
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserSettingsTag
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Resetting progress through the real shell and the real graph over a real database: a completed
 * practice is on Progress and in the Mistakes queue, the learner resets from Settings, and both
 * areas are empty without a restart — while the saved Question and both settings survive.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal class ProgressResetIntegrationTest {

    @Test
    fun aCompletedPracticeIsGoneFromProgressAndMistakesAfterAResetWithoutRestarting() =
        runResetIntegrationTest { koin, storage ->
            val completedAttemptId = runBlocking { completeAWrongPractice(koin) }
            val savedQuestionId = runBlocking {
                val questionId = requireNotNull(
                    koin.get<AssessmentRepository>().getById(completedAttemptId),
                ).questionAttempts.first().questionId
                koin.get<SavedQuestionRepository>().save(questionId)
                koin.get<LessonStudyRepository>().markStudied("studied_lesson")
                questionId
            }
            koin.get<StudyProgressStateHolder>().refresh()

            // Before: Progress has content and the Mistakes badge counts the wrong answers, which
            // are due because the practice was completed two days ago.
            onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.PROGRESS)).performClick()
            waitUntil(timeoutMillis = AwaitTimeoutMillis) { onAllNodesWithTag(ProgressContentTag).fetchSemanticsNodes().isNotEmpty() }
            waitUntil(timeoutMillis = AwaitTimeoutMillis) {
                onAllNodesWithTag(AppNavigationBadgeTag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }

            // Reset from Settings, opened where the learner opens it.
            onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.TOPICS)).performClick()
            waitForIdle()
            onNodeWithTag(TopicBrowserSettingsTag).performClick()
            waitForIdle()
            onNodeWithTag(SettingsResetProgressTag).performScrollTo().performClick()
            onNodeWithTag(SettingsResetConfirmTag).performClick()
            waitUntil(timeoutMillis = AwaitTimeoutMillis) {
                onAllNodesWithTag(SettingsResetDialogTag).fetchSemanticsNodes().isEmpty()
            }
            // Still on Settings.
            onNodeWithTag(SettingsResetProgressTag).assertIsDisplayed()
            waitUntil(timeoutMillis = AwaitTimeoutMillis) {
                onAllNodesWithTag(AppNavigationBadgeTag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
            }

            onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.PROGRESS)).performClick()
            waitUntil(timeoutMillis = AwaitTimeoutMillis) { onAllNodesWithText(ProgressEmpty).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText(ProgressEmpty).assertIsDisplayed()

            onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.MISTAKES)).performClick()
            waitUntil(timeoutMillis = AwaitTimeoutMillis) { onAllNodesWithText(MistakesEmpty).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText(MistakesEmpty).assertIsDisplayed()

            // Deleted: the attempt and the studied mark, with the shared study projection emptied.
            runBlocking {
                assertNull(koin.get<AssessmentRepository>().getById(completedAttemptId))
                assertEquals(emptyList(), koin.get<AssessmentRepository>().getCompletedAttempts())
                assertEquals(emptyList(), koin.get<LessonStudyRepository>().getStudiedLessons())
            }
            waitUntil(timeoutMillis = AwaitTimeoutMillis) {
                (koin.get<StudyProgressStateHolder>().state.value as? StudyProgressState.Loaded)
                    ?.studiedLessonIds?.isEmpty() == true
            }
            // Kept: the saved Question and both settings.
            runBlocking {
                assertEquals(
                    listOf(savedQuestionId),
                    koin.get<SavedQuestionRepository>().getSavedQuestions().map { it.questionId },
                )
            }
            assertEquals(ThemePreference.Dark, ThemePreferenceStore(storage).read())
            assertEquals(true, KmpContentPreferenceStore(storage).read())
        }

    /** A Mixed practice completed through the real engine, every answer wrong, so it leaves mistakes. */
    private suspend fun completeAWrongPractice(koin: Koin): String {
        val engine = koin.get<AssessmentEngine>()
        val started = assertIs<AssessmentStartResult.Started>(
            engine.start(AssessmentConfig.Mixed(questionCount = 3)),
        )
        koin.get<AssessmentRepository>().save(started.session.attempt)
        var session = started.session
        session.questions.forEach { question ->
            val wrong = question.answers.first { it.id !in question.correctAnswerIds }.id
            session = engine.submitAnswer(session, question.id, listOf(wrong))
        }
        return koin.get<CompleteAssessment>()(session).attempt.id
    }

    private fun runResetIntegrationTest(block: suspend ComposeUiTest.(Koin, AppPreferenceStorage) -> Unit) {
        synchronized(appIntegrationMainDispatcherLock) {
            stopKoin()
            Dispatchers.setMain(Dispatchers.Unconfined)
            try {
                runComposeUiTest {
                    // Not closed, for the reason SharedHostStartupTest gives.
                    val database = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
                        .setDriver(BundledSQLiteDriver())
                        .build()
                    runBlocking {
                        CurriculumImporter(database, loadCurriculum = { BundledCurriculumSource.load() })
                            .importCurriculum()
                    }
                    val storage = MapStorage()
                    ThemePreferenceStore(storage).write(ThemePreference.Dark)
                    KmpContentPreferenceStore(storage).write(true)

                    val koin = startKoin {
                        modules(
                            module {
                                single<CurriculumDatabase> { database }
                                single<AppPreferenceStorage> { storage }
                            },
                            curriculumDataModule,
                            learningContentModule,
                            assessmentDataModule,
                            savedQuestionDataModule,
                            lessonStudyDataModule,
                            progressResetDataModule,
                            topicStudyPresentationModule,
                            appearanceModule,
                            curriculumVisibilityModule,
                            module {
                                // The practice below finishes two days in the past, so its
                                // mistakes are due and the badge has something to count before
                                // the reset. A practice finished just now would be scheduled but
                                // not yet due, and the badge only counts due mistakes.
                                single {
                                    AssessmentEngine(
                                        questionSelector = get(),
                                        now = { Clock.System.now() - 2.days },
                                    )
                                }
                            },
                        )
                    }.koin

                    setContent { App() }
                    waitForIdle()

                    block(koin, storage)
                }
            } finally {
                stopKoin()
                Dispatchers.resetMain()
            }
        }
    }
}

private const val AwaitTimeoutMillis = 10_000L
private const val ProgressEmpty =
    "Complete an interview or focused practice session to start tracking your progress."
private const val MistakesEmpty = "No unresolved mistakes."

private class MapStorage : AppPreferenceStorage {
    private val values = mutableMapOf<String, String>()

    override fun read(key: String): String? = values[key]

    override fun write(key: String, value: String?) {
        if (value == null) values.remove(key) else values[key] = value
    }
}
