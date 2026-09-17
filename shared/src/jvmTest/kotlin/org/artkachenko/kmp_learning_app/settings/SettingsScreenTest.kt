package org.artkachenko.kmp_learning_app.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.product.ProductMetadata

@OptIn(ExperimentalTestApi::class)
internal class SettingsScreenTest {

    @Test
    fun theAppearanceSectionOffersOneLabelledThemeSwitch() = runComposeUiTest {
        setContent {
            MaterialTheme {
                SettingsScreen(isDarkTheme = false, onDarkThemeChange = {}, onBack = {})
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
                SettingsScreen(isDarkTheme = true, onDarkThemeChange = {}, onBack = {})
            }
        }

        onNodeWithTag(SettingsDarkThemeSwitchTag).assertIsOn()
    }

    @Test
    fun theSwitchIsOffWhenTheAppIsLight() = runComposeUiTest {
        setContent {
            MaterialTheme {
                SettingsScreen(isDarkTheme = false, onDarkThemeChange = {}, onBack = {})
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
                SettingsScreen(
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
                SettingsScreen(
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
                SettingsScreen(isDarkTheme = true, onDarkThemeChange = {}, onBack = {})
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
                SettingsScreen(isDarkTheme = false, onDarkThemeChange = {}, onBack = {})
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
                SettingsScreen(isDarkTheme = false, onDarkThemeChange = {}, onBack = { backs += 1 })
            }
        }

        onNodeWithText("Settings").assertIsDisplayed()
        onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs)
    }

    /**
     * The scope is two sections. This is the guard against Settings quietly becoming a preference
     * framework: nothing here is a placeholder for an account, a language or a notifications
     * screen, and a later change that adds one has to change this test deliberately.
     */
    @Test
    fun theScreenCarriesTheTwoSectionsAndNoSpeculativeOnes() = runComposeUiTest {
        setContent {
            MaterialTheme {
                SettingsScreen(isDarkTheme = false, onDarkThemeChange = {}, onBack = {})
            }
        }

        onNodeWithText("Appearance").assertIsDisplayed()
        onNodeWithText("About").assertIsDisplayed()
        listOf("Account", "Profile", "Language", "Notifications", "Licences", "Feedback")
            .forEach { onNodeWithText(it).assertDoesNotExist() }
    }
}

/** The Material minimum touch target. */
private val MinimumTouchTarget = 48.dp
