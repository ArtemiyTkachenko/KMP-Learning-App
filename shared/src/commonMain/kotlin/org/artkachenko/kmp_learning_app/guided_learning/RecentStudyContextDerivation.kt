package org.artkachenko.kmp_learning_app.guided_learning

import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.TestAttempt

/**
 * The stable study context one stored attempt describes, or `null` when it describes none.
 *
 * This is the single definition of "recent study" for guided learning, so the recommendation
 * policy's tie-break and the Continue Studying surface cannot drift apart. Two rules live here
 * rather than being repeated by each consumer:
 *
 * - an `IN_PROGRESS` attempt has no study context. Guided learning describes what the learner has
 *   *finished*, and an abandoned attempt is not somewhere to send them back to. Callers normally
 *   pass completed history already, so this is a defensive guard on a lower-level input rather
 *   than a filter anything is allowed to depend on;
 * - a Mixed run is real recent activity but identifies no Topic, so it reports
 *   [RecentStudyContext.Mixed] rather than having a scope invented from the Questions it asked.
 *
 * The persisted [AssessmentConfig] is the source of truth. Nothing here reads a display name, a
 * Question, an answer, or the attempt ID: an attempt is evidence of what was studied, never a
 * navigation target.
 */
internal fun TestAttempt.toRecentStudyContext(): RecentStudyContext? {
    if (status != AssessmentStatus.COMPLETED) return null
    return when (val config = config) {
        is AssessmentConfig.Focused -> RecentStudyContext.Focused(config.scope)
        is AssessmentConfig.Mixed -> RecentStudyContext.Mixed
    }
}
