package org.artkachenko.kmp_learning_app.assessment_taking

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.assessment_taking_answer_save_error
import kmp_learning_app.shared.generated.resources.assessment_taking_completion_save_error
import kmp_learning_app.shared.generated.resources.assessment_taking_finish
import kmp_learning_app.shared.generated.resources.assessment_taking_loading
import kmp_learning_app.shared.generated.resources.assessment_taking_no_questions
import kmp_learning_app.shared.generated.resources.assessment_taking_next_question
import kmp_learning_app.shared.generated.resources.assessment_taking_question_progress
import kmp_learning_app.shared.generated.resources.assessment_taking_ready
import kmp_learning_app.shared.generated.resources.assessment_taking_results_opening
import kmp_learning_app.shared.generated.resources.assessment_taking_select_all
import kmp_learning_app.shared.generated.resources.assessment_taking_select_one
import kmp_learning_app.shared.generated.resources.assessment_taking_start_error
import kmp_learning_app.shared.generated.resources.assessment_taking_submit
import kmp_learning_app.shared.generated.resources.assessment_taking_submitting
import org.artkachenko.kmp_learning_app.assessment_review.AnswerOutcome
import org.artkachenko.kmp_learning_app.assessment_review.AnswerOutcomeColors
import org.artkachenko.kmp_learning_app.assessment_review.QuestionAnswerTag
import org.artkachenko.kmp_learning_app.assessment_review.QuestionExplanationBlock
import org.artkachenko.kmp_learning_app.assessment_review.QuestionOutcomeBadge
import org.artkachenko.kmp_learning_app.assessment_review.answerOutcome
import org.artkachenko.kmp_learning_app.assessment_review.colors
import org.artkachenko.kmp_learning_app.assessment_review.questionOutcome
import org.artkachenko.kmp_learning_app.assessment_review.tagLabel
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.ScreenStatus
import org.artkachenko.kmp_learning_app.ui.theme.AppMotion
import org.jetbrains.compose.resources.stringResource
import org.artkachenko.kmp_learning_app.ui.theme.AppContentWidth
import org.artkachenko.kmp_learning_app.ui.theme.AppScreenPane
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppContentMargin

internal const val AssessmentTakingLoadingTag = "focused_practice_loading"
internal const val AssessmentTakingSubmitTag = "focused_practice_submit"
internal const val AssessmentTakingFinishTag = "focused_practice_finish"

internal const val AssessmentProgressMeterTag = "assessment_progress_meter"

/** The revealed verdict for the question just answered. */
internal const val AssessmentTakingOutcomeTag = "assessment_taking_outcome"


