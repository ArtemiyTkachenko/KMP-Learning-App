package org.artkachenko.kmp_learning_app.progress_reset

import kotlin.coroutines.cancellation.CancellationException
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressStateHolder

/**
 * Starts the learner over: deletes every attempt and every studied-Lesson mark, then marks every
 * app-scoped cache derived from them stale.
 *
 * The caches are the two projections everything learner-facing reads. [AssessmentHistoryStore]
 * feeds — through `VisibleAssessmentHistory` — Progress, topic accuracy and coverage, the mistake
 * queue and its navigation badge, the interview record, recommendations, Continue Studying and
 * unseen-practice selection. [StudyProgressStateHolder] feeds every studied mark and Unit and Topic
 * study figure. Invalidating those two is what makes all of those surfaces read empty without a
 * restart, and nothing else holds a copy.
 *
 * The invalidation is in `finally` for the reason [org.artkachenko.kmp_learning_app.assessment
 * .session.CompleteAssessment] gives: the delete can commit and the caller still be cancelled at the
 * resumption after it, which would leave every surface showing deleted history for the rest of the
 * process. Both invalidations are non-suspending, so they run even under cancellation.
 *
 * Unlike completion, a delete that *failed* skips them. The delete is one transaction, so an
 * ordinary exception means nothing was committed and both caches are already right — and a failing
 * database is likely to fail the re-read too, which [StudyProgressStateHolder.invalidate] answers
 * with `Error`, taking every studied mark off the Learn surfaces over data that is intact.
 * Cancellation is the one outcome that cannot say whether the commit landed, so it still invalidates.
 */
internal class ResetLearnerProgress(
    private val repository: ProgressResetRepository,
    private val historyStore: AssessmentHistoryStore,
    private val studyProgress: StudyProgressStateHolder,
) {
    /** Throws if the delete fails, and then nothing was deleted. */
    suspend operator fun invoke() {
        var deleteFailed = false
        try {
            repository.deleteLearnerProgress()
        } catch (failure: Exception) {
            deleteFailed = failure !is CancellationException
            throw failure
        } finally {
            if (!deleteFailed) {
                historyStore.invalidate()
                studyProgress.invalidate()
            }
        }
    }
}
