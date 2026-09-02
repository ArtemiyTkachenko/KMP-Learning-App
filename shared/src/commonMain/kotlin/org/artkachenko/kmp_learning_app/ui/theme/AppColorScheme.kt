package org.artkachenko.kmp_learning_app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Explicit Material 3 schemes.
 *
 * The app previously called `MaterialTheme { }` with no arguments, which meant the M3 baseline
 * purple palette and — because `isSystemInDarkTheme()` was never consulted — a permanently light
 * UI on every host. These roles are derived from a single indigo seed so the product has its own
 * identity, and the container/variant roles are filled in because the screens rely on them for
 * hierarchy (see [AppSemanticColors] for correct/incorrect, which M3 does not model).
 *
 * **Every role is now stated.** The schemes used to name only the roles the screens read directly,
 * which left the rest — `inverseSurface`, `inversePrimary`, `inverseOnSurface`, `scrim`,
 * `surfaceBright`, `surfaceDim`, `surfaceTint`, and the whole fixed-colour family — resolving to
 * `lightColorScheme`/`darkColorScheme` defaults, which are the Material *baseline purple*. Nothing
 * looked wrong while no component happened to read one, but a Snackbar draws its container from
 * `inverseSurface` and its action from `inversePrimary`, so the first one shown would have arrived
 * in a palette this app never chose. Partial schemes fail quietly and late; this one cannot.
 *
 * The fixed-colour roles hold the same value in both schemes. That is their definition: they exist
 * for content that must not flip with the theme, so light and dark deliberately agree.
 */
internal val AppLightColorScheme = lightColorScheme(
    primary = Color(0xFF3F5BA9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBE1FF),
    onPrimaryContainer = Color(0xFF00174B),
    secondary = Color(0xFF5A5D72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDFE1F9),
    onSecondaryContainer = Color(0xFF171B2C),
    tertiary = Color(0xFF76546E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD7F3),
    onTertiaryContainer = Color(0xFF2C1229),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE2E1EC),
    onSurfaceVariant = Color(0xFF45464F),
    outline = Color(0xFF767680),
    outlineVariant = Color(0xFFC6C5D0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F2FA),
    surfaceContainer = Color(0xFFEFEDF4),
    surfaceContainerHigh = Color(0xFFE9E7EF),
    surfaceContainerHighest = Color(0xFFE3E1E9),
    // The dimmest and brightest neutrals the light scheme can reach. Components that need to sit
    // below or above the whole container ramp use these rather than borrowing a step from it.
    surfaceDim = Color(0xFFDBD9E1),
    surfaceBright = Color(0xFFFBF8FF),
    // Inverted surfaces: Snackbars and tooltips paint a dark card in the light theme. Without these
    // the app's first Snackbar would have appeared in Material's baseline palette.
    inverseSurface = Color(0xFF303036),
    inverseOnSurface = Color(0xFFF2F0F7),
    inversePrimary = Color(0xFFB4C5FF),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF3F5BA9),
    primaryFixed = Color(0xFFDBE1FF),
    primaryFixedDim = Color(0xFFB4C5FF),
    onPrimaryFixed = Color(0xFF00174B),
    onPrimaryFixedVariant = Color(0xFF264190),
    secondaryFixed = Color(0xFFDFE1F9),
    secondaryFixedDim = Color(0xFFC3C5DD),
    onSecondaryFixed = Color(0xFF171B2C),
    onSecondaryFixedVariant = Color(0xFF424659),
    tertiaryFixed = Color(0xFFFFD7F3),
    tertiaryFixedDim = Color(0xFFE5BAD8),
    onTertiaryFixed = Color(0xFF2C1229),
    onTertiaryFixedVariant = Color(0xFF5C3C56),
)

internal val AppDarkColorScheme = darkColorScheme(
    primary = Color(0xFFB4C5FF),
    onPrimary = Color(0xFF092B78),
    primaryContainer = Color(0xFF264190),
    onPrimaryContainer = Color(0xFFDBE1FF),
    secondary = Color(0xFFC3C5DD),
    onSecondary = Color(0xFF2C2F42),
    secondaryContainer = Color(0xFF424659),
    onSecondaryContainer = Color(0xFFDFE1F9),
    tertiary = Color(0xFFE5BAD8),
    onTertiary = Color(0xFF44263F),
    tertiaryContainer = Color(0xFF5C3C56),
    onTertiaryContainer = Color(0xFFFFD7F3),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE3E1E9),
    surface = Color(0xFF121318),
    onSurface = Color(0xFFE3E1E9),
    surfaceVariant = Color(0xFF45464F),
    onSurfaceVariant = Color(0xFFC6C5D0),
    outline = Color(0xFF90909A),
    outlineVariant = Color(0xFF45464F),
    surfaceContainerLowest = Color(0xFF0D0E13),
    surfaceContainerLow = Color(0xFF1B1B21),
    surfaceContainer = Color(0xFF1F1F25),
    surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF34343A),
    surfaceDim = Color(0xFF121318),
    surfaceBright = Color(0xFF38383F),
    // The mirror of the light scheme's inversion: a Snackbar in the dark theme is a light card.
    inverseSurface = Color(0xFFE3E1E9),
    inverseOnSurface = Color(0xFF303036),
    inversePrimary = Color(0xFF3F5BA9),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFFB4C5FF),
    // Identical to the light scheme by definition: fixed roles do not flip with the theme.
    primaryFixed = Color(0xFFDBE1FF),
    primaryFixedDim = Color(0xFFB4C5FF),
    onPrimaryFixed = Color(0xFF00174B),
    onPrimaryFixedVariant = Color(0xFF264190),
    secondaryFixed = Color(0xFFDFE1F9),
    secondaryFixedDim = Color(0xFFC3C5DD),
    onSecondaryFixed = Color(0xFF171B2C),
    onSecondaryFixedVariant = Color(0xFF424659),
    tertiaryFixed = Color(0xFFFFD7F3),
    tertiaryFixedDim = Color(0xFFE5BAD8),
    onTertiaryFixed = Color(0xFF2C1229),
    onTertiaryFixedVariant = Color(0xFF5C3C56),
)
