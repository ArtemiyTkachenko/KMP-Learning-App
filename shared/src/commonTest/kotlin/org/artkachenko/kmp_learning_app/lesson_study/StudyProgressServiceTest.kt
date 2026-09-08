package org.artkachenko.kmp_learning_app.lesson_study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.lesson_study.repository.LessonStudyRepository

internal class StudyProgressServiceTest {
    @Test
    fun unitProgressIntersectsStoredIdentitiesWithCurrentActiveLessons() = runTest {
        val service = StudyProgressService(
            FakeLessonStudyRepository(
                // Newest-first with an orphan in the middle, as persistence really returns it.
                StudiedLesson(lessonId = "lesson_deleted", studiedAtEpochMillis = 300),
                StudiedLesson(lessonId = "lesson_b", studiedAtEpochMillis = 200),
                StudiedLesson(lessonId = "lesson_retired", studiedAtEpochMillis = 100),
            ),
        )

        val progress = service.deriveUnit(
            learningUnit(
                id = "unit_compose",
                lessons = listOf(
                    learningLesson("lesson_a"),
                    learningLesson("lesson_b"),
                    learningLesson("lesson_retired", status = ContentStatus.DEPRECATED),
                ),
            ),
        )

        assertEquals(
            listOf(
                LessonStudyProgress("lesson_a", isStudied = false),
                LessonStudyProgress("lesson_b", isStudied = true),
            ),
            progress.lessons,
        )
        assertEquals(StudyProgressSummary.Progress(studiedCount = 1, totalCount = 2), progress.summary)
    }

    @Test
    fun topicProgressReadsStudyStateOnceForTheWholeSnapshot() = runTest {
        val repository = FakeLessonStudyRepository(StudiedLesson("lesson_a", studiedAtEpochMillis = 1))
        val service = StudyProgressService(repository)

        val progress = service.deriveTopic(
            topicId = "android_ui",
            units = listOf(
                learningUnit(id = "unit_compose", lessons = listOf(learningLesson("lesson_a"))),
                learningUnit(id = "unit_state", lessons = listOf(learningLesson("lesson_b"))),
            ),
        )

        // Two Units, one read: sampling study state per Unit would let a Topic and its Units
        // disagree if a mark landed between them.
        assertEquals(1, repository.studiedLessonReads)
        assertEquals(StudyProgressSummary.Progress(studiedCount = 1, totalCount = 2), progress.summary)
    }

    @Test
    fun aStoredFactStaysReadableForALessonThatIsNoLongerActive() = runTest {
        val service = StudyProgressService(
            FakeLessonStudyRepository(StudiedLesson("lesson_retired", studiedAtEpochMillis = 1)),
        )

        // Persistence answers about the stored claim; current progress is the derivation's question,
        // and neither reinterprets nor deletes the record because the Lesson was retired.
        assertTrue(service.isLessonStudied("lesson_retired"))
        assertFalse(service.isLessonStudied("lesson_a"))
    }
}

private class FakeLessonStudyRepository(
    vararg studiedLessons: StudiedLesson,
) : LessonStudyRepository {
    private val studiedLessons = studiedLessons.toMutableList()

    var studiedLessonReads = 0
        private set

    override suspend fun markStudied(lessonId: String) {
        if (studiedLessons.none { it.lessonId == lessonId }) {
            studiedLessons += StudiedLesson(lessonId = lessonId, studiedAtEpochMillis = 0)
        }
    }

    override suspend fun unmarkStudied(lessonId: String) {
        studiedLessons.removeAll { it.lessonId == lessonId }
    }

    override suspend fun isStudied(lessonId: String): Boolean = studiedLessons.any { it.lessonId == lessonId }

    override suspend fun getStudiedLessons(): List<StudiedLesson> {
        studiedLessonReads++
        return studiedLessons.toList()
    }
}
