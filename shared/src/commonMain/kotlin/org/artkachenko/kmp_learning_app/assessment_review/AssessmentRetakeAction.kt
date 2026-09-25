package org.artkachenko.kmp_learning_app.assessment_review

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeState
import org.artkachenko.kmp_learning_app.ui.theme.AppMotion
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing

/**
 * The words one product puts on taking its assessment again.
 *
 * `AssessmentRetakeController` already says that the two result screens differ in exactly two
 * things: whether there is a loaded result to repeat, and "the wording the screen puts on each of
 * these states". The first belongs to each ViewModel. This is the second, and holding it as five
 * strings is what let the control itself stop being written twice — a practice run is repeated and
 * an interview is retaken, but the state machine behind both is one.
 */
@Immutable
internal data class AssessmentRetakeWording(
    val action: String,
    val starting: String,
    val sourceMissing: String,
    val noQuestions: String,
    val error: String,
)

/**
 * Taking a completed assessment again: one control, in all six states the retake can be in.
 *
 * Both result screens had a copy of this — the same `when` over
 * [AssessmentRetakeState] producing the same three red notices, above the same button with the
 * same two-branch body. What they produced was also the last abrupt state change left in the
 * assessment flow, and the roughest:
 *
 * - The button **replaced its label with a bare spinner**, and Material's default
 *   `CircularProgressIndicator` is 40dp, so pressing it grew a text button into something half as
 *   tall again as it started. The one moment the control is busy was the one moment it changed
 *   size.
 * - The word for that state — "Starting practice" — was a **separate line above the button**, so
 *   the button said nothing about why it had stopped working, and a second line of layout appeared
 *   and disappeared beside it.
 * - The three failure notices appeared and vanished in a single frame, moving the button each time.
 *
 * All three are the same fix: the control states its own condition. The label crosses over to
 * "Starting practice" beside a spinner sized like the leading icon it effectively is, the box
 * eases between the two widths, and the notice above expands and fades rather than appearing. It
 * is the same treatment the Submit button takes when it becomes Next, which is the same kind of
 * moment — one control, one place, several things it can be saying.
 *
 * [progressTestTag] is required rather than optional so both products expose the busy state to a
 * test the same way. Only the Mixed result named it before, which is why only the Mixed result had
 * a test asserting the spinner is shown.
 */
@Composable
internal fun AssessmentRetakeAction(
    state: AssessmentRetakeState,
    wording: AssessmentRetakeWording,
    onRetake: () -> Unit,
    actionTestTag: String,
    progressTestTag: String,
    modifier: Modifier = Modifier,
) {
    // `Created` is busy too, and deliberately: the attempt exists and navigation to it has not
    // happened yet, so re-entry stays closed. See AssessmentRetakeController.start.
    val isBusy = state == AssessmentRetakeState.Creating || state is AssessmentRetakeState.Created
    val failure = when (state) {
        AssessmentRetakeState.SourceAttemptNotFound -> wording.sourceMissing
        AssessmentRetakeState.NoEligibleQuestions -> wording.noQuestions
        AssessmentRetakeState.Error -> wording.error
        AssessmentRetakeState.Creating,
        AssessmentRetakeState.Idle,
        is AssessmentRetakeState.Created,
        -> null
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.Related)) {
        // One `AnimatedContent` over the nullable message covers both things that can happen here:
        // a notice arriving where there was none, and one failure being replaced by another. Its
        // own size transform is what keeps the button below from being shoved.
        AnimatedContent(
            targetState = failure,
            transitionSpec = {
                fadeIn(AppMotion.revealSpec()) togetherWith
                    fadeOut(AppMotion.effectSpec(AppMotion.StateChangeDurationMillis / 2))
            },
            label = "retakeFailure",
        ) { message ->
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        OutlinedButton(
            onClick = onRetake,
            enabled = !isBusy,
            // Disabled here means *working*, not unavailable, and those want opposite treatments.
            // Material fades a disabled button's content to 38% of `onSurface`, which is the right
            // answer for an action the learner cannot take — and the wrong one for a control whose
            // whole job at that moment is to report that something is happening: it would dim the
            // spinner and the word explaining it at the exact moment they are the only things on
            // screen saying so. This is the same argument the practice screen makes for drawing a
            // revealed answer's radio button as live rather than as disabled.
            colors = ButtonDefaults.outlinedButtonColors(
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier.testTag(actionTestTag),
        ) {
            AnimatedContent(
                targetState = isBusy,
                transitionSpec = {
                    val enter = fadeIn(AppMotion.effectSpec())
                    val exit = fadeOut(AppMotion.effectSpec(AppMotion.StateChangeDurationMillis / 2))
                    // Unclipped, so neither label is cut while the box travels between the width of
                    // "Practice again" and the width of a spinner plus "Starting practice".
                    enter togetherWith exit using SizeTransform(clip = false)
                },
                label = "retakeAction",
            ) { busy ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(ProgressIndicatorSize)
                                .testTag(progressTestTag),
                            strokeWidth = ProgressStrokeWidth,
                        )
                        Text(
                            text = wording.starting,
                            modifier = Modifier.padding(start = AppSpacing.Related),
                        )
                    } else {
                        Text(wording.action)
                    }
                }
            }
        }
    }
}

/**
 * Sized as the leading icon it effectively is, rather than as Material's standalone 40dp indicator.
 *
 * A progress indicator inside a button is a glyph beside a word, and the other two buttons in this
 * file — the save bookmark and the disclosure chevron — already take 18dp for exactly that reason.
 * The stroke is scaled to match: the 4dp default on an 18dp circle is a ring, not a spinner.
 */
private val ProgressIndicatorSize = 18.dp
private val ProgressStrokeWidth = 2.dp
