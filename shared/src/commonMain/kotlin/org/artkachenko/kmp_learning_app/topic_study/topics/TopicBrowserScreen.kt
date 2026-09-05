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
import androidx.compose.material3.OutlinedCard
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
import kmp_learning_app.shared.generated.resources.recommended_next_mistakes_action
import kmp_learning_app.shared.generated.resources.recommended_next_mistakes_reason
import kmp_learning_app.shared.generated.resources.recommended_next_title
import kmp_learning_app.shared.generated.resources.recommended_next_topics_action
import kmp_learning_app.shared.generated.resources.recommended_next_topics_reason
import kmp_learning_app.shared.generated.resources.recommended_next_unseen_action
import kmp_learning_app.shared.generated.resources.recommended_next_unseen_action_generic
import kmp_learning_app.shared.generated.resources.recommended_next_unseen_reason
import kmp_learning_app.shared.generated.resources.recommended_next_weak_area_action
import kmp_learning_app.shared.generated.resources.recommended_next_weak_area_action_generic
import kmp_learning_app.shared.generated.resources.recommended_next_weak_area_reason
import kmp_learning_app.shared.generated.resources.recommended_next_weak_area_reason_generic
import kmp_learning_app.shared.generated.resources.saved_questions_entry_subtitle
import kmp_learning_app.shared.generated.resources.saved_questions_title
import kmp_learning_app.shared.generated.resources.topic_browser_empty
import kmp_learning_app.shared.generated.resources.topic_browser_error
import kmp_learning_app.shared.generated.resources.topic_browser_learning_units
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
import org.artkachenko.kmp_learning_app.guided_learning.LearningRecommendationRationale
import org.artkachenko.kmp_learning_app.guided_learning.LearningRecommendationTarget
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
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

