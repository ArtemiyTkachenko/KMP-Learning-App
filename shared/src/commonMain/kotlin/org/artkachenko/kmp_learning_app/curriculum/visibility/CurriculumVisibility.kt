package org.artkachenko.kmp_learning_app.curriculum.visibility

/**
 * Which Topics the learner currently sees, as the set of Topic IDs that are hidden.
 *
 * Visibility is not existence. A hidden Topic is removed from every eligibility read — the ACTIVE
 * catalogue, practice selection, learning browsing — and from derived history, but it still
 * resolves by ID, so stored attempts, saved Questions and study records that reference it stay
 * readable and reappear unchanged when the Topic becomes visible again.
 *
 * Hiding is by Topic only, because a Topic is the unit the curriculum is authored and classified
 * in: every Question, Subtopic and Learning Unit has exactly one home Topic, and the
 * content-boundary invariants guarantee no core content depends on optional content.
 */
internal data class CurriculumVisibility(val hiddenTopicIds: Set<String>) {

    fun isTopicVisible(topicId: String): Boolean = topicId !in hiddenTopicIds

    /** True when nothing is hidden, so a caller can skip classification work entirely. */
    val hidesNothing: Boolean
        get() = hiddenTopicIds.isEmpty()

    internal companion object {
        /**
         * The Topic that holds Kotlin Multiplatform material.
         *
         * The only place application code names it: Topic membership is the whole classification,
         * so no Question, Unit or screen carries a KMP flag of its own, and nothing outside this
         * companion compares a Topic ID with this value.
         */
        private const val KotlinMultiplatformTopicId: String = "kmp"

        /**
         * The section [topicId] is presented in: the Kotlin Multiplatform Topic in its own, every
         * other Topic in Android Engineering. Uses the same constant as [from], so the Topic that is
         * optional and the Topic that is grouped apart cannot drift apart.
         */
        fun sectionOf(topicId: String): CurriculumSection =
            if (topicId == KotlinMultiplatformTopicId) {
                CurriculumSection.KotlinMultiplatform
            } else {
                CurriculumSection.AndroidEngineering
            }

        /** The visibility the learner's Kotlin Multiplatform preference implies. */
        fun from(includeKmpContent: Boolean): CurriculumVisibility =
            CurriculumVisibility(
                hiddenTopicIds = if (includeKmpContent) emptySet() else setOf(KotlinMultiplatformTopicId),
            )
    }
}
