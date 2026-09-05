package org.artkachenko.kmp_learning_app.curriculum.learning.content

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningBlock
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningCurriculum
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningDepth
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningLesson
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningSection
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import org.artkachenko.kmp_learning_app.curriculum.learning.serialization.LearningCurriculumJsonCodec
import org.artkachenko.kmp_learning_app.curriculum.learning.validation.LearningCurriculumValidationErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal class LearningContentLoaderTest {
    @Test
    fun validDocumentIsReturnedAfterValidation() = runTest {
        val loaded = loader(learningCurriculum = validLearningCurriculum).load()

        assertEquals(validLearningCurriculum, loaded)
    }

    @Test
    fun emptyDocumentIsValidRatherThanAFailure() = runTest {
        val loaded = loader(learningCurriculum = LearningCurriculum(units = emptyList())).load()

        assertEquals(emptyList(), loaded.units)
    }

    @Test
    fun malformedJsonIsReportedAsADecodeFailure() = runTest {
        val loader = LearningContentLoader(
            loadLearningCurriculum = { LearningCurriculumJsonCodec.decode("{\"units\": [") },
            loadCurriculum = { baseCurriculum },
        )

        val failure = assertFailsWith<LearningContentLoadException> { loader.load() }.failure

        val decode = assertIs<LearningContentLoadFailure.Decode>(failure)
        assertIs<SerializationException>(decode.cause)
    }

    @Test
    fun contentRejectedByValidationIsReportedAsAValidationFailureCarryingEveryError() = runTest {
        val invalid = LearningCurriculum(
            units = listOf(
                unit(
                    topicId = "unknown_topic",
                    lessons = listOf(lesson(primarySubtopicIds = listOf("unknown_subtopic"))),
                ),
            ),
        )

        val failure = assertFailsWith<LearningContentLoadException> {
            loader(learningCurriculum = invalid).load()
        }.failure

        val validation = assertIs<LearningContentLoadFailure.Validation>(failure)
        assertEquals(
            listOf(
                LearningCurriculumValidationErrorCode.UNKNOWN_HOME_TOPIC,
                LearningCurriculumValidationErrorCode.UNKNOWN_PRIMARY_SUBTOPIC,
            ),
            validation.errors.map { it.code },
        )
    }

    @Test
    fun aDecodedButInvalidDocumentIsNeverReturnedAsContent() = runTest {
        val invalid = LearningCurriculum(units = listOf(unit(topicId = "unknown_topic")))

        val thrown = assertFailsWith<LearningContentLoadException> {
            loader(learningCurriculum = invalid).load()
        }

        // The distinction that matters: the failure names the defect instead of degrading
        // into an empty document that would look like a valid bundle with no content.
        assertIs<LearningContentLoadFailure.Validation>(thrown.failure)
        assertTrue(thrown.message.orEmpty().contains("unknown_topic"))
    }

    private fun loader(learningCurriculum: LearningCurriculum) =
        LearningContentLoader(
            loadLearningCurriculum = { learningCurriculum },
            loadCurriculum = { baseCurriculum },
        )

    private fun unit(
        id: String = "unit_thinking_in_compose",
        topicId: String = "android_ui",
        lessons: List<LearningLesson> = listOf(lesson()),
    ) = LearningUnit(
        id = id,
        topicId = topicId,
        title = "Thinking in Compose",
        summary = "How declarative UI changes the way a screen is written.",
        lessons = lessons,
    )

    private fun lesson(
        id: String = "lesson_declarative_ui",
        primarySubtopicIds: List<String> = listOf("compose_recomposition"),
    ) = LearningLesson(
        id = id,
        title = "Declarative UI",
        summary = "Why Compose describes the UI for a state instead of mutating a tree.",
        primarySubtopicIds = primarySubtopicIds,
        supportingSubtopicIds = emptyList(),
        sections = listOf(
            LearningSection(
                depth = LearningDepth.CORE,
                blocks = listOf(
                    LearningBlock.Paragraph(
                        text = "Compose describes the UI for the current state rather than mutating a view tree.",
                    ),
                ),
            ),
        ),
        relatedLessonIds = emptyList(),
        sources = listOf(
            SourceReference(
                title = "Thinking in Compose",
                url = "https://developer.android.com/develop/ui/compose/mental-model",
            ),
        ),
    )

    private val validLearningCurriculum = LearningCurriculum(units = listOf(unit()))

    private val baseCurriculum = Curriculum(
        topics = listOf(Topic(id = "android_ui", name = "Android UI")),
        subtopics = listOf(
            Subtopic(id = "compose_recomposition", topicId = "android_ui", name = "Recomposition"),
        ),
        questions = emptyList(),
    )
}
