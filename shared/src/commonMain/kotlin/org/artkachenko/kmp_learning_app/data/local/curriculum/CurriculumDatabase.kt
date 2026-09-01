package org.artkachenko.kmp_learning_app.data.local.curriculum

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import org.artkachenko.kmp_learning_app.data.local.assessment.AssessmentAttemptDao
import org.artkachenko.kmp_learning_app.data.local.assessment.entity.QuestionAttemptEntity
import org.artkachenko.kmp_learning_app.data.local.assessment.entity.QuestionAttemptSelectedAnswerEntity
import org.artkachenko.kmp_learning_app.data.local.assessment.entity.TestAttemptEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.AnswerOptionEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionCorrectAnswerEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionSourceEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.SubtopicEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.TopicEntity

@Database(
    entities = [
        TopicEntity::class,
        SubtopicEntity::class,
        QuestionEntity::class,
        AnswerOptionEntity::class,
        QuestionCorrectAnswerEntity::class,
        QuestionSourceEntity::class,
        TestAttemptEntity::class,
        QuestionAttemptEntity::class,
        QuestionAttemptSelectedAnswerEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@ConstructedBy(CurriculumDatabaseConstructor::class)
internal abstract class CurriculumDatabase : RoomDatabase() {
    abstract fun curriculumDao(): CurriculumDao

    abstract fun assessmentAttemptDao(): AssessmentAttemptDao
}

@Suppress("KotlinNoActualForExpect")
internal expect object CurriculumDatabaseConstructor : RoomDatabaseConstructor<CurriculumDatabase> {
    override fun initialize(): CurriculumDatabase
}
