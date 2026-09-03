package org.artkachenko.kmp_learning_app

import androidx.compose.ui.graphics.vector.ImageVector
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.destination_interview
import kmp_learning_app.shared.generated.resources.destination_mistakes
import kmp_learning_app.shared.generated.resources.destination_progress
import kmp_learning_app.shared.generated.resources.destination_topics
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.jetbrains.compose.resources.StringResource

/**
 * The areas reachable from the navigation bar.
 *
 * The start screen used to carry four unrelated jobs at once — the topic list, the mixed interview
 * call to action, the progress entry, and the app heading. Each of those is now its own
 * destination, so none of them has to compete for room on one screen.
 */
internal enum class AppTopLevelDestination(
    val route: AppRoute,
    val label: StringResource,
    val icon: ImageVector,
) {
    TOPICS(AppRoute.Topics, Res.string.destination_topics, AppIcons.Topics),
    INTERVIEW(AppRoute.Interview, Res.string.destination_interview, AppIcons.Interview),
    PROGRESS(AppRoute.Progress, Res.string.destination_progress, AppIcons.Insights),
    MISTAKES(AppRoute.MistakeReview, Res.string.destination_mistakes, AppIcons.Warning),
    ;

    internal companion object {
        /** Where the app opens, and where back returns from any other area. */
        val Start: AppTopLevelDestination = TOPICS

        /** The destination whose area [route] belongs to, or null for a detail screen. */
        fun forRoute(route: AppRoute): AppTopLevelDestination? =
            entries.firstOrNull { it.route == route }
    }
}

/**
 * Whether navigation between areas stays available on [route].
 *
 * Browsing screens keep it: hiding the bar on every detail trapped a learner inside an area until
 * they pressed back, so a topic could not be left for Progress in one move. Screens that own the
 * learner's full attention hide it instead — an assessment in progress should not offer a one-tap
 * exit, and a result reads as a conclusion to dismiss rather than a place to switch away from.
 */
internal fun AppRoute.showsAreaNavigation(): Boolean =
    when (this) {
        is AppRoute.Topics,
        is AppRoute.Interview,
        is AppRoute.Progress,
        is AppRoute.MistakeReview,
        is AppRoute.Topic,
        is AppRoute.ProgressTopic,
        // Browsing saved Questions is review, not an assessment: there is nothing in progress to
        // interrupt, so switching areas from here costs the learner nothing.
        is AppRoute.SavedQuestions,
        // Setting practice up is not yet doing it: nothing has been started, so leaving costs the
        // learner nothing and the bar stays, exactly as on the Topic this was opened from.
        is AppRoute.PracticeBuilderTopic,
        is AppRoute.PracticeBuilderSubtopic,
        -> true

        is AppRoute.MixedInterview,
        is AppRoute.MixedInterviewAttempt,
        is AppRoute.MixedInterviewResult,
        is AppRoute.FocusedTopicPractice,
        is AppRoute.FocusedSubtopicPractice,
        is AppRoute.FocusedPracticeAttempt,
        is AppRoute.FocusedPracticeResult,
        -> false
    }
