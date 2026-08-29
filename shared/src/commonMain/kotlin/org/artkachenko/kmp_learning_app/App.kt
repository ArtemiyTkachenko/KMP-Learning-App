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
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingLaunch
import org.artkachenko.kmp_learning_app.mixed_interview.MixedInterviewDestination
import org.artkachenko.kmp_learning_app.mixed_interview.MixedInterviewResultDestination
import org.artkachenko.kmp_learning_app.mixed_interview.mixedInterviewStartRoute
import org.artkachenko.kmp_learning_app.mixed_interview.toAssessmentTakingLaunch
import org.artkachenko.kmp_learning_app.mixed_interview.toAssessmentConfig
import org.artkachenko.kmp_learning_app.progress.ProgressDestination
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
                        onStartMixedInterview = {
                            backStack.add(mixedInterviewStartRoute())
                        },
                        onOpenProgress = {
                            backStack.add(AppRoute.Progress)
                        },
                    )
                }
                entry<AppRoute.Progress> {
                    ProgressDestination(
                        onBack = { popBack() },
                        onOpenFocusedResult = { attemptId ->
                            backStack.add(AppRoute.FocusedPracticeResult(attemptId))
                        },
                        onOpenMixedResult = { attemptId ->
                            backStack.add(AppRoute.MixedInterviewResult(attemptId))
                        },
                    )
                }
                entry<AppRoute.MixedInterview> { route ->
                    MixedInterviewDestination(
                        launch = AssessmentTakingLaunch.New(route.toAssessmentConfig()),
                        onBack = { popBack() },
                        onAttemptPersisted = { attemptId ->
                            backStack.replaceTopWith(AppRoute.MixedInterviewAttempt(attemptId))
                        },
                        onCompleted = { attemptId ->
                            backStack.replaceTopWith(AppRoute.MixedInterviewResult(attemptId))
                        },
                    )
                }
                entry<AppRoute.MixedInterviewAttempt> { route ->
                    MixedInterviewDestination(
                        launch = route.toAssessmentTakingLaunch(),
                        onBack = { popBack() },
                        onAttemptPersisted = {},
                        onCompleted = { attemptId ->
                            backStack.replaceTopWith(AppRoute.MixedInterviewResult(attemptId))
                        },
                    )
                }
                entry<AppRoute.MixedInterviewResult> { route ->
                    MixedInterviewResultDestination(
                        attemptId = route.attemptId,
                        onBack = { popBack() },
                        onRetakeCreated = { attemptId ->
                            backStack.add(AppRoute.MixedInterviewAttempt(attemptId))
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
                        launch = AssessmentTakingLaunch.New(route.toAssessmentConfig()),
                        onBack = { popBack() },
                        onAttemptPersisted = { attemptId ->
                            backStack.replaceTopWith(AppRoute.FocusedPracticeAttempt(attemptId))
                        },
                        onCompleted = { attemptId ->
                            backStack.replaceTopWith(AppRoute.FocusedPracticeResult(attemptId))
                        },
                    )
                }
                entry<AppRoute.FocusedSubtopicPractice> { route ->
                    FocusedPracticeDestination(
                        launch = AssessmentTakingLaunch.New(route.toAssessmentConfig()),
                        onBack = { popBack() },
                        onAttemptPersisted = { attemptId ->
                            backStack.replaceTopWith(AppRoute.FocusedPracticeAttempt(attemptId))
                        },
                        onCompleted = { attemptId ->
                            backStack.replaceTopWith(AppRoute.FocusedPracticeResult(attemptId))
                        },
                    )
                }
                entry<AppRoute.FocusedPracticeAttempt> { route ->
                    FocusedPracticeDestination(
                        launch = AssessmentTakingLaunch.ExistingAttempt(route.attemptId),
                        onBack = { popBack() },
                        onAttemptPersisted = {},
                        onCompleted = { attemptId ->
                            backStack.replaceTopWith(AppRoute.FocusedPracticeResult(attemptId))
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
