package org.artkachenko.kmp_learning_app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeState
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentResultOutcome
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentRetakeWording
import org.artkachenko.kmp_learning_app.assessment_review.ReviewAnswerUiModel
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionUiModel
import org.artkachenko.kmp_learning_app.assessment_review.ReviewSourceUiModel
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressUiState
import org.artkachenko.kmp_learning_app.mixed_interview.InterviewAttemptUiModel
import org.artkachenko.kmp_learning_app.mixed_interview.InterviewHistoryUiModel
import org.artkachenko.kmp_learning_app.mixed_interview.InterviewHistoryUiState
import org.artkachenko.kmp_learning_app.mixed_interview.InterviewStartScreen
import org.artkachenko.kmp_learning_app.progress.ProgressActionPaneTag
import org.artkachenko.kmp_learning_app.progress.ProgressContentTag
import org.artkachenko.kmp_learning_app.progress.ProgressCoverageUiModel
import org.artkachenko.kmp_learning_app.progress.ProgressScreen
import org.artkachenko.kmp_learning_app.progress.ProgressStandingPaneTag
import org.artkachenko.kmp_learning_app.progress.ProgressTopicUiModel
import org.artkachenko.kmp_learning_app.progress.ProgressUiState
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestion
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionAnswerUiModel
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionContentUiModel
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionItem
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionsScreen
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionsUiState
import org.artkachenko.kmp_learning_app.saved_questions.savedQuestionRemoveTag
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeAvailability
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderContentTag
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderScreen
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderStartButtonTag
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderUiState
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeScopeKind
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeScopeUiModel
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeSourceOption
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.SubtopicPracticeItem
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicDetailScreen
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicDetailUiState
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicLearningUnitsUiState
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicSubtopicsPage
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicSubtopicsTabTag
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
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
    /**
     * A saved Question's text stays whole at this type size, because its bookmark gets out of the way.
     *
     * The card header is the same shape as a Subtopic row — a weighted text column beside a
     * non-weighted trailing control — and it acquired the same problem when the bare "Remove" text
     * button became the shared bookmark affordance, which carries an icon and a word and so is
     * wider. A plain `Row` measures the non-weighted child first, leaving a long question with less
     * width than its own longest word and breaking it across a line *inside* that word.
     * `TrailingFigureRow` is the rule this repository already has for that, and the header goes
     * through it.
     *
     * The assertion is the decision rather than the appearance: the bookmark is below the question
     * text rather than beside it. Nothing moves at an ordinary type size, which is what the other
     * Saved Questions tests exercise.
     */
    @Test
    fun theSavedQuestionBookmarkMovesBelowALongQuestionAtADoubledTypeSize() =
        runSkikoComposeUiTest(size = PhoneDisplay, density = DoubledText) {
            setContent {
                AppTheme {
                    Box(Modifier.size(PhoneWidth, PhoneHeight).testTag(TestRootTag)) {
                        SavedQuestionsScreen(
                            state = SavedQuestionsUiState.Content(
                                listOf(
                                    SavedQuestionItem.Available(
                                        savedQuestion = SavedQuestion("q1", 100),
                                        question = SavedQuestionContentUiModel(
                                            questionId = "q1",
                                            // The longest word is the whole point: it is what
                                            // `minIntrinsicWidth` measures and what a squeezed
                                            // column would break inside.
                                            text = "What triggers recomposition?",
                                            answers = listOf(
                                                SavedQuestionAnswerUiModel(
                                                    id = "q1_a",
                                                    text = "State read in composition changes",
                                                    isCorrectAnswer = true,
                                                ),
                                            ),
                                            explanation = "Authored explanation",
                                            sources = listOf(
                                                ReviewSourceUiModel(
                                                    "Source A",
                                                    "https://example.com/a",
                                                ),
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                            onRetry = {},
                            onBrowseTopics = {},
                            onRemoveSaved = {},
                            onSourceClick = {},
                            modifier = Modifier.size(PhoneWidth, PhoneHeight),
                        )
                    }
                }
            }

            val windowWidth = onNodeWithTag(TestRootTag).fetchSemanticsNode().boundsInRoot.width
            val text = onNodeWithText("What triggers recomposition?", useUnmergedTree = true)
                .assertIsDisplayed()
                .assertWithin(windowWidth)
                .fetchSemanticsNode().boundsInRoot
            val bookmark = onNodeWithTag(savedQuestionRemoveTag("q1"))
                .assertIsDisplayed()
                .assertWithin(windowWidth)
                .fetchSemanticsNode().boundsInRoot

            assertTrue(
                bookmark.top >= text.bottom,
                "The bookmark spans ${bookmark.top}..${bookmark.bottom} beside text ending at " +
                    "${text.bottom}, so it is still squeezing the column the question wraps in.",
            )
            // Dropping is only half the decision. `TrailingFigureRow` measures the stacked figure
            // at the row's full width on purpose, so that a figure which aligns itself to the
            // trailing edge stays in the right-hand column — and this call site passed the button
            // itself rather than an end-aligned column around it, so *the button* took the full
            // width. Its label then centred and its hit area became the whole card line, four
            // times the affordance that draws it. The control stays narrower than the row it
            // sits in.
            assertTrue(
                bookmark.width < text.width,
                "The bookmark is ${bookmark.width}px wide inside a ${text.width}px row, so the " +
                    "control still spans the whole card rather than sitting at its trailing edge.",
            )
        }

    /**
     * The Progress hero states its figures as figures, not as columns of single characters.
     *
     * The hero's three metric rows and its coverage title were each an
     * `Arrangement.SpaceBetween` `Row` of two unweighted `Text`s. A `Row` measures its children in
     * order against the width that is left, so the label — unweighted and unbounded — took the
     * whole line and the value was measured against almost nothing. At this type size on this
     * window "140" was drawn as three stacked digits and "29.2%" as a vertical column: the
     * figures a learner opens Progress to read, unreadable.
     *
     * `theDashboardKeepsItsFiguresInsideTheWindow` above does not catch it, and correctly so — it
     * asserts that nothing leaves the window, and nothing did. The digits were being shredded
     * inside it, which is exactly the shape `VC-001` found on the two shared performance rows.
     *
     * The assertion is that each figure still reads as a line of text rather than as a stack of
     * characters, which is a property of the layout and not of the host's font metrics: a figure
     * broken one character per line is far taller than it is wide, and an intact one is not.
     *
     * The window is [PhoneWidth] wide and [TallPhoneHeight] tall rather than the [PhoneHeight] the
     * rest of this file uses. The defect is horizontal — a figure squeezed below its own width —
     * so the narrow measure is the part that matters and is unchanged; the height only decides how
     * much of the hero is on screen. At this type size the hero is about two phone screens tall, so
     * on a 640dp window the coverage percentage sits just below the fold, and whether it lands
     * inside or outside depends on the host's font metrics. This test first passed on one platform
     * and failed on another for exactly that reason, which was the test measuring the window rather
     * than the layout.
     */
    @Test
    fun theProgressHeroKeepsItsFiguresOnOneLineAtADoubledTypeSize() =
        runSkikoComposeUiTest(size = TallPhoneDisplay, density = DoubledText) {
            setContent {
                AppTheme {
                    Box(Modifier.size(PhoneWidth, TallPhoneHeight).testTag(TestRootTag)) {
                        ProgressScreen(heroFigureState(), {}, {}, {}, {}, { _, _ -> }, {}, {})
                    }
                }
            }

            assertReadsAsALine(onNodeWithText("140"), "The hero's answered-question count")
            assertReadsAsALine(onNodeWithText("29.2%"), "The hero's coverage percentage")
        }

    /**
     * The interview record's heading keeps its count beside it rather than stacking its letters.
     *
     * The same `SpaceBetween` pair as the Progress hero's rows, and worse here only because the
     * trailing string is words: at this type size "4 completed" was drawn one character per line,
     * a column tall enough to push the record it heads most of a screen further down.
     *
     * The record sits below the fold on this window and the screen is a `LazyColumn`, so the list
     * is driven to it before the node is asked for.
     */
    @Test
    fun theInterviewRecordHeadingKeepsItsCountWholeAtADoubledTypeSize() =
        runSkikoComposeUiTest(size = PhoneDisplay, density = DoubledText) {
            setContent {
                AppTheme {
                    Box(Modifier.size(PhoneWidth, PhoneHeight).testTag(TestRootTag)) {
                        InterviewStartScreen({}, history = interviewRecordState())
                    }
                }
            }

            onNode(hasScrollAction()).performScrollToNode(hasText("4 completed"))
            assertReadsAsALine(onNodeWithText("4 completed"), "The record's attempt count")
        }

    /**
     * A Topic detail tab grows with the type size instead of clipping its label to a fixed height.
     *
     * `TabHeight` is 48dp for two reasons at once — it is
     * `PrimaryNavigationTabTokens.ContainerHeight` and it is the Material touch target — and it was
     * applied as a fixed `height`. At this type size that left the cell with no vertical room for
     * the label to wrap into, so "Practice" and "Subtopics" were cut mid-word with no ellipsis, and
     * no room for the line box either, so the selected label's descender was sliced flat by the
     * bottom of the cell. It is now a minimum, and `PrimaryTabRow` sizes itself from its tallest
     * tab.
     *
     * The assertion is that decision rather than the appearance, in both directions: at an ordinary
     * type scale the cell is still exactly the Material height, and at a doubled one it is taller.
     * Asserting that a label is not clipped would mean asserting the host's font metrics, and a
     * clipped `Text` reports the bounds it was clamped to rather than the bounds it wanted.
     */
    @Test
    fun theTopicTabsGrowWithTheTypeSizeRatherThanClippingTheirLabels() {
        runSkikoComposeUiTest(size = PhoneDisplay, density = OrdinaryText) {
            setContent {
                AppTheme {
                    Box(Modifier.size(PhoneWidth, PhoneHeight).testTag(TestRootTag)) {
                        TopicDetailScreen(tabState(), null, {}, {}, {}, {}, {})
                    }
                }
            }

            val height = onNodeWithTag(TopicSubtopicsTabTag).fetchSemanticsNode().boundsInRoot.height
            assertEquals(
                MaterialTabHeight.value,
                height,
                "At an ordinary type scale the tab must still be exactly the Material tab height.",
            )
        }

        runSkikoComposeUiTest(size = PhoneDisplay, density = DoubledText) {
            setContent {
                AppTheme {
                    Box(Modifier.size(PhoneWidth, PhoneHeight).testTag(TestRootTag)) {
                        TopicDetailScreen(tabState(), null, {}, {}, {}, {}, {})
                    }
                }
            }

            val height = onNodeWithTag(TopicSubtopicsTabTag).fetchSemanticsNode().boundsInRoot.height
            assertTrue(
                height > MaterialTabHeight.value,
                "The tab is ${height}px tall at a doubled type size, which is still the fixed " +
                    "${MaterialTabHeight.value}px cell — so its label is being clipped to fit " +
                    "rather than given room to wrap.",
            )
        }
    }

    /**
     * The two shared performance rows keep their title whole, because the accuracy figure drops.
     *
     * [PerformanceCard] and [AccuracyRow] are the same shape as a Subtopic row — a weighted text
     * column beside a non-weighted figure — and carry it to the Progress dashboard, the per-Topic
     * screen, and a Mixed interview's breakdown. Both measured the figure first, so at this type
     * size the title was left with less width than its own longest word and broke across a line
     * *inside* it: "Structured concurren / cy" on the card, "Structur / ed concu / rrency" on the
     * row. Nothing overflowed, which is why `theDashboardKeepsItsFiguresInsideTheWindow` passed
     * throughout — the text was being shredded inside the window rather than escaping it.
     *
     * Both now go through `TrailingFigureRow`, which drops the figure below the title exactly when
     * that would happen. The chevron deliberately does not take part: it is a fixed glyph that
     * costs the same width at every type scale, and a navigation affordance that moved below the
     * title would stop marking the row as one that travels.
     *
     * The assertion is the decision rather than the appearance, as on the Subtopic row: the figure
     * is below the title rather than beside it. Asserting that a title is not hyphenated would be
     * asserting the host's font metrics. The components are rendered directly because the defect
     * was theirs and they have several callers; nothing here is specific to one screen.
     */
    @Test
    fun theSharedPerformanceRowsMoveTheirFigureBelowTheTitleAtADoubledTypeSize() =
        runSkikoComposeUiTest(size = PhoneDisplay, density = DoubledText) {
            setContent {
                AppTheme {
                    Box(Modifier.size(PhoneWidth, PhoneHeight).testTag(TestRootTag)) {
                        Column(
                            Modifier.size(PhoneWidth, PhoneHeight).padding(AppSpacing.Comfortable),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
                        ) {
                            PerformanceCard(
                                // The longest word is the point: it is what `minIntrinsicWidth`
                                // measures and what a squeezed column would break inside.
                                title = "Structured concurrency",
                                detail = "12 of 30 answered",
                                percentage = 88.0,
                                caption = "accuracy",
                            )
                            AccuracyRow(
                                title = "Kotlin language fundamentals",
                                detail = "40 of 120 answered",
                                percentage = 72.0,
                                onClick = {},
                            )
                        }
                    }
                }
            }

            val windowWidth = onNodeWithTag(TestRootTag).fetchSemanticsNode().boundsInRoot.width
            // Both rows merge their descendants, so the labels themselves are what have to be
            // compared rather than the merged node that answers for the whole row.
            assertFigureBelowTitle(
                title = onNodeWithText("Structured concurrency", useUnmergedTree = true)
                    .assertIsDisplayed()
                    .assertWithin(windowWidth),
                figure = onNodeWithText("88%", useUnmergedTree = true)
                    .assertIsDisplayed()
                    .assertWithin(windowWidth),
                subject = "The card's figure",
            )
            assertFigureBelowTitle(
                title = onNodeWithText("Kotlin language fundamentals", useUnmergedTree = true)
                    .assertIsDisplayed()
                    .assertWithin(windowWidth),
                figure = onNodeWithText("72%", useUnmergedTree = true)
                    .assertIsDisplayed()
                    .assertWithin(windowWidth),
                subject = "The row's figure",
            )
        }
}

/**
 * A figure squeezed below its own intrinsic width is broken *inside* the string, one character per
 * line, which makes it far taller than it is wide. An intact one is a line of text and is not.
 *
 * This is deliberately a shape rather than a measurement against a font: it holds for any string of
 * more than one character in any face, and it is exactly what "the figure was not shredded" means.
 */
private fun assertReadsAsALine(figure: SemanticsNodeInteraction, subject: String) {
    val bounds = figure.assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    assertTrue(
        bounds.width > bounds.height,
        "$subject is ${bounds.width}x${bounds.height}px, taller than it is wide, so it is still " +
            "being broken one character per line instead of measured as a figure.",
    )
}

/** The one reflow decision both shared performance rows make, asserted the same way for each. */
private fun assertFigureBelowTitle(
    title: SemanticsNodeInteraction,
    figure: SemanticsNodeInteraction,
    subject: String,
) {
    val titleBounds = title.fetchSemanticsNode().boundsInRoot
    val figureBounds = figure.fetchSemanticsNode().boundsInRoot
    assertTrue(
        figureBounds.top >= titleBounds.bottom,
        "$subject spans ${figureBounds.top}..${figureBounds.bottom} beside a title ending at " +
            "${titleBounds.bottom}, so it is still squeezing the column the title wraps in.",
    )
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

/**
 * The same narrow phone, tall enough to hold a whole surface at a doubled type size.
 *
 * For a test whose subject is how wide something is measured, not how much of it is on screen. The
 * width is deliberately still [PhoneWidth]: that is the constraint under test.
 */
private val TallPhoneHeight = 1600.dp
private val TallPhoneDisplay = Size(PhoneWidth.value, TallPhoneHeight.value)

/** Density 1 and the ordinary type scale, so the two halves of the tab test differ in one thing. */
private val OrdinaryText = Density(density = 1f, fontScale = 1f)

/**
 * `PrimaryNavigationTabTokens.ContainerHeight`, which is also the Material touch target.
 *
 * Duplicated rather than exposed, for the reason `ProgressHeroThemeTest` states about its own
 * copy: the production constant is private because it is one surface's decision, and widening its
 * visibility so a test can read it would be changing the code to suit the test.
 */
private val MaterialTabHeight = 48.dp

/** A hero whose counts are wide enough that a squeezed value has something to break inside. */
private fun heroFigureState() = ProgressUiState.Content(
    completedAttemptCount = 9,
    answeredQuestionCount = 140,
    correctAnswerCount = 98,
    percentage = 70.0,
    coverage = ProgressCoverageUiModel(140, 480, 29.2),
    recentPerformance = null,
    unresolvedMistakeCount = 0,
    weakAreas = emptyList(),
    topics = emptyList(),
    history = emptyList(),
)

private fun interviewRecordState() = InterviewHistoryUiState.Content(
    InterviewHistoryUiModel(
        attemptCount = 4,
        latest = InterviewAttemptUiModel("latest", 15, 20, 75.0, RecordCompletedAt),
        best = InterviewAttemptUiModel("best", 18, 20, 90.0, RecordCompletedAt),
    ),
)

/** Fixed so the record's date renders the same whatever the agent's clock and zone are. */
private val RecordCompletedAt: Instant = Instant.fromEpochMilliseconds(1_757_594_626_872)

/** A loaded Topic, so the three tabs actually compose. Their content is not what is asserted. */
private fun tabState() = TopicDetailUiState.Content(
    topic = Topic("topic_a", "Coroutines and Flow"),
    topicQuestionCount = 30,
    subtopics = emptyList(),
    learningUnits = TopicLearningUnitsUiState.Available(emptyList()),
    learningContext = null,
    studyProgress = StudyProgressUiState.Unavailable,
    unresolvedMistakeCount = null,
)
