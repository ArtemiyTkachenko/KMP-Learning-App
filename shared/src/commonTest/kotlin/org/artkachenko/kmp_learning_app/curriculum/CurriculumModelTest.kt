package org.artkachenko.kmp_learning_app.curriculum

import kotlin.test.Test
import kotlin.test.assertEquals

internal class CurriculumModelTest {
    @Test
    fun questionCanRepresentMultipleCorrectAnswers() {
        val question = question(
            answers = listOf(
                AnswerOption(id = "answer_a", text = "First correct answer"),
                AnswerOption(id = "answer_b", text = "Incorrect answer"),
                AnswerOption(id = "answer_c", text = "Second correct answer"),
            ),
            selectionMode = AnswerSelectionMode.MULTIPLE,
            correctAnswerIds = listOf("answer_a", "answer_c"),
        )

        assertEquals(listOf("answer_a", "answer_c"), question.correctAnswerIds)
    }

    @Test
    fun deprecatedContentCanBeRepresentedWithoutBlockingConstruction() {
        val topic = Topic(
            id = "android_platform",
            name = "Android Platform & Application Model",
            status = ContentStatus.DEPRECATED,
        )
        val subtopic = Subtopic(
            id = "android_process_model",
            topicId = topic.id,
            name = "Application and process model",
            status = ContentStatus.DEPRECATED,
        )
        val question = question(
            topicId = topic.id,
            subtopicId = subtopic.id,
            status = ContentStatus.DEPRECATED,
        )

        assertEquals(ContentStatus.DEPRECATED, topic.status)
        assertEquals(ContentStatus.DEPRECATED, subtopic.status)
        assertEquals(ContentStatus.DEPRECATED, question.status)
    }

    @Test
    fun flatCurriculumRelationshipsCanBeExpressedThroughStableIds() {
        val topic = Topic(
            id = "android_platform",
            name = "Android Platform & Application Model",
        )
        val subtopic = Subtopic(
            id = "android_process_model",
            topicId = topic.id,
            name = "Application and process model",
        )
        val question = question(
            id = "android_process_model_process_isolation",
            topicId = topic.id,
            subtopicId = subtopic.id,
        )
        val curriculum = Curriculum(
            topics = listOf(topic),
            subtopics = listOf(subtopic),
            questions = listOf(question),
        )

        assertEquals(topic.id, curriculum.subtopics.single().topicId)
        assertEquals(topic.id, curriculum.questions.single().topicId)
        assertEquals(subtopic.id, curriculum.questions.single().subtopicId)
    }

    private fun question(
        id: String = "question_id",
        topicId: String = "android_platform",
        subtopicId: String = "android_process_model",
        answers: List<AnswerOption> = listOf(
            AnswerOption(id = "answer_a", text = "Correct answer"),
            AnswerOption(id = "answer_b", text = "Incorrect answer"),
        ),
        selectionMode: AnswerSelectionMode = AnswerSelectionMode.SINGLE,
        correctAnswerIds: List<String> = listOf("answer_a"),
        status: ContentStatus = ContentStatus.ACTIVE,
    ) = Question(
        id = id,
        topicId = topicId,
        subtopicId = subtopicId,
        text = "Which statement is correct?",
        answers = answers,
        selectionMode = selectionMode,
        level = QuestionLevel.FOUNDATION,
        correctAnswerIds = correctAnswerIds,
        explanation = "The selected answer matches the documented behavior.",
        sources = listOf(
            SourceReference(
                title = "Android Developers",
                url = "https://developer.android.com/",
            ),
        ),
        status = status,
    )
}
