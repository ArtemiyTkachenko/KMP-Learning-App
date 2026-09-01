package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic

@OptIn(ExperimentalTestApi::class)
internal class TopicDetailScreenTest {
    @Test
    fun contentRendersTopicSubtopicsCountsAndStartActions() = runComposeUiTest {
        var topicStarts = 0
        var subtopicId: String? = null
        val topic = Topic("topic_a", "Topic A")
        val subtopic = Subtopic("subtopic_a", topic.id, "Subtopic A")

        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Content(
                        topic = topic,
                        topicQuestionCount = 3,
                        subtopics = listOf(SubtopicPracticeItem(subtopic, 2)),
                    ),
                    onBack = {},
                    onStartTopicPractice = { topicStarts += 1 },
                    onStartSubtopicPractice = { subtopicId = it },
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Topic A").assertIsDisplayed()
        onNodeWithText("Subtopic A").assertIsDisplayed()
        onNodeWithText("Available questions: 3").assertIsDisplayed()
        onNodeWithText("Available questions: 2").assertIsDisplayed()
        onNodeWithTag(TopicPracticeButtonTag).performClick()
        assertEquals(1, topicStarts)
    }

    @Test
    fun subtopicStartUsesStableIdAndEmptyItemsAreAbsent() = runComposeUiTest {
        var clicked: String? = null
        val topic = Topic("topic_a", "Topic A")
        val subtopic = Subtopic("subtopic_stable", topic.id, "Visible Subtopic")

        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Content(
                        topic = topic,
                        topicQuestionCount = 1,
                        subtopics = listOf(SubtopicPracticeItem(subtopic, 1)),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = { clicked = it },
                    onRetry = {},
                )
            }
        }

        onNodeWithTag(SubtopicPracticeButtonTag).performClick()
        assertEquals("subtopic_stable", clicked)
        onAllNodesWithText("Empty Subtopic").assertCountEquals(0)
    }

    @Test
    fun noQuestionsAndNotFoundStatesDoNotShowPracticeAction() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.NoQuestions(Topic("topic_a", "Topic A")),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("No practice questions are currently available.").assertIsDisplayed()
        onAllNodesWithText("Start Practice").assertCountEquals(0)
    }

    @Test
    fun loadingAndErrorStatesRenderActions() = runComposeUiTest {
        var retryCount = 0
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Error,
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onRetry = { retryCount += 1 },
                )
            }
        }
        onNodeWithText("Topics could not be loaded").assertIsDisplayed()
        onNodeWithText("Retry").performClick()
        assertEquals(1, retryCount)

        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Loading,
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onRetry = {},
                )
            }
        }
        onNodeWithTag(TopicDetailLoadingTag).assertIsDisplayed()
    }

    @Test
    fun targetSubtopicIsPositionedByStableIdWhenContentLoads() = runComposeUiTest {
        val topic = Topic("topic_a", "Topic A")
        val subtopics = (1..16).map { index ->
            SubtopicPracticeItem(
                subtopic = Subtopic("subtopic_$index", topic.id, "Subtopic $index"),
                questionCount = 1,
            )
        }
        setContent {
            MaterialTheme {
                Box(Modifier.size(400.dp, 360.dp)) {
                    TopicDetailScreen(
                        state = TopicDetailUiState.Content(
                            topic = topic,
                            topicQuestionCount = subtopics.size,
                            subtopics = subtopics,
                        ),
                        targetSubtopicId = "subtopic_15",
                        onBack = {},
                        onStartTopicPractice = {},
                        onStartSubtopicPractice = {},
                        onRetry = {},
                    )
                }
            }
        }

        onNodeWithText("Subtopic 15").assertIsDisplayed()
    }

    @Test
    fun missingTargetSubtopicKeepsNormalTopicContent() = runComposeUiTest {
        val topic = Topic("topic_a", "Topic A")
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Content(
                        topic = topic,
                        topicQuestionCount = 1,
                        subtopics = listOf(
                            SubtopicPracticeItem(
                                Subtopic("subtopic_a", topic.id, "Subtopic A"),
                                1,
                            ),
                        ),
                    ),
                    targetSubtopicId = "retired_subtopic",
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Subtopic A").assertIsDisplayed()
        onNodeWithTag(TopicPracticeButtonTag).assertIsDisplayed()
    }
}
