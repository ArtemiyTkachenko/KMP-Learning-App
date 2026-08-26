package org.artkachenko.kmp_learning_app.data.local.curriculum.repository

import androidx.room3.withReadTransaction
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
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

    override suspend fun getActiveQuestions(): List<Question> =
        database.withReadTransaction {
            val dao = database.curriculumDao()
            dao.getActiveQuestions(
                activeStatus = activeStatus,
            ).toDomainQuestions(dao)
        }

    override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
        database.withReadTransaction {
            val dao = database.curriculumDao()
            dao.getActiveQuestionsForTopic(
                topicId = topicId,
                activeStatus = activeStatus,
            ).toDomainQuestions(dao)
        }

    override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
        database.withReadTransaction {
            val dao = database.curriculumDao()
            dao.getActiveQuestionsForSubtopic(
                subtopicId = subtopicId,
                activeStatus = activeStatus,
            ).toDomainQuestions(dao)
        }

    override suspend fun getQuestionById(questionId: String): Question? =
        database.withReadTransaction {
            val dao = database.curriculumDao()
            val question = dao.getQuestionById(questionId) ?: return@withReadTransaction null
            listOf(question).toDomainQuestions(dao).single()
        }

    private suspend fun List<QuestionEntity>.toDomainQuestions(
        dao: CurriculumDao,
    ): List<Question> {
        if (isEmpty()) return emptyList()

        val questionIds = map { it.id }
        val answersByQuestion = dao.getAnswerOptionsForQuestions(questionIds)
            .groupBy { it.questionId }
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
