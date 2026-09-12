package org.artkachenko.kmp_learning_app.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.mistake_review_none
import kmp_learning_app.shared.generated.resources.mistake_review_unresolved_count
import kmp_learning_app.shared.generated.resources.progress_review_mistakes_action
import kmp_learning_app.shared.generated.resources.mixed_interview_title
import kmp_learning_app.shared.generated.resources.practice_shortcut_weak_area
import kmp_learning_app.shared.generated.resources.progress_accuracy_caption
import kmp_learning_app.shared.generated.resources.progress_completed_attempts_label
import kmp_learning_app.shared.generated.resources.progress_correct_answers_label
import kmp_learning_app.shared.generated.resources.progress_coverage_count
import kmp_learning_app.shared.generated.resources.progress_coverage_title
import kmp_learning_app.shared.generated.resources.progress_coverage_unavailable
import kmp_learning_app.shared.generated.resources.progress_empty
import kmp_learning_app.shared.generated.resources.progress_empty_action
import kmp_learning_app.shared.generated.resources.progress_error
import kmp_learning_app.shared.generated.resources.progress_focused_practice
import kmp_learning_app.shared.generated.resources.progress_focused_subtopic_scope
import kmp_learning_app.shared.generated.resources.progress_history
import kmp_learning_app.shared.generated.resources.progress_loading
import kmp_learning_app.shared.generated.resources.progress_overall
import kmp_learning_app.shared.generated.resources.progress_questions_answered_label
import kmp_learning_app.shared.generated.resources.progress_recent_title
import kmp_learning_app.shared.generated.resources.progress_recent_trend_description
import kmp_learning_app.shared.generated.resources.progress_recent_trend_insufficient
import kmp_learning_app.shared.generated.resources.progress_recent_trend_title
import kmp_learning_app.shared.generated.resources.progress_recent_window_one
import kmp_learning_app.shared.generated.resources.progress_recent_window_other
import kmp_learning_app.shared.generated.resources.progress_score
import kmp_learning_app.shared.generated.resources.progress_subtopic_unavailable
import kmp_learning_app.shared.generated.resources.progress_title
import kmp_learning_app.shared.generated.resources.progress_topic_performance
import kmp_learning_app.shared.generated.resources.progress_topic_unavailable
import kmp_learning_app.shared.generated.resources.progress_weak_areas
import kmp_learning_app.shared.generated.resources.progress_weak_areas_none_detail
import kmp_learning_app.shared.generated.resources.progress_weak_areas_none_title
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset
import org.artkachenko.kmp_learning_app.ui.AccuracyHeadline
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.AppTwoPaneRow
import org.artkachenko.kmp_learning_app.ui.theme.AppContentWidth
import org.artkachenko.kmp_learning_app.ui.theme.AppScreenPane
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppWindowSizeClass
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.MetricFigure
import org.artkachenko.kmp_learning_app.ui.MetricRow
import org.artkachenko.kmp_learning_app.ui.PerformanceCard
import org.artkachenko.kmp_learning_app.ui.ProgressMeter
import org.artkachenko.kmp_learning_app.ui.PrimarySummaryCard
import org.artkachenko.kmp_learning_app.ui.ScreenAction
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenStateTransition
import org.artkachenko.kmp_learning_app.ui.SecondarySummaryCard
import org.artkachenko.kmp_learning_app.ui.accuracyColor
import org.artkachenko.kmp_learning_app.ui.formatAccuracy
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.artkachenko.kmp_learning_app.ui.time.timestampText
import org.jetbrains.compose.resources.stringResource

internal const val ProgressLoadingTag = "progress_loading"

/** The scrolling dashboard itself, so tests can reach sections below the fold. */
internal const val ProgressContentTag = "progress_content"

/**
 * The two panes of the expanded dashboard.
 *
 * [ProgressContentTag] deliberately does not move onto either of them. It names the single
 * scrolling dashboard, and at expanded widths there is no such thing — there are two scrollers
 * holding different groups, and a test that scrolled "the dashboard" to find a Topic card would be
 * asking a question with no answer. A test that cares about the expanded layout names the pane it
 * expects the content in, which is the assertion worth making.
 */
