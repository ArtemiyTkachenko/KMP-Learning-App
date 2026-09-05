package org.artkachenko.kmp_learning_app.topic_study.learning_lesson

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The minimal Lesson surface E21-03 owns: identity, the three non-content states, and Back.
 * Structured block rendering and Sources belong to E21-04 and are deliberately untested here.
 */
@OptIn(ExperimentalTestApi::class)
internal class LearningLessonScreenTest {
    @Test
    fun loadingStateRenders() = runComposeUiTest {
        setContent {
            MaterialTheme {
                LearningLessonScreen(LearningLessonUiState.Loading, {}, {})
            }
        }

        onNodeWithTag(LearningLessonLoadingTag).assertIsDisplayed()
        onNodeWithText("Loading lesson").assertIsDisplayed()
    }

    @Test
    fun notFoundStateSaysTheLessonIsUnavailableWithoutOfferingRetry() = runComposeUiTest {
        setContent {
            MaterialTheme {
                LearningLessonScreen(LearningLessonUiState.NotFound, {}, {})
            }
        }

        onNodeWithText("This lesson is not available.").assertIsDisplayed()
        onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test
    fun errorStateRendersAndRetries() = runComposeUiTest {
        var retryCount = 0
        setContent {
            MaterialTheme {
                LearningLessonScreen(LearningLessonUiState.Error, {}, { retryCount += 1 })
            }
        }

        onNodeWithText("Learning content could not be loaded.").assertIsDisplayed()
        onNodeWithText("Retry").performClick()
        assertEquals(1, retryCount)
    }

    @Test
    fun contentRendersTitleAndSummaryAndBackIsAvailable() = runComposeUiTest {
        var backCount = 0
        setContent {
            MaterialTheme {
                LearningLessonScreen(
                    state = LearningLessonUiState.Content(
                        unitId = "unit_thinking_in_compose",
                        lessonId = "lesson_declarative_ui",
                        title = "Declarative UI",
                        summary = "Why declarative UI exists.",
                    ),
                    onBack = { backCount += 1 },
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Declarative UI").assertIsDisplayed()
        onNodeWithText("Why declarative UI exists.").assertIsDisplayed()

        onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backCount)
    }
}
