package org.artkachenko.kmp_learning_app.topic_study.topics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingTarget
import org.artkachenko.kmp_learning_app.guided_learning.LearningRecommendationTarget
import org.artkachenko.kmp_learning_app.lesson_study.ContinueLearningTarget
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun TopicBrowserDestination(
    onTopicClick: (String) -> Unit,
    onSubtopicClick: (topicId: String, subtopicId: String) -> Unit,
    onContinueStudying: (ContinueStudyingTarget) -> Unit,
    onRecommendedNext: (LearningRecommendationTarget) -> Unit,
    onContinueLearning: (ContinueLearningTarget) -> Unit,
    onSavedQuestions: () -> Unit,
    viewModel: TopicBrowserViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TopicBrowserScreen(
        state = state,
        onTopicClick = onTopicClick,
        onSubtopicClick = onSubtopicClick,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onRetry = viewModel::retry,
        // The semantic target, not a route: navigation is mapped by the shell, so this screen
        // stays unaware of Navigation 3 exactly as it is for Topic and Subtopic clicks.
        onContinueStudyingClick = onContinueStudying,
        onRecommendedNextClick = onRecommendedNext,
        // Stable Unit and Lesson IDs only, mapped to the existing Lesson route by the shell. The
        // screen stays unaware of Navigation 3 here exactly as it is everywhere else on it.
        onContinueLearningClick = onContinueLearning,
        // A static entry: the screen never learns how many Questions are saved, so nothing here
        // reads saved state to decide whether the destination exists.
        onSavedQuestionsClick = onSavedQuestions,
    )
}
