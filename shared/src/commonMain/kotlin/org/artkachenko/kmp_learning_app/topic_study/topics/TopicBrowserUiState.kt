package org.artkachenko.kmp_learning_app.topic_study.topics

import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingContext
import org.artkachenko.kmp_learning_app.guided_learning.LearningRecommendationRationale
import org.artkachenko.kmp_learning_app.guided_learning.LearningRecommendationTarget
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
        /**
         * The one policy-chosen next action, or `null` when none can be justified.
         *
         * Optional enrichment beside [continueStudying] rather than a screen-level state, for the
         * same reason: Topics is a catalogue first, and guidance that cannot be derived must cost
         * the learner nothing. The two are independent — [continueStudying] answers "where was I?"
         * and this answers "what should I do now?" — so either may be present without the other,
         * and neither is suppressed or altered because the other exists.
         *
         * Absent while a query is active, exactly as the continue shortcut is: a recommendation is
         * not a search result, and its reason is not searchable text.
         */
        val recommendedNext: RecommendedNextUiModel? = null,
    ) : TopicBrowserUiState

    data object Empty : TopicBrowserUiState

    data object Error : TopicBrowserUiState
}

/**
 * One Topic as the browser presents it: curriculum identity, what authored study material exists
 * for it, and what the learner has done with it.
 *
 * The learning context lives here rather than on `curriculum.Topic` because it describes the
 * learner, not the content — the same Topic reads differently for two people, and the curriculum
 * domain has to stay presentation- and history-agnostic. [learningUnitCount] stays here for the
 * mirror-image reason: it comes from a second publisher-owned content source joined by stable Topic
 * ID, so it is presentation composition rather than base assessment taxonomy.
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
    /**
     * How many ACTIVE Learning Units the learning curriculum publishes for this Topic.
     *
     * Three distinct states, and the distinction is the point:
     *
     * - `null` — learning content has not been read yet, or could not be read at all. Availability
     *   is unknown, so the row says nothing about it;
     * - `0` — learning content was read and this Topic has no authored study material;
     * - `> 0` — explanatory material exists.
     *
     * A failed read must never collapse into `0`: "we could not read the learning curriculum" and
     * "this Topic has nothing to read" are different statements to make to a learner.
     *
     * This is publisher-owned availability only. It carries no learner-owned study progress —
     * nothing here is read, started, or completed, because no such fact exists yet.
     */
    val learningUnitCount: Int? = null,
)

/**
 * One recommendation as the browser presents it: the policy's own decision, plus the current name
 * the copy needs.
 *
 * [target] and [rationale] are the domain's typed values, copied verbatim. Presentation switches on
 * the rationale to pick localized wording and never re-derives, re-ranks, or second-guesses the
 * decision behind it — the reason shown is the exact fact the policy acted on.
 *
 * [topicName] is the only thing added here, because `LearningRecommendationRationale.UnseenCoverage`
 * identifies its Topic by stable ID and a display name must come from the catalogue this screen has
 * already loaded rather than from anything stored.
 */
internal data class RecommendedNextUiModel(
    val target: LearningRecommendationTarget,
    val rationale: LearningRecommendationRationale,
    /**
     * Current name of the Topic an unseen-coverage rationale names, or `null` when the rationale
     * names none or the catalogue no longer resolves it. A missing name degrades the copy; it never
     * withholds the recommendation, because the decision did not depend on the name.
     */
    val topicName: String? = null,
)

internal data class SubtopicSearchResult(
    val subtopicId: String,
    val subtopicName: String,
    val parentTopicId: String,
    val parentTopicName: String,
)
