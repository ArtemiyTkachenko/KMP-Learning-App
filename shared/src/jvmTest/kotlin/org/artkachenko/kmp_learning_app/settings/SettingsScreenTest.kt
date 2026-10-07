package org.artkachenko.kmp_learning_app.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.product.ProductMetadata
import org.artkachenko.kmp_learning_app.product.ProductRepositoryUrl
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme

@OptIn(ExperimentalTestApi::class)
internal class SettingsScreenTest {

    @Test
    fun theAppearanceSectionOffersOneLabelledThemeSwitch() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TestSettingsScreen(isDarkTheme = false, onDarkThemeChange = {}, onBack = {})
            }
        }

        onNodeWithText("Appearance").assertIsDisplayed()
        onNodeWithText("Dark theme").assertIsDisplayed()
        onNodeWithTag(SettingsDarkThemeSwitchTag)
            .assertIsToggleable()
            .assertHeightIsAtLeast(MinimumTouchTarget)
    }

    /** The switch shows the effective theme, so a dark app arrives with it already on. */
    @Test
    fun theSwitchReflectsTheEffectiveTheme() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TestSettingsScreen(isDarkTheme = true, onDarkThemeChange = {}, onBack = {})
            }
        }

        onNodeWithTag(SettingsDarkThemeSwitchTag).assertIsOn()
    }

    @Test
    fun theSwitchIsOffWhenTheAppIsLight() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TestSettingsScreen(isDarkTheme = false, onDarkThemeChange = {}, onBack = {})
            }
        }

        onNodeWithTag(SettingsDarkThemeSwitchTag).assertIsOff()
    }

    /**
     * The whole row is the toggle, which is what makes the target the two-line row rather than the
     * thumb, and what gives assistive technology one switch instead of a row beside a switch.
     */
    @Test
    fun movingTheSwitchReportsTheRequestedValue() = runComposeUiTest {
        val requested = mutableListOf<Boolean>()
        setContent {
            MaterialTheme {
                TestSettingsScreen(
                    isDarkTheme = false,
                    onDarkThemeChange = { requested += it },
                    onBack = {},
                )
            }
        }

        onNodeWithTag(SettingsDarkThemeSwitchTag).performClick()

        assertEquals(listOf(true), requested)
    }

    @Test
    fun movingTheSwitchBackReportsLight() = runComposeUiTest {
        val requested = mutableListOf<Boolean>()
        setContent {
            MaterialTheme {
                TestSettingsScreen(
                    isDarkTheme = true,
                    onDarkThemeChange = { requested += it },
                    onBack = {},
                )
            }
        }

        onNodeWithTag(SettingsDarkThemeSwitchTag).performClick()

        assertEquals(listOf(false), requested)
    }

    @Test
    fun theRowPublishesSwitchSemanticsRatherThanAPlainClick() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TestSettingsScreen(isDarkTheme = true, onDarkThemeChange = {}, onBack = {})
            }
        }

        val semantics = onNodeWithTag(SettingsDarkThemeSwitchTag).fetchSemanticsNode()
        val toggleable = semantics.config.first { it.key.name == "ToggleableState" }.value
        assertEquals(ToggleableState.On, toggleable)
    }

    /**
     * The About section names the product and the version, both from the canonical metadata. The
     * expected strings are built from [ProductMetadata] rather than written out, so this asserts
     * that the screen shows the canonical values — not that someone typed 0.1.0 twice.
     */
    @Test
    fun theAboutSectionNamesTheProductAndItsCanonicalVersion() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TestSettingsScreen(isDarkTheme = false, onDarkThemeChange = {}, onBack = {})
            }
        }

        onNodeWithTag(SettingsAboutTag).assertIsDisplayed()
        onNodeWithText("About").assertIsDisplayed()
        onNodeWithText(ProductMetadata.NAME).assertIsDisplayed()
        onNodeWithText("Version ${ProductMetadata.VERSION} (${ProductMetadata.BUILD_NUMBER})")
            .assertIsDisplayed()
    }

    @Test
    fun theScreenIsTitledAndCanBeLeft() = runComposeUiTest {
        var backs = 0
        setContent {
            MaterialTheme {
                TestSettingsScreen(isDarkTheme = false, onDarkThemeChange = {}, onBack = { backs += 1 })
            }
        }

        onNodeWithText("Settings").assertIsDisplayed()
        onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs)
    }

    @Test
    fun theLearningContentSectionOffersOneLabelledKmpSwitch() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TestSettingsScreen()
            }
        }

        onNodeWithText("Learning content").assertIsDisplayed()
        onNodeWithText("Include Kotlin Multiplatform content").assertIsDisplayed()
        onNodeWithText("Show Kotlin Multiplatform topics, lessons, and practice questions.")
            .assertIsDisplayed()
        onNodeWithTag(SettingsKmpContentSwitchTag)
            .assertIsToggleable()
            .assertHeightIsAtLeast(MinimumTouchTarget)
    }

    /** No effective state here: the switch shows the stored choice exactly. */
    @Test
    fun theKmpSwitchShowsTheChoiceItIsGiven() = runComposeUiTest {
        var include by mutableStateOf(false)
        setContent {
            MaterialTheme {
                TestSettingsScreen(includeKmpContent = include)
            }
        }

        onNodeWithTag(SettingsKmpContentSwitchTag).assertIsOff()
        include = true
        onNodeWithTag(SettingsKmpContentSwitchTag).assertIsOn()
    }

    @Test
    fun movingTheKmpSwitchReportsTheRequestedValueInBothDirections() = runComposeUiTest {
        val requested = mutableListOf<Boolean>()
        var include by mutableStateOf(false)
        setContent {
            MaterialTheme {
                TestSettingsScreen(
                    includeKmpContent = include,
                    onIncludeKmpContentChange = { requested += it },
                )
            }
        }

        onNodeWithTag(SettingsKmpContentSwitchTag).performClick()
        include = true
        waitForIdle()
        onNodeWithTag(SettingsKmpContentSwitchTag).performClick()

        assertEquals(listOf(true, false), requested)
    }

    /**
     * One toggleable node carrying the label and the state, rather than a clickable row beside an
     * independently toggleable switch.
     */
    @Test
    fun theKmpRowPublishesOneSwitchNode() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TestSettingsScreen(includeKmpContent = true)
            }
        }

        onNodeWithTag(SettingsKmpContentSwitchTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
        // Exactly two toggleable nodes on the whole screen: one per setting, no inner switches.
        assertEquals(2, onAllNodes(isToggleable()).fetchSemanticsNodes().size)
    }

    /** Each switch reports only to its own callback. */
    @Test
    fun theTwoSwitchesAreIndependent() = runComposeUiTest {
        val theme = mutableListOf<Boolean>()
        val kmp = mutableListOf<Boolean>()
        setContent {
            MaterialTheme {
                TestSettingsScreen(
                    onDarkThemeChange = { theme += it },
                    onIncludeKmpContentChange = { kmp += it },
                )
            }
        }

        onNodeWithTag(SettingsDarkThemeSwitchTag).performClick()
        assertEquals(listOf(true), theme)
        assertTrue(kmp.isEmpty())

        onNodeWithTag(SettingsKmpContentSwitchTag).performClick()
        assertEquals(listOf(true), theme)
        assertEquals(listOf(true), kmp)
    }

    /**
     * The scope is four sections: appearance, the curriculum the app teaches, the learner's data,
     * and what this build is. This is the guard against Settings quietly becoming a preference
     * framework: nothing here is a placeholder for an account, a language or a notifications screen,
     * and a later change that adds one has to change this test deliberately.
     */
    @Test
    fun theScreenCarriesTheFourSectionsInOrderAndNoSpeculativeOnes() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TestSettingsScreen()
            }
        }

        val headings = listOf("Appearance", "Learning content", "Your data", "About").map { title ->
            onNodeWithText(title).performScrollTo().fetchSemanticsNode().boundsInRoot.top
        }
        assertEquals(headings.sorted(), headings)
        listOf("Account", "Profile", "Language", "Notifications", "Licences", "Feedback")
            .forEach { onNodeWithText(it).assertDoesNotExist() }
        // The KMP row belongs to Learning content, not to Appearance.
        val kmpTop = onNodeWithTag(SettingsKmpContentSwitchTag).fetchSemanticsNode().boundsInRoot.top
        assertTrue(kmpTop > headings[1] && kmpTop < headings[2])
        // Reset is the only row under Your data; Send feedback is under About.
        val resetTop = onNodeWithTag(SettingsResetProgressTag).fetchSemanticsNode().boundsInRoot.top
        assertTrue(resetTop > headings[2] && resetTop < headings[3])
        val feedbackTop = onNodeWithTag(SettingsSendFeedbackTag).fetchSemanticsNode().boundsInRoot.top
        assertTrue(feedbackTop > headings[3])
        assertEquals(1, onAllNodesWithText("Dark theme").fetchSemanticsNodes().size)
    }

    @Test
    fun theResetRowIsOneButtonThatAsksForConfirmation() = runComposeUiTest {
        var requests = 0
        setContent {
            MaterialTheme {
                TestSettingsScreen(onResetProgress = { requests += 1 })
            }
        }

        onNodeWithTag(SettingsResetProgressTag)
            .performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(MinimumTouchTarget)
            .performClick()

        assertEquals(1, requests)
        // The screen does not open the dialog by itself: that is the state it is given.
        onNodeWithTag(SettingsResetDialogTag).assertDoesNotExist()
    }

    /** The destructive action takes the theme's error role, and keeps it while it reports working. */
    @Test
    fun theConfirmButtonIsDrawnInTheThemeErrorColourIdleAndBusy() = runComposeUiTest {
        var state by mutableStateOf<ProgressResetUiState>(ProgressResetUiState.Confirming())
        var error = Color.Unspecified
        setContent {
            AppTheme(darkTheme = false) {
                error = MaterialTheme.colorScheme.error
                TestSettingsScreen(progressReset = state)
            }
        }

        assertEquals(error, confirmContainerColour())
        state = ProgressResetUiState.Resetting
        waitForIdle()
        assertEquals(error, confirmContainerColour())
    }

    @Test
    fun sendFeedbackOpensTheIssueChooserOfTheOneRepository() = runComposeUiTest {
        val opened = mutableListOf<String>()
        setContent {
            CompositionLocalProvider(LocalUriHandler provides RecordingUriHandler(opened)) {
                MaterialTheme {
                    TestSettingsScreen()
                }
            }
        }

        onNodeWithTag(SettingsSendFeedbackTag)
            .performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()

        assertEquals(listOf("$ProductRepositoryUrl/issues/new/choose"), opened)
        assertEquals(
            "https://github.com/ArtemiyTkachenko/KMP-Learning-App/issues/new/choose",
            opened.single(),
        )
        onNodeWithTag(SettingsSendFeedbackFailedTag).assertDoesNotExist()
    }

    /** No host handler for the address: the failure is said under the row rather than swallowed. */
    @Test
    fun aFeedbackLinkThatCannotOpenSaysSoInline() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalUriHandler provides RecordingUriHandler(failure = true)) {
                MaterialTheme {
                    TestSettingsScreen()
                }
            }
        }

        onNodeWithTag(SettingsSendFeedbackTag).performScrollTo().performClick()

        onNodeWithTag(SettingsSendFeedbackFailedTag).performScrollTo().assertIsDisplayed()
        onNodeWithText("The feedback page could not be opened.").assertIsDisplayed()
    }

    /** A pixel of the pill's top edge at its centre: container, never label. */
    private fun androidx.compose.ui.test.ComposeUiTest.confirmContainerColour(): Color {
        val image = onNodeWithTag(SettingsResetConfirmTag).captureToImage()
        return image.toPixelMap()[image.width / 2, 2]
    }
}

private class RecordingUriHandler(
    private val opened: MutableList<String> = mutableListOf(),
    private val failure: Boolean = false,
) : UriHandler {
    override fun openUri(uri: String) {
        if (failure) throw IllegalStateException("No handler for $uri")
        opened += uri
    }
}

/** The screen with every value defaulted, so each test states only what it is about. */
@Composable
private fun TestSettingsScreen(
    isDarkTheme: Boolean = false,
    onDarkThemeChange: (Boolean) -> Unit = {},
    includeKmpContent: Boolean = false,
    onIncludeKmpContentChange: (Boolean) -> Unit = {},
    onBack: () -> Unit = {},
    progressReset: ProgressResetUiState = ProgressResetUiState.Idle,
    onResetProgress: () -> Unit = {},
) {
    SettingsScreen(
        isDarkTheme = isDarkTheme,
        onDarkThemeChange = onDarkThemeChange,
        includeKmpContent = includeKmpContent,
        onIncludeKmpContentChange = onIncludeKmpContentChange,
        progressReset = progressReset,
        onResetProgress = onResetProgress,
        onConfirmReset = {},
        onDismissReset = {},
        onBack = onBack,
    )
}

/** The Material minimum touch target. */
private val MinimumTouchTarget = 48.dp
