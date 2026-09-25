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
 *
 * ## The three colour families
 *
 * Every chromatic role in both schemes belongs to one of three hues, and they are neighbours on
 * purpose: **indigo** (primary, ~248°), **periwinkle-grey** (secondary, the same hue drained of
 * chroma), and **violet** (tertiary, ~275°). The tertiary used to be a rose pink, which read as a
 * second, unrelated brand rather than a supporting tone — an Interview-focus callout and a Topic
 * marker on the same screen looked like they came from two products. Moving it to violet makes the
 * whole accent range one sweep from indigo through periwinkle to violet, which is also the
 * direction [AppSemanticColors.heroGradientStart] travels.
 *
 * The neutrals are not grey. They carry a small amount of the same blue so surfaces read as part of
 * the brand rather than as a default canvas underneath it, which is most of what separates a
 * "technical" palette from an uncoloured one. The chroma is low enough that body text on them is
 * still read as black on white.
 *
 * ## Surface hierarchy
 *
 * The screens use three levels and nothing else, mapped onto Material roles rather than onto
 * tokens of this app's own:
 *
 * | Level | Role | What draws it |
 * | --- | --- | --- |
 * | 0 — page | `background` (= `surface`) | `AppNavigationScaffold`'s container |
 * | 1 — ordinary content | `surfaceContainerLow` | most Cards, `SecondarySummaryCard` |
 * | 2 — raised / interactive | `surfaceContainer` and up | `AccuracyHeroCard` (at `surfaceContainerHigh`), a weak `PerformanceCard`, menus, sheets |
 *
 * The complaint this answers is that the levels were nearly the same colour. The light ramp used to
 * step about 6/255 per level and the dark one about 4/255, which is below what a display in a lit
 * room resolves, so a card on a page and a card on a card all read as one tone. The steps are now
 * 8–9/255 in light and 8–11/255 in dark, and `AppColorSchemeTest` asserts a minimum
 * luminance ratio between consecutive levels so a later edit cannot quietly flatten them again.
 *
 * Light and dark reach that separation differently, which is why neither is the other inverted.
 * Light stays *tonal*: the page is the brightest thing on screen — a cool off-white, not white —
 * and every level above it is a slightly deeper, slightly bluer tint, so layering never becomes
 * white-on-white and never needs a shadow to be legible. Dark is *luminance*: the page is near
 * black and each level is lifted toward a lit grey-blue, so separation comes from light rather than
 * from borders, which is what keeps dark cards from turning into flat grey rectangles.
 */
internal val AppLightColorScheme = lightColorScheme(
    // Indigo at full strength. The light theme can afford a deep, saturated brand because it sits
    // on an almost-white page; the dark scheme's periwinkle is the same hue raised for a dark one.
    primary = Color(0xFF3B48A6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDFE1FF),
    onPrimaryContainer = Color(0xFF141C5C),
    secondary = Color(0xFF565D7D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E2F6),
    onSecondaryContainer = Color(0xFF141A33),
    tertiary = Color(0xFF6F4BA0),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEDDCFF),
    onTertiaryContainer = Color(0xFF28074F),
    // Deliberately identical to [AppLightSemanticColors.incorrect] and its container. A product
    // with two different reds — one for a failed answer and one for a failed operation — has two
    // error languages; keeping them one value is what makes red mean a single thing here.
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF7DCD9),
    onErrorContainer = Color(0xFF3F0906),
    background = Color(0xFFF6F7FC),
    onBackground = Color(0xFF1A1B22),
    surface = Color(0xFFF6F7FC),
    onSurface = Color(0xFF1A1B22),
    surfaceVariant = Color(0xFFDFE2EF),
    onSurfaceVariant = Color(0xFF454859),
    outline = Color(0xFF757888),
    outlineVariant = Color(0xFFC2C6D7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEDEFF8),
    surfaceContainer = Color(0xFFE4E7F3),
    surfaceContainerHigh = Color(0xFFDCDFEE),
    surfaceContainerHighest = Color(0xFFD4D8E9),
    // The dimmest and brightest neutrals the light scheme can reach. Components that need to sit
    // below or above the whole container ramp use these rather than borrowing a step from it.
    surfaceDim = Color(0xFFD0D4E6),
    surfaceBright = Color(0xFFF6F7FC),
    // Inverted surfaces: Snackbars and tooltips paint a dark card in the light theme. Without these
    // the app's first Snackbar would have appeared in Material's baseline palette.
    inverseSurface = Color(0xFF2E2F38),
    inverseOnSurface = Color(0xFFF1F1F9),
    inversePrimary = Color(0xFFBAC3FF),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF3B48A6),
    primaryFixed = Color(0xFFDFE1FF),
    primaryFixedDim = Color(0xFFBAC3FF),
    onPrimaryFixed = Color(0xFF141C5C),
    onPrimaryFixedVariant = Color(0xFF2F3C93),
    secondaryFixed = Color(0xFFE0E2F6),
    secondaryFixedDim = Color(0xFFC0C4E4),
    onSecondaryFixed = Color(0xFF141A33),
    onSecondaryFixedVariant = Color(0xFF404667),
    tertiaryFixed = Color(0xFFEDDCFF),
    tertiaryFixedDim = Color(0xFFD7BBFF),
    onTertiaryFixed = Color(0xFF28074F),
    onTertiaryFixedVariant = Color(0xFF553786),
)

