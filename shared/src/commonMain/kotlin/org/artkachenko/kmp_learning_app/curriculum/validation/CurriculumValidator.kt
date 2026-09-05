package org.artkachenko.kmp_learning_app.curriculum.validation

import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic

internal class CurriculumValidator {
    fun validate(curriculum: Curriculum): List<CurriculumValidationError> {
        val errors = mutableListOf<CurriculumValidationError>()
        val topicIds = curriculum.topics.map { it.id }.toSet()
        val subtopicsById = curriculum.subtopics.associateBy { it.id }

        validateMinimumCoverage(curriculum, errors)
        validateTopics(curriculum.topics, errors)
        validateSubtopics(curriculum.subtopics, topicIds, errors)
        validateQuestions(curriculum.questions, topicIds, subtopicsById, errors)

        return errors
    }

    private fun validateMinimumCoverage(
        curriculum: Curriculum,
        errors: MutableList<CurriculumValidationError>,
    ) {
        if (curriculum.topics.isEmpty()) {
            errors.add(error(CurriculumValidationErrorCode.EMPTY_TOPICS, null, "Curriculum must contain at least one topic."))
        }
        if (curriculum.subtopics.isEmpty()) {
            errors.add(error(CurriculumValidationErrorCode.EMPTY_SUBTOPICS, null, "Curriculum must contain at least one subtopic."))
        }
        if (curriculum.questions.isEmpty()) {
            errors.add(error(CurriculumValidationErrorCode.EMPTY_QUESTIONS, null, "Curriculum must contain at least one question."))
            return
        }

        val questionTopicIds = curriculum.questions.map { it.topicId }.toSet()
        curriculum.topics.forEach { topic ->
            if (topic.id.isNotBlank() && topic.id !in questionTopicIds) {
                errors.add(
                    error(
                        CurriculumValidationErrorCode.TOPIC_WITHOUT_QUESTIONS,
                        topic.id,
                        "Topic '${topic.id}' must have at least one question.",
                    ),
                )
            }
        }
    }

    private fun validateTopics(
        topics: List<Topic>,
        errors: MutableList<CurriculumValidationError>,
    ) {
        val duplicateIds = duplicateNonBlankValues(topics.map { it.id })

        topics.forEach { topic ->
            if (topic.id.isBlank()) {
                errors.add(error(CurriculumValidationErrorCode.BLANK_TOPIC_ID, null, "Topic id must not be blank."))
            }
            if (topic.name.isBlank()) {
                errors.add(error(CurriculumValidationErrorCode.BLANK_TOPIC_NAME, topic.id, "Topic '${topic.id}' name must not be blank."))
            }
            if (topic.id in duplicateIds) {
                errors.add(error(CurriculumValidationErrorCode.DUPLICATE_TOPIC_ID, topic.id, "Topic id '${topic.id}' is duplicated."))
            }
        }
    }

    private fun validateSubtopics(
        subtopics: List<Subtopic>,
        topicIds: Set<String>,
        errors: MutableList<CurriculumValidationError>,
    ) {
        val duplicateIds = duplicateNonBlankValues(subtopics.map { it.id })

        subtopics.forEach { subtopic ->
            if (subtopic.id.isBlank()) {
                errors.add(error(CurriculumValidationErrorCode.BLANK_SUBTOPIC_ID, null, "Subtopic id must not be blank."))
            }
            if (subtopic.topicId.isBlank()) {
                errors.add(error(CurriculumValidationErrorCode.BLANK_SUBTOPIC_TOPIC_ID, subtopic.id, "Subtopic '${subtopic.id}' topicId must not be blank."))
            } else if (subtopic.topicId !in topicIds) {
                errors.add(error(CurriculumValidationErrorCode.UNKNOWN_TOPIC, subtopic.id, "Subtopic '${subtopic.id}' references unknown topic '${subtopic.topicId}'."))
            }
            if (subtopic.name.isBlank()) {
                errors.add(error(CurriculumValidationErrorCode.BLANK_SUBTOPIC_NAME, subtopic.id, "Subtopic '${subtopic.id}' name must not be blank."))
            }
            if (subtopic.id in duplicateIds) {
                errors.add(error(CurriculumValidationErrorCode.DUPLICATE_SUBTOPIC_ID, subtopic.id, "Subtopic id '${subtopic.id}' is duplicated."))
            }
        }
    }

