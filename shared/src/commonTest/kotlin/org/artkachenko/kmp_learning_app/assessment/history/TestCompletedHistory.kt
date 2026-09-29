package org.artkachenko.kmp_learning_app.assessment.history

import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository

/**
 * Completed history read straight from [this] repository on every call, with no cache and no
 * visibility projection.
 *
 * For tests of a service's own derivation, where what matters is the attempts it is given and how
 * often it asks for them. The application never binds this: it binds the visible projection.
 */
internal fun AssessmentRepository.asCompletedHistory(): CompletedAssessmentHistory =
    CompletedAssessmentHistory { getCompletedAttempts() }
