package org.artkachenko.kmp_learning_app.progress

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset
import org.artkachenko.kmp_learning_app.learning_progress.ReadinessGap
import org.artkachenko.kmp_learning_app.learning_progress.ReadinessStatus
import org.artkachenko.kmp_learning_app.learning_progress.ReadinessTarget
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras

@OptIn(ExperimentalTestApi::class)
internal class ProgressReadinessScreenTest {

    @Test
    fun withNoTargetTheSectionShowsTheChooserAndNoStatuses() = runComposeUiTest {
        var chosen: ReadinessTarget? = null
        setContent {
            MaterialTheme {
                dashboard(ProgressReadinessUiModel.TargetNotChosen, onChooseTarget = { chosen = it })
            }
        }

        scrollTo(ProgressReadinessChooserTag)
        onNodeWithText("What are you preparing for?").assertIsDisplayed()
        onNodeWithText("Junior").assertIsDisplayed()
        onNodeWithText("Mid-level").assertIsDisplayed()
        onNodeWithText("Senior").assertIsDisplayed()
        onNodeWithTag(ProgressReadinessGroupTag).assertDoesNotExist()
        onNodeWithTag(progressReadinessStatusTag(ReadinessStatus.READY)).assertDoesNotExist()

        onNodeWithTag(progressReadinessTargetTag(ReadinessTarget.SENIOR)).performClick()
        assertEquals(ReadinessTarget.SENIOR, chosen)
    }

    @Test
    fun eachStatusRowStatesItsVerdictInWordsWithItsEvidenceAndFirstMissingItem() = runComposeUiTest {
        setContent {
            MaterialTheme { dashboard(threeStatuses()) }
        }

        scrollTo(ProgressReadinessGroupTag)
        // The header: the target and a count, never a score.
        onNodeWithText("Preparing for Senior").assertIsDisplayed()
        onNodeWithText("1 of 3 topics ready").assertIsDisplayed()

        onNode(hasTestTag(progressReadinessRowTag("android")) and hasText("Ready", substring = true))
            .assertIsDisplayed()
        onNode(
            hasTestTag(progressReadinessRowTag("compose")) and
                hasText("Getting there", substring = true) and
                hasText("Review 2 due mistakes", substring = true),
        ).assertIsDisplayed()
        onNode(
            hasTestTag(progressReadinessRowTag("coroutines")) and
                hasText("Not enough evidence yet", substring = true) and
                hasText("3 answers at this level", substring = true) and
                hasText("Answer 2 more questions", substring = true),
        ).assertIsDisplayed()
    }

    @Test
    fun aSeniorVerdictWithoutAdvancedQuestionsSaysSoOnTheRow() = runComposeUiTest {
        setContent {
            MaterialTheme { dashboard(threeStatuses()) }
        }

        scrollTo(ProgressReadinessGroupTag)
        onNode(
            hasTestTag(progressReadinessRowTag("android")) and
                hasText("No Advanced questions in this topic yet", substring = true),
        ).assertIsDisplayed()
    }

    @Test
    fun aRowOpensTheTopicDrillDown() = runComposeUiTest {
        var opened: String? = null
        setContent {
            MaterialTheme { dashboard(threeStatuses(), onTopicClick = { opened = it }) }
        }

        scrollTo(progressReadinessRowTag("compose"))
        onNodeWithTag(progressReadinessRowTag("compose")).performClick()
        assertEquals("compose", opened)
    }

    @Test
    fun changeReopensTheChooserWithTheCurrentTargetAlreadyChosen() = runComposeUiTest {
        var chosen: ReadinessTarget? = null
        setContent {
            MaterialTheme { dashboard(threeStatuses(), onChooseTarget = { chosen = it }) }
        }

        scrollTo(ProgressReadinessChangeTargetTag)
        onNodeWithTag(ProgressReadinessChooserTag).assertDoesNotExist()
        onNodeWithTag(ProgressReadinessChangeTargetTag).performClick()
        scrollTo(ProgressReadinessChooserTag)
        onNodeWithTag(progressReadinessTargetTag(ReadinessTarget.SENIOR)).assertIsNotEnabled()

        onNodeWithTag(progressReadinessTargetTag(ReadinessTarget.JUNIOR)).performClick()
        assertEquals(ReadinessTarget.JUNIOR, chosen)
        onNodeWithTag(ProgressReadinessChooserTag).assertDoesNotExist()
    }

