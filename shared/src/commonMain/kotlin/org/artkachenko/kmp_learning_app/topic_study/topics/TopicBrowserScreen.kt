package org.artkachenko.kmp_learning_app.topic_study.topics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.continue_studying_source_mistakes
import kmp_learning_app.shared.generated.resources.continue_studying_source_unseen
import kmp_learning_app.shared.generated.resources.continue_studying_source_weak_areas
import kmp_learning_app.shared.generated.resources.continue_studying_title
import kmp_learning_app.shared.generated.resources.learning_context_accuracy
import kmp_learning_app.shared.generated.resources.learning_context_explored
import kmp_learning_app.shared.generated.resources.learning_context_not_studied
import kmp_learning_app.shared.generated.resources.progress_weak_label
import kmp_learning_app.shared.generated.resources.topic_browser_empty
import kmp_learning_app.shared.generated.resources.topic_browser_error
import kmp_learning_app.shared.generated.resources.topic_browser_loading
import kmp_learning_app.shared.generated.resources.topic_browser_clear_search
import kmp_learning_app.shared.generated.resources.topic_browser_search_label
import kmp_learning_app.shared.generated.resources.topic_browser_search_no_results
import kmp_learning_app.shared.generated.resources.topic_browser_search_subtopics
import kmp_learning_app.shared.generated.resources.topic_browser_search_topics
import kmp_learning_app.shared.generated.resources.topic_browser_subtitle
import kmp_learning_app.shared.generated.resources.topic_browser_title
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingContext
import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingTarget
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.LearningContextUiModel
import org.artkachenko.kmp_learning_app.ui.SectionHeading
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.StatusBadge
import org.artkachenko.kmp_learning_app.ui.TopicVisualMarker
import org.artkachenko.kmp_learning_app.ui.accuracyColor
import org.artkachenko.kmp_learning_app.ui.formatAccuracy
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppListBottomPadding
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppContentMargin
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.jetbrains.compose.resources.stringResource

internal const val TopicBrowserLoadingTag = "topic_browser_loading"
internal const val TopicBrowserHeaderTag = "topic_browser_header"
internal const val TopicBrowserViewportTag = "topic_browser_viewport"
internal const val TopicBrowserSearchFieldTag = "topic_browser_search_field"
internal const val TopicBrowserContinueStudyingTag = "topic_browser_continue_studying"

/**
 * Space between the top safe area and the heading.
 *
 * Design spacing only: the status-bar allowance is the window inset applied above it. The two are
 * deliberately separate and additive. This was 24.dp back when the screen applied no inset and the
 * heading ran under the status bar, so the value stood in for both; once the inset was added the
 * old margin stacked on top of a safe area that is 54.dp on a current phone, leaving the heading
 * most of an inch down the screen.
 */
private val TopicBrowserHeaderSpacing = 12.dp

