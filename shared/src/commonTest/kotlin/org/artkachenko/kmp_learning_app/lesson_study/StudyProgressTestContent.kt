package org.artkachenko.kmp_learning_app.lesson_study

import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningBlock
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningDepth
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningLesson
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningSection
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit

/**
 * Real content values rather than mocks: the derivation is ordinary Kotlin over the learning model,
 * so a fixture is both cheaper and closer to what it actually reads.
 */
internal fun learningUnit(
    id: String,
    topicId: String = "android_ui",
    lessons: List<LearningLesson>,
    status: ContentStatus = ContentStatus.ACTIVE,
) = LearningUnit(
    id = id,
    topicId = topicId,
    title = "Unit $id",
    summary = "What the learner takes away from unit $id.",
    lessons = lessons,
    status = status,
)

internal fun learningLesson(
    id: String,
    title: String = "Lesson $id",
    primarySubtopicIds: List<String> = listOf("compose_recomposition"),
    supportingSubtopicIds: List<String> = emptyList(),
    status: ContentStatus = ContentStatus.ACTIVE,
    body: String = "Compose describes the UI for the current state rather than mutating a view tree.",
) = LearningLesson(
    id = id,
    title = title,
    summary = "What the learner takes away from lesson $id.",
    primarySubtopicIds = primarySubtopicIds,
    supportingSubtopicIds = supportingSubtopicIds,
    sections = listOf(
        LearningSection(
            depth = LearningDepth.CORE,
            blocks = listOf(LearningBlock.Paragraph(text = body)),
        ),
    ),
    relatedLessonIds = emptyList(),
    sources = listOf(
        SourceReference(
            title = "Thinking in Compose",
            url = "https://developer.android.com/develop/ui/compose/mental-model",
        ),
    ),
    status = status,
)
