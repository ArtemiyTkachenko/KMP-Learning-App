package org.artkachenko.kmp_learning_app.assessment_review

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import org.artkachenko.kmp_learning_app.ui.theme.AppDarkSemanticColors
import org.artkachenko.kmp_learning_app.ui.theme.AppLightSemanticColors
import org.artkachenko.kmp_learning_app.ui.theme.AppSemanticColors
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme
import org.artkachenko.kmp_learning_app.ui.theme.BodyTextContrast
import org.artkachenko.kmp_learning_app.ui.theme.assertContrastAtLeast

@OptIn(ExperimentalTestApi::class)
internal class AssessmentCompletionHeroTest {

    /**
     * The figure is counted out over half a second, and none of that may reach a screen reader or
     * a test. The node overrides its own `text` with the settled value, so the score is readable on
     * the very first frame — before the count has moved at all — rather than only once it lands.
     *
     * The clock is held still deliberately: with it running, a passing assertion would prove only
     * that the animation finished before the assertion ran.
     */
    @Test
    fun theScoreIsReadableBeforeTheCountHasRun() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            AppTheme(darkTheme = false) {
                AssessmentCompletionHero(
                    correctAnswers = 9,
                    totalQuestions = 10,
                    percentage = 90.0,
                    title = "Practice complete",
                )
            }
        }
        mainClock.advanceTimeByFrame()

        onNodeWithText("90%").assertIsDisplayed()
        onNodeWithText("Score: 9 / 10").assertIsDisplayed()
        onNodeWithText("Practice complete").assertIsDisplayed()
    }

    /**
     * Below five questions a percentage claims a precision the run does not have, so the raw score
     * is the figure instead. That rule predates the hero and is unchanged by it; what is asserted
     * here is that the hero did not quietly start printing "50%" for a two-of-four run.
     */
    @Test
    fun tooFewQuestionsForAPercentageShowTheRawScoreAsTheFigure() = runComposeUiTest {
        setContent {
            AppTheme(darkTheme = false) {
                AssessmentCompletionHero(
                    correctAnswers = 2,
                    totalQuestions = 4,
                    percentage = 50.0,
                    title = "Practice complete",
                )
            }
        }

        onNodeWithText("Score: 2 / 4").assertIsDisplayed()
        onNodeWithText("50%").assertDoesNotExist()
    }

    /**
     * The one thing about this card that is information rather than decoration.
     *
     * The hero puts the accuracy percentage in its own semantic colour on top of the brand
     * gradient, which is a pairing neither palette was designed against: the gradient's documented
     * contract covers `onPrimaryContainer` and nothing else. Both endpoints are checked against all
     * three accuracy colours, because a linear sweep passes through nothing darker or lighter than
     * the two colours it interpolates.
     */
    @Test
    fun everyAccuracyColourIsLegibleAcrossTheHeroGradient() {
        listOf(
            "light" to AppLightSemanticColors,
            "dark" to AppDarkSemanticColors,
        ).forEach { (name, semantic) ->
            val accuracyColors = listOf(
                "correct" to semantic.correct,
                "partially correct" to semantic.partiallyCorrect,
                "incorrect" to semantic.incorrect,
            )
            semantic.gradientEndpoints().forEach { (endpoint, background) ->
                accuracyColors.forEach { (role, foreground) ->
                    assertContrastAtLeast(
                        foreground,
                        background,
                        BodyTextContrast,
                        "$name $role figure on the hero gradient $endpoint",
                    )
                }
            }
        }
    }

    private fun AppSemanticColors.gradientEndpoints(): List<Pair<String, Color>> = listOf(
        "start" to heroGradientStart,
        "end" to heroGradientEnd,
    )
}
