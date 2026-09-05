package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit

/**
 * The study section's own state, deliberately not flattened into a nullable list.
 *
 * Learning content is a second publisher-owned source behind the same screen, so it has to be able
 * to be still arriving, successfully empty, or unreadable — three answers a `List?` cannot tell
 * apart. [Available] with an empty list is a repository that answered "this Topic has no authored
 * Units"; [Unavailable] is a repository that could not answer at all, and must never be rendered as
 * the former. Neither can influence whether the Topic loaded, or whether it can be practised.
 */
internal sealed interface TopicLearningUnitsUiState {
    /** The Topic is already usable; its study material is still being resolved. */
    data object Loading : TopicLearningUnitsUiState

    data class Available(
        val units: List<LearningUnitItemUiModel>,
    ) : TopicLearningUnitsUiState

    /** Learning content could not be read. Practice and progress are unaffected. */
    data object Unavailable : TopicLearningUnitsUiState
}

/**
 * One Learning Unit as Topic Detail needs it: enough to decide whether to read it, and nothing else.
 *
 * The authored [org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit] carries whole
 * Lessons, their sections, their Sources, and their Subtopic relationships. None of that belongs on
 * a discovery row, and [unitId] rather than the object is what leaves this screen, so navigation
 * later carries a stable identity instead of serialized publisher content.
 */
internal data class LearningUnitItemUiModel(
    val unitId: String,
    val title: String,
    val summary: String,
    /** ACTIVE Lessons only: retired material is not current study material. */
    val activeLessonCount: Int,
)

/**
 * The authored Units for a Topic, as this screen needs them.
 *
 * Order is carried through untouched, because the repository's order is authored pedagogical order.
 * Deprecated Lessons are excluded rather than counted: the count is a promise about what there is to
 * read now, not about how much was ever written.
 */
internal fun List<LearningUnit>.toLearningUnitItems(): List<LearningUnitItemUiModel> =
    map { unit ->
        LearningUnitItemUiModel(
            unitId = unit.id,
            title = unit.title,
            summary = unit.summary,
            activeLessonCount = unit.lessons.count { it.status == ContentStatus.ACTIVE },
        )
    }
