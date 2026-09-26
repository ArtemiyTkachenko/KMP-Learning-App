package org.artkachenko.kmp_learning_app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
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
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeState
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentResultOutcome
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentRetakeWording
import org.artkachenko.kmp_learning_app.assessment_review.ReviewAnswerUiModel
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionUiModel
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
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
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.SubtopicPracticeItem
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicSubtopicsPage
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
     * The builder is the densest form in the app — three sets of options, two of which wrap and one
     * of which stacks — and the one most likely to push its Start button off the bottom. The
     * wrapping is what is being checked: at this type size the count and level tiles cannot fit one
     * line, so a fixed `Row` would carry the last option off the right-hand edge instead of moving
     * it to the next line, and a full-width source row has to stay inside the window rather than
     * growing past it with its label.
     */
    @Test
    fun thePracticeBuilderWrapsItsOptionsAndKeepsStartReachable() =
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
     * A Subtopic name stays whole at this type size, because its accuracy figure gets out of the way.
     *
     * The row is a weighted text column beside a non-weighted accuracy block, and a `Row` measures
     * the non-weighted child first — so the name used to be left with less width than its own
     * longest word and was broken across a line *inside* the word. `TrailingFigureRow` drops the
     * figure below the name when that would happen.
     *
     * The assertion is the decision rather than the appearance: the figure is below the name rather
     * than beside it. A test that tried to assert "the name is not hyphenated" would be asserting
     * the host's font metrics.
     */
    @Test
    fun theSubtopicRowMovesItsFigureBelowTheNameAtADoubledTypeSize() =
        runSkikoComposeUiTest(size = PhoneDisplay, density = DoubledText) {
            setContent {
                AppTheme {
                    Box(Modifier.size(PhoneWidth, PhoneHeight).testTag(TestRootTag)) {
                        TopicSubtopicsPage(
                            subtopics = listOf(
                                SubtopicPracticeItem(
                                    subtopic = Subtopic(
                                        id = "subtopic_a",
                                        topicId = "topic_a",
                                        name = "Structured concurrency",
                                    ),
                                    questionCount = 30,
                                    learningContext = LearningContextUiModel(
                                        attemptedQuestionCount = 12,
                                        totalQuestionCount = 30,
                                        coveragePercentage = 40.0,
                                        accuracyPercentage = 88.0,
                                        isWeak = false,
                                    ),
                                ),
                            ),
                            onStartSubtopicPractice = {},
                            onPracticePreset = {},
                            onBrowsePractice = {},
                            listState = rememberLazyListState(),
                            modifier = Modifier.size(PhoneWidth, PhoneHeight),
                        )
                    }
                }
            }

            val windowWidth = onNodeWithTag(TestRootTag).fetchSemanticsNode().boundsInRoot.width
            // The row merges its descendants, so the merged tree answers with the whole row for
            // either of these. The labels themselves are what have to be compared.
            val name = onNodeWithText("Structured concurrency", useUnmergedTree = true)
                .assertIsDisplayed()
                .assertWithin(windowWidth)
                .fetchSemanticsNode().boundsInRoot
            val figure = onNodeWithText("accuracy", useUnmergedTree = true)
                .assertIsDisplayed()
                .assertWithin(windowWidth)
                .fetchSemanticsNode().boundsInRoot

            assertTrue(
                figure.top >= name.bottom,
                "The figure spans ${figure.top}..${figure.bottom} beside a name ending at " +
                    "${name.bottom}, so it is still squeezing the column the name wraps in.",
            )
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

            // The standing hero is a fixed-size ring beside a display-scale figure, which is the
            // shape that runs off the edge of a small phone at this type size. Its column is
            // weighted rather than laid out at its intrinsic width precisely so this holds.
            onNodeWithText("70%").assertIsDisplayed().assertWithin(windowWidth)
            onNodeWithText("All-time accuracy").assertWithin(windowWidth)
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

    /**
     * The completion hero, whose figure is the largest type in the product.
     *
     * A display-scale numeral at a doubled type size beside a fixed 64dp ring is the exact shape that
     * runs off the right-hand edge of a small phone, and the score is the one thing on this screen
     * the learner came for. The figure block is weighted rather than laid out at its intrinsic width
     * precisely so this holds; the assertion is that it does, for the widest figure the app produces
     * — a two-digit numerator over a two-digit total.
     *
     * The second assertion is the one about usability: the reveal is an animation over a control that
     * must stay pressable, so the primary action has to be both inside the window and reachable.
     */
    @Test
    fun theCompletionHeroKeepsItsFigureAndItsActionInsideTheWindow() =
        runSkikoComposeUiTest(size = PhoneDisplay, density = DoubledText) {
            setContent {
                AppTheme {
                    Box(Modifier.size(PhoneWidth, PhoneHeight).testTag(TestRootTag)) {
                        AssessmentResultOutcome(
                            title = "Interview complete",
                            correctAnswers = 16,
                            totalQuestions = 20,
                            percentage = 80.0,
                            questions = List(20) { index ->
                                ReviewQuestionItem.Available(
                                    ReviewQuestionUiModel(
                                        questionId = "q$index",
                                        topicId = "kotlin",
                                        subtopicId = "kotlin_basics",
                                        text = "Question $index",
                                        isCorrect = index >= 4,
                                        answers = listOf(
                                            ReviewAnswerUiModel("a", "Answer A", true, index >= 4),
                                        ),
                                        explanation = "Explanation",
                                        sources = emptyList(),
                                    ),
                                )
                            },
                            retakeState = AssessmentRetakeState.Idle,
                            retakeWording = AssessmentRetakeWording(
                                action = "Retake interview",
                                starting = "Starting interview",
                                sourceMissing = "Gone.",
                                noQuestions = "None.",
                                error = "Failed.",
                            ),
                            onRetake = {},
                            retakeActionTestTag = HeroRetakeTag,
                            retakeProgressTestTag = HeroRetakeProgressTag,
                            onPracticeMistakes = {},
                        )
                    }
                }
            }

            val windowWidth = onNodeWithTag(TestRootTag).fetchSemanticsNode().boundsInRoot.width

            onNodeWithText("16 / 20").assertIsDisplayed().assertWithin(windowWidth)
            onNodeWithText("80% correct").assertWithin(windowWidth)
            onNodeWithText("Practice 4 mistakes").assertWithin(windowWidth)
            onNodeWithTag(HeroRetakeTag).assertIsDisplayed().assertWithin(windowWidth)
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
private const val HeroRetakeTag = "large_font_hero_retake"
private const val HeroRetakeProgressTag = "large_font_hero_retake_progress"

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
