package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.practice_shortcut_unseen_count
import kmp_learning_app.shared.generated.resources.practice_shortcut_weak_area
import org.artkachenko.kmp_learning_app.ui.LearningContextUiModel
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The targeted practice accelerators, shared by the Practice page and every Subtopic row.
 *
 * They are one implementation because the two surfaces must offer the same two intents under the
 * same two conditions; only the scope they carry and their test tags differ. Both live in this file
 * rather than in either page so neither page owns the other's shortcut rules.
 */

/**
 * Whether a scope's already-derived context justifies any targeted shortcut at all.
 *
 * Defined on the nullable receiver so an unknown context answers `false` here rather than at every
 * call site: unknown analytics justify nothing, and that is a different statement from "not weak,
 * nothing left to see". This is also the only place the layout asks the question, so a row's
 * spacing and its controls cannot disagree about whether it has any.
 */
internal val LearningContextUiModel?.hasTargetedPractice: Boolean
    get() = this != null && (isWeak || hasPartialCoverage)

/**
 * Whether unseen practice would actually narrow anything for this scope.
 *
 * Deliberately stricter than [LearningContextUiModel.hasUnseenQuestions], which asks only whether
 * any current Question is still unmet. That is a truthful statement about coverage, and it is the
 * wrong question to put a *shortcut* behind: on a scope with nothing attempted it is true of the
 * whole bank, so unseen practice and ordinary practice would draw from one identical pool. Two
 * controls for one outcome is not an accelerator — and because it is true of every scope at once, a
 * Topic nobody has touched used to repeat the offer on the Topic and on every Subtopic beneath it,
 * where it distinguished nothing from nothing.
 *
 * Requiring something to have been attempted is what makes the offer mean "the part you have not
 * reached yet". The model property is left alone: it describes coverage, other surfaces ask it, and
 * this is a presentation decision about when an action is worth showing.
 */
private val LearningContextUiModel.hasPartialCoverage: Boolean
    get() = attemptedQuestionCount > 0 && hasUnseenQuestions

/**
 * The targeted shortcuts a scope's already-derived learning context justifies, if any.
 *
 * Both conditions are read off the model: [LearningContextUiModel.isWeak] is the domain's verdict
 * and is never re-derived from the accuracy shown beside it, and [hasPartialCoverage] only restates
 * the coverage counts already displayed.
 *
 * The unseen label carries how many Questions are left, from the same two counts the row is already
 * showing. It is a description of what the learner is looking at, not a promise about the run: which
 * stable Question IDs are actually unseen stays with the selector, which re-derives them from
 * completed history at the moment practice is configured, and the number can legitimately have moved
 * by then.
 *
 * Both can be true at once and both are then offered. Nothing here ranks one above the other: the
 * learner chose to look at this scope, and choosing one intent for them is what Recommended Next
 * does, on a different surface and on purpose.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TargetedPracticeActions(
    context: LearningContextUiModel?,
    onPracticeWeakAreas: () -> Unit,
    onPracticeUnseen: () -> Unit,
    weakTestTag: String,
    unseenTestTag: String,
    modifier: Modifier = Modifier,
) {
    if (context == null || !context.hasTargetedPractice) return
    // Wraps rather than clips: two labelled text buttons do not share a line on a compact width or
    // at a large font scale.
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
    ) {
        if (context.isWeak) {
            TextButton(onClick = onPracticeWeakAreas, modifier = Modifier.testTag(weakTestTag)) {
                Text(text = stringResource(Res.string.practice_shortcut_weak_area))
            }
        }
        if (context.hasPartialCoverage) {
            val unseenCount = context.totalQuestionCount - context.attemptedQuestionCount
            TextButton(onClick = onPracticeUnseen, modifier = Modifier.testTag(unseenTestTag)) {
                Text(
                    text = pluralStringResource(
                        Res.plurals.practice_shortcut_unseen_count,
                        unseenCount,
                        unseenCount,
                    ),
                )
            }
        }
    }
}
