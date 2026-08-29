package org.artkachenko.kmp_learning_app.progress

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class ProgressTopicScreenTest {
    @Test
    fun loadingStateRenders() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressTopicScreen(ProgressTopicUiState.Loading, {}, {})
            }
        }

        onNodeWithTag(ProgressTopicLoadingTag).assertIsDisplayed()
        onNodeWithText("Loading topic performance").assertIsDisplayed()
    }

    @Test
    fun emptyStateExplainsThatNothingWasAnsweredYet() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressTopicScreen(ProgressTopicUiState.Empty, {}, {})
            }
        }

        onNodeWithText("No completed answers have been recorded for this topic yet.")
            .assertIsDisplayed()
    }

    @Test
    fun errorStateRendersAndRetries() = runComposeUiTest {
        var retryCount = 0
        setContent {
            MaterialTheme {
                ProgressTopicScreen(ProgressTopicUiState.Error, {}, { retryCount += 1 })
            }
        }

        onNodeWithText("Topic performance could not be loaded.").assertIsDisplayed()
        onNodeWithText("Retry").performClick()
        assertEquals(1, retryCount)
    }

    @Test
    fun contentRendersAggregateAndSubtopicRowsWithWeakMarkers() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressTopicScreen(
                    state = ProgressTopicUiState.Content(
                        topicName = "Kotlin",
                        answeredCount = 20,
                        correctCount = 14,
                        percentage = 70.0,
                        isWeak = false,
                        subtopics = subtopicRows(),
                    ),
                    onBack = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Kotlin").assertIsDisplayed()
        onNodeWithText("14 / 20 correct").assertIsDisplayed()
        onNodeWithText("70%").assertIsDisplayed()
        onNodeWithText("Subtopics").assertExists()
        onNodeWithText("Coroutines").assertExists()
        onNodeWithText("2 / 3 correct").assertExists()
        onNodeWithText("66.7%").assertExists()
        onNodeWithText("Basics").assertExists()
        // Only the weak subtopic is flagged; the aggregate card is not weak here.
        onAllNodesWithText("Weak area").assertCountEquals(1)
    }

    @Test
    fun missingMetadataUsesExplicitFallbackText() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressTopicScreen(
                    state = ProgressTopicUiState.Content(
                        topicName = null,
                        answeredCount = 2,
                        correctCount = 0,
                        percentage = 0.0,
                        isWeak = true,
                        subtopics = listOf(
                            ProgressSubtopicUiModel("unknown", null, 2, 0, 0.0, isWeak = true),
                        ),
                    ),
                    onBack = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Topic unavailable").assertExists()
        onNodeWithText("Subtopic unavailable").assertExists()
    }

    @Test
    fun subtopicSectionIsAbsentWhenTheTopicHasNoObservedSubtopics() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressTopicScreen(
                    state = ProgressTopicUiState.Content(
                        topicName = "Kotlin",
                        answeredCount = 1,
                        correctCount = 1,
                        percentage = 100.0,
                        isWeak = false,
                        subtopics = emptyList(),
                    ),
                    onBack = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Subtopics").assertDoesNotExist()
    }
}

private fun subtopicRows(): List<ProgressSubtopicUiModel> =
    listOf(
        ProgressSubtopicUiModel("coroutines", "Coroutines", 3, 2, 66.666, isWeak = false),
        ProgressSubtopicUiModel("basics", "Basics", 2, 0, 0.0, isWeak = true),
    )
