package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.mixed_interview_completed
import kmp_learning_app.shared.generated.resources.mixed_interview_title
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicStudyTopAppBar
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MixedInterviewCompletedDestination(
    attemptId: String,
    onBack: () -> Unit,
) {
    require(attemptId.isNotBlank()) {
        "attemptId must not be blank."
    }
    MixedInterviewCompletedScreen(onBack = onBack)
}

@Composable
internal fun MixedInterviewCompletedScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopicStudyTopAppBar(
            title = stringResource(Res.string.mixed_interview_title),
            onBack = onBack,
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.mixed_interview_completed),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}
