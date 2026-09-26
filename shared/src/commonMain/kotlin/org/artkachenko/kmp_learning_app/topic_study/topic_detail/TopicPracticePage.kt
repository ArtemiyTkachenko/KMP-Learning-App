package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.Alignment
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
import org.artkachenko.kmp_learning_app.ui.AccuracyHeroCard
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.LearningContextUiModel
import org.artkachenko.kmp_learning_app.ui.ProgressMeter
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.SecondarySummaryCard
import org.artkachenko.kmp_learning_app.ui.StatusBadge
import org.artkachenko.kmp_learning_app.ui.theme.AppMotion
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
        TopicPracticeSummary(
            context = learningContext,
            topicQuestionCount = topicQuestionCount,
        ) {
            TopicPracticeActions(
                topicId = topicId,
                recommendation = recommendation,
                onStartTopicPractice = onStartTopicPractice,
                onPracticePreset = onPracticePreset,
            )
        }
    }
}

/**
 * The promoted action, and the way past it — as the footer of the evidence it was derived from.
 *
 * One filled button, always. When there is evidence it is the recommended run; when there is none it
 * is ordinary practice, exactly as before, and there is no second control at all — a "Custom
 * practice" button beside an unfiltered "Start practice" button would be two labels for one thing.
 *
 * ## The reason comes first
 *
 * It used to sit *under* the button, which meant a learner read the decision and then its
 * justification: "Recommended: this is currently one of your weak areas." arrived as a footnote to
 * the control it exists to explain. Stating the premise first is what makes the block an argument
 * rather than a control with a caption, and it is the same order the surface above it already
 * uses — the figure, then what it means.
 *
 * ## Why this is inside the card
 *
 * Every input to the recommendation is printed on the surface this is the footer of: the weak badge,
 * the coverage counts, the accuracy. Emitted as a sibling it landed on the page background between
 * two surfaces, which is where a control reads as navigation for the screen rather than as what this
 * Topic offers — the defect `docs/development/material-design.md` records for a per-entry action,
 * and the reason `ReviewQuestionCard` grew a footer slot. It also left "Custom practice…" with
 * nothing to align to: a bare `TextButton` carries its own internal inset, so its label started a
 * few pixels right of every other line on the page.
 *
 * ## Why it is animated
 *
 * This is the one block on the page that changes while it is composed. Returning from a run
 * refreshes the coverage counts and the mistake queue, and the recommendation can legitimately move
 * — a weak Topic that is no longer weak becomes an unseen or a mistakes run, and the label, the
 * reason, and whether there is a secondary control at all all change together. One `AnimatedContent`
 * over the recommendation is what makes that read as the page reconsidering rather than as three
 * independent swaps in a single frame. Nothing waits on it, and the callbacks are untouched.
 */
