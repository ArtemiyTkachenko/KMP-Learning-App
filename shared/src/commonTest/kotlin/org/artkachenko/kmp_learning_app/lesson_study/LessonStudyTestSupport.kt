package org.artkachenko.kmp_learning_app.lesson_study

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.artkachenko.kmp_learning_app.lesson_study.repository.LessonStudyRepository

/**
 * An app-scoped study projection for tests, on the test scheduler.
 *
 * Deliberately not `backgroundScope`: work launched there is not run by `advanceUntilIdle`, so a
 * refresh or a mutation would silently never complete.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun TestScope.studyProgressStateHolder(
    repository: LessonStudyRepository = FakeLessonStudyRepository(),
): StudyProgressStateHolder =
    StudyProgressStateHolder(
        repository = repository,
        scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler)),
    )

/**
 * In-memory stand-in for the persisted `studied_lesson` table, shared by the derivation, service,
 * holder, and Learn presentation tests.
 *
 * It mirrors the persisted contract callers depend on: a repeated mark neither duplicates the
 * identity nor changes its original recorded time, and unmarking removes the row. Reads and writes
 * are counted so a screen that quietly added a repository subscription of its own — instead of
 * observing the one app-scoped projection — shows up as extra calls, and so the tests that forbid
 * opening a Lesson from persisting anything can assert on silence rather than on absence.
 */
internal class FakeLessonStudyRepository(
    vararg initial: StudiedLesson,
) : LessonStudyRepository {
    private val studiedLessons = initial.toMutableList()

    val markCalls = mutableListOf<String>()
    val unmarkCalls = mutableListOf<String>()

    var studiedLessonReads = 0
        private set

    /** Every read the holder makes goes through [getStudiedLessons]; this stays at zero. */
    var isStudiedCalls = 0
        private set

    var failReads = false
    var failMutations = false

    /** When set, a write suspends until it completes, so a pending mutation can be observed. */
    var writeGate: CompletableDeferred<Unit>? = null

    /** The same for a read, so a surface can be observed while study state is still Loading. */
    var readGate: CompletableDeferred<Unit>? = null

    override suspend fun markStudied(lessonId: String) {
        markCalls += lessonId
        writeGate?.await()
        if (failMutations) error("Mark studied failed.")
        if (studiedLessons.none { it.lessonId == lessonId }) {
            studiedLessons.add(0, StudiedLesson(lessonId = lessonId, studiedAtEpochMillis = 9_000))
        }
    }

    override suspend fun unmarkStudied(lessonId: String) {
        unmarkCalls += lessonId
        writeGate?.await()
        if (failMutations) error("Unmark studied failed.")
        studiedLessons.removeAll { it.lessonId == lessonId }
    }

    override suspend fun isStudied(lessonId: String): Boolean {
        isStudiedCalls += 1
        return studiedLessons.any { it.lessonId == lessonId }
    }

    override suspend fun getStudiedLessons(): List<StudiedLesson> {
        studiedLessonReads += 1
        readGate?.await()
        if (failReads) error("Study state unavailable.")
        return studiedLessons.toList()
    }
}
