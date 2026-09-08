package org.artkachenko.kmp_learning_app.lesson_study

/**
 * Current studied state of one Lesson the derivation included, keyed on stable Lesson identity.
 *
 * Nothing publisher-owned is carried here — no title, summary, body, or Subtopic mapping — because
 * every Learn surface already resolves those from `LearningContentRepository`, and a copy could only
 * disagree with the Lesson the learner is looking at. The persisted `studiedAt` is absent for the
 * same reason it is not needed: current completion is a question about identity, not about when the
 * claim was made, and weighting progress by recency would make the answer depend on history rather
 * than on the current curriculum.
 */
internal data class LessonStudyProgress(
    val lessonId: String,
    val isStudied: Boolean,
)

/**
 * Study progress over one Learning Unit's current ACTIVE Lessons, in authored order.
 *
 * [lessons] holds ACTIVE Lessons only, so a DEPRECATED Lesson is absent from the list and from both
 * sides of [summary]: retired material can never block completion of the material that currently
 * exists.
 */
internal data class LearningUnitStudyProgress(
    val unitId: String,
    val lessons: List<LessonStudyProgress>,
    val summary: StudyProgressSummary,
)

/**
 * Study progress over a Topic's current ACTIVE home Units, in authored order.
 *
 * [summary] is lesson-weighted across [units] rather than an average of their percentages, so a Unit
 * of ten Lessons counts for more than a Unit of two. [units] may still contain individually empty
 * Units, which contribute to neither side of the Topic aggregate.
 */
internal data class TopicStudyProgress(
    val topicId: String,
    val units: List<LearningUnitStudyProgress>,
    val summary: StudyProgressSummary,
)

/**
 * A studied count against an ACTIVE Lesson total, or the explicit absence of anything to study.
 *
 * [Empty] exists so that "no ACTIVE Lesson in scope" is an answer rather than an accident of an
 * empty denominator: `0 / 0` reads as complete, and 0% claims outstanding work that does not exist.
 * Neither is honest, so the case gets its own result and presentation is free to say so.
 *
 * Completion is a derived property of [Progress] rather than a third constructor argument, so a
 * caller cannot assemble a result that claims 2 of 5 Lessons are studied and that the Unit is
 * finished.
 */
internal sealed interface StudyProgressSummary {
    /** No current ACTIVE Lesson is in scope: neither 0% nor 100%, and neither complete nor not. */
    data object Empty : StudyProgressSummary

    data class Progress(
        val studiedCount: Int,
        val totalCount: Int,
    ) : StudyProgressSummary {
        init {
            require(totalCount > 0) { "Progress needs an ACTIVE Lesson total; use Empty instead." }
            require(studiedCount in 0..totalCount) {
                "studiedCount $studiedCount is outside 0..$totalCount."
            }
        }

        val isComplete: Boolean
            get() = studiedCount == totalCount
    }

    companion object {
        /** The single place the empty denominator becomes [Empty] rather than a zero-total count. */
        fun of(
            studiedCount: Int,
            totalCount: Int,
        ): StudyProgressSummary =
            if (totalCount == 0) {
                Empty
            } else {
                Progress(studiedCount = studiedCount, totalCount = totalCount)
            }
    }
}