@Composable
private fun ColumnScope.TopicPracticeActions(
    topicId: String,
    recommendation: TopicPracticeRecommendation,
    onStartTopicPractice: () -> Unit,
    onPracticePreset: (PracticePreset) -> Unit,
) {
    AnimatedContent(
        targetState = recommendation,
        transitionSpec = {
            val enter = fadeIn(AppMotion.revealSpec())
            val exit = fadeOut(AppMotion.effectSpec(AppMotion.StateChangeDurationMillis / 2))
            // The block's height changes with the recommendation — a reason line and a secondary
            // control appear and disappear — so the resize travels with the crossfade instead of
            // snapping under it.
            enter togetherWith exit using SizeTransform(clip = false)
        },
        modifier = Modifier.fillMaxWidth(),
        label = "topicPracticeRecommendation",
    ) { current ->
        val isRecommendation = current != TopicPracticeRecommendation.Everything
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Related),
        ) {
            current.reasonLabel()?.let { reason ->
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                // `current` rather than the outer recommendation on purpose: during a crossfade the
                // outgoing button is still on screen, and it must start the run its own label
                // names rather than the one replacing it.
                onClick = {
                    if (isRecommendation) {
                        onPracticePreset(
                            PracticePreset(
                                scope = AssessmentScope.Topic(topicId),
                                source = current.source,
                            ),
                        )
                    } else {
                        onStartTopicPractice()
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag(TopicPracticeButtonTag),
            ) {
                Text(text = current.actionLabel())
            }
            if (isRecommendation) {
                TextButton(
                    onClick = onStartTopicPractice,
                    // Centred under a full-width primary action. Leading-aligned it was the one
                    // line on the page that started at neither the card's inset nor the button's
                    // edge, because a `TextButton` insets its own label.
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag(TopicCustomPracticeTag),
                ) {
                    Text(text = stringResource(Res.string.topic_detail_custom_practice))
                }
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
 * The whole Practice page as one surface: what is known about this Topic, then what to do about it.
 *
 * All-time accuracy leads when there is any, because it is the figure the learner came for: the
 * page's [AccuracyHeroCard], with current coverage under a rule as the second, differently-scoped
 * question. The two are deliberately drawn with different controls — a ring for the rate, a meter
 * for how much of the bank is behind the learner — because the only reason they share a card is
 * that they are not the same reading.
 *
 * With no accuracy to lead on, the whole thing steps down to a quieter card rather than passing a
 * null figure to the hero: an unstudied Topic should not open with a display-size surface at all,
 * and it must never open with a fabricated 0%. The third branch is analytics being absent
 * altogether, which used to be a lone `bodyMedium onSurfaceVariant` line on the page background
 * above the only button — the quietest text the scale has, acting as a caption for nothing. It
 * takes the same quiet card and states the authored count at the weight the other two states state
 * their subject.
 *
 * [action] is the footer every branch ends with, under a rule of its own. The card carries the
 * action rather than sitting above it because the action is *derived from this card* — see
 * [TopicPracticeActions]. Two rules therefore appear in the accuracy branch, which is correct: it
 * has three things to say — the rate, the coverage, and the run they call for — and each is a
 * different kind of statement.
 */
@Composable
private fun TopicPracticeSummary(
    context: LearningContextUiModel?,
    topicQuestionCount: Int,
    action: @Composable ColumnScope.() -> Unit,
) {
    val accuracy = context?.accuracyPercentage
    if (context != null && accuracy != null) {
        AccuracyHeroCard(
            percentage = accuracy,
            caption = stringResource(Res.string.topic_detail_accuracy_caption),
        ) {
            if (context.isWeak) {
                StatusBadge(
                    text = stringResource(Res.string.progress_weak_label),
                    contentColor = AppThemeExtras.semanticColors.onPartiallyCorrectContainer,
                    containerColor = AppThemeExtras.semanticColors.partiallyCorrectContainer,
                    icon = AppIcons.Warning,
                )
            }
            SummaryRule()
            TopicCoverage(context)
            SummaryRule()
            action()
        }
        return
    }
    SecondarySummaryCard {
        Text(
            // A context that has never been answered says so; no context at all cannot say anything
            // about the learner, so it falls back to what the curriculum authored. The
            // recommendation degrades with it: with no context and no history the policy returns
            // Everything, so the action below is ordinary practice and the secondary control
            // disappears rather than duplicating it.
            text = if (context != null) {
                stringResource(Res.string.learning_context_not_studied)
            } else {
                stringResource(Res.string.topic_detail_available_questions, topicQuestionCount)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        context?.let { TopicCoverage(it) }
        SummaryRule()
        action()
    }
}

/** The hairline between two kinds of statement on one summary surface. */
@Composable
private fun SummaryRule() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/**
 * Current curriculum coverage, in neutral theme colours throughout.
 *
 * Coverage is not scored: low coverage means material is still ahead of the learner, not that they
 * did badly, so it never borrows the correct/incorrect palette that accuracy uses. The meter is
 * driven by the exact counts rather than by the rounded percentage above it.
 *
 * The meter needs something to measure as well as a bank to measure against. A Topic nobody has
 * opened was drawing an empty bar under "0 of 42 questions explored" — a gauge at a value the
 * learner never produced, which is the reading `docs/development/material-design.md` rules out for
 * an empty state. The counts line stays, because it is a true statement and it is already the thing
 * that says nothing has been attempted.
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
        if (context.hasCoverageScope && context.attemptedQuestionCount > 0) {
            ProgressMeter(
                fraction = context.attemptedQuestionCount.toFloat() / context.totalQuestionCount,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag(TopicCoverageMeterTag),
            )
        }
    }
}
