package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.learning_context_coverage_count
import kmp_learning_app.shared.generated.resources.learning_context_coverage_title
import kmp_learning_app.shared.generated.resources.learning_context_not_studied
import kmp_learning_app.shared.generated.resources.progress_weak_label
import kmp_learning_app.shared.generated.resources.topic_detail_accuracy_caption
import kmp_learning_app.shared.generated.resources.topic_detail_available_questions
import kmp_learning_app.shared.generated.resources.topic_detail_no_questions
import kmp_learning_app.shared.generated.resources.topic_detail_start_practice
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset
import org.artkachenko.kmp_learning_app.ui.AccuracyHeadline
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.LearningContextUiModel
import org.artkachenko.kmp_learning_app.ui.PrimarySummaryCard
import org.artkachenko.kmp_learning_app.ui.ProgressMeter
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.SecondarySummaryCard
import org.artkachenko.kmp_learning_app.ui.StatusBadge
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.jetbrains.compose.resources.stringResource

/**
 * Topic-level practice: one primary action and the context that justifies it.
 *
 * Two kinds of practice entry point that must stay distinct. [onStartTopicPractice] is ordinary
 * practice and carries a scope only, so the builder applies its `ALL` default. [onPracticePreset]
 * carries a scope *and* an existing question source, and is emitted only where this page is already
 * displaying the signal that justifies it — the domain's `isWeak` verdict, or coverage that still
 * has current questions left in it. Neither is re-derived here, and neither ranks above the other:
 * a Topic that is both weak and partly covered offers both, because the learner chose to look at
 * it. Choosing one for them is what Recommended Next does, elsewhere and on purpose.
 *
 * The old in-page "Practice" heading is gone: the selected tab already says what this page is, and
 * with it goes the `hasStudySection` condition that only existed to decide whether the heading was
 * separating anything.
 */
@Composable
internal fun TopicPracticePage(
    topicId: String,
    topicQuestionCount: Int,
    learningContext: LearningContextUiModel?,
    onStartTopicPractice: () -> Unit,
    onPracticePreset: (PracticePreset) -> Unit,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    if (topicQuestionCount == 0) {
        // The Topic itself is fine and its other two tabs are untouched, so this states only that
        // there is nothing to practise rather than becoming a terminal screen.
        ScreenMessage(
            message = stringResource(Res.string.topic_detail_no_questions),
            modifier = modifier,
        )
        return
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(appScreenContentPadding(top = AppSpacing.Comfortable)),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
    ) {
        if (learningContext == null) {
            // Analytics are unavailable, so the page falls back to the authored count and says
            // nothing about the learner. Practice is unaffected.
            Text(
                text = stringResource(
                    Res.string.topic_detail_available_questions,
                    topicQuestionCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            TopicLearningSummary(learningContext)
        }
        // One primary action for the Topic; the Subtopics tab is the lower-emphasis path, so this
        // page never shows several filled buttons of equal weight.
        Button(
            onClick = onStartTopicPractice,
            modifier = Modifier.fillMaxWidth().testTag(TopicPracticeButtonTag),
        ) {
            Text(text = stringResource(Res.string.topic_detail_start_practice))
        }
        // Accelerators beside the primary action, never instead of it: with no analytics this block
        // is simply empty and ordinary practice is unaffected.
        TargetedPracticeActions(
            context = learningContext,
            onPracticeWeakAreas = {
                onPracticePreset(
                    PracticePreset(
                        scope = AssessmentScope.Topic(topicId),
                        source = PracticeQuestionSource.WEAK_AREAS,
                    ),
                )
            },
            onPracticeUnseen = {
                onPracticePreset(
                    PracticePreset(
                        scope = AssessmentScope.Topic(topicId),
                        source = PracticeQuestionSource.UNSEEN,
                    ),
                )
            },
            weakTestTag = TopicWeakPracticeTag,
            unseenTestTag = TopicUnseenPracticeTag,
        )
    }
}

/**
 * The Topic's learning summary: one coherent surface rather than two competing cards.
 *
 * All-time accuracy leads when there is any, because it is the figure the learner came for, with
 * current coverage under a divider as the second, differently-scoped question. With no accuracy to
 * lead on, the whole thing steps down to a quieter card: an unstudied Topic should not open with a
 * display-size headline, and it must never open with a fabricated 0%.
 */
@Composable
private fun TopicLearningSummary(context: LearningContextUiModel) {
    val accuracy = context.accuracyPercentage
    if (accuracy == null) {
        SecondarySummaryCard {
            Text(
                text = stringResource(Res.string.learning_context_not_studied),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TopicCoverage(context)
        }
    } else {
        PrimarySummaryCard {
            AccuracyHeadline(
                percentage = accuracy,
                caption = stringResource(Res.string.topic_detail_accuracy_caption),
            )
            if (context.isWeak) {
                StatusBadge(
                    text = stringResource(Res.string.progress_weak_label),
                    contentColor = AppThemeExtras.semanticColors.onPartiallyCorrectContainer,
                    containerColor = AppThemeExtras.semanticColors.partiallyCorrectContainer,
                    icon = AppIcons.Warning,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            TopicCoverage(context)
        }
    }
}

/**
 * Current curriculum coverage, in neutral theme colours throughout.
 *
 * Coverage is not scored: low coverage means material is still ahead of the learner, not that they
 * did badly, so it never borrows the correct/incorrect palette that accuracy uses. The meter is
 * driven by the exact counts rather than by the rounded percentage above it.
 */
@Composable
private fun TopicCoverage(context: LearningContextUiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight)) {
        Text(
            text = stringResource(Res.string.learning_context_coverage_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(
                Res.string.learning_context_coverage_count,
                context.attemptedQuestionCount,
                context.totalQuestionCount,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (context.hasCoverageScope) {
            ProgressMeter(
                fraction = context.attemptedQuestionCount.toFloat() / context.totalQuestionCount,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
