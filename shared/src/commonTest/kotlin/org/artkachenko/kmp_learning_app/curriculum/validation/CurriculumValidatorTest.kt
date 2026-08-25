package org.artkachenko.kmp_learning_app.curriculum.validation

import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class CurriculumValidatorTest {
    private val validator = CurriculumValidator()

    @Test
    fun validCurriculumHasNoErrors() {
        assertTrue(validator.validate(validCurriculum()).isEmpty())
    }

    @Test
    fun validatesMinimumCurriculumCoverage() {
        val curriculum = Curriculum(
            topics = emptyList(),
            subtopics = emptyList(),
            questions = emptyList(),
        )

        assertCodes(
            curriculum,
            CurriculumValidationErrorCode.EMPTY_TOPICS,
            CurriculumValidationErrorCode.EMPTY_SUBTOPICS,
            CurriculumValidationErrorCode.EMPTY_QUESTIONS,
        )
    }

    @Test
    fun validatesTopicFieldsAndDuplicateIds() {
        val curriculum = validCurriculum(
            topics = listOf(
                Topic(id = "topic_1", name = "Topic"),
                Topic(id = "topic_1", name = "Duplicate topic"),
                Topic(id = "", name = ""),
            ),
        )

        assertCodes(
            curriculum,
            CurriculumValidationErrorCode.DUPLICATE_TOPIC_ID,
            CurriculumValidationErrorCode.DUPLICATE_TOPIC_ID,
            CurriculumValidationErrorCode.BLANK_TOPIC_ID,
            CurriculumValidationErrorCode.BLANK_TOPIC_NAME,
        )
    }

    @Test
    fun validatesSubtopicFieldsDuplicateIdsAndUnknownTopic() {
        val curriculum = validCurriculum(
            subtopics = listOf(
                Subtopic(id = "subtopic_1", topicId = "topic_1", name = "Subtopic"),
                Subtopic(id = "subtopic_1", topicId = "topic_1", name = "Duplicate subtopic"),
                Subtopic(id = "", topicId = "", name = ""),
                Subtopic(id = "unknown_topic_subtopic", topicId = "missing_topic", name = "Unknown topic"),
            ),
        )

        assertCodes(
            curriculum,
            CurriculumValidationErrorCode.DUPLICATE_SUBTOPIC_ID,
            CurriculumValidationErrorCode.DUPLICATE_SUBTOPIC_ID,
            CurriculumValidationErrorCode.BLANK_SUBTOPIC_ID,
            CurriculumValidationErrorCode.BLANK_SUBTOPIC_TOPIC_ID,
            CurriculumValidationErrorCode.BLANK_SUBTOPIC_NAME,
            CurriculumValidationErrorCode.UNKNOWN_TOPIC,
        )
    }

    @Test
    fun validatesQuestionFieldsDuplicateIdsAndHierarchy() {
        val curriculum = validCurriculum(
            topics = listOf(
                Topic(id = "topic_1", name = "Topic"),
                Topic(id = "topic_2", name = "Other topic"),
            ),
            subtopics = listOf(
                Subtopic(id = "subtopic_1", topicId = "topic_1", name = "Subtopic"),
                Subtopic(id = "topic_2_subtopic", topicId = "topic_2", name = "Other subtopic"),
            ),
            questions = listOf(
                question(id = "question_1"),
                question(id = "question_1", text = "Duplicate id question?"),
                question(
                    id = "",
                    topicId = "",
                    subtopicId = "",
                    text = "",
                ),
                question(
                    id = "unknown_topic_question",
                    topicId = "missing_topic",
                ),
                question(
                    id = "unknown_subtopic_question",
                    subtopicId = "missing_subtopic",
                ),
                question(
                    id = "mismatched_subtopic_question",
                    topicId = "topic_1",
                    subtopicId = "topic_2_subtopic",
                ),
            ),
        )

        assertCodes(
            curriculum,
            CurriculumValidationErrorCode.TOPIC_WITHOUT_QUESTIONS,
            CurriculumValidationErrorCode.DUPLICATE_QUESTION_ID,
            CurriculumValidationErrorCode.DUPLICATE_QUESTION_ID,
            CurriculumValidationErrorCode.BLANK_QUESTION_ID,
            CurriculumValidationErrorCode.BLANK_QUESTION_TOPIC_ID,
            CurriculumValidationErrorCode.BLANK_QUESTION_SUBTOPIC_ID,
            CurriculumValidationErrorCode.BLANK_QUESTION_TEXT,
            CurriculumValidationErrorCode.UNKNOWN_TOPIC,
            CurriculumValidationErrorCode.UNKNOWN_SUBTOPIC,
            CurriculumValidationErrorCode.SUBTOPIC_TOPIC_MISMATCH,
        )
    }

    @Test
    fun validatesAnswerOptions() {
        val curriculum = validCurriculum(
            questions = listOf(
                question(
                    answers = listOf(
                        AnswerOption(id = "answer_a", text = "Only answer"),
                    ),
                    correctAnswerIds = listOf("answer_a"),
                ),
                question(
                    id = "bad_answers_question",
                    answers = listOf(
                        AnswerOption(id = "", text = ""),
                        AnswerOption(id = "duplicate_answer", text = "First duplicate"),
                        AnswerOption(id = "duplicate_answer", text = "Second duplicate"),
                    ),
                    correctAnswerIds = listOf("duplicate_answer"),
                ),
            ),
        )

        assertCodes(
            curriculum,
            CurriculumValidationErrorCode.TOO_FEW_ANSWERS,
            CurriculumValidationErrorCode.BLANK_ANSWER_ID,
            CurriculumValidationErrorCode.BLANK_ANSWER_TEXT,
            CurriculumValidationErrorCode.DUPLICATE_ANSWER_ID,
            CurriculumValidationErrorCode.DUPLICATE_ANSWER_ID,
        )
    }

    @Test
    fun validatesCorrectAnswers() {
        val curriculum = validCurriculum(
            questions = listOf(
                question(correctAnswerIds = emptyList()),
                question(
                    id = "duplicate_correct_answer_question",
                    correctAnswerIds = listOf("answer_a", "answer_a"),
                ),
                question(
                    id = "unknown_correct_answer_question",
                    correctAnswerIds = listOf("missing_answer"),
                ),
            ),
        )

        assertCodes(
            curriculum,
            CurriculumValidationErrorCode.NO_CORRECT_ANSWERS,
            CurriculumValidationErrorCode.DUPLICATE_CORRECT_ANSWER_ID,
            CurriculumValidationErrorCode.DUPLICATE_CORRECT_ANSWER_ID,
            CurriculumValidationErrorCode.UNKNOWN_CORRECT_ANSWER,
        )
    }

    @Test
    fun allowsOneOrMultipleCorrectAnswers() {
        assertTrue(
            validator.validate(
                validCurriculum(
                    questions = listOf(
                        question(correctAnswerIds = listOf("answer_a")),
                        question(
                            id = "multi_correct_question",
                            correctAnswerIds = listOf("answer_a", "answer_b"),
                        ),
                    ),
                ),
            ).isEmpty(),
        )
    }

    @Test
    fun validatesExplanation() {
        assertCodes(
            validCurriculum(
                questions = listOf(
                    question(explanation = ""),
                ),
            ),
            CurriculumValidationErrorCode.BLANK_EXPLANATION,
        )
    }

    @Test
    fun validatesSources() {
        val curriculum = validCurriculum(
            questions = listOf(
                question(sources = emptyList()),
                question(
                    id = "blank_source_question",
                    sources = listOf(SourceReference(title = "", url = "")),
                ),
                question(
                    id = "invalid_source_question",
                    sources = listOf(SourceReference(title = "Invalid", url = "ftp://example.com/path")),
                ),
                question(
                    id = "missing_host_source_question",
                    sources = listOf(SourceReference(title = "Missing host", url = "https://")),
                ),
            ),
        )

        assertCodes(
            curriculum,
            CurriculumValidationErrorCode.NO_SOURCES,
            CurriculumValidationErrorCode.BLANK_SOURCE_TITLE,
            CurriculumValidationErrorCode.BLANK_SOURCE_URL,
            CurriculumValidationErrorCode.INVALID_SOURCE_URL,
            CurriculumValidationErrorCode.INVALID_SOURCE_URL,
        )
    }

    @Test
    fun allowsHttpAndHttpsSourceUrls() {
        assertTrue(
            validator.validate(
                validCurriculum(
                    questions = listOf(
                        question(
                            sources = listOf(SourceReference(title = "HTTPS source", url = "https://example.com/path")),
                        ),
                        question(
                            id = "http_source_question",
                            sources = listOf(SourceReference(title = "HTTP source", url = "http://example.com/path")),
                        ),
                    ),
                ),
            ).isEmpty(),
        )
    }

    @Test
    fun deprecatedContentIsNotRejectedSolelyForStatus() {
        val curriculum = validCurriculum(
            topics = listOf(Topic(id = "topic_1", name = "Topic", status = ContentStatus.DEPRECATED)),
            subtopics = listOf(
                Subtopic(
                    id = "subtopic_1",
                    topicId = "topic_1",
                    name = "Subtopic",
                    status = ContentStatus.DEPRECATED,
                ),
            ),
            questions = listOf(question(status = ContentStatus.DEPRECATED)),
        )

        assertTrue(validator.validate(curriculum).isEmpty())
    }

    @Test
    fun returnsMultipleErrorsWithoutFailingFast() {
        val errors = validator.validate(
            validCurriculum(
                topics = listOf(Topic(id = "topic_1", name = "")),
                subtopics = listOf(Subtopic(id = "subtopic_1", topicId = "missing_topic", name = "")),
                questions = listOf(
                    question(
                        text = "",
                        answers = listOf(AnswerOption(id = "", text = "")),
                        correctAnswerIds = emptyList(),
                        explanation = "",
                        sources = emptyList(),
                    ),
                ),
            ),
        )

        assertEquals(
            listOf(
                CurriculumValidationErrorCode.BLANK_TOPIC_NAME,
                CurriculumValidationErrorCode.UNKNOWN_TOPIC,
                CurriculumValidationErrorCode.BLANK_SUBTOPIC_NAME,
                CurriculumValidationErrorCode.BLANK_QUESTION_TEXT,
                CurriculumValidationErrorCode.BLANK_EXPLANATION,
                CurriculumValidationErrorCode.SUBTOPIC_TOPIC_MISMATCH,
                CurriculumValidationErrorCode.TOO_FEW_ANSWERS,
                CurriculumValidationErrorCode.BLANK_ANSWER_ID,
                CurriculumValidationErrorCode.BLANK_ANSWER_TEXT,
                CurriculumValidationErrorCode.NO_CORRECT_ANSWERS,
                CurriculumValidationErrorCode.NO_SOURCES,
            ),
            errors.map { it.code },
        )
    }

    @Test
    fun validationErrorOrderingIsDeterministic() {
        val malformedCurriculum = validCurriculum(
            topics = listOf(Topic(id = "topic_1", name = "")),
            subtopics = listOf(Subtopic(id = "subtopic_1", topicId = "missing_topic", name = "")),
            questions = listOf(question(text = "", correctAnswerIds = emptyList(), sources = emptyList())),
        )

        val firstResult = validator.validate(malformedCurriculum)

        repeat(5) {
            assertEquals(firstResult, validator.validate(malformedCurriculum))
        }
    }

    private fun assertCodes(
        curriculum: Curriculum,
        vararg expectedCodes: CurriculumValidationErrorCode,
    ) {
        assertEquals(expectedCodes.toList(), validator.validate(curriculum).map { it.code })
    }

    private fun validCurriculum(
        topics: List<Topic> = listOf(Topic(id = "topic_1", name = "Topic")),
        subtopics: List<Subtopic> = listOf(Subtopic(id = "subtopic_1", topicId = "topic_1", name = "Subtopic")),
        questions: List<Question> = listOf(question()),
    ) = Curriculum(
        topics = topics,
        subtopics = subtopics,
        questions = questions,
    )

    private fun question(
        id: String = "question_1",
        topicId: String = "topic_1",
        subtopicId: String = "subtopic_1",
        text: String = "Which answer is correct?",
        answers: List<AnswerOption> = listOf(
            AnswerOption(id = "answer_a", text = "Correct answer"),
            AnswerOption(id = "answer_b", text = "Incorrect answer"),
        ),
        correctAnswerIds: List<String> = listOf("answer_a"),
        explanation: String = "The correct answer matches the documented behavior.",
        sources: List<SourceReference> = listOf(SourceReference(title = "Source", url = "https://example.com/reference")),
        status: ContentStatus = ContentStatus.ACTIVE,
    ) = Question(
        id = id,
        topicId = topicId,
        subtopicId = subtopicId,
        text = text,
        answers = answers,
        correctAnswerIds = correctAnswerIds,
        explanation = explanation,
        sources = sources,
        status = status,
    )
}
