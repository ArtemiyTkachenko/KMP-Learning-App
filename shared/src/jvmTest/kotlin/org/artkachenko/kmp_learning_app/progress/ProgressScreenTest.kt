package org.artkachenko.kmp_learning_app.progress

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class ProgressScreenTest {
    @Test
    fun loadingStateRenders() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(
                    state = ProgressUiState.Loading,
                    onBack = {},
                    onRetry = {},
                    onHistoryClick = { _, _ -> },
                )
            }
        }

        onNodeWithTag(ProgressLoadingTag).assertIsDisplayed()
        onNodeWithText("Loading progress").assertIsDisplayed()
    }

    @Test
    fun emptyStateRendersGuidance() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(ProgressUiState.Empty, {}, {}, { _, _ -> })
            }
        }
        onNodeWithText(
            "Complete an interview or focused practice session to start tracking your progress.",
        ).assertIsDisplayed()
    }

    @Test
    fun errorStateRendersAndRetries() = runComposeUiTest {
        var retryCount = 0
        setContent {
            MaterialTheme {
                ProgressScreen(ProgressUiState.Error, {}, { retryCount += 1 }, { _, _ -> })
            }
        }
        onNodeWithText("Progress could not be loaded.").assertIsDisplayed()
        onNodeWithText("Retry").performClick()
        assertEquals(1, retryCount)
    }

    @Test
    fun contentRendersOverallWeakAreasTopicsAndFormatting() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(
                    state = contentState(
                        weakAreas = listOf(
                            WeakAreaUiModel(
                                WeakAreaType.SUBTOPIC,
                                "subtopic",
                                "State",
                                null,
                                3,
                                2,
                                66.666,
                            ),
                            WeakAreaUiModel(
                                WeakAreaType.TOPIC,
                                "topic",
                                null,
                                null,
                                5,
                                2,
                                40.0,
                            ),
                        ),
                        topics = listOf(
                            ProgressTopicUiModel("a", "Kotlin", 20, 14, 70.0),
                            ProgressTopicUiModel("b", null, 3, 1, 33.333),
                        ),
                    ),
                    onBack = {},
                    onRetry = {},
                    onHistoryClick = { _, _ -> },
                )
            }
        }

        onNodeWithText("3 completed assessments").assertIsDisplayed()
        onNodeWithText("30 questions answered").assertIsDisplayed()
        onNodeWithText("21 correct answers").assertIsDisplayed()
        onNodeWithText("70% accuracy").assertIsDisplayed()
        onNodeWithText("Weak areas").assertIsDisplayed()
        onNodeWithText("State").assertIsDisplayed()
        onAllNodesWithText("Topic unavailable").assertCountEquals(3)
        onNodeWithText("Kotlin").assertIsDisplayed()
        onNodeWithText("14 / 20 correct").assertExists()
        onNodeWithText("70%").assertExists()
        onNodeWithText("66.7%").assertExists()
        onNodeWithText("33.3%").assertExists()
    }

    @Test
    fun weakSectionIsAbsentWhenNoWeakAreasExist() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(contentState(), {}, {}, { _, _ -> })
            }
        }

        onNodeWithText("Weak areas").assertDoesNotExist()
    }

    @Test
    fun mixedAndFocusedHistoryRenderAndEmitStableTargets() = runComposeUiTest {
        val clicks = mutableListOf<Pair<CompletedAssessmentType, String>>()
        val history = listOf(
            CompletedAttemptUiModel(
                "mixed-id",
                CompletedAssessmentType.MIXED,
                null,
                20,
                15,
                75.0,
                "2026-08-29T00:15:00Z",
            ),
            CompletedAttemptUiModel(
                "focused-id",
                CompletedAssessmentType.FOCUSED,
                FocusedScopeUiModel.Subtopic("Kotlin", "Coroutines"),
                10,
                8,
                80.0,
                "2026-08-28T21:30:00Z",
            ),
        )

        setContent {
            MaterialTheme {
                ProgressScreen(
                    contentState(history = history),
                    onBack = {},
                    onRetry = {},
                    onHistoryClick = { type, id -> clicks += type to id },
                )
            }
        }

        onNodeWithText("Mixed Android Interview").assertIsDisplayed().performClick()
        onNodeWithText("Focused practice").assertIsDisplayed().performClick()
        onNodeWithText("Kotlin · Coroutines").assertIsDisplayed()
        onNodeWithText("2026-08-29T00:15:00Z").assertIsDisplayed()
        assertEquals(
            listOf(
                CompletedAssessmentType.MIXED to "mixed-id",
                CompletedAssessmentType.FOCUSED to "focused-id",
            ),
            clicks,
        )
    }

    @Test
    fun focusedHistoryUsesExplicitMissingScopeFallbacks() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(
                    contentState(
                        history = listOf(
                            CompletedAttemptUiModel(
                                "missing-topic",
                                CompletedAssessmentType.FOCUSED,
                                FocusedScopeUiModel.Topic(null),
                                1,
                                0,
                                0.0,
                                "2026-08-29T00:00:00Z",
                            ),
                            CompletedAttemptUiModel(
                                "missing-subtopic",
                                CompletedAssessmentType.FOCUSED,
                                FocusedScopeUiModel.Subtopic(null, null),
                                1,
                                0,
                                0.0,
                                "2026-08-28T00:00:00Z",
                            ),
                        ),
                    ),
                    onBack = {},
                    onRetry = {},
                    onHistoryClick = { _, _ -> },
                )
            }
        }

        onNodeWithText("Topic unavailable").assertIsDisplayed()
        onNodeWithText("Subtopic unavailable").assertIsDisplayed()
    }

    @Test
    fun percentageFormattingUsesWholeNumbersOrOneDecimalPlace() {
        assertEquals("75", formatProgressPercentage(75.0))
        assertEquals("66.7", formatProgressPercentage(66.666))
    }
}

private fun contentState(
    weakAreas: List<WeakAreaUiModel> = emptyList(),
    topics: List<ProgressTopicUiModel> = emptyList(),
    history: List<CompletedAttemptUiModel> = emptyList(),
): ProgressUiState.Content =
    ProgressUiState.Content(
        completedAttemptCount = 3,
        answeredQuestionCount = 30,
        correctAnswerCount = 21,
        percentage = 70.0,
        weakAreas = weakAreas,
        topics = topics,
        history = history,
    )
