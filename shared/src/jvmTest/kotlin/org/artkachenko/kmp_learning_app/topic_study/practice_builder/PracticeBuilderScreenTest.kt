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
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
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

/**
 * The builder's behaviour and semantics, asserted through roles and states rather than through the
 * components that publish them.
 *
 * Nothing here names a component type, a colour, a dimension, or an animation. The screen's options
 * were `FilterChip`s and are now selection surfaces of the screen's own; every assertion below held
 * before that change and holds after it, which is the point — what a learner and a screen reader can
 * do with the form is the contract, and the treatment is not.
 *
 * The configuration is one lazy column, so an option below the fold is not merely off-screen, it is
 * out of composition. [scrollToTag] and [scrollToText] are what the assertions go through for that
 * reason; a bare `onNodeWithTag` would be asserting that the window happens to be tall enough.
 */
@OptIn(ExperimentalTestApi::class)
internal class PracticeBuilderScreenTest {
    @Test
    fun rendersEveryControlWithTheDefaultSelections() = runComposeUiTest {
        setContentWith(state())

        onNodeWithText("Topic: Coroutines").assertIsDisplayed()
        onNodeWithTag(practiceQuestionCountTag(10)).assertIsSelected()
        onNodeWithTag(practiceQuestionCountTag(5)).assertIsNotSelected()
        QuestionLevel.entries.forEach { level ->
            scrollToTag(practiceLevelTag(level)).assertIsSelected()
        }
        scrollToTag(practiceSourceTag(PracticeQuestionSource.ALL)).assertIsSelected()
        scrollToTag(PracticeBuilderStartButtonTag).assertIsEnabled()
    }