    private fun validateQuestions(
        questions: List<Question>,
        topicIds: Set<String>,
        subtopicsById: Map<String, Subtopic>,
        errors: MutableList<CurriculumValidationError>,
    ) {
        val duplicateIds = duplicateNonBlankValues(questions.map { it.id })

        questions.forEach { question ->
            validateQuestionFields(question, duplicateIds, errors)
            validateQuestionHierarchy(question, topicIds, subtopicsById, errors)
            validateAnswers(question, errors)
            validateCorrectAnswers(question, errors)
            validateSources(question, errors)
        }
    }

    private fun validateQuestionFields(
        question: Question,
        duplicateIds: Set<String>,
        errors: MutableList<CurriculumValidationError>,
    ) {
        if (question.id.isBlank()) {
            errors.add(error(CurriculumValidationErrorCode.BLANK_QUESTION_ID, null, "Question id must not be blank."))
        }
        if (question.topicId.isBlank()) {
            errors.add(error(CurriculumValidationErrorCode.BLANK_QUESTION_TOPIC_ID, question.id, "Question '${question.id}' topicId must not be blank."))
        }
        if (question.subtopicId.isBlank()) {
            errors.add(error(CurriculumValidationErrorCode.BLANK_QUESTION_SUBTOPIC_ID, question.id, "Question '${question.id}' subtopicId must not be blank."))
        }
        if (question.text.isBlank()) {
            errors.add(error(CurriculumValidationErrorCode.BLANK_QUESTION_TEXT, question.id, "Question '${question.id}' text must not be blank."))
        } else if (question.text.containsPlaceholder()) {
            errors.add(error(CurriculumValidationErrorCode.PLACEHOLDER_QUESTION_TEXT, question.id, "Question '${question.id}' text contains placeholder content."))
        }
        if (question.id in duplicateIds) {
            errors.add(error(CurriculumValidationErrorCode.DUPLICATE_QUESTION_ID, question.id, "Question id '${question.id}' is duplicated."))
        }
        if (question.explanation.isBlank()) {
            errors.add(error(CurriculumValidationErrorCode.BLANK_EXPLANATION, question.id, "Question '${question.id}' explanation must not be blank."))
        } else if (question.explanation.containsPlaceholder()) {
            errors.add(error(CurriculumValidationErrorCode.PLACEHOLDER_EXPLANATION, question.id, "Question '${question.id}' explanation contains placeholder content."))
        }
    }

    private fun validateQuestionHierarchy(
        question: Question,
        topicIds: Set<String>,
        subtopicsById: Map<String, Subtopic>,
        errors: MutableList<CurriculumValidationError>,
    ) {
        if (question.topicId.isNotBlank() && question.topicId !in topicIds) {
            errors.add(error(CurriculumValidationErrorCode.UNKNOWN_TOPIC, question.id, "Question '${question.id}' references unknown topic '${question.topicId}'."))
        }

        val subtopic = subtopicsById[question.subtopicId]
        if (question.subtopicId.isNotBlank() && subtopic == null) {
            errors.add(error(CurriculumValidationErrorCode.UNKNOWN_SUBTOPIC, question.id, "Question '${question.id}' references unknown subtopic '${question.subtopicId}'."))
        }

        if (
            question.topicId in topicIds &&
            subtopic != null &&
            subtopic.topicId != question.topicId
        ) {
            errors.add(
                error(
                    CurriculumValidationErrorCode.SUBTOPIC_TOPIC_MISMATCH,
                    question.id,
                    "Question '${question.id}' references topic '${question.topicId}' but subtopic '${question.subtopicId}' belongs to topic '${subtopic.topicId}'.",
                ),
            )
        }
    }

