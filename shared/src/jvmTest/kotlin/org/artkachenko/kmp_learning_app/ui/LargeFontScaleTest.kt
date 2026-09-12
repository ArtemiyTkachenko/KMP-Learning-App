package org.artkachenko.kmp_learning_app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.progress.ProgressActionPaneTag
import org.artkachenko.kmp_learning_app.progress.ProgressContentTag
import org.artkachenko.kmp_learning_app.progress.ProgressCoverageUiModel
import org.artkachenko.kmp_learning_app.progress.ProgressScreen
import org.artkachenko.kmp_learning_app.progress.ProgressStandingPaneTag
import org.artkachenko.kmp_learning_app.progress.ProgressTopicUiModel
import org.artkachenko.kmp_learning_app.progress.ProgressUiState
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeAvailability
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderContentTag
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderScreen
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderStartButtonTag
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderUiState
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeScopeKind
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeScopeUiModel
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeSourceOption
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme
import org.artkachenko.kmp_learning_app.ui.theme.AppWindowSizeClass
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppWindowSizeClass

/**
 * The layouts at a doubled font scale, in the narrowest window the app supports.
 *
 * This is the condition that breaks a layout built on fixed heights and single-line rows, and it is
 * not exotic: a 200% text size is an ordinary accessibility setting on every platform this app
 * ships to. The tests assert the two things that actually go wrong — content running off the side
 * of the window, and a primary action becoming unreachable — rather than trying to assert that
 * something "looks right", which no assertion can see.
 *
 * The density is fixed at 1 and only the font scale is raised, so a failure is unambiguously about
 * type size rather than about a smaller window in disguise.
 */
@OptIn(ExperimentalTestApi::class)
internal class LargeFontScaleTest {

    /**
     * The builder is the densest form in the app — three sets of chips, each with a leading control
     * inside the chip — and the one most likely to push its Start button off the bottom. `FlowRow`
     * is what is being checked here: at this type size the chips cannot fit one line, so a fixed
     * `Row` would carry the last option off the right-hand edge instead of wrapping it.
     */
    @Test
    fun thePracticeBuilderWrapsItsChipsAndKeepsStartReachable() =
        runSkikoComposeUiTest(size = PhoneDisplay, density = DoubledText) {
            setContent {
                AppTheme {
                    Box(Modifier.size(PhoneWidth, PhoneHeight).testTag(TestRootTag)) {
                        PracticeBuilderScreen(
                            state = builderState(),
                            onBack = {},
                            onQuestionCountClick = {},
                            onLevelClick = {},
                            onSourceClick = {},
                            onStartClick = {},
                            onRetryAvailability = {},
                        )
                    }
                }
            }

            val windowWidth = onNodeWithTag(TestRootTag).fetchSemanticsNode().boundsInRoot.width

            val form = onNodeWithTag(PracticeBuilderContentTag)
            form.performScrollToNode(hasText("Advanced"))
            onNodeWithText("Advanced").assertWithin(windowWidth)
            form.performScrollToNode(hasText("Mistakes"))
            onNodeWithText("Mistakes").assertWithin(windowWidth)
            form.performScrollToNode(hasTestTag(PracticeBuilderStartButtonTag))
            onNodeWithTag(PracticeBuilderStartButtonTag)
                .assertIsDisplayed()
                .assertWithin(windowWidth)
        }

    /**
     * The dashboard at this type size is several screens tall, which is fine — it scrolls. What
     * must not happen is a metric row whose label and value are pushed apart until the value leaves
     * the window, which is the failure mode of a `SpaceBetween` row with two long strings in it.
     */
    @Test
    fun theDashboardKeepsItsFiguresInsideTheWindow() =
        runSkikoComposeUiTest(size = PhoneDisplay, density = DoubledText) {
            setContent {
                AppTheme {
                    Box(Modifier.size(PhoneWidth, PhoneHeight).testTag(TestRootTag)) {
                        ProgressScreen(dashboardState(), {}, {}, {}, {}, { _, _ -> }, {}, {})
                    }
                }
            }

            val windowWidth = onNodeWithTag(TestRootTag).fetchSemanticsNode().boundsInRoot.width

            onNodeWithText("Questions answered").assertWithin(windowWidth)
            onNodeWithTag(ProgressContentTag).performScrollToNode(hasText("Kotlin"))
            onNodeWithText("Kotlin").assertWithin(windowWidth)
            // The section that states there is nothing to single out is prose rather than a card,
            // so it is the one most likely to overflow if its container were fixed-width.
            onNodeWithTag(ProgressContentTag)
                .performScrollToNode(hasText("Nothing standing out yet"))
            onNodeWithText("Nothing standing out yet").assertWithin(windowWidth)
        }

