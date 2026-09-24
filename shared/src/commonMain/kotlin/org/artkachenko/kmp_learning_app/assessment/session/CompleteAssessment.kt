package org.artkachenko.kmp_learning_app.assessment.session

import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository

/**
 * Finishes one assessment: scores it, persists the completed attempt, and marks the shared history
 * cache stale. The counterpart of
 * [org.artkachenko.kmp_learning_app.assessment.start.StartAssessment], which creates the durable
 * identity this ends.
 *
 * ## Why the invalidation lives here
 *
 * Completion is the *only* transition that changes completed history, so it is the only point at
 * which the app-scoped cache behind Progress, the mistake queue, the interview record, the Mistakes
 * badge, Topic learning context and unseen-practice selection can be wrong. Nothing in
 * [AssessmentRepository.save]'s signature says that writing a `COMPLETED` attempt obliges a second
 * call, and the repository cannot make the call itself: [AssessmentHistoryStore] is built *on* the
 * repository, so depending on the store from the data layer would close a cycle. Owning both steps
 * in one operation is what leaves no second call for a later caller to forget — the same defect
 * shape as `CQ-STATE-012` and `CQ-STATE-013`, both of which became real bugs because a caller did.
 *
 * ## Why the invalidation is in `finally`
 *
 * [AssessmentRepository.save] is a single atomic write transaction, but the step *after* it is
 * reached by resuming a continuation — and a cancelled job throws at that resumption. So the
 * transaction could commit and the invalidation still never run, leaving the attempt `COMPLETED` in
 * the database while every derived surface silently omitted it for the rest of the process. The
 * learner backing out of the taking destination as the write lands is enough to produce that.
 *
 * Marking the cache stale unconditionally is safe because invalidation is idempotent and cheap: its
 * only effect is one re-read of the attempt table, which after a write that did *not* commit returns
 * exactly the attempts already cached. Announcing a re-read that changed nothing is the documented
 * contract of [AssessmentHistoryStore.history], so a spurious invalidation costs one query and
 * changes nothing observable. Losing a required one costs correctness. The asymmetry is the whole
 * argument, and it is why this needs no `NonCancellable`: [AssessmentHistoryStore.invalidate] does
 * not suspend, so a `finally` block runs it even on cancellation.
 */
internal class CompleteAssessment(
    private val assessmentEngine: AssessmentEngine,
    private val assessmentRepository: AssessmentRepository,
    private val historyStore: AssessmentHistoryStore,
) {
    /**
     * Scores [session] and persists it as completed, returning the completed session.
     *
     * Throws if [session] cannot be completed — already completed, or not every question answered —
     * and then nothing is written and nothing is invalidated, because no completed attempt was
     * created. Repeating the call after a failed write is safe: the attempt keeps its identity and
     * the write is a full replace, so a retry re-scores and rewrites the same row rather than
     * creating a second occurrence.
     */
    suspend operator fun invoke(session: AssessmentSession): AssessmentSession {
        val completedSession = assessmentEngine.complete(session)
        try {
            assessmentRepository.save(completedSession.attempt)
        } finally {
            historyStore.invalidate()
        }
        return completedSession
    }
}