    private fun validateAnswers(
        question: Question,
        errors: MutableList<CurriculumValidationError>,
    ) {
        if (question.answers.size < 2) {
            errors.add(error(CurriculumValidationErrorCode.TOO_FEW_ANSWERS, question.id, "Question '${question.id}' must have at least two answers."))
        }

        val duplicateIds = duplicateNonBlankValues(question.answers.map { it.id })
        val duplicateTexts = duplicateNonBlankValues(question.answers.map { it.text.normalizedForComparison() })
        question.answers.forEach { answer ->
            if (answer.id.isBlank()) {
                errors.add(error(CurriculumValidationErrorCode.BLANK_ANSWER_ID, question.id, "Question '${question.id}' has an answer with a blank id."))
            }
            if (answer.text.isBlank()) {
                errors.add(error(CurriculumValidationErrorCode.BLANK_ANSWER_TEXT, answer.id, "Answer '${answer.id}' text must not be blank."))
            } else if (answer.text.containsPlaceholder()) {
                errors.add(error(CurriculumValidationErrorCode.PLACEHOLDER_ANSWER_TEXT, answer.id, "Answer '${answer.id}' text contains placeholder content."))
            }
            if (answer.id in duplicateIds) {
                errors.add(error(CurriculumValidationErrorCode.DUPLICATE_ANSWER_ID, answer.id, "Question '${question.id}' has duplicate answer id '${answer.id}'."))
            }
            if (answer.text.normalizedForComparison() in duplicateTexts) {
                errors.add(error(CurriculumValidationErrorCode.DUPLICATE_ANSWER_TEXT, answer.id, "Question '${question.id}' offers answer '${answer.id}' twice as the same option text."))
            }
        }
    }

    private fun validateCorrectAnswers(
        question: Question,
        errors: MutableList<CurriculumValidationError>,
    ) {
        if (question.correctAnswerIds.isEmpty()) {
            errors.add(error(CurriculumValidationErrorCode.NO_CORRECT_ANSWERS, question.id, "Question '${question.id}' must have at least one correct answer."))
        }
        if (
            question.selectionMode == AnswerSelectionMode.SINGLE &&
            question.correctAnswerIds.size > 1
        ) {
            errors.add(
                error(
                    CurriculumValidationErrorCode.SELECTION_MODE_CORRECT_ANSWER_MISMATCH,
                    question.id,
                    "Question '${question.id}' uses SINGLE selection mode but has multiple correct answers.",
                ),
            )
        }

        val duplicateCorrectAnswerIds = duplicateNonBlankValues(question.correctAnswerIds)
        val answerIds = question.answers.map(AnswerOption::id).toSet()

        question.correctAnswerIds.forEach { correctAnswerId ->
            if (correctAnswerId in duplicateCorrectAnswerIds) {
                errors.add(error(CurriculumValidationErrorCode.DUPLICATE_CORRECT_ANSWER_ID, question.id, "Question '${question.id}' has duplicate correct answer id '$correctAnswerId'."))
            }
            if (correctAnswerId !in answerIds) {
                errors.add(error(CurriculumValidationErrorCode.UNKNOWN_CORRECT_ANSWER, question.id, "Question '${question.id}' references unknown correct answer '$correctAnswerId'."))
            }
        }
    }

    private fun validateSources(
        question: Question,
        errors: MutableList<CurriculumValidationError>,
    ) {
        if (question.sources.isEmpty()) {
            errors.add(error(CurriculumValidationErrorCode.NO_SOURCES, question.id, "Question '${question.id}' must have at least one source."))
        }

        question.sources.forEach { source ->
            validateSource(question, source, errors)
        }
    }

    private fun validateSource(
        question: Question,
        source: SourceReference,
        errors: MutableList<CurriculumValidationError>,
    ) {
        if (source.title.isBlank()) {
            errors.add(error(CurriculumValidationErrorCode.BLANK_SOURCE_TITLE, question.id, "Question '${question.id}' has a source with a blank title."))
        } else if (source.title.containsPlaceholder()) {
            errors.add(error(CurriculumValidationErrorCode.PLACEHOLDER_SOURCE_TITLE, question.id, "Question '${question.id}' has placeholder source title '${source.title}'."))
        }
        if (source.url.isBlank()) {
            errors.add(error(CurriculumValidationErrorCode.BLANK_SOURCE_URL, question.id, "Question '${question.id}' has a source with a blank URL."))
        } else if (!source.url.isValidHttpUrl()) {
            errors.add(error(CurriculumValidationErrorCode.INVALID_SOURCE_URL, question.id, "Question '${question.id}' has invalid source URL '${source.url}'."))
        } else if (source.url.isPlaceholderUrl()) {
            errors.add(error(CurriculumValidationErrorCode.PLACEHOLDER_SOURCE_URL, question.id, "Question '${question.id}' has placeholder source URL '${source.url}'."))
        }
    }

    private fun error(
        code: CurriculumValidationErrorCode,
        entityId: String?,
        message: String,
    ) = CurriculumValidationError(
        code = code,
        entityId = entityId?.takeUnless { it.isBlank() },
        message = message,
    )
}
