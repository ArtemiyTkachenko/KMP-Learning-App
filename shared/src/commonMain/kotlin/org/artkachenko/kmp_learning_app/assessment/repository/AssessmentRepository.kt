package org.artkachenko.kmp_learning_app.assessment.repository

import org.artkachenko.kmp_learning_app.assessment.TestAttempt

/**
 * Durable storage for assessment attempts, addressed as whole aggregates.
 *
 * A [TestAttempt] is one aggregate — its configuration, its ordered question occurrences, and the
 * answer IDs selected for each of them — and this interface deliberately exposes no way to touch a
 * part of one. Everything a caller can do is read a coherent attempt or replace a coherent attempt,
 * which is what lets the domain invariants a [TestAttempt] enforces in memory also describe every
 * attempt that comes back out of storage.
 *
 * Callers are expected to serialise their own writes: one live owner per attempt ID. An assessment
 * is taken by exactly one screen and started exactly once, so nothing here arbitrates between two
 * concurrent writers, and no version or status guard would make an out-of-order pair of snapshots
 * meaningful if one ever appeared.
 */
internal interface AssessmentRepository {
    /**
     * Persists [attempt] as the authoritative snapshot of that attempt ID, atomically.
     *
     * Creation and update are the same operation on purpose: an assessment saves the same ID after
     * every answered Question, and treating each save as the current whole truth is what keeps the
     * stored aggregate from drifting. Occurrences and selected answers that are no longer part of
     * the aggregate do not survive the save, and no intermediate state — a new attempt row beside
     * the previous snapshot's children — is ever visible to a reader. Saving the same aggregate
     * twice is therefore idempotent.
     *
     * When this returns normally the attempt is durable. Anything the caller does afterwards, such
     * as invalidating a cache or navigating, is application synchronisation rather than durability.
     * A failure propagates; it does not partially commit.
     */
    suspend fun save(attempt: TestAttempt)

    /**
     * The attempt stored under [attemptId], or `null` when no such attempt exists.
     *
     * `null` means absent and nothing else. A stored attempt that cannot be reconstructed into a
     * valid aggregate — an unknown discriminator, a malformed payload, score fields that contradict
     * the status — fails rather than being reported as missing or silently repaired, because a
     * corrupted assessment and an assessment the learner never took are not the same answer.
     */
    suspend fun getById(attemptId: String): TestAttempt?

    /**
     * Every completed attempt, newest first by completion time, then start time, then stable ID.
     *
     * In-progress attempts are excluded at the storage boundary; they remain addressable through
     * [getById]. The result is one coherent snapshot of history rather than a set of independently
     * timed reads, and it fails as a whole if any attempt in it cannot be reconstructed.
     */
    suspend fun getCompletedAttempts(): List<TestAttempt>
}