    @Test
    fun filterControlsExposeSingleAndMultipleSelectionRoles() = runComposeUiTest {
        setContentWith(state())

        onNodeWithTag(practiceQuestionCountTag(10)).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton),
        )
        scrollToTag(practiceLevelTag(QuestionLevel.FOUNDATION)).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox),
        )
        scrollToTag(practiceSourceTag(PracticeQuestionSource.ALL)).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton),
        )
    }

    /**
     * Each count is one node, announced by the sentence rather than by its two visible fragments.
     *
     * The tile draws the figure and the unit on separate lines, and without the name the block
     * publishes for itself a screen reader would read "10" and "questions" as two unrelated pieces
     * of a control whose whole content is the number.
     */
    @Test
    fun aQuestionCountIsAnnouncedAsOnePhrase() = runComposeUiTest {
        setContentWith(state())

        onNodeWithTag(practiceQuestionCountTag(10)).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf("10 questions"),
            ),
        )
    }

    @Test
    fun choosingALengthReportsItToTheStateHolder() = runComposeUiTest {
        val chosen = mutableListOf<Int>()
        setContentWith(state(), onQuestionCountClick = { chosen += it })

        onNodeWithTag(practiceQuestionCountTag(20)).performClick()
        onNodeWithTag(practiceQuestionCountTag(5)).performClick()

        assertEquals(listOf(20, 5), chosen)
    }

    @Test
    fun togglingALevelReportsItToTheStateHolder() = runComposeUiTest {
        val toggled = mutableListOf<QuestionLevel>()
        setContentWith(state(), onLevelClick = { toggled += it })

        scrollToTag(practiceLevelTag(QuestionLevel.APPLIED)).performClick()
        scrollToTag(practiceLevelTag(QuestionLevel.ADVANCED)).performClick()

        assertEquals(listOf(QuestionLevel.APPLIED, QuestionLevel.ADVANCED), toggled)
    }

    /**
     * The screen renders the protected selection rather than enforcing it: the ViewModel answered
     * the final-level tap by keeping the level, and that is what has to reach the control.
     */
    @Test
    fun theProtectedFinalLevelStaysSelectedInTheUi() = runComposeUiTest {
        setContentWith(state(levels = setOf(QuestionLevel.ADVANCED)))

        scrollToTag(practiceLevelTag(QuestionLevel.ADVANCED)).assertIsSelected()
        scrollToTag(practiceLevelTag(QuestionLevel.FOUNDATION)).assertIsNotSelected()
        scrollToTag(practiceLevelTag(QuestionLevel.APPLIED)).assertIsNotSelected()
        scrollToTag(PracticeBuilderStartButtonTag).assertIsEnabled()
    }

    @Test
    fun everyPracticeSourceIsEnabledIncludingMistakes() = runComposeUiTest {
        val chosen = mutableListOf<PracticeQuestionSource>()
        setContentWith(state(), onSourceClick = { chosen += it })

        PracticeQuestionSource.entries.forEach { source ->
            scrollToTag(practiceSourceTag(source)).assertIsEnabled()
        }
        scrollToText("Mistakes").assertIsDisplayed()
        onNodeWithText("Dimmed sources are not available yet.").assertDoesNotExist()

        scrollToTag(practiceSourceTag(PracticeQuestionSource.UNRESOLVED_MISTAKES)).performClick()

        assertEquals(listOf(PracticeQuestionSource.UNRESOLVED_MISTAKES), chosen)
    }

    @Test
    fun anImplementedSourceCanBeChosen() = runComposeUiTest {
        val chosen = mutableListOf<PracticeQuestionSource>()
        setContentWith(state(), onSourceClick = { chosen += it })

        scrollToTag(practiceSourceTag(PracticeQuestionSource.ALL)).assertIsEnabled()
        scrollToTag(practiceSourceTag(PracticeQuestionSource.UNSEEN)).assertIsEnabled()
        scrollToTag(practiceSourceTag(PracticeQuestionSource.WEAK_AREAS)).assertIsEnabled()
        scrollToText("Unseen").assertIsDisplayed()
        scrollToText("Weak areas").assertIsDisplayed()

        scrollToTag(practiceSourceTag(PracticeQuestionSource.WEAK_AREAS)).performClick()

        assertEquals(listOf(PracticeQuestionSource.WEAK_AREAS), chosen)
    }

    /**
     * A source whose policy does not exist stays visible, stays announced, and reports nothing when
     * pressed. Hiding it would leave the learner unable to see that the option exists at all, and
     * the disabled semantics are what stop a screen reader offering it as a choice.
     */
    @Test
    fun anUnavailableSourceStaysVisibleAndInert() = runComposeUiTest {
        val chosen = mutableListOf<PracticeQuestionSource>()
        setContentWith(
            state(unavailableSources = setOf(PracticeQuestionSource.UNRESOLVED_MISTAKES)),
            onSourceClick = { chosen += it },
        )

        val unavailable = scrollToTag(practiceSourceTag(PracticeQuestionSource.UNRESOLVED_MISTAKES))
        unavailable.assertIsDisplayed().assertIsNotEnabled()
        scrollToText("Mistakes").assertIsDisplayed()
        scrollToText("Dimmed sources are not available yet.").assertIsDisplayed()

        unavailable.performClick()

        assertEquals(emptyList(), chosen)
    }

    /**
     * A selected source with nothing left to ask stays selected and selectable; only Start
     * responds. Reverting the selection would hide that the learner has finished the unseen pool.
     */
    @Test
    fun aChosenUnseenSourceRendersSelectedEvenWithNothingLeftToAsk() = runComposeUiTest {
        setContentWith(
            state(
                source = PracticeQuestionSource.UNSEEN,
                availability = PracticeAvailability.NoEligibleQuestions,
            ),
        )

        scrollToTag(practiceSourceTag(PracticeQuestionSource.UNSEEN)).assertIsSelected()
        scrollToTag(practiceSourceTag(PracticeQuestionSource.UNSEEN)).assertIsEnabled()
        scrollToTag(practiceSourceTag(PracticeQuestionSource.ALL)).assertIsNotSelected()
        scrollToTag(PracticeBuilderStartButtonTag).assertIsNotEnabled()
        scrollToText("No questions match this setup. Try more levels.").assertIsDisplayed()
    }

    @Test
    fun startIsDisabledWithFeedbackWhileNothingIsEligible() = runComposeUiTest {
        setContentWith(state(availability = PracticeAvailability.NoEligibleQuestions))

        scrollToTag(PracticeBuilderStartButtonTag).assertIsNotEnabled()
        scrollToText("No questions match this setup. Try more levels.").assertIsDisplayed()
    }

    @Test
    fun startIsWithheldWhileEligibilityIsStillBeingChecked() = runComposeUiTest {
        setContentWith(state(availability = PracticeAvailability.Checking))

        scrollToTag(PracticeBuilderStartButtonTag).assertIsNotEnabled()
        scrollToTag(PracticeBuilderAvailabilityTag).assertIsDisplayed()
    }

    /**
     * The available verdict states its count and announces the whole sentence.
     *
     * The figure and its caption are drawn on two lines and published as one phrase, so the run the
     * configuration produces is one statement to a screen reader rather than a number followed by
     * a fragment.
     */
    @Test
    fun theAvailableVerdictStatesTheEligibleCount() = runComposeUiTest {
        setContentWith(state(availability = PracticeAvailability.Available(12)))

        scrollToText("12 questions ready").assertIsDisplayed()
        scrollToTag(PracticeBuilderStartButtonTag).assertIsEnabled()
    }

    /**
     * Retry belongs to the one availability state the learner cannot resolve by reconfiguring.
     *
     * A failed read is an operation that can be repeated; a setup that matches nothing and a target
     * that is no longer practiceable are settled answers, and offering to repeat them would promise
     * a different outcome from the same question.
     */
    @Test
    fun onlyAFailedCheckOffersRetry() = runComposeUiTest {
        var retries = 0
        setContentWith(state(availability = PracticeAvailability.Error), onRetry = { retries++ })

        scrollToText("Available questions could not be checked.").assertIsDisplayed()
        scrollToTag(PracticeBuilderRetryTag).assertIsDisplayed().performClick()
        scrollToTag(PracticeBuilderStartButtonTag).assertIsNotEnabled()

        assertEquals(1, retries)
    }

    @Test
    fun noRetryIsOfferedForAConfigurationTheLearnerCanChange() = runComposeUiTest {
        setContentWith(state(availability = PracticeAvailability.NoEligibleQuestions))

        onNodeWithTag(PracticeBuilderRetryTag).assertDoesNotExist()
    }

    @Test
    fun anUnpracticeableTargetDisablesStartWithoutOfferingRetry() = runComposeUiTest {
        setContentWith(state(availability = PracticeAvailability.TargetUnavailable))

        scrollToText("This learning unit is no longer available for practice.").assertIsDisplayed()
        scrollToTag(PracticeBuilderStartButtonTag).assertIsNotEnabled()
        onNodeWithTag(PracticeBuilderRetryTag).assertDoesNotExist()
    }

    @Test
    fun startReportsTheClick() = runComposeUiTest {
        var starts = 0
        setContentWith(state(), onStartClick = { starts++ })

        scrollToTag(PracticeBuilderStartButtonTag).performClick()

        assertEquals(1, starts)
    }

    /**
     * A narrow window is the layout's real constraint: four count tiles, three levels, and four
     * sources have to wrap or stack and remain reachable rather than being clipped off the right
     * edge.
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
        onNode(hasScrollAction()).performScrollToNode(
            hasTestTag(practiceSourceTag(PracticeQuestionSource.UNRESOLVED_MISTAKES)),
        )
        onNodeWithTag(practiceSourceTag(PracticeQuestionSource.UNRESOLVED_MISTAKES))
            .assertIsDisplayed()
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
            // The conclusion is what the second pane is for, and it is short enough to sit at the
            // top of its own scroller while the configuration beside it scrolls independently.
            onNodeWithTag(PracticeBuilderAvailabilityTag).assertIsDisplayed()

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

    /**
     * The thing being configured is announced as a heading, like every other screen's subject line.
     *
     * The three field labels under it were already headings — `SectionHeading` applies it — so
     * heading navigation reached "Questions", "Levels" and "Draw from" while the scope they all
     * apply to was reachable only by reading past them.
     */
    @Test
    fun theScopeBeingConfiguredIsAHeading() = runComposeUiTest {
        setContentWith(state())

        onNodeWithText("Topic: Coroutines").assertIsDisplayed().assert(isHeading())
    }

    /** Brings a lazily composed option into view before asserting anything about it. */
    private fun ComposeUiTest.scrollToTag(tag: String): SemanticsNodeInteraction {
        onNodeWithTag(PracticeBuilderContentTag).performScrollToNode(hasTestTag(tag))
        return onNodeWithTag(tag)
    }

    private fun ComposeUiTest.scrollToText(text: String): SemanticsNodeInteraction {
        onNodeWithTag(PracticeBuilderContentTag).performScrollToNode(hasText(text))
        return onNodeWithText(text)
    }

    private fun ComposeUiTest.setContentWith(
        state: PracticeBuilderUiState,
        onQuestionCountClick: (Int) -> Unit = {},
        onLevelClick: (QuestionLevel) -> Unit = {},
        onSourceClick: (PracticeQuestionSource) -> Unit = {},
        onStartClick: () -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        setContent {
            MaterialTheme {
                PracticeBuilderScreen(
                    state = state,
                    onBack = {},
                    onQuestionCountClick = onQuestionCountClick,
                    onLevelClick = onLevelClick,
                    onSourceClick = onSourceClick,
                    onStartClick = onStartClick,
                    onRetryAvailability = onRetry,
                )
            }
        }
    }

    private fun state(
        levels: Set<QuestionLevel> = AllQuestionLevels,
        source: PracticeQuestionSource = PracticeQuestionSource.ALL,
        availability: PracticeAvailability = PracticeAvailability.Available(12),
        unavailableSources: Set<PracticeQuestionSource> = emptySet(),
    ): PracticeBuilderUiState =
        PracticeBuilderUiState(
            scope = PracticeScopeUiModel(PracticeScopeKind.TOPIC, "Coroutines"),
            questionCount = DefaultPracticeQuestionCount,
            questionCountOptions = PracticeQuestionCountOptions,
            levels = levels,
            source = source,
            sourceOptions = PracticeQuestionSource.entries.map { option ->
                PracticeSourceOption(source = option, isAvailable = option !in unavailableSources)
            },
            availability = availability,
        )
}

/** Comfortably past the expanded breakpoint, so the two-pane arrangement actually composes. */
private val DesktopWidth = 1440.dp
private val DesktopHeight = 900.dp
private val DesktopDisplay = Size(DesktopWidth.value, DesktopHeight.value)
