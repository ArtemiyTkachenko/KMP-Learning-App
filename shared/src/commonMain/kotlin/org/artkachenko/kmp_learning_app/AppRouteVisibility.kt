package org.artkachenko.kmp_learning_app

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.history.VisibleHistoryProjection
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSessionLoadResult
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSessionLoader
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibility

/**
 * What a back-stack entry is known to target under one [CurriculumVisibility].
 *
 * Three answers rather than a Boolean, because failing to classify a route is not evidence that it
 * targets hidden content. A missing ID, a failed read or a stale route is [Unknown] and stays on the
 * stack, where its destination's existing NotFound or Error handling answers for it. Only
 * [KnownHidden] is pruned.
 */
internal enum class AppRouteVisibility { Visible, KnownHidden, Unknown }

/**
 * Classifies routes by the content they target, derived from current content and never from how an
 * ID is spelled. Ownership is always a Topic, asked through [CurriculumVisibility.isTopicVisible]:
 *
 * - Area roots, Saved Questions and Settings identify no single Topic's curriculum: [Visible].
 * - `Topic`, `ProgressTopic`, `PracticeBuilderTopic`: the route's Topic ID itself.
 * - `PracticeBuilderSubtopic`: the resolved Subtopic's parent Topic.
 * - `LearningUnit`, `LearningLesson`, `PracticeBuilderLearningUnit`: the resolved Unit's home Topic.
 *   A Lesson route carries its Unit, and the Unit's ownership decides the Lesson's.
 * - Completed results: [KnownHidden] only when [VisibleHistoryProjection] leaves nothing of the
 *   attempt — the same rule, including "unresolved Questions stay visible", that the result screens,
 *   Progress and interview history apply. A partially visible result stays.
 * - In-progress attempts: [AssessmentSessionLoader]'s own rule — any resolved Question of a hidden
 *   Topic makes the whole attempt unavailable, because an in-progress attempt is never shortened.
 *
 * This is the pruning authority only. Every destination also guards itself, so a route this has not
 * classified yet — or could not — never shows hidden content.
 */
internal class AppRouteVisibilityResolver(
    private val curriculumRepository: CurriculumRepository,
    private val learningContentRepository: LearningContentRepository,
    private val assessmentRepository: AssessmentRepository,
    private val assessmentSessionLoader: AssessmentSessionLoader,
) {
    suspend fun classify(route: AppRoute, visibility: CurriculumVisibility): AppRouteVisibility {
        // Nothing hidden means nothing to look up.
        if (visibility.hidesNothing) return AppRouteVisibility.Visible
        return try {
            classifyOrNull(route, visibility) ?: AppRouteVisibility.Unknown
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (@Suppress("TooGenericExceptionCaught") failure: Exception) {
            AppRouteVisibility.Unknown
        }
    }

    /** Null when the route's owner cannot be established. */
    private suspend fun classifyOrNull(
        route: AppRoute,
        visibility: CurriculumVisibility,
    ): AppRouteVisibility? =
        when (route) {
            AppRoute.Topics,
            AppRoute.Interview,
            AppRoute.Progress,
            AppRoute.MistakeReview,
            AppRoute.SavedQuestions,
            AppRoute.Settings,
            -> AppRouteVisibility.Visible

            // The route's ID *is* the Topic's identity, so no read is needed to ask about it.
            is AppRoute.Topic -> topicVisibility(route.topicId, visibility)
            is AppRoute.ProgressTopic -> topicVisibility(route.topicId, visibility)
            is AppRoute.PracticeBuilderTopic -> topicVisibility(route.topicId, visibility)

            is AppRoute.PracticeBuilderSubtopic ->
                curriculumRepository.getSubtopicById(route.subtopicId)
                    ?.let { topicVisibility(it.topicId, visibility) }

            is AppRoute.LearningUnit -> unitVisibility(route.unitId, visibility)
            is AppRoute.LearningLesson -> unitVisibility(route.unitId, visibility)
            is AppRoute.PracticeBuilderLearningUnit -> unitVisibility(route.unitId, visibility)

            is AppRoute.FocusedPracticeResult -> completedVisibility(route.attemptId, visibility)
            is AppRoute.MixedInterviewResult -> completedVisibility(route.attemptId, visibility)

            is AppRoute.FocusedPracticeAttempt -> attemptVisibility(route.attemptId, visibility)
            is AppRoute.MixedInterviewAttempt -> attemptVisibility(route.attemptId, visibility)
        }

    private fun topicVisibility(topicId: String, visibility: CurriculumVisibility): AppRouteVisibility =
        if (visibility.isTopicVisible(topicId)) AppRouteVisibility.Visible else AppRouteVisibility.KnownHidden

    private suspend fun unitVisibility(
        unitId: String,
        visibility: CurriculumVisibility,
    ): AppRouteVisibility? =
        learningContentRepository.getUnitById(unitId)?.let { topicVisibility(it.topicId, visibility) }

    private suspend fun completedVisibility(
        attemptId: String,
        visibility: CurriculumVisibility,
    ): AppRouteVisibility? {
        val attempt = assessmentRepository.getById(attemptId) ?: return null
        // Not completed yet: the result destination's own NotCompleted answers for it.
        if (attempt.status != AssessmentStatus.COMPLETED) return null
        val visible = VisibleHistoryProjection.visibleAttempts(listOf(attempt), visibility, curriculumRepository)
        return if (visible.isEmpty()) AppRouteVisibility.KnownHidden else AppRouteVisibility.Visible
    }

    private suspend fun attemptVisibility(
        attemptId: String,
        visibility: CurriculumVisibility,
    ): AppRouteVisibility? =
        when (assessmentSessionLoader.load(attemptId, visibility)) {
            is AssessmentSessionLoadResult.Loaded -> AppRouteVisibility.Visible
            AssessmentSessionLoadResult.ContentUnavailable -> AppRouteVisibility.KnownHidden
            // An attempt route whose attempt has finished hands over to its result, so it is judged
            // by the result's rule.
            AssessmentSessionLoadResult.NotInProgress -> completedVisibility(attemptId, visibility)
            AssessmentSessionLoadResult.AttemptNotFound,
            is AssessmentSessionLoadResult.MissingQuestion,
            -> null
        }
}

/**
 * Keeps every area's back stack free of routes that target hidden content, for as long as it runs.
 *
 * Validates the stacks under the current visibility first — which is what makes a stack restored
 * from a session where Kotlin Multiplatform content was shown valid under a setting that hides it —
 * and again on every change. `collectLatest` cancels a classification still running when the
 * visibility changes, so a pass started under an older visibility can never prune a stack after a
 * newer one has taken over.
 *
 * Navigation state only: nothing here writes an attempt, a saved Question or a study record, and
 * showing content again does not recreate routes that were pruned.
 */
internal suspend fun pruneRoutesHiddenBy(
    visibility: Flow<CurriculumVisibility>,
    navigator: AppNavigator,
    resolver: AppRouteVisibilityResolver,
) {
    visibility.collectLatest { current ->
        if (current.hidesNothing) return@collectLatest
        val hidden = navigator.detailRoutes()
            .filter { resolver.classify(it, current) == AppRouteVisibility.KnownHidden }
            .toSet()
        if (hidden.isNotEmpty()) navigator.pruneFrom(hidden)
    }
}
