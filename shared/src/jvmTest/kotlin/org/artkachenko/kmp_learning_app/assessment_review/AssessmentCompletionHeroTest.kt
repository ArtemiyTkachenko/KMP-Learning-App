package org.artkachenko.kmp_learning_app.assessment_review

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressPolicy
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

        onNodeWithText("9 / 10").assertIsDisplayed()
        onNodeWithText("90% correct").assertIsDisplayed()
        onNodeWithText("Practice complete").assertIsDisplayed()
    }

    /**
     * Below five questions a percentage claims a precision the run does not have, so the score
     * stands alone. That rule predates the hero and is unchanged by it; what is asserted here is
     * that the hero did not quietly start printing "50%" for a two-of-four run, and that the line
     * under the figure still says what the figure is.
     */
    @Test
    fun tooFewQuestionsForAPercentageLeaveTheFigureUnqualified() = runComposeUiTest {
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

        onNodeWithText("2 / 4").assertIsDisplayed()
        onNodeWithText("correct").assertIsDisplayed()
        onNodeWithText("50% correct").assertDoesNotExist()
    }

    /**
     * The ring is a picture of a number that is already written twice beside it.
     *
     * Material's determinate `CircularProgressIndicator` publishes `progressBarRangeInfo`, which on
     * this card would hand assistive technology a third statement of the same fact — and, for the
     * first half-second, a *wrong* one, because it would report wherever the reveal had got to
     * rather than the score. The hero clears it, so this asserts there is no progress semantics on
     * the card at all.
     */
    @Test
    fun theRingIsNotAnnouncedBecauseTheFigureAlreadyStatesTheScore() = runComposeUiTest {
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

        onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
        ).assertCountEquals(0)
    }

    /**
     * A weak result is encouragement, not a fault.
     *
     * The band is asserted rather than the colour, because the band is the decision: what must hold
     * is that a run below the domain's own weakness threshold lands in [ResultEmphasis.LOW], which
     * the hero paints in the app's warning amber. Every other metric in the product resolves that
     * band to `semanticColors.incorrect` via `accuracyColor` — the same red an answer gets for being
     * wrong — and this is the one surface where that would be a verdict on the learner rather than a
     * statement about content.
     */
    @Test
    fun aWeakResultIsTheLowBandRatherThanAnError() {
        assertEquals(ResultEmphasis.LOW, resultEmphasisFor(0.0))
        assertEquals(ResultEmphasis.LOW, resultEmphasisFor(30.0))
        assertEquals(
            ResultEmphasis.LOW,
            resultEmphasisFor(LearningProgressPolicy.WeakAccuracyThresholdPercentage - 0.1),
        )
        assertEquals(
            ResultEmphasis.MIXED,
            resultEmphasisFor(LearningProgressPolicy.WeakAccuracyThresholdPercentage),
        )
        assertEquals(ResultEmphasis.MIXED, resultEmphasisFor(84.9))
        assertEquals(ResultEmphasis.STRONG, resultEmphasisFor(85.0))
        assertEquals(ResultEmphasis.STRONG, resultEmphasisFor(100.0))
    }

    /**
     * The one thing about this card that is information rather than decoration.
     *
     * The hero puts its emphasis colour on the ring and the accuracy line on top of the brand
     * gradient, which is a pairing neither palette was designed against: the gradient's documented
     * contract covers `onPrimaryContainer` and nothing else. Both endpoints are checked, because a
     * linear sweep passes through nothing darker or lighter than the two colours it interpolates.
     *
     * `incorrect` is deliberately absent: the hero never renders it, and
     * [aWeakResultIsTheLowBandRatherThanAnError] is what keeps that true. `onPrimaryContainer` — the
     * MIXED band and the colour of the figure itself — is covered by `AppColorSchemeTest` against
     * the same two endpoints.
     */
    @Test
    fun everyEmphasisColourIsLegibleAcrossTheHeroGradient() {
        listOf(
            "light" to AppLightSemanticColors,
            "dark" to AppDarkSemanticColors,
        ).forEach { (name, semantic) ->
            val emphasisColors = listOf(
                "strong" to semantic.correct,
                "low" to semantic.partiallyCorrect,
            )
            semantic.gradientEndpoints().forEach { (endpoint, background) ->
                emphasisColors.forEach { (role, foreground) ->
                    assertContrastAtLeast(
                        foreground,
                        background,
                        BodyTextContrast,
                        "$name $role emphasis on the hero gradient $endpoint",
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
