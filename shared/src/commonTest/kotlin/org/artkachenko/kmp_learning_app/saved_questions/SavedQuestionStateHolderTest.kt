package org.artkachenko.kmp_learning_app.saved_questions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
internal class SavedQuestionStateHolderTest {
    @Test
    fun loadedStateKeepsTheRepositoryListAndItsOrder() = runTest {
        val repository = FakeSavedQuestionRepository(
            listOf(SavedQuestion("q3", 3_000), SavedQuestion("q1", 1_000)),
        )
        val holder = holder(repository)

        holder.refresh()
        advanceUntilIdle()

        val loaded = assertIs<SavedQuestionsState.Loaded>(holder.state.value)
        assertEquals(listOf("q3", "q1"), loaded.savedQuestions.map { it.questionId })
        assertEquals(setOf("q3", "q1"), loaded.savedQuestionIds)
        assertEquals(emptySet(), loaded.pendingQuestionIds)
    }

    @Test
    fun savingAnUnsavedQuestionPersistsItAndThenShowsItAsSaved() = runTest {
        val repository = FakeSavedQuestionRepository(listOf(SavedQuestion("q1", 1_000)))
        val holder = loadedHolder(repository)

        holder.toggleSaved("q2")
        advanceUntilIdle()

        assertEquals(listOf("q2"), repository.saveCalls)
        assertEquals(setOf("q1", "q2"), savedIds(holder))
        // The database's primary key is the idempotency guarantee, so the holder never asks whether
        // a Question is already saved before writing it.
        assertEquals(0, repository.isSavedCalls)
    }

    @Test
    fun unsavingASavedQuestionRemovesIt() = runTest {
        val repository = FakeSavedQuestionRepository(
            listOf(SavedQuestion("q1", 1_000), SavedQuestion("q2", 2_000)),
        )
        val holder = loadedHolder(repository)

        holder.toggleSaved("q1")
        advanceUntilIdle()

        assertEquals(listOf("q1"), repository.unsaveCalls)
        assertEquals(setOf("q2"), savedIds(holder))
    }

    @Test
    fun aSecondTapOnThePendingQuestionIsIgnoredWhileOtherQuestionsStayUsable() = runTest {
        val repository = FakeSavedQuestionRepository()
        val gate = CompletableDeferred<Unit>()
        repository.saveGate = gate
        val holder = loadedHolder(repository)

        holder.toggleSaved("q1")
        advanceUntilIdle()
        val pending = assertIs<SavedQuestionsState.Loaded>(holder.state.value)
        assertEquals(setOf("q1"), pending.pendingQuestionIds)

        // Rapid repeated taps on the same card cannot launch a competing toggle.
        holder.toggleSaved("q1")
        holder.toggleSaved("q1")
        advanceUntilIdle()
        assertEquals(listOf("q1"), repository.saveCalls)

        // A different card is untouched by the one in flight.
        holder.toggleSaved("q2")
        advanceUntilIdle()
        assertEquals(listOf("q1", "q2"), repository.saveCalls)

        gate.complete(Unit)
        advanceUntilIdle()
        val settled = assertIs<SavedQuestionsState.Loaded>(holder.state.value)
        assertEquals(emptySet(), settled.pendingQuestionIds)
        assertEquals(setOf("q1", "q2"), settled.savedQuestionIds)
    }

    @Test
    fun aFailedMutationLeavesThePersistedStateVisibleAndClearsPending() = runTest {
        val repository = FakeSavedQuestionRepository(listOf(SavedQuestion("q1", 1_000)))
        val holder = loadedHolder(repository)
        repository.failMutations = true

        holder.toggleSaved("q1")
        advanceUntilIdle()

        // The unsave failed, so the Question is still saved and still reads as saved.
        val loaded = assertIs<SavedQuestionsState.Loaded>(holder.state.value)
        assertEquals(setOf("q1"), loaded.savedQuestionIds)
        assertEquals(emptySet(), loaded.pendingQuestionIds)

        repository.failMutations = false
        holder.toggleSaved("q1")
        advanceUntilIdle()
        assertEquals(emptySet(), savedIds(holder))
    }

    @Test
    fun anUnreadableRepositoryIsErrorRatherThanAnEmptySavedSet() = runTest {
        val repository = FakeSavedQuestionRepository(listOf(SavedQuestion("q1", 1_000)))
        repository.failReads = true
        val holder = holder(repository)

        holder.refresh()
        advanceUntilIdle()

        // Not Loaded(emptyList()): an already-saved Question must not be presented as unsaved.
        assertIs<SavedQuestionsState.Error>(holder.state.value)
        holder.toggleSaved("q1")
        advanceUntilIdle()
        assertTrue(repository.saveCalls.isEmpty())
        assertTrue(repository.unsaveCalls.isEmpty())

        // Opening a review surface again retries, so the failure is not permanent for the session.
        repository.failReads = false
        holder.refresh()
        advanceUntilIdle()
        assertEquals(setOf("q1"), savedIds(holder))
    }

