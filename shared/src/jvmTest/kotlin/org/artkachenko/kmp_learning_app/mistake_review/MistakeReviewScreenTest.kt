package org.artkachenko.kmp_learning_app.mistake_review

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment_review.ReviewAnswerUiModel
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionUiModel
import org.artkachenko.kmp_learning_app.assessment_review.ReviewSourceUiModel
import org.artkachenko.kmp_learning_app.assessment_review.reviewQuestionSaveTag
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestion
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionsState

@OptIn(ExperimentalTestApi::class)
internal class MistakeReviewScreenTest {
    @Test
    fun loadingStateRenders() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(MistakeReviewUiState.Loading, {}, {}, {}, {}, {})
            }
        }

        onNodeWithTag(MistakeReviewLoadingTag).assertIsDisplayed()
        onNodeWithText("Loading mistakes").assertIsDisplayed()
    }

    @Test
    fun emptyStateExplainsTheResolutionRule() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(MistakeReviewUiState.Empty, {}, {}, {}, {}, {})
            }
        }

        onNodeWithText("No unresolved mistakes.").assertIsDisplayed()
        onNodeWithText(
            "Questions disappear from this list after your most recent completed answer is correct.",
        ).assertIsDisplayed()
    }

    @Test
    fun errorStateRendersAndRetries() = runComposeUiTest {
        var retryCount = 0
        setContent {
            MaterialTheme {
                MistakeReviewScreen(MistakeReviewUiState.Error, {}, { retryCount += 1 }, {}, {}, {})
            }
        }

        onNodeWithText("Mistakes could not be loaded.").assertIsDisplayed()
        onNodeWithText("Retry").performClick()
        assertEquals(1, retryCount)
    }

    @Test
    fun availableMistakeReusesTheSharedReviewCard() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(listOf(availableMistake("q1"))),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = {},
                    onPracticePreset = {},
                )
            }
        }

        onNodeWithText("Questions stay here until your most recent completed answer is correct.")
            .assertIsDisplayed()
        onNodeWithText("Question q1").assertIsDisplayed()
        // Rendered by the shared ReviewQuestionCard rather than a mistake-specific copy.
        onNodeWithText("Incorrect").assertExists()
        onNodeWithText("Your answer").assertExists()
        onNodeWithText("Correct answer").assertExists()
        onNodeWithText("Explanation").performScrollTo().assertIsDisplayed()
        onNodeWithText("Explanation for q1").assertExists()
        onNodeWithText("Source: Kotlin docs").assertExists()
    }

    @Test
    fun missingMistakeReusesTheSharedMissingQuestionComponent() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(
                        listOf(
                            UnresolvedMistake("gone", "attempt", ReviewQuestionItem.Missing("gone")),
                        ),
                    ),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = {},
                    onPracticePreset = {},
                )
            }
        }

        onNodeWithText("Question gone is no longer available.").assertIsDisplayed()
    }

    @Test
    fun multipleMistakesRenderInQueueOrder() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(
                        listOf(availableMistake("q3"), availableMistake("q1")),
                    ),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = {},
                    onPracticePreset = {},
                )
            }
        }

        onNodeWithText("Question q3").assertIsDisplayed()
        onNodeWithText("Question q1").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sourceClickEmitsTheExactUrl() = runComposeUiTest {
        val clicked = mutableListOf<String>()
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(listOf(availableMistake("q1"))),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = clicked::add,
                    onPracticePreset = {},
                )
            }
        }

        onNodeWithText("Source: Kotlin docs").performScrollTo().performClick()

        assertEquals(listOf("https://kotlinlang.org/q1"), clicked)
    }

    @Test
    fun sourceOpenFailureRemainsVisible() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(listOf(availableMistake("q1"))),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = {},
                    onPracticePreset = {},
                    failedSourceUrl = "https://kotlinlang.org/q1",
                )
            }
        }

        onNodeWithText("This source could not be opened.").performScrollTo().assertIsDisplayed()
    }

    /**
     * The entry supplies the scope and nothing else. Its own Question ID does not travel, because
     * the shortcut asks for unresolved practice in that Subtopic, not for this Question again.
     */
    @Test
    fun anUnresolvedMistakeOffersScopedMistakePracticeForItsOwnSubtopic() = runComposeUiTest {
        val presets = mutableListOf<PracticePreset>()
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(
                        listOf(availableMistake("q1", subtopicId = "kotlin_flows")),
                    ),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = {},
                    onPracticePreset = presets::add,
                )
            }
        }

        onNodeWithTag(mistakePracticeShortcutTag("q1")).performScrollTo().performClick()

        assertEquals(
            listOf(
                PracticePreset(
                    scope = AssessmentScope.Subtopic("kotlin_flows"),
                    source = PracticeQuestionSource.UNRESOLVED_MISTAKES,
                ),
            ),
            presets,
        )
    }

    /**
     * Review content the curriculum no longer holds cannot name a current Subtopic, so the entry
     * stays a plain "no longer available" note rather than acquiring a shortcut to an invented one.
     */
    @Test
    fun missingReviewContentInventsNoPracticeScope() = runComposeUiTest {
        val presets = mutableListOf<PracticePreset>()
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(
                        listOf(UnresolvedMistake("gone", "attempt", ReviewQuestionItem.Missing("gone"))),
                    ),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = {},
                    onPracticePreset = presets::add,
                )
            }
        }

        onNodeWithTag(mistakePracticeShortcutTag("gone")).assertDoesNotExist()
        onNodeWithText("Practice unresolved mistakes in this subtopic").assertDoesNotExist()
        assertEquals(emptyList(), presets)
    }

    /** The queue still renders its explanations; the shortcut is an addition, not a replacement. */
    @Test
    fun theShortcutDoesNotDisplaceTheReviewContent() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(listOf(availableMistake("q1"))),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = {},
                    onPracticePreset = {},
                )
            }
        }

        onNodeWithText("Question q1").assertIsDisplayed()
        onNodeWithText("Explanation for q1").performScrollTo().assertIsDisplayed()
        onNodeWithTag(mistakePracticeShortcutTag("q1")).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun anUnresolvedMistakeOffersSaveAndReportsItsExactQuestionId() = runComposeUiTest {
        val toggled = mutableListOf<String>()
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(listOf(availableMistake("q1"))),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = {},
                    onPracticePreset = {},
                    savedQuestions = SavedQuestionsState.Loaded(emptyList()),
                    onToggleSaved = toggled::add,
                )
            }
        }

        onNodeWithText("Save").assertIsDisplayed()
        onNodeWithTag(reviewQuestionSaveTag("q1")).performScrollTo().performClick()
        assertEquals(listOf("q1"), toggled)
    }

    @Test
    fun aSavedUnresolvedMistakeOffersUnsave() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(listOf(availableMistake("q1"))),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = {},
                    onPracticePreset = {},
                    savedQuestions = SavedQuestionsState.Loaded(
                        listOf(SavedQuestion("q1", savedAtEpochMillis = 1_000)),
                    ),
                    onToggleSaved = {},
                )
            }
        }

        onNodeWithText("Unsave").assertIsDisplayed()
        onNodeWithText("Save").assertDoesNotExist()
    }

    @Test
    fun aMissingUnresolvedMistakeStaysAPlaceholderWithNoSaveAction() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(
                        listOf(UnresolvedMistake("gone", "attempt", ReviewQuestionItem.Missing("gone"))),
                    ),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = {},
                    onPracticePreset = {},
                    savedQuestions = SavedQuestionsState.Loaded(emptyList()),
                    onToggleSaved = {},
                )
            }
        }

        onNodeWithText("Question gone is no longer available.").assertIsDisplayed()
        onNodeWithTag(reviewQuestionSaveTag("gone")).assertDoesNotExist()
        onNodeWithText("Save").assertDoesNotExist()
    }

    /** Two independent actions on one entry: saving is not practising, and neither replaces the other. */
    @Test
    fun savingAndTheScopedPracticeShortcutRemainSeparatelyClickable() = runComposeUiTest {
        val toggled = mutableListOf<String>()
        val presets = mutableListOf<PracticePreset>()
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(
                        listOf(availableMistake("q1", subtopicId = "kotlin_flows")),
                    ),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = {},
                    onPracticePreset = presets::add,
                    savedQuestions = SavedQuestionsState.Loaded(emptyList()),
                    onToggleSaved = toggled::add,
                )
            }
        }

        onNodeWithTag(reviewQuestionSaveTag("q1")).performScrollTo().performClick()
        assertEquals(listOf("q1"), toggled)
        assertEquals(emptyList(), presets)

        onNodeWithTag(mistakePracticeShortcutTag("q1")).performScrollTo().performClick()
        assertEquals(
            listOf(
                PracticePreset(
                    scope = AssessmentScope.Subtopic("kotlin_flows"),
                    source = PracticeQuestionSource.UNRESOLVED_MISTAKES,
                ),
            ),
            presets,
        )
        assertEquals(listOf("q1"), toggled)
    }

    /** Unreadable saved state costs the affordance, not the queue. */
    @Test
    fun unavailableSavedStateLeavesTheQueueIntact() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(listOf(availableMistake("q1"))),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = {},
                    onPracticePreset = {},
                    savedQuestions = SavedQuestionsState.Error,
                    onToggleSaved = {},
                )
            }
        }

        onNodeWithText("Question q1").assertIsDisplayed()
        onNodeWithText("Explanation for q1").performScrollTo().assertIsDisplayed()
        onNodeWithTag(mistakePracticeShortcutTag("q1")).performScrollTo().assertIsDisplayed()
        onNodeWithTag(reviewQuestionSaveTag("q1")).assertDoesNotExist()
    }
}

private fun availableMistake(
    questionId: String,
    subtopicId: String = "kotlin_coroutines",
): UnresolvedMistake =
    UnresolvedMistake(
        questionId = questionId,
        sourceAttemptId = "attempt",
        reviewItem = ReviewQuestionItem.Available(
            ReviewQuestionUiModel(
                questionId = questionId,
                topicId = "kotlin",
                subtopicId = subtopicId,
                text = "Question $questionId",
                isCorrect = false,
                answers = listOf(
                    ReviewAnswerUiModel("${questionId}_a", "Answer A", false, isCorrectAnswer = true),
                    ReviewAnswerUiModel("${questionId}_b", "Answer B", true, isCorrectAnswer = false),
                ),
                explanation = "Explanation for $questionId",
                sources = listOf(
                    ReviewSourceUiModel("Kotlin docs", "https://kotlinlang.org/$questionId"),
                ),
            ),
        ),
    )
