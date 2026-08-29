package org.artkachenko.kmp_learning_app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Answer-correctness colours.
 *
 * Material 3 has no "success" or "warning" role, so review screens previously borrowed
 * `colorScheme.primary` for a correct answer — which rendered as the baseline purple rather than
 * anything a learner would read as correct. These are explicit product tokens with light and dark
 * values, kept beside the scheme so both stay in step.
 *
 * [partiallyCorrect] covers a multiple-answer question where the learner picked only correct
 * options but missed at least one. That distinction is derived in presentation from data the
 * review models already carry; no scoring behaviour changes.
 */
@Immutable
internal data class AppSemanticColors(
    val correct: Color,
    val onCorrectContainer: Color,
    val correctContainer: Color,
    val partiallyCorrect: Color,
    val onPartiallyCorrectContainer: Color,
    val partiallyCorrectContainer: Color,
    val incorrect: Color,
    val onIncorrectContainer: Color,
    val incorrectContainer: Color,
)

internal val AppLightSemanticColors = AppSemanticColors(
    correct = Color(0xFF1F6E43),
    onCorrectContainer = Color(0xFF00210F),
    correctContainer = Color(0xFFB7F1C8),
    partiallyCorrect = Color(0xFF7A5900),
    onPartiallyCorrectContainer = Color(0xFF261A00),
    partiallyCorrectContainer = Color(0xFFFFDF9B),
    incorrect = Color(0xFFBA1A1A),
    onIncorrectContainer = Color(0xFF410002),
    incorrectContainer = Color(0xFFFFDAD6),
)

internal val AppDarkSemanticColors = AppSemanticColors(
    correct = Color(0xFF9CD5AD),
    onCorrectContainer = Color(0xFFB7F1C8),
    correctContainer = Color(0xFF00522C),
    partiallyCorrect = Color(0xFFF2C047),
    onPartiallyCorrectContainer = Color(0xFFFFDF9B),
    partiallyCorrectContainer = Color(0xFF5C4300),
    incorrect = Color(0xFFFFB4AB),
    onIncorrectContainer = Color(0xFFFFDAD6),
    incorrectContainer = Color(0xFF93000A),
)

/**
 * Static because the value only changes when the whole theme changes, so reads should not
 * introduce recomposition scopes of their own.
 */
internal val LocalAppSemanticColors = staticCompositionLocalOf { AppLightSemanticColors }