@Composable
internal fun TopicBrowserScreen(
    state: TopicBrowserUiState,
    onTopicClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    topWindowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
    onSubtopicClick: (topicId: String, subtopicId: String) -> Unit = { _, _ -> },
    onSearchQueryChange: (String) -> Unit = {},
    onContinueStudyingClick: (ContinueStudyingTarget) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // This screen carries its own heading instead of an AppTopBar, so it owns the top safe
            // area; the shell leaves that inset unconsumed for exactly this reason. The bottom is
            // not ours: the shell's Scaffold already ends this content at the top of the navigation
            // bar, so bottom padding out here would show as a strip of background above it. Any
            // scroll-end spacing belongs inside the list, as contentPadding.
            .windowInsetsPadding(topWindowInsets)
            .padding(horizontal = LocalAppContentMargin.current)
            .padding(top = TopicBrowserHeaderSpacing),
    ) {
        Text(
            text = stringResource(Res.string.topic_browser_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(TopicBrowserHeaderTag),
        )
        Text(
            text = stringResource(Res.string.topic_browser_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (state is TopicBrowserUiState.Content) {
            TopicSearchField(
                query = state.query,
                onQueryChange = onSearchQueryChange,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Box(modifier = Modifier.weight(1f).testTag(TopicBrowserViewportTag)) {
            when (state) {
                TopicBrowserUiState.Loading -> ScreenLoading(
                    message = stringResource(Res.string.topic_browser_loading),
                    testTag = TopicBrowserLoadingTag,
                )
                is TopicBrowserUiState.Content -> when {
                    state.query.isBlank() -> TopicList(
                        topics = state.topics,
                        onTopicClick = onTopicClick,
                        // Absent unless the state carries one, which it never does while a query
                        // is active: the card belongs to browsing, not to search results.
                        continueStudying = state.continueStudying,
                        onContinueStudyingClick = onContinueStudyingClick,
                    )
                    state.topicMatches.isEmpty() && state.subtopicMatches.isEmpty() -> {
                        ScreenMessage(
                            message = stringResource(
                                Res.string.topic_browser_search_no_results,
                                state.query.trim(),
                            ),
                        )
                    }
                    else -> TopicSearchResults(
                        topicMatches = state.topicMatches,
                        subtopicMatches = state.subtopicMatches,
                        onTopicClick = onTopicClick,
                        onSubtopicClick = onSubtopicClick,
                    )
                }
                TopicBrowserUiState.Empty -> ScreenMessage(
                    message = stringResource(Res.string.topic_browser_empty),
                )
                TopicBrowserUiState.Error -> ScreenError(
                    message = stringResource(Res.string.topic_browser_error),
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun TopicSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().testTag(TopicBrowserSearchFieldTag),
        label = { Text(stringResource(Res.string.topic_browser_search_label)) },
        leadingIcon = {
            Icon(
                imageVector = AppIcons.Search,
                contentDescription = null,
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = stringResource(Res.string.topic_browser_clear_search),
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
    )
}

@Composable
private fun TopicList(
    topics: List<TopicBrowserItemUiModel>,
    onTopicClick: (String) -> Unit,
    continueStudying: ContinueStudyingContext?,
    onContinueStudyingClick: (ContinueStudyingTarget) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = AppListBottomPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Inside the list rather than pinned above it: a shortcut is worth one glance on arrival,
        // and scrolls away for a learner who came to browse the catalogue instead.
        continueStudying?.let { context ->
            item(key = "continue_studying") {
                ContinueStudyingCard(
                    context = context,
                    onClick = onContinueStudyingClick,
                )
            }
        }
        items(
            items = topics,
            key = { it.topicId },
        ) { topic ->
            TopicRow(
                topic = topic,
                onTopicClick = onTopicClick,
            )
        }
    }
}

/**
 * The way back into what the learner was last working on.
 *
 * One card and one tap, deliberately: it names the context and goes there. It carries no score, no
 * coverage figure, and no explanation of why it is being offered — the reason is simply that this
 * is where they were, and a second competing action would turn the top of Topics into a dashboard.
 * Whether the destination is Topic detail or a practice setup is the card's supporting line, not a
 * choice presented here.
 */
@Composable
private fun ContinueStudyingCard(
    context: ContinueStudyingContext,
    onClick: (ContinueStudyingTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = { onClick(context.target) },
        modifier = modifier.fillMaxWidth().testTag(TopicBrowserContinueStudyingTag),
        shape = MaterialTheme.shapes.medium,
        // A quiet container rather than the primaryContainer the Interview hero uses: this is a
        // shortcut sitting above the Topic cards, and it should read as one of them with emphasis.
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.Comfortable),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
            ) {
                Text(
                    text = stringResource(Res.string.continue_studying_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    // Resolved from the current curriculum, so a renamed Topic is named correctly
                    // here without anything stored in history being migrated.
                    text = context.scopeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                context.supportingLabel()?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * The single supporting line: which Topic a Subtopic belongs to, or which practice intent is being
 * returned to.
 *
 * The domain hands over a typed source rather than a stored sentence, so the copy is written and
 * localized here. `ALL` has no label because it never reaches this card — an untargeted run returns
 * to content, where the parent Topic is the useful line instead.
 */
@Composable
private fun ContinueStudyingContext.supportingLabel(): String? =
    when (val target = target) {
        is ContinueStudyingTarget.Topic -> parentTopicName
        is ContinueStudyingTarget.Practice -> when (target.preset.source) {
            PracticeQuestionSource.ALL -> null
            PracticeQuestionSource.UNSEEN -> Res.string.continue_studying_source_unseen
            PracticeQuestionSource.WEAK_AREAS -> Res.string.continue_studying_source_weak_areas
            PracticeQuestionSource.UNRESOLVED_MISTAKES ->
                Res.string.continue_studying_source_mistakes
        }?.let { stringResource(it) }
    }

/**
 * One Topic card, in the order a learner reads it: what the Topic is, how much of it they have
 * explored, and — trailing, where a column of figures forms — how accurately they have answered it.
 *
 * Restraint is the point. The card carries no chart, no recent performance, and no recommendation:
 * a learner scanning seventeen Topics on a phone needs each one to stay two or three short lines,
 * and long Topic names here run to two lines on their own.
 */
@Composable
private fun TopicRow(
    topic: TopicBrowserItemUiModel,
    onTopicClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = { onTopicClick(topic.topicId) },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.Comfortable),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopicVisualMarker(topicId = topic.topicId)
            Column(
                // Long Topic names wrap rather than push the marker out of the card.
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = topic.topicName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                topic.learningContext?.let { TopicLearningContext(it) }
            }
            // Absent for an unseen Topic rather than showing 0%: never answered is not the same
            // statement as answered and got none right.
            topic.learningContext?.accuracyPercentage?.let { accuracy ->
                TopicAccuracy(accuracy)
            }
        }
    }
}

/**
 * The supporting lines under a Topic name.
 *
 * Coverage is a count rather than a bare percentage, because "12 of 28 explored" says what the
 * figure measures and "43%" beside an accuracy percentage does not. It stays in the neutral
 * variant colour: a learner at 10% coverage has not done anything wrong, they simply have most of
 * the bank still ahead of them.
 */
@Composable
private fun TopicLearningContext(context: LearningContextUiModel) {
    if (context.hasCoverageScope) {
        Text(
            text = stringResource(
                Res.string.learning_context_explored,
                context.attemptedQuestionCount,
                context.totalQuestionCount,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (context.isUnstudied) {
        Text(
            text = stringResource(Res.string.learning_context_not_studied),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    // Only the domain's verdict raises this badge. A Topic can read as low accuracy without being
    // weak, when too few answers have been recorded to meet the policy's evidence threshold.
    if (context.isWeak) {
        StatusBadge(
            text = stringResource(Res.string.progress_weak_label),
            contentColor = AppThemeExtras.semanticColors.onPartiallyCorrectContainer,
            containerColor = AppThemeExtras.semanticColors.partiallyCorrectContainer,
            icon = AppIcons.Warning,
        )
    }
}

/** Labelled so the figure cannot be mistaken for the coverage count beside it. */
@Composable
private fun TopicAccuracy(accuracy: Double) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = formatAccuracy(accuracy),
            style = MaterialTheme.typography.titleMedium,
            color = accuracyColor(accuracy),
        )
        Text(
            text = stringResource(Res.string.learning_context_accuracy),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TopicSearchResults(
    topicMatches: List<TopicBrowserItemUiModel>,
    subtopicMatches: List<SubtopicSearchResult>,
    onTopicClick: (String) -> Unit,
    onSubtopicClick: (topicId: String, subtopicId: String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = AppListBottomPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (topicMatches.isNotEmpty()) {
            item(key = "topic_results_heading") {
                SectionHeading(stringResource(Res.string.topic_browser_search_topics))
            }
            items(
                items = topicMatches,
                key = { "topic:${it.topicId}" },
            ) { result ->
                // The same row as normal browsing: a Topic match is the same Topic, so it keeps its
                // marker and its learning context rather than becoming a second kind of card.
                //
                // Results are re-filtered on every keystroke, so rows appear, disappear, and move
                // constantly while a query is being typed. Without `animateItem` the list teleports
                // between arrangements and it is impossible to see whether a row left or simply
                // shifted — the animation carries the filtering, which is what makes it functional
                // rather than decorative.
                TopicRow(
                    topic = result,
                    onTopicClick = onTopicClick,
                    modifier = Modifier.animateItem(),
                )
            }
        }
        if (subtopicMatches.isNotEmpty()) {
            item(key = "subtopic_results_heading") {
                SectionHeading(stringResource(Res.string.topic_browser_search_subtopics))
            }
            items(
                items = subtopicMatches,
                key = { "subtopic:${it.subtopicId}" },
            ) { result ->
                SubtopicResultRow(
                    result = result,
                    onClick = onSubtopicClick,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun SubtopicResultRow(
    result: SubtopicSearchResult,
    onClick: (topicId: String, subtopicId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = { onClick(result.parentTopicId, result.subtopicId) },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The parent Topic's marker, resolved from its stable ID rather than its display name,
            // so a Subtopic hit carries the same context cue as the Topic itself.
            TopicVisualMarker(topicId = result.parentTopicId)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = result.subtopicName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = result.parentTopicName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Preview
@Composable
private fun TopicBrowserScreenPreview() {
    AppTheme {
        TopicBrowserScreen(
            state = TopicBrowserUiState.Content(
                // Real curriculum IDs so the preview shows the authored markers, and the three
                // learning states the card has to keep distinguishable.
                topics = listOf(
                    TopicBrowserItemUiModel(
                        topicId = "android_platform",
                        topicName = "Android Platform & Application Model",
                        learningContext = LearningContextUiModel(
                            attemptedQuestionCount = 12,
                            totalQuestionCount = 28,
                            coveragePercentage = 12.0 / 28 * 100,
                            accuracyPercentage = 76.0,
                            isWeak = false,
                        ),
                    ),
                    TopicBrowserItemUiModel(
                        topicId = "android_ui",
                        topicName = "UI — Views & Jetpack Compose",
                        learningContext = LearningContextUiModel(
                            attemptedQuestionCount = 0,
                            totalQuestionCount = 22,
                            coveragePercentage = 0.0,
                            accuracyPercentage = null,
                            isWeak = false,
                        ),
                    ),
                    TopicBrowserItemUiModel(
                        topicId = "architecture",
                        topicName = "Application Architecture & Design Principles",
                        learningContext = LearningContextUiModel(
                            attemptedQuestionCount = 6,
                            totalQuestionCount = 19,
                            coveragePercentage = 6.0 / 19 * 100,
                            accuracyPercentage = 41.0,
                            isWeak = true,
                        ),
                    ),
                ),
            ),
            onTopicClick = {},
            onSubtopicClick = { _, _ -> },
            onSearchQueryChange = {},
            onRetry = {},
        )
    }
}
