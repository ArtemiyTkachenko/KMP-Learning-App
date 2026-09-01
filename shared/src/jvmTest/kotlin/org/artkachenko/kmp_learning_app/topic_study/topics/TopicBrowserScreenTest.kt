package org.artkachenko.kmp_learning_app.topic_study.topics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
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
import org.artkachenko.kmp_learning_app.curriculum.Topic

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
                            Topic("topic_a", "Topic A"),
                            Topic("topic_b", "Topic B"),
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
                        topics = listOf(Topic("topic_a", "Topic A")),
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
                            Topic("compose", "Compose Overview"),
                            Topic("android", "Android UI"),
                        ),
                        query = "compose",
                        topicMatches = listOf(
                            TopicSearchResult("compose", "Compose Overview"),
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
                        topics = listOf(Topic("topic_a", "Topic A")),
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
                        topics = listOf(Topic("architecture", "Architecture")),
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
    fun topicClickReturnsStableTopicId() = runComposeUiTest {
        var clickedTopicId: String? = null

        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(Topic("topic_stable_id", "Topic Name")),
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

private const val TestRootTag = "topic_browser_test_root"
private val TestTopInset = 48.dp

/** Mirrors TopicBrowserHeaderSpacing, which is private to the screen. */
private val TestHeaderSpacing = 12.dp
