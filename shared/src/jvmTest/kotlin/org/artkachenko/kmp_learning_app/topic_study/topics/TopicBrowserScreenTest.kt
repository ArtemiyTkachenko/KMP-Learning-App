package org.artkachenko.kmp_learning_app.topic_study.topics

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.AppRoute
import org.artkachenko.kmp_learning_app.curriculum.Topic

@OptIn(ExperimentalTestApi::class)
internal class TopicBrowserScreenTest {
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
                    onStartMixedInterview = {},
                    onOpenProgress = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Topics").assertIsDisplayed()
        onNodeWithText("Topic A").assertIsDisplayed()
        onNodeWithText("Topic B").assertIsDisplayed()
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
                    onStartMixedInterview = {},
                    onOpenProgress = {},
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
                    onStartMixedInterview = {},
                    onOpenProgress = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithTag(TopicBrowserLoadingTag).assertIsDisplayed()
        onNodeWithText("Loading topics").assertIsDisplayed()
        onNodeWithText("View progress").assertIsDisplayed()
        onNodeWithText("Start Mixed Interview").assertIsDisplayed()
    }

    @Test
    fun emptyStateRenders() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Empty,
                    onTopicClick = {},
                    onStartMixedInterview = {},
                    onOpenProgress = {},
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
                    onStartMixedInterview = {},
                    onOpenProgress = {},
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
    fun mixedInterviewEntryRendersAndInvokesStartOnce() = runComposeUiTest {
        var startCount = 0

        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Content(
                        topics = listOf(Topic("topic", "Topic")),
                    ),
                    onTopicClick = {},
                    onStartMixedInterview = { startCount += 1 },
                    onOpenProgress = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Mixed Android Interview").assertIsDisplayed()
        onNodeWithText("20-question interview").assertIsDisplayed()
        onNodeWithText("Start Mixed Interview").performClick()

        assertEquals(1, startCount)
    }

    @Test
    fun progressEntryRendersAndInvokesCallbackOnce() = runComposeUiTest {
        var openCount = 0

        setContent {
            MaterialTheme {
                TopicBrowserScreen(
                    state = TopicBrowserUiState.Error,
                    onTopicClick = {},
                    onStartMixedInterview = {},
                    onOpenProgress = { openCount += 1 },
                    onRetry = {},
                )
            }
        }

        onNodeWithText("View progress").assertIsDisplayed().performClick()

        assertEquals(1, openCount)
    }

    @Test
    fun topicRouteUsesStableIdentityOnly() {
        val route = AppRoute.Topic(topicId = "topic_stable_id")

        assertEquals("topic_stable_id", route.topicId)
    }
}
