package org.artkachenko.kmp_learning_app.lesson_study

/**
 * How much a Learn surface can currently say about the learner's study record, over content it can
 * already show.
 *
 * The three cases exist for the reason `TopicLearningUnitsUiState` draws the same distinction over
 * publisher content: a nullable value cannot tell "still arriving" from "could not be read", and
 * both would end up rendering as the third thing — nothing studied. Study state is enrichment
 * layered over readable content, so [Unavailable] costs an indicator and never a screen.
 *
 * Deliberately narrow: this wraps study state on the three Learn destinations and is not a general
 * async-result type for the app. The surfaces that answer a different question — publisher
 * availability, assessment analytics — already model their own outcomes, and folding them into one
 * generic would merge failures that have to stay apart.
 */
internal sealed interface StudyProgressUiState<out T> {
    /** Content is already readable; the study record is still being read. */
    data object Loading : StudyProgressUiState<Nothing>

    data class Available<out T>(val value: T) : StudyProgressUiState<T>

    /** The study record could not be read. Content, navigation, and practice are unaffected. */
    data object Unavailable : StudyProgressUiState<Nothing>
}

/**
 * The one mapping from the shared holder's state to what a screen may claim.
 *
 * Written once so no destination can decide for itself that an unreadable study record means an
 * empty one: [StudyProgressState.Error] becomes [StudyProgressUiState.Unavailable] here and
 * nowhere else. [transform] receives the loaded snapshot, which is where each surface applies its
 * own derivation — a Lesson's studied flag, a Unit's progress, a Topic's per-Unit progress — all
 * from the single persisted read the holder already performed.
 */
internal inline fun <T> StudyProgressState.toUiState(
    transform: (StudyProgressState.Loaded) -> T,
): StudyProgressUiState<T> =
    when (this) {
        StudyProgressState.Loading -> StudyProgressUiState.Loading
        StudyProgressState.Error -> StudyProgressUiState.Unavailable
        is StudyProgressState.Loaded -> StudyProgressUiState.Available(transform(this))
    }
