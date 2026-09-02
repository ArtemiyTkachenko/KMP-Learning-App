package org.artkachenko.kmp_learning_app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The product type scale.
 *
 * The app previously passed no `typography` to `MaterialTheme`, so it ran on the Material baseline
 * scale and every screen that wanted more weight than the baseline gives reached for `fontWeight`
 * at the call site. That happened thirteen times across eight files, which is exactly the drift a
 * `Typography` exists to prevent: two screens showing the same role could disagree, and nothing
 * failed when they did.
 *
 * Ten of those became redundant once the weights moved here. The other three were not headings at
 * all but the numeric figure a card exists to show, which is why they are a shared component —
 * `MetricFigure` — rather than a heavier value on a heading role.
 *
 * No font resource is bundled. `AppIcons` is hand-authored specifically to keep the JS and Wasm
 * bundles small, and a variable font would cost several times what that saved. The identity here
 * comes from weight and tracking on each platform's own default family instead.
 *
 * Tracking moves in opposite directions by size, which is the standard optical correction: large
 * text is set tighter because generous default tracking reads as loose at display sizes, and small
 * label text is set looser because it is usually uppercase-ish, short, and needs the separation.
 */
private val Default = Typography()

internal val AppTypography = Default.copy(
    displayLarge = Default.displayLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1.0).sp,
    ),
    displayMedium = Default.displayMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.75).sp,
    ),
    // The headline accuracy figure. It is the largest number on four screens and previously carried
    // `FontWeight.Bold` inline in AccuracyHeadline.
    displaySmall = Default.displaySmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = Default.headlineLarge.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.35).sp,
    ),
    // Screen titles that are not in a TopAppBar: the Topics and Interview headers.
    headlineMedium = Default.headlineMedium.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.25).sp,
    ),
    headlineSmall = Default.headlineSmall.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.25).sp,
    ),
    // Section headings, and the trailing accuracy figure on a performance row.
    titleLarge = Default.titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.15).sp,
    ),
    // The workhorse: every card title, metric value, and Topic name.
    titleMedium = Default.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
    ),
    // Badge and tag text. Deliberately the widest tracking in the scale: these are two or three
    // words inside a tinted pill, where letters set at body tracking crowd their own container.
    labelMedium = Default.labelMedium.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
    ),
    labelSmall = Default.labelSmall.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.6.sp,
    ),
)
