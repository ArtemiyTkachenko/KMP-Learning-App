package org.artkachenko.kmp_learning_app.data.local.curriculum.importer

import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.AnswerOptionEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionCorrectAnswerEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionSourceEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.SubtopicEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.TopicEntity

internal data class CurriculumPersistenceSnapshot(
    val topics: List<TopicEntity>,
    val subtopics: List<SubtopicEntity>,
    val questions: List<QuestionEntity>,
    val answerOptions: List<AnswerOptionEntity>,
    val correctAnswers: List<QuestionCorrectAnswerEntity>,
    val sources: List<QuestionSourceEntity>,
)
