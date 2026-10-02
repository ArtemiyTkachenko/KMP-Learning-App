package org.artkachenko.kmp_learning_app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.OnBackCompletedFallback
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.curriculum.learning.content.learningContentModule
import org.artkachenko.kmp_learning_app.curriculum.visibility.curriculumVisibilityModule
import org.artkachenko.kmp_learning_app.curriculum.visibility.kmpContentPreferenceTestModule
import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.lesson_study.lessonStudyDataModule
import org.artkachenko.kmp_learning_app.data.local.saved_questions.savedQuestionDataModule
import org.artkachenko.kmp_learning_app.settings.appearanceModule
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserHeaderTag
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserSettingsTag
import org.koin.compose.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module

/**
 * Back as the platform delivers it, through the composed shell rather than through
 * `AppNavigator.popBack()`.
 *
 * `AppNavigatorTest` pins what each back step does to the stacks, and every journey presses the
 * toolbar's Back, which calls the navigator directly. Neither reaches the two handlers `AppShell`
 * composes: `NavDisplay`'s own, enabled only while the current stack has a previous entry, and the
 * outer one gated by `canLeaveArea` that returns an area root to Start. Remove or mis-gate either
 * and system Back silently stops working while every other test stays green.
 *
 * The test supplies the dispatcher a host would, and its fallback stands in for the host: it runs
 * only when no composed handler consumed the event, which is when Android would close the app.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal class AppShellBackNavigationTest {

    @Test
    fun backOnTheStartAreaRootIsLeftToTheHost() = runShellBackTest { back ->
        back.press()

        assertEquals(1, back.unconsumed)
        onNodeWithTag(TopicBrowserHeaderTag).assertIsDisplayed()
    }

    @Test
    fun backOnAnotherAreaRootReturnsToStart() = runShellBackTest { back ->
        onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.PROGRESS)).performClick()
        waitForText(ProgressEmptyText)

        back.press()

        assertEquals(0, back.unconsumed)
        waitForTag(TopicBrowserHeaderTag)
        onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.Start)).assertIsSelected()
    }

    @Test
    fun backOnADetailPopsOnlyTheDetail() = runShellBackTest { back ->
        onNodeWithTag(TopicBrowserSettingsTag).performClick()
        waitForText(SettingsSectionText)

        back.press()

        assertEquals(0, back.unconsumed)
        waitForTag(TopicBrowserHeaderTag)
        onNodeWithText(SettingsSectionText).assertDoesNotExist()
    }

    private class BackInput {
        var unconsumed = 0
        val input = DirectNavigationEventInput()
        val dispatcher = NavigationEventDispatcher(
            OnBackCompletedFallback { unconsumed++ },
        ).apply { addInput(input) }
    }

    private class Back(private val composeTest: ComposeUiTest, private val back: BackInput) {
        val unconsumed: Int get() = back.unconsumed

        fun press() {
            composeTest.runOnIdle { back.input.backCompleted() }
            composeTest.waitForIdle()
        }
    }

    private fun runShellBackTest(block: suspend ComposeUiTest.(Back) -> Unit) {
        synchronized(appIntegrationMainDispatcherLock) {
            stopKoin()
            Dispatchers.setMain(Dispatchers.Unconfined)
            var database: CurriculumDatabase? = null
            try {
                runComposeUiTest {
                    // An empty curriculum is enough: back behaviour is the shell's, and the three
                    // surfaces used here each render without content.
                    val db = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
                        .setDriver(BundledSQLiteDriver())
                        .build()
                    database = db
                    val backInput = BackInput()
                    val owner = object : NavigationEventDispatcherOwner {
                        override val navigationEventDispatcher = backInput.dispatcher
                    }

                    setContent {
                        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                            KoinApplication(
                                configuration = koinConfiguration {
                                    modules(
                                        listOf(
                                            curriculumDataModule,
                                            curriculumVisibilityModule,
                                            kmpContentPreferenceTestModule(includeKmpContent = false),
                                            learningContentModule,
                                            assessmentDataModule,
                                            savedQuestionDataModule,
                                            lessonStudyDataModule,
                                            topicStudyPresentationModule,
                                            // Settings is the detail used below; its storage is
                                            // the in-memory one the KMP test module binds.
                                            appearanceModule,
                                            module { single<CurriculumDatabase> { db } },
                                        ),
                                    )
                                },
                            ) {
                                App()
                            }
                        }
                    }
                    waitForTag(TopicBrowserHeaderTag)

                    block(Back(this, backInput))
                }
            } finally {
                stopKoin()
                database?.close()
                Dispatchers.resetMain()
            }
        }
    }

    private fun ComposeUiTest.waitForTag(tag: String) {
        waitUntil(timeoutMillis = AwaitTimeoutMillis) {
            onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun ComposeUiTest.waitForText(text: String) {
        waitUntil(timeoutMillis = AwaitTimeoutMillis) {
            onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }
}

private const val ProgressEmptyText = "start tracking your progress"
private const val SettingsSectionText = "Appearance"
private const val AwaitTimeoutMillis = 10_000L
