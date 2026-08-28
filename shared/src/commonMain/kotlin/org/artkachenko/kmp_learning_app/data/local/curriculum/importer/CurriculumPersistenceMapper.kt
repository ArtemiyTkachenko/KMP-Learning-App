package org.artkachenko.kmp_learning_app.data.local.curriculum.importer

import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.AnswerOptionEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionCorrectAnswerEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionSourceEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.SubtopicEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.TopicEntity

internal fun Curriculum.toPersistenceSnapshot(): CurriculumPersistenceSnapshot =
    CurriculumPersistenceSnapshot(
        topics = topics.mapIndexed { index, topic ->
            TopicEntity(
                id = topic.id,
                name = topic.name,
                status = topic.status.name,
                sortOrder = index,
            )
        },
        subtopics = subtopics.mapIndexed { index, subtopic ->
            SubtopicEntity(
                id = subtopic.id,
                topicId = subtopic.topicId,
                name = subtopic.name,
                status = subtopic.status.name,
                sortOrder = index,
            )
        },
        questions = questions.mapIndexed { index, question ->
            QuestionEntity(
                id = question.id,
                topicId = question.topicId,
                subtopicId = question.subtopicId,
                text = question.text,
                explanation = question.explanation,
                status = question.status.name,
                sortOrder = index,
            )
        },
        answerOptions = questions.flatMap { question ->
            question.answers.mapIndexed { index, answer ->
                AnswerOptionEntity(
                    questionId = question.id,
                    id = answer.id,
                    text = answer.text,
                    sortOrder = index,
                    // Every authored option is active. Upserting this also reactivates an
                    // option that a previous bundle had retired and this one restores.
                    status = ContentStatus.ACTIVE.name,
                )
            }
        },
        correctAnswers = questions.flatMap { question ->
            question.correctAnswerIds.map { correctAnswerId ->
                QuestionCorrectAnswerEntity(
                    questionId = question.id,
                    answerId = correctAnswerId,
                )
            }
        },
        sources = questions.flatMap { question ->
            question.sources.mapIndexed { index, source ->
                QuestionSourceEntity(
                    questionId = question.id,
                    url = source.url,
                    title = source.title,
                    sortOrder = index,
                )
            }
        },
    )
