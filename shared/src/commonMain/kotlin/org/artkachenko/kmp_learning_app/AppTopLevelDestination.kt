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
 * The rule is one sentence: normal application mode everywhere, focus mode only while a question is
 * actually being answered. Browsing, reading, configuring, and reviewing are all normal mode —
 * hiding the bar on every detail trapped a learner inside an area until they pressed back, so a
 * topic could not be left for Progress in one move. Focus mode is reserved for the two screens where
 * an assessment is in flight and a one-tap exit would abandon it.
 *
 * A result screen is deliberately normal mode. The assessment is over by the time it is reached, and
 * a learner reading their answers back is browsing; keeping them in focus mode meant finishing a
 * practice run left the app without navigation until they pressed back, which is the arbitrary
 * appear/disappear behaviour the rule exists to remove.
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
        is AppRoute.PracticeBuilderLearningUnit,
        // Reading is browsing: a Unit overview and a Lesson are study material the learner chose to
        // open, with nothing in progress to interrupt. They are details of Learn in exactly the way
        // Topic detail is, so leaving for Progress stays one move away.
        is AppRoute.LearningUnit,
        is AppRoute.LearningLesson,
        // Reviewing a finished assessment is reading, not answering. The attempt is persisted and
        // scored before either result route is reached, so there is nothing left to interrupt and
        // the learner returns to normal application chrome the moment the assessment ends.
        is AppRoute.MixedInterviewResult,
        is AppRoute.FocusedPracticeResult,
        -> true

        // Focus mode: a question is on screen and unanswered work would be abandoned by a one-tap
        // move to another area.
        is AppRoute.MixedInterviewAttempt,
        is AppRoute.FocusedPracticeAttempt,
        -> false
    }
