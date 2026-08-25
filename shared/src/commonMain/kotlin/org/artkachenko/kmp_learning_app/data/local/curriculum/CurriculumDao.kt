package org.artkachenko.kmp_learning_app.data.local.curriculum

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.AnswerOptionEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionCorrectAnswerEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionSourceEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.SubtopicEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.TopicEntity

@Dao
internal interface CurriculumDao {
    @Upsert
    suspend fun upsertTopics(topics: List<TopicEntity>)

    @Upsert
    suspend fun upsertSubtopics(subtopics: List<SubtopicEntity>)

    @Upsert
    suspend fun upsertQuestions(questions: List<QuestionEntity>)

    @Upsert
    suspend fun upsertAnswerOptions(answerOptions: List<AnswerOptionEntity>)

    @Upsert
    suspend fun upsertCorrectAnswers(correctAnswers: List<QuestionCorrectAnswerEntity>)

    @Upsert
    suspend fun upsertQuestionSources(sources: List<QuestionSourceEntity>)

    @Query("DELETE FROM question_correct_answer WHERE question_id IN (:questionIds)")
    suspend fun deleteCorrectAnswersForQuestions(questionIds: List<String>)

    @Query("DELETE FROM question_source WHERE question_id IN (:questionIds)")
    suspend fun deleteQuestionSourcesForQuestions(questionIds: List<String>)

    @Query("SELECT * FROM topic WHERE id = :id")
    suspend fun getTopicById(id: String): TopicEntity?

    @Query("SELECT * FROM subtopic WHERE id = :id")
    suspend fun getSubtopicById(id: String): SubtopicEntity?

    @Query("SELECT * FROM question WHERE id = :id")
    suspend fun getQuestionById(id: String): QuestionEntity?

    @Query("SELECT * FROM answer_option WHERE question_id = :questionId ORDER BY sort_order")
    suspend fun getAnswerOptionsForQuestion(questionId: String): List<AnswerOptionEntity>

    @Query("SELECT answer_id FROM question_correct_answer WHERE question_id = :questionId ORDER BY answer_id")
    suspend fun getCorrectAnswerIdsForQuestion(questionId: String): List<String>

    @Query("SELECT * FROM question_source WHERE question_id = :questionId ORDER BY sort_order")
    suspend fun getSourcesForQuestion(questionId: String): List<QuestionSourceEntity>

    @Query("SELECT COUNT(*) FROM topic")
    suspend fun countTopics(): Int

    @Query("SELECT COUNT(*) FROM subtopic")
    suspend fun countSubtopics(): Int

    @Query("SELECT COUNT(*) FROM question")
    suspend fun countQuestions(): Int

    @Query("SELECT COUNT(*) FROM answer_option")
    suspend fun countAnswerOptions(): Int

    @Query("SELECT COUNT(*) FROM question_correct_answer")
    suspend fun countCorrectAnswers(): Int

    @Query("SELECT COUNT(*) FROM question_source")
    suspend fun countQuestionSources(): Int
}
