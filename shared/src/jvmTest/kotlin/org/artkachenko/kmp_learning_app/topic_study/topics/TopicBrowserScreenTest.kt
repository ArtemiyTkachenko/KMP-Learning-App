package org.artkachenko.kmp_learning_app.topic_study.topics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.AppRoute
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingContext
import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingTarget
import org.artkachenko.kmp_learning_app.guided_learning.LearningRecommendationRationale
import org.artkachenko.kmp_learning_app.guided_learning.LearningRecommendationTarget
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset
import org.artkachenko.kmp_learning_app.ui.LearningContextUiModel
import org.artkachenko.kmp_learning_app.ui.topicVisualMarkerTag

@OptIn(ExperimentalTestApi::class)
internal class TopicBrowserScreenTest {
    @Test
    fun topSafeAreaAndHeaderSpacingAreEachAppliedOnce() = runComposeUiTest {
        // The shell leaves the top inset unconsumed so this screen, which has no AppTopBar, owns
        // it. The heading should therefore sit at safe area + the screen's own header spacing:
        // not at twice the inset, and not flush against the safe area with the spacing dropped.
        setContent {
            MaterialTheme {
                Box(Modifier.size(400.dp, 800.dp).testTag(TestRootTag)) {
                    TopicBrowserScreen(
                        state = TopicBrowserUiState.Empty,
                        onTopicClick = {},
                        onRetry = {},
                        topWindowInsets = WindowInsets(top = TestTopInset),
                    )
                }
            }
        }

        val rootTop = onNodeWithTag(TestRootTag).fetchSemanticsNode().boundsInRoot.top
        val headerTop = onNodeWithTag(TopicBrowserHeaderTag).fetchSemanticsNode().boundsInRoot.top

        assertEquals(
            expected = rootTop + with(density) { (TestTopInset + TestHeaderSpacing).toPx() },
            actual = headerTop,
            absoluteTolerance = 0.5f,
        )
    }

    @Test
    fun headerSpacingDoesNotScaleWithTheTopInset() = runComposeUiTest {
        // Guards against the inset being applied twice: doubling the inset must move the heading
        // down by exactly one inset, not two.
        setContent {
            MaterialTheme {
                Box(Modifier.size(400.dp, 800.dp).testTag(TestRootTag)) {
                    TopicBrowserScreen(
                        state = TopicBrowserUiState.Empty,
                        onTopicClick = {},
                        onRetry = {},
                        topWindowInsets = WindowInsets(top = TestTopInset * 2),
                    )
                }
            }
        }
        val headerTop = onNodeWithTag(TopicBrowserHeaderTag).fetchSemanticsNode().boundsInRoot.top
        val rootTop = onNodeWithTag(TestRootTag).fetchSemanticsNode().boundsInRoot.top

        assertEquals(
            expected = rootTop + with(density) { (TestTopInset * 2 + TestHeaderSpacing).toPx() },
            actual = headerTop,
            absoluteTolerance = 0.5f,
        )
    }

    @Test
    fun contentViewportReachesTheBottomOfTheScreen() = runComposeUiTest {
        // The shell's Scaffold already ends this content at the top of the navigation bar, so the
        // screen must not hold any bottom padding outside the list: that shows as a strip of
        // background above the bar. Scroll-end spacing lives inside the list instead.
        setContent {
            MaterialTheme {
                Box(Modifier.size(400.dp, 800.dp).testTag(TestRootTag)) {
                    TopicBrowserScreen(
                        state = TopicBrowserUiState.Empty,
                        onTopicClick = {},
                        onRetry = {},
                        topWindowInsets = WindowInsets(0.dp),
                    )
                }
            }
        }

        val rootBottom = onNodeWithTag(TestRootTag).fetchSemanticsNode().boundsInRoot.bottom
        val viewportBottom = onNodeWithTag(TopicBrowserViewportTag)
            .fetchSemanticsNode().boundsInRoot.bottom

        assertEquals(rootBottom, viewportBottom, absoluteTolerance = 0.5f)
    }

