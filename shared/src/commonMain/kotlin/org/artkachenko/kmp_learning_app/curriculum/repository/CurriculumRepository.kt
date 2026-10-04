package org.artkachenko.kmp_learning_app.curriculum.repository

import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic

/**
 * Curriculum reads for practice selection, historical review, and stored references.
 *
 * The `getActive*` functions are the eligibility surface: they return only ACTIVE content and
 * are what practice flows select from. The two stable-ID Question reads stay deliberately outside
 * that filtering, so a DEPRECATED Question a learner still references can be read back. They
 * differ only in which AnswerOptions a resolved Question carries:
 *
 * - [getQuestionsByIds] reconstructs history. It also returns AnswerOptions the curriculum has
 *   retired but kept because a stored attempt selected them.
 * - [getQuestionsByIdsForCurrentContent] presents what the curriculum authors now for a stable
 *   identity. It returns only the currently authored AnswerOptions.
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

    /**
     * The historical Question resolver, keyed by the stable ID that resolved each entry.
     *
     * An absent key means the curriculum no longer holds that identity; it is deliberately not an
     * empty [Question], so a caller still distinguishes "missing" from "unreadable", which throws.
     * Duplicates in [questionIds] collapse, and an empty collection returns an empty map without
     * reading anything.
     *
     * Question status is not filtered, and the AnswerOptions include retired ones a stored
     * attempt selected, so an attempt can still show the text of the option it chose. Text,
     * correct-answer key, explanation and sources are always the current ones: no historical
     * version of them is stored. Use this where stored answer IDs must stay resolvable — review,
     * resume, history — and [getQuestionsByIdsForCurrentContent] where none exist.
     *
     * There is deliberately no single-ID variant. Every caller — the progress derivation, the
     * mistake queue, assessment review, the saved list, resuming an attempt — knows the whole set
     * of identities it needs before it reads any of them, and in the Room-backed repository a
     * per-ID read costs a read transaction and four statements *each*. Offering both shapes would
     * also mean an implementation could answer them inconsistently, and a decorator written with
     * Kotlin's `by` delegation could quietly bypass an override of one of them.
     */
    suspend fun getQuestionsByIds(questionIds: Collection<String>): Map<String, Question>

    /**
     * The current authored content of each stable Question identity, keyed by that ID.
     *
     * Question status is not filtered — a DEPRECATED Question still resolves — but the
     * AnswerOptions are only the ones the curriculum authors now. An option kept solely because a
     * stored attempt selected it is left out, because there is no attempt here for it to explain.
     * Missing, duplicate and empty input behave exactly as in [getQuestionsByIds].
     */
    suspend fun getQuestionsByIdsForCurrentContent(
        questionIds: Collection<String>,
    ): Map<String, Question>
}