    /**
     * The two-pane arrangements at a doubled type size, which is the tightest case the app has: a
     * pane is half a window, and at this scale its text is the size a phone renders at 1x. The
     * panes must still hold their content rather than pushing it out of the window, and the screen
     * must still be two panes — falling back to one column would be a silent loss of the layout.
     *
     * `runSkikoComposeUiTest` defaults to a 1024x768 display, which is a *medium* window under
     * these rules, so the size is passed explicitly.
     */
    @Test
    fun theExpandedDashboardKeepsBothPanesInsideTheWindow() =
        runSkikoComposeUiTest(size = DesktopDisplay, density = DoubledText) {
            setContent {
                AppTheme {
                    CompositionLocalProvider(
                        LocalAppWindowSizeClass provides AppWindowSizeClass.Expanded,
                    ) {
                        Box(Modifier.size(DesktopWidth, DesktopHeight).testTag(TestRootTag)) {
                            ProgressScreen(dashboardState(), {}, {}, {}, {}, { _, _ -> }, {}, {})
                        }
                    }
                }
            }

            val windowWidth = onNodeWithTag(TestRootTag).fetchSemanticsNode().boundsInRoot.width

            onNodeWithTag(ProgressStandingPaneTag).assertIsDisplayed().assertWithin(windowWidth)
            onNodeWithTag(ProgressActionPaneTag).assertIsDisplayed().assertWithin(windowWidth)
            onNodeWithText("All-time accuracy").assertWithin(windowWidth)
            onNodeWithTag(ProgressActionPaneTag).performScrollToNode(hasText("Kotlin"))
            onNodeWithText("Kotlin").assertWithin(windowWidth)
        }
}

private fun SemanticsNodeInteraction.assertWithin(windowWidth: Float): SemanticsNodeInteraction {
    val bounds = fetchSemanticsNode().boundsInRoot
    assertTrue(
        bounds.left >= -1f && bounds.right <= windowWidth + 1f,
        "Node spans ${bounds.left}..${bounds.right} in a ${windowWidth}px window.",
    )
    return this
}

/** A small phone, which is where a doubled type size has the least room to go wrong. */
private val PhoneWidth = 360.dp
private val PhoneHeight = 640.dp
private val PhoneDisplay = Size(PhoneWidth.value, PhoneHeight.value)

/** Comfortably past the expanded breakpoint, so the two-pane arrangements actually compose. */
private val DesktopWidth = 1440.dp
private val DesktopHeight = 900.dp
private val DesktopDisplay = Size(DesktopWidth.value, DesktopHeight.value)

/** Density 1 so pixels and `Dp` agree; only the type scale is raised. */
private val DoubledText = Density(density = 1f, fontScale = 2f)

private const val TestRootTag = "large_font_test_root"

private fun builderState() = PracticeBuilderUiState(
    scope = PracticeScopeUiModel(kind = PracticeScopeKind.TOPIC, name = "Kotlin"),
    questionCount = 10,
    questionCountOptions = listOf(5, 10, 15, 20),
    levels = setOf(QuestionLevel.FOUNDATION),
    source = PracticeQuestionSource.ALL,
    sourceOptions = PracticeQuestionSource.entries.map {
        PracticeSourceOption(source = it, isAvailable = true)
    },
    availability = PracticeAvailability.Available(eligibleQuestionCount = 12),
)

private fun dashboardState() = ProgressUiState.Content(
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
