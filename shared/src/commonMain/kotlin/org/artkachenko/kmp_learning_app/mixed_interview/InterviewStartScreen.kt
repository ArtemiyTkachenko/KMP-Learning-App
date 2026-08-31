package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.interview_history_attempts
import kmp_learning_app.shared.generated.resources.interview_history_best
import kmp_learning_app.shared.generated.resources.interview_history_latest
import kmp_learning_app.shared.generated.resources.interview_history_score
import kmp_learning_app.shared.generated.resources.interview_history_title
import kmp_learning_app.shared.generated.resources.mixed_interview_description
import kmp_learning_app.shared.generated.resources.mixed_interview_how_it_works
import kmp_learning_app.shared.generated.resources.mixed_interview_question_count
import kmp_learning_app.shared.generated.resources.mixed_interview_start
import kmp_learning_app.shared.generated.resources.mixed_interview_title
import org.artkachenko.kmp_learning_app.ui.PerformanceCard
import org.jetbrains.compose.resources.stringResource

internal const val InterviewStartButtonTag = "interview_start"

internal const val InterviewRecordTag = "interview_record"
internal const val InterviewHistoryLoadingTag = "interview_history_loading"

/**
 * The mixed interview's own destination.
 *
 * It used to be a card competing for room on the topic list. On its own screen the call to action
 * can lead, and there is space to say what the interview actually is before starting one.
 */
@Composable
internal fun InterviewStartScreen(
    onStartMixedInterview: () -> Unit,
    modifier: Modifier = Modifier,
    history: InterviewHistoryUiState = InterviewHistoryUiState.Loading,
    onOpenResult: (String) -> Unit = {},
) {
    // Scrollable rather than a fixed Column: with both a latest and a best result the heading,
    // invitation, explanation, and two record cards overflow a compact window or a large font
    // scale, and the lower cards were then unreachable.
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            // This screen leads with its own heading instead of an AppTopBar, so there is no bar
            // here to pad for the status bar; without this the heading sits underneath it.
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = stringResource(Res.string.mixed_interview_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(
                            Res.string.mixed_interview_question_count,
                            MixedInterviewDefaults.QuestionCount,
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(Res.string.mixed_interview_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Button(
                        onClick = onStartMixedInterview,
                        modifier = Modifier.fillMaxWidth().testTag(InterviewStartButtonTag),
                    ) {
                        Text(text = stringResource(Res.string.mixed_interview_start))
                    }
                }
            }
        }
        item {
            Text(
                text = stringResource(Res.string.mixed_interview_how_it_works),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Loading is distinct from "no record yet": rendering the empty shape while the read is in
        // flight is what made the card appear underneath the learner a moment after arriving.
        when (history) {
            InterviewHistoryUiState.Loading -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.testTag(InterviewHistoryLoadingTag))
                }
            }
            InterviewHistoryUiState.Empty -> Unit
            is InterviewHistoryUiState.Content -> item {
                InterviewRecord(history = history.history, onOpenResult = onOpenResult)
            }
        }
    }
}

/**
 * The learner's own results, shown only once there are some. On a first visit the screen stays a
 * plain invitation rather than an empty table; afterwards these two rows are the reason to return.
 */
@Composable
private fun InterviewRecord(
    history: InterviewHistoryUiModel,
    onOpenResult: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(InterviewRecordTag),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.interview_history_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(
                    Res.string.interview_history_attempts,
                    history.attemptCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        InterviewRecordRow(
            title = stringResource(Res.string.interview_history_latest),
            attempt = history.latest,
            onOpenResult = onOpenResult,
        )
        // Repeating one attempt under both headings would be noise, so the best row appears only
        // once it is a different interview from the latest one.
        if (history.best.attemptId != history.latest.attemptId) {
            InterviewRecordRow(
                title = stringResource(Res.string.interview_history_best),
                attempt = history.best,
                onOpenResult = onOpenResult,
            )
        }
    }
}

@Composable
private fun InterviewRecordRow(
    title: String,
    attempt: InterviewAttemptUiModel,
    onOpenResult: (String) -> Unit,
) {
    PerformanceCard(
        title = title,
        detail = stringResource(
            Res.string.interview_history_score,
            attempt.correctAnswers,
            attempt.totalQuestions,
        ),
        percentage = attempt.percentage,
        showChevron = true,
        modifier = Modifier.clickable { onOpenResult(attempt.attemptId) },
    )
}
