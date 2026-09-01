package org.artkachenko.kmp_learning_app.mistake_review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.mistake_review_description
import kmp_learning_app.shared.generated.resources.mistake_review_empty
import kmp_learning_app.shared.generated.resources.mistake_review_empty_action
import kmp_learning_app.shared.generated.resources.mistake_review_empty_detail
import kmp_learning_app.shared.generated.resources.mistake_review_error
import kmp_learning_app.shared.generated.resources.mistake_review_loading
import kmp_learning_app.shared.generated.resources.mistake_review_title
import org.artkachenko.kmp_learning_app.assessment_review.MissingReviewQuestion
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionCard
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.ScreenAction
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.jetbrains.compose.resources.stringResource

internal const val MistakeReviewLoadingTag = "mistake_review_loading"

@Composable
internal fun MistakeReviewScreen(
    state: MistakeReviewUiState,
    onBack: (() -> Unit)? = null,
    onRetry: () -> Unit,
    onBrowseTopics: () -> Unit,
    onSourceClick: (String) -> Unit,
    failedSourceUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        AppTopBar(stringResource(Res.string.mistake_review_title), onBack)
        when (state) {
            MistakeReviewUiState.Loading -> ScreenLoading(
                message = stringResource(Res.string.mistake_review_loading),
                testTag = MistakeReviewLoadingTag,
                modifier = Modifier.weight(1f),
            )
            MistakeReviewUiState.Empty -> ScreenAction(
                message = stringResource(Res.string.mistake_review_empty),
                actionLabel = stringResource(Res.string.mistake_review_empty_action),
                onAction = onBrowseTopics,
                modifier = Modifier.weight(1f),
                detail = stringResource(Res.string.mistake_review_empty_detail),
                icon = AppIcons.CheckCircle,
                iconTint = AppThemeExtras.semanticColors.correct,
            )
            MistakeReviewUiState.Error -> ScreenError(
                message = stringResource(Res.string.mistake_review_error),
                onRetry = onRetry,
                modifier = Modifier.weight(1f),
            )
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
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(Res.string.mistake_review_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
