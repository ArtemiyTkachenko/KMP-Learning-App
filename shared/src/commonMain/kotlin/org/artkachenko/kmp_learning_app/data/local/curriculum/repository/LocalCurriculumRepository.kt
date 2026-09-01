package org.artkachenko.kmp_learning_app.data.local.curriculum.repository

import androidx.room3.withReadTransaction
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDao
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionEntity

internal class LocalCurriculumRepository(
    private val database: CurriculumDatabase,
) : CurriculumRepository {
    private val activeStatus = ContentStatus.ACTIVE.name

    override suspend fun getActiveTopics(): List<Topic> =
        database.curriculumDao()
            .getTopicsByStatus(activeStatus)
            .map { it.toDomain() }

    override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> =
        database.curriculumDao()
            .getActiveSubtopicsForTopic(
                topicId = topicId,
                activeStatus = activeStatus,
            )
            .map { it.toDomain() }

    override suspend fun getTopicById(topicId: String): Topic? =
        database.curriculumDao()
            .getTopicById(topicId)
            ?.toDomain()

    override suspend fun getSubtopicById(subtopicId: String): Subtopic? =
        database.curriculumDao()
            .getSubtopicById(subtopicId)
            ?.toDomain()

    override suspend fun getActiveQuestions(): List<Question> =
        database.withReadTransaction {
            val dao = database.curriculumDao()
            dao.getActiveQuestions(
                activeStatus = activeStatus,
            ).toDomainQuestions(dao, includeRetiredAnswers = false)
        }

    override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
        database.withReadTransaction {
            val dao = database.curriculumDao()
            dao.getActiveQuestionsForTopic(
                topicId = topicId,
                activeStatus = activeStatus,
            ).toDomainQuestions(dao, includeRetiredAnswers = false)
        }

    override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
        database.withReadTransaction {
            val dao = database.curriculumDao()
            dao.getActiveQuestionsForSubtopic(
                subtopicId = subtopicId,
                activeStatus = activeStatus,
            ).toDomainQuestions(dao, includeRetiredAnswers = false)
        }

    override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> {
        val levelNames = levels.toLevelNames() ?: return emptyList()
        return database.withReadTransaction {
            val dao = database.curriculumDao()
            dao.getActiveQuestionsForLevels(
                levels = levelNames,
                activeStatus = activeStatus,
            ).toDomainQuestions(dao, includeRetiredAnswers = false)
        }
    }

    override suspend fun getActiveQuestionsByTopicAndLevels(
        topicId: String,
        levels: Set<QuestionLevel>,
    ): List<Question> {
        val levelNames = levels.toLevelNames() ?: return emptyList()
        return database.withReadTransaction {
            val dao = database.curriculumDao()
            dao.getActiveQuestionsForTopicAndLevels(
                topicId = topicId,
                levels = levelNames,
                activeStatus = activeStatus,
            ).toDomainQuestions(dao, includeRetiredAnswers = false)
        }
    }

    override suspend fun getActiveQuestionsBySubtopicAndLevels(
        subtopicId: String,
        levels: Set<QuestionLevel>,
    ): List<Question> {
        val levelNames = levels.toLevelNames() ?: return emptyList()
        return database.withReadTransaction {
            val dao = database.curriculumDao()
            dao.getActiveQuestionsForSubtopicAndLevels(
                subtopicId = subtopicId,
                levels = levelNames,
                activeStatus = activeStatus,
            ).toDomainQuestions(dao, includeRetiredAnswers = false)
        }
    }

    override suspend fun getQuestionById(questionId: String): Question? =
        database.withReadTransaction {
            val dao = database.curriculumDao()
            val question = dao.getQuestionById(questionId) ?: return@withReadTransaction null
            // Historical resolver: retired options are included so an attempt that
            // selected one can still be reviewed with its original answer text.
            listOf(question).toDomainQuestions(dao, includeRetiredAnswers = true).single()
        }

    /**
     * Maps a level selection onto the persisted column values, or `null` when nothing is
     * selected.
     *
     * An empty selection is answered here instead of in SQL: `IN ()` is a SQLite-specific
     * extension rather than portable SQL, and the four platform drivers should not have to
     * agree on it for the API's documented "empty selection matches nothing" contract to hold.
     */
    private fun Set<QuestionLevel>.toLevelNames(): List<String>? =
        if (isEmpty()) null else map { it.name }

    private suspend fun List<QuestionEntity>.toDomainQuestions(
        dao: CurriculumDao,
        includeRetiredAnswers: Boolean,
    ): List<Question> {
        if (isEmpty()) return emptyList()

        val questionIds = map { it.id }
        val answerOptions = if (includeRetiredAnswers) {
            dao.getAnswerOptionsForQuestions(questionIds)
        } else {
            dao.getActiveAnswerOptionsForQuestions(
                questionIds = questionIds,
                activeStatus = activeStatus,
            )
        }
        val answersByQuestion = answerOptions.groupBy { it.questionId }
        val correctAnswersByQuestion = dao.getCorrectAnswersForQuestions(questionIds)
            .groupBy { it.questionId }
        val sourcesByQuestion = dao.getSourcesForQuestions(questionIds)
            .groupBy { it.questionId }

        return map { question ->
            question.toDomain(
                answers = answersByQuestion[question.id].orEmpty(),
                correctAnswerIds = correctAnswersByQuestion[question.id].orEmpty()
                    .map { it.answerId },
                sources = sourcesByQuestion[question.id].orEmpty(),
            )
        }
    }
}
