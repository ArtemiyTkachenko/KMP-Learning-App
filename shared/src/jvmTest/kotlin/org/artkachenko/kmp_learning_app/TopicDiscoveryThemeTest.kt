package org.artkachenko.kmp_learning_app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.topic_study.topics.SubtopicSearchResult
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserItemUiModel
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserSearchFieldTag
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserScreen
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserUiState
import org.artkachenko.kmp_learning_app.ui.LearningContextUiModel
import org.artkachenko.kmp_learning_app.ui.theme.AppDarkColorScheme
import org.artkachenko.kmp_learning_app.ui.theme.AppDarkSemanticColors
import org.artkachenko.kmp_learning_app.ui.theme.AppLightColorScheme
import org.artkachenko.kmp_learning_app.ui.theme.AppLightSemanticColors
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme
import org.artkachenko.kmp_learning_app.ui.topicVisualMarkerTag

/**
 * Discovery in both themes.
 *
 * The screen and the shell are composed together, because the hierarchy E13-02 introduced is a
 * relationship between the page background the shell paints and the surfaces the screen puts on
 * top of it — neither says anything on its own.
 *
 * Two kinds of check, deliberately: that every representative discovery state composes and keeps
 * its content in either theme, and that the colour roles those states rely on stay separated in
 * both palettes. Exact palette values belong to the theme source, so nothing here asserts one; a
 * screenshot suite would be the wrong weight for a verification issue.
 */
@OptIn(ExperimentalTestApi::class)
internal class TopicDiscoveryThemeTest {
    @Test
    fun everyDiscoveryStateKeepsItsContentInTheLightTheme() = runComposeUiTest {
        assertDiscoveryStatesRender(darkTheme = false)
    }

    @Test
    fun everyDiscoveryStateKeepsItsContentInTheDarkTheme() = runComposeUiTest {
        assertDiscoveryStatesRender(darkTheme = true)
    }

    @Test
    fun bothPalettesKeepTheDiscoverySurfacesApartAndTheMarkerLegible() {
        val semanticByName = mapOf(
            "light" to AppLightSemanticColors,
            "dark" to AppDarkSemanticColors,
        )
        listOf("light" to AppLightColorScheme, "dark" to AppDarkColorScheme).forEach { (name, scheme) ->
            // Three tones carry the whole hierarchy: the page, the Topic card on it, and the
            // marker inside the card. If any two collapse into the same value the screen becomes
            // flat, which is precisely what E13-02 replaced outlines with.
            assertNotEquals(scheme.background, scheme.surfaceContainerLow, "$name page vs card")
            assertNotEquals(
                scheme.surfaceContainerLow,
                scheme.primaryContainer,
                "$name card vs marker",
            )

            // WCAG 2.1 asks 3:1 of a meaningful graphic against its background and 4.5:1 of body
            // text. The marker glyph is the tinted graphic; the Topic name and its supporting
            // parent-Topic line are the text.
            //
            // E19-02 moved the marker from a neutral container onto the accent role, so this pair
            // is now onPrimaryContainer against primaryContainer. Held to the text threshold rather
            // than the 3:1 graphic one: these are paired on/container roles, and a pairing that
            // cannot carry text is a sign the two tones are too close whatever is drawn on them.
            assertContrastAtLeast(
                scheme.onPrimaryContainer,
                scheme.primaryContainer,
                4.5,
                "$name marker glyph",
            )
            assertContrastAtLeast(scheme.onSurface, scheme.surfaceContainerLow, 4.5, "$name topic name")
            assertContrastAtLeast(
                scheme.onSurfaceVariant,
                scheme.surfaceContainerLow,
                4.5,
                "$name parent topic context",
            )
            // Learning context adds two more things to the same card. Coverage borrows the neutral
            // variant colour checked above; the weak badge is the one semantic element, and it
            // draws its own container rather than inheriting the card's.
            val semantic = semanticByName.getValue(name)
            assertContrastAtLeast(
                semantic.onPartiallyCorrectContainer,
                semantic.partiallyCorrectContainer,
                4.5,
                "$name weak badge",
            )
            assertNotEquals(
                semantic.partiallyCorrectContainer,
                scheme.surfaceContainerLow,
                "$name weak badge vs card",
            )
        }
    }

    /**
     * Guards the failure mode E19-02 fixed rather than the values it chose.
     *
     * Both schemes previously named only the roles a screen read directly, so everything else fell
     * through to `lightColorScheme`/`darkColorScheme` defaults — Material's baseline purple. That is
     * invisible until some component reads one: a Snackbar takes its container from `inverseSurface`
     * and its action label from `inversePrimary`, and would have arrived in a palette this product
     * never chose.
     *
     * The roles listed are the ones that were actually unspecified, checked against a
     * default-constructed scheme. `surfaceTint` is deliberately absent: its default is `primary`,
     * which this app does supply, so agreeing with the default there is correct rather than a gap.
     */
    @Test
    fun noSchemeRoleFallsBackToTheMaterialBaselinePalette() {
        listOf(
            Triple("light", AppLightColorScheme, lightColorScheme()),
            Triple("dark", AppDarkColorScheme, darkColorScheme()),
        ).forEach { (name, scheme, baseline) ->
            previouslyUnspecifiedRoles.forEach { (role, read) ->
                assertNotEquals(
                    read(baseline),
                    read(scheme),
                    "$name $role still resolves to the Material baseline value",
                )
            }
        }
    }

