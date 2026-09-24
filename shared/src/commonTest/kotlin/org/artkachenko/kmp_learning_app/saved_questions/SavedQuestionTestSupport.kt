package org.artkachenko.kmp_learning_app.saved_questions

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.artkachenko.kmp_learning_app.saved_questions.repository.SavedQuestionRepository

/**
 * An app-scoped holder for tests, on the test scheduler.
 *
 * Deliberately not `backgroundScope`: work launched there is not run by `advanceUntilIdle`, so a
 * refresh or a mutation would silently never complete.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun TestScope.savedQuestionStateHolder(
    repository: SavedQuestionRepository = FakeSavedQuestionRepository(),
): SavedQuestionStateHolder =
    SavedQuestionStateHolder(
        repository = repository,
        scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler)),
    )

/**
 * In-memory stand-in for the persisted saved-Question table, shared by the state-holder tests and
 * the review-surface tests that only need to observe what reached the repository.
 *
 * It mirrors the persisted contract that matters to callers: a repeated save neither duplicates the
 * identity nor changes its original timestamp.
 */
internal class FakeSavedQuestionRepository(
    initial: List<SavedQuestion> = emptyList(),
) : SavedQuestionRepository {
    private val saved = initial.toMutableList()

    val saveCalls = mutableListOf<String>()
    val unsaveCalls = mutableListOf<String>()

    /** Every read the holder makes goes through [getSavedQuestions]; this must stay at zero. */
    var isSavedCalls = 0
        private set

    /**
     * Reads of the saved table. Only the app-scoped holder should perform them, so a surface that
     * quietly added a repository subscription of its own would show up here as extra reads.
     */
    var readCalls = 0
        private set

    var failReads = false
    var failMutations = false

    /** When set, a save suspends until it completes, so a pending mutation can be observed. */
    var saveGate: CompletableDeferred<Unit>? = null

    /** The same, for a removal: what a browsing surface's pending action is waiting on. */
    var unsaveGate: CompletableDeferred<Unit>? = null

    /** When set, a read suspends until it completes, so a surface can be observed while Loading. */
    var readGate: CompletableDeferred<Unit>? = null

    /**
     * When set, the *next* read takes its snapshot on entry and only returns it once this
     * completes — a read that reached the database before a later write and returns after it.
     * [readGate] cannot express that, because it snapshots after the gate opens.
     */
    var staleReadGate: CompletableDeferred<Unit>? = null

    override suspend fun save(questionId: String) {
        saveCalls += questionId
        saveGate?.await()
        if (failMutations) error("Save failed.")
        if (saved.none { it.questionId == questionId }) {
            saved.add(0, SavedQuestion(questionId, savedAtEpochMillis = 9_000))
        }
    }

    override suspend fun unsave(questionId: String) {
        unsaveCalls += questionId
        unsaveGate?.await()
        if (failMutations) error("Unsave failed.")
        saved.removeAll { it.questionId == questionId }
    }

    override suspend fun isSaved(questionId: String): Boolean {
        isSavedCalls += 1
        return saved.any { it.questionId == questionId }
    }

    override suspend fun getSavedQuestions(): List<SavedQuestion> {
        readCalls += 1
        val stale = staleReadGate
        if (stale != null) {
            staleReadGate = null
            val snapshot = saved.toList()
            stale.await()
            if (failReads) error("Saved questions unavailable.")
            return snapshot
        }
        readGate?.await()
        if (failReads) error("Saved questions unavailable.")
        return saved.toList()
    }
}
