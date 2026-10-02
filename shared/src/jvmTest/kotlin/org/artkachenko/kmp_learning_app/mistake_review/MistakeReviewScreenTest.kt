package org.artkachenko.kmp_learning_app.mistake_review

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import org.artkachenko.kmp_learning_app.ui.theme.AppWindowSizeClass
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppWindowSizeClass
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AllQuestionLevels
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
    /**
     * What this queue currently holds is the screen's subject, and is announced as a heading.
     *
     * The bar above says which screen this is; this says what is in it. It was the only subject
     * line in the app a screen reader could not jump to, and the only styled `Text` whose colour
     * came from the ambient `LocalContentColor` rather than from a named role — correct inside the
     * shell, black in a preview or an isolated composition.
     */
    @Test
    fun theOutstandingCountIsTheScreensHeading() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(
                        mistakes = listOf(availableMistake("q1", subtopicId = "flows")),
                    ),
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = {},
                    onPracticePreset = {},
                )
            }
        }

        onNodeWithText("1 unresolved mistake to review").assertIsDisplayed().assert(isHeading())
    }

    @Test
    fun aMappedMistakeNavigatesToItsStableLessonIds() = runComposeUiTest {
        val opened = mutableListOf<MistakeStudyLesson>()
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(
                        listOf(
                            availableMistake("q1").copy(
                                studyLesson = MistakeStudyLesson(
                                    unitId = "unit-compose",
                                    lessonId = "lesson-state",
                                    title = "State hoisting",
                                ),
                            ),
                        ),
                    ),
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = {},
                    onPracticePreset = {},
                    onStudyLesson = opened::add,
                )
            }
        }

        onNodeWithText("Review lesson: State hoisting").performScrollTo().performClick()

        assertEquals(
            listOf(MistakeStudyLesson("unit-compose", "lesson-state", "State hoisting")),
            opened,
        )
    }

    @Test
    fun queueSummaryStartsOnePracticeForEveryAvailableMistake() = runComposeUiTest {
        val configs = mutableListOf<AssessmentConfig.Focused>()
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(
                        listOf(
                            availableMistake("q1", subtopicId = "flows"),
                            availableMistake("q2", subtopicId = "coroutines"),
                            UnresolvedMistake("gone", "attempt", ReviewQuestionItem.Missing("gone")),
                        ),
                    ),
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = {},
                    onPracticePreset = {},
                    onStartPractice = configs::add,
                )
            }
        }

        onNodeWithText("3 unresolved mistakes to review").assertIsDisplayed()
        // Why the button offers fewer than the count above it. Without this line the learner reads
        // "3 unresolved mistakes" over an offer to practise two, with nothing accounting for the
        // third.
        onNodeWithText("1 of 3 are no longer in the curriculum and cannot be practised.")
            .assertIsDisplayed()
        onNodeWithText("Practice 2 mistakes").performClick()

        assertEquals(
            listOf(
                AssessmentConfig.Focused(
                    scope = AssessmentScope.Subtopics(setOf("flows", "coroutines")),
                    questionCount = 2,
                    levels = AllQuestionLevels,
                    source = PracticeQuestionSource.UNRESOLVED_MISTAKES,
                ),
            ),
            configs,
        )
    }

    /**
     * The expanded arrangement is a separate branch, so it is the one place an action could be
     * dropped while the compact screen still shows it: the remediation block and its practice
     * action must sit in their own pane, and the queue in the other, still operable.
     */
    @Test
    fun theExpandedQueueKeepsRemediationAndTheQueueInTheirOwnPanes() =
        runSkikoComposeUiTest(size = Size(ExpandedWidth.value, ExpandedHeight.value)) {
            val configs = mutableListOf<AssessmentConfig.Focused>()
            setContent {
                MaterialTheme {
                    CompositionLocalProvider(
                        LocalAppWindowSizeClass provides AppWindowSizeClass.Expanded,
                    ) {
                        Box(Modifier.size(ExpandedWidth, ExpandedHeight)) {
                            MistakeReviewScreen(
                                state = MistakeReviewUiState.Content(
                                    listOf(availableMistake("q1"), availableMistake("q2")),
                                ),
                                onRetry = {},
                                onBrowseTopics = {},
                                onSourceClick = {},
                                onPracticePreset = {},
                                onStartPractice = configs::add,
                            )
                        }
                    }
                }
            }

            onNode(
                hasText("2 unresolved mistakes to review") and
                    hasAnyAncestor(hasTestTag(MistakeRemediationPaneTag)),
            ).assertIsDisplayed()
            onNode(
                hasText("Question q1") and hasAnyAncestor(hasTestTag(MistakeQueuePaneTag)),
            ).assertIsDisplayed()
            onNode(
                hasText("Practice 2 mistakes") and
                    hasAnyAncestor(hasTestTag(MistakeRemediationPaneTag)),
            ).performClick()

            assertEquals(2, configs.single().questionCount)
        }

    /** With nothing missing there is no discrepancy, so the screen says nothing about one. */
    @Test
    fun aFullyPractisableQueueCarriesNoUnavailableNotice() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(
                        listOf(
                            availableMistake("q1", subtopicId = "flows"),
                            availableMistake("q2", subtopicId = "coroutines"),
                        ),
                    ),
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = {},
                    onPracticePreset = {},
                )
            }
        }

        onNodeWithText("no longer in the curriculum", substring = true).assertDoesNotExist()
        onNodeWithText("Practice 2 mistakes").assertIsDisplayed()
    }

    @Test
    fun missingQuestionsAreCountedButDoNotCreateAFabricatedPracticeScope() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(
                        listOf(UnresolvedMistake("gone", "attempt", ReviewQuestionItem.Missing("gone"))),
                    ),
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = {},
                    onPracticePreset = {},
                )
            }
        }

        onNodeWithText("1 unresolved mistake to review").assertIsDisplayed()
        onNodeWithTag(MistakeReviewPracticeAllTag).assertDoesNotExist()
    }

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
        // Rendered by the shared ReviewQuestionCard rather than a mistake-specific copy — but
        // without its "Incorrect" badge: the heading above already says every entry here is an
        // unresolved mistake, and repeating that on every card is a wall of red saying nothing the
        // learner did not know when they opened the screen.
        onNodeWithText("Incorrect").assertDoesNotExist()
        onNodeWithText("✕ Incorrectly selected").assertExists()
        onNodeWithText("✓ Correct answer").assertExists()
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
        // Scrolled through the list rather than reached directly: the queue is a `LazyColumn`, so
        // a second full review card below the fold is not composed at all and `performScrollTo`
        // has nothing to scroll to. This asserts what the test is named for — that q1 is in the
        // queue, after q3 — without depending on how much of it happens to fit the viewport.
        onNodeWithTag(MistakeQueuePaneTag).performScrollToNode(hasText("Question q1"))
        onNodeWithText("Question q1").assertIsDisplayed()
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
    fun aSavedUnresolvedMistakeReadsAsSaved() = runComposeUiTest {
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

        onNodeWithText("Saved").assertIsDisplayed()
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

    /**
     * The count and its unit are one announcement, not two fragments.
     *
     * The subject line became a figure over a unit, so it is now two `Text` nodes where it was one
     * sentence. What has to hold is the information and the semantics, not the component: heading
     * navigation still lands on the whole sentence, and a screen reader does not hear "3" followed
     * by an unrelated "unresolved mistakes".
     */
    @Test
    fun theOutstandingCountIsAnnouncedAsOneSentenceAndStillAHeading() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(
                        listOf(availableMistake("q1"), availableMistake("q2")),
                    ),
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = {},
                    onPracticePreset = {},
                )
            }
        }

        onNodeWithText("2 unresolved mistakes to review").assertIsDisplayed().assert(isHeading())
        // The figure's two halves are cleared, so neither is reachable as a node of its own.
        onNodeWithText("unresolved mistakes").assertDoesNotExist()
    }

    /**
     * The block the screen leads with is a surface, and the queue is still below it.
     *
     * The remediation offer was four loose pieces of type on the page background, outranked by
     * every review card beneath it. It is now the screen's one level-2 surface. Asserted on the
     * handle and on the information either side of it rather than on tone or elevation, which are
     * not things a semantics assertion can see.
     */
    @Test
    fun theRemediationBlockLeadsTheQueue() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(listOf(availableMistake("q1"))),
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = {},
                    onPracticePreset = {},
                )
            }
        }

        onNodeWithTag(MistakeRemediationSurfaceTag).assertIsDisplayed()
        onNodeWithText("1 unresolved mistake to review").assertIsDisplayed()
        onNodeWithText(
            "Questions stay here until your most recent completed answer is correct.",
        ).assertIsDisplayed()
        onNodeWithTag(MistakeReviewPracticeAllTag).assertIsDisplayed()
        onNodeWithText("Question q1").assertIsDisplayed()
    }

    /**
     * The entrance never gates the one control the screen exists to offer.
     *
     * The block now arrives through an `AnimatedVisibility`, and a reveal that had to finish before
     * Practice could be pressed would be a motion defect rather than a motion flourish. The click
     * is driven here without waiting for the animation to settle.
     */
    @Test
    fun practiceIsClickableWithoutWaitingForTheEntrance() = runComposeUiTest {
        val started = mutableListOf<AssessmentConfig.Focused>()
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(listOf(availableMistake("q1"))),
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = {},
                    onPracticePreset = {},
                    onStartPractice = { started += it },
                )
            }
        }

        onNodeWithTag(MistakeReviewPracticeAllTag).performClick()

        assertEquals(1, started.size)
        assertEquals(1, started.single().questionCount)
    }

    /**
     * A singular queue reads as one mistake.
     *
     * The count string was not a plural, so a learner with one outstanding Question was told they
     * had "1 unresolved mistakes to review" — on the screen, and on the Progress row that shares
     * the string.
     */
    @Test
    fun aSingleOutstandingMistakeIsStatedInTheSingular() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(listOf(availableMistake("q1"))),
                    onRetry = {},
                    onBrowseTopics = {},
                    onSourceClick = {},
                    onPracticePreset = {},
                )
            }
        }

        onNodeWithText("1 unresolved mistake to review").assertIsDisplayed()
        onNodeWithText("1 unresolved mistakes to review").assertDoesNotExist()
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

private val ExpandedWidth = 1280.dp
private val ExpandedHeight = 800.dp
