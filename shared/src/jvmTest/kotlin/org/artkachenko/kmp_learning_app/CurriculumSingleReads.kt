package org.artkachenko.kmp_learning_app

import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDao
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionEntity

/**
 * Reading one identity, for tests that assert what a single Question persisted or resolved to.
 *
 * These are extensions rather than members on purpose. Production resolves Questions only in
 * batches — every caller knows its whole set of identities before it reads any of them, and in the
 * Room-backed repository a per-id read costs a read transaction and four statements — so adding a
 * per-id member would put a shape in the contract that nothing uses, that an implementation could
 * answer inconsistently with the batched one, and that a `by`-delegating decorator could bypass an
 * override of. An extension cannot be overridden or delegated around, so a test reading one
 * identity here is provably reading it through the same code production reads many.
 */
internal suspend fun CurriculumRepository.getQuestionById(questionId: String): Question? =
    getQuestionsByIds(listOf(questionId))[questionId]

internal suspend fun CurriculumDao.getQuestionById(id: String): QuestionEntity? =
    getQuestionsByIds(listOf(id)).singleOrNull()
