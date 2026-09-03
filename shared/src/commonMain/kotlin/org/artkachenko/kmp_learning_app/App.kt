package org.artkachenko.kmp_learning_app

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingLaunch
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewDestination
import org.artkachenko.kmp_learning_app.mixed_interview.InterviewStartDestination
import org.artkachenko.kmp_learning_app.mixed_interview.MixedInterviewDestination
import org.artkachenko.kmp_learning_app.mixed_interview.MixedInterviewResultDestination
import org.artkachenko.kmp_learning_app.mixed_interview.mixedInterviewStartRoute
import org.artkachenko.kmp_learning_app.mixed_interview.toAssessmentConfig
import org.artkachenko.kmp_learning_app.mixed_interview.toAssessmentTakingLaunch
import org.artkachenko.kmp_learning_app.progress.ProgressDestination
import org.artkachenko.kmp_learning_app.progress.ProgressTopicDestination
import org.artkachenko.kmp_learning_app.topic_study.focused_practice.FocusedPracticeDestination
import org.artkachenko.kmp_learning_app.topic_study.focused_practice.toAssessmentConfig
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultDestination
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderDestination
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.toAssessmentScope
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.toPracticeBuilderRoute
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.toPracticeRoute
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicDetailDestination
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserDestination
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    AppTheme {
        AppShell()
    }
}

