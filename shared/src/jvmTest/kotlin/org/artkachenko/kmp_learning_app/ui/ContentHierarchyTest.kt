package org.artkachenko.kmp_learning_app.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme

@OptIn(ExperimentalTestApi::class)
internal class ContentHierarchyTest {
    @Test
    fun sectionHeadingExposesItsHierarchyToAccessibilityServices() = runComposeUiTest {
        setContent {
            AppTheme {
                SectionHeading("Question review")
            }
        }

        onNodeWithText("Question review").assert(isHeading())
    }
}
