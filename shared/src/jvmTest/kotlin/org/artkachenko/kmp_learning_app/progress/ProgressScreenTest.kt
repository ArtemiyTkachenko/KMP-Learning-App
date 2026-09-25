package org.artkachenko.kmp_learning_app.progress

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset

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
                    onBrowseTopics = {},
                    onTopicClick = {},
                    onHistoryClick = { _, _ -> },
                    onPracticePreset = {},
                    onReviewMistakes = {},
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
                ProgressScreen(ProgressUiState.Empty, {}, {}, {}, {}, { _, _ -> }, {}, {})
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
                ProgressScreen(ProgressUiState.Error, {}, { retryCount += 1 }, {}, {}, { _, _ -> }, {}, {})
            }
        }
        onNodeWithText("Progress could not be loaded.").assertIsDisplayed()
        onNodeWithText("Retry").performClick()
        assertEquals(1, retryCount)
    }

    @Test
    fun contentRendersOverallStatisticsAndWeakAreaFallbacks() = runComposeUiTest {
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
                    ),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onTopicClick = {},
                    onHistoryClick = { _, _ -> },
                    onPracticePreset = {},
                    onReviewMistakes = {},
                )
            }
        }

        // Overall counts are label/value rows now, so assert the values too: asserting only the
        // labels would pass no matter what numbers the state carried.
        // Pair each value with its own label: two metrics can legitimately share a value, so a
        // bare onNodeWithText("3") is both ambiguous and unable to say which row it checked.
        onNode(hasText("Completed sessions") and hasText("3")).assertIsDisplayed()
        onNode(hasText("Questions answered") and hasText("30")).assertIsDisplayed()
        onNode(hasText("Correct answers") and hasText("21")).assertIsDisplayed()
        // Now that a recent figure exists, the lifetime one has to say which of the two it is.
        onNodeWithText("All-time accuracy").assertIsDisplayed()
        onAllNodesWithText("70%").assertCountEquals(1)
        onNodeWithText("Weak areas").assertIsDisplayed()
        onNodeWithText("State").assertIsDisplayed()
        onNodeWithText("66.7%").assertExists()
        // A weak subtopic missing its parent name and a weak topic missing its own name both fall
        // back rather than disappearing. The rows carry a practice shortcut each now, so the second
        // one starts below the fold and has to be scrolled into composition first.
        onNodeWithTag(ProgressContentTag).performScrollToNode(hasText("2 / 5 correct"))
        onAllNodesWithText("Topic unavailable").assertCountEquals(2)
    }

    @Test
    fun topicPerformanceRowsRenderCountsPercentageAndNameFallback() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(
                    state = contentState(
                        topics = listOf(
                            ProgressTopicUiModel("a", "Kotlin", 20, 14, 70.0),
                            ProgressTopicUiModel("b", null, 3, 1, 33.333),
                        ),
                    ),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onTopicClick = {},
                    onHistoryClick = { _, _ -> },
                    onPracticePreset = {},
                    onReviewMistakes = {},
                )
            }
        }

        // The weak-areas section now states that nothing stands out rather than vanishing, so the
        // Topic rows below it start off screen and have to be scrolled into composition first.
        onNodeWithTag(ProgressContentTag).performScrollToNode(hasText("Topic performance"))
        onNodeWithText("Topic performance").assertExists()
        onNodeWithText("Kotlin").assertIsDisplayed()
        onNodeWithText("14 / 20 correct").assertExists()
        // The overall headline also reads 70%, so both nodes are expected here.
        onAllNodesWithText("70%").assertCountEquals(2)
        onNodeWithTag(ProgressContentTag).performScrollToNode(hasText("Topic unavailable"))
        onNodeWithText("Topic unavailable").assertExists()
        onNodeWithText("33.3%").assertExists()
    }

    /**
     * An empty weak-area list means one of two different things and the state carries no flag
     * saying which, so the presence of per-Topic performance is what decides. With Topics derived,
     * the weakness rule ran over real observations and singled nothing out — a fact worth stating
     * rather than a gap. Nothing is classified to fill it: the section still lists exactly what the
     * domain put in `weakAreas`, which here is nothing.
     */
    @Test
    fun anEmptyWeakAreaListIsStatedWhenTopicPerformanceWasDerived() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(
                    state = contentState(
                        topics = listOf(ProgressTopicUiModel("a", "Kotlin", 20, 14, 70.0)),
                    ),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onTopicClick = {},
                    onHistoryClick = { _, _ -> },
                    onPracticePreset = {},
                    onReviewMistakes = {},
                )
            }
        }

        onNodeWithText("Weak areas").assertIsDisplayed()
        onNodeWithText("Nothing standing out yet").assertIsDisplayed()
        // The count is the learner's own evidence, not a threshold or a target.
        onNodeWithText(
            "Across your 30 answered questions, no topic or subtopic is far enough behind the " +
                "rest to single out. Keep practising and any that fall behind will be listed here.",
        ).assertIsDisplayed()
    }

    @Test
    fun observationBasedSectionsAreAbsentWhenTheyHaveNoRows() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(contentState(), {}, {}, {}, {}, { _, _ -> }, {}, {})
            }
        }

        // Overall statistics survive a curriculum import that orphans historical questions, so
        // the derived sections must disappear rather than leave dangling headers. The weak-area
        // section goes with them: with no Topic performance derived there are no current
        // observations, and "nothing stands out" would be a claim the app cannot support.
        onNodeWithText("Completed sessions").assertIsDisplayed()
        onNodeWithText("Weak areas").assertDoesNotExist()
        onNodeWithText("Topic performance").assertDoesNotExist()
        onNodeWithText("Session history").assertDoesNotExist()
    }

    @Test
    fun theUnresolvedCountRoutesIntoTheMistakeQueue() = runComposeUiTest {
        var reviews = 0
        setContent {
            MaterialTheme {
                ProgressScreen(
                    state = contentState(unresolvedMistakeCount = 3),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onTopicClick = {},
                    onHistoryClick = { _, _ -> },
                    onPracticePreset = {},
                    onReviewMistakes = { reviews += 1 },
                )
            }
        }

        onNodeWithText("3 unresolved mistakes to review").assertIsDisplayed()
        // Progress answers "what should I work on next?", and the most concrete answer it holds is
        // a queue of questions already got wrong. Reporting its size and then refusing to open it
        // stopped one step short of being useful.
        onNodeWithTag(ProgressReviewMistakesTag).assertHasClickAction().performClick()
        assertEquals(1, reviews)
    }

    /** An empty queue has nothing to open, so the row states a fact and offers no action. */
    @Test
    fun aResolvedQueueReportsItselfWithoutOfferingARoute() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(
                    state = contentState(unresolvedMistakeCount = 0),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onTopicClick = {},
                    onHistoryClick = { _, _ -> },
                    onPracticePreset = {},
                    onReviewMistakes = {},
                )
            }
        }

        onNodeWithText("No unresolved mistakes — nice work.").assertIsDisplayed()
        onNodeWithTag(ProgressReviewMistakesTag).assertDoesNotExist()
    }

    @Test
    fun theUnresolvedCountIsAbsentWhenNoAssessmentHasBeenCompleted() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(ProgressUiState.Empty, {}, {}, {}, {}, { _, _ -> }, {}, {})
            }
        }

        // With zero completed assessments there cannot be an unresolved completed mistake.
        onNodeWithText("unresolved mistakes", substring = true).assertDoesNotExist()
    }

    @Test
    fun nothingUnresolvedReportsTheAchievementInsteadOfACount() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(
                    state = contentState(unresolvedMistakeCount = 0),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onTopicClick = {},
                    onHistoryClick = { _, _ -> },
                    onPracticePreset = {},
                    onReviewMistakes = {},
                )
            }
        }

        onNodeWithText("No unresolved mistakes — nice work.").assertIsDisplayed()
        onNodeWithText("unresolved mistakes to review", substring = true).assertDoesNotExist()
    }

    @Test
    fun emptyProgressOffersAWayToStartLearning() = runComposeUiTest {
        var browsed = 0
        setContent {
            MaterialTheme {
                ProgressScreen(ProgressUiState.Empty, {}, {}, { browsed += 1 }, {}, { _, _ -> }, {}, {})
            }
        }

        onNodeWithText("Browse topics").assertIsDisplayed().performClick()

        assertEquals(1, browsed)
    }

    @Test
    fun topicPerformanceCardEmitsStableTopicIdOnce() = runComposeUiTest {
        val clicked = mutableListOf<String>()
        setContent {
            MaterialTheme {
                ProgressScreen(
                    state = contentState(
                        topics = listOf(
                            ProgressTopicUiModel("topic_kotlin", "Kotlin", 20, 14, 70.0),
                            ProgressTopicUiModel("topic_android", "Android", 10, 5, 50.0),
                        ),
                    ),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onTopicClick = clicked::add,
                    onHistoryClick = { _, _ -> },
                    onPracticePreset = {},
                    onReviewMistakes = {},
                )
            }
        }

        onNodeWithTag(ProgressContentTag).performScrollToNode(hasText("Android"))
        onNodeWithText("Android")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()

        assertEquals(listOf("topic_android"), clicked)
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
                Instant.parse("2026-08-29T00:15:00Z"),
            ),
            CompletedAttemptUiModel(
                "focused-id",
                CompletedAssessmentType.FOCUSED,
                FocusedScopeUiModel.Subtopic("Kotlin", "Coroutines"),
                10,
                8,
                80.0,
                Instant.parse("2026-08-28T21:30:00Z"),
            ),
        )

        setContent {
            MaterialTheme {
                ProgressScreen(
                    contentState(history = history),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onTopicClick = {},
                    onHistoryClick = { type, id -> clicks += type to id },
                    onPracticePreset = {},
                    onReviewMistakes = {},
                )
            }
        }

        onNodeWithText("Session history").assertIsDisplayed()
        onNodeWithText("Mixed Android Interview")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        onNodeWithText("Practice")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        onNodeWithText("Kotlin · Coroutines").assertIsDisplayed()
        // The raw instant never reaches the UI; how it reads is `timestampText`'s job and is
        // asserted there, against an explicit zone rather than the agent's.
        onNodeWithText("2026-08-29T00:15:00Z").assertDoesNotExist()
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
                                Instant.parse("2026-08-29T00:00:00Z"),
                            ),
                            CompletedAttemptUiModel(
                                "missing-subtopic",
                                CompletedAssessmentType.FOCUSED,
                                FocusedScopeUiModel.Subtopic(null, null),
                                1,
                                0,
                                0.0,
                                Instant.parse("2026-08-28T00:00:00Z"),
                            ),
                        ),
                    ),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onTopicClick = {},
                    onHistoryClick = { _, _ -> },
                    onPracticePreset = {},
                    onReviewMistakes = {},
                )
            }
        }

        onNodeWithTag(ProgressContentTag).performScrollToNode(hasText("Topic unavailable"))
        onNodeWithTag(ProgressContentTag).performScrollToNode(hasText("Subtopic unavailable"))
    }

    @Test
    fun coverageReportsItsDenominatorRatherThanABarePercentage() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(
                    contentState(coverage = ProgressCoverageUiModel(25, 100, 25.0)),
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                    {},
                    {},
                )
            }
        }

        onNodeWithText("Curriculum coverage").assertIsDisplayed()
        // Coverage next to accuracy is two unexplained percentages unless the counts are visible,
        // and the bar alone must never be the only place the figure appears.
        onNodeWithText("25 of 100 questions explored").assertIsDisplayed()
    }

    @Test
    fun broadCoverageStaysARestrainedCountAndPercentage() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(
                    contentState(coverage = ProgressCoverageUiModel(90, 100, 90.0)),
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                    {},
                    {},
                )
            }
        }

        onNodeWithText("90 of 100 questions explored").assertIsDisplayed()
    }

    @Test
    fun anEmptyActiveBankIsReportedInsteadOfZeroPercentCoverage() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(
                    contentState(coverage = ProgressCoverageUiModel(0, 0, null)),
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                    {},
                    {},
                )
            }
        }

        // 0/0 is "nothing to cover", which is not the same claim as "0% covered".
        onNodeWithText("No active curriculum available").assertIsDisplayed()
        onNodeWithText("0 of 0 questions explored").assertDoesNotExist()
    }

    @Test
    fun recentPerformanceIsPresentedApartFromLifetimeAccuracy() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(
                    contentState(
                        recentPerformance = recentState(
                            attemptPercentages = listOf(60.0, 68.0, 72.0, 77.0, 81.0),
                            correctAnswerCount = 41,
                            answeredQuestionCount = 50,
                        ),
                    ),
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                    {},
                    {},
                )
            }
        }

        onNodeWithText("Recent performance").assertIsDisplayed()
        // The lifetime figure is 70%; only the recent surface may read 82%.
        onNodeWithText("82%").assertIsDisplayed()
        onNodeWithText("Last 5 completed sessions").assertIsDisplayed()
        onNodeWithText("41 / 50 correct").assertIsDisplayed()
        onNodeWithText("All-time accuracy").assertIsDisplayed()
        onAllNodesWithText("70%").assertCountEquals(1)
    }

    @Test
    fun oneCompletedAssessmentReadsAsASingularWindowWithNoTrend() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(
                    contentState(
                        recentPerformance = recentState(
                            attemptPercentages = listOf(82.0),
                            correctAnswerCount = 9,
                            answeredQuestionCount = 11,
                        ),
                    ),
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                    {},
                    {},
                )
            }
        }

        onNodeWithText("Last completed session").assertIsDisplayed()
        onNodeWithText("9 / 11 correct").assertIsDisplayed()
        onNodeWithTag(ProgressRecentTrendChartTag).assertDoesNotExist()
        onNodeWithText("A trend appears after 3 completed sessions.").assertIsDisplayed()
    }

    @Test
    fun twoCompletedAssessmentsKeepTheSummaryAndWithholdOnlyTheChart() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(
                    contentState(
                        recentPerformance = recentState(
                            attemptPercentages = listOf(70.0, 90.0),
                            correctAnswerCount = 16,
                            answeredQuestionCount = 20,
                        ),
                    ),
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                    {},
                    {},
                )
            }
        }

        // Two assessments are still real evidence, so hiding the whole surface would throw away
        // information the learner has actually earned.
        onNodeWithText("Recent performance").assertIsDisplayed()
        onNodeWithText("80%").assertIsDisplayed()
        onNodeWithText("Last 2 completed sessions").assertIsDisplayed()
        onNodeWithTag(ProgressRecentTrendChartTag).assertDoesNotExist()
        onNodeWithText("A trend appears after 3 completed sessions.").assertIsDisplayed()
    }

    @Test
    fun theTrendChartAppearsAtThreeAttemptsAndDescribesThemOldestFirst() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(
                    contentState(
                        recentPerformance = recentState(
                            attemptPercentages = listOf(60.0, 68.0, 72.0),
                            correctAnswerCount = 20,
                            answeredQuestionCount = 30,
                        ),
                    ),
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                    {},
                    {},
                )
            }
        }

        onNodeWithText("A trend appears after 3 completed sessions.").assertDoesNotExist()
        // The drawing carries no information of its own: everything it plots is also readable.
        onNodeWithTag(ProgressRecentTrendChartTag)
            .assertContentDescriptionEquals(
                "Recent session accuracy, oldest to newest: 60%, 68%, 72%.",
            )
    }

    /**
     * The chart grows up out of its own axis on first appearance, and none of that may reach a
     * screen reader or a test.
     *
     * The description is the settled series from the first frame — before the growth has moved at
     * all — rather than something that becomes true once the animation lands. The clock is held
     * still deliberately: with it running, a passing assertion would prove only that the animation
     * finished before the assertion ran.
     */
    @Test
    fun theTrendIsDescribedInFullBeforeItHasFinishedDrawing() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            MaterialTheme {
                ProgressScreen(
                    contentState(
                        recentPerformance = recentState(
                            attemptPercentages = listOf(60.0, 68.0, 72.0),
                            correctAnswerCount = 20,
                            answeredQuestionCount = 30,
                        ),
                    ),
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                    {},
                    {},
                )
            }
        }
        mainClock.advanceTimeByFrame()

        onNodeWithTag(ProgressRecentTrendChartTag)
            .assertIsDisplayed()
            .assertContentDescriptionEquals(
                "Recent session accuracy, oldest to newest: 60%, 68%, 72%.",
            )
    }

    @Test
    fun theFiveAttemptWindowIsPlottedWhole() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(
                    contentState(
                        recentPerformance = recentState(
                            attemptPercentages = listOf(60.0, 68.0, 72.0, 77.0, 81.0),
                        ),
                    ),
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                    {},
                    {},
                )
            }
        }

        onNodeWithTag(ProgressRecentTrendChartTag)
            .assertContentDescriptionEquals(
                "Recent session accuracy, oldest to newest: 60%, 68%, 72%, 77%, 81%.",
            )
    }

    @Test
    fun aNewLearnerSeesGuidanceRatherThanAnAllZeroDashboard() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(ProgressUiState.Empty, {}, {}, {}, {}, { _, _ -> }, {}, {})
            }
        }

        onNodeWithText(
            "Complete an interview or focused practice session to start tracking your progress.",
        ).assertIsDisplayed()
        onNodeWithText("Curriculum coverage").assertDoesNotExist()
        onNodeWithText("Recent performance").assertDoesNotExist()
        onNodeWithTag(ProgressRecentTrendChartTag).assertDoesNotExist()
        onNodeWithText("0%").assertDoesNotExist()
    }

    @Test
    fun theNewSummariesDoNotDisplaceTheExistingDiagnosticSections() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(
                    contentState(
                        recentPerformance = recentState(listOf(60.0, 68.0, 72.0)),
                        unresolvedMistakeCount = 2,
                        weakAreas = listOf(
                            WeakAreaUiModel(WeakAreaType.TOPIC, "topic", "Kotlin", null, 5, 2, 40.0),
                        ),
                        topics = listOf(ProgressTopicUiModel("topic", "Kotlin", 20, 14, 70.0)),
                        history = listOf(
                            CompletedAttemptUiModel(
                                "mixed-id",
                                CompletedAssessmentType.MIXED,
                                null,
                                20,
                                15,
                                75.0,
                                Instant.parse("2026-08-29T00:15:00Z"),
                            ),
                        ),
                    ),
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                    {},
                    {},
                )
            }
        }

        // The summaries push the diagnostics further down a scrolling list, which is the intended
        // hierarchy; what must not happen is any of them dropping out of the screen altogether.
        onNodeWithTag(ProgressContentTag)
            .performScrollToNode(hasText("2 unresolved mistakes to review"))
        onNodeWithText("2 unresolved mistakes to review").assertIsDisplayed()
        onNodeWithTag(ProgressContentTag).performScrollToNode(hasText("Weak areas"))
        onNodeWithText("Weak areas").assertIsDisplayed()
        onNodeWithTag(ProgressContentTag).performScrollToNode(hasText("Topic performance"))
        onNodeWithText("Topic performance").assertIsDisplayed()
        onNodeWithTag(ProgressContentTag).performScrollToNode(hasText("Session history"))
        onNodeWithText("Session history").assertIsDisplayed()
    }

    /**
     * Coverage stopped being a card of its own and became context inside the hero, which is a claim
     * about *where* it is rather than about how it looks — so the assertion is an ancestry one and
     * not a colour or a spacing.
     *
     * Everything it said before, it still says: the title, the raw counts that make the percentage
     * interpretable, and the percentage itself.
     */
    @Test
    fun curriculumCoverageIsStatedInsideTheHeroRatherThanAsItsOwnCard() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(
                    contentState(coverage = ProgressCoverageUiModel(25, 100, 25.0)),
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                    {},
                    {},
                )
            }
        }

        val insideHero = hasAnyAncestor(hasTestTag(ProgressHeroTag))
        onNode(hasText("Curriculum coverage") and insideHero).assertIsDisplayed()
        onNode(hasText("25 of 100 questions explored") and insideHero).assertIsDisplayed()
        // The lifetime evidence stayed with the figure it is evidence for.
        onNode(hasText("Questions answered") and hasText("30") and insideHero).assertIsDisplayed()
    }

    /**
     * An empty ACTIVE bank reports that there is nothing to cover, in the hero, rather than drawing
     * a meter at 0% — 0/0 is not 0% covered, and moving the block did not change that.
     */
    @Test
    fun anEmptyActiveBankIsStillReportedInsideTheHero() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(
                    contentState(coverage = ProgressCoverageUiModel(0, 0, null)),
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                    {},
                    {},
                )
            }
        }

        onNode(
            hasText("No active curriculum available") and
                hasAnyAncestor(hasTestTag(ProgressHeroTag)),
        ).assertIsDisplayed()
    }

    /**
     * The hero counts its accuracy up on first appearance, and none of that may reach a screen
     * reader or a test.
     *
     * The figure states the settled value from the first frame — before the count has moved at all
     * — rather than becoming true once the animation lands, exactly as the trend chart's
     * description does. The clock is held still deliberately: with it running, a passing assertion
     * would prove only that the animation finished before the assertion ran.
     */
    @Test
    fun theHeroAnnouncesItsSettledAccuracyBeforeTheCountHasFinished() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            MaterialTheme {
                ProgressScreen(contentState(), {}, {}, {}, {}, { _, _ -> }, {}, {})
            }
        }
        mainClock.advanceTimeByFrame()

        // 70% is the lifetime figure in `contentState`, and the count starts at 0.
        onAllNodesWithText("70%").assertCountEquals(1)
    }

    /**
     * Session history is the most tertiary thing on the dashboard and is drawn as a row rather than
     * a card now. What must survive that is everything a learner acts with: the scope it was run
     * over, the score, the accuracy, and a button that navigates by stable attempt ID.
     */
    @Test
    fun aHistoryRowKeepsItsEvidenceAndItsNavigation() = runComposeUiTest {
        val clicks = mutableListOf<Pair<CompletedAssessmentType, String>>()
        setContent {
            MaterialTheme {
                ProgressScreen(
                    contentState(
                        history = listOf(
                            CompletedAttemptUiModel(
                                "focused-id",
                                CompletedAssessmentType.FOCUSED,
                                FocusedScopeUiModel.Subtopic("Kotlin", "Coroutines"),
                                10,
                                8,
                                80.0,
                                Instant.parse("2026-08-28T21:30:00Z"),
                            ),
                        ),
                    ),
                    {},
                    {},
                    {},
                    {},
                    { type, id -> clicks += type to id },
                    {},
                    {},
                )
            }
        }

        onNodeWithTag(ProgressContentTag).performScrollToNode(hasTestTag(progressHistoryCardTag("focused-id")))
        onNodeWithTag(progressHistoryCardTag("focused-id"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            // The score and the completion time fold onto one supporting line; both are still there.
            .assert(hasText("8 / 10 correct", substring = true))
            .assert(hasText("Kotlin · Coroutines", substring = true))
            .assert(hasText("80%", substring = true))
            .performClick()

        assertEquals(listOf(CompletedAssessmentType.FOCUSED to "focused-id"), clicks)
    }

    /**
     * Per-Topic performance is one grouped table rather than a card each, and the assertion is an
     * ancestry one: what changed is how many containers the section draws, not what any row says.
     *
     * Every row keeps what a learner acts on — the name, the counts, the accuracy, and a button
     * that navigates by stable Topic ID.
     */
    @Test
    fun topicPerformanceRowsShareOneContainerAndKeepTheirOwnNavigation() = runComposeUiTest {
        val clicked = mutableListOf<String>()
        setContent {
            MaterialTheme {
                ProgressScreen(
                    contentState(
                        topics = listOf(
                            ProgressTopicUiModel("a", "Kotlin", 20, 14, 70.0),
                            ProgressTopicUiModel("b", "Compose", 30, 27, 90.0),
                        ),
                    ),
                    {},
                    {},
                    {},
                    { clicked += it },
                    { _, _ -> },
                    {},
                    {},
                )
            }
        }

        onNodeWithTag(ProgressContentTag).performScrollToNode(hasTestTag(progressTopicCardTag("b")))
        // Both rows hang off one container, which is the whole point of the section's new shape.
        onNodeWithTag(progressTopicCardTag("a"))
            .assert(hasAnyAncestor(hasTestTag(ProgressTopicGroupTag)))
        onNodeWithTag(progressTopicCardTag("b"))
            .assert(hasAnyAncestor(hasTestTag(ProgressTopicGroupTag)))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(hasText("27 / 30 correct", substring = true))
            .assert(hasText("90%", substring = true))
            .performClick()

        assertEquals(listOf("b"), clicked)
    }

    /**
     * A weak area is deliberately *not* in that group. It is singled out by an accent border, which
     * is a property of a container, and the contrast between the bordered cards and the quiet table
     * beneath them is what separates "these need attention" from "here is everything".
     */
    @Test
    fun aWeakAreaRowStaysOutsideTheTopicPerformanceGroup() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ProgressScreen(
                    contentState(
                        weakAreas = listOf(
                            WeakAreaUiModel(WeakAreaType.TOPIC, "a", "Kotlin", null, 20, 8, 40.0),
                        ),
                        topics = listOf(ProgressTopicUiModel("a", "Kotlin", 20, 8, 40.0)),
                    ),
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                    {},
                    {},
                )
            }
        }

        onNodeWithTag(ProgressContentTag).performScrollToNode(hasTestTag(progressTopicCardTag("a")))
        onNodeWithTag(progressWeakAreaPracticeTag(
            WeakAreaUiModel(WeakAreaType.TOPIC, "a", "Kotlin", null, 20, 8, 40.0),
        )).assert(hasAnyAncestor(hasTestTag(ProgressTopicGroupTag)).not())
    }

    @Test
    fun aWeakTopicRowOffersPracticeForItsExactScopeAndSource() = runComposeUiTest {
        val presets = mutableListOf<PracticePreset>()
        setContent {
            MaterialTheme {
                ProgressScreen(
                    state = contentState(
                        weakAreas = listOf(
                            WeakAreaUiModel(
                                WeakAreaType.TOPIC,
                                "topic_kotlin",
                                "Kotlin",
                                null,
                                5,
                                2,
                                40.0,
                            ),
                        ),
                    ),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onTopicClick = {},
                    onHistoryClick = { _, _ -> },
                    onPracticePreset = presets::add,
                    onReviewMistakes = {},
                )
            }
        }

        onNodeWithTag(ProgressContentTag).performScrollToNode(hasText("Practice weak area"))
        onNodeWithText("Practice weak area").assertIsDisplayed().performClick()

        assertEquals(
            listOf(
                PracticePreset(
                    scope = AssessmentScope.Topic("topic_kotlin"),
                    source = PracticeQuestionSource.WEAK_AREAS,
                ),
            ),
            presets,
        )
    }

    @Test
    fun aWeakSubtopicRowOffersPracticeForItsExactScopeAndSource() = runComposeUiTest {
        val presets = mutableListOf<PracticePreset>()
        val area = WeakAreaUiModel(
            WeakAreaType.SUBTOPIC,
            "subtopic_state",
            "State",
            "Kotlin",
            3,
            1,
            33.3,
        )
        setContent {
            MaterialTheme {
                ProgressScreen(
                    state = contentState(weakAreas = listOf(area)),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onTopicClick = {},
                    onHistoryClick = { _, _ -> },
                    onPracticePreset = presets::add,
                    onReviewMistakes = {},
                )
            }
        }

        // By row handle rather than by label: every weak row carries the same wording.
        onNodeWithTag(ProgressContentTag)
            .performScrollToNode(hasTestTag(progressWeakAreaPracticeTag(area)))
        onNodeWithTag(progressWeakAreaPracticeTag(area)).performClick()

        assertEquals(
            listOf(
                PracticePreset(
                    scope = AssessmentScope.Subtopic("subtopic_state"),
                    source = PracticeQuestionSource.WEAK_AREAS,
                ),
            ),
            presets,
        )
    }

    /**
     * The dashboard's two aggregate signals name no Topic or Subtopic, so neither may acquire a
     * shortcut: a focused preset built from either one would be a scope chosen for the learner.
     * Topic performance rows are not weak-area rows and keep their existing drill-down only.
     */
    @Test
    fun onlyWeakAreaRowsAcquireAPracticeShortcut() = runComposeUiTest {
        val presets = mutableListOf<PracticePreset>()
        val topicClicks = mutableListOf<String>()
        setContent {
            MaterialTheme {
                ProgressScreen(
                    state = contentState(
                        coverage = ProgressCoverageUiModel(25, 100, 25.0),
                        unresolvedMistakeCount = 17,
                        topics = listOf(ProgressTopicUiModel("topic_kotlin", "Kotlin", 20, 14, 70.0)),
                    ),
                    onBack = {},
                    onRetry = {},
                    onBrowseTopics = {},
                    onTopicClick = topicClicks::add,
                    onHistoryClick = { _, _ -> },
                    onPracticePreset = presets::add,
                    onReviewMistakes = {},
                )
            }
        }

        onNodeWithText("25 of 100 questions explored").assertIsDisplayed()
        onNodeWithText("17 unresolved mistakes to review").assertIsDisplayed()
        // Coverage is a statistic with no single scope to act on, so it offers nothing. The mistake
        // row does route into the queue, but it is a route and never a practice preset: this count
        // spans the whole curriculum and focused practice has to name a Topic or Subtopic.
        onNodeWithText("25 of 100 questions explored").assertHasNoClickAction()
        onAllNodesWithText("Practice weak area").assertCountEquals(0)
        onAllNodesWithText("Practice unseen questions").assertCountEquals(0)

        // The ordinary Topic drill-down is untouched by the shortcut work.
        onNodeWithTag(ProgressContentTag).performScrollToNode(hasText("Kotlin"))
        onNodeWithText("Kotlin").performClick()

        assertEquals(listOf("topic_kotlin"), topicClicks)
        assertEquals(emptyList(), presets)
    }
}

