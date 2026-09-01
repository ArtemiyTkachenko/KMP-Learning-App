package org.artkachenko.kmp_learning_app.topic_study.practice_builder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.assessment.AllQuestionLevels
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel

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
    fun unavailableSourcesAreShownButCannotBeChosen() = runComposeUiTest {
        val chosen = mutableListOf<PracticeQuestionSource>()
        setContentWith(state(), onSourceClick = { chosen += it })

        onNodeWithTag(practiceSourceTag(PracticeQuestionSource.UNSEEN)).assertIsNotEnabled()
        onNodeWithTag(practiceSourceTag(PracticeQuestionSource.WEAK_AREAS)).assertIsNotEnabled()
        onNodeWithTag(practiceSourceTag(PracticeQuestionSource.UNRESOLVED_MISTAKES))
            .assertIsNotEnabled()
        onNodeWithTag(practiceSourceTag(PracticeQuestionSource.ALL)).assertIsEnabled()
        // Named, so the learner can see what targeted practice will offer.
        onNodeWithText("Unseen").assertIsDisplayed()
        onNodeWithText("Weak areas").assertIsDisplayed()
        onNodeWithText("Mistakes").assertIsDisplayed()
        onNodeWithText("Dimmed sources are not available yet.").assertIsDisplayed()

        onNodeWithTag(practiceSourceTag(PracticeQuestionSource.UNSEEN)).performClick()

        assertEquals(emptyList(), chosen)
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
        onNodeWithTag(PracticeBuilderStartButtonTag).performScrollTo().assertIsDisplayed()
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

    private fun state(
        levels: Set<QuestionLevel> = AllQuestionLevels,
        availability: PracticeAvailability = PracticeAvailability.Available(12),
    ): PracticeBuilderUiState =
        PracticeBuilderUiState(
            scope = PracticeScopeUiModel(PracticeScopeKind.TOPIC, "Coroutines"),
            questionCount = DefaultPracticeQuestionCount,
            questionCountOptions = PracticeQuestionCountOptions,
            levels = levels,
            source = PracticeQuestionSource.ALL,
            sourceOptions = PracticeQuestionSource.entries.map { source ->
                PracticeSourceOption(
                    source = source,
                    isAvailable = source == PracticeQuestionSource.ALL,
                )
            },
            availability = availability,
        )
}
