package org.artkachenko.kmp_learning_app.assessment.history

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository

/**
 * A stand-in for the app scope the caches run on in production.
 *
 * Bound to `Dispatchers.Main`, which these tests replace with a test dispatcher, so the eagerly
 * shared flows are driven by `advanceUntilIdle()` like the rest of the test body.
 */
internal fun testCacheScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

internal fun testHistoryStore(
    repository: AssessmentRepository,
    scope: CoroutineScope,
): AssessmentHistoryStore = AssessmentHistoryStore(repository, scope)

/** Retained so call sites read the same; the caches share eagerly and need no collector. */
internal fun keepSubscribed(@Suppress("UNUSED_PARAMETER") flow: StateFlow<*>) = Unit