@Composable
private fun AppShell(
    modifier: Modifier = Modifier,
) {
    // Owns one saveable back stack per area; see rememberAppNavigator for the explicit call sites.
    val navigator = rememberAppNavigator()
    val backStack = navigator.backStack
    fun popBack() {
        navigator.popBack()
    }

    val currentRoute = navigator.currentRoute
    val showsNavigation = currentRoute?.showsAreaNavigation() ?: true

    val shellViewModel: AppShellViewModel = koinViewModel()
    // Derived from the shared history cache, so it follows an assessment completing rather than
    // being recounted every time navigation moves between areas.
    val unresolvedMistakeCount by shellViewModel.unresolvedMistakeCount.collectAsStateWithLifecycle()
    val badges = mapOf(AppTopLevelDestination.MISTAKES to unresolvedMistakeCount)

    // NavDisplay enables its own back handler only while the stack it was given has a previous
    // entry, so at an area's root back reaches nothing and the host closes the app. This handler
    // covers exactly that case. It is called unconditionally and gated by isBackEnabled, because
    // the library invokes the last-composed *enabled* handler and a conditional call would reorder
    // composition. NavDisplay's handler is composed deeper, and canLeaveArea is true exactly when
    // NavDisplay's is disabled, so the two are mutually exclusive.
    val areaBackState = rememberNavigationEventState(
        currentInfo = NavigationEventInfo.None,
        backInfo = if (navigator.canLeaveArea) listOf(NavigationEventInfo.None) else emptyList(),
    )
    NavigationBackHandler(
        state = areaBackState,
        isBackEnabled = navigator.canLeaveArea,
        onBackCompleted = { navigator.popBack() },
    )

    AppNavigationScaffold(
        selected = navigator.area,
        onSelect = navigator::select,
        showsNavigation = showsNavigation,
        modifier = modifier,
        badges = badges,
    ) { contentPadding ->
        NavDisplay(
            backStack = backStack,
            // The shell's insets are applied exactly once, here, and then consumed so nothing
            // deeper adds them a second time. Previously this padded by contentPadding and then
            // by safeContentPadding: Scaffold reports its inset padding without consuming it, so
            // both saw the same system bars and every screen was inset twice.
            //
            // contentPadding carries no top, which leaves the status bar unconsumed on purpose:
            // each screen's TopAppBar pads for it and paints its container behind it, which is
            // what puts the bar against the top edge of the window.
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding),
            entryDecorators = listOf(
                // Saveable state must be installed before the ViewModel decorator so each
                // navigation entry owns the state registry used by its ViewModel store owner.
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            onBack = {
                popBack()
            },
            transitionSpec = appTransitionSpec(),
            popTransitionSpec = appPopTransitionSpec(),
            predictivePopTransitionSpec = appPredictivePopTransitionSpec(),
            entryProvider = entryProvider {
                entry<AppRoute.Topics> {
                    TopicBrowserDestination(
                        onTopicClick = { topicId ->
                            navigator.push(AppRoute.Topic(topicId = topicId))
                        },
                        onSubtopicClick = { topicId, subtopicId ->
                            navigator.push(
                                AppRoute.Topic(
                                    topicId = topicId,
                                    subtopicId = subtopicId,
                                ),
                            )
                        },
                        // Continuing recent study pushes an existing destination — Topic detail or
                        // the Practice Builder — and never an attempt route: it returns the learner
                        // to a learning context rather than resuming an assessment.
                        onContinueStudying = { target ->
                            navigator.push(target.toAppRoute())
                        },
                        // The recommended action reaches an existing capability — Topics, Mistake
                        // Review, or the Practice Builder — and never starts an assessment. Two of
                        // those are areas rather than details, so the mapping selects or pushes as
                        // that destination requires.
                        onRecommendedNext = navigator::openRecommendation,
                    )
                }
                entry<AppRoute.Interview> {
                    InterviewStartDestination(
                        onStartMixedInterview = {
                            navigator.push(mixedInterviewStartRoute())
                        },
                        onOpenResult = { attemptId ->
                            navigator.push(AppRoute.MixedInterviewResult(attemptId))
                        },
                    )
                }
                entry<AppRoute.Progress> {
                    ProgressDestination(
                        onBrowseTopics = { navigator.select(AppTopLevelDestination.TOPICS) },
                        onOpenTopic = { topicId ->
                            navigator.push(AppRoute.ProgressTopic(topicId))
                        },
                        onOpenFocusedResult = { attemptId ->
                            navigator.push(AppRoute.FocusedPracticeResult(attemptId))
                        },
                        onOpenMixedResult = { attemptId ->
                            navigator.push(AppRoute.MixedInterviewResult(attemptId))
                        },
                    )
                }
                entry<AppRoute.ProgressTopic> { route ->
                    ProgressTopicDestination(
                        topicId = route.topicId,
                        onBack = { popBack() },
                    )
                }
                entry<AppRoute.MistakeReview> {
                    MistakeReviewDestination(
                        onBrowseTopics = { navigator.select(AppTopLevelDestination.TOPICS) },
                    )
                }
                entry<AppRoute.MixedInterview> { route ->
                    MixedInterviewDestination(
                        launch = AssessmentTakingLaunch.New(route.toAssessmentConfig()),
                        onBack = { popBack() },
                        onAttemptPersisted = { attemptId ->
                            navigator.replaceTop(AppRoute.MixedInterviewAttempt(attemptId))
                        },
                        onCompleted = { attemptId ->
                            navigator.replaceTop(AppRoute.MixedInterviewResult(attemptId))
                        },
                    )
                }
                entry<AppRoute.MixedInterviewAttempt> { route ->
                    MixedInterviewDestination(
                        launch = route.toAssessmentTakingLaunch(),
                        onBack = { popBack() },
                        onAttemptPersisted = {},
                        onCompleted = { attemptId ->
                            navigator.replaceTop(AppRoute.MixedInterviewResult(attemptId))
                        },
                    )
                }
                entry<AppRoute.MixedInterviewResult> { route ->
                    MixedInterviewResultDestination(
                        attemptId = route.attemptId,
                        onBack = { popBack() },
                        onRetakeCreated = { attemptId ->
                            navigator.push(AppRoute.MixedInterviewAttempt(attemptId))
                        },
                    )
                }
                entry<AppRoute.Topic> { route ->
                    TopicDetailDestination(
                        topicId = route.topicId,
                        targetSubtopicId = route.subtopicId,
                        onBack = {
                            popBack()
                        },
                        // Practice is configured before it starts, so a Topic or Subtopic action
                        // opens the builder already scoped to it rather than launching a run.
                        onConfigurePractice = { scope ->
                            navigator.push(scope.toPracticeBuilderRoute())
                        },
                    )
                }
                entry<AppRoute.PracticeBuilderTopic> { route ->
                    PracticeBuilderDestination(
                        scope = route.toAssessmentScope(),
                        onBack = { popBack() },
                        onStartPractice = { config ->
                            navigator.push(config.toPracticeRoute())
                        },
                        // Only the selection the builder opens on. It still applies its own count
                        // and level defaults and runs its normal preflight, so nothing starts here.
                        initialSource = route.source,
                    )
                }
                entry<AppRoute.PracticeBuilderSubtopic> { route ->
                    PracticeBuilderDestination(
                        scope = route.toAssessmentScope(),
                        onBack = { popBack() },
                        onStartPractice = { config ->
                            navigator.push(config.toPracticeRoute())
                        },
                        initialSource = route.source,
                    )
                }
                entry<AppRoute.FocusedTopicPractice> { route ->
                    FocusedPracticeDestination(
                        launch = AssessmentTakingLaunch.New(route.toAssessmentConfig()),
                        onBack = { popBack() },
                        onAttemptPersisted = { attemptId ->
                            navigator.replaceTop(AppRoute.FocusedPracticeAttempt(attemptId))
                        },
                        onCompleted = { attemptId ->
                            navigator.replaceTop(AppRoute.FocusedPracticeResult(attemptId))
                        },
                    )
                }
                entry<AppRoute.FocusedSubtopicPractice> { route ->
                    FocusedPracticeDestination(
                        launch = AssessmentTakingLaunch.New(route.toAssessmentConfig()),
                        onBack = { popBack() },
                        onAttemptPersisted = { attemptId ->
                            navigator.replaceTop(AppRoute.FocusedPracticeAttempt(attemptId))
                        },
                        onCompleted = { attemptId ->
                            navigator.replaceTop(AppRoute.FocusedPracticeResult(attemptId))
                        },
                    )
                }
                entry<AppRoute.FocusedPracticeAttempt> { route ->
                    FocusedPracticeDestination(
                        launch = AssessmentTakingLaunch.ExistingAttempt(route.attemptId),
                        onBack = { popBack() },
                        onAttemptPersisted = {},
                        onCompleted = { attemptId ->
                            navigator.replaceTop(AppRoute.FocusedPracticeResult(attemptId))
                        },
                    )
                }
                entry<AppRoute.FocusedPracticeResult> { route ->
                    FocusedResultDestination(
                        attemptId = route.attemptId,
                        onBack = { popBack() },
                        onRetakeCreated = { attemptId ->
                            navigator.push(AppRoute.FocusedPracticeAttempt(attemptId))
                        },
                    )
                }
            },
        )
    }
}
