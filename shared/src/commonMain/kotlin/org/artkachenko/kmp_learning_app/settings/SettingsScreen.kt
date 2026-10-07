package org.artkachenko.kmp_learning_app.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.settings_about_title
import kmp_learning_app.shared.generated.resources.settings_about_version
import kmp_learning_app.shared.generated.resources.settings_appearance_title
import kmp_learning_app.shared.generated.resources.settings_dark_theme_label
import kmp_learning_app.shared.generated.resources.settings_dark_theme_supporting
import kmp_learning_app.shared.generated.resources.settings_kmp_content_label
import kmp_learning_app.shared.generated.resources.settings_kmp_content_supporting
import kmp_learning_app.shared.generated.resources.settings_learning_content_title
import kmp_learning_app.shared.generated.resources.settings_reset_dialog_cancel
import kmp_learning_app.shared.generated.resources.settings_reset_dialog_confirm
import kmp_learning_app.shared.generated.resources.settings_reset_dialog_deletes
import kmp_learning_app.shared.generated.resources.settings_reset_dialog_irreversible
import kmp_learning_app.shared.generated.resources.settings_reset_dialog_keeps
import kmp_learning_app.shared.generated.resources.settings_reset_dialog_resetting
import kmp_learning_app.shared.generated.resources.settings_reset_dialog_title
import kmp_learning_app.shared.generated.resources.settings_reset_failed
import kmp_learning_app.shared.generated.resources.settings_reset_progress_label
import kmp_learning_app.shared.generated.resources.settings_reset_progress_supporting
import kmp_learning_app.shared.generated.resources.settings_send_feedback_label
import kmp_learning_app.shared.generated.resources.settings_send_feedback_open_failed
import kmp_learning_app.shared.generated.resources.settings_send_feedback_supporting
import kmp_learning_app.shared.generated.resources.settings_title
import kmp_learning_app.shared.generated.resources.settings_your_data_title
import org.artkachenko.kmp_learning_app.product.ProductMetadata
import org.artkachenko.kmp_learning_app.product.ProductRepositoryUrl
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.SectionHeading
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.theme.AppContentWidth
import org.artkachenko.kmp_learning_app.ui.theme.AppIconSize
import org.artkachenko.kmp_learning_app.ui.theme.AppScreenPane
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppStroke
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppContentMargin
import org.artkachenko.kmp_learning_app.ui.theme.appListContentPadding
import org.jetbrains.compose.resources.stringResource

internal const val SettingsDarkThemeSwitchTag = "settings_dark_theme_switch"
internal const val SettingsKmpContentSwitchTag = "settings_kmp_content_switch"
internal const val SettingsAboutTag = "settings_about"
internal const val SettingsResetProgressTag = "settings_reset_progress"
internal const val SettingsResetDialogTag = "settings_reset_dialog"
internal const val SettingsResetConfirmTag = "settings_reset_confirm"
internal const val SettingsResetProgressIndicatorTag = "settings_reset_progress_indicator"
internal const val SettingsResetFailedTag = "settings_reset_failed"
internal const val SettingsSendFeedbackTag = "settings_send_feedback"
internal const val SettingsSendFeedbackFailedTag = "settings_send_feedback_failed"

/**
 * GitHub's issue chooser rather than one form: it offers the question-report form and a blank
 * issue, so feedback that is not about one Question still has somewhere to go.
 */
private const val SendFeedbackUrl = "$ProductRepositoryUrl/issues/new/choose"

/**
 * Application settings: how the app looks, which curriculum it teaches, the learner's data, and what
 * this build of it is.
 *
 * Deliberately four sections and nothing else. There is no account, no language, no notifications,
 * and no preference framework: the product has two settings, each a switch written out here, and two
 * actions — resetting progress and sending feedback — each a row written out here. A screen that
 * pretended to more would be scaffolding rather than a feature. Neither preference belongs to this
 * screen — both are app-scoped holders that outlive it — so it only shows and forwards them; the
 * reset's dialog state is [progressReset], owned by `ProgressResetViewModel`.
 *
 * [isDarkTheme] is the *effective* theme, not the stored preference, which is what lets one switch
 * stand in for a three-state preference: a learner who has never opened this screen sees the switch
 * already reflecting their operating system, and moving it records the explicit choice.
 * [includeKmpContent] is the stored choice itself, which defaults to off.
 *
 * "Your data" holds only the reset, because it is the one row that destroys anything. "Send
 * feedback" sits under About instead: it is about the product and the people who maintain it, and
 * putting a harmless outward link beside a destructive action would make the destructive one easier
 * to hit by habit.
 */
