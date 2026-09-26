package org.artkachenko.kmp_learning_app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Palette-wide invariants for both schemes.
 *
 * This is the counterpart to `TopicDiscoveryThemeTest`, which checks the handful of roles one
 * screen puts together. The properties here belong to the palette itself and hold no matter what
 * composes it, so they are asserted once against the schemes rather than once per screen.
 *
 * Nothing here pins a hex value. A value assertion would fail on every deliberate change and pass
 * on every accidental one, which is the wrong way round; what these tests defend is the *shape* of
 * the palette — that the levels stay apart, that every paired role stays readable, and that the
 * light and dark schemes stay different systems rather than one inverted.
 */
internal class AppColorSchemeTest {

    /**
     * The surface hierarchy, level by level.
     *
     * The three levels are `background` (the page), `surfaceContainerLow` (ordinary content), and
     * `surfaceContainer` and above (raised or interactive). The failure this guards is the one the
     * palette was revised to fix: the light ramp stepped about 6/255 and the dark ramp as little as
     * 4/255 between consecutive levels, which is below what a display in a lit room resolves, so a
     * card on a page and a card on a card read as a single tone.
     *
     * 1.06 is chosen to sit above what the previous light ramp achieved (1.048–1.055) and below
     * what this one does (1.070–1.075 light, 1.097–1.192 dark). It is a floor on the *smallest*
     * step, so widening one level cannot pay for flattening another.
     */
    @Test
    fun bothSchemesKeepConsecutiveSurfaceLevelsApart() {
        forEachScheme { name, scheme ->
            val ramp = listOf(
                "background" to scheme.background,
                "surfaceContainerLow" to scheme.surfaceContainerLow,
                "surfaceContainer" to scheme.surfaceContainer,
                "surfaceContainerHigh" to scheme.surfaceContainerHigh,
                "surfaceContainerHighest" to scheme.surfaceContainerHighest,
            )
            ramp.zipWithNext { (lowerName, lower), (upperName, upper) ->
                val ratio = contrastRatio(lower, upper)
                assertTrue(
                    ratio >= MinimumSurfaceStep,
                    "$name $lowerName -> $upperName separation is $ratio, below $MinimumSurfaceStep",
                )
            }
        }
    }

    /**
     * Light and dark reach that separation in opposite directions, and that is what makes them two
     * designs rather than one inverted twice.
     *
     * Light is tonal: the page is the brightest surface and every level above it is a deeper tint,
     * so layering never becomes white on white. Dark is luminance: the page is the darkest surface
     * and every level above it is lifted toward light, so separation does not depend on borders.
     */
    @Test
    fun theTwoSchemesLayerInOppositeDirections() {
        assertTrue(
            AppLightColorScheme.background.luminance() >
                AppLightColorScheme.surfaceContainerHighest.luminance(),
            "the light page should be brighter than the surfaces stacked on it",
        )
        assertTrue(
            AppDarkColorScheme.background.luminance() <
                AppDarkColorScheme.surfaceContainerHighest.luminance(),
            "the dark page should be darker than the surfaces stacked on it",
        )
    }

    /**
     * Every paired content/container role in the scheme, at the WCAG 2.1 threshold for body text.
     *
     * Material guarantees this for a generated scheme; this app states its roles by hand, so the
     * pairing is only as good as the last edit to the file. Held to 4.5 rather than the 3:1 large
     * text allowance because any of these containers can carry a sentence.
     */
    @Test
    fun everyPairedRoleCarriesBodyText() {
        forEachScheme { name, scheme ->
            listOf(
                "onPrimary/primary" to (scheme.onPrimary to scheme.primary),
                "onPrimaryContainer/primaryContainer" to
                    (scheme.onPrimaryContainer to scheme.primaryContainer),
                "onSecondary/secondary" to (scheme.onSecondary to scheme.secondary),
                "onSecondaryContainer/secondaryContainer" to
                    (scheme.onSecondaryContainer to scheme.secondaryContainer),
                "onTertiary/tertiary" to (scheme.onTertiary to scheme.tertiary),
                "onTertiaryContainer/tertiaryContainer" to
                    (scheme.onTertiaryContainer to scheme.tertiaryContainer),
                "onError/error" to (scheme.onError to scheme.error),
                "onErrorContainer/errorContainer" to
                    (scheme.onErrorContainer to scheme.errorContainer),
                "onSurfaceVariant/surfaceVariant" to
                    (scheme.onSurfaceVariant to scheme.surfaceVariant),
                "inverseOnSurface/inverseSurface" to
                    (scheme.inverseOnSurface to scheme.inverseSurface),
            ).forEach { (pair, colors) ->
                assertContrastAtLeast(colors.first, colors.second, BodyTextContrast, "$name $pair")
            }
        }
    }

