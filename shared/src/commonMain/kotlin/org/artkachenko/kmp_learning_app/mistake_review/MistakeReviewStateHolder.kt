package org.artkachenko.kmp_learning_app.mistake_review

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistory
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository

/**
 * App-scoped mistake queue, derived from the shared history cache.
 *
 * The queue is rebuilt from completed attempts, which the navigation entry made this screen do from
 * scratch on every visit: the ViewModel is destroyed on a tab switch, so returning always started at
 * a spinner. Holding the last queue here means it is on screen for the first frame, with the re-read
 * happening behind it.
 */
internal class MistakeReviewStateHolder(
    private val mistakeReviewService: MistakeReviewService,
    historyStore: AssessmentHistoryStore,
    scope: CoroutineScope,
    private val learningContentRepository: LearningContentRepository? = null,
) {
    /**
     * Counts requests to derive again from history that has not itself changed; see
     * [retryDerivation].
     */
    private val derivations = MutableStateFlow(0)

    val state: StateFlow<MistakeReviewUiState> = combine(historyStore.history, derivations) { history, _ ->
        history
    }
        .map { history ->
            when (history) {
                AssessmentHistory.Loading -> MistakeReviewUiState.Loading
                AssessmentHistory.Failed -> MistakeReviewUiState.Error
                is AssessmentHistory.Loaded -> queueFor(history.attempts)
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, MistakeReviewUiState.Loading)

    /**
     * Derives the queue again from the currently cached history.
     *
     * Invalidating that history recovers an unreadable attempt table, but not a queue derivation
     * that failed over history which read perfectly well — an unavailable curriculum while
     * reconstructing review content. A re-read of unchanged history is an equal value that a
     * `StateFlow` does not re-emit, so without this the derivation would never run again and Retry
     * would leave the queue in [MistakeReviewUiState.Error] for the rest of the session.
     */
    fun retryDerivation() {
        derivations.update { it + 1 }
    }

    private suspend fun queueFor(attempts: List<org.artkachenko.kmp_learning_app.assessment.TestAttempt>) =
        runCatching {
            // The service already orders the queue by most recent unresolved occurrence, so
            // presentation preserves that list exactly. Handing over the cached history keeps this
            // to the curriculum reads for the unresolved items alone.
            attachStudyLessons(mistakeReviewService.load(attempts))
        }.fold(
            onSuccess = { mistakes ->
                if (mistakes.isEmpty()) MistakeReviewUiState.Empty else MistakeReviewUiState.Content(mistakes)
            },
            onFailure = { failure ->
                if (failure is CancellationException) throw failure
                MistakeReviewUiState.Error
            },
        )

    private suspend fun attachStudyLessons(
        mistakes: List<UnresolvedMistake>,
    ): List<UnresolvedMistake> {
        val units = runCatching { learningContentRepository?.getActiveUnits() }
            .getOrElse { failure ->
                if (failure is CancellationException) throw failure
                return mistakes
            } ?: return mistakes
        return mistakes.withStudyLessons(units)
    }
}

/** Maps only an unambiguous primary curriculum relationship; no title/text matching is involved. */
internal fun List<UnresolvedMistake>.withStudyLessons(
    units: List<LearningUnit>,
): List<UnresolvedMistake> {
    val lessonsBySubtopic = buildMap {
        units.forEach { unit ->
            unit.lessons.filter { it.status == ContentStatus.ACTIVE }.forEach { lesson ->
                lesson.primarySubtopicIds.forEach { subtopicId ->
                    val links = getOrPut(subtopicId) { mutableListOf<MistakeStudyLesson>() }
                    links += MistakeStudyLesson(unit.id, lesson.id, lesson.title)
                }
            }
        }
    }
    return map { mistake ->
        val question = (mistake.reviewItem as? ReviewQuestionItem.Available)?.question
        val link = question?.subtopicId?.let { lessonsBySubtopic[it]?.distinct()?.singleOrNull() }
        mistake.copy(studyLesson = link)
    }
}