@Composable
internal fun SettingsScreen(
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    includeKmpContent: Boolean,
    onIncludeKmpContentChange: (Boolean) -> Unit,
    progressReset: ProgressResetUiState,
    onResetProgress: () -> Unit,
    onConfirmReset: () -> Unit,
    onDismissReset: () -> Unit,
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
                    text = stringResource(Res.string.settings_your_data_title),
                    modifier = Modifier.padding(horizontal = margin),
                )
                SettingsActionRow(
                    label = stringResource(Res.string.settings_reset_progress_label),
                    supportingText = stringResource(Res.string.settings_reset_progress_supporting),
                    onClick = onResetProgress,
                    margin = margin,
                    testTag = SettingsResetProgressTag,
                )
                SectionHeading(
                    text = stringResource(Res.string.settings_about_title),
                    modifier = Modifier.padding(horizontal = margin),
                )
                AboutProduct(modifier = Modifier.padding(horizontal = margin))
                SendFeedbackRow(margin = margin)
            }
        }
    }
    ProgressResetDialog(
        state = progressReset,
        onConfirm = onConfirmReset,
        onDismiss = onDismissReset,
    )
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
 * One action: a two-line row that is one button, shaped like [SettingsSwitchRow] so the screen reads
 * as one list. The whole row is the target, with the margin inside the `clickable` so the state
 * layer spans the pane.
 */
@Composable
private fun SettingsActionRow(
    label: String,
    supportingText: String,
    onClick: () -> Unit,
    margin: Dp,
    testTag: String,
    trailingIcon: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(testTag)
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
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(AppSpacing.Comfortable))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                modifier = Modifier.size(AppIconSize.Row),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Opens the repository's issue chooser in the browser, with the handling every external link in the
 * app uses: `openUri` throws when no host handler can open the address, and the failure is said
 * inline under the row rather than swallowed. Nothing leaves the device until the learner submits a
 * form on GitHub themselves.
 */
@Composable
private fun SendFeedbackRow(margin: Dp) {
    val uriHandler = LocalUriHandler.current
    var openFailed by remember { mutableStateOf(false) }
    Column {
        SettingsActionRow(
            label = stringResource(Res.string.settings_send_feedback_label),
            supportingText = stringResource(Res.string.settings_send_feedback_supporting),
            onClick = { openFailed = runCatching { uriHandler.openUri(SendFeedbackUrl) }.isFailure },
            margin = margin,
            testTag = SettingsSendFeedbackTag,
            trailingIcon = AppIcons.OpenInNew,
        )
        if (openFailed) {
            Text(
                text = stringResource(Res.string.settings_send_feedback_open_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(horizontal = margin)
                    .testTag(SettingsSendFeedbackFailedTag),
            )
        }
    }
}

/**
 * The confirmation in front of the one destructive action in the app.
 *
 * It says exactly what goes and what stays, because "reset" alone does not: saved Questions and
 * settings survive, which a learner would otherwise have to find out afterwards. The confirm button
 * takes the scheme's `error` / `onError` — this is the case that role exists for — and keeps those
 * colours while it is disabled for working, since dimming it to Material's 38% would fade the spinner
 * at the moment it is the only thing saying the reset is running. While it runs the dialog cannot be
 * dismissed, so the learner cannot leave a reset whose outcome they have not seen. A failure keeps
 * the dialog open with the reason, and Reset is the retry.
 */
@Composable
private fun ProgressResetDialog(
    state: ProgressResetUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (state == ProgressResetUiState.Idle) return
    val resetting = state == ProgressResetUiState.Resetting
    val failed = (state as? ProgressResetUiState.Confirming)?.failed == true
    // The shell's SelectionContainer reaches into a dialog through composition locals, but the
    // dialog is a separate layout hierarchy, so a mouse press on its text started a selection the
    // registrar could not place and threw on desktop. A confirmation is not content to copy.
    DisableSelection {
        ResetAlertDialog(resetting = resetting, failed = failed, onConfirm = onConfirm, onDismiss = onDismiss)
    }
}

@Composable
private fun ResetAlertDialog(
    resetting: Boolean,
    failed: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(SettingsResetDialogTag),
        properties = DialogProperties(
            dismissOnBackPress = !resetting,
            dismissOnClickOutside = !resetting,
        ),
        title = { Text(stringResource(Res.string.settings_reset_dialog_title)) },
        text = {
            Column(
                // Scrolls so the whole statement stays reachable at a large type size.
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
            ) {
                Text(stringResource(Res.string.settings_reset_dialog_deletes))
                Text(stringResource(Res.string.settings_reset_dialog_keeps))
                Text(stringResource(Res.string.settings_reset_dialog_irreversible))
                if (failed) {
                    Text(
                        text = stringResource(Res.string.settings_reset_failed),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(SettingsResetFailedTag),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !resetting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    disabledContainerColor = MaterialTheme.colorScheme.error,
                    disabledContentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.testTag(SettingsResetConfirmTag),
            ) {
                if (resetting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(AppIconSize.Action)
                            .testTag(SettingsResetProgressIndicatorTag),
                        color = LocalContentColor.current,
                        strokeWidth = AppStroke.Indicator,
                    )
                    Text(
                        text = stringResource(Res.string.settings_reset_dialog_resetting),
                        modifier = Modifier.padding(start = AppSpacing.Related),
                    )
                } else {
                    Text(stringResource(Res.string.settings_reset_dialog_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !resetting) {
                Text(stringResource(Res.string.settings_reset_dialog_cancel))
            }
        },
    )
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
            progressReset = ProgressResetUiState.Idle,
            onResetProgress = {},
            onConfirmReset = {},
            onDismissReset = {},
            onBack = {},
        )
    }
}
