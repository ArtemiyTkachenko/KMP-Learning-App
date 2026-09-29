package org.artkachenko.kmp_learning_app.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.tooling.preview.Preview
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.settings_about_title
import kmp_learning_app.shared.generated.resources.settings_about_version
import kmp_learning_app.shared.generated.resources.settings_appearance_title
import kmp_learning_app.shared.generated.resources.settings_dark_theme_label
import kmp_learning_app.shared.generated.resources.settings_dark_theme_supporting
import kmp_learning_app.shared.generated.resources.settings_kmp_content_label
import kmp_learning_app.shared.generated.resources.settings_kmp_content_supporting
import kmp_learning_app.shared.generated.resources.settings_learning_content_title
import kmp_learning_app.shared.generated.resources.settings_title
import org.artkachenko.kmp_learning_app.product.ProductMetadata
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.SectionHeading
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.theme.AppContentWidth
import org.artkachenko.kmp_learning_app.ui.theme.AppScreenPane
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppContentMargin
import org.artkachenko.kmp_learning_app.ui.theme.appListContentPadding
import org.jetbrains.compose.resources.stringResource

internal const val SettingsDarkThemeSwitchTag = "settings_dark_theme_switch"
internal const val SettingsKmpContentSwitchTag = "settings_kmp_content_switch"
internal const val SettingsAboutTag = "settings_about"

/**
 * Application settings: how the app looks, which curriculum it teaches, and what this build of it is.
 *
 * Deliberately three sections and nothing else. There is no account, no language, no notifications,
 * and no preference framework: the product has two settings, each a switch written out here, and a
 * screen that pretended otherwise would be scaffolding rather than a feature. Neither preference
 * belongs to this screen — both are app-scoped holders that outlive it — so it only shows and
 * forwards them.
 *
 * [isDarkTheme] is the *effective* theme, not the stored preference, which is what lets one switch
 * stand in for a three-state preference: a learner who has never opened this screen sees the switch
 * already reflecting their operating system, and moving it records the explicit choice.
 * [includeKmpContent] is the stored choice itself, which defaults to off.
 */
@Composable
internal fun SettingsScreen(
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    includeKmpContent: Boolean,
    onIncludeKmpContentChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = rememberAppTopBarScrollBehavior()
    val margin = LocalAppContentMargin.current

    Column(modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        AppTopBar(stringResource(Res.string.settings_title), onBack, scrollBehavior)
        AppScreenPane(AppContentWidth.Standard) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    // Vertical only: each switch row below is its own interactive surface and
                    // carries the horizontal margin itself, so its state layer spans the pane.
                    .padding(appListContentPadding()),
            ) {
                SectionHeading(
                    text = stringResource(Res.string.settings_appearance_title),
                    modifier = Modifier.padding(horizontal = margin),
                    // The first heading on the screen, directly under the bar, so it takes the
                    // ordinary top gap rather than a section break from nothing.
                    topPadding = AppSpacing.Related,
                )
                SettingsSwitchRow(
                    label = stringResource(Res.string.settings_dark_theme_label),
                    supportingText = stringResource(Res.string.settings_dark_theme_supporting),
                    checked = isDarkTheme,
                    onCheckedChange = onDarkThemeChange,
                    margin = margin,
                    testTag = SettingsDarkThemeSwitchTag,
                )
                SectionHeading(
                    text = stringResource(Res.string.settings_learning_content_title),
                    modifier = Modifier.padding(horizontal = margin),
                )
                SettingsSwitchRow(
                    label = stringResource(Res.string.settings_kmp_content_label),
                    supportingText = stringResource(Res.string.settings_kmp_content_supporting),
                    checked = includeKmpContent,
                    onCheckedChange = onIncludeKmpContentChange,
                    margin = margin,
                    testTag = SettingsKmpContentSwitchTag,
                )
                SectionHeading(
                    text = stringResource(Res.string.settings_about_title),
                    modifier = Modifier.padding(horizontal = margin),
                )
                AboutProduct(modifier = Modifier.padding(horizontal = margin))
            }
        }
    }
}

/**
 * One setting: a two-line row that is one toggle, not a row containing a toggle.
 *
 * The `Switch` takes `onCheckedChange = null`, which makes the row the single interactive and
 * accessible node. That is what gives assistive technology one switch to announce instead of a
 * clickable row beside a separate switch, and what makes the whole two-line row the touch target
 * rather than the 52dp thumb.
 *
 * Shared by the two switches on this screen and nothing more: it takes the text and the state
 * directly, with no setting identity or model behind it.
 */
@Composable
private fun SettingsSwitchRow(
    label: String,
    supportingText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    margin: Dp,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch,
            )
            .testTag(testTag)
            // Inside the toggleable, so the state layer spans the pane rather than stopping at
            // the text. See the list rules in docs/development/material-design.md.
            .padding(horizontal = margin, vertical = AppSpacing.Comfortable),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(AppSpacing.Comfortable))
        Switch(checked = checked, onCheckedChange = null)
    }
}

/**
 * What this build of the product is.
 *
 * Two lines of type rather than a card: a card here would be one more surface for content that is
 * already the quietest thing on the screen. The name and version come from [ProductMetadata], which
 * is generated from the repository's canonical `product.properties` — the same definition Android's
 * versionName, the Desktop package version and the iOS marketing version are built from, so this
 * cannot claim a version the host does not have.
 */
@Composable
private fun AboutProduct(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = AppSpacing.Grouped).testTag(SettingsAboutTag),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
    ) {
        Text(
            text = ProductMetadata.NAME,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(
                Res.string.settings_about_version,
                ProductMetadata.VERSION,
                ProductMetadata.BUILD_NUMBER.toString(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    AppTheme {
        SettingsScreen(
            isDarkTheme = true,
            onDarkThemeChange = {},
            includeKmpContent = false,
            onIncludeKmpContentChange = {},
            onBack = {},
        )
    }
}
