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
    }
}
