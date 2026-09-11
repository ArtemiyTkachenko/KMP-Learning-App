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
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.interview_history_attempts
import kmp_learning_app.shared.generated.resources.interview_history_best
import kmp_learning_app.shared.generated.resources.interview_history_empty_detail
import kmp_learning_app.shared.generated.resources.interview_history_empty_title
import kmp_learning_app.shared.generated.resources.interview_history_latest
import kmp_learning_app.shared.generated.resources.interview_history_score
import kmp_learning_app.shared.generated.resources.interview_history_title
import kmp_learning_app.shared.generated.resources.mixed_interview_description
import kmp_learning_app.shared.generated.resources.mixed_interview_how_it_works
import kmp_learning_app.shared.generated.resources.mixed_interview_question_count
import kmp_learning_app.shared.generated.resources.mixed_interview_review_note
import kmp_learning_app.shared.generated.resources.mixed_interview_start
import kmp_learning_app.shared.generated.resources.mixed_interview_title
import org.artkachenko.kmp_learning_app.ui.MetricFigure
import org.artkachenko.kmp_learning_app.ui.PerformanceCard
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.artkachenko.kmp_learning_app.ui.time.timestampText
import org.jetbrains.compose.resources.stringResource

internal const val InterviewStartButtonTag = "interview_start"

internal const val InterviewRecordTag = "interview_record"

/** The deliberate first-visit state, so a test can tell it from an absent record. */
internal const val InterviewNoHistoryTag = "interview_no_history"
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
        contentPadding = appScreenContentPadding(top = AppSpacing.Section, bottom = AppSpacing.Section),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = stringResource(Res.string.mixed_interview_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
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
                    MetricFigure(
                        text = stringResource(
                            Res.string.mixed_interview_question_count,
                            MixedInterviewDefaults.QuestionCount,
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(Res.string.mixed_interview_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    // The one rule that makes an Interview different from Practice, stated before
                    // the learner commits to twenty questions rather than discovered on question
                    // one. Practice marks each answer as it is given; an Interview holds every
                    // verdict back until it is over, which is exactly the asymmetry this screen
                    // exists to make deliberate.
                    Text(
                        text = stringResource(Res.string.mixed_interview_review_note),
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
            InterviewHistoryUiState.Empty -> item {
                FirstInterviewNote()
            }
            is InterviewHistoryUiState.Content -> item {
                InterviewRecord(history = history.history, onOpenResult = onOpenResult)
            }
        }
    }
}

/**
 * The first-visit state, said deliberately rather than left as a gap.
 *
 * A learner with no interviews used to reach a screen that simply stopped after the explanation,
 * with the space where the record will be showing nothing at all — which reads as something that
 * failed to load rather than as something they have not done yet. This states what will appear here
 * and why it is worth coming back to, in the quietest treatment on the page: the invitation to start
 * is the filled button above, and a second call to action down here would compete with it.
 *
 * It deliberately shows no empty table, no zeroed score, and no placeholder row — a 0 of 20 would be
 * a result the learner never got.
 */
@Composable
private fun FirstInterviewNote() {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(InterviewNoHistoryTag),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
    ) {
        Text(
            text = stringResource(Res.string.interview_history_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(Res.string.interview_history_empty_detail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The learner's own results, shown only once there are some.
 *
 * The most recent interview leads, and carries its date. Recency is what makes the record useful —
 * "how did I do last time, and how long ago was that?" is the question a returning learner has — and
 * a record without a date cannot answer the second half of it.
 *
 * Best is kept, and kept second. It is genuine information and the data model already supports it,
 * but it is not what this screen is for: a personal best promoted above the latest result turns a
 * preparation tool into a high-score table, and the app has no leaderboard, streak, or points
 * anywhere else. Repeating one attempt under both headings would be noise, so the best row appears
 * only once it is a different interview from the latest one.
 */
@Composable
private fun InterviewRecord(
    history: InterviewHistoryUiModel,
    onOpenResult: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(InterviewRecordTag),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Related),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.interview_history_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
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
            showsDate = true,
            onOpenResult = onOpenResult,
        )
        if (history.best.attemptId != history.latest.attemptId) {
            InterviewRecordRow(
                title = stringResource(Res.string.interview_history_best),
                attempt = history.best,
                // The best result's own date is not the point of the row — it is the score that
                // earns it a place — and printing two dates invites reading the pair as a timeline.
                showsDate = false,
                onOpenResult = onOpenResult,
            )
        }
    }
}

@Composable
private fun InterviewRecordRow(
    title: String,
    attempt: InterviewAttemptUiModel,
    showsDate: Boolean,
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
        caption = if (showsDate) timestampText(attempt.completedAt) else null,
        showChevron = true,
        modifier = Modifier.clickable { onOpenResult(attempt.attemptId) },
    )
}
