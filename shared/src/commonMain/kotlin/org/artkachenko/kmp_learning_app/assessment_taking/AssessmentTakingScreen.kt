package org.artkachenko.kmp_learning_app.assessment_taking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            // Entering rather than appearing. The verdict and the explanation are new content
            // arriving under answers the learner is already looking at, so they decelerate into
            // place; `key(question.id)` above resets this, so every question reveals once.
            AnimatedVisibility(
                visible = state.feedback != null,
                enter = fadeIn(AppMotion.effectSpec()) + expandVertically(AppMotion.spatialSpec()),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped)) {
                    QuestionOutcomeBadge(
                        outcome = questionOutcome(
                            scoredCorrect = state.feedback?.isCorrect == true,
                            selectedAnswerIds = state.selectedAnswerIds,
                            correctAnswerIds = state.question.correctAnswerIds,
                        ),
                        modifier = Modifier.testTag(AssessmentTakingOutcomeTag),
                    )
                    QuestionExplanationBlock(state.question.explanation)
                }
            }
            Button(
                onClick = if (state.feedback == null) onSubmit else onNext,
                enabled = (state.canSubmit && !state.isSubmitting) || state.feedback != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AssessmentTakingSubmitTag),
            ) {
                Text(
                    if (state.feedback != null) {
                        stringResource(Res.string.assessment_taking_next_question)
                    } else {
                        stringResource(
                            if (state.isSubmitting) {
                                Res.string.assessment_taking_submitting
                            } else {
                                Res.string.assessment_taking_submit
                            },
                        )
                    },
                )
            }
        }
    }
}

/**
 * One answer option, in the three states it has: at rest, chosen, and marked.
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
    val selectionModifier = if (mode == AnswerSelectionMode.SINGLE) {
        Modifier.selectable(
            selected = selected,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onClick,
        )
    } else {
        Modifier.toggleable(
            value = selected,
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
    val marked = outcome?.colors(neutralContainer = restingContainer)

    // Choosing an answer is the action this whole product exists for, and it used to be the least
    // responsive thing in it: the container colour and the border jumped between two values in a
    // single frame, so the row registered the tap without ever acknowledging it. Easing the three
    // properties is the feedback — the state is what is being animated, not decoration around it.
    // The reveal rides the same three animations, so a result grows out of the selection rather
    // than replacing it.
    val containerColor by animateColorAsState(
        targetValue = when {
            marked != null -> marked.container
            selected -> MaterialTheme.colorScheme.secondaryContainer
            else -> restingContainer
        },
        animationSpec = AppMotion.effectSpec(),
        label = "answerContainer",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            marked != null -> marked.border
            selected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = AppMotion.effectSpec(),
        label = "answerBorder",
    )
    // The border width is spatial rather than an effect: it is a size, so it springs like one. A
    // marked row is emphasised on the same terms as a chosen one, except where the mark is the
    // absence of one — an option that was neither picked nor correct has nothing to emphasise.
    val borderWidth by animateDpAsState(
        targetValue = if (selected || (outcome != null && outcome != AnswerOutcome.NEUTRAL)) {
            SelectedBorderWidth
        } else {
            UnselectedBorderWidth
        },
        animationSpec = AppMotion.spatialSpec(),
        label = "answerBorderWidth",
    )

    Surface(
        modifier = Modifier.fillMaxWidth().then(selectionModifier),
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
                        accent = marked?.tagColor,
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
                    // A marked row states its outcome in words and colour of its own, so the answer
                    // text stays the ordinary reading colour rather than taking the selected one.
                    color = if (selected && outcome == null) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                val label = outcome?.tagLabel()
                if (label != null && marked?.tagColor != null) {
                    QuestionAnswerTag(text = label, color = marked.tagColor)
                }
            }
        }
    }
}

/**
 * The radio or checkbox, tinted by outcome once there is one.
 *
 * [accent] is null while the question is open, and the control then uses Material's own selection
 * colours. After the reveal it is the outcome's accent, so the mark the learner reads first — the
 * filled control they put there themselves — is already the right or wrong colour before they get
 * to the label.
 */
@Composable
private fun AnswerControl(
    mode: AnswerSelectionMode,
    selected: Boolean,
    enabled: Boolean,
    accent: Color?,
) {
    if (mode == AnswerSelectionMode.SINGLE) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
            colors = if (accent == null) {
                RadioButtonDefaults.colors()
            } else {
                RadioButtonDefaults.colors(selectedColor = accent, unselectedColor = accent)
            },
        )
    } else {
        Checkbox(
            checked = selected,
            onCheckedChange = null,
            enabled = enabled,
            colors = if (accent == null) {
                CheckboxDefaults.colors()
            } else {
                CheckboxDefaults.colors(checkedColor = accent, uncheckedColor = accent)
            },
        )
    }
}

private val SelectedBorderWidth = 2.dp
private val UnselectedBorderWidth = 1.dp

private val AnswerRowMinHeight = 48.dp

/** Matches the line height of [MaterialTheme.typography] bodyLarge so the control aligns to the
 *  first line of a wrapping answer rather than to the middle of the block. */
private val AnswerLineHeight = 24.dp
