package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun TopicDetailDestination(
    topicId: String,
    targetSubtopicId: String? = null,
    onBack: () -> Unit,
    onConfigurePractice: (AssessmentScope) -> Unit,
    viewModel: TopicDetailViewModel = koinViewModel { parametersOf(topicId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TopicDetailScreen(
        state = state,
        targetSubtopicId = targetSubtopicId,
        onBack = onBack,
        onStartTopicPractice = {
            viewModel.topicPracticeScope()?.let(onConfigurePractice)
        },
        onStartSubtopicPractice = { subtopicId ->
            viewModel.subtopicPracticeScope(subtopicId)?.let(onConfigurePractice)
        },
        onRetry = viewModel::retry,
    )
}