internal const val ProgressStandingPaneTag = "progress_standing_pane"
internal const val ProgressActionPaneTag = "progress_action_pane"

/** Stable per-row handle so tests can target a Topic card without depending on label uniqueness. */
internal fun progressTopicCardTag(topicId: String): String = "progress_topic_card_$topicId"

/** Stable per-row handle for completed attempts whose visible labels may be identical. */
internal fun progressHistoryCardTag(attemptId: String): String = "progress_history_card_$attemptId"

/** The row that opens Mistake Review, so a test can act on it rather than on its label text. */
internal const val ProgressReviewMistakesTag = "progress_review_mistakes"

/**
 * Material's minimum touch target. Stated here because this row is not a Material component and so
 * gets no minimum of its own, and it becomes a tap target whenever the queue is non-empty.
 */
private val MinimumTouchTargetSize = 48.dp

/** Stable per-row handle for a weak area's practice shortcut, whose label repeats across rows. */
internal fun progressWeakAreaPracticeTag(area: WeakAreaUiModel): String =
    "progress_weak_area_practice_${area.type}_${area.stableId}"

/**
 * [onPracticePreset] carries a semantic practice intent, never a route: the dashboard says which
 * scope and which existing question source the learner asked for, and the navigation boundary turns
 * that into the Practice Builder. Only weak-area rows produce one — they are the dashboard's only
 * signal that names a Topic or Subtopic.
 */
