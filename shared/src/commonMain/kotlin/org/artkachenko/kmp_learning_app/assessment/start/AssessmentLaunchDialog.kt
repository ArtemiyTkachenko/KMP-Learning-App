package org.artkachenko.kmp_learning_app.assessment.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.assessment_launch_cancel
import kmp_learning_app.shared.generated.resources.assessment_launch_failed
import kmp_learning_app.shared.generated.resources.assessment_launch_loading
import kmp_learning_app.shared.generated.resources.assessment_launch_no_questions
import kmp_learning_app.shared.generated.resources.assessment_launch_unexpected
import kmp_learning_app.shared.generated.resources.assessment_launch_retry
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.jetbrains.compose.resources.stringResource

internal const val AssessmentLaunchLoadingTag = "assessment_launch_loading"
internal const val AssessmentLaunchDialogTag = "assessment_launch_dialog"

/** A modal boundary keeps the originating route stable and blocks a second launch request. */
@Composable
internal fun AssessmentLaunchDialog(
    state: AssessmentLaunchState,
    onRetry: () -> Unit,
    onDismissFailure: () -> Unit,
) {
    when (state) {
        AssessmentLaunchState.Idle -> Unit
        AssessmentLaunchState.Launching -> Dialog(onDismissRequest = {}) {
            Surface(
                modifier = Modifier.testTag(AssessmentLaunchDialogTag),
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.Generous),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.testTag(AssessmentLaunchLoadingTag))
                    Text(stringResource(Res.string.assessment_launch_loading))
                }
            }
        }
        is AssessmentLaunchState.Failed -> AlertDialog(
            onDismissRequest = onDismissFailure,
            title = { Text(stringResource(Res.string.assessment_launch_failed)) },
            text = {
                Text(
                    stringResource(
                        when (state.reason) {
                            AssessmentLaunchFailure.NoEligibleQuestions ->
                                Res.string.assessment_launch_no_questions
                            AssessmentLaunchFailure.Unexpected ->
                                Res.string.assessment_launch_unexpected
                        },
                    ),
                )
            },
            confirmButton = {
                Button(onClick = onRetry) {
                    Text(stringResource(Res.string.assessment_launch_retry))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissFailure) {
                    Text(stringResource(Res.string.assessment_launch_cancel))
                }
            },
        )
    }
}
