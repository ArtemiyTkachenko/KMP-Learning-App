package org.artkachenko.kmp_learning_app.curriculum.repository

import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic

/**
 * Curriculum reads for practice selection and historical review.
 *
 * The `getActive*` functions are the eligibility surface: they return only ACTIVE content and
 * are what practice flows select from. [getQuestionById] is the historical resolver and stays
 * deliberately outside that filtering so a retired Question referenced by a stored attempt can
 * still be read back.
 *
 * The `...AndLevels` functions narrow that eligibility to authored interview depth. Passing
 * several levels means inclusive OR (`FOUNDATION + ADVANCED` returns Questions of either level,
 * not Questions that are somehow both). An empty selection therefore matches nothing and returns
 * an empty list, consistently on every scope; "any level" is expressed by calling the unfiltered
 * function rather than by passing an empty set.
 */
internal interface CurriculumRepository {
    suspend fun getActiveTopics(): List<Topic>

    suspend fun getActiveSubtopics(topicId: String): List<Subtopic>

    suspend fun getActiveQuestions(): List<Question>

    suspend fun getActiveQuestionsByTopic(topicId: String): List<Question>

    suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question>

    suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question>

    suspend fun getActiveQuestionsByTopicAndLevels(
        topicId: String,
        levels: Set<QuestionLevel>,
    ): List<Question>

    suspend fun getActiveQuestionsBySubtopicAndLevels(
        subtopicId: String,
        levels: Set<QuestionLevel>,
    ): List<Question>

    suspend fun getTopicById(topicId: String): Topic?

    suspend fun getSubtopicById(subtopicId: String): Subtopic?

    suspend fun getQuestionById(questionId: String): Question?
}
