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
import org.artkachenko.kmp_learning_app.topic_study.focused_practice.toAssessmentConfig
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
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                }
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
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onStartFocusedPractice = { config ->
                            backStack.add(config.toAppRoute())
                        },
                    )
                }
                entry<AppRoute.FocusedTopicPractice> { route ->
                    FocusedPracticeDestination(
                        config = route.toAssessmentConfig(),
                        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
                    )
                }
                entry<AppRoute.FocusedSubtopicPractice> { route ->
                    FocusedPracticeDestination(
                        config = route.toAssessmentConfig(),
                        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
                    )
                }
            },
        )
    }
}