    /**
     * Body text and the brand read on every level of the hierarchy, not only on the page.
     *
     * A role that is legible on `background` and marginal on `surfaceContainerHighest` is a role
     * that fails inside a dialog or a code block rather than on the screen it was chosen against,
     * which is exactly where nobody looks.
     */
    @Test
    fun textAndBrandStayLegibleOnEverySurfaceLevel() {
        forEachScheme { name, scheme ->
            scheme.surfaceLevels().forEach { (level, surface) ->
                assertContrastAtLeast(scheme.onSurface, surface, BodyTextContrast, "$name body on $level")
                assertContrastAtLeast(
                    scheme.onSurfaceVariant,
                    surface,
                    BodyTextContrast,
                    "$name supporting text on $level",
                )
                assertContrastAtLeast(scheme.primary, surface, BodyTextContrast, "$name primary on $level")
            }
        }
    }

    /**
     * The semantic family: containers a block can be filled with, and accents drawn beside neutral
     * text.
     *
     * Each accent is checked against every surface level, because that is how it is actually used —
     * a correct-answer border sits on whatever container the card chose, not on a matching tint.
     *
     * The containers are checked only against the two levels they are ever drawn on: a badge, an
     * answer option, or a callout sits on the page or inside an ordinary card, never inside a
     * raised one. That restriction is not a convenience. A tonal container sits *within* the
     * neutral luminance range by construction, so it necessarily matches the brightness of some
     * step of the ramp — Material's own `primaryContainer` collides with `surfaceContainer` in this
     * scheme exactly as `correctContainer` does. Asserting against every level would therefore
     * demand a container outside the ramp entirely, which is a saturated block, which is the thing
     * this palette was revised to stop drawing.
     */
    @Test
    fun theSemanticFamilyReadsOnEverySurfaceLevel() {
        forEachSemanticPalette { name, scheme, semantic ->
            listOf(
                "correct" to Triple(semantic.correct, semantic.correctContainer, semantic.onCorrectContainer),
                "partiallyCorrect" to Triple(
                    semantic.partiallyCorrect,
                    semantic.partiallyCorrectContainer,
                    semantic.onPartiallyCorrectContainer,
                ),
                "incorrect" to Triple(
                    semantic.incorrect,
                    semantic.incorrectContainer,
                    semantic.onIncorrectContainer,
                ),
            ).forEach { (role, colors) ->
                val (accent, container, onContainer) = colors
                assertContrastAtLeast(onContainer, container, BodyTextContrast, "$name $role container text")
                // A marked answer option fills itself with the container but leaves the answer in
                // the ordinary reading colour, because the option's text is the authored question
                // rather than a statement about the outcome. Both practice and review do this, so
                // `onSurface` has to survive every semantic container as well as its own.
                assertContrastAtLeast(
                    scheme.onSurface,
                    container,
                    BodyTextContrast,
                    "$name body text on the $role container",
                )
                scheme.surfaceLevels().forEach { (level, surface) ->
                    assertContrastAtLeast(accent, surface, BodyTextContrast, "$name $role accent on $level")
                }
                scheme.surfaceLevels().take(2).forEach { (level, surface) ->
                    assertTrue(
                        contrastRatio(container, surface) >= MinimumSurfaceStep,
                        "$name $role container is indistinguishable from $level",
                    )
                }
            }
        }
    }

    /**
     * The selection fill has to be visible on the raised chrome it is drawn on, not only on a page.
     *
     * `secondaryContainer` is the app's selection fill, and it is the one container role used above
     * level 1: the compact navigation pill sits on `surfaceContainer`. The other tonal containers are
     * exempt from the deeper levels for the reason argued on
     * [theSemanticFamilyReadsOnEverySurfaceLevel] — a callout or a badge is only ever drawn on the
     * page or in an ordinary card — and that exemption is exactly what let this one collide with the
     * navigation bar at 1.04:1 in the light scheme, where the pill marking which of four areas the
     * learner is in was very nearly invisible.
     *
     * The levels asserted are the ones the role is actually drawn on, listed so a new call site on a
     * deeper surface is a decision rather than a discovery: `background` (the `NavigationRailItem`
     * indicator), `surfaceContainerLow` (the Topic row's learning-units badge, inside an ordinary
     * card), and `surfaceContainer` (the compact navigation pill). The Practice Builder's options
     * were a `background` call site until they moved to the primary family with the rest of the
     * app's selection surfaces; the rail indicator is what keeps that level asserted.
     */
    @Test
    fun theSelectionFillIsVisibleOnEverySurfaceItIsDrawnOn() {
        forEachScheme { name, scheme ->
            scheme.surfaceLevels().take(3).forEach { (level, surface) ->
                val ratio = contrastRatio(scheme.secondaryContainer, surface)
                assertTrue(
                    ratio >= MinimumSurfaceStep,
                    "$name secondaryContainer is indistinguishable from $level at $ratio",
                )
            }
        }
    }

