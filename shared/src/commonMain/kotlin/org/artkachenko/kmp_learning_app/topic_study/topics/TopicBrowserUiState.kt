package org.artkachenko.kmp_learning_app.topic_study.topics

import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingContext
import org.artkachenko.kmp_learning_app.ui.LearningContextUiModel

internal sealed interface TopicBrowserUiState {
    data object Loading : TopicBrowserUiState

    /**
     * Curriculum decides this state; learning context only decorates it. A Topic is browsable,
     * searchable, and openable whether or not analytics ever arrive, which is why history has no
     * say in Loading, Empty, or Error.
     */
    data class Content(
        val topics: List<TopicBrowserItemUiModel>,
        val searchableSubtopics: List<SubtopicSearchResult> = emptyList(),
        val query: String = "",
        /**
         * The same enriched rows as [topics], filtered: a Topic match is the same Topic, so it
         * carries the same marker and the same learning context rather than a second derivation.
         */
        val topicMatches: List<TopicBrowserItemUiModel> = emptyList(),
        val subtopicMatches: List<SubtopicSearchResult> = emptyList(),
        /**
         * A shortcut back into recent study context, or `null` when there is no usable one.
         *
         * Optional enrichment on this state rather than a state of its own: Topics is a catalogue
         * first, and a screen-level `ContinueStudying` state would let an absent, stale, or
         * unreadable history decide whether the learner can browse at all.
         *
         * Absent while a query is active. The card is not a search result, and it is not a Topic
         * that happens to match — a learner who has started typing has already said what they are
         * looking for.
         */
        val continueStudying: ContinueStudyingContext? = null,
    ) : TopicBrowserUiState

    data object Empty : TopicBrowserUiState

    data object Error : TopicBrowserUiState
}

/**
 * One Topic as the browser presents it: curriculum identity, plus what the learner has done with it.
 *
 * The learning context lives here rather than on `curriculum.Topic` because it describes the
 * learner, not the content — the same Topic reads differently for two people, and the curriculum
 * domain has to stay presentation- and history-agnostic.
 */
internal data class TopicBrowserItemUiModel(
    val topicId: String,
    val topicName: String,
    /**
     * `null` while analytics have not loaded or could not be derived. That is unknown history, not
     * empty history, so a row in this state says nothing about the learner rather than claiming the
     * Topic has never been studied.
     */
    val learningContext: LearningContextUiModel? = null,
)

internal data class SubtopicSearchResult(
    val subtopicId: String,
    val subtopicName: String,
    val parentTopicId: String,
    val parentTopicName: String,
)
