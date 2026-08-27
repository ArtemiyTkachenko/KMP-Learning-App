package org.artkachenko.kmp_learning_app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserDestination
import org.artkachenko.kmp_learning_app.topic_study.focused_practice.FocusedPracticeDestination
import org.artkachenko.kmp_learning_app.topic_study.focused_practice.FocusedPracticeLaunch
import org.artkachenko.kmp_learning_app.topic_study.focused_practice.toAssessmentConfig
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultDestination
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicDetailDestination
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.toAppRoute

@Composable
fun App() {
    MaterialTheme {
        AppShell()
    }
}

@Composable
private fun AppShell(
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(
        appNavigationSavedStateConfiguration,
        AppRoute.Topics,
    )
    fun popBack() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
    fun replaceTop(route: AppRoute) {
        popBack()
        backStack.add(route)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .safeContentPadding(),
            entryDecorators = listOf(
                // Saveable state must be installed before the ViewModel decorator so each
                // navigation entry owns the state registry used by its ViewModel store owner.
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            onBack = {
                popBack()
            },
            entryProvider = entryProvider {
                entry<AppRoute.Topics> {
                    TopicBrowserDestination(
                        onTopicClick = { topicId ->
                            backStack.add(AppRoute.Topic(topicId = topicId))
                        },
                    )
                }
                entry<AppRoute.Topic> { route ->
                    TopicDetailDestination(
                        topicId = route.topicId,
                        onBack = {
                            popBack()
                        },
                        onStartFocusedPractice = { config ->
                            backStack.add(config.toAppRoute())
                        },
                    )
                }
                entry<AppRoute.FocusedTopicPractice> { route ->
                    FocusedPracticeDestination(
                        launch = FocusedPracticeLaunch.New(route.toAssessmentConfig()),
                        onBack = { popBack() },
                        onCompleted = { attemptId ->
                            replaceTop(AppRoute.FocusedPracticeResult(attemptId))
                        },
                    )
                }
                entry<AppRoute.FocusedSubtopicPractice> { route ->
                    FocusedPracticeDestination(
                        launch = FocusedPracticeLaunch.New(route.toAssessmentConfig()),
                        onBack = { popBack() },
                        onCompleted = { attemptId ->
                            replaceTop(AppRoute.FocusedPracticeResult(attemptId))
                        },
                    )
                }
                entry<AppRoute.FocusedPracticeAttempt> { route ->
                    FocusedPracticeDestination(
                        launch = FocusedPracticeLaunch.ExistingAttempt(route.attemptId),
                        onBack = { popBack() },
                        onCompleted = { attemptId ->
                            replaceTop(AppRoute.FocusedPracticeResult(attemptId))
                        },
                    )
                }
                entry<AppRoute.FocusedPracticeResult> { route ->
                    FocusedResultDestination(
                        attemptId = route.attemptId,
                        onBack = { popBack() },
                        onRetakeCreated = { attemptId ->
                            backStack.add(AppRoute.FocusedPracticeAttempt(attemptId))
                        },
                    )
                }
            },
        )
    }
}
