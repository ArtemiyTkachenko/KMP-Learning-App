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
import kmp_learning_app.shared.generated.resources.practice_shortcut_unseen
import kmp_learning_app.shared.generated.resources.practice_shortcut_weak_area
import org.artkachenko.kmp_learning_app.ui.LearningContextUiModel
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
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
    get() = this != null && (isWeak || hasUnseenQuestions)

/**
 * The targeted shortcuts a scope's already-derived learning context justifies, if any.
 *
 * Both conditions are read verbatim off the model: [LearningContextUiModel.isWeak] is the domain's
 * verdict and is never re-derived from the accuracy shown beside it, and
 * [LearningContextUiModel.hasUnseenQuestions] only restates the coverage counts already displayed.
 *
 * Both can be true at once and both are then offered. Which Questions either source actually yields
 * is decided later, by the selector, from history as it stands when practice is configured.
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
        if (context.hasUnseenQuestions) {
            TextButton(onClick = onPracticeUnseen, modifier = Modifier.testTag(unseenTestTag)) {
                Text(text = stringResource(Res.string.practice_shortcut_unseen))
            }
        }
    }
}
