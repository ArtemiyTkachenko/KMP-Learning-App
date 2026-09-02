package org.artkachenko.kmp_learning_app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The product shape scale.
 *
 * The app previously inherited the Material baseline scale (4/8/12/16/28). The consequential value
 * there is `medium` at 12.dp, because `shapes.medium` is the shape of almost every content
 * surface in this product — Topic rows, performance rows, answer options — and a 12.dp corner on a
 * full-width card is the most generic shape Material can produce. Raising it to 16.dp is the single
 * change here that most alters how considered the app looks, and it costs one file.
 *
 * The values follow Material 3's expressive shape scale rather than being invented: `large` is the
 * expressive `largeIncreased` step (20.dp), which keeps a summary card clearly rounder than the
 * rows beneath it now that those rows have moved up to 16.dp. `extraSmall` and `small` move off the
 * 4.dp grid on purpose — a corner radius is an optical value, not a layout measurement, and 4.dp on
 * an 8.dp-tall element reads as square.
 *
 * [Shapes] does not model a pill. Components that need one keep `RoundedCornerShape(percent = 50)`
 * locally, because a pill is a function of the element's own height rather than a scale step.
 */
internal val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