    @Test
    fun theDrillDownBlockStatesEveryPieceOfEvidenceAndEveryMissingItem() = runComposeUiTest {
        val presets = mutableListOf<PracticePreset>()
        var reviewed = 0
        setContent {
            MaterialTheme {
                ProgressTopicScreen(
                    state = topicContent(),
                    onBack = {},
                    onRetry = {},
                    readiness = gettingThere(),
                    onReviewMistakes = { reviewed += 1 },
                    onPracticePreset = { presets += it },
                )
            }
        }

        onNodeWithTag(ProgressTopicReadinessTag).assertIsDisplayed()
        onNodeWithText("Readiness for Senior").assertIsDisplayed()
        onNodeWithText("Seen 4 of 10 questions at this level").assertIsDisplayed()
        onNodeWithText("Recent accuracy 64% over your last 10 answers", substring = true).assertExists()
        onNodeWithText("2 due mistakes").assertExists()
        onNodeWithText("Review 2 due mistakes").assertExists()
        onNodeWithText("Recent accuracy 64% (aim for 80%)").assertExists()
        onNodeWithText("Seen 40% of questions (aim for 60%)").assertExists()
        onNodeWithText("No Advanced questions", substring = true).assertDoesNotExist()

        // Each action reaches an existing destination: the Mistakes area, or the builder on this Topic.
        onNodeWithTag(progressReadinessGapActionTag(ReadinessGap.DueMistakes(2))).performClick()
        onNodeWithTag(progressReadinessGapActionTag(ReadinessGap.RecentAccuracyBelow(64.0, 80.0))).performClick()
        onNodeWithTag(progressReadinessGapActionTag(ReadinessGap.CoverageBelow(40.0, 60.0))).performClick()
        assertEquals(1, reviewed)
        assertEquals(
            // At the Senior target's levels, so the run counts towards the gap it was opened for.
            listOf(
                PracticePreset(AssessmentScope.Topic("compose"), PracticeQuestionSource.ALL, ReadinessTarget.SENIOR.levels),
                PracticePreset(AssessmentScope.Topic("compose"), PracticeQuestionSource.UNSEEN, ReadinessTarget.SENIOR.levels),
            ),
            presets,
        )
    }