    /**
     * A fixed role is the light scheme's container value, held across both themes.
     *
     * That is what "fixed" means, and the schemes state it by hand, so it is only as true as the
     * last edit to the file: when the light selection fill was deepened to clear the navigation bar,
     * `secondaryFixed` had to move with it in *both* schemes or the definition would have quietly
     * stopped holding for one family out of three.
     */
    @Test
    fun fixedRolesRestateTheLightSchemeContainers() {
        val light = AppLightColorScheme
        listOf(
            "primaryFixed" to (light.primaryContainer to { s: ColorScheme -> s.primaryFixed }),
            "secondaryFixed" to (light.secondaryContainer to { s: ColorScheme -> s.secondaryFixed }),
            "tertiaryFixed" to (light.tertiaryContainer to { s: ColorScheme -> s.tertiaryFixed }),
            "onPrimaryFixed" to (light.onPrimaryContainer to { s: ColorScheme -> s.onPrimaryFixed }),
            "onSecondaryFixed" to
                (light.onSecondaryContainer to { s: ColorScheme -> s.onSecondaryFixed }),
            "onTertiaryFixed" to
                (light.onTertiaryContainer to { s: ColorScheme -> s.onTertiaryFixed }),
        ).forEach { (role, expectation) ->
            val (container, read) = expectation
            forEachScheme { name, scheme ->
                assertEquals(container, read(scheme), "$name $role")
            }
        }
    }

    /**
     * One red, not two.
     *
     * The scheme's `error` family and the semantic `incorrect` family are the same colours by
     * intent: a product whose failed answer and failed operation are different reds has two error
     * languages and no way for a learner to learn either. Asserted rather than commented, because
     * the two live in different files and drift is silent.
     */
    @Test
    fun theErrorRoleAndTheIncorrectRoleAreOneColour() {
        forEachSemanticPalette { name, scheme, semantic ->
            assertEquals(scheme.error, semantic.incorrect, "$name error vs incorrect")
            assertEquals(scheme.errorContainer, semantic.incorrectContainer, "$name error container")
            assertEquals(scheme.onErrorContainer, semantic.onIncorrectContainer, "$name on error container")
        }
    }

    /**
     * The hero gradient's two properties, checked once here so neither call site has to re-derive
     * them: the endpoints are actually different (a gradient between equal colours is a fill wearing
     * a more expensive API), and `onPrimaryContainer` carries text across the whole sweep, which is
     * the contract the token documents instead of shipping an on-colour of its own.
     *
     * A surface that puts something *other* than `onPrimaryContainer` on the sweep is outside this
     * contract and checks its own colours — `AssessmentCompletionHeroTest` and
     * `ProgressHeroThemeTest` both do.
     */
    @Test
    fun theHeroGradientIsASweepThatTextCanCross() {
        forEachSemanticPalette { name, scheme, semantic ->
            assertTrue(
                semantic.heroGradientStart != semantic.heroGradientEnd,
                "$name hero gradient has identical endpoints",
            )
            assertContrastAtLeast(
                scheme.onPrimaryContainer,
                semantic.heroGradientStart,
                BodyTextContrast,
                "$name hero gradient start",
            )
            assertContrastAtLeast(
                scheme.onPrimaryContainer,
                semantic.heroGradientEnd,
                BodyTextContrast,
                "$name hero gradient end",
            )
        }
    }

    private fun ColorScheme.surfaceLevels(): List<Pair<String, Color>> = listOf(
        "background" to background,
        "surfaceContainerLow" to surfaceContainerLow,
        "surfaceContainer" to surfaceContainer,
        "surfaceContainerHigh" to surfaceContainerHigh,
        "surfaceContainerHighest" to surfaceContainerHighest,
    )

    private fun forEachScheme(block: (String, ColorScheme) -> Unit) {
        block("light", AppLightColorScheme)
        block("dark", AppDarkColorScheme)
    }

    private fun forEachSemanticPalette(block: (String, ColorScheme, AppSemanticColors) -> Unit) {
        block("light", AppLightColorScheme, AppLightSemanticColors)
        block("dark", AppDarkColorScheme, AppDarkSemanticColors)
    }
}

/** WCAG 2.1 body-text minimum. */
internal const val BodyTextContrast: Double = 4.5

/**
 * The floor on one step of the surface ramp, and on a semantic container against a surface.
 *
 * Not a readability threshold — two adjacent neutrals are never a text pairing. It is the smallest
 * luminance difference this palette accepts as *visible*, calibrated against the ramp it replaced.
 */
private const val MinimumSurfaceStep: Double = 1.06

internal fun assertContrastAtLeast(
    foreground: Color,
    background: Color,
    minimumRatio: Double,
    description: String,
) {
    val ratio = contrastRatio(foreground, background)
    assertTrue(
        ratio >= minimumRatio,
        "$description contrast is $ratio, below the required $minimumRatio",
    )
}

/** WCAG 2.1 contrast ratio; Compose's [luminance] is already the relative luminance it uses. */
internal fun contrastRatio(first: Color, second: Color): Double {
    val lighter = maxOf(first.luminance(), second.luminance()).toDouble()
    val darker = minOf(first.luminance(), second.luminance()).toDouble()
    return (lighter + 0.05) / (darker + 0.05)
}