    @Test
    fun aFailedRefreshAfterASuccessfulOneKeepsTheKnownSavedState() = runTest {
        val repository = FakeSavedQuestionRepository(listOf(SavedQuestion("q1", 1_000)))
        val holder = loadedHolder(repository)

        repository.failReads = true
        holder.refresh()
        advanceUntilIdle()

        assertEquals(setOf("q1"), savedIds(holder))
    }

    @Test
    fun togglingBeforeSavedStateIsKnownPersistsNothing() = runTest {
        val repository = FakeSavedQuestionRepository()
        val holder = holder(repository)

        assertIs<SavedQuestionsState.Loading>(holder.state.value)
        holder.toggleSaved("q1")
        advanceUntilIdle()

        assertTrue(repository.saveCalls.isEmpty())
        assertFalse(repository.getSavedQuestions().any { it.questionId == "q1" })
    }

    @Test
    fun cancelledRefreshDoesNotBecomeAnError() = runTest {
        val repository = FakeSavedQuestionRepository()
        repository.readGate = CompletableDeferred()
        val owner = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val holder = SavedQuestionStateHolder(repository, owner)

        holder.refresh()
        runCurrent()
        owner.cancel()
        advanceUntilIdle()

        // The owner going away is not the repository failing, and must not be reported as it.
        assertIs<SavedQuestionsState.Loading>(holder.state.value)
    }

    @Test
    fun cancelledMutationIsNotSettledAsAnOperationalFailure() = runTest {
        val repository = FakeSavedQuestionRepository()
        val owner = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val holder = SavedQuestionStateHolder(repository, owner)
        holder.refresh()
        advanceUntilIdle()
        repository.saveGate = CompletableDeferred()

        holder.toggleSaved("q1")
        runCurrent()
        owner.cancel()
        advanceUntilIdle()

        val loaded = assertIs<SavedQuestionsState.Loaded>(holder.state.value)
        assertEquals(emptySet(), loaded.savedQuestionIds)
        assertEquals(setOf("q1"), loaded.pendingQuestionIds)
    }

    /**
     * A read issued before a write must not undo it afterwards.
     *
     * A review surface refreshes as it opens, so a Question saved or removed just after arriving has
     * a read in flight that predates the write. Before the read-back was ordered against those
     * reads, the late refresh overwrote the persisted result and the card was drawn in the state the
     * learner had just left.
     */
    @Test
    fun aReadIssuedBeforeAMutationDoesNotOverwriteItAfterwards() = runTest {
        val repository = FakeSavedQuestionRepository(listOf(SavedQuestion("q1", 1_000)))
        val holder = loadedHolder(repository)

        // A surface opens: its refresh samples the pre-write table and is held open.
        val staleRead = CompletableDeferred<Unit>()
        repository.staleReadGate = staleRead
        holder.refresh()
        advanceUntilIdle()

        // The learner removes the Question. The write and its read-back both complete.
        holder.toggleSaved("q1")
        advanceUntilIdle()
        assertEquals(listOf("q1"), repository.unsaveCalls)

        // Only now does the older read return.
        staleRead.complete(Unit)
        advanceUntilIdle()

        val settled = assertIs<SavedQuestionsState.Loaded>(holder.state.value)
        assertEquals(emptySet(), settled.savedQuestionIds)
        assertEquals(emptySet(), settled.pendingQuestionIds)
    }

    /**
     * Two cards, two different Questions, one after the other — which the per-Question pending
     * marker deliberately allows, because a mutation on one card must not disable another.
     *
     * Each mutation reads the whole table back, so before those reads were ordered the first one
     * could sample before the second write and publish after it, putting a Question the learner had
     * just removed back on screen while the database said otherwise.
     */
    @Test
    fun anOlderReadBackCannotRestoreAQuestionAnotherMutationRemoved() = runTest {
        val repository = FakeSavedQuestionRepository(listOf(SavedQuestion("q2", 2_000)))
        val holder = loadedHolder(repository)

        // Saving q1 writes, then holds its read-back open on the pre-removal snapshot.
        val staleReadBack = CompletableDeferred<Unit>()
        repository.staleReadGate = staleReadBack
        holder.toggleSaved("q1")
        advanceUntilIdle()

        // Removing q2 writes and reads back while the first read-back is still outstanding.
        holder.toggleSaved("q2")
        advanceUntilIdle()

        staleReadBack.complete(Unit)
        advanceUntilIdle()

        val settled = assertIs<SavedQuestionsState.Loaded>(holder.state.value)
        assertEquals(setOf("q1"), settled.savedQuestionIds)
        assertEquals(emptySet(), settled.pendingQuestionIds)
    }

    private fun TestScope.holder(repository: FakeSavedQuestionRepository) =
        savedQuestionStateHolder(repository)

    private fun TestScope.loadedHolder(repository: FakeSavedQuestionRepository) =
        holder(repository).also {
            it.refresh()
            advanceUntilIdle()
        }

    private fun savedIds(holder: SavedQuestionStateHolder): Set<String> =
        assertIs<SavedQuestionsState.Loaded>(holder.state.value).savedQuestionIds
}