@Composable
internal fun ProgressScreen(
    state: ProgressUiState,
    onBack: (() -> Unit)? = null,
    onRetry: () -> Unit,
    onBrowseTopics: () -> Unit,
    onTopicClick: (String) -> Unit,
    onHistoryClick: (CompletedAssessmentType, String) -> Unit,
    onPracticePreset: (PracticePreset) -> Unit,
    onReviewMistakes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = rememberAppTopBarScrollBehavior()
    Column(modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        AppTopBar(stringResource(Res.string.progress_title), onBack, scrollBehavior)
        // The bar above spans the window; the content below it does not. A `TopAppBar` is chrome
        // and belongs to the window, which is why it sits outside the pane — inside one, a wide
        // desktop window showed a bar that stopped short of both edges and floated over the page.
        AppScreenPane(AppContentWidth.Paned) {
            ScreenStateTransition(state = state, modifier = Modifier.fillMaxSize()) { current ->
                when (current) {
                    ProgressUiState.Loading -> ScreenLoading(
                        message = stringResource(Res.string.progress_loading),
                        testTag = ProgressLoadingTag,
                        modifier = Modifier.fillMaxSize(),
                    )
                    ProgressUiState.Empty -> ScreenAction(
                        message = stringResource(Res.string.progress_empty),
                        actionLabel = stringResource(Res.string.progress_empty_action),
                        onAction = onBrowseTopics,
                        modifier = Modifier.fillMaxSize(),
                        icon = AppIcons.Insights,
                    )
                    ProgressUiState.Error -> ScreenError(
                        message = stringResource(Res.string.progress_error),
                        onRetry = onRetry,
                        modifier = Modifier.fillMaxSize(),
                    )
                    is ProgressUiState.Content -> ProgressContent(
                        state = current,
                        onTopicClick = onTopicClick,
                        onHistoryClick = onHistoryClick,
                        onPracticePreset = onPracticePreset,
                        onReviewMistakes = onReviewMistakes,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

/**
 * The dashboard, as one column or as two.
 *
 * The information is the same either way and so is its order. What changes is only how much of it
 * a learner can see at once: on a phone the three groups follow one another down a single scroll,
 * and on a desktop the standing group — the lifetime figures, coverage, and the recent window —
 * sits in its own pane while the groups a learner can *act* on take the other. That is the split
 * the screen already had implicitly, stated in the layout.
 *
 * The groups are declared once, as [LazyListScope] extensions, and both layouts call them in the
 * same order. This is the mechanism that keeps the two honest: a section added to the dashboard
 * reaches the phone and the desktop at the same position, and neither arrangement can quietly
 * acquire a card the other does not have. It is also the accessibility guarantee — composition
 * order is traversal order, so the desktop reads standing, then actionable, then detail, exactly
 * as the phone does.
 *
 * The P0 semantic distinctions survive the split unchanged: accuracy stays a rate on a display
 * figure, coverage stays a count out of a finite bank with the meter as a second channel, studied
 * state is not represented here at all, and recent performance keeps its own labelled card with
 * its own evidence rule. Nothing became a tile in a grid of interchangeable numbers.
 */
@Composable
private fun ProgressContent(
    state: ProgressUiState.Content,
    onTopicClick: (String) -> Unit,
    onHistoryClick: (CompletedAssessmentType, String) -> Unit,
    onPracticePreset: (PracticePreset) -> Unit,
    onReviewMistakes: () -> Unit,
    modifier: Modifier,
) {
    if (LocalAppWindowSizeClass.current.isExpanded) {
        AppTwoPaneRow(
            modifier = modifier,
            primary = {
                ProgressPane(Modifier.weight(1f).testTag(ProgressStandingPaneTag)) {
                    standingSection(state)
                }
            },
            secondary = {
                ProgressPane(Modifier.weight(1f).testTag(ProgressActionPaneTag)) {
                    actionableSection(state, onPracticePreset, onReviewMistakes)
                    detailSection(state, onTopicClick, onHistoryClick)
                }
            },
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(ProgressContentTag),
        contentPadding = appScreenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
    ) {
        standingSection(state)
        actionableSection(state, onPracticePreset, onReviewMistakes)
        detailSection(state, onTopicClick, onHistoryClick)
    }
}

/** One pane of the expanded dashboard: the same list styling, scrolling on its own. */
@Composable
private fun ProgressPane(
    modifier: Modifier,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxHeight(),
        contentPadding = appScreenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
        content = content,
    )
}

/** Where the learner stands: lifetime accuracy, how much of the bank they have seen, and lately. */
private fun LazyListScope.standingSection(state: ProgressUiState.Content) {
    item {
        ProgressSectionTitle(stringResource(Res.string.progress_overall))
    }
    item {
        OverallSummary(state)
    }
    // Coverage and recent performance sit under the headline as quieter summaries: they answer
    // different questions from all-time accuracy, so they must be separate surfaces, but making
    // all three equally dominant would leave the screen with no headline at all.
    item {
        CurriculumCoverageSummary(state.coverage)
    }
    state.recentPerformance?.let { recent ->
        item {
            RecentPerformanceSummary(recent)
        }
    }
}

/** What the learner can do something about: the mistake queue, and the areas going badly. */
private fun LazyListScope.actionableSection(
    state: ProgressUiState.Content,
    onPracticePreset: (PracticePreset) -> Unit,
    onReviewMistakes: () -> Unit,
) {
    item {
        UnresolvedMistakeSummary(
            unresolvedCount = state.unresolvedMistakeCount,
            onReviewMistakes = onReviewMistakes,
        )
    }
    if (state.weakAreas.isNotEmpty()) {
        item {
            ProgressSectionTitle(
                stringResource(Res.string.progress_weak_areas),
                topPadding = AppSpacing.Grouped,
            )
        }
        items(state.weakAreas, key = { "${it.type}:${it.stableId}" }) { area ->
            WeakAreaCard(area) { onPracticePreset(area.toPracticePreset()) }
        }
    } else if (state.topics.isNotEmpty()) {
        // Evidence, not emptiness — but only where the emptiness is actually evidence.
        //
        // An empty weak-area list means one of two quite different things, and the state carries
        // no flag saying which. `topics` is what tells them apart: if per-Topic performance was
        // derived at all, then the weakness rule ran over real observations and simply singled
        // nothing out, which is a fact worth stating. If `topics` is empty too, the derivation
        // produced nothing — the case a curriculum import creates when it replaces the question
        // IDs the history refers to — and "no area stands out" would be a claim about a learner
        // the app has no current observations for. That case keeps the P0 behaviour of showing no
        // section at all rather than a dangling heading over a sentence that is not true.
        //
        // Nothing is classified here to fill the gap either way: which areas qualify as weak
        // remains the P0 evidence rule's answer.
        item {
            ProgressSectionTitle(
                stringResource(Res.string.progress_weak_areas),
                topPadding = AppSpacing.Grouped,
            )
        }
        item {
            WeakAreasEarlyState(answeredQuestionCount = state.answeredQuestionCount)
        }
    }
}

/** The record behind the figures: per-Topic performance, then completed attempts. */
private fun LazyListScope.detailSection(
    state: ProgressUiState.Content,
    onTopicClick: (String) -> Unit,
    onHistoryClick: (CompletedAssessmentType, String) -> Unit,
) {
    // Observation-based sections can be empty even when overall statistics exist, for
    // example after a curriculum import replaces the question IDs the history refers to.
    if (state.topics.isNotEmpty()) {
        item {
            ProgressSectionTitle(
                stringResource(Res.string.progress_topic_performance),
                topPadding = AppSpacing.Grouped,
            )
        }
        items(state.topics, key = ProgressTopicUiModel::topicId) { topic ->
            TopicPerformanceCard(topic) { onTopicClick(topic.topicId) }
        }
    }
    if (state.history.isNotEmpty()) {
        item {
            ProgressSectionTitle(
                stringResource(Res.string.progress_history),
                topPadding = AppSpacing.Grouped,
            )
        }
        items(state.history, key = CompletedAttemptUiModel::attemptId) { attempt ->
            HistoryCard(attempt) {
                onHistoryClick(attempt.assessmentType, attempt.attemptId)
            }
        }
    }
}

/**
 * What the weak-areas section says when the domain has named none.
 *
 * Reached only once the learner has answered something: with no answers at all the whole dashboard
 * is `ProgressUiState.Empty` and this section never composes. So the sentence can be specific —
 * there is evidence, it simply has not singled anything out yet — rather than the generic "nothing
 * here" that would leave a learner unsure whether the feature was broken or they were doing well.
 */
@Composable
private fun WeakAreasEarlyState(answeredQuestionCount: Int) {
    // Deliberately not a card. The section it stands in is a column of cards, each of which is a
    // weak area; a card here would be a seventh surface of the same kind holding the statement
    // that there are none, which is the shape of the thing it is denying. Two lines of type under
    // the heading say it without pretending to be a row.
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight)) {
        Text(
            text = stringResource(Res.string.progress_weak_areas_none_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(
                Res.string.progress_weak_areas_none_detail,
                answeredQuestionCount,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Accuracy is the headline of the whole app, so it leads at display size with a meter behind it;
 * the counts that support it become a scannable label/value column instead of four equal lines.
 */
@Composable
private fun OverallSummary(state: ProgressUiState.Content) {
    PrimarySummaryCard {
        AccuracyHeadline(
            percentage = state.percentage,
            caption = stringResource(Res.string.progress_accuracy_caption),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight)) {
            MetricRow(
                label = stringResource(Res.string.progress_completed_attempts_label),
                value = state.completedAttemptCount.toString(),
            )
            MetricRow(
                label = stringResource(Res.string.progress_questions_answered_label),
                value = state.answeredQuestionCount.toString(),
            )
            MetricRow(
                label = stringResource(Res.string.progress_correct_answers_label),
                value = state.correctAnswerCount.toString(),
            )
        }
    }
}

/**
 * How much of the current question bank the learner has seen — a different question from how
 * accurately they answered it, and one the percentage alone cannot answer, so the raw counts are
 * always shown beside it and the meter is never the only representation.
 *
 * The figure is deliberately not tinted with [accuracyColor]: colouring 30% coverage red would read
 * as a bad score, when it only means most of the bank is still ahead of the learner.
 */
@Composable
private fun CurriculumCoverageSummary(coverage: ProgressCoverageUiModel) {
    SecondarySummaryCard {
        Text(
            text = stringResource(Res.string.progress_coverage_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val percentage = coverage.percentage
        if (percentage == null) {
            // 0/0 is "nothing to cover", not 0% covered, so say that rather than draw an empty bar.
            Text(
                text = stringResource(Res.string.progress_coverage_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(
                    Res.string.progress_coverage_count,
                    coverage.attemptedQuestionCount,
                    coverage.totalQuestionCount,
                ),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ProgressMeter(
                // The exact count ratio, not the rounded percentage above it.
                fraction = coverage.attemptedQuestionCount.toFloat() / coverage.totalQuestionCount,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * The latest few assessments, kept visibly apart from the lifetime figures above: all-time accuracy
 * moves very slowly once history is long, so a learner who has improved needs a second, explicitly
 * labelled signal rather than a reweighted first one.
 *
 * The percentage is the domain's question-weighted accuracy across the whole window, not the mean of
 * the plotted attempts — a 1/1 attempt and a 10/20 attempt make 11/21, not 75%.
 */
@Composable
private fun RecentPerformanceSummary(recent: ProgressRecentPerformanceUiModel) {
    SecondarySummaryCard {
        Text(
            text = stringResource(Res.string.progress_recent_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        MetricFigure(
            text = formatAccuracy(recent.percentage),
            color = accuracyColor(recent.percentage),
        )
        Text(
            text = recentWindowLabel(recent.attemptCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(
                Res.string.progress_score,
                recent.correctAnswerCount,
                recent.answeredQuestionCount,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (val trend = recent.trend) {
            // Still real evidence, so the summary above stays; only the trajectory is withheld, and
            // as a plain statement rather than a warning about something the learner did wrong.
            is ProgressRecentTrendUiModel.InsufficientHistory -> Text(
                text = stringResource(
                    Res.string.progress_recent_trend_insufficient,
                    trend.requiredAttemptCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            is ProgressRecentTrendUiModel.Available -> {
                Text(
                    text = stringResource(Res.string.progress_recent_trend_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val percentages = trend.attempts.map(ProgressRecentAttemptUiModel::percentage)
                RecentTrendChart(
                    percentages = percentages,
                    description = stringResource(
                        Res.string.progress_recent_trend_description,
                        percentages.joinToString(transform = ::formatAccuracy),
                    ),
                )
            }
        }
    }
}

@Composable
private fun recentWindowLabel(attemptCount: Int): String =
    if (attemptCount == 1) {
        stringResource(Res.string.progress_recent_window_one)
    } else {
        stringResource(Res.string.progress_recent_window_other, attemptCount)
    }

/**
 * The size of the mistake queue, and — when there is one — the way into it.
 *
 * This used to be inert text, on the reasoning that the Mistakes navigation item already carries the
 * same count as a badge. That reasoning holds for the *count*, and not for the route: Progress
 * exists to answer "what should I work on next?", and the single most concrete answer it can give is
 * a queue of questions the learner has already got wrong. A line of text stating that queue's size
 * and then declining to open it is the dashboard stopping one step short of being useful.
 *
 * It remains one row rather than becoming a card, and it stays a route into Mistake Review rather
 * than starting anything: this count spans the whole curriculum, and focused practice has to name a
 * Topic or Subtopic. Choosing one — the first, the weakest, the one holding the most mistakes —
 * would be a recommendation made silently on the learner's behalf. Scoped mistake practice is
 * offered where a scope is actually known, on a queue entry in Mistake Review.
 *
 * The resolved state keeps no action: an empty queue has nothing to review, and a row that stayed
 * tappable when it led to an empty screen would be worse than a statement of fact. The whole row is
 * the target when it is one, with the chevron saying so, and the tap is announced from the row's own
 * label plus an explicit action label rather than from the icon.
 */
@Composable
private fun UnresolvedMistakeSummary(
    unresolvedCount: Int,
    onReviewMistakes: () -> Unit,
) {
    val semantic = AppThemeExtras.semanticColors
    val resolved = unresolvedCount == 0
    val reviewLabel = stringResource(Res.string.progress_review_mistakes_action)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (resolved) {
                    Modifier
                } else {
                    Modifier
                        .clickable(
                            onClickLabel = reviewLabel,
                            onClick = onReviewMistakes,
                        )
                        .testTag(ProgressReviewMistakesTag)
                },
            )
            // Inside the clickable, so the state layer spans the row rather than being inset from
            // it, and the row still clears the minimum touch target when it is a target.
            .heightIn(min = MinimumTouchTargetSize)
            .padding(vertical = AppSpacing.Tight),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Related),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (resolved) AppIcons.CheckCircle else AppIcons.Warning,
            contentDescription = null,
            tint = if (resolved) semantic.correct else semantic.incorrect,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = if (resolved) {
                stringResource(Res.string.mistake_review_none)
            } else {
                stringResource(Res.string.mistake_review_unresolved_count, unresolvedCount)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (!resolved) {
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * A weak area, still primarily a performance row.
 *
 * The row gains one low-emphasis text button rather than becoming a practice card or a click target
 * of its own: the section exists to report where the learner is struggling, and a whole card that
 * silently starts configuring practice would hide that meaning behind an unlabelled tap.
 *
 * The shortcut is offered because the row is here at all — the domain put it in the snapshot's weak
 * areas — so nothing about weakness is re-decided from the percentage this card displays.
 *
 * It carries no "Weak area" badge, unlike the same card elsewhere. Every row in this section is a
 * weak area and the section says so directly above them, so a badge on each one repeats the heading
 * once per card and adds nothing a learner did not already know from where they are looking. The
 * accent container and the accuracy colour still mark the row; ordering, the figure, and the counts
 * carry the rest. The badge stays where it is doing work — on a Topic card in a mixed list, where
 * nothing else states the verdict.
 */
@Composable
private fun WeakAreaCard(
    area: WeakAreaUiModel,
    onPracticeClick: () -> Unit,
) {
    val title = when (area.type) {
        WeakAreaType.TOPIC ->
            area.title ?: stringResource(Res.string.progress_topic_unavailable)
        WeakAreaType.SUBTOPIC ->
            area.title ?: stringResource(Res.string.progress_subtopic_unavailable)
    }
    val subtitle = when {
        area.type != WeakAreaType.SUBTOPIC -> null
        area.title == null -> area.subtitle
        else -> area.subtitle ?: stringResource(Res.string.progress_topic_unavailable)
    }
    ProgressPerformanceCard(
        title = title,
        subtitle = subtitle,
        correctCount = area.correctCount,
        answeredCount = area.answeredCount,
        percentage = area.percentage,
        isWeak = true,
        weakLabel = null,
        action = {
            TextButton(
                onClick = onPracticeClick,
                modifier = Modifier.testTag(progressWeakAreaPracticeTag(area)),
            ) {
                Text(text = stringResource(Res.string.practice_shortcut_weak_area))
            }
        },
    )
}

@Composable
private fun TopicPerformanceCard(
    topic: ProgressTopicUiModel,
    onClick: () -> Unit,
) {
    ProgressPerformanceCard(
        title = topic.topicName ?: stringResource(Res.string.progress_topic_unavailable),
        subtitle = null,
        correctCount = topic.correctCount,
        answeredCount = topic.answeredCount,
        percentage = topic.percentage,
        modifier = Modifier
            .testTag(progressTopicCardTag(topic.topicId))
            .clickable(onClick = onClick),
        showChevron = true,
    )
}

@Composable
private fun HistoryCard(
    attempt: CompletedAttemptUiModel,
    onClick: () -> Unit,
) {
    PerformanceCard(
        title = when (attempt.assessmentType) {
            CompletedAssessmentType.MIXED -> stringResource(Res.string.mixed_interview_title)
            CompletedAssessmentType.FOCUSED -> stringResource(Res.string.progress_focused_practice)
        },
        detail = stringResource(
            Res.string.progress_score,
            attempt.correctAnswers,
            attempt.totalQuestions,
        ),
        percentage = attempt.percentage,
        modifier = Modifier
            .testTag(progressHistoryCardTag(attempt.attemptId))
            .clickable(onClick = onClick),
        subtitle = focusedScopeLabel(attempt.focusedScope),
        // Formatted here, where the reader's zone and their idea of "today" are available; the state
        // carries the instant itself. See ui/time/TimestampText.kt.
        caption = timestampText(attempt.completedAt),
        showChevron = true,
    )
}

@Composable
private fun focusedScopeLabel(scope: FocusedScopeUiModel?): String? =
    when (scope) {
        null -> null
        is FocusedScopeUiModel.Topic ->
            scope.topicName ?: stringResource(Res.string.progress_topic_unavailable)
        is FocusedScopeUiModel.Subtopic -> {
            val subtopicName = scope.subtopicName
                ?: return stringResource(Res.string.progress_subtopic_unavailable)
            stringResource(
                Res.string.progress_focused_subtopic_scope,
                scope.topicName ?: stringResource(Res.string.progress_topic_unavailable),
                subtopicName,
            )
        }
    }
