package org.artkachenko.kmp_learning_app.assessment.start

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme

@OptIn(ExperimentalTestApi::class)
internal class AssessmentLaunchDialogTest {
    @Test
    fun launchingShowsBlockingProgress() = runComposeUiTest {
        setContent {
            AppTheme {
                AssessmentLaunchDialog(
                    state = AssessmentLaunchState.Launching,
                    onRetry = {},
                    onDismissFailure = {},
                )
            }
        }

        onNodeWithTag(AssessmentLaunchDialogTag).assertIsDisplayed()
        onNodeWithTag(AssessmentLaunchLoadingTag).assertIsDisplayed()
        onNodeWithText("Preparing your assessment…").assertIsDisplayed()
    }

    @Test
    fun noQuestionsFailureOffersRetryAndCancel() = runComposeUiTest {
        val actions = mutableListOf<String>()
        setContent {
            AppTheme {
                AssessmentLaunchDialog(
                    state = AssessmentLaunchState.Failed(
                        config = AssessmentConfig.Mixed(questionCount = 20),
                        reason = AssessmentLaunchFailure.NoEligibleQuestions,
                    ),
                    onRetry = { actions += "retry" },
                    onDismissFailure = { actions += "cancel" },
                )
            }
        }

        onNodeWithText("No questions are available for this selection.").assertIsDisplayed()
        onNodeWithText("Retry").performClick()
        onNodeWithText("Cancel").performClick()
        assertEquals(listOf("retry", "cancel"), actions)
    }
}
