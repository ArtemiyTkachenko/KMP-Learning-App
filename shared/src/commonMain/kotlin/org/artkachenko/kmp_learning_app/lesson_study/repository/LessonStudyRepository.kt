package org.artkachenko.kmp_learning_app.lesson_study.repository

import org.artkachenko.kmp_learning_app.lesson_study.StudiedLesson

/**
 * Stores explicitly marked Lesson identity only; Lesson content remains owned by
 * LearningContentRepository.
 *
 * Marking an already-studied Lesson is a no-op, including preserving its original time. Unmarking
 * removes the record rather than writing a second historical event, so a later mark records a new
 * time. No operation consults current learning content: a marked ID may resolve to an ACTIVE or
 * DEPRECATED Lesson, or to nothing at all, and a record whose Lesson no longer resolves stays
 * readable and removable by its stable ID. Whether such a record counts toward current progress is
 * a derivation question, not a persistence one.
 */
internal interface LessonStudyRepository {
    suspend fun markStudied(lessonId: String)

    suspend fun unmarkStudied(lessonId: String)

    suspend fun isStudied(lessonId: String): Boolean

    /** Most recently newly marked first, with stable Lesson id as the tie-breaker. */
    suspend fun getStudiedLessons(): List<StudiedLesson>
}