internal const val TopicBrowserLoadingTag = "topic_browser_loading"
internal const val TopicBrowserHeaderTag = "topic_browser_header"
internal const val TopicBrowserViewportTag = "topic_browser_viewport"
internal const val TopicBrowserSearchFieldTag = "topic_browser_search_field"
internal const val TopicBrowserContinueStudyingTag = "topic_browser_continue_studying"
internal const val TopicBrowserRecommendedNextTag = "topic_browser_recommended_next"
internal const val TopicBrowserSavedQuestionsTag = "topic_browser_saved_questions"

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
    onRecommendedNextClick: (LearningRecommendationTarget) -> Unit = {},
    onSavedQuestionsClick: () -> Unit = {},
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
                        // Absent unless the state carries them, which it never does while a query
                        // is active: both cards belong to browsing, not to search results.
                        recommendedNext = state.recommendedNext,
                        onRecommendedNextClick = onRecommendedNextClick,
                        continueStudying = state.continueStudying,
                        onContinueStudyingClick = onContinueStudyingClick,
                        onSavedQuestionsClick = onSavedQuestionsClick,
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
    recommendedNext: RecommendedNextUiModel?,
    onRecommendedNextClick: (LearningRecommendationTarget) -> Unit,
    continueStudying: ContinueStudyingContext?,
    onContinueStudyingClick: (ContinueStudyingTarget) -> Unit,
    onSavedQuestionsClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = AppListBottomPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Inside the list rather than pinned above it: guidance is worth one glance on arrival, and
        // scrolls away for a learner who came to browse the catalogue instead. At most one of each,
        // and in priority order — what to do now, then the way back to what was being done.
        recommendedNext?.let { recommendation ->
            item(key = "recommended_next") {
                RecommendedNextCard(
                    recommendation = recommendation,
                    onClick = onRecommendedNextClick,
                )
            }
        }
        continueStudying?.let { context ->
            item(key = "continue_studying") {
                ContinueStudyingCard(
                    context = context,
                    onClick = onContinueStudyingClick,
                )
            }
        }
        // Below the guidance and above the catalogue, and always present: it is a way into content
        // the learner curated themselves, not a third thing the app is suggesting they do.
        item(key = "saved_questions") {
            SavedQuestionsEntry(onClick = onSavedQuestionsClick)
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
 * The way into the Questions the learner saved for themselves.
 *
 * Not a third guided-learning card, and drawn so that it cannot be read as one: the two cards above
 * it are filled with the primary and secondary containers because a policy chose them, while this is
 * an outlined utility row that says only where it goes. Nothing about it is derived — it carries no
 * count, no recommendation, and no reason, so it never has to read saved state to decide whether the
 * destination is worth offering. The empty state lives on the destination, which is exactly why the
 * entry must remain reachable when nothing has been saved yet.
 */
@Composable
private fun SavedQuestionsEntry(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().testTag(TopicBrowserSavedQuestionsTag),
        shape = MaterialTheme.shapes.medium,
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
                    text = stringResource(Res.string.saved_questions_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(Res.string.saved_questions_entry_subtitle),
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

/**
 * The way back into what the learner was last working on.
 *
 * One card and one tap, deliberately: it names the context and goes there. It carries no score, no
 * coverage figure, and no explanation of why it is being offered — the reason is simply that this
 * is where they were. Whether the destination is Topic detail or a practice setup is the card's
 * supporting line, not a choice presented here.
 *
 * It remains a compact continuity shortcut beneath the one policy-driven action above it: Recommended
 * Next is the primary guidance, this is the secondary way back, and there are deliberately still only
 * those two guided-learning cards. The Saved Questions entry below them is a learner-owned collection
 * rather than a third suggestion. Neither card turns the Topic rows below into recommendation cards.
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
 * The single most useful thing to do now, and the fact that makes it so.
 *
 * One card, one tap, and no alternatives: there is no ranking, no score, no second suggestion, no
 * "why?" affordance, and nothing to dismiss or configure. `LearningRecommendationPolicy` chose this
 * action from an ordered decision tree, and the card's job is to say what it chose and why — the
 * rationale is already typed, so nothing here infers a reason of its own.
 *
 * It uses the primary container while Continue Studying stays on the secondary one, which is the
 * whole visual statement being made: of the two guided cards, this is the one to act on, and the
 * other is the way back to where the learner was.
 */
@Composable
private fun RecommendedNextCard(
    recommendation: RecommendedNextUiModel,
    onClick: (LearningRecommendationTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = { onClick(recommendation.target) },
        modifier = modifier.fillMaxWidth().testTag(TopicBrowserRecommendedNextTag),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
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
                    text = stringResource(Res.string.recommended_next_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = recommendation.actionLabel(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    // Always present: a recommendation the learner cannot see a reason for is the
                    // one thing this feature is not allowed to be.
                    text = recommendation.rationaleLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * What the learner is being asked to do, chosen by the typed rationale alone.
 *
 * Switching on the rationale is presentation mapping, not a decision: the target was already picked,
 * and this only writes it in words. A weak area with no current display name and an unseen Topic the
 * catalogue no longer lists both degrade to neutral wording rather than hiding an action the policy
 * made on evidence that did not include a name.
 */
@Composable
private fun RecommendedNextUiModel.actionLabel(): String =
    when (val rationale = rationale) {
        LearningRecommendationRationale.NewUser ->
            stringResource(Res.string.recommended_next_topics_action)

        is LearningRecommendationRationale.UnresolvedMistakes ->
            stringResource(Res.string.recommended_next_mistakes_action)

        is LearningRecommendationRationale.WeakArea -> rationale.areaName
            ?.let { stringResource(Res.string.recommended_next_weak_area_action, it) }
            ?: stringResource(Res.string.recommended_next_weak_area_action_generic)

        is LearningRecommendationRationale.UnseenCoverage -> topicName
            ?.let { stringResource(Res.string.recommended_next_unseen_action, it) }
            ?: stringResource(Res.string.recommended_next_unseen_action_generic)
    }

/**
 * Why this action, in plain language.
 *
 * The counts come from the rationale exactly as the policy recorded them; nothing is recounted here,
 * and no percentage, score, or confidence is introduced that the domain never expressed.
 */
@Composable
private fun RecommendedNextUiModel.rationaleLabel(): String =
    when (val rationale = rationale) {
        LearningRecommendationRationale.NewUser ->
            stringResource(Res.string.recommended_next_topics_reason)

        is LearningRecommendationRationale.UnresolvedMistakes -> pluralStringResource(
            Res.plurals.recommended_next_mistakes_reason,
            rationale.count,
            rationale.count,
        )

        is LearningRecommendationRationale.WeakArea ->
            if (rationale.areaName == null) {
                stringResource(Res.string.recommended_next_weak_area_reason_generic)
            } else {
                stringResource(Res.string.recommended_next_weak_area_reason)
            }

        is LearningRecommendationRationale.UnseenCoverage -> pluralStringResource(
            Res.plurals.recommended_next_unseen_reason,
            rationale.unseenQuestionCount,
            rationale.unseenQuestionCount,
        )
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
                // Above the learner's own figures, and only when there is something to read: what
                // the Topic contains is a fact about the content, so it is stated before anything
                // about the person reading it.
                TopicLearningAvailability(topic.learningUnitCount)
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
 * Whether this Topic publishes explanatory material, and how much of it.
 *
 * Drawn as a neutral badge, deliberately not in the semantic colours the weak-area badge below it
 * uses: this states what exists to read, never how the learner is doing. There is no studied,
 * started, or completed fact behind it, and E21 has no learner-owned study progress to show.
 *
 * Silent for both of the non-positive cases, for different reasons. A `null` count is availability
 * nobody could read, so claiming anything would be a guess; a `0` count is a Topic with no authored
 * material, and "0 learning units" is noise on most rows of a seventeen-Topic list. Either way the
 * row stays an ordinary, clickable Topic.
 */
@Composable
private fun TopicLearningAvailability(learningUnitCount: Int?) {
    if (learningUnitCount == null || learningUnitCount <= 0) return
    StatusBadge(
        text = pluralStringResource(
            Res.plurals.topic_browser_learning_units,
            learningUnitCount,
            learningUnitCount,
        ),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
    )
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
                // learning states the card has to keep distinguishable. The availability counts
                // match the authored learning curriculum: only android_ui publishes a Unit today.
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
                        learningUnitCount = 0,
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
                        learningUnitCount = 1,
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
                        learningUnitCount = 0,
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
