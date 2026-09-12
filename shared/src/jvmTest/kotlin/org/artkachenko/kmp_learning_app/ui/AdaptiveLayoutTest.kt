package org.artkachenko.kmp_learning_app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.AppNavigationScaffold
import org.artkachenko.kmp_learning_app.AppTopLevelDestination
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningBlock
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningDepth
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningSection
import org.artkachenko.kmp_learning_app.progress.ProgressActionPaneTag
import org.artkachenko.kmp_learning_app.progress.ProgressContentTag
import org.artkachenko.kmp_learning_app.progress.ProgressCoverageUiModel
import org.artkachenko.kmp_learning_app.progress.ProgressScreen
import org.artkachenko.kmp_learning_app.progress.ProgressStandingPaneTag
import org.artkachenko.kmp_learning_app.progress.ProgressTopicUiModel
import org.artkachenko.kmp_learning_app.progress.ProgressUiState
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonOutlineTag
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonReadingColumnTag
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonScreen
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonUiState
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserNoResultsTag
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserScreen
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserUiState
import org.artkachenko.kmp_learning_app.ui.theme.AppLayout
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme
import org.artkachenko.kmp_learning_app.ui.theme.AppWindowSizeClass
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppWindowSizeClass

/**
 * The adaptive-layout rules P2 introduced: one window-class definition, and screens that compose
 * differently at each class without changing what they say.
 *
 * Except where the shell itself is under test, the window class is supplied directly rather than by
 * sizing a window. That is the contract these screens actually have — they read
 * [LocalAppWindowSizeClass] and never measure — so a test that sized a window would be asserting
 * the shell's behaviour again in every screen's file.
 */
@OptIn(ExperimentalTestApi::class)
internal class AdaptiveLayoutTest {

    @Test
    fun theWindowClassBoundariesAreTheOnesTheLayoutRulesAreWrittenAgainst() {
        assertEquals(AppWindowSizeClass.Compact, AppLayout.windowSizeClassFor(0.dp))
        assertEquals(AppWindowSizeClass.Compact, AppLayout.windowSizeClassFor(599.dp))
        // Both boundaries are inclusive at the lower edge, which is what Material states and what
        // the navigation rail has always used for the first of them.
        assertEquals(AppWindowSizeClass.Medium, AppLayout.windowSizeClassFor(600.dp))
        assertEquals(AppWindowSizeClass.Medium, AppLayout.windowSizeClassFor(1039.dp))
        assertEquals(AppWindowSizeClass.Expanded, AppLayout.windowSizeClassFor(1040.dp))
        assertEquals(AppWindowSizeClass.Expanded, AppLayout.windowSizeClassFor(2560.dp))
    }

    /**
     * The shell is the only thing in the app that measures the window, so it is the only thing that
     * may decide the class. A screen measuring for itself is how a codebase ends up with four
     * breakpoints that disagree.
     */
    @Test
    fun theShellPublishesTheClassItMeasured() = runSkikoComposeUiTest(WideDisplay) {
        var width by mutableStateOf(360.dp)
        var published: AppWindowSizeClass? = null
        setContent {
            AppTheme {
                Box(Modifier.size(width, 900.dp)) {
                    AppNavigationScaffold(
                        selected = AppTopLevelDestination.TOPICS,
                        onSelect = {},
                        showsNavigation = true,
                    ) {
                        published = LocalAppWindowSizeClass.current
                    }
                }
            }
        }

        assertEquals(AppWindowSizeClass.Compact, published)

        width = 800.dp
        waitForIdle()
        assertEquals(AppWindowSizeClass.Medium, published)

        width = 1400.dp
        waitForIdle()
        assertEquals(AppWindowSizeClass.Expanded, published)
    }