internal val AppDarkColorScheme = darkColorScheme(
    // Periwinkle: the light scheme's indigo lifted until it carries text on a near-black page.
    primary = Color(0xFFBAC3FF),
    onPrimary = Color(0xFF1B2775),
    primaryContainer = Color(0xFF2F3C93),
    onPrimaryContainer = Color(0xFFDFE1FF),
    secondary = Color(0xFFC0C4E4),
    onSecondary = Color(0xFF2A3050),
    secondaryContainer = Color(0xFF404667),
    onSecondaryContainer = Color(0xFFE0E2F6),
    tertiary = Color(0xFFD7BBFF),
    onTertiary = Color(0xFF3D1E6E),
    tertiaryContainer = Color(0xFF553786),
    onTertiaryContainer = Color(0xFFEDDCFF),
    // As in the light scheme, one red: these mirror [AppDarkSemanticColors.incorrect].
    error = Color(0xFFF2B8B2),
    onError = Color(0xFF48100D),
    errorContainer = Color(0xFF631513),
    onErrorContainer = Color(0xFFF7DCD9),
    background = Color(0xFF101118),
    onBackground = Color(0xFFE3E4EE),
    surface = Color(0xFF101118),
    onSurface = Color(0xFFE3E4EE),
    surfaceVariant = Color(0xFF44485A),
    onSurfaceVariant = Color(0xFFC3C6D6),
    outline = Color(0xFF8D90A1),
    outlineVariant = Color(0xFF44485A),
    surfaceContainerLowest = Color(0xFF0A0B10),
    surfaceContainerLow = Color(0xFF191B24),
    surfaceContainer = Color(0xFF21242F),
    surfaceContainerHigh = Color(0xFF2B2E3B),
    surfaceContainerHighest = Color(0xFF363A48),
    surfaceDim = Color(0xFF101118),
    surfaceBright = Color(0xFF383C4A),
    // The mirror of the light scheme's inversion: a Snackbar in the dark theme is a light card.
    inverseSurface = Color(0xFFE3E4EE),
    inverseOnSurface = Color(0xFF2E2F38),
    inversePrimary = Color(0xFF3B48A6),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFFBAC3FF),
    // Identical to the light scheme by definition: fixed roles do not flip with the theme.
    primaryFixed = Color(0xFFDFE1FF),
    primaryFixedDim = Color(0xFFBAC3FF),
    onPrimaryFixed = Color(0xFF141C5C),
    onPrimaryFixedVariant = Color(0xFF2F3C93),
    secondaryFixed = Color(0xFFE0E2F6),
    secondaryFixedDim = Color(0xFFC0C4E4),
    onSecondaryFixed = Color(0xFF141A33),
    onSecondaryFixedVariant = Color(0xFF404667),
    tertiaryFixed = Color(0xFFEDDCFF),
    tertiaryFixedDim = Color(0xFFD7BBFF),
    onTertiaryFixed = Color(0xFF28074F),
    onTertiaryFixedVariant = Color(0xFF553786),
)
