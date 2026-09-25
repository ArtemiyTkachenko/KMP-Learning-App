package org.artkachenko.kmp_learning_app.progress

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import kotlin.test.Test
import org.artkachenko.kmp_learning_app.ui.theme.AppDarkColorScheme
import org.artkachenko.kmp_learning_app.ui.theme.AppDarkSemanticColors
import org.artkachenko.kmp_learning_app.ui.theme.AppLightColorScheme
import org.artkachenko.kmp_learning_app.ui.theme.AppLightSemanticColors
import org.artkachenko.kmp_learning_app.ui.theme.AppSemanticColors
import org.artkachenko.kmp_learning_app.ui.theme.BodyTextContrast
import org.artkachenko.kmp_learning_app.ui.theme.assertContrastAtLeast

/**
 * The Progress hero puts information on the brand gradient, which is a pairing the palette was not
 * designed against: the gradient's documented contract covers `onPrimaryContainer` and nothing
 * else, and `AppColorSchemeTest` verifies exactly that.
 *
 * This hero goes further in two ways, and both are checked here rather than left to the eye.
 *
 * The headline accuracy is tinted with `accuracyColor`, which resolves to one of the three semantic
 * accent colours — *including* `incorrect`, which the completion hero deliberately never renders
 * and so never had to clear. A learner below the weakness threshold is exactly who most needs to
 * read this figure.
 *
 * The supporting text is the hero's on-colour at reduced alpha, which is a colour that exists only
 * once it has been composited over whatever it sits on. The composite is what has to be legible, so
 * the composite is what is measured.
 *
 * Both endpoints are checked in both schemes, because a linear sweep passes through nothing darker
 * or lighter than the two colours it interpolates.
 */
internal class ProgressHeroThemeTest {

    @Test
    fun everyAccuracyBandIsLegibleAcrossTheHeroGradient() {
        forEachGradientEndpoint { name, _, semantic, endpoint, background ->
            listOf(
                "strong" to semantic.correct,
                "weak" to semantic.partiallyCorrect,
                // The band the completion hero refuses to draw and this one must.
                "low" to semantic.incorrect,
            ).forEach { (band, foreground) ->
                assertContrastAtLeast(
                    foreground,
                    background,
                    BodyTextContrast,
                    "$name $band accuracy on the Progress hero gradient $endpoint",
                )
            }
        }
    }

    @Test
    fun supportingHeroTextStaysLegibleOnceItsAlphaIsComposited() {
        forEachGradientEndpoint { name, scheme, _, endpoint, background ->
            assertContrastAtLeast(
                scheme.onPrimaryContainer.copy(alpha = SupportingTextAlpha)
                    .compositeOver(background),
                background,
                BodyTextContrast,
                "$name supporting hero text on the Progress hero gradient $endpoint",
            )
        }
    }

    private fun forEachGradientEndpoint(
        block: (
            name: String,
            scheme: androidx.compose.material3.ColorScheme,
            semantic: AppSemanticColors,
            endpoint: String,
            background: Color,
        ) -> Unit,
    ) {
        listOf(
            Triple("light", AppLightColorScheme, AppLightSemanticColors),
            Triple("dark", AppDarkColorScheme, AppDarkSemanticColors),
        ).forEach { (name, scheme, semantic) ->
            listOf(
                "start" to semantic.heroGradientStart,
                "end" to semantic.heroGradientEnd,
            ).forEach { (endpoint, background) ->
                block(name, scheme, semantic, endpoint, background)
            }
        }
    }
}

/**
 * Mirrors `ProgressHero`'s own supporting-text alpha.
 *
 * Duplicated rather than exposed, because the production constant is private for a reason — it is
 * one surface's decision, not a token — and a test that could only run by widening its visibility
 * would be changing the code to suit itself. The cost of the copy is that the two must be kept in
 * step, which is what the name is for.
 */
private const val SupportingTextAlpha = 0.8f