private fun contentState(
    coverage: ProgressCoverageUiModel = ProgressCoverageUiModel(25, 100, 25.0),
    recentPerformance: ProgressRecentPerformanceUiModel? = null,
    unresolvedMistakeCount: Int = 0,
    weakAreas: List<WeakAreaUiModel> = emptyList(),
    topics: List<ProgressTopicUiModel> = emptyList(),
    history: List<CompletedAttemptUiModel> = emptyList(),
): ProgressUiState.Content =
    ProgressUiState.Content(
        completedAttemptCount = 3,
        answeredQuestionCount = 30,
        correctAnswerCount = 21,
        percentage = 70.0,
        coverage = coverage,
        recentPerformance = recentPerformance,
        unresolvedMistakeCount = unresolvedMistakeCount,
        weakAreas = weakAreas,
        topics = topics,
        history = history,
    )

/**
 * A recent window whose accuracy differs from the 70% lifetime figure above, so an assertion can
 * only pass by finding the right one of the two.
 */
private fun recentState(
    attemptPercentages: List<Double>,
    correctAnswerCount: Int = 41,
    answeredQuestionCount: Int = 50,
): ProgressRecentPerformanceUiModel =
    ProgressRecentPerformanceUiModel(
        attemptCount = attemptPercentages.size,
        answeredQuestionCount = answeredQuestionCount,
        correctAnswerCount = correctAnswerCount,
        percentage = correctAnswerCount.toDouble() / answeredQuestionCount * 100.0,
        trend = if (attemptPercentages.size >= 3) {
            ProgressRecentTrendUiModel.Available(
                attemptPercentages.mapIndexed { index, percentage ->
                    ProgressRecentAttemptUiModel("attempt-$index", percentage)
                },
            )
        } else {
            ProgressRecentTrendUiModel.InsufficientHistory(requiredAttemptCount = 3)
        },
    )
