package org.artkachenko.kmp_learning_app.mistake_review

import kotlin.time.Instant
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem

/**
 * A Question still on the mistake review schedule; see `MistakeScheduleDerivation`.
 *
 * [sourceAttemptId] identifies the attempt the latest incorrect occurrence came from — the answer
 * [reviewItem] shows. It is not user-visible; it exists so the occurrence selection stays provable
 * in tests and debuggable later.
 *
 * [isDue] was decided when the queue was derived; [dueFrom] is when a coming-up entry becomes due.
 */
internal data class UnresolvedMistake(
    val questionId: String,
    val sourceAttemptId: String,
    val reviewItem: ReviewQuestionItem,
    val dueFrom: Instant,
    val isDue: Boolean,
    val studyLesson: MistakeStudyLesson? = null,
)

internal data class MistakeStudyLesson(
    val unitId: String,
    val lessonId: String,
    val title: String,
)