    /**
     * The roles the schemes used to leave to Material's defaults.
     *
     * Written out rather than read reflectively because `kotlin.reflect.full` would mean adding
     * kotlin-reflect purely for one assertion. The cost is that a role introduced by a future
     * Material version has to be added here by hand — so the list names the failure it guards
     * rather than pretending to be exhaustive.
     */
    private val previouslyUnspecifiedRoles: List<Pair<String, (ColorScheme) -> Color>> = listOf(
        "inversePrimary" to { it.inversePrimary },
        "inverseSurface" to { it.inverseSurface },
        "inverseOnSurface" to { it.inverseOnSurface },
        "surfaceBright" to { it.surfaceBright },
        "surfaceDim" to { it.surfaceDim },
        "primaryFixed" to { it.primaryFixed },
        "primaryFixedDim" to { it.primaryFixedDim },
        "onPrimaryFixed" to { it.onPrimaryFixed },
        "onPrimaryFixedVariant" to { it.onPrimaryFixedVariant },
        "secondaryFixed" to { it.secondaryFixed },
        "secondaryFixedDim" to { it.secondaryFixedDim },
        "onSecondaryFixed" to { it.onSecondaryFixed },
        "onSecondaryFixedVariant" to { it.onSecondaryFixedVariant },
        "tertiaryFixed" to { it.tertiaryFixed },
        "tertiaryFixedDim" to { it.tertiaryFixedDim },
        "onTertiaryFixed" to { it.onTertiaryFixed },
        "onTertiaryFixedVariant" to { it.onTertiaryFixedVariant },
    )

    private fun ComposeUiTest.assertDiscoveryStatesRender(darkTheme: Boolean) {
        val state: MutableState<TopicBrowserUiState> = mutableStateOf(BrowsingState)
        setContent {
            AppTheme(darkTheme = darkTheme) {
                Box(Modifier.size(400.dp, 700.dp)) {
                    AppNavigationScaffold(
                        selected = AppTopLevelDestination.TOPICS,
                        onSelect = {},
                        showsNavigation = true,
                    ) { contentPadding ->
                        TopicBrowserScreen(
                            state = state.value,
                            onTopicClick = {},
                            onRetry = {},
                            modifier = Modifier.padding(contentPadding),
                            topWindowInsets = WindowInsets(0),
                        )
                    }
                }
            }
        }

        onNodeWithText(UiTopicName).assertIsDisplayed()
        onNodeWithTag(topicVisualMarkerTag(UiTopicId), useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag(TopicBrowserSearchFieldTag).assertIsDisplayed()

        // Learning context reads in either palette: a neutral coverage count on both cards, the
        // unstudied Topic saying so instead of showing a fabricated 0%, and the weak one carrying
        // both its accuracy and the domain's badge.
        onNodeWithText("0 of 10 explored").assertIsDisplayed()
        onNodeWithText("Not studied yet").assertIsDisplayed()
        onNodeWithText("4 of 10 explored").assertIsDisplayed()
        onNodeWithText("42%").assertIsDisplayed()
        onNodeWithText("Weak area").assertIsDisplayed()

        state.value = SearchState
        waitForIdle()
        onNodeWithText(NetworkingTopicName).assertIsDisplayed()
        onNodeWithText(SubtopicName).assertIsDisplayed()
        // The parent Topic stays readable text rather than being replaced by its marker.
        onNodeWithText(UiTopicName).assertIsDisplayed()
        onNodeWithTag(topicVisualMarkerTag(UiTopicId), useUnmergedTree = true).assertIsDisplayed()

        state.value = NoResultState
        waitForIdle()
        onNodeWithText("No topics or subtopics match \"nothing\"").assertIsDisplayed()

        state.value = TopicBrowserUiState.Empty
        waitForIdle()
        onNodeWithText("No topics available").assertIsDisplayed()

        state.value = TopicBrowserUiState.Error
        waitForIdle()
        onNodeWithText("Topics could not be loaded").assertIsDisplayed()
        onNodeWithText("Retry").assertIsDisplayed()
    }

    private fun assertContrastAtLeast(
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
    private fun contrastRatio(first: Color, second: Color): Double {
        val lighter = maxOf(first.luminance(), second.luminance()).toDouble()
        val darker = minOf(first.luminance(), second.luminance()).toDouble()
        return (lighter + 0.05) / (darker + 0.05)
    }
}

private const val UiTopicId = "android_ui"
private const val UiTopicName = "UI — Views & Jetpack Compose"
private const val NetworkingTopicId = "networking"
private const val NetworkingTopicName = "Networking & Serialization"
private const val SubtopicName = "Compose snapshot state"

/**
 * The three learning states a Topic card has to keep apart in either palette: observed, weak, and
 * unstudied. Coverage stays neutral in all three; only accuracy and the weak badge are semantic.
 */
private val BrowsingState = TopicBrowserUiState.Content(
    topics = listOf(
        TopicBrowserItemUiModel(
            topicId = UiTopicId,
            topicName = UiTopicName,
            learningContext = LearningContextUiModel(
                attemptedQuestionCount = 0,
                totalQuestionCount = 10,
                coveragePercentage = 0.0,
                accuracyPercentage = null,
                isWeak = false,
            ),
        ),
        TopicBrowserItemUiModel(
            topicId = NetworkingTopicId,
            topicName = NetworkingTopicName,
            learningContext = LearningContextUiModel(
                attemptedQuestionCount = 4,
                totalQuestionCount = 10,
                coveragePercentage = 40.0,
                accuracyPercentage = 42.0,
                isWeak = true,
            ),
        ),
    ),
)

private val SearchState = BrowsingState.copy(
    query = "compose",
    topicMatches = listOf(BrowsingState.topics[1]),
    subtopicMatches = listOf(
        SubtopicSearchResult(
            subtopicId = "compose_state",
            subtopicName = SubtopicName,
            parentTopicId = UiTopicId,
            parentTopicName = UiTopicName,
        ),
    ),
)

private val NoResultState = BrowsingState.copy(query = "nothing")
