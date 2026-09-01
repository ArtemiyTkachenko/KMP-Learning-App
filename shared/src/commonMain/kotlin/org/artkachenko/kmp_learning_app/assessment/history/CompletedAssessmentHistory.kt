package org.artkachenko.kmp_learning_app.assessment.history

import org.artkachenko.kmp_learning_app.assessment.TestAttempt

/**
 * A one-shot read of completed assessment history, for domain code that derives from it.
 *
 * Selection needs the completed attempts once per request rather than a subscription, and it has no
 * use for where they came from. Keeping that behind this interface is what lets
 * [AssessmentHistoryStore] answer the Practice Builder's repeated preflight reads out of the
 * app-scoped cache while unseen and weak-area selection stay plain domain logic, testable with a
 * lambda and with no `StateFlow`, cache, or coroutine scope of their own.
 */
internal fun interface CompletedAssessmentHistory {
    /**
     * Completed attempts, newest first, matching what
     * [org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository.getCompletedAttempts]
     * returns.
     *
     * Throws when history cannot be read, and callers must let that propagate: an unreadable
     * history is not an empty one, and treating it as empty would silently report every Question as
     * unseen or erase all weak areas and start a practice run built on a false premise.
     */
    suspend fun completedAttempts(): List<TestAttempt>
}

/**
 * Completed history could not be read, so nothing derived from it can be answered.
 *
 * A distinct type rather than the repository's own failure, because [AssessmentHistoryStore] holds
 * the failure as state instead of an exception and still has to report it to a suspending caller.
 */
internal class AssessmentHistoryUnavailableException :
    Exception("Completed assessment history could not be read.")