@Composable
internal fun AssessmentTakingScreen(
    title: String,
    state: AssessmentTakingUiState,
    onAnswerClick: (String) -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit = {},
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = rememberAppTopBarScrollBehavior()
    Column(modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        AppTopBar(
            title = title,
            onBack = onBack,
            scrollBehavior = scrollBehavior,
        )
        // Pinned under the bar rather than placed in the scrolling content: how far through the
        // assessment the learner is should stay answerable while they read a long question.
        if (state is AssessmentTakingUiState.Content) {
            AssessmentProgressMeter(
                questionNumber = state.questionNumber,
                totalQuestions = state.totalQuestions,
            )
        }
        AppScreenPane(AppContentWidth.Standard) {
            when (state) {
                AssessmentTakingUiState.Loading -> ScreenLoading(
                    message = stringResource(Res.string.assessment_taking_loading),
                    testTag = AssessmentTakingLoadingTag,
                    modifier = Modifier.weight(1f),
                )

                AssessmentTakingUiState.NoQuestions -> ScreenMessage(
                    message = stringResource(Res.string.assessment_taking_no_questions),
                    modifier = Modifier.weight(1f),
                )

                AssessmentTakingUiState.Error -> ScreenError(
                    message = stringResource(Res.string.assessment_taking_start_error),
                    onRetry = onRetry,
                    modifier = Modifier.weight(1f),
                )

                is AssessmentTakingUiState.Content -> key(state.question.id) {
                    QuestionContent(
                        state = state,
                        onAnswerClick = onAnswerClick,
                        onSubmit = onSubmit,
                        onNext = onNext,
                        modifier = Modifier.weight(1f),
                    )
                }

                is AssessmentTakingUiState.ReadyToComplete -> ScreenStatus(Modifier.weight(1f)) {
                    Text(text = stringResource(Res.string.assessment_taking_ready))
                    if (state.completionFailed) {
                        Text(
                            text = stringResource(Res.string.assessment_taking_completion_save_error),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    Button(
                        onClick = onComplete,
                        enabled = !state.isCompleting,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .testTag(AssessmentTakingFinishTag),
                    ) {
                        if (state.isCompleting) {
                            CircularProgressIndicator()
                        } else {
                            Text(text = stringResource(Res.string.assessment_taking_finish))
                        }
                    }
                }

                is AssessmentTakingUiState.CompletionSucceeded -> ScreenMessage(
                    message = stringResource(Res.string.assessment_taking_results_opening),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * How much of the assessment is behind the learner. The counter alone gave the number but not the
 * shape of it, so "3 of 20" and "3 of 5" read the same at a glance.
 */
@Composable
private fun AssessmentProgressMeter(questionNumber: Int, totalQuestions: Int) {
    val fraction = if (totalQuestions <= 0) {
        0f
    } else {
        ((questionNumber - 1).coerceIn(0, totalQuestions).toFloat()) / totalQuestions
    }
    val animated by animateFloatAsState(
        targetValue = fraction,
        animationSpec = AppMotion.effectSpec(AppMotion.ProgressDurationMillis),
        label = "assessmentProgress",
    )
    LinearProgressIndicator(
        progress = { animated },
        modifier = Modifier
            .fillMaxWidth()
            // The window's own margin rather than a literal: this meter sits outside the content
            // pane, spanning the window with the question counter it belongs to, so it has to line
            // up with the margin the pane inside it uses.
            .padding(
                horizontal = LocalAppContentMargin.current,
                vertical = AppSpacing.Related,
            )
            .testTag(AssessmentProgressMeterTag),
    )
}

@Composable
private fun QuestionContent(
    state: AssessmentTakingUiState.Content,
    onAnswerClick: (String) -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = appScreenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
    ) {
        item {
            // Three distinct tiers: progress metadata, the question itself, and the supporting
            // instruction. They previously shared bodyLarge/onSurface and read as one block.
            Text(
                text = stringResource(
                    Res.string.assessment_taking_question_progress,
                    state.questionNumber,
                    state.totalQuestions,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = state.question.text,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = AppSpacing.Grouped)
                    .semantics { heading() },
            )
            Text(
                text = stringResource(
                    if (state.question.selectionMode == AnswerSelectionMode.SINGLE) {
                        Res.string.assessment_taking_select_one
                    } else {
                        Res.string.assessment_taking_select_all
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = AppSpacing.Related),
            )
        }
        items(
            items = state.question.answers,
            key = { it.id },
        ) { answer ->
            val selected = answer.id in state.selectedAnswerIds
            AnswerRow(
                answerText = answer.text,
                selected = selected,
                mode = state.question.selectionMode,
                enabled = !state.isSubmitting && state.feedback == null,
                // Null until the answer is submitted. The reveal is the rows themselves: the
                // learner's own options are marked in place, rather than the screen naming the
                // correct answer in a sentence underneath and leaving them to find the row it means.
                outcome = state.feedback?.let {
                    answerOutcome(
                        wasSelected = selected,
                        isCorrectAnswer = answer.id in state.question.correctAnswerIds,
                    )
                },
                onClick = { onAnswerClick(answer.id) },
            )
        }
        item {
            if (state.submissionFailed) {
                Text(
                    text = stringResource(Res.string.assessment_taking_answer_save_error),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            // Entering rather than appearing, and entering in order. The verdict and the
            // explanation are new content arriving under answers the learner is already looking
            // at, so they decelerate into place; `key(question.id)` above resets this, so every
            // question reveals once.
            //
            // One `AnimatedVisibility` owns the expansion and the two children fade in behind it
            // on staggered specs, rather than a container per piece: the page relayouts once while
            // the verdict still reads as arriving before the paragraph explaining it. The stagger
            // lives in the animation specs, so nothing in the interaction is waiting on a
            // coroutine to finish before the learner can press Next.
            AnimatedVisibility(
                visible = state.feedback != null,
                enter = expandVertically(AppMotion.spatialSpec()),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped)) {
                    QuestionOutcomeBadge(
                        outcome = questionOutcome(
                            scoredCorrect = state.feedback?.isCorrect == true,
                            selectedAnswerIds = state.selectedAnswerIds,
                            correctAnswerIds = state.question.correctAnswerIds,
                        ),
                        modifier = Modifier
                            .animateEnterExit(
                                enter = fadeIn(AppMotion.revealSpec(VerdictRevealDelayMillis)) +
                                    scaleIn(
                                        animationSpec = AppMotion.revealSpec(VerdictRevealDelayMillis),
                                        initialScale = VerdictInitialScale,
                                    ),
                            )
                            .testTag(AssessmentTakingOutcomeTag),
                    )
                    QuestionExplanationBlock(
                        explanation = state.question.explanation,
                        modifier = Modifier.animateEnterExit(
                            enter = fadeIn(AppMotion.revealSpec(ExplanationRevealDelayMillis)),
                        ),
                    )
                }
            }
            // One control throughout, because it is one decision point: the question's next action.
            // Replacing the label outright made the most important moment in the flow — the point
            // at which an open question became an answered one — the only part of it that did not
            // move. The button keeps its place, its width, and its primary emphasis; only the word
            // inside it crosses over, and the box around that word eases rather than snapping
            // between the widths of "Submit" and "Next question".
            val actionLabel = when {
                state.feedback != null -> stringResource(Res.string.assessment_taking_next_question)
                state.isSubmitting -> stringResource(Res.string.assessment_taking_submitting)
                else -> stringResource(Res.string.assessment_taking_submit)
            }
            Button(
                onClick = if (state.feedback == null) onSubmit else onNext,
                enabled = (state.canSubmit && !state.isSubmitting) || state.feedback != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AssessmentTakingSubmitTag),
            ) {
                AnimatedContent(
                    targetState = actionLabel,
                    transitionSpec = {
                        // The exit is half the entrance, as everywhere else in the app: the old
                        // label should be gone before the new one is legible, not dissolve into it.
                        val enter = fadeIn(AppMotion.effectSpec())
                        val exit = fadeOut(
                            AppMotion.effectSpec(AppMotion.StateChangeDurationMillis / 2),
                        )
                        // Unclipped, so neither label is cut off while the box between the two
                        // widths is still travelling.
                        enter togetherWith exit using SizeTransform(clip = false)
                    },
                    label = "assessmentAction",
                ) { label -> Text(label) }
            }
        }
    }
}

/**
 * One answer option, in the states it can hold.
 *
 * The row is the touch target and the selection surface: answers used to be bare rows separated
 * only by 6dp, so they were hard to tell apart, and the control was centred against the whole
 * block instead of the first line of a wrapping answer.
 *
 * [outcome] is null until the answer is submitted and non-null afterwards, and it is what makes the
 * reveal happen on the rows rather than beneath them. Once it is set the row stops being a control
 * and becomes a result: the selection modifier is disabled so the choice cannot be changed, and the
 * container, border, control tint, and label all restate the same fact through
 * [org.artkachenko.kmp_learning_app.assessment_review.AnswerOutcome] — the identical vocabulary the
 * results screen uses, so a learner meets it once.
 */
@Composable
private fun AnswerRow(
    answerText: String,
    selected: Boolean,
    mode: AnswerSelectionMode,
    enabled: Boolean,
    outcome: AnswerOutcome?,
    onClick: () -> Unit,
) {
    // Owned here rather than left to the selection modifier's own, because the row reads the press
    // as well as indicating it. Material still draws its ripple from the same source, so there is
    // one press and two responses to it instead of a hand-rolled gesture detector.
    val interactionSource = remember { MutableInteractionSource() }
    val selectionModifier = if (mode == AnswerSelectionMode.SINGLE) {
        Modifier.selectable(
            selected = selected,
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onClick,
        )
    } else {
        Modifier.toggleable(
            value = selected,
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            role = Role.Checkbox,
            onValueChange = { onClick() },
        )
    }

    // An option at rest sits one level above the page rather than on it. It used to take
    // `surface`, which is the page's own colour, so an unselected answer was an outline and
    // nothing else — the one list in the product where every row is a thing to be picked was
    // also the only one whose rows were not objects.
    val restingContainer = MaterialTheme.colorScheme.surfaceContainerLow

    // Choosing an answer is the action this whole product exists for, and it used to be the least
    // responsive thing in it: every property jumped between two values in a single frame, so the
    // row registered the tap without ever acknowledging it.
    //
    // Everything the row says about itself is one discrete fact — [AnswerVisualState] — so it is
    // one `Transition` rather than six independent animations racing each other. The reveal is not
    // a second mechanism layered on top: submitting simply re-targets the same transition, which is
    // what makes a result grow out of the selection instead of replacing it.
    val transition = updateTransition(
        targetState = answerVisualState(selected = selected, outcome = outcome),
        label = "answerState",
    )
    val containerColor by transition.animateColor(
        transitionSpec = { AppMotion.effectSpec() },
        label = "answerContainer",
    ) { it.colors(restingContainer).container }
    val borderColor by transition.animateColor(
        transitionSpec = { AppMotion.effectSpec() },
        label = "answerBorder",
    ) { it.colors(restingContainer).border }
    // A marked row is emphasised on the same terms as a chosen one, except where the mark is the
    // absence of one — an option that was neither picked nor correct has nothing to emphasise.
    // The border width is spatial rather than an effect: it is a size, so it springs like one.
    val borderWidth by transition.animateDp(
        transitionSpec = { AppMotion.spatialSpec() },
        label = "answerBorderWidth",
    ) { if (it == AnswerVisualState.Resting) UnselectedBorderWidth else SelectedBorderWidth }
    // A marked row states its outcome in words and colour of its own, so the answer text returns to
    // the ordinary reading colour once the row is a result rather than a choice.
    val textColor by transition.animateColor(
        transitionSpec = { AppMotion.effectSpec() },
        label = "answerText",
    ) {
        if (it == AnswerVisualState.Selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    }
    // The control's own colours travel with the rest. Material would otherwise repaint the one mark
    // that records *what the learner themselves did* in a single frame, at the exact moment the row
    // around it is easing into a verdict.
    val controlSelectedColor by transition.animateColor(
        transitionSpec = { AppMotion.effectSpec() },
        label = "answerControlSelected",
    ) { it.colors(restingContainer).tagColor ?: MaterialTheme.colorScheme.primary }
    val controlUnselectedColor by transition.animateColor(
        transitionSpec = { AppMotion.effectSpec() },
        label = "answerControlUnselected",
    ) { it.colors(restingContainer).tagColor ?: MaterialTheme.colorScheme.onSurfaceVariant }

    // Pressing an answer should feel like pressing something. The scale is deliberately barely
    // perceptible and lives in a draw layer, so it changes nothing about layout, hit testing, or
    // when [onClick] runs — the callback is the selection modifier's and is never waited on.
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) PressedScale else 1f,
        animationSpec = AppMotion.spatialSpec(),
        label = "answerPress",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .then(selectionModifier),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        border = BorderStroke(width = borderWidth, color = borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AnswerRowMinHeight)
                .padding(AppSpacing.Grouped),
            verticalAlignment = Alignment.Top,
        ) {
            // The control's own 48dp minimum would push it below the first text line, so the
            // enforcement is dropped here and the row above carries the touch target instead.
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Box(
                    modifier = Modifier.height(AnswerLineHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    AnswerControl(
                        mode = mode,
                        selected = selected,
                        // A marked control is drawn as live rather than disabled even though the
                        // row no longer accepts input. Material greys a disabled control to 38%
                        // opacity, which would fade the one mark that says *what the learner
                        // themselves did* at the exact moment they are being told whether it was
                        // right. The row carries the disabled semantics; the control carries the
                        // record.
                        enabled = enabled || outcome != null,
                        selectedColor = controlSelectedColor,
                        unselectedColor = controlUnselectedColor,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(start = AppSpacing.Grouped),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
            ) {
                Text(
                    text = answerText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                )
                val label = outcome?.tagLabel()
                val tagColor = outcome?.colors(restingContainer)?.tagColor
                // The label is the row's non-colour channel, so it arrives with the colour rather
                // than ahead of it: without this the word appeared on the frame of submission while
                // the container behind it was still a fifth of the way through easing. There is no
                // exit — a row only ever gains a mark, and a new question replaces the whole
                // subtree through `key(question.id)`.
                AnimatedVisibility(
                    visible = label != null && tagColor != null,
                    enter = fadeIn(AppMotion.revealSpec(AnswerTagRevealDelayMillis)) +
                        expandVertically(AppMotion.spatialSpec()),
                ) {
                    if (label != null && tagColor != null) {
                        QuestionAnswerTag(text = label, color = tagColor)
                    }
                }
            }
        }
    }
}

/**
 * Everything one option's appearance depends on, as a single value.
 *
 * The row was previously drawn from two independent inputs — a `selected` flag and a nullable
 * outcome — recombined in a `when` at each of six property sites. Naming the four appearances the
 * row actually has is what lets one `Transition` drive all of them, and what stops a later edit
 * from teaching one property a rule the other five do not know.
 *
 * It is derived, not stored: nothing in the ViewModel changes, and this adds no state that could
 * disagree with the selection or the feedback it comes from.
 */
private enum class AnswerVisualState { Resting, Selected, Correct, Incorrect, Missed }

private fun answerVisualState(selected: Boolean, outcome: AnswerOutcome?): AnswerVisualState =
    when (outcome) {
        AnswerOutcome.CORRECT -> AnswerVisualState.Correct
        AnswerOutcome.WRONG -> AnswerVisualState.Incorrect
        AnswerOutcome.MISSED -> AnswerVisualState.Missed
        AnswerOutcome.NEUTRAL -> AnswerVisualState.Resting
        null -> if (selected) AnswerVisualState.Selected else AnswerVisualState.Resting
    }

/**
 * The colours of each appearance.
 *
 * Four of the five defer to the shared review vocabulary rather than restating it, so practice and
 * results cannot drift into two answers to "what does a wrong option look like". Only
 * [AnswerVisualState.Selected] is this screen's own, because it is the one state a review surface
 * has no concept of: an answer chosen but not yet submitted.
 *
 * That state takes the **primary** family. It used to take `secondaryContainer`, and the secondary
 * family is by this palette's own definition the primary hue *drained of chroma* — the same colour
 * the neutral surface ramp is tinted with. A selected option was therefore one more step on that
 * ramp rather than a different kind of thing, and it was carrying a `primary` border on top of a
 * near-neutral fill. `primaryContainer` is the brand colour at container strength, so the fill now
 * agrees with the border and selection reads as chromatic rather than as slightly darker.
 */
@Composable
private fun AnswerVisualState.colors(restingContainer: Color): AnswerOutcomeColors = when (this) {
    AnswerVisualState.Resting -> AnswerOutcome.NEUTRAL.colors(restingContainer)
    AnswerVisualState.Selected -> AnswerOutcomeColors(
        border = MaterialTheme.colorScheme.primary,
        container = MaterialTheme.colorScheme.primaryContainer,
        // No tag: an unsubmitted choice has no verdict to label, and the control tints fall back to
        // Material's own selection colours through this null.
        tagColor = null,
    )
    AnswerVisualState.Correct -> AnswerOutcome.CORRECT.colors(restingContainer)
    AnswerVisualState.Incorrect -> AnswerOutcome.WRONG.colors(restingContainer)
    AnswerVisualState.Missed -> AnswerOutcome.MISSED.colors(restingContainer)
}

/**
 * The radio or checkbox, in whatever colours the row's transition currently holds.
 *
 * The colours are the caller's rather than resolved here, because they are mid-animation values:
 * while the question is open they are Material's own selection colours, and after the reveal they
 * have travelled to the outcome's accent. The mark the learner reads first — the filled control
 * they put there themselves — therefore changes colour with the row around it rather than ahead
 * of it.
 */
@Composable
private fun AnswerControl(
    mode: AnswerSelectionMode,
    selected: Boolean,
    enabled: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
) {
    if (mode == AnswerSelectionMode.SINGLE) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = selectedColor,
                unselectedColor = unselectedColor,
            ),
        )
    } else {
        Checkbox(
            checked = selected,
            onCheckedChange = null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = selectedColor,
                uncheckedColor = unselectedColor,
            ),
        )
    }
}

/**
 * How far the verdict badge and the explanation trail the answer surfaces they belong to.
 *
 * Small enough that the whole reveal is over well inside half a second, and ordered so the learner
 * reads the rows, then the verdict, then the reason — which is the order the information is useful
 * in. Nothing is gated on these: the Next button is live from the first frame of the reveal.
 */
private const val AnswerTagRevealDelayMillis = 60
private const val VerdictRevealDelayMillis = 80
private const val ExplanationRevealDelayMillis = 170

/** Barely a shrink. Enough to read as a surface giving under a finger, not as a button bouncing. */
private const val PressedScale = 0.98f

/** The badge settles in from just under its size; a larger start would read as a pop. */
private const val VerdictInitialScale = 0.94f

private val SelectedBorderWidth = 2.dp
private val UnselectedBorderWidth = 1.dp

private val AnswerRowMinHeight = 48.dp

/** Matches the line height of [MaterialTheme.typography] bodyLarge so the control aligns to the
 *  first line of a wrapping answer rather than to the middle of the block. */
private val AnswerLineHeight = 24.dp
