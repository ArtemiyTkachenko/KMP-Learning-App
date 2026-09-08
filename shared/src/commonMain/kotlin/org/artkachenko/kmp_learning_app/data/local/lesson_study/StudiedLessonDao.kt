package org.artkachenko.kmp_learning_app.data.local.lesson_study

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import org.artkachenko.kmp_learning_app.data.local.lesson_study.entity.StudiedLessonEntity

@Dao
internal interface StudiedLessonDao {
    /**
     * IGNORE rather than REPLACE: the first explicit mark is what established when the current
     * studied fact began, so re-marking must not move the recorded time forward.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(studiedLesson: StudiedLessonEntity)

    @Query("DELETE FROM studied_lesson WHERE lesson_id = :lessonId")
    suspend fun deleteByLessonId(lessonId: String)

    @Query("SELECT * FROM studied_lesson WHERE lesson_id = :lessonId")
    suspend fun getByLessonId(lessonId: String): StudiedLessonEntity?

    @Query(
        """
        SELECT *
        FROM studied_lesson
        ORDER BY studied_at_epoch_millis DESC, lesson_id ASC
        """,
    )
    suspend fun getAll(): List<StudiedLessonEntity>

    @Query("SELECT COUNT(*) FROM studied_lesson")
    suspend fun count(): Int
}
