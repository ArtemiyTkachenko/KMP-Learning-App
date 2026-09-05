package org.artkachenko.kmp_learning_app.topic_study.learning_unit

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class LearningUnitScreenTest {
    @Test
    fun loadingStateRenders() = runComposeUiTest {
        setContent {
            MaterialTheme {
                LearningUnitScreen(LearningUnitUiState.Loading, {}, {}, {}, {})
            }
        }

        onNodeWithTag(LearningUnitLoadingTag).assertIsDisplayed()
        onNodeWithText("Loading learning unit").assertIsDisplayed()
    }

    @Test
    fun notFoundStateSaysTheUnitIsUnavailableWithoutOfferingRetry() = runComposeUiTest {
        setContent {
            MaterialTheme {
                LearningUnitScreen(LearningUnitUiState.NotFound, {}, {}, {}, {})
            }
        }

        onNodeWithText("This learning unit is not available.").assertIsDisplayed()
        // Retrying an identity that names nothing current would only fail the same way.
        onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test
    fun errorStateRendersAndRetries() = runComposeUiTest {
        var retryCount = 0
        setContent {
            MaterialTheme {
                LearningUnitScreen(LearningUnitUiState.Error, {}, {}, {}, { retryCount += 1 })
            }
        }

        onNodeWithText("Learning content could not be loaded.").assertIsDisplayed()
        onNodeWithText("Retry").performClick()
        assertEquals(1, retryCount)
    }

    @Test
    fun contentRendersTheUnitAndItsLessonsAndBackIsAvailable() = runComposeUiTest {
        var backCount = 0
        setContent {
            MaterialTheme {
                LearningUnitScreen(
                    state = content(),
                    onBack = { backCount += 1 },
                    onLessonClick = {},
                    onPracticeUnit = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Thinking in Compose").assertIsDisplayed()
        onNodeWithText("Why Compose changes how UI is written.").assertIsDisplayed()
        onNodeWithText("Lessons").assertIsDisplayed()
        onNodeWithText("Declarative UI").assertIsDisplayed()
        onNodeWithText("Why declarative UI exists.").assertIsDisplayed()
        onNodeWithText("State down, events up").assertIsDisplayed()

        onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backCount)
    }

    @Test
    fun selectingALessonEmitsItsStableIdentity() = runComposeUiTest {
        val clicked = mutableListOf<String>()
        setContent {
            MaterialTheme {
                LearningUnitScreen(
                    state = content(),
                    onBack = {},
                    onLessonClick = { clicked += it },
                    onPracticeUnit = {},
                    onRetry = {},
                )
            }
        }

        // The row is addressed by its Lesson ID and emits that same ID: neither the row's position
        // nor its title is what leaves this screen.
        onNodeWithTag(learningLessonRowTag("lesson_state_down_events_up")).performClick()

        assertEquals(listOf("lesson_state_down_events_up"), clicked)
    }

    /**
     * Authored order is the reading order, so it has to be observable rather than assumed: the row
     * the state listed first has to be the row drawn first.
     */
    @Test
    fun lessonRowsAreLaidOutInTheOrderTheStateGaveThem() = runComposeUiTest {
        setContent {
            MaterialTheme {
                LearningUnitScreen(content(), {}, {}, {}, {})
            }
        }

        val first = onNodeWithTag(learningLessonRowTag("lesson_declarative_ui"))
            .fetchSemanticsNode().positionInRoot.y
        val second = onNodeWithTag(learningLessonRowTag("lesson_state_down_events_up"))
            .fetchSemanticsNode().positionInRoot.y

        assertTrue(first < second, "Authored order was not preserved: $first, $second")
    }

    @Test
    fun aUnitWithNoCurrentLessonsExplainsItselfWithoutInventingRows() = runComposeUiTest {
        setContent {
            MaterialTheme {
                LearningUnitScreen(
                    state = LearningUnitUiState.Content(
                        unitId = "unit_thinking_in_compose",
                        title = "Thinking in Compose",
                        summary = "Why Compose changes how UI is written.",
                        lessons = emptyList(),
                    ),
                    onBack = {},
                    onLessonClick = {},
                    onPracticeUnit = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Thinking in Compose").assertIsDisplayed()
        onNodeWithText("No lessons are currently available in this unit.").assertIsDisplayed()
        // The heading belongs to a list; with nothing to list it would be a dangling label.
        onNodeWithText("Lessons").assertDoesNotExist()
    }

    /**
     * Study flows into practice from the overview, and the action says what it practises: a visible
     * label rather than an icon, so the Material button's own semantics are what a screen reader
     * announces without a second custom description.
     */
    @Test
    fun theUnitOffersPracticeAndTheActionEmitsIt() = runComposeUiTest {
        var practiceCount = 0
        setContent {
            MaterialTheme {
                LearningUnitScreen(
                    state = content(),
                    onBack = {},
                    onLessonClick = {},
                    onPracticeUnit = { practiceCount += 1 },
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Practice this unit").assertIsDisplayed()
        onNodeWithTag(LearningUnitPracticeButtonTag).performClick()

        assertEquals(1, practiceCount)
    }

    /** The action is part of the Unit, not of the reading list, so it survives an empty one. */
    @Test
    fun practiceStaysAvailableOnAUnitWithNoCurrentLessons() = runComposeUiTest {
        setContent {
            MaterialTheme {
                LearningUnitScreen(
                    state = LearningUnitUiState.Content(
                        unitId = "unit_thinking_in_compose",
                        title = "Thinking in Compose",
                        summary = "Why Compose changes how UI is written.",
                        lessons = emptyList(),
                    ),
                    onBack = {},
                    onLessonClick = {},
                    onPracticeUnit = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithTag(LearningUnitPracticeButtonTag).assertIsDisplayed()
    }

    /** Nothing to practise on a screen that is not showing a Unit. */
    @Test
    fun practiceIsAbsentWhileTheUnitIsUnavailable() = runComposeUiTest {
        setContent {
            MaterialTheme {
                LearningUnitScreen(LearningUnitUiState.NotFound, {}, {}, {}, {})
            }
        }

        onNodeWithTag(LearningUnitPracticeButtonTag).assertDoesNotExist()
    }

    private fun content(): LearningUnitUiState.Content =
        LearningUnitUiState.Content(
            unitId = "unit_thinking_in_compose",
            title = "Thinking in Compose",
            summary = "Why Compose changes how UI is written.",
            lessons = listOf(
                LearningLessonItemUiModel(
                    lessonId = "lesson_declarative_ui",
                    title = "Declarative UI",
                    summary = "Why declarative UI exists.",
                ),
                LearningLessonItemUiModel(
                    lessonId = "lesson_state_down_events_up",
                    title = "State down, events up",
                    summary = "How state and events flow.",
                ),
            ),
        )
}
