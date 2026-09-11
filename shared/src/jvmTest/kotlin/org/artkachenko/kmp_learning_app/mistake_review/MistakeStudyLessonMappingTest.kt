package org.artkachenko.kmp_learning_app.mistake_review

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionUiModel
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningLesson
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit

internal class MistakeStudyLessonMappingTest {
    @Test
    fun aUniquePrimarySubtopicRelationshipMapsToStableLessonIds() {
        val mapped = listOf(mistake("state_hoisting")).withStudyLessons(
            listOf(unit("unit-compose", lesson("lesson-state", listOf("state_hoisting")))),
        )

        assertEquals(
            MistakeStudyLesson("unit-compose", "lesson-state", "Lesson lesson-state"),
            mapped.single().studyLesson,
        )
    }

    @Test
    fun ambiguousPrimaryRelationshipsDoNotInventALessonLink() {
        val mapped = listOf(mistake("state")).withStudyLessons(
            listOf(
                unit(
                    "unit-compose",
                    lesson("lesson-one", listOf("state")),
                    lesson("lesson-two", listOf("state")),
                ),
            ),
        )

        assertNull(mapped.single().studyLesson)
    }

    @Test
    fun supportingOrInactiveRelationshipsDoNotCreateALessonLink() {
        val supporting = lesson("supporting", primary = emptyList(), supporting = listOf("state"))
        val inactive = lesson("inactive", primary = listOf("state"), status = ContentStatus.DEPRECATED)

        val mapped = listOf(mistake("state")).withStudyLessons(
            listOf(unit("unit-compose", supporting, inactive)),
        )

        assertNull(mapped.single().studyLesson)
    }

    private fun mistake(subtopicId: String) = UnresolvedMistake(
        questionId = "q1",
        sourceAttemptId = "attempt",
        reviewItem = ReviewQuestionItem.Available(
            ReviewQuestionUiModel(
                questionId = "q1",
                topicId = "compose",
                subtopicId = subtopicId,
                text = "Question",
                isCorrect = false,
                answers = emptyList(),
                explanation = "Explanation",
                sources = emptyList(),
            ),
        ),
    )

    private fun unit(
        id: String,
        vararg lessons: LearningLesson,
    ) = LearningUnit(
        id = id,
        topicId = "compose",
        title = "Unit $id",
        summary = "Summary",
        lessons = lessons.toList(),
    )

    private fun lesson(
        id: String,
        primary: List<String>,
        supporting: List<String> = emptyList(),
        status: ContentStatus = ContentStatus.ACTIVE,
    ) = LearningLesson(
        id = id,
        title = "Lesson $id",
        summary = "Summary",
        primarySubtopicIds = primary,
        supportingSubtopicIds = supporting,
        sections = emptyList(),
        relatedLessonIds = emptyList(),
        sources = emptyList(),
        status = status,
    )
}
