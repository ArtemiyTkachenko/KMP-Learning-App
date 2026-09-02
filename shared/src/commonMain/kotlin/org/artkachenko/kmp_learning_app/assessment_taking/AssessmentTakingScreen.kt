package org.artkachenko.kmp_learning_app.assessment_taking

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.assessment_taking_answer_save_error
import kmp_learning_app.shared.generated.resources.assessment_taking_completion_save_error
import kmp_learning_app.shared.generated.resources.assessment_taking_finish
import kmp_learning_app.shared.generated.resources.assessment_taking_loading
import kmp_learning_app.shared.generated.resources.assessment_taking_no_questions
import kmp_learning_app.shared.generated.resources.assessment_taking_question_progress
import kmp_learning_app.shared.generated.resources.assessment_taking_ready
import kmp_learning_app.shared.generated.resources.assessment_taking_results_opening
import kmp_learning_app.shared.generated.resources.assessment_taking_select_all
import kmp_learning_app.shared.generated.resources.assessment_taking_select_one
import kmp_learning_app.shared.generated.resources.assessment_taking_start_error
import kmp_learning_app.shared.generated.resources.assessment_taking_submit
import kmp_learning_app.shared.generated.resources.assessment_taking_submitting
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

internal const val AssessmentTakingLoadingTag = "focused_practice_loading"
internal const val AssessmentTakingSubmitTag = "focused_practice_submit"
internal const val AssessmentTakingFinishTag = "focused_practice_finish"

internal const val AssessmentProgressMeterTag = "assessment_progress_meter"


@Composable
internal fun AssessmentTakingScreen(
    title: String,
    state: AssessmentTakingUiState,
    onAnswerClick: (String) -> Unit,
    onSubmit: () -> Unit,
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

            is AssessmentTakingUiState.Content -> QuestionContent(
                state = state,
                onAnswerClick = onAnswerClick,
                onSubmit = onSubmit,
                modifier = Modifier.weight(1f),
            )

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
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag(AssessmentProgressMeterTag),
    )
}

@Composable
private fun QuestionContent(
    state: AssessmentTakingUiState.Content,
    onAnswerClick: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = appScreenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                modifier = Modifier.padding(top = 12.dp),
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
                modifier = Modifier.padding(top = 8.dp),
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
                enabled = !state.isSubmitting,
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
            Button(
                onClick = onSubmit,
                enabled = state.canSubmit && !state.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AssessmentTakingSubmitTag),
            ) {
                Text(
                    text = stringResource(
                        if (state.isSubmitting) {
                            Res.string.assessment_taking_submitting
                        } else {
                            Res.string.assessment_taking_submit
                        },
                    ),
                )
            }
        }
    }
}

/**
 * One answer option.
 *
 * The row is the touch target and the selection surface: answers used to be bare rows separated
 * only by 6dp, so they were hard to tell apart, and the control was centred against the whole
 * block instead of the first line of a wrapping answer.
 */
@Composable
private fun AnswerRow(
    answerText: String,
    selected: Boolean,
    mode: AnswerSelectionMode,
    enabled: Boolean,
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

    // Choosing an answer is the action this whole product exists for, and it used to be the least
    // responsive thing in it: the container colour and the border jumped between two values in a
    // single frame, so the row registered the tap without ever acknowledging it. Easing the three
    // properties is the feedback — the state is what is being animated, not decoration around it.
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = AppMotion.effectSpec(),
        label = "answerContainer",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = AppMotion.effectSpec(),
        label = "answerBorder",
    )
    // The border width is spatial rather than an effect: it is a size, so it springs like one.
    val borderWidth by animateDpAsState(
        targetValue = if (selected) SelectedBorderWidth else UnselectedBorderWidth,
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
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // The control's own 48dp minimum would push it below the first text line, so the
            // enforcement is dropped here and the row above carries the touch target instead.
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Box(
                    modifier = Modifier.height(AnswerLineHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    if (mode == AnswerSelectionMode.SINGLE) {
                        RadioButton(selected = selected, onClick = null, enabled = enabled)
                    } else {
                        Checkbox(checked = selected, onCheckedChange = null, enabled = enabled)
                    }
                }
            }
            Text(
                text = answerText,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

private val SelectedBorderWidth = 2.dp
private val UnselectedBorderWidth = 1.dp

private val AnswerRowMinHeight = 48.dp

/** Matches the line height of [MaterialTheme.typography] bodyLarge so the control aligns to the
 *  first line of a wrapping answer rather than to the middle of the block. */
private val AnswerLineHeight = 24.dp
