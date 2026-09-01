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
                        coverage = ProgressCoverageUiModel(12, 30, 40.0),
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
    fun currentCoverageSitsBesideAllTimeCorrectnessOnBothScopes() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressTopicScreen(
                    state = ProgressTopicUiState.Content(
                        topicName = "Kotlin",
                        answeredCount = 20,
                        correctCount = 14,
                        percentage = 70.0,
                        isWeak = false,
                        coverage = ProgressCoverageUiModel(12, 30, 40.0),
                        subtopics = subtopicRows(),
                    ),
                    onBack = {},
                    onRetry = {},
                )
            }
        }

        // The two fractions on each card measure different things, so the coverage line names its
        // denominator rather than leaving two bare numbers side by side.
        onNodeWithText("14 / 20 correct").assertIsDisplayed()
        onNodeWithText("12 of 30 current questions explored").assertIsDisplayed()
        onNodeWithText("2 / 3 correct").assertExists()
        onNodeWithText("6 of 12 current questions explored").assertExists()
        // Zero current coverage does not erase the Subtopic's historical performance.
        onNodeWithText("0 / 2 correct").assertExists()
        onNodeWithText("0 of 4 current questions explored").assertExists()
    }

    @Test
    fun aScopeWithNoCurrentQuestionsSimplyOmitsCoverage() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressTopicScreen(
                    state = ProgressTopicUiState.Content(
                        topicName = "Retired Kotlin",
                        answeredCount = 4,
                        correctCount = 3,
                        percentage = 75.0,
                        isWeak = false,
                        coverage = null,
                        subtopics = listOf(
                            ProgressSubtopicUiModel("old", "Old Coroutines", 2, 1, 50.0, false),
                        ),
                    ),
                    onBack = {},
                    onRetry = {},
                )
            }
        }

        // Everything the learner answered is still reported; only the coverage line is absent.
        onNodeWithText("3 / 4 correct").assertExists()
        onNodeWithText("1 / 2 correct").assertExists()
        onNodeWithText("current questions explored", substring = true).assertDoesNotExist()
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
        ProgressSubtopicUiModel(
            "coroutines",
            "Coroutines",
            3,
            2,
            66.666,
            isWeak = false,
            coverage = ProgressCoverageUiModel(6, 12, 50.0),
        ),
        // Answered entirely on questions that are no longer ACTIVE: real historical performance,
        // no current coverage.
        ProgressSubtopicUiModel(
            "basics",
            "Basics",
            2,
            0,
            0.0,
            isWeak = true,
            coverage = ProgressCoverageUiModel(0, 4, 0.0),
        ),
    )
