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

    /**
     * Removes answer options that the incoming curriculum no longer authors for [questionId].
     *
     * Scoped per question because answer identity is the composite (question_id, id):
     * CurriculumValidator only enforces answer-id uniqueness within a question, so the
     * same answer id may legitimately belong to a different question.
     *
     * Options that a historical attempt selected are kept. question_attempt_selected_answer
     * has a NO ACTION foreign key onto answer_option(question_id, id), so deleting a
     * referenced row would abort the whole import transaction and leave the app unable to
     * start. The tradeoff is that such a retained option can still appear in a new
     * assessment for that question; historical review integrity is worth more.
     *
     * [keepAnswerIds] is never empty: callers derive it from the answer options they just
     * wrote, and CurriculumValidator requires at least two answers per question.
     */
    @Query(
        """
        DELETE FROM answer_option
        WHERE question_id = :questionId
            AND id NOT IN (:keepAnswerIds)
            AND NOT EXISTS (
                SELECT 1
                FROM question_attempt_selected_answer selected
                WHERE selected.question_id = answer_option.question_id
                    AND selected.answer_id = answer_option.id
            )
        """,
    )
    suspend fun deleteAnswerOptionsForQuestionExcept(
        questionId: String,
        keepAnswerIds: List<String>,
    )

    /**
     * Retires the answer options that survived [deleteAnswerOptionsForQuestionExcept]
     * because a historical attempt still selects them.
     *
     * Renaming an AnswerOption id is a content operation, but the old row lives on in
     * the database. Without this the retired option would keep appearing as an extra
     * choice in new assessments, so it is marked DEPRECATED and filtered out of active
     * curriculum queries while remaining resolvable through getQuestionById.
     */
    @Query(
        """
        UPDATE answer_option
        SET status = :deprecatedStatus
        WHERE question_id = :questionId
            AND id NOT IN (:keepAnswerIds)
        """,
    )
    suspend fun deprecateAnswerOptionsForQuestionExcept(
        questionId: String,
        keepAnswerIds: List<String>,
        deprecatedStatus: String,
    )

    @Query("SELECT * FROM topic WHERE id = :id")
    suspend fun getTopicById(id: String): TopicEntity?

    @Query("SELECT * FROM topic WHERE status = :status ORDER BY sort_order")
    suspend fun getTopicsByStatus(status: String): List<TopicEntity>

    @Query("SELECT * FROM subtopic WHERE id = :id")
    suspend fun getSubtopicById(id: String): SubtopicEntity?

    @Query(
        """
        SELECT s.*
        FROM subtopic s
        JOIN topic t
            ON t.id = s.topic_id
        WHERE s.topic_id = :topicId
            AND s.status = :activeStatus
            AND t.status = :activeStatus
        ORDER BY s.sort_order
        """,
    )
    suspend fun getActiveSubtopicsForTopic(
        topicId: String,
        activeStatus: String,
    ): List<SubtopicEntity>

    @Query("SELECT * FROM question WHERE id = :id")
    suspend fun getQuestionById(id: String): QuestionEntity?

    @Query(
        """
        SELECT q.*
        FROM question q
        JOIN topic t
            ON t.id = q.topic_id
        JOIN subtopic s
            ON s.id = q.subtopic_id
            AND s.topic_id = q.topic_id
        WHERE q.status = :activeStatus
            AND s.status = :activeStatus
            AND t.status = :activeStatus
        ORDER BY q.sort_order
        """,
    )
    suspend fun getActiveQuestions(
        activeStatus: String,
    ): List<QuestionEntity>

    @Query(
        """
        SELECT q.*
        FROM question q
        JOIN topic t
            ON t.id = q.topic_id
        JOIN subtopic s
            ON s.id = q.subtopic_id
            AND s.topic_id = q.topic_id
        WHERE q.topic_id = :topicId
            AND q.status = :activeStatus
            AND s.status = :activeStatus
            AND t.status = :activeStatus
        ORDER BY q.sort_order
        """,
    )
    suspend fun getActiveQuestionsForTopic(
        topicId: String,
        activeStatus: String,
    ): List<QuestionEntity>

    @Query(
        """
        SELECT q.*
        FROM question q
        JOIN topic t
            ON t.id = q.topic_id
        JOIN subtopic s
            ON s.id = q.subtopic_id
            AND s.topic_id = q.topic_id
        WHERE q.subtopic_id = :subtopicId
            AND q.status = :activeStatus
            AND s.status = :activeStatus
            AND t.status = :activeStatus
        ORDER BY q.sort_order
        """,
    )
    suspend fun getActiveQuestionsForSubtopic(
        subtopicId: String,
        activeStatus: String,
    ): List<QuestionEntity>

    /**
     * Level-aware variants of the active-question queries.
     *
     * [levels] is matched with `IN`, so several selected levels mean inclusive OR. Filtering
     * stays in SQL so callers never have to load the whole active bank to narrow it by level.
     * Callers must not pass an empty [levels] collection; LocalCurriculumRepository resolves
     * that case before it reaches Room.
     */
    @Query(
        """
        SELECT q.*
        FROM question q
        JOIN topic t
            ON t.id = q.topic_id
        JOIN subtopic s
            ON s.id = q.subtopic_id
            AND s.topic_id = q.topic_id
        WHERE q.status = :activeStatus
            AND s.status = :activeStatus
            AND t.status = :activeStatus
            AND q.level IN (:levels)
        ORDER BY q.sort_order
        """,
    )
    suspend fun getActiveQuestionsForLevels(
        levels: List<String>,
        activeStatus: String,
    ): List<QuestionEntity>

    @Query(
        """
        SELECT q.*
        FROM question q
        JOIN topic t
            ON t.id = q.topic_id
        JOIN subtopic s
            ON s.id = q.subtopic_id
            AND s.topic_id = q.topic_id
        WHERE q.topic_id = :topicId
            AND q.status = :activeStatus
            AND s.status = :activeStatus
            AND t.status = :activeStatus
            AND q.level IN (:levels)
        ORDER BY q.sort_order
        """,
    )
    suspend fun getActiveQuestionsForTopicAndLevels(
        topicId: String,
        levels: List<String>,
        activeStatus: String,
    ): List<QuestionEntity>

    @Query(
        """
        SELECT q.*
        FROM question q
        JOIN topic t
            ON t.id = q.topic_id
        JOIN subtopic s
            ON s.id = q.subtopic_id
            AND s.topic_id = q.topic_id
        WHERE q.subtopic_id = :subtopicId
            AND q.status = :activeStatus
            AND s.status = :activeStatus
            AND t.status = :activeStatus
            AND q.level IN (:levels)
        ORDER BY q.sort_order
        """,
    )
    suspend fun getActiveQuestionsForSubtopicAndLevels(
        subtopicId: String,
        levels: List<String>,
        activeStatus: String,
    ): List<QuestionEntity>

    @Query("SELECT * FROM answer_option WHERE question_id = :questionId ORDER BY sort_order")
    suspend fun getAnswerOptionsForQuestion(questionId: String): List<AnswerOptionEntity>

    @Query("SELECT * FROM answer_option WHERE question_id IN (:questionIds) ORDER BY question_id, sort_order")
    suspend fun getAnswerOptionsForQuestions(questionIds: List<String>): List<AnswerOptionEntity>

    @Query(
        """
        SELECT *
        FROM answer_option
        WHERE question_id IN (:questionIds)
            AND status = :activeStatus
        ORDER BY question_id, sort_order
        """,
    )
    suspend fun getActiveAnswerOptionsForQuestions(
        questionIds: List<String>,
        activeStatus: String,
    ): List<AnswerOptionEntity>

    @Query("SELECT answer_id FROM question_correct_answer WHERE question_id = :questionId ORDER BY answer_id")
    suspend fun getCorrectAnswerIdsForQuestion(questionId: String): List<String>

    @Query("SELECT * FROM question_correct_answer WHERE question_id IN (:questionIds) ORDER BY question_id, answer_id")
    suspend fun getCorrectAnswersForQuestions(questionIds: List<String>): List<QuestionCorrectAnswerEntity>

    @Query("SELECT * FROM question_source WHERE question_id = :questionId ORDER BY sort_order")
    suspend fun getSourcesForQuestion(questionId: String): List<QuestionSourceEntity>

    @Query("SELECT * FROM question_source WHERE question_id IN (:questionIds) ORDER BY question_id, sort_order")
    suspend fun getSourcesForQuestions(questionIds: List<String>): List<QuestionSourceEntity>

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