    /**
     * The dashboard's groups are the same at both classes and in the same order; only the number of
     * columns changes. The panes are asserted by tag rather than by position, because the point is
     * that the standing figures and the actionable lists end up in different scrollers, not where
     * either happens to be drawn.
     */
    @Test
    fun theDashboardIsOneScrollerWhenCompactAndTwoPanesWhenExpanded() =
        runSkikoComposeUiTest(WideDisplay) {
        var windowClass by mutableStateOf(AppWindowSizeClass.Compact)
        setContent {
            AppTheme {
                CompositionLocalProvider(LocalAppWindowSizeClass provides windowClass) {
                    Box(Modifier.size(1400.dp, 900.dp)) {
                        ProgressScreen(dashboard(), {}, {}, {}, {}, { _, _ -> }, {}, {})
                    }
                }
            }
        }

        onNodeWithTag(ProgressContentTag).assertIsDisplayed()
        onNodeWithTag(ProgressStandingPaneTag).assertDoesNotExist()

        windowClass = AppWindowSizeClass.Expanded
        waitForIdle()

        onNodeWithTag(ProgressStandingPaneTag).assertIsDisplayed()
        onNodeWithTag(ProgressActionPaneTag).assertIsDisplayed()
        // The single-scroller handle deliberately does not follow the content onto one of the
        // panes: at this class there is no single dashboard scroller for it to name.
        onNodeWithTag(ProgressContentTag).assertDoesNotExist()
        onNodeWithText("All-time accuracy").assertIsDisplayed()
        onNodeWithText("Kotlin").assertIsDisplayed()
    }

    /**
     * Prose does not get wider because the window did.
     *
     * The assertion is a fraction of the window rather than a dp, so retuning the measure does not
     * break it — but it is a tight enough fraction that the previous cap, which every other screen
     * still uses, would fail it. That is the whole point of the change: "narrower than the window"
     * was already true at 840dp.
     */
    @Test
    fun aWideWindowKeepsProseAtTheReadingMeasure() = runSkikoComposeUiTest(WideDisplay) {
        setContent {
            AppTheme {
                Box(Modifier.size(1600.dp, 900.dp)) {
                    LessonScreen(sections = listOf(titledSection("Core idea")))
                }
            }
        }

        val column =
            onNodeWithTag(LearningLessonReadingColumnTag).fetchSemanticsNode().boundsInRoot.width
        // `runSkikoComposeUiTest` runs at a density of 1, so a bound in pixels is the same number
        // as the `Dp` that produced it. The expectation is derived from the tokens rather than
        // written out, so retuning the measure retunes the test with it. The margin is the compact
        // one because nothing here provides the shell's.
        val expected = AppLayout.ReadingMeasure.value + AppSpacing.Comfortable.value * 2
        assertTrue(
            column <= expected + 1f,
            "Prose column was ${'$'}column wide in a ${'$'}{WideDisplay.width}px window; the " +
                "reading measure allows ${'$'}expected.",
        )
    }

    /**
     * The outline is orientation for a window with room to spare, not a feature of the reader. On a
     * phone it would cost more of the reading area than the Lesson title.
     */
    @Test
    fun theLessonOutlineAppearsOnlyOnAnExpandedWindow() = runSkikoComposeUiTest(WideDisplay) {
        var windowClass by mutableStateOf(AppWindowSizeClass.Compact)
        setContent {
            AppTheme {
                CompositionLocalProvider(LocalAppWindowSizeClass provides windowClass) {
                    Box(Modifier.size(1600.dp, 900.dp)) {
                        LessonScreen(
                            sections = listOf(
                                titledSection("Core idea"),
                                titledSection("In practice"),
                            ),
                        )
                    }
                }
            }
        }

        onNodeWithTag(LearningLessonOutlineTag).assertDoesNotExist()

        windowClass = AppWindowSizeClass.Expanded
        waitForIdle()

        onNodeWithTag(LearningLessonOutlineTag).assertIsDisplayed()
        onNodeWithText("In this lesson").assertIsDisplayed()
        // Two nodes, and that is the assertion: the outline entry names exactly what the page
        // draws as the Section's heading, because both come from the authored title rather than
        // from anything scraped out of the rendered page.
        onAllNodesWithText("In practice").assertCountEquals(2)
    }

    /**
     * An outline entry moves the page. The Sections are long enough that the second one starts well
     * below the fold, so a reader who can see it afterwards got there by the entry and not by the
     * Lesson happening to be short.
     */
    @Test
    fun anOutlineEntryScrollsToItsSection() = runSkikoComposeUiTest(WideDisplay) {
        setContent {
            AppTheme {
                CompositionLocalProvider(
                    LocalAppWindowSizeClass provides AppWindowSizeClass.Expanded,
                ) {
                    Box(Modifier.size(1600.dp, 900.dp)) {
                        LessonScreen(
                            sections = listOf(
                                longSection("Core idea"),
                                longSection("In practice"),
                            ),
                        )
                    }
                }
            }
        }

        onNodeWithText("Core idea paragraph 0.").assertIsDisplayed()
        onNodeWithText("In practice paragraph 0.").assertIsNotDisplayed()

        onNodeWithTag(LearningLessonOutlineTag).onChildren()
            .filterToOne(hasText("In practice") and hasClickAction())
            .performClick()
        waitForIdle()

        onNodeWithText("In practice paragraph 0.").assertIsDisplayed()
        onNodeWithText("Core idea paragraph 0.").assertIsNotDisplayed()
    }

