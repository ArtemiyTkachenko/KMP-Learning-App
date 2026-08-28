package org.artkachenko.kmp_learning_app.assessment_review

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class AssessmentReviewComponentsTest {
    @Test
    fun questionReviewRendersAnswerMeaningExplanationAndOrderedSources() = runComposeUiTest {
        val openedUrls = mutableListOf<String>()
        setContent {
            MaterialTheme {
                ReviewQuestionCard(
                    question = ReviewQuestionUiModel(
                        questionId = "q1",
                        topicId = "topic",
                        text = "Question text",
                        isCorrect = false,
                        answers = listOf(
                            ReviewAnswerUiModel("a", "Selected wrong", true, false),
                            ReviewAnswerUiModel("b", "Missed correct", false, true),
                        ),
                        explanation = "Authored explanation",
                        sources = listOf(
                            ReviewSourceUiModel("Source B", "https://example.com/b"),
                            ReviewSourceUiModel("Source A", "https://example.com/a"),
                        ),
                    ),
                    onSourceClick = { openedUrls += it },
                )
            }
        }

        onNodeWithText("Incorrect").assertIsDisplayed()
        onNodeWithText("Your answer").assertIsDisplayed()
        onNodeWithText("Correct answer").assertIsDisplayed()
        onNodeWithText("Authored explanation").assertIsDisplayed()
        onNodeWithText("Source: Source B").performClick()
        onNodeWithText("Source: Source A").performClick()
        assertEquals(
            listOf("https://example.com/b", "https://example.com/a"),
            openedUrls,
        )
    }

    @Test
    fun missingQuestionIsExplicit() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MissingReviewQuestion("missing-id")
            }
        }

        onNodeWithText("Question missing-id is no longer available.").assertIsDisplayed()
    }
}
