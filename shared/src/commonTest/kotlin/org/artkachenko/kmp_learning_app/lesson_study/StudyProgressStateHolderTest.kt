package org.artkachenko.kmp_learning_app.lesson_study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/**
 * The app-scoped study projection every Learn surface observes.
 *
 * The behaviour under test is not "does a Boolean flip" but the contract that lets three live
 * destinations share one truth: the database decides what is visible, a write is never drawn before
 * it is persisted, and a transient failure never repaints a studied Lesson as unstudied.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class StudyProgressStateHolderTest {
    @Test
    fun loadedStateKeepsTheRepositoryListAndExposesItsIdentities() = runTest {
        val repository = FakeLessonStudyRepository(
            StudiedLesson("lesson_c", 3_000),
            StudiedLesson("lesson_a", 1_000),
        )
        val holder = studyProgressStateHolder(repository)

        holder.refresh()
        advanceUntilIdle()

        val loaded = assertIs<StudyProgressState.Loaded>(holder.state.value)
        assertEquals(listOf("lesson_c", "lesson_a"), loaded.studiedLessons.map { it.lessonId })
        assertEquals(setOf("lesson_c", "lesson_a"), loaded.studiedLessonIds)
        assertEquals(emptySet(), loaded.pendingLessonIds)
    }

    /** A learner who has studied nothing is a successful answer, not a failure to answer. */
    @Test
    fun anEmptyStudyTableIsLoadedRatherThanError() = runTest {
        val holder = studyProgressStateHolder(FakeLessonStudyRepository())

        holder.refresh()
        advanceUntilIdle()

        val loaded = assertIs<StudyProgressState.Loaded>(holder.state.value)
        assertEquals(emptySet(), loaded.studiedLessonIds)
    }

    @Test
    fun anUnreadableStudyTableIsErrorRatherThanAnEmptyStudiedSet() = runTest {
        val repository = FakeLessonStudyRepository(StudiedLesson("lesson_a", 1_000))
        repository.failReads = true
        val holder = studyProgressStateHolder(repository)

        holder.refresh()
        advanceUntilIdle()

        // Not Loaded(emptyList()): a Lesson the learner already studied must never be presented as
        // unstudied because the record could not be read.
        assertEquals(StudyProgressState.Error, holder.state.value)
    }

    @Test
    fun aFailedReReadKeepsTheLastSuccessfullyReadState() = runTest {
        val repository = FakeLessonStudyRepository(StudiedLesson("lesson_a", 1_000))
        val holder = loadedHolder(repository)

        repository.failReads = true
        holder.refresh()
        advanceUntilIdle()

        val loaded = assertIs<StudyProgressState.Loaded>(holder.state.value)
        assertEquals(setOf("lesson_a"), loaded.studiedLessonIds)
    }

    @Test
    fun aFailedInitialReadRecoversOnALaterRefresh() = runTest {
        val repository = FakeLessonStudyRepository(StudiedLesson("lesson_a", 1_000))
        repository.failReads = true
        val holder = studyProgressStateHolder(repository)
        holder.refresh()
        advanceUntilIdle()
        assertEquals(StudyProgressState.Error, holder.state.value)

        repository.failReads = false
        holder.refresh()
        advanceUntilIdle()

        assertEquals(setOf("lesson_a"), studiedIds(holder))
    }

    /** Every Learn destination refreshes on open; overlapping opens must not each query the table. */
    @Test
    fun concurrentRefreshesShareOneRead() = runTest {
        val repository = FakeLessonStudyRepository()
        val holder = studyProgressStateHolder(repository)

        holder.refresh()
        holder.refresh()
        holder.refresh()
        advanceUntilIdle()

        assertEquals(1, repository.studiedLessonReads)
    }

    @Test
    fun markingAnUnstudiedLessonPersistsItAndThenShowsItAsStudied() = runTest {
        val repository = FakeLessonStudyRepository(StudiedLesson("lesson_a", 1_000))
        val holder = loadedHolder(repository)

        holder.toggleStudied("lesson_b")
        advanceUntilIdle()

        assertEquals(listOf("lesson_b"), repository.markCalls)
        assertEquals(setOf("lesson_a", "lesson_b"), studiedIds(holder))
        // Persistence's own idempotency is the guarantee, so the holder never asks first.
        assertEquals(0, repository.isStudiedCalls)
    }

    @Test
    fun unmarkingAStudiedLessonReversesIt() = runTest {
        val repository = FakeLessonStudyRepository(
            StudiedLesson("lesson_a", 1_000),
            StudiedLesson("lesson_b", 2_000),
        )
        val holder = loadedHolder(repository)

        holder.toggleStudied("lesson_a")
        advanceUntilIdle()

        assertEquals(listOf("lesson_a"), repository.unmarkCalls)
        assertEquals(setOf("lesson_b"), studiedIds(holder))
    }

    /**
     * The load-bearing acceptance criterion: nothing visible may claim a state that was not
     * persisted, so the value stays put until the write and the read after it have both succeeded.
     */
    @Test
    fun aFailedMarkLeavesTheLessonUnstudied() = runTest {
        val repository = FakeLessonStudyRepository()
        val holder = loadedHolder(repository)
        repository.failMutations = true

        holder.toggleStudied("lesson_a")
        advanceUntilIdle()

        val loaded = assertIs<StudyProgressState.Loaded>(holder.state.value)
        assertEquals(emptySet(), loaded.studiedLessonIds)
        assertEquals(emptySet(), loaded.pendingLessonIds)
    }

    @Test
    fun aFailedUnmarkLeavesTheLessonStudied() = runTest {
        val repository = FakeLessonStudyRepository(StudiedLesson("lesson_a", 1_000))
        val holder = loadedHolder(repository)
        repository.failMutations = true

        holder.toggleStudied("lesson_a")
        advanceUntilIdle()

        val loaded = assertIs<StudyProgressState.Loaded>(holder.state.value)
        assertEquals(setOf("lesson_a"), loaded.studiedLessonIds)
        assertEquals(emptySet(), loaded.pendingLessonIds)

        repository.failMutations = false
        holder.toggleStudied("lesson_a")
        advanceUntilIdle()
        assertEquals(emptySet(), studiedIds(holder))
    }

    /**
     * The write landed but the read after it did not, so what persistence now holds is unknown to
     * this process. The holder keeps the last state it actually read rather than inventing the one
     * the write would have produced, and the next refresh is what discovers the truth. Documented
     * here because "visible state comes from read-back" is what makes that the correct answer
     * rather than a stale one.
     */
    @Test
    fun aSuccessfulWriteWhoseReadBackFailsKeepsTheLastKnownStateUntilTheNextRefresh() = runTest {
        val repository = FakeLessonStudyRepository()
        val holder = loadedHolder(repository)
        repository.failReads = true

        holder.toggleStudied("lesson_a")
        advanceUntilIdle()

        assertEquals(listOf("lesson_a"), repository.markCalls)
        val settled = assertIs<StudyProgressState.Loaded>(holder.state.value)
        assertEquals(emptySet(), settled.studiedLessonIds)
        assertEquals(emptySet(), settled.pendingLessonIds)

        repository.failReads = false
        holder.refresh()
        advanceUntilIdle()

        assertEquals(setOf("lesson_a"), studiedIds(holder))
    }

    @Test
    fun aSecondTapOnThePendingLessonIsIgnoredWhileOtherLessonsStayUsable() = runTest {
        val repository = FakeLessonStudyRepository()
        val gate = CompletableDeferred<Unit>()
        repository.writeGate = gate
        val holder = loadedHolder(repository)

        holder.toggleStudied("lesson_a")
        advanceUntilIdle()
        val pending = assertIs<StudyProgressState.Loaded>(holder.state.value)
        assertEquals(setOf("lesson_a"), pending.pendingLessonIds)
        // The value the learner sees is still the persisted one while the write is in flight.
        assertEquals(emptySet(), pending.studiedLessonIds)

        holder.toggleStudied("lesson_a")
        holder.toggleStudied("lesson_a")
        advanceUntilIdle()
        assertEquals(listOf("lesson_a"), repository.markCalls)

        // A different Lesson is untouched by the one in flight.
        holder.toggleStudied("lesson_b")
        advanceUntilIdle()
        assertEquals(listOf("lesson_a", "lesson_b"), repository.markCalls)

        gate.complete(Unit)
        advanceUntilIdle()
        val settled = assertIs<StudyProgressState.Loaded>(holder.state.value)
        assertEquals(emptySet(), settled.pendingLessonIds)
        assertEquals(setOf("lesson_a", "lesson_b"), settled.studiedLessonIds)
    }

    /**
     * A read that reached the database before a write must not publish its older snapshot after it.
     *
     * Opening a Learn destination calls [StudyProgressStateHolder.refresh], so a learner who marks a
     * Lesson studied just after arriving has a read in flight that predates the write. Before the
     * read-back was ordered against those reads, the late refresh overwrote the persisted result and
     * the Lesson was drawn as unstudied while the database said otherwise — a state this holder
     * exists to make impossible, arriving from the read side rather than the write side.
     */
    @Test
    fun aReadIssuedBeforeAWriteDoesNotOverwriteItAfterwards() = runTest {
        val repository = FakeLessonStudyRepository()
        val holder = loadedHolder(repository)

        // A destination opens: its refresh reads the pre-write snapshot and is held open.
        val staleRead = CompletableDeferred<Unit>()
        repository.staleReadGate = staleRead
        holder.refresh()
        advanceUntilIdle()

        // The learner marks the Lesson. The write and its read-back both complete.
        holder.toggleStudied("lesson_a")
        advanceUntilIdle()
        assertEquals(listOf("lesson_a"), repository.markCalls)

        // Only now does the older read return.
        staleRead.complete(Unit)
        advanceUntilIdle()

        val settled = assertIs<StudyProgressState.Loaded>(holder.state.value)
        assertEquals(setOf("lesson_a"), settled.studiedLessonIds)
        assertEquals(emptySet(), settled.pendingLessonIds)
    }

    /** A toggle needs a persisted value to reverse; guessing one is the fabrication being forbidden. */
    @Test
    fun aToggleIsIgnoredWhileStudyStateIsUnknown() = runTest {
        val repository = FakeLessonStudyRepository()
        val holder = studyProgressStateHolder(repository)

        holder.toggleStudied("lesson_a")
        advanceUntilIdle()
        assertEquals(StudyProgressState.Loading, holder.state.value)

        repository.failReads = true
        holder.refresh()
        advanceUntilIdle()
        holder.toggleStudied("lesson_a")
        advanceUntilIdle()

        assertEquals(StudyProgressState.Error, holder.state.value)
        assertEquals(emptyList(), repository.markCalls)
        assertEquals(emptyList(), repository.unmarkCalls)
    }

    private fun TestScope.loadedHolder(
        repository: FakeLessonStudyRepository,
    ): StudyProgressStateHolder =
        studyProgressStateHolder(repository).also {
            it.refresh()
            advanceUntilIdle()
        }

    private fun studiedIds(holder: StudyProgressStateHolder): Set<String> =
        assertIs<StudyProgressState.Loaded>(holder.state.value).studiedLessonIds
}
