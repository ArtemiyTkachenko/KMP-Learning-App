package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.topic_browser_error
import kmp_learning_app.shared.generated.resources.topic_detail_accuracy_caption
import kmp_learning_app.shared.generated.resources.topic_detail_available_questions
import kmp_learning_app.shared.generated.resources.topic_detail_heading
import kmp_learning_app.shared.generated.resources.topic_detail_loading
import kmp_learning_app.shared.generated.resources.topic_detail_no_questions
import kmp_learning_app.shared.generated.resources.topic_detail_not_found
import kmp_learning_app.shared.generated.resources.topic_detail_start_practice
import kmp_learning_app.shared.generated.resources.topic_detail_subtopics
import org.artkachenko.kmp_learning_app.ui.AccuracyHeadline
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.PrimarySummaryCard
import org.artkachenko.kmp_learning_app.ui.SectionHeading
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.accuracyColor
import org.artkachenko.kmp_learning_app.ui.formatAccuracy
import org.jetbrains.compose.resources.stringResource

internal const val TopicDetailLoadingTag = "topic_detail_loading"
internal const val TopicPracticeButtonTag = "topic_practice_button"
internal const val SubtopicPracticeButtonTag = "subtopic_practice_button"

@Composable
internal fun TopicDetailScreen(
    state: TopicDetailUiState,
    targetSubtopicId: String? = null,
    onBack: () -> Unit,
    onStartTopicPractice: () -> Unit,
    onStartSubtopicPractice: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(
            title = when (state) {
                is TopicDetailUiState.Content -> state.topic.name
                is TopicDetailUiState.NoQuestions -> state.topic.name
                else -> stringResource(Res.string.topic_detail_heading)
            },
            onBack = onBack,
        )

        when (state) {
            TopicDetailUiState.Loading -> ScreenLoading(
                message = stringResource(Res.string.topic_detail_loading),
                testTag = TopicDetailLoadingTag,
                modifier = Modifier.weight(1f),
            )

            is TopicDetailUiState.Content -> {
                TopicContent(
                    state = state,
                    targetSubtopicId = targetSubtopicId,
                    onStartTopicPractice = onStartTopicPractice,
                    onStartSubtopicPractice = onStartSubtopicPractice,
                    modifier = Modifier.weight(1f),
                )
            }

            is TopicDetailUiState.NoQuestions -> ScreenMessage(
                message = stringResource(Res.string.topic_detail_no_questions),
                modifier = Modifier.weight(1f),
            )

            TopicDetailUiState.NotFound -> ScreenMessage(
                message = stringResource(Res.string.topic_detail_not_found),
                modifier = Modifier.weight(1f),
            )

            TopicDetailUiState.Error -> ScreenError(
                message = stringResource(Res.string.topic_browser_error),
                onRetry = onRetry,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TopicContent(
    state: TopicDetailUiState.Content,
    targetSubtopicId: String?,
    onStartTopicPractice: () -> Unit,
    onStartSubtopicPractice: (String) -> Unit,
    modifier: Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.subtopics, targetSubtopicId) {
        val subtopicIndex = state.subtopics.indexOfFirst {
            it.subtopic.id == targetSubtopicId
        }
        if (subtopicIndex >= 0) {
            // The first lazy-list item is the topic summary and action block.
            listState.scrollToItem(subtopicIndex + 1)
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.accuracyPercentage?.let { accuracy ->
                    PrimarySummaryCard {
                        AccuracyHeadline(
                            percentage = accuracy,
                            caption = stringResource(Res.string.topic_detail_accuracy_caption),
                        )
                    }
                }
                Text(
                    text = stringResource(
                        Res.string.topic_detail_available_questions,
                        state.topicQuestionCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // One primary action for the topic; subtopic rows below are the lower-emphasis
                // path, so the screen no longer shows several filled buttons of equal weight.
                Button(
                    onClick = onStartTopicPractice,
                    modifier = Modifier.fillMaxWidth().testTag(TopicPracticeButtonTag),
                ) {
                    Text(text = stringResource(Res.string.topic_detail_start_practice))
                }
                SectionHeading(
                    text = stringResource(Res.string.topic_detail_subtopics),
                )
            }
        }
        items(
            items = state.subtopics,
            key = { it.subtopic.id },
        ) { item ->
            // The row itself starts practice, so the per-row filled button is gone: it duplicated
            // the row's own click target and competed with the topic-level primary action.
            Card(
                onClick = { onStartSubtopicPractice(item.subtopic.id) },
                modifier = Modifier.fillMaxWidth().testTag(SubtopicPracticeButtonTag),
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.subtopic.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(
                                Res.string.topic_detail_available_questions,
                                item.questionCount,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    item.accuracyPercentage?.let { accuracy ->
                        Text(
                            text = formatAccuracy(accuracy),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = accuracyColor(accuracy),
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
    }
}