    /**
     * A Lesson whose Sections carry no titles and sit in one depth run has exactly one heading, so
     * an outline of it would be a list of one. The entries are derived from the same two facts the
     * page draws headings from, which is what makes this checkable at all.
     */
    @Test
    fun aLessonWithOneHeadingGetsNoOutlineEvenWhenThereIsRoom() =
        runSkikoComposeUiTest(WideDisplay) {
        setContent {
            AppTheme {
                CompositionLocalProvider(
                    LocalAppWindowSizeClass provides AppWindowSizeClass.Expanded,
                ) {
                    Box(Modifier.size(1600.dp, 900.dp)) {
                        LessonScreen(
                            sections = List(3) {
                                LearningSection(
                                    depth = LearningDepth.CORE,
                                    blocks = listOf(LearningBlock.Paragraph("Body $it.")),
                                )
                            },
                        )
                    }
                }
            }
        }

        onNodeWithTag(LearningLessonOutlineTag).assertDoesNotExist()
    }

    /**
     * A search that matches nothing states what was searched for and offers the way out of it. It
     * recommends nothing: this screen searches the catalogue it is showing, and no match means the
     * catalogue does not hold the word.
     */
    @Test
    fun aSearchWithNoMatchesKeepsTheQueryAndOffersToClearIt() = runComposeUiTest {
        val queries = mutableListOf<String>()
        setContent {
            AppTheme {
                Box(Modifier.size(400.dp, 900.dp)) {
                    TopicBrowserScreen(
                        state = TopicBrowserUiState.Content(
                            topics = emptyList(),
                            query = "monads",
                        ),
                        onTopicClick = {},
                        onRetry = {},
                        onSearchQueryChange = queries::add,
                    )
                }
            }
        }

        onNodeWithTag(TopicBrowserNoResultsTag).assertIsDisplayed()
        onNodeWithText("No topics or subtopics match \"monads\"").assertIsDisplayed()
        onNodeWithText("Clear search").performClick()

        assertEquals(listOf(""), queries)
    }

    @Composable
    private fun LessonScreen(sections: List<LearningSection>) {
        LearningLessonScreen(
            state = LearningLessonUiState.Content(
                unitId = "unit",
                lessonId = "lesson",
                title = "Lesson title",
                summary = "Lesson summary.",
                placement = null,
                sections = sections,
                sources = emptyList(),
                previousLesson = null,
                nextLesson = null,
            ),
            onBack = {},
            onRetry = {},
            onNavigateLesson = {},
            onPracticeUnit = {},
            onOpenSource = {},
        )
    }
}

/**
 * A virtual display past [AppLayout.ExpandedWidthBreakpoint].
 *
 * The default is 1024x768, which is a *medium* window under this app's rules, so a test that only
 * sized a `Box` inside it would be measuring a 1024px window and quietly asserting the medium
 * behaviour instead of the expanded one.
 */
private val WideDisplay = Size(1600f, 900f)

/** Long enough that the Section after it starts below the fold of a 900px window. */
private fun longSection(title: String) = LearningSection(
    depth = LearningDepth.CORE,
    blocks = List(20) { LearningBlock.Paragraph("$title paragraph $it.") },
    title = title,
)

private fun titledSection(title: String) = LearningSection(
    depth = LearningDepth.CORE,
    blocks = listOf(LearningBlock.Paragraph("Body of $title.")),
    title = title,
)

private fun dashboard(): ProgressUiState.Content = ProgressUiState.Content(
    completedAttemptCount = 3,
    answeredQuestionCount = 30,
    correctAnswerCount = 21,
    percentage = 70.0,
    coverage = ProgressCoverageUiModel(25, 100, 25.0),
    recentPerformance = null,
    unresolvedMistakeCount = 2,
    weakAreas = emptyList(),
    topics = listOf(ProgressTopicUiModel("a", "Kotlin", 20, 14, 70.0)),
    history = emptyList(),
)
