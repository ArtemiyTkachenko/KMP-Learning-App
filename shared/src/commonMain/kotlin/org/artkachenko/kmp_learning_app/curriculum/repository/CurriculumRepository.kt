package org.artkachenko.kmp_learning_app.curriculum.repository

import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic

internal interface CurriculumRepository {
    suspend fun getActiveTopics(): List<Topic>

    suspend fun getActiveSubtopics(topicId: String): List<Subtopic>

    suspend fun getActiveQuestions(): List<Question>

    suspend fun getActiveQuestionsByTopic(topicId: String): List<Question>

    suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question>

    suspend fun getTopicById(topicId: String): Topic?

    suspend fun getSubtopicById(subtopicId: String): Subtopic?

    suspend fun getQuestionById(questionId: String): Question?
}
