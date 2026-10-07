package org.artkachenko.kmp_learning_app.mistake_review

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistory
import org.artkachenko.kmp_learning_app.assessment.history.VisibleAssessmentHistory
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
 *
 * [learningContentRepository] is required rather than optional: it is the source of every study
 * link on the queue, and an omitted one would remove those links from every entry without failing
 * anything. Learning content that cannot be read is still optional enrichment — the queue is shown
 * without links — but that is a runtime outcome, not a mode a constructor call can select.
 */
internal class MistakeReviewStateHolder(
    private val mistakeReviewService: MistakeReviewService,
    visibleHistory: VisibleAssessmentHistory,
    scope: CoroutineScope,
    private val learningContentRepository: LearningContentRepository,
) {
    /**
     * Re-derives the queue on every settled refresh of the shared history, which is what makes one
     * [VisibleAssessmentHistory.invalidate] recover both failures this screen can show, on every
     * visibility change — the queue is derived from the visible projection only — and when a
     * coming-up entry becomes due, which the history re-announces so the entry moves to Due now.
     *
     * Re-reading the attempt table recovers an unreadable one. It also recovers a queue derivation
     * that failed over history which read perfectly well — an unavailable curriculum while
     * reconstructing review content — because the store re-announces the cached history once the
     * re-read settles whether or not the attempts changed.
     */
    val state: StateFlow<MistakeReviewUiState> = visibleHistory.history
        .map { history ->
            when (history) {
                AssessmentHistory.Loading -> MistakeReviewUiState.Loading
                AssessmentHistory.Failed -> MistakeReviewUiState.Error
                is AssessmentHistory.Loaded -> queueFor(history.attempts)
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, MistakeReviewUiState.Loading)

    private suspend fun queueFor(attempts: List<org.artkachenko.kmp_learning_app.assessment.TestAttempt>) =
        runCatching {
            // The service already orders the queue soonest due first and decides which entries are
            // due, so presentation preserves that list exactly and only splits it. Handing over the
            // cached history keeps this to the curriculum reads for the scheduled items alone.
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
        val units = runCatching { learningContentRepository.getActiveUnits() }
            .getOrElse { failure ->
                if (failure is CancellationException) throw failure
                return mistakes
            }
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
