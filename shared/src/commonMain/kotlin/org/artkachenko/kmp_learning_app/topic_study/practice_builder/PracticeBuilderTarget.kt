package org.artkachenko.kmp_learning_app.topic_study.practice_builder

/**
 * What the learner chose to practise, before it is anything an assessment can run.
 *
 * A target is a *navigation* identity — the content the learner tapped, addressed by the stable ID
 * that screen knows — while `AssessmentScope` is an *assessment* identity: which Questions are
 * eligible. For a Topic or a Subtopic the two happen to coincide, which is why the builder used to
 * take a scope directly. A Learning Unit breaks that coincidence: its scope is the set of concepts
 * its current Lessons are responsible for teaching, which only exists after the Unit has been read
 * from `LearningContentRepository`.
 *
 * Keeping the distinction in the type is what stops the derived set from being computed on the
 * screen that offers practice and carried through the back stack, where it would quietly outlive
 * the authoring it was derived from. [PracticeTargetResolver] is the one place the crossing
 * happens.
 */
internal sealed interface PracticeBuilderTarget {
    data class Topic(
        val topicId: String,
    ) : PracticeBuilderTarget

    data class Subtopic(
        val subtopicId: String,
    ) : PracticeBuilderTarget

    /**
     * One authored Learning Unit, practised on the concepts it currently teaches.
     *
     * Named `LearningUnit` rather than `Unit` because `kotlin.Unit` is always in scope, and a
     * builder target that reads as the empty type would be actively misleading.
     */
    data class LearningUnit(
        val unitId: String,
    ) : PracticeBuilderTarget
}
