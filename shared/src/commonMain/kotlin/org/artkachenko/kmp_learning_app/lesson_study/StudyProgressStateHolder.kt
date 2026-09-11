package org.artkachenko.kmp_learning_app.lesson_study

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.artkachenko.kmp_learning_app.lesson_study.repository.LessonStudyRepository

/**
 * App-scoped studied-Lesson state, shared by every Learn surface.
 *
 * Navigation 3 keeps the Learn back stack alive, so marking a Lesson studied in the reader happens
 * while the Learning Unit overview and Topic Detail that led there still exist. If each of those
 * destinations read the study table for itself, going back would show two stale screens above a
 * fresh one — three caches of one truth. Holding the projection here means one, outliving any
 * navigation entry, and a mark made in a child updates the parents already on the stack.
 *
 * [repository] remains the source of truth. Nothing is stored here that the database does not
 * already hold: this is the in-memory projection the UI observes, read back from the repository
 * after every mutation rather than assembled independently. Deriving Unit and Topic progress from
 * the identities published here is presentation's job, through the pure
 * [StudyProgressDerivation] — one persisted snapshot feeding every visible figure, rather than each
 * screen sampling Room again and letting a Unit disagree with its Topic.
 */
internal class StudyProgressStateHolder(
    private val repository: LessonStudyRepository,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<StudyProgressState>(StudyProgressState.Loading)
    val state: StateFlow<StudyProgressState> = _state.asStateFlow()

    /**
     * Held for the duration of a read so concurrent Learn entries share one query, not one each,
     * and so a read can never publish a snapshot older than one already published.
     *
     * A mutation's read-back takes it too. Without that, a [refresh] read that reached the database
     * *before* a write could return *after* it and overwrite the persisted result with its own
     * older snapshot — leaving a Lesson the learner had just marked drawn as unstudied while the
     * database said otherwise. The write itself stays outside the lock, so a mutation on one Lesson
     * is still never delayed by a mutation on another.
     */
    private val reading = Mutex()

    /**
     * Reads studied state, unless a read is already running.
     *
     * Called when a Learn destination opens rather than once at startup: the read is a single small
     * table scan, and repeating it is what lets a surface recover from an earlier failed read
     * instead of leaving the mark control unavailable for the rest of the session. Publisher
     * content never waits on it — the two load independently.
     */
    fun refresh() {
        if (!reading.tryLock()) return
        scope.launch {
            try {
                runCatching { repository.getStudiedLessons() }.fold(
                    onSuccess = { studied ->
                        _state.update { current ->
                            when (current) {
                                // A mutation in flight keeps its pending marker across the refresh.
                                is StudyProgressState.Loaded -> current.copy(studiedLessons = studied)
                                else -> StudyProgressState.Loaded(studied)
                            }
                        }
                    },
                    // A failed re-read leaves an earlier successful one in place: a transient
                    // failure must not repaint every Lesson in the Learn stack as unstudied.
                    onFailure = {
                        _state.update { current ->
                            if (current is StudyProgressState.Loaded) {
                                current
                            } else {
                                StudyProgressState.Error
                            }
                        }
                    },
                )
            } finally {
                reading.unlock()
            }
        }
    }

    /**
     * Marks an unstudied Lesson studied, or unmarks a studied one.
     *
     * Ignored while study state is unknown — a toggle needs a persisted value to reverse, and
     * guessing one would be the fabrication the whole state model exists to prevent — and while
     * this Lesson already has a write in flight, so repeated taps cannot launch competing
     * mutations. Persistence's own idempotency remains the final guarantee; this only keeps what
     * the learner sees predictable.
     *
     * The visible state changes only after the write succeeds and is then read back from the
     * repository, so the control never displays a studied state that was not persisted — and the
     * read-back is ordered against concurrent [refresh] reads, so nothing published afterwards can
     * revert it to a snapshot taken before the write.
     */
    fun toggleStudied(lessonId: String) {
        val loaded = _state.value as? StudyProgressState.Loaded ?: return
        if (lessonId in loaded.pendingLessonIds) return
        val mark = lessonId !in loaded.studiedLessonIds
        _state.update { current ->
            if (current is StudyProgressState.Loaded) {
                current.copy(pendingLessonIds = current.pendingLessonIds + lessonId)
            } else {
                current
            }
        }
        scope.launch {
            runCatching {
                if (mark) repository.markStudied(lessonId) else repository.unmarkStudied(lessonId)
                // The read-back is ordered against [refresh]'s reads; see [reading].
                reading.withLock { repository.getStudiedLessons() }
            }.fold(
                onSuccess = { studied -> settle(lessonId) { it.copy(studiedLessons = studied) } },
                // The write failed, or the read after it did. Either way the last state actually
                // read from the repository stands rather than a guess at what the write made true:
                // when only the read-back failed the database may well hold the new value, and the
                // next refresh is what discovers that. Inventing it here is what the read-back
                // contract exists to forbid.
                onFailure = { settle(lessonId) { it } },
            )
        }
    }

    private fun settle(
        lessonId: String,
        transform: (StudyProgressState.Loaded) -> StudyProgressState.Loaded,
    ) {
        _state.update { current ->
            if (current is StudyProgressState.Loaded) {
                transform(current).let { updated ->
                    updated.copy(pendingLessonIds = updated.pendingLessonIds - lessonId)
                }
            } else {
                current
            }
        }
    }
}
