package org.artkachenko.kmp_learning_app.saved_questions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.artkachenko.kmp_learning_app.assessment_review.ReviewSourceUiModel
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme

@OptIn(ExperimentalTestApi::class)
internal class SavedQuestionsScreenTest {
    @Test
    fun loadingUsesTheSharedScreenLoadingTreatment() = runComposeUiTest {
        setContent { AppTheme { screen(SavedQuestionsUiState.Loading) } }

        onNodeWithTag(SavedQuestionsLoadingTag).assertIsDisplayed()
        onNodeWithText("Loading saved questions").assertIsDisplayed()
    }

    @Test
    fun theEmptyStateSaysWhereSavingHappensAndOffersAWayBack() = runComposeUiTest {
        var browsedTopics = 0
        setContent {
            AppTheme { screen(SavedQuestionsUiState.Empty, onBrowseTopics = { browsedTopics += 1 }) }
        }

        onNodeWithText("No saved questions yet.").assertIsDisplayed()
        onNodeWithText(
            "Save a question while reviewing an assessment or your mistakes and it will appear here.",
        ).assertIsDisplayed()
        // An empty collection is not a failure, so it offers a way forward rather than a Retry.
        onNodeWithText("Retry").assertDoesNotExist()
        onNodeWithText("Browse topics").performClick()

        assertEquals(1, browsedTopics)
    }

