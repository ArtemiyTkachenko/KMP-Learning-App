package org.artkachenko.kmp_learning_app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.learning.content.learningContentModule
import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImportResult
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.data.local.lesson_study.lessonStudyDataModule
import org.artkachenko.kmp_learning_app.data.local.saved_questions.savedQuestionDataModule
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonNextTag
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonPreviousTag
import org.artkachenko.kmp_learning_app.topic_study.learning_unit.learningLessonRowTag
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserSearchFieldTag
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicStudyListTag
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.learningUnitCardTag
import org.koin.compose.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module

/**
 * Reading the shipped Thinking in Compose Unit through the whole app.
 *
 * The learning half of the shell has no fixture standing in for it: the Units, Lessons, prose,
 * code, tables, callouts, and Sources here are the ones that ship, loaded through the app's own
 * Koin wiring. Only the assessment catalogue is a fixture, and only so the Topic exists to be
 * opened — this journey never starts practice.
 *
 * What it proves that the ViewModel and screen suites cannot: that reading on *replaces* the
 * Lesson entry. Back after several Next presses has to return to the Unit overview, and only the
 * real back stack under the real `App()` can say whether it does.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal class LearningReaderJourneyIntegrationTest {
    @Test
    fun theShippedComposeUnitIsReadEndToEndAndBackLeavesTheReaderForItsUnit() = runReaderTest {
        // The study half of the Topic, read from the bundled learning document.
        openTopicFromBrowser()
        waitForTag(learningUnitCardTag(ComposeUnitId))
        onNodeWithText(ComposeUnitTitle).assertIsDisplayed()
        onNodeWithTag(learningUnitCardTag(ComposeUnitId)).performClick()

        waitForTag(learningLessonRowTag(FirstLessonId))
        onNodeWithTag(learningLessonRowTag(FirstLessonId)).performClick()

        // The first shipped Lesson, rendered as a structured document rather than a title card:
        // its depth layers, a semantic callout, a real Kotlin example, and its Sources.
        waitForText(FirstLessonTitle)
        onNodeWithText("Core").assertIsDisplayed()
        onNodeWithText("Common mistake").performScrollTo().assertIsDisplayed()
        onNodeWithText("Practical").performScrollTo().assertIsDisplayed()
        // The authored language label above a shipped Kotlin example. The code itself sits in the
        // block's own horizontal scroll, which is what keeps a long source line from widening the
        // page — the screen suite asserts that containment directly.
        onAllNodesWithText("kotlin")[0].performScrollTo().assertIsDisplayed()
        onNodeWithText("Sources").performScrollTo().assertIsDisplayed()
        onNode(hasText(FirstLessonSourceTitle) and hasClickAction())
            .performScrollTo()
            .assertIsDisplayed()
        // Nothing precedes the first Lesson, so no control claims otherwise.
        onNodeWithTag(LearningLessonPreviousTag).assertDoesNotExist()

        onNodeWithTag(LearningLessonNextTag).performScrollTo().performClick()
        waitForText(SecondLessonTitle)
        // An authored subheading the second Lesson carries and the first does not, so this is the
        // new Lesson's body rather than the previous one still on screen.
        onNodeWithText(SecondLessonSectionTitle).performScrollTo().assertIsDisplayed()

        onNodeWithTag(LearningLessonNextTag).performScrollTo().performClick()
        waitForText(ThirdLessonTitle)
        // The last Lesson: Previous still leads back through the Unit, Next has nowhere to go.
        onNodeWithTag(LearningLessonPreviousTag).performScrollTo().assertIsDisplayed()
        onNodeWithTag(LearningLessonNextTag).assertDoesNotExist()

        // Previous is a sibling move, not a pop.
        onNodeWithTag(LearningLessonPreviousTag).performScrollTo().performClick()
        waitForText(SecondLessonTitle)

        // The point of replacing rather than pushing: after four sibling moves, one Back press
        // still leaves the reader for the Unit overview instead of walking back through Lessons.
        onNodeWithContentDescription("Back").performClick()
        waitForText(ComposeUnitTitle)
        onNodeWithTag(learningLessonRowTag(FirstLessonId)).assertIsDisplayed()
    }

    /**
     * Learn -> the Topic, confirming arrival rather than assuming it.
     *
     * The Topic's name appears on the browser row and again in Topic Detail's top bar, so the search
     * field — which only the browser has — is what says which screen is being looked at. The browser
     * also rebuilds its rows as learning context resolves underneath them, and a click into a row
     * being replaced is lost; a learner who stayed put would tap again, so this does too.
     */
    private suspend fun ComposeUiTest.openTopicFromBrowser() {
        waitForTag(TopicBrowserSearchFieldTag)
        waitForText(UiTopicName)
        repeat(NavigationAttempts) {
            if (onAllNodesWithTag(TopicStudyListTag, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            ) {
                return
            }
            onNodeWithText(UiTopicName).performClick()
            waitForIdle()
        }
        waitForTag(TopicStudyListTag)
    }

    private suspend fun ComposeUiTest.waitForText(text: String) {
        waitUntil(timeoutMillis = ReaderWaitTimeoutMillis) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private suspend fun ComposeUiTest.waitForTag(tag: String) {
        waitUntil(timeoutMillis = ReaderWaitTimeoutMillis) {
            onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Boots the real app over an in-memory catalogue at a phone-shaped window.
     *
     * The window is deliberately narrow: a Lesson carries code lines and a five-column comparison
     * far wider than a phone, so if either widened the page instead of scrolling inside itself,
     * the controls this journey clicks would be off screen.
     */
    private fun runReaderTest(block: suspend ComposeUiTest.() -> Unit) {
        synchronized(appIntegrationMainDispatcherLock) {
            stopKoin()
            Dispatchers.setMain(Dispatchers.Unconfined)
            var database: CurriculumDatabase? = null
            try {
                runSkikoComposeUiTest(size = Size(WindowWidth.value, WindowHeight.value)) {
                    val db = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
                        .setDriver(BundledSQLiteDriver())
                        .build()
                    database = db
                    assertIs<CurriculumImportResult.Imported>(
                        CurriculumImporter(db, loadCurriculum = { readerCurriculum() })
                            .importCurriculum(),
                    )

                    setContent {
                        MaterialTheme {
                            KoinApplication(
                                configuration = koinConfiguration {
                                    modules(
                                        listOf(
                                            curriculumDataModule,
                                            learningContentModule,
                                            assessmentDataModule,
                                            savedQuestionDataModule,
                                            lessonStudyDataModule,
                                            topicStudyPresentationModule,
                                            module { single<CurriculumDatabase> { db } },
                                        ),
                                    )
                                },
                            ) {
                                Box(Modifier.size(WindowWidth, WindowHeight)) { App() }
                            }
                        }
                    }

                    block()
                }
            } finally {
                stopKoin()
                database?.close()
                Dispatchers.resetMain()
            }
        }
    }
}

private const val ReaderWaitTimeoutMillis = 5_000L

/** How many times the journey re-taps a Topic row that did not navigate. */
private const val NavigationAttempts = 3
private val WindowWidth: Dp = 400.dp
private val WindowHeight: Dp = 900.dp

private const val UiTopicId = "android_ui"
private const val UiTopicName = "UI — Views & Jetpack Compose"

/**
 * Everything below is the shipped learning document, not a fixture. These tests boot the real
 * learning-content module, so retiring a Lesson or renaming a Section here is a content change
 * that should fail this journey rather than pass it silently.
 */
private const val ComposeUnitId = "unit_thinking_in_compose"
private const val ComposeUnitTitle = "Thinking in Compose"
private const val FirstLessonId = "lesson_declarative_ui"
private const val FirstLessonTitle = "Declarative UI and Why Compose Exists"
private const val FirstLessonSourceTitle = "Thinking in Compose"
private const val SecondLessonTitle = "What a Composable Is and How It Executes"
private const val SecondLessonSectionTitle = "The execution contract"
private const val ThirdLessonTitle = "State Down, Events Up"

/** Only enough catalogue for the Topic to exist and be opened; this journey never practises. */
private fun readerCurriculum(): Curriculum =
    Curriculum(
        topics = listOf(Topic(UiTopicId, UiTopicName)),
        subtopics = listOf(Subtopic("compose_state", UiTopicId, "Compose snapshot state")),
        questions = listOf(
            Question(
                id = "question_reader",
                topicId = UiTopicId,
                subtopicId = "compose_state",
                text = "A question the reading journey never opens.",
                answers = listOf(AnswerOption("a", "A"), AnswerOption("b", "B")),
                selectionMode = AnswerSelectionMode.SINGLE,
                level = QuestionLevel.FOUNDATION,
                correctAnswerIds = listOf("a"),
                explanation = "Explanation.",
                sources = listOf(SourceReference("Source", "https://example.com/reader")),
            ),
        ),
    )
