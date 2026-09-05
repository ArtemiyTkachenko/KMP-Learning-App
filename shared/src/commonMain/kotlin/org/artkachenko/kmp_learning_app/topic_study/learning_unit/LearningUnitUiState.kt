package org.artkachenko.kmp_learning_app.topic_study.learning_unit

import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit

/**
 * The Unit overview's state.
 *
 * [NotFound] and [Error] are separate on purpose, and neither may stand in for the other.
 * [NotFound] is a settled answer — the ID names nothing current, so there is nothing to retry and
 * offering a Retry button would promise a different outcome from repeating the same lookup.
 * [Error] is the document failing to load at all, which is worth trying again.
 *
 * Unlike Topic Detail, learning content is this destination's only capability: there is no
 * practice surface underneath that has to survive an unreadable document, so a failure here is a
 * screen-level state rather than a section-level one.
 */
internal sealed interface LearningUnitUiState {
    data object Loading : LearningUnitUiState

    /**
     * A current Unit and what can be read in it.
     *
     * Presentation fields only: the authored [LearningUnit] carries whole Lessons with their
     * sections, Sources, and Subtopic relationships, and none of that belongs on a screen whose
     * job is choosing what to read. [lessons] may be empty — see [LearningUnitViewModel].
     */
    data class Content(
        val unitId: String,
        val title: String,
        val summary: String,
        val lessons: List<LearningLessonItemUiModel>,
    ) : LearningUnitUiState

    /** The ID names no Unit, or names one that is no longer current study material. */
    data object NotFound : LearningUnitUiState

    /** The learning document could not be read. Retryable, unlike [NotFound]. */
    data object Error : LearningUnitUiState
}

/**
 * One Lesson as the overview needs it: enough to decide whether to read it, and nothing else.
 *
 * No status, no completion, no question coverage. Status is already spent deciding whether the row
 * exists at all, and learner progress is not modelled anywhere in EPIC-21.
 */
internal data class LearningLessonItemUiModel(
    val lessonId: String,
    val title: String,
    val summary: String,
)

/**
 * The Unit's current Lessons, in authored order.
 *
 * Deprecated Lessons are dropped rather than shown disabled: they are retired material, not
 * unavailable current material, and a Unit keeps them only so a stored reference to one still
 * resolves. Nothing is sorted — list position is the authored pedagogical order.
 */
internal fun LearningUnit.toActiveLessonItems(): List<LearningLessonItemUiModel> =
    lessons
        .filter { it.status == ContentStatus.ACTIVE }
        .map { lesson ->
            LearningLessonItemUiModel(
                lessonId = lesson.id,
                title = lesson.title,
                summary = lesson.summary,
            )
        }
