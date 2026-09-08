package org.artkachenko.kmp_learning_app.data.local.lesson_study.repository

import kotlin.time.Clock
import kotlin.time.Instant
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.lesson_study.entity.StudiedLessonEntity
import org.artkachenko.kmp_learning_app.lesson_study.StudiedLesson
import org.artkachenko.kmp_learning_app.lesson_study.repository.LessonStudyRepository

internal class LocalLessonStudyRepository(
    private val database: CurriculumDatabase,
    private val now: () -> Instant = { Clock.System.now() },
) : LessonStudyRepository {
    override suspend fun markStudied(lessonId: String) {
        database.studiedLessonDao().insert(
            StudiedLessonEntity(
                lessonId = lessonId,
                studiedAtEpochMillis = now().toEpochMilliseconds(),
            ),
        )
    }

    override suspend fun unmarkStudied(lessonId: String) {
        database.studiedLessonDao().deleteByLessonId(lessonId)
    }

    override suspend fun isStudied(lessonId: String): Boolean =
        database.studiedLessonDao().getByLessonId(lessonId) != null

    override suspend fun getStudiedLessons(): List<StudiedLesson> =
        database.studiedLessonDao().getAll().map { studiedLesson ->
            StudiedLesson(
                lessonId = studiedLesson.lessonId,
                studiedAtEpochMillis = studiedLesson.studiedAtEpochMillis,
            )
        }
}
