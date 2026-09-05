package org.artkachenko.kmp_learning_app.curriculum.learning

import kotlinx.serialization.Serializable
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.SourceReference

/**
 * One focused piece of reading inside a [LearningUnit].
 *
 * [primarySubtopicIds] are the concepts the Lesson is responsible for teaching
 * thoroughly; [supportingSubtopicIds] are concepts explained only far enough for this
 * Lesson to stay understandable. They are separate fields because their meaning differs:
 * only primary concepts are intended to become practice coverage. Either list may name
 * Subtopics owned by another Topic.
 *
 * Reference validity — whether these IDs and [relatedLessonIds] resolve — is not the
 * model's concern; content validation owns it.
 */
@Serializable
internal data class LearningLesson(
    val id: String,
    val title: String,
    val summary: String,
    val primarySubtopicIds: List<String>,
    val supportingSubtopicIds: List<String>,
    val sections: List<LearningSection>,
    val relatedLessonIds: List<String>,
    val sources: List<SourceReference>,
    val status: ContentStatus = ContentStatus.ACTIVE,
)
