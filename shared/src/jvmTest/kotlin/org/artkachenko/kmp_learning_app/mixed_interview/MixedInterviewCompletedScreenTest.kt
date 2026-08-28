package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class MixedInterviewCompletedScreenTest {
    @Test
    fun completionShellRendersAndSupportsBack() = runComposeUiTest {
        var backCount = 0
        setContent {
            MaterialTheme {
                MixedInterviewCompletedScreen(
                    onBack = { backCount += 1 },
                )
            }
        }

        onNodeWithText("Interview completed").assertIsDisplayed()
        onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backCount)
    }
}
