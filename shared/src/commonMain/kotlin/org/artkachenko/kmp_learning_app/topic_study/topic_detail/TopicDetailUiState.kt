package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressUiState
import org.artkachenko.kmp_learning_app.lesson_study.TopicStudyProgress
import org.artkachenko.kmp_learning_app.ui.LearningContextUiModel

/**
 * A Topic is two independent capabilities, and this state says so.
 *
 * [Content] means the Topic exists and nothing more. It used to mean "the Topic exists and can be
 * practised", which was correct while Topic Detail was only a practice surface and became wrong the
 * moment a Topic could carry authored study material: a Topic with Learning Units and no active
 * Questions would have collapsed into a terminal "no questions" message that hid the very content
 * the learner opened it for. Practice availability is now read off [Content.topicQuestionCount]
 * rather than off the state's identity.
 */
internal sealed interface TopicDetailUiState {
    data object Loading : TopicDetailUiState

    data class Content(
        val topic: Topic,
        /**
         * Authored ACTIVE questions in this Topic, which is what practice can draw from — and, at
         * zero, the single reason the Topic cannot be practised.
         */
        val topicQuestionCount: Int,
        val subtopics: List<SubtopicPracticeItem>,
        /**
         * The study half of the Topic, loaded from a different publisher-owned source and therefore
         * carrying its own loading and failure states. Defaults to Loading so an unresolved study
         * section is simply absent rather than claiming the Topic has nothing to read.
         */
        val learningUnits: TopicLearningUnitsUiState = TopicLearningUnitsUiState.Loading,
        /**
         * Coverage and accuracy for the whole Topic, or `null` when analytics have not loaded or
         * could not be derived. Curriculum failure is an Error state; analytics failure is only an
         * absent summary, because starting practice must never depend on a statistic.
         */
        val learningContext: LearningContextUiModel? = null,
        /**
         * The learner's study record over this Topic's Learning Units, as a third independently
         * failing enrichment. It is not folded into [learningUnits], which answers whether the
         * publisher's Units can be read at all, and not into [learningContext], which is derived
         * from completed assessments: studied Lessons are not question coverage, and a screen that
         * merged them could manufacture either from the other. Every combination of the two states
         * is therefore legal, and a study-state failure leaves [learningUnits] Available.
         */
        val studyProgress: StudyProgressUiState<TopicStudyProgress> = StudyProgressUiState.Loading,
        /**
         * How many of this Topic's ACTIVE Questions are currently unresolved mistakes, or `null`
         * when completed history is unknown.
         *
         * Derived from the same history cache [learningContext] comes from, intersected with the
         * Question IDs the curriculum read above already produced — so it costs no extra repository
         * read and cannot count a mistake against a Question this Topic no longer holds. It is
         * nullable for the same reason [learningContext] is: unknown history is not an empty
         * queue, and a recommendation must never be made or withheld on a signal nobody read.
         */
        val unresolvedMistakeCount: Int? = null,
    ) : TopicDetailUiState

    data object NotFound : TopicDetailUiState

    data object Error : TopicDetailUiState
}
