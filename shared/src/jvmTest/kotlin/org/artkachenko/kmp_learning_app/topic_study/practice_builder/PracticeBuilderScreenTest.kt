package org.artkachenko.kmp_learning_app.topic_study.practice_builder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.assessment.AllQuestionLevels
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme
import org.artkachenko.kmp_learning_app.ui.theme.AppWindowSizeClass
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppWindowSizeClass

@OptIn(ExperimentalTestApi::class)
internal class PracticeBuilderScreenTest {
    @Test
    fun rendersEveryControlWithTheDefaultSelections() = runComposeUiTest {
        setContentWith(state())

        onNodeWithText("Topic: Coroutines").assertIsDisplayed()
        onNodeWithTag(practiceQuestionCountTag(10)).assertIsSelected()
        onNodeWithTag(practiceQuestionCountTag(5)).assertIsNotSelected()
        QuestionLevel.entries.forEach { level ->
            onNodeWithTag(practiceLevelTag(level)).assertIsSelected()
        }
        onNodeWithTag(practiceSourceTag(PracticeQuestionSource.ALL)).assertIsSelected()
        onNodeWithTag(PracticeBuilderStartButtonTag).assertIsEnabled()
    }

    @Test
    fun filterControlsExposeSingleAndMultipleSelectionRoles() = runComposeUiTest {
        setContentWith(state())

        onNodeWithTag(practiceQuestionCountTag(10)).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton),
        )
        onNodeWithTag(practiceLevelTag(QuestionLevel.FOUNDATION)).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox),
        )
        onNodeWithTag(practiceSourceTag(PracticeQuestionSource.ALL)).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton),
        )
    }

    @Test
    fun togglingALevelReportsItToTheStateHolder() = runComposeUiTest {
        val toggled = mutableListOf<QuestionLevel>()
        setContentWith(state(), onLevelClick = { toggled += it })

        onNodeWithTag(practiceLevelTag(QuestionLevel.APPLIED)).performClick()
        onNodeWithTag(practiceLevelTag(QuestionLevel.ADVANCED)).performClick()

        assertEquals(listOf(QuestionLevel.APPLIED, QuestionLevel.ADVANCED), toggled)
    }

    /**
     * The screen renders the protected selection rather than enforcing it: the ViewModel answered
     * the final-level tap by keeping the level, and that is what has to reach the chip.
     */
    @Test
    fun theProtectedFinalLevelStaysSelectedInTheUi() = runComposeUiTest {
        setContentWith(state(levels = setOf(QuestionLevel.ADVANCED)))

        onNodeWithTag(practiceLevelTag(QuestionLevel.ADVANCED)).assertIsSelected()
        onNodeWithTag(practiceLevelTag(QuestionLevel.FOUNDATION)).assertIsNotSelected()
        onNodeWithTag(practiceLevelTag(QuestionLevel.APPLIED)).assertIsNotSelected()
        onNodeWithTag(PracticeBuilderStartButtonTag).assertIsEnabled()
    }

    @Test
    fun everyPracticeSourceIsEnabledIncludingMistakes() = runComposeUiTest {
        val chosen = mutableListOf<PracticeQuestionSource>()
        setContentWith(state(), onSourceClick = { chosen += it })

        PracticeQuestionSource.entries.forEach { source ->
            onNodeWithTag(practiceSourceTag(source)).assertIsEnabled()
        }
        onNodeWithText("Mistakes").assertIsDisplayed()
        onNodeWithText("Dimmed sources are not available yet.").assertDoesNotExist()

        onNodeWithTag(practiceSourceTag(PracticeQuestionSource.UNRESOLVED_MISTAKES)).performClick()

        assertEquals(listOf(PracticeQuestionSource.UNRESOLVED_MISTAKES), chosen)
    }

    @Test
    fun anImplementedSourceCanBeChosen() = runComposeUiTest {
        val chosen = mutableListOf<PracticeQuestionSource>()
        setContentWith(state(), onSourceClick = { chosen += it })

        onNodeWithTag(practiceSourceTag(PracticeQuestionSource.ALL)).assertIsEnabled()
        onNodeWithTag(practiceSourceTag(PracticeQuestionSource.UNSEEN)).assertIsEnabled()
        onNodeWithTag(practiceSourceTag(PracticeQuestionSource.WEAK_AREAS)).assertIsEnabled()
        onNodeWithText("Unseen").assertIsDisplayed()
        onNodeWithText("Weak areas").assertIsDisplayed()

        onNodeWithTag(practiceSourceTag(PracticeQuestionSource.WEAK_AREAS)).performClick()

        assertEquals(listOf(PracticeQuestionSource.WEAK_AREAS), chosen)
    }

    /**
     * A selected source with nothing left to ask stays selected and selectable; only Start
     * responds. Reverting the chip would hide that the learner has finished the unseen pool.
     */
    @Test
    fun aChosenUnseenSourceRendersSelectedEvenWithNothingLeftToAsk() = runComposeUiTest {
        setContentWith(
            state(
                source = PracticeQuestionSource.UNSEEN,
                availability = PracticeAvailability.NoEligibleQuestions,
            ),
        )

        onNodeWithTag(practiceSourceTag(PracticeQuestionSource.UNSEEN)).assertIsSelected()
        onNodeWithTag(practiceSourceTag(PracticeQuestionSource.UNSEEN)).assertIsEnabled()
        onNodeWithTag(practiceSourceTag(PracticeQuestionSource.ALL)).assertIsNotSelected()
        onNodeWithTag(PracticeBuilderStartButtonTag).assertIsNotEnabled()
        onNodeWithText("No questions match this setup. Try more levels.").assertIsDisplayed()
    }

    @Test
    fun startIsDisabledWithFeedbackWhileNothingIsEligible() = runComposeUiTest {
        setContentWith(state(availability = PracticeAvailability.NoEligibleQuestions))

        onNodeWithTag(PracticeBuilderStartButtonTag).assertIsNotEnabled()
        onNodeWithText("No questions match this setup. Try more levels.").assertIsDisplayed()
    }

    @Test
    fun startIsWithheldWhileEligibilityIsStillBeingChecked() = runComposeUiTest {
        setContentWith(state(availability = PracticeAvailability.Checking))

        onNodeWithTag(PracticeBuilderStartButtonTag).assertIsNotEnabled()
        onNodeWithTag(PracticeBuilderAvailabilityTag).assertIsDisplayed()
    }

    @Test
    fun startReportsTheClick() = runComposeUiTest {
        var starts = 0
        setContentWith(state(), onStartClick = { starts++ })

        onNodeWithTag(PracticeBuilderStartButtonTag).performClick()

        assertEquals(1, starts)
    }

    /**
     * A narrow window is the layout's real constraint: four count chips, three levels, and four
     * sources have to wrap and remain reachable rather than being clipped off the right edge.
     */
    @Test
    fun everyControlStaysReachableAtACompactWidth() = runComposeUiTest {
        setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    PracticeBuilderScreen(
                        state = state(),
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

        onNodeWithTag(practiceQuestionCountTag(20)).performScrollTo().assertIsDisplayed()
        onNodeWithTag(practiceSourceTag(PracticeQuestionSource.UNRESOLVED_MISTAKES))
            .performScrollTo().assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasTestTag(PracticeBuilderStartButtonTag))
        onNodeWithTag(PracticeBuilderStartButtonTag).assertIsDisplayed()
    }

    /**
     * The expanded arrangement, which had no test of its own.
     *
     * Two things are asserted and the second is the one that could regress silently. The form and
     * the summary must still be separate panes — falling back to one column would be a quiet loss
     * of the layout. And Start must be its own width there: the button is `fillMaxWidth` on a
     * phone, where the card is the window, and in a pane that is two fifths of a desktop window the
     * same modifier produced a five-hundred-pixel bar. Half the pane is a generous ceiling that
     * a stretched button cannot pass and an ordinary one cannot approach.
     */
    @Test
    fun theExpandedBuilderSeparatesTheFormFromTheSummaryAndKeepsStartItsOwnWidth() =
        runSkikoComposeUiTest(size = DesktopDisplay) {
            setContent {
                AppTheme {
                    CompositionLocalProvider(
                        LocalAppWindowSizeClass provides AppWindowSizeClass.Expanded,
                    ) {
                        Box(Modifier.size(DesktopWidth, DesktopHeight)) {
                            PracticeBuilderScreen(
                                state = state(),
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
            }

            onNodeWithTag(PracticeBuilderFormPaneTag).assertIsDisplayed()
            onNodeWithTag(PracticeBuilderSummaryPaneTag).assertIsDisplayed()
            onNodeWithTag(practiceQuestionCountTag(20)).assertIsDisplayed()

            val paneWidth = onNodeWithTag(PracticeBuilderSummaryPaneTag)
                .fetchSemanticsNode().boundsInRoot.width
            val startWidth = onNodeWithTag(PracticeBuilderStartButtonTag)
                .assertIsDisplayed()
                .fetchSemanticsNode().boundsInRoot.width

            assertTrue(
                startWidth < paneWidth / 2f,
                "Start was ${startWidth}px in a ${paneWidth}px pane; it should size to its label.",
            )
        }

    private fun ComposeUiTest.setContentWith(
        state: PracticeBuilderUiState,
        onLevelClick: (QuestionLevel) -> Unit = {},
        onSourceClick: (PracticeQuestionSource) -> Unit = {},
        onStartClick: () -> Unit = {},
    ) {
        setContent {
            MaterialTheme {
                PracticeBuilderScreen(
                    state = state,
                    onBack = {},
                    onQuestionCountClick = {},
                    onLevelClick = onLevelClick,
                    onSourceClick = onSourceClick,
                    onStartClick = onStartClick,
                    onRetryAvailability = {},
                )
            }
        }
    }

    /**
     * The thing being configured is announced as a heading, like every other screen's subject line.
     *
     * The three field labels under it were already headings — `SectionHeading` applies it — so
     * heading navigation reached "Questions", "Levels" and "Draw from" while the scope they all
     * apply to was reachable only by reading past them.
     */
    @Test
    fun theScopeBeingConfiguredIsAHeading() = runComposeUiTest {
        setContent {
            MaterialTheme {
                PracticeBuilderScreen(
                    state = state(),
                    onBack = {},
                    onQuestionCountClick = {},
                    onLevelClick = {},
                    onSourceClick = {},
                    onStartClick = {},
                    onRetryAvailability = {},
                )
            }
        }

        onNodeWithText("Topic: Coroutines").assertIsDisplayed().assert(isHeading())
    }

    private fun state(
        levels: Set<QuestionLevel> = AllQuestionLevels,
        source: PracticeQuestionSource = PracticeQuestionSource.ALL,
        availability: PracticeAvailability = PracticeAvailability.Available(12),
    ): PracticeBuilderUiState =
        PracticeBuilderUiState(
            scope = PracticeScopeUiModel(PracticeScopeKind.TOPIC, "Coroutines"),
            questionCount = DefaultPracticeQuestionCount,
            questionCountOptions = PracticeQuestionCountOptions,
            levels = levels,
            source = source,
            sourceOptions = PracticeQuestionSource.entries.map { option ->
                PracticeSourceOption(source = option, isAvailable = true)
            },
            availability = availability,
        )
}

/** Comfortably past the expanded breakpoint, so the two-pane arrangement actually composes. */
private val DesktopWidth = 1440.dp
private val DesktopHeight = 900.dp
private val DesktopDisplay = Size(DesktopWidth.value, DesktopHeight.value)
