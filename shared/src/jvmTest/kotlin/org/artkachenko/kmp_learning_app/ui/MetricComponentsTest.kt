package org.artkachenko.kmp_learning_app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressPolicy
import org.artkachenko.kmp_learning_app.ui.theme.AppDarkSemanticColors
import org.artkachenko.kmp_learning_app.ui.theme.AppLightSemanticColors
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme

@OptIn(ExperimentalTestApi::class)
internal class MetricComponentsTest {
    @Test
    fun accuracyFormattingUsesWholeNumbersOrOneDecimalPlace() {
        assertEquals("75%", formatAccuracy(75.0))
        assertEquals("66.7%", formatAccuracy(66.666))
        assertEquals("0%", formatAccuracy(0.0))
        assertEquals("100%", formatAccuracy(100.0))
    }

    @Test
    fun accuracyColourTracksTheDomainWeaknessThreshold() {
        // Below the threshold the domain calls an area weak, so the figure reads as incorrect;
        // at or above it, it stops being a warning. Keyed to the policy constant so the UI and
        // the domain cannot drift apart.
        val threshold = LearningProgressPolicy.WeakAccuracyThresholdPercentage

        assertEquals(AppLightSemanticColors.incorrect, lightColorFor(threshold - 0.1))
        assertEquals(AppLightSemanticColors.partiallyCorrect, lightColorFor(threshold))
        assertEquals(AppLightSemanticColors.partiallyCorrect, lightColorFor(84.9))
        assertEquals(AppLightSemanticColors.correct, lightColorFor(85.0))
        assertEquals(AppLightSemanticColors.correct, lightColorFor(100.0))
    }

    @Test
    fun accuracyColourFollowsTheActiveTheme() {
        assertEquals(AppDarkSemanticColors.incorrect, colorFor(10.0, dark = true))
        assertEquals(AppDarkSemanticColors.correct, colorFor(95.0, dark = true))
    }

    private fun lightColorFor(percentage: Double): Color = colorFor(percentage, dark = false)

    private fun colorFor(percentage: Double, dark: Boolean): Color {
        var captured = Color.Unspecified
        runComposeUiTest {
            setContent {
                AppTheme(darkTheme = dark) { captured = accuracyColor(percentage) }
            }
            waitForIdle()
        }
        return captured
    }
}
