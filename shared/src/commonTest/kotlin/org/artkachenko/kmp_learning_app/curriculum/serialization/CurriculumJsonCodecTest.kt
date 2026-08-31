package org.artkachenko.kmp_learning_app.curriculum.serialization

import kotlinx.serialization.SerializationException
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class CurriculumJsonCodecTest {
    @Test
    fun validCurriculumJsonDecodesSuccessfully() {
        assertEquals(sampleCurriculum(), CurriculumJsonCodec.decode(sampleCurriculumJson))
    }

    @Test
    fun singleSelectionModeDecodes() {
        val decoded = CurriculumJsonCodec.decode(
            sampleCurriculumJson.replace("\"MULTIPLE\"", "\"SINGLE\""),
        )

        assertEquals(AnswerSelectionMode.SINGLE, decoded.questions.single().selectionMode)
    }

    @Test
    fun multipleSelectionModeWithOneCorrectAnswerDecodes() {
        val decoded = CurriculumJsonCodec.decode(sampleCurriculumJson)

        assertEquals(AnswerSelectionMode.MULTIPLE, decoded.questions.single().selectionMode)
        assertEquals(listOf("answer_a"), decoded.questions.single().correctAnswerIds)
    }

    @Test
    fun curriculumRoundTripPreservesEquivalentValue() {
        val curriculum = sampleCurriculum()

        assertEquals(curriculum, CurriculumJsonCodec.decode(CurriculumJsonCodec.encode(curriculum)))
    }

    @Test
    fun multipleCorrectAnswerIdsSurviveRoundTrip() {
        val curriculum = sampleCurriculum(
            correctAnswerIds = listOf("answer_a", "answer_b"),
        )

        val decoded = CurriculumJsonCodec.decode(CurriculumJsonCodec.encode(curriculum))

        assertEquals(listOf("answer_a", "answer_b"), decoded.questions.single().correctAnswerIds)
    }

    @Test
    fun activeStatusSurvivesRoundTrip() {
        val decoded = CurriculumJsonCodec.decode(CurriculumJsonCodec.encode(sampleCurriculum()))

        assertEquals(ContentStatus.ACTIVE, decoded.topics.single().status)
        assertEquals(ContentStatus.ACTIVE, decoded.subtopics.single().status)
        assertEquals(ContentStatus.ACTIVE, decoded.questions.single().status)
    }

    @Test
    fun deprecatedStatusSurvivesRoundTrip() {
        val curriculum = sampleCurriculum(status = ContentStatus.DEPRECATED)

        val decoded = CurriculumJsonCodec.decode(CurriculumJsonCodec.encode(curriculum))

        assertEquals(ContentStatus.DEPRECATED, decoded.topics.single().status)
        assertEquals(ContentStatus.DEPRECATED, decoded.subtopics.single().status)
        assertEquals(ContentStatus.DEPRECATED, decoded.questions.single().status)
    }

    @Test
    fun malformedJsonSyntaxFailsDecoding() {
        assertFailsWith<SerializationException> {
            CurriculumJsonCodec.decode("""{"topics": [""")
        }
    }

    @Test
    fun missingRequiredPropertyFailsDecoding() {
        assertFailsWith<SerializationException> {
            CurriculumJsonCodec.decode(
                """
                {
                  "topics": [
                    {
                      "id": "topic_1",
                      "status": "ACTIVE"
                    }
                  ],
                  "subtopics": [],
                  "questions": []
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun invalidContentStatusValueFailsDecoding() {
        assertFailsWith<SerializationException> {
            CurriculumJsonCodec.decode(
                sampleCurriculumJson.replace(
                    oldValue = """"status":"ACTIVE"""",
                    newValue = """"status":"ARCHIVED"""",
                ),
            )
        }
    }

    @Test
    fun missingSelectionModeFailsDecoding() {
        assertFailsWith<SerializationException> {
            CurriculumJsonCodec.decode(
                sampleCurriculumJson.replace("\"selectionMode\":\"MULTIPLE\",", ""),
            )
        }
    }

    @Test
    fun invalidSelectionModeFailsDecoding() {
        assertFailsWith<SerializationException> {
            CurriculumJsonCodec.decode(
                sampleCurriculumJson.replace("\"MULTIPLE\"", "\"ALL\""),
            )
        }
    }

    @Test
    fun unexpectedPropertyFailsDecoding() {
        assertFailsWith<SerializationException> {
            CurriculumJsonCodec.decode(
                sampleCurriculumJson.replace(
                    oldValue = """"topics":[""",
                    newValue = """"unexpected":"value","topics":[""",
                ),
            )
        }
    }

    private fun sampleCurriculum(
        correctAnswerIds: List<String> = listOf("answer_a"),
        selectionMode: AnswerSelectionMode = AnswerSelectionMode.MULTIPLE,
        status: ContentStatus = ContentStatus.ACTIVE,
    ) = Curriculum(
        topics = listOf(
            Topic(id = "topic_1", name = "Topic", status = status),
        ),
        subtopics = listOf(
            Subtopic(id = "subtopic_1", topicId = "topic_1", name = "Subtopic", status = status),
        ),
        questions = listOf(
            Question(
                id = "question_1",
                topicId = "topic_1",
                subtopicId = "subtopic_1",
                text = "Which answers are correct? Select all that apply.",
                answers = listOf(
                    AnswerOption(id = "answer_a", text = "First correct answer"),
                    AnswerOption(id = "answer_b", text = "Second correct answer"),
                ),
                selectionMode = selectionMode,
                correctAnswerIds = correctAnswerIds,
                explanation = "The listed answer IDs identify the correct answer options.",
                sources = listOf(SourceReference(title = "Source", url = "https://example.com/source")),
                status = status,
            ),
        ),
    )

    private val sampleCurriculumJson =
        """{"topics":[{"id":"topic_1","name":"Topic","status":"ACTIVE"}],"subtopics":[{"id":"subtopic_1","topicId":"topic_1","name":"Subtopic","status":"ACTIVE"}],"questions":[{"id":"question_1","topicId":"topic_1","subtopicId":"subtopic_1","text":"Which answers are correct? Select all that apply.","answers":[{"id":"answer_a","text":"First correct answer"},{"id":"answer_b","text":"Second correct answer"}],"selectionMode":"MULTIPLE","correctAnswerIds":["answer_a"],"explanation":"The listed answer IDs identify the correct answer options.","sources":[{"title":"Source","url":"https://example.com/source"}],"status":"ACTIVE"}]}"""
}
