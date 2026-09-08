package org.artkachenko.kmp_learning_app.lesson_study

/**
 * Where Continue Learning sends the learner: one Lesson, inside the Unit it was authored in.
 *
 * Stable identity only. No title, summary, or authored position travels, for the same reason
 * `AppRoute.LearningLesson` carries none — the Lesson is resolved from current publisher content on
 * arrival, so a re-authored Lesson can never be opened under prose captured when the card was drawn.
 * The Unit is carried because the existing Lesson route addresses a Lesson *within* a Unit, and
 * reconstructing that parent at navigation time would be a second, weaker answer to a question the
 * walk already answered exactly.
 */
internal data class ContinueLearningTarget(
    val unitId: String,
    val lessonId: String,
)

/**
 * What Continue Learning currently has to say about the learner's next Lesson.
 *
 * [Complete] and [Empty] are separate cases and must not collapse into each other: "you have studied
 * everything there is" is a statement about the learner, "there is nothing here to study" is a
 * statement about the content, and rendering either as the other congratulates a learner who has
 * done nothing or hides a finished course.
 *
 * There is deliberately no loading or failure case here. This type answers the question only when
 * both inputs are known; a caller that cannot read learning content or study state has no outcome to
 * present, and inventing one — an empty studied set, an empty curriculum — is exactly the
 * fabrication the study-progress contract forbids.
 */
internal sealed interface ContinueLearningOutcome {
    /** At least one current ACTIVE Lesson is unstudied, and [target] names the first of them. */
    data class Next(val target: ContinueLearningTarget) : ContinueLearningOutcome

    /** Current ACTIVE Lessons exist and every one of them is marked studied. */
    data object Complete : ContinueLearningOutcome

    /** The current curriculum publishes no ACTIVE Lesson eligible for study at all. */
    data object Empty : ContinueLearningOutcome
}
