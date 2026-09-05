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
 *
 * [onLearningUnitClick] is the study half's handoff and is optional because the Learning Unit
 * destination does not exist yet. Absent, the Unit cards render as informational content rather
 * than as controls that lead nowhere; present, each card emits the stable Unit ID and nothing else.
 * The shell owns navigation, so nothing here resolves a Unit or knows what a route looks like.
 */
@Composable
internal fun TopicDetailDestination(
    topicId: String,
    targetSubtopicId: String? = null,
    onBack: () -> Unit,
    onConfigurePractice: (AssessmentScope) -> Unit,
    onConfigureTargetedPractice: (PracticePreset) -> Unit,
    onLearningUnitClick: ((String) -> Unit)? = null,
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
        onLearningUnitClick = onLearningUnitClick,
    )
}
