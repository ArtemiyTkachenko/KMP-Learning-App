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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.learning_context_coverage_count
import kmp_learning_app.shared.generated.resources.learning_context_coverage_title
import kmp_learning_app.shared.generated.resources.learning_context_not_studied
import kmp_learning_app.shared.generated.resources.practice_shortcut_mistake_count
import kmp_learning_app.shared.generated.resources.practice_shortcut_unseen_count
import kmp_learning_app.shared.generated.resources.practice_shortcut_weak_area
import kmp_learning_app.shared.generated.resources.progress_weak_label
import kmp_learning_app.shared.generated.resources.topic_detail_accuracy_caption
import kmp_learning_app.shared.generated.resources.topic_detail_available_questions
import kmp_learning_app.shared.generated.resources.topic_detail_custom_practice
import kmp_learning_app.shared.generated.resources.topic_detail_no_questions
import kmp_learning_app.shared.generated.resources.topic_detail_recommended_mistakes_reason
import kmp_learning_app.shared.generated.resources.topic_detail_recommended_unseen_reason
import kmp_learning_app.shared.generated.resources.topic_detail_recommended_weak_reason
import kmp_learning_app.shared.generated.resources.topic_detail_start_practice
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
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
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Topic-level practice: the one action this Topic's evidence calls for, and the context that
 * justifies it.
 *
 * Two kinds of practice entry point that must stay distinct. [onStartTopicPractice] is ordinary
 * practice and carries a scope only, so the builder applies its `ALL` default; it is now the
 * *secondary* control, because "practise everything, unfiltered" is the least informed thing this
 * page can offer and used to be the only filled button on it. [onPracticePreset] carries a scope
 * *and* an existing question source, and is emitted for whichever source
 * [topicPracticeRecommendation] ranked first out of signals this page is already displaying.
 *
 * Neither signal is re-derived here: the weak verdict, the mistake count, and the coverage counts
 * all arrive already decided. What is new is only that one of them is promoted rather than all of
 * them being offered as equal text buttons — which is the same principle Recommended Next applies on
 * the Learn screen, now applied to the surface where the learner has already chosen the Topic.
 *
 * Custom practice stays one tap away and reaches the builder with every dimension editable, so
 * promoting a recommendation takes nothing away: a learner who wants unseen questions on a weak
 * Topic opens the builder and selects it.
 */
@Composable
internal fun TopicPracticePage(
    topicId: String,
    topicQuestionCount: Int,
    learningContext: LearningContextUiModel?,
    unresolvedMistakeCount: Int?,
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
    val recommendation = topicPracticeRecommendation(
        context = learningContext,
        unresolvedMistakeCount = unresolvedMistakeCount,
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(appScreenContentPadding(top = AppSpacing.Comfortable)),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
    ) {
        if (learningContext == null) {
            // Analytics are unavailable, so the page falls back to the authored count and says
            // nothing about the learner. The recommendation degrades with it: with no context and
            // no history the policy returns Everything, so the primary action is ordinary practice
            // and the secondary control disappears rather than duplicating it.
            Text(
                text = stringResource(
                    Res.string.topic_detail_available_questions,
                    topicQuestionCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            TopicLearningSummary(context = learningContext)
        }
        TopicPracticeActions(
            topicId = topicId,
            recommendation = recommendation,
            onStartTopicPractice = onStartTopicPractice,
            onPracticePreset = onPracticePreset,
        )
    }
}

/**
 * The promoted action, and the way past it.
 *
 * One filled button, always. When there is evidence it is the recommended run and a supporting line
 * underneath says why in the same terms the summary above already used; when there is none it is
 * ordinary practice, exactly as before, and there is no second control at all — a "Custom practice"
 * button beside an unfiltered "Start practice" button would be two labels for one thing.
 */
@Composable
private fun TopicPracticeActions(
    topicId: String,
    recommendation: TopicPracticeRecommendation,
    onStartTopicPractice: () -> Unit,
    onPracticePreset: (PracticePreset) -> Unit,
) {
    val isRecommendation = recommendation != TopicPracticeRecommendation.Everything
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Related),
    ) {
        Button(
            onClick = {
                if (isRecommendation) {
                    onPracticePreset(
                        PracticePreset(
                            scope = AssessmentScope.Topic(topicId),
                            source = recommendation.source,
                        ),
                    )
                } else {
                    onStartTopicPractice()
                }
            },
            modifier = Modifier.fillMaxWidth().testTag(TopicPracticeButtonTag),
        ) {
            Text(text = recommendation.actionLabel())
        }
        recommendation.reasonLabel()?.let { reason ->
            Text(
                text = reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isRecommendation) {
            TextButton(
                onClick = onStartTopicPractice,
                modifier = Modifier.testTag(TopicCustomPracticeTag),
            ) {
                Text(text = stringResource(Res.string.topic_detail_custom_practice))
            }
        }
    }
}

/**
 * What the promoted button says it will do.
 *
 * Switching on the recommendation is presentation mapping, not a decision: the source was already
 * chosen, and this only writes it in words. The unseen label carries its count for the same reason
 * the old shortcut did — it describes what the learner is looking at, not a promise about the run,
 * since which stable Question IDs are actually unseen is re-derived by the selector when practice is
 * configured and can legitimately have moved by then.
 */
@Composable
private fun TopicPracticeRecommendation.actionLabel(): String =
    when (this) {
        TopicPracticeRecommendation.WeakAreas ->
            stringResource(Res.string.practice_shortcut_weak_area)
        is TopicPracticeRecommendation.Mistakes -> pluralStringResource(
            Res.plurals.practice_shortcut_mistake_count,
            count,
            count,
        )
        is TopicPracticeRecommendation.Unseen -> pluralStringResource(
            Res.plurals.practice_shortcut_unseen_count,
            count,
            count,
        )
        TopicPracticeRecommendation.Everything ->
            stringResource(Res.string.topic_detail_start_practice)
    }

/**
 * Why this action, or `null` when there is nothing to justify.
 *
 * Always present for a recommendation, for the reason Recommended Next states: an action the learner
 * cannot see a reason for is the one thing a promoted choice is not allowed to be. The counts are
 * already in the labels, so these lines say what kind of evidence was used rather than restating a
 * figure.
 */
@Composable
private fun TopicPracticeRecommendation.reasonLabel(): String? =
    when (this) {
        TopicPracticeRecommendation.WeakAreas ->
            stringResource(Res.string.topic_detail_recommended_weak_reason)
        is TopicPracticeRecommendation.Mistakes ->
            stringResource(Res.string.topic_detail_recommended_mistakes_reason)
        is TopicPracticeRecommendation.Unseen ->
            stringResource(Res.string.topic_detail_recommended_unseen_reason)
        TopicPracticeRecommendation.Everything -> null
    }

/**
 * The Topic's learning summary: one coherent surface rather than two competing cards.
 *
 * All-time accuracy leads when there is any, because it is the figure the learner came for, with
 * current coverage under a divider as the second, differently-scoped question. With no accuracy to
 * lead on, the whole thing steps down to a quieter card: an unstudied Topic should not open with a
 * display-size headline, and it must never open with a fabricated 0%.
 *
 * It now carries no shortcuts of its own. The two text buttons that used to end this card were the
 * only evidence-driven practice on the page and they sat above the one filled button, which inverted
 * the emphasis; the promoted action below the card is built from exactly these figures, so the card
 * is left to state the evidence and the action to act on it.
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
