package org.artkachenko.kmp_learning_app.data.local.curriculum.repository

import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.AnswerOptionEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionSourceEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.SubtopicEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.TopicEntity

internal fun TopicEntity.toDomain(): Topic =
    Topic(
        id = id,
        name = name,
        status = status.toContentStatus(),
    )

internal fun SubtopicEntity.toDomain(): Subtopic =
    Subtopic(
        id = id,
        topicId = topicId,
        name = name,
        status = status.toContentStatus(),
    )

internal fun QuestionEntity.toDomain(
    answers: List<AnswerOptionEntity>,
    correctAnswerIds: List<String>,
    sources: List<QuestionSourceEntity>,
): Question =
    Question(
        id = id,
        topicId = topicId,
        subtopicId = subtopicId,
        text = text,
        answers = answers.map { it.toDomain() },
        selectionMode = AnswerSelectionMode.valueOf(selectionMode),
        correctAnswerIds = correctAnswerIds,
        explanation = explanation,
        sources = sources.map { it.toDomain() },
        status = status.toContentStatus(),
    )

private fun AnswerOptionEntity.toDomain(): AnswerOption =
    AnswerOption(
        id = id,
        text = text,
    )

private fun QuestionSourceEntity.toDomain(): SourceReference =
    SourceReference(
        title = title,
        url = url,
    )

private fun String.toContentStatus(): ContentStatus =
    ContentStatus.valueOf(this)
