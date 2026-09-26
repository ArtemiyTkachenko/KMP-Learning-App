package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import org.artkachenko.kmp_learning_app.ui.theme.AppDarkColorScheme
import org.artkachenko.kmp_learning_app.ui.theme.AppDarkSemanticColors
import org.artkachenko.kmp_learning_app.ui.theme.AppLightColorScheme
import org.artkachenko.kmp_learning_app.ui.theme.AppLightSemanticColors
import org.artkachenko.kmp_learning_app.ui.theme.AppSemanticColors
import org.artkachenko.kmp_learning_app.ui.theme.BodyTextContrast
import org.artkachenko.kmp_learning_app.ui.theme.assertContrastAtLeast

/**
 * The interview hero is the brand gradient's third call site, and it puts one thing on the sweep
 * that neither of the other two does: a **filled button**.
 *
 * Everything else about this surface is already covered. `AppColorSchemeTest` holds the gradient's
 * own contract — that `onPrimaryContainer` carries text across both endpoints in both schemes —
 * and the hero's titles, figure, and rule titles take exactly that colour. `ProgressHeroThemeTest`
 * measures `onPrimaryContainer` at the shared 0.8 supporting alpha, composited, over all four
 * endpoints, which is the colour this hero's description and rule details take. Repeating either
 * here would be two tests holding one answer.
 *
 * What is new is `primary` filled with `onPrimary` on it, sitting directly on the sweep rather than
 * on a surface from the neutral ramp. Two things have to hold and neither is implied by the
 * gradient's contract: the button has to be *findable* as an object against the surface behind it,
 * and its label has to be readable on its own fill. The first is the interesting one — in the light
 * scheme `heroGradientStart` is byte-for-byte `primaryContainer`, so the action and its background
 * are two tones of one hue, and a palette edit that lightened the brand or deepened the sweep could
 * quietly bring them together.
 */
internal class InterviewStartHeroThemeTest {

    /**
     * The Start action reads as an object on the gradient, in both schemes and at both endpoints.
     *
     * The threshold is WCAG 2.1 SC 1.4.11 *Non-text Contrast*, which asks 3:1 between a user
     * interface component and the colour adjacent to it. This is a boundary between two filled
     * areas rather than a text pairing, which is why it is not [BodyTextContrast].
     */
    @Test
    fun theStartActionSeparatesFromTheHeroGradient() {
        forEachGradientEndpoint { name, scheme, endpoint, background ->
            assertContrastAtLeast(
                scheme.primary,
                background,
                NonTextContrast,
                "$name Start action on the interview hero gradient $endpoint",
            )
        }
    }

    /** The action's own label, which is a text pairing and takes the text threshold. */
    @Test
    fun theStartActionLabelIsLegibleOnItsOwnFill() {
        listOf(
            "light" to AppLightColorScheme,
            "dark" to AppDarkColorScheme,
        ).forEach { (name, scheme) ->
            assertContrastAtLeast(
                scheme.onPrimary,
                scheme.primary,
                BodyTextContrast,
                "$name Start action label",
            )
        }
    }

    private fun forEachGradientEndpoint(
        block: (name: String, scheme: ColorScheme, endpoint: String, background: Color) -> Unit,
    ) {
        listOf(
            Triple("light", AppLightColorScheme, AppLightSemanticColors),
            Triple("dark", AppDarkColorScheme, AppDarkSemanticColors),
        ).forEach { (name, scheme, semantic: AppSemanticColors) ->
            listOf(
                "start" to semantic.heroGradientStart,
                "end" to semantic.heroGradientEnd,
            ).forEach { (endpoint, background) ->
                block(name, scheme, endpoint, background)
            }
        }
    }
}

/** WCAG 2.1 SC 1.4.11: the minimum between a UI component and what it sits on. */
private const val NonTextContrast: Double = 3.0