    @Test
    fun contentRendersTopicNames() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(
                            topicItem("topic_a", "Topic A"),
                            topicItem("topic_b", "Topic B"),
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Topics").assertIsDisplayed()
        onNodeWithText("Topic A").assertIsDisplayed()
        onNodeWithText("Topic B").assertIsDisplayed()
        onNodeWithTag(TopicBrowserSearchFieldTag).assertIsDisplayed()
    }

    @Test
    fun typingInSearchFieldEmitsQueryChange() = runComposeUiTest {
        var query = ""
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(topicItem("topic_a", "Topic A")),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                    onSearchQueryChange = { query = it },
                )
            }
        }

        onNodeWithTag(TopicBrowserSearchFieldTag).performTextInput("flow")

        assertEquals("flow", query)
    }

    @Test
    fun mixedSearchResultsRenderParentContextAndReturnStableIds() = runComposeUiTest {
        var clickedTopicId: String? = null
        var clickedSubtopicIds: Pair<String, String>? = null
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(
                            topicItem("compose", "Compose Overview"),
                            topicItem("android", "Android UI"),
                        ),
                        query = "compose",
                        topicMatches = listOf(
                            topicItem("compose", "Compose Overview"),
                        ),
                        subtopicMatches = listOf(
                            SubtopicSearchResult(
                                subtopicId = "compose_runtime",
                                subtopicName = "Compose runtime",
                                parentTopicId = "android",
                                parentTopicName = "Android UI",
                            ),
                        ),
                    ),
                    onTopicClick = { clickedTopicId = it },
                    onRetry = {},
                    onSubtopicClick = { topicId, subtopicId ->
                        clickedSubtopicIds = topicId to subtopicId
                    },
                )
            }
        }

        onNodeWithText("Compose Overview").performClick()
        assertEquals("compose", clickedTopicId)

        onNodeWithText("Compose runtime").assertIsDisplayed().performClick()
        assertEquals("android" to "compose_runtime", clickedSubtopicIds)
        onNodeWithText("Android UI").assertIsDisplayed()
        onNodeWithText("Subtopics").assertIsDisplayed()
    }

    @Test
    fun noResultsAndClearActionAreExplicit() = runComposeUiTest {
        var changedQuery: String? = null
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(topicItem("topic_a", "Topic A")),
                        query = "nonsense",
                    ),
                    onTopicClick = {},
                    onRetry = {},
                    onSearchQueryChange = { changedQuery = it },
                )
            }
        }

        onNodeWithText("No topics or subtopics match \"nonsense\"").assertIsDisplayed()
        onNodeWithContentDescription("Clear search").performClick()

        assertEquals("", changedQuery)
    }

    @Test
    fun subtopicOnlyResultRendersItsParentTopic() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(topicItem("architecture", "Architecture")),
                        query = "viewmodel",
                        subtopicMatches = listOf(
                            SubtopicSearchResult(
                                subtopicId = "viewmodel",
                                subtopicName = "ViewModel lifecycle",
                                parentTopicId = "architecture",
                                parentTopicName = "Lifecycle, State & Navigation",
                            ),
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("ViewModel lifecycle").assertIsDisplayed()
        onNodeWithText("Lifecycle, State & Navigation").assertIsDisplayed()
        onNodeWithText("Topics").assertIsDisplayed()
    }

    @Test
    fun topicRowsShowTheirOwnVisualMarkerBesideTheName() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(
                            topicItem("networking", "Networking & Serialization"),
                            topicItem("security", "Security, Privacy & Permissions"),
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        // The name stays the authoritative identity; the marker is an addition, not a replacement.
        onNodeWithText("Networking & Serialization").assertIsDisplayed()
        onNodeWithText("Security, Privacy & Permissions").assertIsDisplayed()
        onNodeWithTag(topicVisualMarkerTag("networking"), useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag(topicVisualMarkerTag("security"), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun aLongTopicNameKeepsBothTheNameAndItsMarkerOnScreen() = runComposeUiTest {
        setContent {
            MaterialTheme {
                Box(Modifier.size(320.dp, 640.dp)) {
                    TopicBrowserScreen(
                        state = TopicBrowserUiState.Content(
                            topics = listOf(
                                topicItem("kmp", "Kotlin Multiplatform & Compose Multiplatform"),
                            ),
                        ),
                        onTopicClick = {},
                        onRetry = {},
                    )
                }
            }
        }

        onNodeWithText("Kotlin Multiplatform & Compose Multiplatform").assertIsDisplayed()
        onNodeWithTag(topicVisualMarkerTag("kmp"), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun searchResultsReuseTheBrowsingTopicMarkerAndParentTopicContext() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(topicItem("networking", "Networking & Serialization")),
                        query = "http",
                        topicMatches = listOf(
                            topicItem("networking", "Networking & Serialization"),
                        ),
                        subtopicMatches = listOf(
                            SubtopicSearchResult(
                                subtopicId = "workmanager",
                                subtopicName = "WorkManager constraints",
                                parentTopicId = "background_work",
                                parentTopicName = "Background Work & OS Constraints",
                            ),
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        // A Topic match is the same Topic, so it must carry the same marker as normal browsing.
        onNodeWithTag(topicVisualMarkerTag("networking"), useUnmergedTree = true).assertIsDisplayed()
        // A Subtopic match inherits its parent Topic's marker, and keeps the parent name too.
        onNodeWithTag(topicVisualMarkerTag("background_work"), useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("WorkManager constraints").assertIsDisplayed()
        onNodeWithText("Background Work & OS Constraints").assertIsDisplayed()
    }

    @Test
    fun discoveryRowsAnnounceTheirNameOnceAndStayFullSizeTargets() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(topicItem("networking", "Networking & Serialization")),
                        query = "compose",
                        topicMatches = listOf(
                            topicItem("networking", "Networking & Serialization"),
                        ),
                        subtopicMatches = listOf(
                            SubtopicSearchResult(
                                subtopicId = "compose_state",
                                subtopicName = "Compose snapshot state",
                                parentTopicId = "android_ui",
                                parentTopicName = "UI — Views & Jetpack Compose",
                            ),
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        // The marker and the chevron are decoration beside text that already says what the row is,
        // so neither may announce anything: a described icon would read the Topic name twice.
        onNodeWithContentDescription("Networking & Serialization").assertDoesNotExist()
        onNodeWithContentDescription("UI — Views & Jetpack Compose").assertDoesNotExist()

        // Each row is one target carrying its whole label, rather than a name and a separate
        // control, and a Subtopic result keeps its parent Topic inside that same label.
        onNodeWithText("Networking & Serialization")
            .assertHasClickAction()
            .assertHeightIsAtLeast(MinimumTouchTarget)
        onNode(hasText("Compose snapshot state") and hasText("UI — Views & Jetpack Compose"))
            .assertHasClickAction()
            .assertHeightIsAtLeast(MinimumTouchTarget)

        // The clear control is the one small target on the screen: its icon is drawn at 40.dp, so
        // what has to reach the minimum is its touch bounds.
        onNodeWithContentDescription("Clear search")
            .assertHasClickAction()
            .assertTouchHeightIsEqualTo(MinimumTouchTarget)
            .assertTouchWidthIsEqualTo(MinimumTouchTarget)
    }

    @Test
    fun anObservedTopicShowsLabelledAccuracyBesideItsCoverageCount() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(
                            topicItem(
                                "kotlin",
                                "Kotlin Language & JVM Fundamentals",
                                learningContext(attempted = 12, total = 28, accuracy = 76.0),
                            ),
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        // The Topic name stays the identity of the row, and its marker stays beside it.
        onNodeWithText("Kotlin Language & JVM Fundamentals").assertIsDisplayed()
        onNodeWithTag(topicVisualMarkerTag("kotlin"), useUnmergedTree = true).assertIsDisplayed()
        // Coverage as a count, so it cannot be read as a second accuracy figure, and accuracy
        // carrying its own label so the two percentages are never an unexplained pair.
        onNodeWithText("12 of 28 explored").assertIsDisplayed()
        onNodeWithText("76%").assertIsDisplayed()
        onNodeWithText("accuracy").assertIsDisplayed()
        onNodeWithText("Not studied yet").assertDoesNotExist()
        onNodeWithText("Weak area").assertDoesNotExist()
    }

    @Test
    fun anUnseenTopicIsNeutralRatherThanZeroPercent() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(
                            topicItem("topic_a", "Topic A", learningContext(0, 14)),
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("0 of 14 explored").assertIsDisplayed()
        onNodeWithText("Not studied yet").assertIsDisplayed()
        // Never answered is not the same as answered and got none right.
        onNodeWithText("0%").assertDoesNotExist()
        onNodeWithText("accuracy").assertDoesNotExist()
        onNodeWithText("Weak area").assertDoesNotExist()
    }

    @Test
    fun historicalAccuracyWithZeroCurrentCoverageIsNotCalledUnstudied() = runComposeUiTest {
        // The Questions this Topic was answered on have since been retired, so real historical
        // accuracy sits beside a current coverage of zero. Both are true at once.
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(
                            topicItem("topic_a", "Topic A", learningContext(0, 8, accuracy = 62.0)),
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("0 of 8 explored").assertIsDisplayed()
        onNodeWithText("62%").assertIsDisplayed()
        onNodeWithText("Not studied yet").assertDoesNotExist()
    }

    @Test
    fun theWeakBadgeFollowsTheDomainFlagAndNotTheAccuracyColour() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(
                            // Weak by the domain's verdict.
                            topicItem("weak", "Weak Topic", learningContext(6, 20, 41.0, isWeak = true)),
                            // Just as low, but on too little evidence for the policy to call it
                            // weak: the figure may render as low accuracy, the badge may not appear.
                            topicItem("sparse", "Sparse Topic", learningContext(1, 20, 0.0)),
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        onAllNodesWithText("Weak area").assertCountEquals(1)
        onNodeWithText("41%").assertIsDisplayed()
        onNodeWithText("0%").assertIsDisplayed()
        // A 0% accuracy is a real answered result here, so the row is not "not studied".
        onNodeWithText("Not studied yet").assertDoesNotExist()
    }

    @Test
    fun aTopicSearchResultCarriesTheSameLearningContextAsBrowsing() = runComposeUiTest {
        val match = topicItem(
            "networking",
            "Networking & Serialization",
            learningContext(attempted = 5, total = 11, accuracy = 80.0),
        )
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(match),
                        query = "networking",
                        topicMatches = listOf(match),
                        subtopicMatches = listOf(
                            SubtopicSearchResult(
                                subtopicId = "http_clients",
                                subtopicName = "HTTP clients",
                                parentTopicId = "networking",
                                parentTopicName = "Networking & Serialization",
                            ),
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("5 of 11 explored").assertIsDisplayed()
        onNodeWithText("80%").assertIsDisplayed()
        // The Subtopic result stays a compact, parent-contextual row: its full learning context
        // belongs on Topic Detail, not in a search list.
        onNodeWithText("HTTP clients").assertIsDisplayed()
        onAllNodesWithText("5 of 11 explored").assertCountEquals(1)
    }

    @Test
    fun aLongTopicNameStaysReadableBesideItsLearningContextOnACompactScreen() = runComposeUiTest {
        setContent {
            MaterialTheme {
                Box(Modifier.size(320.dp, 640.dp)) {
                    TopicBrowserScreen(
                        state = TopicBrowserUiState.Content(
                            topics = listOf(
                                topicItem(
                                    "architecture",
                                    "Application Architecture & Design Principles",
                                    learningContext(14, 31, accuracy = 68.0, isWeak = true),
                                ),
                            ),
                        ),
                        onTopicClick = {},
                        onRetry = {},
                    )
                }
            }
        }

        // Nothing is clipped off a 320.dp card: the wrapped name, the marker, the coverage line,
        // the accuracy figure, and the badge are all on screen together.
        onNodeWithText("Application Architecture & Design Principles").assertIsDisplayed()
        onNodeWithTag(topicVisualMarkerTag("architecture"), useUnmergedTree = true)
            .assertIsDisplayed()
        onNodeWithText("14 of 31 explored").assertIsDisplayed()
        onNodeWithText("68%").assertIsDisplayed()
        onNodeWithText("Weak area").assertIsDisplayed()
        onNodeWithText("Application Architecture & Design Principles")
            .assertHasClickAction()
            .assertHeightIsAtLeast(MinimumTouchTarget)
    }

    @Test
    fun aContinueContextRendersItsScopeAndParentTopicAboveTheCatalogue() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(topicItem("kotlin", "Kotlin")),
                        continueStudying = ContinueStudyingContext(
                            target = ContinueStudyingTarget.Topic("kotlin", "coroutines"),
                            scopeName = "Coroutines",
                            parentTopicName = "Kotlin",
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithTag(TopicBrowserContinueStudyingTag).assertIsDisplayed()
        onNodeWithText("Continue studying").assertIsDisplayed()
        onNodeWithText("Coroutines").assertIsDisplayed()
        // The card is a shortcut, not a report: no score, coverage, or rationale appears on it.
        onNodeWithText("accuracy").assertDoesNotExist()
        onNodeWithText("Weak area").assertDoesNotExist()
    }

    @Test
    fun aTargetedPracticeContextNamesThePracticeItReturnsTo() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(topicItem("kotlin", "Kotlin")),
                        continueStudying = ContinueStudyingContext(
                            target = ContinueStudyingTarget.Practice(
                                PracticePreset(
                                    scope = AssessmentScope.Subtopic("coroutines"),
                                    source = PracticeQuestionSource.WEAK_AREAS,
                                ),
                            ),
                            scopeName = "Coroutines",
                            parentTopicName = "Kotlin",
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Coroutines").assertIsDisplayed()
        onNodeWithText("Weak-area practice").assertIsDisplayed()
    }

    @Test
    fun theContinueCardIsOneTargetEmittingTheSemanticContinueTarget() = runComposeUiTest {
        var clickedTarget: ContinueStudyingTarget? = null
        val target = ContinueStudyingTarget.Topic("kotlin", "coroutines")
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(topicItem("kotlin", "Kotlin")),
                        continueStudying = ContinueStudyingContext(target, "Coroutines", "Kotlin"),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                    onContinueStudyingClick = { clickedTarget = it },
                )
            }
        }

        // One tap, and no Continue/Configure/Dismiss row of competing actions.
        onNodeWithTag(TopicBrowserContinueStudyingTag)
            .assertHasClickAction()
            .assertHeightIsAtLeast(MinimumTouchTarget)
            .performClick()

        assertEquals(target, clickedTarget)
    }

    @Test
    fun noContinueContextLeavesTheCatalogueExactlyAsItWas() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(topicItem("kotlin", "Kotlin")),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithTag(TopicBrowserContinueStudyingTag).assertDoesNotExist()
        onNodeWithText("Continue studying").assertDoesNotExist()
        onNodeWithText("Kotlin").assertIsDisplayed()
    }

    @Test
    fun theContinueCardIsAbsentFromSearchResults() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(topicItem("kotlin", "Kotlin")),
                        query = "kotlin",
                        topicMatches = listOf(topicItem("kotlin", "Kotlin")),
                        // The state holder already withholds it while a query is active; this
                        // asserts the screen cannot reintroduce it even if one arrives.
                        continueStudying = ContinueStudyingContext(
                            target = ContinueStudyingTarget.Topic("kotlin"),
                            scopeName = "Kotlin",
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithTag(TopicBrowserContinueStudyingTag).assertDoesNotExist()
        onNodeWithText("Kotlin").assertIsDisplayed()
    }

    @Test
    fun theContinueCardStaysReadableBesideTheTopicCardsOnACompactScreen() = runComposeUiTest {
        setContent {
            MaterialTheme {
                Box(Modifier.size(320.dp, 640.dp)) {
                    TopicBrowserScreen(
                        state = TopicBrowserUiState.Content(
                            topics = listOf(
                                topicItem(
                                    "architecture",
                                    "Application Architecture & Design Principles",
                                    learningContext(14, 31, accuracy = 68.0),
                                ),
                            ),
                            continueStudying = ContinueStudyingContext(
                                target = ContinueStudyingTarget.Topic("kotlin", "coroutines"),
                                scopeName = "Structured concurrency & cancellation",
                                parentTopicName = "Kotlin Language & JVM Fundamentals",
                            ),
                        ),
                        onTopicClick = {},
                        onRetry = {},
                    )
                }
            }
        }

        onNodeWithText("Continue studying").assertIsDisplayed()
        onNodeWithText("Structured concurrency & cancellation").assertIsDisplayed()
        onNodeWithText("Kotlin Language & JVM Fundamentals").assertIsDisplayed()
        // The Topic rows below are unchanged by the addition.
        onNodeWithText("Application Architecture & Design Principles").assertIsDisplayed()
        onNodeWithText("14 of 31 explored").assertIsDisplayed()
        onNodeWithText("68%").assertIsDisplayed()
    }

    @Test
    fun noRecommendationLeavesTheCatalogueExactlyAsItWas() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(topicItem("kotlin", "Kotlin")),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithTag(TopicBrowserRecommendedNextTag).assertDoesNotExist()
        onNodeWithText("Recommended next").assertDoesNotExist()
        onNodeWithText("Kotlin").assertIsDisplayed()
    }

    @Test
    fun aNewLearnerIsPointedAtTheTopicListInPlainLanguage() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(topicItem("kotlin", "Kotlin")),
                        recommendedNext = RecommendedNextUiModel(
                            target = LearningRecommendationTarget.Topics,
                            rationale = LearningRecommendationRationale.NewUser,
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        // Exactly one card, with its reason on it, and no Topic picked on the learner's behalf.
        onAllNodesWithText("Recommended next").assertCountEquals(1)
        onNodeWithTag(TopicBrowserRecommendedNextTag).assertIsDisplayed()
        onNodeWithText("Choose a topic").assertIsDisplayed()
        onNodeWithText("Start with a topic you want to explore.").assertIsDisplayed()
        // The catalogue below is unchanged by the addition.
        onNodeWithText("Kotlin").assertIsDisplayed()
    }

    @Test
    fun unresolvedMistakesAreExplainedWithTheirExactCount() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(topicItem("kotlin", "Kotlin")),
                        recommendedNext = RecommendedNextUiModel(
                            target = LearningRecommendationTarget.MistakeReview,
                            rationale = LearningRecommendationRationale.UnresolvedMistakes(3),
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Review mistakes").assertIsDisplayed()
        onNodeWithText("You have 3 unresolved mistakes to revisit.").assertIsDisplayed()
    }

    @Test
    fun aSingleUnresolvedMistakeReadsAsOne() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(topicItem("kotlin", "Kotlin")),
                        recommendedNext = RecommendedNextUiModel(
                            target = LearningRecommendationTarget.MistakeReview,
                            rationale = LearningRecommendationRationale.UnresolvedMistakes(1),
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("You have 1 unresolved mistake to revisit.").assertIsDisplayed()
    }

    @Test
    fun aWeakAreaIsNamedAndExplained() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(topicItem("kotlin", "Kotlin")),
                        recommendedNext = RecommendedNextUiModel(
                            target = weakAreaPractice(),
                            rationale = LearningRecommendationRationale.WeakArea(
                                scope = AssessmentScope.Subtopic("coroutines"),
                                areaName = "Coroutines",
                            ),
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Practice Coroutines").assertIsDisplayed()
        onNodeWithText("This is currently one of your weak areas.").assertIsDisplayed()
        // A recommendation is an action and a reason, never a score or a ranking.
        onNodeWithText("accuracy").assertDoesNotExist()
    }

    @Test
    fun aWeakAreaWithNoCurrentNameStillOffersTheAction() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(topicItem("kotlin", "Kotlin")),
                        recommendedNext = RecommendedNextUiModel(
                            target = weakAreaPractice(),
                            rationale = LearningRecommendationRationale.WeakArea(
                                scope = AssessmentScope.Subtopic("coroutines"),
                                areaName = null,
                            ),
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        // A missing display name degrades the wording; it does not withhold a decision the policy
        // made without needing the name.
        onNodeWithTag(TopicBrowserRecommendedNextTag).assertIsDisplayed()
        onNodeWithText("Practice a weak area").assertIsDisplayed()
        onNodeWithText("Focus on an area that needs more work.").assertIsDisplayed()
    }

    @Test
    fun unseenCoverageNamesTheTopicAndCountsWhatIsLeft() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(topicItem("kotlin", "Kotlin")),
                        recommendedNext = RecommendedNextUiModel(
                            target = unseenPractice(),
                            rationale = LearningRecommendationRationale.UnseenCoverage(
                                topicId = "kotlin",
                                unseenQuestionCount = 12,
                            ),
                            topicName = "Kotlin",
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Explore Kotlin").assertIsDisplayed()
        onNodeWithText("12 questions in this topic are still unseen.").assertIsDisplayed()
    }

    @Test
    fun unseenCoverageWithNoResolvableNameFallsBackToNeutralCopy() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(topicItem("kotlin", "Kotlin")),
                        recommendedNext = RecommendedNextUiModel(
                            target = unseenPractice(),
                            rationale = LearningRecommendationRationale.UnseenCoverage(
                                topicId = "kotlin",
                                unseenQuestionCount = 1,
                            ),
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Practice unseen questions").assertIsDisplayed()
        onNodeWithText("1 question in this topic is still unseen.").assertIsDisplayed()
    }

    @Test
    fun theRecommendationIsOneTargetEmittingTheSemanticTarget() = runComposeUiTest {
        var clickedTarget: LearningRecommendationTarget? = null
        val target = weakAreaPractice()
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(topicItem("kotlin", "Kotlin")),
                        recommendedNext = RecommendedNextUiModel(
                            target = target,
                            rationale = LearningRecommendationRationale.WeakArea(
                                scope = AssessmentScope.Subtopic("coroutines"),
                                areaName = "Coroutines",
                            ),
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                    onRecommendedNextClick = { clickedTarget = it },
                )
            }
        }

        // One tap, and no dismiss, settings, or "why?" affordance beside it.
        onNodeWithTag(TopicBrowserRecommendedNextTag)
            .assertHasClickAction()
            .assertHeightIsAtLeast(MinimumTouchTarget)
            .performClick()

        assertEquals(target, clickedTarget)
    }

    @Test
    fun theRecommendationIsAbsentFromSearchResults() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(topicItem("kotlin", "Kotlin")),
                        query = "kotlin",
                        topicMatches = listOf(topicItem("kotlin", "Kotlin")),
                        // The state holder already withholds it while a query is active; this
                        // asserts the screen cannot reintroduce it even if one arrives.
                        recommendedNext = RecommendedNextUiModel(
                            target = LearningRecommendationTarget.MistakeReview,
                            rationale = LearningRecommendationRationale.UnresolvedMistakes(3),
                        ),
                    ),
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithTag(TopicBrowserRecommendedNextTag).assertDoesNotExist()
        onNodeWithText("Review mistakes").assertDoesNotExist()
        onNodeWithText("Kotlin").assertIsDisplayed()
    }

    @Test
    fun bothGuidedCardsCanCoexistAboveTheCatalogueOnACompactScreen() = runComposeUiTest {
        setContent {
            MaterialTheme {
                Box(Modifier.size(320.dp, 640.dp)) {
                    TopicBrowserScreen(
                        state = TopicBrowserUiState.Content(
                            topics = listOf(
                                topicItem(
                                    "architecture",
                                    "Application Architecture & Design Principles",
                                    learningContext(14, 31, accuracy = 68.0),
                                ),
                            ),
                            // Deliberately pointing at different things: the highest-priority
                            // action now, and the most recent learning context.
                            recommendedNext = RecommendedNextUiModel(
                                target = LearningRecommendationTarget.MistakeReview,
                                rationale = LearningRecommendationRationale.UnresolvedMistakes(3),
                            ),
                            continueStudying = ContinueStudyingContext(
                                target = ContinueStudyingTarget.Topic("kotlin", "coroutines"),
                                scopeName = "Coroutines",
                                parentTopicName = "Kotlin Language & JVM Fundamentals",
                            ),
                        ),
                        onTopicClick = {},
                        onRetry = {},
                    )
                }
            }
        }

        // Neither suppresses the other, and neither replaces the Topic rows below them.
        onNodeWithTag(TopicBrowserRecommendedNextTag).assertIsDisplayed()
        onNodeWithText("Review mistakes").assertIsDisplayed()
        onNodeWithText("You have 3 unresolved mistakes to revisit.").assertIsDisplayed()
        onNodeWithTag(TopicBrowserContinueStudyingTag).assertIsDisplayed()
        onNodeWithText("Continue studying").assertIsDisplayed()
        onNodeWithText("Coroutines").assertIsDisplayed()
        onNodeWithText("Application Architecture & Design Principles")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun topicClickReturnsStableTopicId() = runComposeUiTest {
        var clickedTopicId: String? = null

        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(topicItem("topic_stable_id", "Topic Name")),
                    ),
                    onTopicClick = { topicId ->
                        clickedTopicId = topicId
                    },
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Topic Name").performClick()

        assertEquals("topic_stable_id", clickedTopicId)
    }

    @Test
    fun loadingStateRenders() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Loading,
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithTag(TopicBrowserLoadingTag).assertIsDisplayed()
        onNodeWithText("Loading topics").assertIsDisplayed()
        onNodeWithTag(TopicBrowserSearchFieldTag).assertDoesNotExist()
        // The interview and progress entries are their own navigation-bar destinations now, so
        // the topic list is only responsible for topics.
        onNodeWithText("View progress").assertDoesNotExist()
        onNodeWithText("Start Mixed Interview").assertDoesNotExist()
    }

    @Test
    fun emptyStateRenders() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Empty,
                    onTopicClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("No topics available").assertIsDisplayed()
    }

    @Test
    fun errorStateRendersAndRetryCanBeClicked() = runComposeUiTest {
        var retryCount = 0

        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Error,
                    onTopicClick = {},
                    onRetry = {
                        retryCount += 1
                    },
                )
            }
        }

        onNodeWithText("Topics could not be loaded").assertIsDisplayed()
        onNodeWithText("Retry").performClick()

        assertEquals(1, retryCount)
    }

    @Test
    fun topicRouteUsesStableIdentityOnly() {
        val route = AppRoute.Topic(topicId = "topic_stable_id")

        assertEquals("topic_stable_id", route.topicId)
        assertEquals(null, route.subtopicId)
    }

    @Test
    fun subtopicSearchRouteCarriesOnlyStableTopicAndSubtopicIds() {
        val route = AppRoute.Topic(
            topicId = "topic_stable_id",
            subtopicId = "subtopic_stable_id",
        )

        assertEquals("topic_stable_id", route.topicId)
        assertEquals("subtopic_stable_id", route.subtopicId)
    }
}

