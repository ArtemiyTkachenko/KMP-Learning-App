package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * [onConfigurePractice] is ordinary practice for a scope, which the builder opens on its `ALL`
 * default. [onConfigureTargetedPractice] carries a scope the screen is already displaying together
 * with the existing question source the displayed signal justifies. Both end in the same builder;
 * only the source the builder opens on differs.
 */
@Composable
internal fun TopicDetailDestination(
    topicId: String,
    targetSubtopicId: String? = null,
    onBack: () -> Unit,
    onConfigurePractice: (AssessmentScope) -> Unit,
    onConfigureTargetedPractice: (PracticePreset) -> Unit,
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
        // Already built from the scope the screen is rendering, so there is nothing left to
        // resolve: the row it came from is in the state the ViewModel produced.
        onPracticePreset = onConfigureTargetedPractice,
        onRetry = viewModel::retry,
    )
}