    @Test
    fun theDrillDownSaysWhenAdvancedQuestionsAreMissingAndWithholdsAccuracyBelowTheMinimum() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressTopicScreen(
                    state = ProgressTopicUiState.Empty,
                    onBack = {},
                    onRetry = {},
                    readiness = notEnoughEvidence().copy(advancedUnavailable = true),
                )
            }
        }

        // A Topic with no answers still shows its readiness, ahead of the empty-state sentence.
        onNodeWithTag(ProgressTopicReadinessTag).assertIsDisplayed()
        onNodeWithText("Not enough evidence yet").assertIsDisplayed()
        onNodeWithText("Recent accuracy is shown after 5 answers at this level.").assertIsDisplayed()
        onNodeWithText("No Advanced questions in this topic yet", substring = true).assertIsDisplayed()
        onNodeWithText("No due mistakes").assertIsDisplayed()
        onNodeWithText("No completed answers have been recorded for this topic yet.").assertExists()
    }

    @Test
    fun aReadyDrillDownListsNothingMissing() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressTopicScreen(topicContent(), {}, {}, readiness = ready())
            }
        }

        onNodeWithText("Ready").assertIsDisplayed()
        onNodeWithText("Still to do").assertDoesNotExist()
    }

    /**
     * Statuses carry words, and the colour is a second channel: never the error family, which the
     * app keeps for errors, and never the weak-area amber, because "getting there" is not a fault.
     * The section is composed in each theme, so both resolve their own tokens.
     */
    @Test
    fun noStatusUsesRedOrTheWeakAreaAmberInEitherTheme() = runComposeUiTest {
        val darkTheme = mutableStateOf(false)
        val forbidden = mutableMapOf<Boolean, Set<Color>>()
        val chosen = mutableMapOf<Pair<Boolean, ReadinessStatus>, ReadinessBadgeColors>()
        setContent {
            AppTheme(darkTheme = darkTheme.value) {
                val semantic = AppThemeExtras.semanticColors
                val scheme = MaterialTheme.colorScheme
                forbidden[darkTheme.value] = setOf(
                    semantic.incorrect, semantic.incorrectContainer, semantic.onIncorrectContainer,
                    semantic.partiallyCorrect, semantic.partiallyCorrectContainer,
                    semantic.onPartiallyCorrectContainer,
                    scheme.error, scheme.errorContainer, scheme.onError, scheme.onErrorContainer,
                )
                ReadinessStatus.entries.forEach { status ->
                    chosen[darkTheme.value to status] = readinessBadgeColors(status)
                }
                dashboard(threeStatuses())
            }
        }
        scrollTo(ProgressReadinessGroupTag)
        onNodeWithText("Getting there").assertIsDisplayed()
        darkTheme.value = true
        waitForIdle()
        scrollTo(ProgressReadinessGroupTag)
        onNodeWithText("Getting there").assertIsDisplayed()

        listOf(false, true).forEach { dark ->
            val tokens = ReadinessStatus.entries.map { chosen.getValue(dark to it) }
            tokens.forEach { colors ->
                assertFalse(colors.content in forbidden.getValue(dark), "content, dark=$dark")
                assertFalse(colors.container in forbidden.getValue(dark), "container, dark=$dark")
            }
            // The three statuses are told apart by colour as well as by words.
            assertEquals(3, tokens.map(ReadinessBadgeColors::container).toSet().size, "dark=$dark")
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.scrollTo(tag: String) {
        onNodeWithTag(ProgressContentTag).performScrollToNode(hasTestTag(tag))
    }
}

@Composable
private fun dashboard(
    readiness: ProgressReadinessUiModel,
    onChooseTarget: (ReadinessTarget) -> Unit = {},
    onTopicClick: (String) -> Unit = {},
) {
    ProgressScreen(
        state = dashboardState(readiness),
        onRetry = {},
        onBrowseTopics = {},
        onTopicClick = onTopicClick,
        onHistoryClick = { _, _ -> },
        onPracticePreset = {},
        onReviewMistakes = {},
        onChooseReadinessTarget = onChooseTarget,
    )
}

private fun dashboardState(readiness: ProgressReadinessUiModel): ProgressUiState.Content =
    ProgressUiState.Content(
        completedAttemptCount = 3,
        answeredQuestionCount = 30,
        correctAnswerCount = 21,
        percentage = 70.0,
        coverage = ProgressCoverageUiModel(25, 100, 25.0),
        recentPerformance = null,
        unresolvedMistakeCount = 0,
        weakAreas = emptyList(),
        topics = emptyList(),
        history = emptyList(),
        readiness = readiness,
    )

private fun threeStatuses(): ProgressReadinessUiModel.Assessed =
    ProgressReadinessUiModel.Assessed(
        target = ReadinessTarget.SENIOR,
        readyTopicCount = 1,
        topics = listOf(ready(), gettingThere(), notEnoughEvidence()),
    )

private fun ready() = ProgressTopicReadinessUiModel(
    target = ReadinessTarget.SENIOR,
    topicId = "android",
    topicName = "Android",
    status = ReadinessStatus.READY,
    answeredCount = 12,
    coveredQuestionCount = 8,
    targetQuestionCount = 10,
    recentAnsweredCount = 10,
    recentCorrectCount = 9,
    recentAccuracy = 90.0,
    dueMistakeCount = 0,
    advancedUnavailable = true,
    gaps = emptyList(),
)

private fun gettingThere() = ProgressTopicReadinessUiModel(
    target = ReadinessTarget.SENIOR,
    topicId = "compose",
    topicName = "Compose",
    status = ReadinessStatus.GETTING_THERE,
    answeredCount = 14,
    coveredQuestionCount = 4,
    targetQuestionCount = 10,
    recentAnsweredCount = 10,
    recentCorrectCount = 6,
    recentAccuracy = 64.0,
    dueMistakeCount = 2,
    advancedUnavailable = false,
    gaps = listOf(
        ReadinessGap.DueMistakes(2),
        ReadinessGap.RecentAccuracyBelow(64.0, 80.0),
        ReadinessGap.CoverageBelow(40.0, 60.0),
    ),
)

private fun notEnoughEvidence() = ProgressTopicReadinessUiModel(
    target = ReadinessTarget.SENIOR,
    topicId = "coroutines",
    topicName = "Coroutines",
    status = ReadinessStatus.NOT_ENOUGH_EVIDENCE,
    answeredCount = 3,
    coveredQuestionCount = 3,
    targetQuestionCount = 12,
    recentAnsweredCount = 3,
    recentCorrectCount = 1,
    recentAccuracy = 100.0 / 3,
    dueMistakeCount = 0,
    advancedUnavailable = false,
    gaps = listOf(ReadinessGap.MoreAnswersNeeded(2)),
)

private fun topicContent() = ProgressTopicUiState.Content(
    topicName = "Compose",
    answeredCount = 14,
    correctCount = 9,
    percentage = 64.3,
    isWeak = false,
    subtopics = emptyList(),
)
