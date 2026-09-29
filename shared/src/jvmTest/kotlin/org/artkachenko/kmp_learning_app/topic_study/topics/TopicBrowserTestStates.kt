package org.artkachenko.kmp_learning_app.topic_study.topics

import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingContext

/**
 * Browsing content built from a flat row list, grouped by the production [toBrowserSections].
 *
 * For screen tests whose subject is a row, a card or the search results rather than the sectioning
 * itself: they state the rows they need and receive the sections the ViewModel would have produced
 * for them.
 */
internal fun browsingContent(
    topics: List<TopicBrowserItemUiModel>,
    searchableSubtopics: List<SubtopicSearchResult> = emptyList(),
    query: String = "",
    topicMatches: List<TopicBrowserItemUiModel> = emptyList(),
    subtopicMatches: List<SubtopicSearchResult> = emptyList(),
    continueStudying: ContinueStudyingContext? = null,
    recommendedNext: RecommendedNextUiModel? = null,
    continueLearning: ContinueLearningUiModel? = null,
): TopicBrowserUiState.Content =
    TopicBrowserUiState.Content(
        sections = topics.toBrowserSections(),
        searchableSubtopics = searchableSubtopics,
        query = query,
        topicMatches = topicMatches,
        subtopicMatches = subtopicMatches,
        continueStudying = continueStudying,
        recommendedNext = recommendedNext,
        continueLearning = continueLearning,
    )

/** Every browsing row across sections, in presentation order, for assertions about rows. */
internal val TopicBrowserUiState.Content.allTopics: List<TopicBrowserItemUiModel>
    get() = sections.flatMap(TopicBrowserSection::topics)
