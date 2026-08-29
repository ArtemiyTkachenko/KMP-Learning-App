package org.artkachenko.kmp_learning_app.mistake_review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.mistake_review_description
import kmp_learning_app.shared.generated.resources.mistake_review_empty
import kmp_learning_app.shared.generated.resources.mistake_review_empty_detail
import kmp_learning_app.shared.generated.resources.mistake_review_error
import kmp_learning_app.shared.generated.resources.mistake_review_loading
import kmp_learning_app.shared.generated.resources.mistake_review_retry
import kmp_learning_app.shared.generated.resources.mistake_review_title
import org.artkachenko.kmp_learning_app.assessment_review.MissingReviewQuestion
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionCard
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicStudyTopAppBar
import org.jetbrains.compose.resources.stringResource

internal const val MistakeReviewLoadingTag = "mistake_review_loading"

@Composable
internal fun MistakeReviewScreen(
    state: MistakeReviewUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSourceClick: (String) -> Unit,
    failedSourceUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        TopicStudyTopAppBar(stringResource(Res.string.mistake_review_title), onBack)
        when (state) {
            MistakeReviewUiState.Loading -> MistakeReviewMessage(Modifier.weight(1f)) {
                CircularProgressIndicator(Modifier.testTag(MistakeReviewLoadingTag))
                Text(stringResource(Res.string.mistake_review_loading))
            }
            MistakeReviewUiState.Empty -> MistakeReviewMessage(Modifier.weight(1f)) {
                Text(stringResource(Res.string.mistake_review_empty))
                Text(
                    text = stringResource(Res.string.mistake_review_empty_detail),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            MistakeReviewUiState.Error -> MistakeReviewMessage(Modifier.weight(1f)) {
                Text(stringResource(Res.string.mistake_review_error))
                Button(onClick = onRetry) {
                    Text(stringResource(Res.string.mistake_review_retry))
                }
            }
            is MistakeReviewUiState.Content -> MistakeReviewContent(
                state = state,
                onSourceClick = onSourceClick,
                failedSourceUrl = failedSourceUrl,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MistakeReviewContent(
    state: MistakeReviewUiState.Content,
    onSourceClick: (String) -> Unit,
    failedSourceUrl: String?,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(Res.string.mistake_review_description),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        // Review rendering is reused from the shared assessment-review components so selected
        // answers, correct answers, explanation, and sources stay consistent with result screens.
        items(state.mistakes, key = UnresolvedMistake::questionId) { mistake ->
            when (val item = mistake.reviewItem) {
                is ReviewQuestionItem.Available -> ReviewQuestionCard(
                    question = item.question,
                    onSourceClick = onSourceClick,
                    failedSourceUrl = failedSourceUrl,
                )
                is ReviewQuestionItem.Missing -> MissingReviewQuestion(item.questionId)
            }
        }
    }
}

@Composable
private fun MistakeReviewMessage(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}
