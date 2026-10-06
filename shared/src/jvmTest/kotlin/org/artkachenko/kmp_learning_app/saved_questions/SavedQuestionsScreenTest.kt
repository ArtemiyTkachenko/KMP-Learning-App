package org.artkachenko.kmp_learning_app.saved_questions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
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
import org.artkachenko.kmp_learning_app.assessment_review.QuestionReportAppVersion
import org.artkachenko.kmp_learning_app.assessment_review.ReviewSourceUiModel
import org.artkachenko.kmp_learning_app.assessment_review.questionReportUrl
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
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibility

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
            "Save a question while reviewing Results or Mistakes and it will appear here.",
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
        // A saved collection is a list to browse, so a card opens on request rather than on
        // arrival; the content assertions below are about what the card holds, not when.
        onNodeWithText("Review answer").performClick()
        onNodeWithText("Answer A").assertIsDisplayed()
        onNodeWithText("Answer B").assertIsDisplayed()
        onNodeWithText("Answer C").assertIsDisplayed()
        // Authored correct answers are legitimate Question content.
        onNodeWithText("Correct answer").assertIsDisplayed()
        onNodeWithText("Explanation").assertIsDisplayed()
        onNodeWithText("Authored explanation").assertIsDisplayed()
        onNodeWithText("Source: Source A").assertIsDisplayed()
        onNodeWithTag(savedQuestionRemoveTag("q1")).assertIsDisplayed().assertIsEnabled()
            .assertReadsAsSaved()

        // None of these can be true of a saved Question: it is not tied to an attempt, so no
        // outcome and no selected answer may be shown or invented.
        onNodeWithText("Correct").assertDoesNotExist()
        onNodeWithText("Incorrect").assertDoesNotExist()
        onNodeWithText("Partially correct").assertDoesNotExist()
        onNodeWithText("Your answer").assertDoesNotExist()
    }

    /**
     * A saved collection is browsed, not read through.
     *
     * Every other Question card in the app can be closed; this one had no disclosure at all, so a
     * collection saved over weeks was that many permanently open blocks of options, explanation and
     * sources, and finding one meant scrolling past all the others in full. The default is the one
     * place this differs from a result transcript, which opens what the learner got wrong because
     * they came to read it: here the question text is the browsing key and the rest is on request.
     */
    @Test
    fun savedCardsListTheirQuestionsAndKeepTheDetailBehindADisclosure() = runComposeUiTest {
        setContent {
            AppTheme {
                screen(
                    SavedQuestionsUiState.Content(listOf(availableItem("q1"), availableItem("q2"))),
                )
            }
        }

        // Both questions are readable at once, which is the whole point of the list.
        onNodeWithText("Question q1").assertIsDisplayed()
        onNodeWithText("Question q2").assertIsDisplayed()
        onNodeWithText("Authored explanation").assertDoesNotExist()
        onNodeWithText("Answer A").assertDoesNotExist()

        // Opening one leaves the other closed: the state is the card's, not the screen's.
        onAllNodesWithText("Review answer")[0].performClick()
        onNodeWithText("Answer A").assertIsDisplayed()
        // Scrolled to, because the opened card above it can be taller than the test window.
        onNodeWithText("Review answer").performScrollTo().assertIsDisplayed()
    }

    /** The screen says how much it holds, as the Mistakes queue beside it always has. */
    @Test
    fun theListStatesItsOwnSize() = runComposeUiTest {
        setContent {
            AppTheme {
                screen(
                    SavedQuestionsUiState.Content(
                        listOf(availableItem("q1"), availableItem("q2")),
                    ),
                )
            }
        }

        onNodeWithText("2 saved questions").assertIsDisplayed()
    }

    /** One saved Question is "1 saved question", not "1 saved questions". */
    @Test
    fun aSingleSavedQuestionReadsAsSingular() = runComposeUiTest {
        setContent {
            AppTheme {
                screen(SavedQuestionsUiState.Content(listOf(availableItem("q1"))))
            }
        }

        onNodeWithText("1 saved question").assertIsDisplayed()
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
            ).resolve(listOf(SavedQuestion("q_old", savedAtEpochMillis = 100)), CurriculumVisibility(hiddenTopicIds = emptySet()))
        }
        setContent { AppTheme { screen(SavedQuestionsUiState.Content(items)) } }

        onNodeWithText("Question q_old").assertIsDisplayed()
        onNodeWithText("Review answer").performClick()
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
        // The content is gone; the saved identity is not, and the bookmark is what represents it.
        onNodeWithTag(savedQuestionRemoveTag("q_gone"))
            .assertIsEnabled()
            .assertReadsAsSaved()
            .performClick()

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

        onNodeWithText("Review answer").performClick()
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
        onNodeWithText("Review answer").performClick()
        onNodeWithText("This source could not be opened.").performScrollTo().assertIsDisplayed()
    }

    /** The report goes through the same external-link handler as the sources, for this card's ID. */
    @Test
    fun aSavedQuestionOffersItsOwnReportLink() = runComposeUiTest {
        val openedUrls = mutableListOf<String>()
        setContent {
            AppTheme {
                screen(
                    SavedQuestionsUiState.Content(listOf(availableItem("q1"))),
                    onSourceClick = { openedUrls += it },
                )
            }
        }

        onNodeWithText("Review answer").performClick()
        onNodeWithText("Report a problem").performScrollTo().performClick()

        assertEquals(listOf(questionReportUrl("q1", QuestionReportAppVersion)), openedUrls)
    }

    @Test
    fun aReportFormThatCannotBeOpenedSaysSoOnItsOwnCard() = runComposeUiTest {
        setContent {
            AppTheme {
                screen(
                    SavedQuestionsUiState.Content(listOf(availableItem("q1"))),
                    failedSourceUrl = questionReportUrl("q1", QuestionReportAppVersion),
                )
            }
        }

        onNodeWithText("Review answer").performClick()
        onNodeWithText("The report form could not be opened.").performScrollTo().assertIsDisplayed()
        onNodeWithText("This source could not be opened.").assertDoesNotExist()
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

    /**
     * The collection says saved state the way the rest of the app says it.
     *
     * Every card here was a plain "Remove" text button, so the one screen made entirely of saved
     * questions was the one screen where saved state read as a destructive command. It is now the
     * shared bookmark control the result screens and the Mistakes queue use, in its saved state:
     * the same label and the same toggleable value, reached by this screen's own handle.
     */
    @Test
    fun anAvailableQuestionCarriesTheSharedSavedBookmarkState() = runComposeUiTest {
        var removed: String? = null
        setContent {
            AppTheme {
                screen(
                    SavedQuestionsUiState.Content(listOf(availableItem("q1"))),
                    onRemoveSaved = { removed = it },
                )
            }
        }

        // The question text is dominant and the bookmark sits beside it, trailing: at an ordinary
        // type size the header measures exactly as the plain `Row` it replaced did. Where it stops
        // fitting, the bookmark drops below rather than squeezing the text — see `LargeFontScaleTest`.
        val text = onNodeWithText("Question q1").fetchSemanticsNode().boundsInRoot
        val bookmark = onNodeWithTag(savedQuestionRemoveTag("q1"))
            .assertReadsAsSaved()
            .fetchSemanticsNode().boundsInRoot
        assertTrue(
            bookmark.left >= text.right,
            "The bookmark spans ${bookmark.left}..${bookmark.right} and the text ends at ${text.right}.",
        )

        onNodeWithTag(savedQuestionRemoveTag("q1")).performClick()

        // Pressing it toggles saved state, which on this screen means leaving the collection.
        assertEquals("q1", removed)
    }

    /**
     * Two controls, two meanings, one card.
     *
     * The bookmark is state and the disclosure is content navigation, so neither may stand in for
     * the other: pressing the bookmark must not open the card, and opening the card must not
     * unsave it. Nothing here wraps the card itself in a click either.
     */
    @Test
    fun theBookmarkAndTheDisclosureStayIndependentControls() = runComposeUiTest {
        val removals = mutableListOf<String>()
        setContent {
            AppTheme {
                screen(
                    SavedQuestionsUiState.Content(listOf(availableItem("q1"))),
                    onRemoveSaved = { removals += it },
                )
            }
        }

        // Toggling saved state leaves the card closed.
        onNodeWithTag(savedQuestionRemoveTag("q1")).performClick()
        assertEquals(listOf("q1"), removals)
        onNodeWithText("Answer A").assertDoesNotExist()

        // Opening the card changes no saved state.
        onNodeWithText("Review answer").performClick()
        onNodeWithText("Answer A").assertIsDisplayed()
        assertEquals(listOf("q1"), removals)

        // And the control is still there, still reading as saved, with the card open.
        onNodeWithTag(savedQuestionRemoveTag("q1")).assertReadsAsSaved()
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

/**
 * The three channels the shared bookmark control states saved state in.
 *
 * The visible label is the *action* and the current value is published separately, so a screen
 * reader hears both. Asserting all three here is what keeps this screen's control from drifting from
 * the review surfaces' — the icon fill is the fourth channel and is deliberately not asserted,
 * because an assertion cannot see it.
 */
private fun SemanticsNodeInteraction.assertReadsAsSaved(): SemanticsNodeInteraction = this
    .assert(hasText("Saved"))
    .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
    .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Saved"))

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
    override suspend fun getQuestionsByIds(questionIds: Collection<String>): Map<String, Question> =
        error("Saved Questions read current content, not attempt history.")

    override suspend fun getQuestionsByIdsForCurrentContent(
        questionIds: Collection<String>,
    ): Map<String, Question> =
        questionIds.associateWith { questionId -> question(questionId) }

    private fun question(questionId: String): Question =
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