/**
 * A Topic row with no learning context: analytics are not what most of these tests are about, and
 * an absent context is the honest representation of history that has not arrived.
 */
private fun topicItem(
    topicId: String,
    topicName: String,
    learningContext: LearningContextUiModel? = null,
) = TopicBrowserItemUiModel(topicId, topicName, learningContext)

/** A weak-area practice intent: a scope and a source, with the builder owning everything else. */
private fun weakAreaPractice() = LearningRecommendationTarget.Practice(
    PracticePreset(
        scope = AssessmentScope.Subtopic("coroutines"),
        source = PracticeQuestionSource.WEAK_AREAS,
    ),
)

private fun unseenPractice() = LearningRecommendationTarget.Practice(
    PracticePreset(
        scope = AssessmentScope.Topic("kotlin"),
        source = PracticeQuestionSource.UNSEEN,
    ),
)

private fun learningContext(
    attempted: Int,
    total: Int,
    accuracy: Double? = null,
    isWeak: Boolean = false,
) = LearningContextUiModel(
    attemptedQuestionCount = attempted,
    totalQuestionCount = total,
    coveragePercentage = if (total == 0) null else attempted.toDouble() / total * 100.0,
    accuracyPercentage = accuracy,
    isWeak = isWeak,
)

private const val TestRootTag = "topic_browser_test_root"
private val TestTopInset = 48.dp

/** Mirrors TopicBrowserHeaderSpacing, which is private to the screen. */
private val TestHeaderSpacing = 12.dp

/** The Material minimum touch target. */
private val MinimumTouchTarget = 48.dp
