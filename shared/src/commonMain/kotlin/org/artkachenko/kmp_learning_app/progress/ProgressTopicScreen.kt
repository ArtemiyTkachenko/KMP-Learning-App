package org.artkachenko.kmp_learning_app.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.progress_subtopic_unavailable
import kmp_learning_app.shared.generated.resources.progress_topic_detail_title
import kmp_learning_app.shared.generated.resources.progress_topic_empty
import kmp_learning_app.shared.generated.resources.progress_topic_error
import kmp_learning_app.shared.generated.resources.progress_topic_coverage
import kmp_learning_app.shared.generated.resources.progress_topic_loading
import kmp_learning_app.shared.generated.resources.progress_topic_subtopics
import kmp_learning_app.shared.generated.resources.progress_topic_unavailable
import kmp_learning_app.shared.generated.resources.progress_not_enough_data
import kmp_learning_app.shared.generated.resources.progress_score
import kmp_learning_app.shared.generated.resources.progress_weak_label
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressPolicy
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.PaddingValues
import org.artkachenko.kmp_learning_app.ui.AccuracyHeroCard
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.StatusBadge
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.theme.AppContentWidth
import org.artkachenko.kmp_learning_app.ui.theme.AppScreenPane
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing

internal const val ProgressTopicLoadingTag = "progress_topic_loading"

@Composable
internal fun ProgressTopicScreen(
    state: ProgressTopicUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = rememberAppTopBarScrollBehavior()
    Column(modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        // The topic name is the aggregate card's title, so the bar keeps a stable label rather
        // than repeating it.
        AppTopBar(stringResource(Res.string.progress_topic_detail_title), onBack, scrollBehavior)
        AppScreenPane(AppContentWidth.Standard) {
            when (state) {
                ProgressTopicUiState.Loading -> ScreenLoading(
                    message = stringResource(Res.string.progress_topic_loading),
                    testTag = ProgressTopicLoadingTag,
                    modifier = Modifier.weight(1f),
                )
                ProgressTopicUiState.Empty -> ScreenMessage(
                    message = stringResource(Res.string.progress_topic_empty),
                    modifier = Modifier.weight(1f),
                )
                ProgressTopicUiState.Error -> ScreenError(
                    message = stringResource(Res.string.progress_topic_error),
                    onRetry = onRetry,
                    modifier = Modifier.weight(1f),
                )
                is ProgressTopicUiState.Content -> ProgressTopicContent(
                    state = state,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ProgressTopicContent(
    state: ProgressTopicUiState.Content,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = appScreenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Comfortable),
    ) {
        item {
            TopicAggregateHero(state)
        }
        // Only Subtopics with completed observations reach the snapshot, so an observed Topic can
        // still have no Subtopic rows. Omit the heading rather than leaving it dangling.
        if (state.subtopics.isNotEmpty()) {
            item {
                ProgressSectionTitle(stringResource(Res.string.progress_topic_subtopics))
            }
            items(state.subtopics, key = ProgressSubtopicUiModel::subtopicId) { subtopic ->
                ProgressPerformanceCard(
                    title = subtopic.subtopicName
                        ?: stringResource(Res.string.progress_subtopic_unavailable),
                    subtitle = evidenceLabel(subtopic.answeredCount),
                    correctCount = subtopic.correctCount,
                    answeredCount = subtopic.answeredCount,
                    percentage = subtopic.percentage.takeIf {
                        subtopic.answeredCount >= LearningProgressPolicy.WeakAreaMinimumAnswered
                    },
                    caption = coverageCaption(subtopic.coverage),
                    isWeak = subtopic.isWeak &&
                        subtopic.answeredCount >= LearningProgressPolicy.WeakAreaMinimumAnswered,
                    // This is the one list in the app whose rows exist to be read against each
                    // other, so it is the one list whose rows get a meter. See [PerformanceCard].
                    comparesWithSiblings = true,
                )
            }
        }
    }
}

/**
 * The Topic this screen is about, as its hero.
 *
 * It was a `ProgressPerformanceCard(isSummary = true)`: the same row shape as the four Subtopic
 * cards under it, one container step up and one corner radius larger. On screen that is not a
 * hierarchy — the screen's subject and its parts read as five cards of the same kind, and a learner
 * arriving from the dashboard had nothing telling them which figure was the Topic's.
 *
 * The evidence rule is unchanged and is the reason [AccuracyHeroCard] takes a nullable percentage:
 * below `WeakAreaMinimumAnswered` there is no honest figure, so the hero states the counts and lets
 * the "not enough data" line explain itself rather than drawing a ring at a number the learner
 * never produced. Weakness is likewise still the domain's verdict, and is still suppressed below
 * the same minimum.
 *
 * Coverage stays a caption rather than becoming a second figure: it counts current Questions once
 * each while the line above it is all-time and occurrence-based, and two display figures with
 * different denominators on one card is how a learner comes to believe they are the same number.
 */
@Composable
private fun TopicAggregateHero(state: ProgressTopicUiState.Content) {
    val hasEvidence = state.answeredCount >= LearningProgressPolicy.WeakAreaMinimumAnswered
    AccuracyHeroCard(
        percentage = state.percentage.takeIf { hasEvidence },
        caption = stringResource(
            Res.string.progress_score,
            state.correctCount,
            state.answeredCount,
        ),
        title = state.topicName ?: stringResource(Res.string.progress_topic_unavailable),
    ) {
        // `evidenceLabel` and the badge are mutually exclusive by construction — one needs the
        // evidence minimum and the other needs it missing — so at most one verdict line appears.
        val verdict = evidenceLabel(state.answeredCount)
        val coverage = coverageCaption(state.coverage)
        val isWeak = state.isWeak && hasEvidence
        if (verdict == null && coverage == null && !isWeak) return@AccuracyHeroCard

        // One supporting block at its own tighter spacing. Emitted as siblings these three each
        // took the hero's 16dp gap, which left a badge floating at the bottom of the card looking
        // like something the coverage line had nothing to do with.
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Related)) {
            if (isWeak) {
                StatusBadge(
                    text = stringResource(Res.string.progress_weak_label),
                    contentColor = AppThemeExtras.semanticColors.onPartiallyCorrectContainer,
                    containerColor = AppThemeExtras.semanticColors.partiallyCorrectContainer,
                    icon = AppIcons.Warning,
                )
            }
            verdict?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            coverage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun evidenceLabel(answeredCount: Int): String? =
    if (answeredCount < LearningProgressPolicy.WeakAreaMinimumAnswered) {
        stringResource(Res.string.progress_not_enough_data)
    } else {
        null
    }

/**
 * Wording that names the denominator, because this row already carries a second fraction: "5 / 8
 * correct" is all-time and occurrence-based, while this one counts current Questions once each.
 */
@Composable
private fun coverageCaption(coverage: ProgressCoverageUiModel?): String? =
    coverage?.let {
        stringResource(
            Res.string.progress_topic_coverage,
            it.attemptedQuestionCount,
            it.totalQuestionCount,
        )
    }