    @Test
    fun errorOffersRetry() = runComposeUiTest {
        var retries = 0
        setContent {
            AppTheme { screen(SavedQuestionsUiState.Error, onRetry = { retries += 1 }) }
        }

        onNodeWithText("Saved questions could not be loaded.").assertIsDisplayed()
        onNodeWithText("Retry").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun anAvailableQuestionShowsAuthoredContentAndNoAssessmentOutcome() = runComposeUiTest {
        setContent {
            AppTheme {
                screen(
                    SavedQuestionsUiState.Content(listOf(availableItem("q1"))),
                )
            }
        }

        onNodeWithText("Question q1").assertIsDisplayed()
        onNodeWithText("Answer A").assertIsDisplayed()
        onNodeWithText("Answer B").assertIsDisplayed()
        onNodeWithText("Answer C").assertIsDisplayed()
        // Authored correct answers are legitimate Question content.
        onNodeWithText("Correct answer").assertIsDisplayed()
        onNodeWithText("Explanation").assertIsDisplayed()
        onNodeWithText("Authored explanation").assertIsDisplayed()
        onNodeWithText("Source: Source A").assertIsDisplayed()
        onNodeWithTag(savedQuestionRemoveTag("q1")).assertIsDisplayed().assertIsEnabled()

        // None of these can be true of a saved Question: it is not tied to an attempt, so no
        // outcome and no selected answer may be shown or invented.
        onNodeWithText("Correct").assertDoesNotExist()
        onNodeWithText("Incorrect").assertDoesNotExist()
        onNodeWithText("Partially correct").assertDoesNotExist()
        onNodeWithText("Your answer").assertDoesNotExist()
    }

    /**
     * A DEPRECATED Question resolves to ordinary content, so it must render as ordinary content:
     * the acceptance criterion is that retired Questions stay reviewable.
     */
    @Test
    fun deprecatedContentRendersExactlyLikeCurrentContent() = runComposeUiTest {
        val items = runBlocking {
            SavedQuestionContentResolver(
                ScreenCurriculumRepository(status = ContentStatus.DEPRECATED),
            ).resolve(listOf(SavedQuestion("q_old", savedAtEpochMillis = 100)))
        }
        setContent { AppTheme { screen(SavedQuestionsUiState.Content(items)) } }

        onNodeWithText("Question q_old").assertIsDisplayed()
        onNodeWithText("Answer A").assertIsDisplayed()
        onNodeWithText("Correct answer").assertIsDisplayed()
        onNodeWithText("Authored explanation").assertIsDisplayed()
        onNodeWithText("Source: Source A").assertIsDisplayed()
        onNodeWithText("Question q_old is no longer available.").assertDoesNotExist()
    }

    @Test
    fun missingContentIsStatedPlainlyAndStaysRemovable() = runComposeUiTest {
        var removed: String? = null
        setContent {
            AppTheme {
                screen(
                    SavedQuestionsUiState.Content(
                        listOf(SavedQuestionItem.Missing(SavedQuestion("q_gone", 100))),
                    ),
                    onRemoveSaved = { removed = it },
                )
            }
        }

        onNodeWithText("Question q_gone is no longer available.").assertIsDisplayed()
        // Nothing is invented to fill the card.
        onNodeWithText("Explanation").assertDoesNotExist()
        onNodeWithText("Correct answer").assertDoesNotExist()

        // Unlike a result screen's placeholder, this identity is already saved, so it must be
        // possible to get rid of it.
        onNodeWithTag(savedQuestionRemoveTag("q_gone")).assertIsEnabled().performClick()

        assertEquals("q_gone", removed)
    }

    @Test
    fun removingAQuestionReportsItsOwnStableId() = runComposeUiTest {
        val removals = mutableListOf<String>()
        setContent {
            AppTheme {
                screen(
                    SavedQuestionsUiState.Content(listOf(availableItem("q1"), availableItem("q2"))),
                    onRemoveSaved = { removals += it },
                )
            }
        }

        onNodeWithTag(savedQuestionRemoveTag("q2")).performScrollTo().performClick()

        assertEquals(listOf("q2"), removals)
    }

    @Test
    fun aSourceOpensIndependentlyOfTheRemovalAction() = runComposeUiTest {
        val openedUrls = mutableListOf<String>()
        val removals = mutableListOf<String>()
        setContent {
            AppTheme {
                screen(
                    SavedQuestionsUiState.Content(listOf(availableItem("q1"))),
                    onRemoveSaved = { removals += it },
                    onSourceClick = { openedUrls += it },
                )
            }
        }

        onNodeWithText("Source: Source A").performScrollTo().performClick()

        assertEquals(listOf("https://example.com/q1/a"), openedUrls)
        assertTrue(removals.isEmpty())
    }

    @Test
    fun aSourceThatCannotBeOpenedSaysSoOnItsOwnCard() = runComposeUiTest {
        setContent {
            AppTheme {
                screen(
                    SavedQuestionsUiState.Content(listOf(availableItem("q1"))),
                    failedSourceUrl = "https://example.com/q1/a",
                )
            }
        }

        // The same message the result screens show, beside the link that failed.
        onNodeWithText("This source could not be opened.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun aPendingRemovalDisablesOnlyThatQuestionsAction() = runComposeUiTest {
        setContent {
            AppTheme {
                screen(
                    SavedQuestionsUiState.Content(
                        items = listOf(availableItem("q1"), availableItem("q2")),
                        pendingQuestionIds = setOf("q1"),
                    ),
                )
            }
        }

        onNodeWithTag(savedQuestionRemoveTag("q1")).assertIsNotEnabled()
        onNodeWithTag(savedQuestionRemoveTag("q2")).performScrollTo().assertIsEnabled()
    }

    @Test
    fun questionsAreRenderedInTheOrderTheyWereSupplied() = runComposeUiTest {
        setContent {
            AppTheme {
                Box(Modifier.size(400.dp, 2000.dp)) {
                    screen(
                        SavedQuestionsUiState.Content(
                            listOf(
                                availableItem("q3"),
                                SavedQuestionItem.Missing(SavedQuestion("q2", 200)),
                                availableItem("q1"),
                            ),
                        ),
                    )
                }
            }
        }

        // The saved order is the browsing order: a missing entry keeps its place rather than
        // being grouped at the end.
        val first = onNodeWithText("Question q3").fetchSemanticsNode().boundsInRoot.top
        val second = onNodeWithText("Question q2 is no longer available.")
            .fetchSemanticsNode().boundsInRoot.top
        val third = onNodeWithText("Question q1").fetchSemanticsNode().boundsInRoot.top

        assertTrue(first < second, "q3 should precede q2")
        assertTrue(second < third, "q2 should precede q1")
    }
}

@androidx.compose.runtime.Composable
private fun screen(
    state: SavedQuestionsUiState,
    onRetry: () -> Unit = {},
    onBrowseTopics: () -> Unit = {},
    onRemoveSaved: (String) -> Unit = {},
    onSourceClick: (String) -> Unit = {},
    failedSourceUrl: String? = null,
) {
    SavedQuestionsScreen(
        state = state,
        onBack = {},
        onRetry = onRetry,
        onBrowseTopics = onBrowseTopics,
        onRemoveSaved = onRemoveSaved,
        onSourceClick = onSourceClick,
        failedSourceUrl = failedSourceUrl,
    )
}

private fun availableItem(questionId: String): SavedQuestionItem.Available =
    SavedQuestionItem.Available(
        savedQuestion = SavedQuestion(questionId, savedAtEpochMillis = 100),
        question = SavedQuestionContentUiModel(
            questionId = questionId,
            text = "Question $questionId",
            answers = listOf(
                SavedQuestionAnswerUiModel("${questionId}_a", "Answer A", isCorrectAnswer = true),
                SavedQuestionAnswerUiModel("${questionId}_b", "Answer B", isCorrectAnswer = false),
                SavedQuestionAnswerUiModel("${questionId}_c", "Answer C", isCorrectAnswer = false),
            ),
            explanation = "Authored explanation",
            sources = listOf(ReviewSourceUiModel("Source A", "https://example.com/$questionId/a")),
        ),
    )

/** Resolves any ID to one Question with the given lifecycle status. */
private class ScreenCurriculumRepository(
    private val status: ContentStatus,
) : CurriculumRepository {
    override suspend fun getQuestionById(questionId: String): Question =
        Question(
            id = questionId,
            topicId = "kotlin",
            subtopicId = "coroutines",
            text = "Question $questionId",
            answers = listOf(
                AnswerOption("${questionId}_a", "Answer A"),
                AnswerOption("${questionId}_b", "Answer B"),
            ),
            selectionMode = AnswerSelectionMode.SINGLE,
            level = QuestionLevel.FOUNDATION,
            correctAnswerIds = listOf("${questionId}_a"),
            explanation = "Authored explanation",
            sources = listOf(SourceReference("Source A", "https://example.com/$questionId/a")),
            status = status,
        )

    override suspend fun getActiveTopics(): List<Topic> = error("ACTIVE lookup must not be used.")
    override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestions(): List<Question> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsByTopicAndLevels(
        topicId: String,
        levels: Set<QuestionLevel>,
    ): List<Question> = error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsBySubtopicAndLevels(
        subtopicId: String,
        levels: Set<QuestionLevel>,
    ): List<Question> = error("ACTIVE lookup must not be used.")
    override suspend fun getTopicById(topicId: String): Topic? = error("Topic lookup is not needed.")
    override suspend fun getSubtopicById(subtopicId: String): Subtopic? =
        error("Subtopic lookup is not needed.")
}
