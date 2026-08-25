package org.artkachenko.kmp_learning_app.curriculum.content

import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.SourceReference

internal fun question(
    id: String,
    topicId: String,
    subtopicId: String,
    text: String,
    correctAnswerIds: List<String>,
    explanation: String,
    sources: List<SourceReference>,
    answers: List<AnswerOption>,
) = Question(
    id = id,
    topicId = topicId,
    subtopicId = subtopicId,
    text = text,
    answers = answers,
    correctAnswerIds = correctAnswerIds,
    explanation = explanation,
    sources = sources,
)

internal fun answer(id: String, text: String) = AnswerOption(
    id = id,
    text = text,
)

