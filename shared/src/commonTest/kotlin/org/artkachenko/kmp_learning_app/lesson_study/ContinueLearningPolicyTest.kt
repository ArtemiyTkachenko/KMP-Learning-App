package org.artkachenko.kmp_learning_app.lesson_study

import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus

/**
 * The whole Continue Learning decision, proved here and nowhere else.
 *
 * The policy is pure over content and identities, so every case is expressible as a list and a set —
 * no repository, no coroutine, no back stack. Presentation tests assert what the ViewModel does with
 * the outcome and what the card renders; neither re-proves which Lesson is next.
 */
internal class ContinueLearningPolicyTest {
    @Test
    fun aLearnerWithNoProgressStartsAtTheFirstLessonOfTheFirstUnit() {
        val outcome = ContinueLearningPolicy.resolve(
            units = listOf(
                learningUnit("unit_a", lessons = listOf(learningLesson("a1"), learningLesson("a2"))),
                learningUnit("unit_b", lessons = listOf(learningLesson("b1"))),
            ),
            studiedLessonIds = emptySet(),
        )

        assertEquals(next("unit_a", "a1"), outcome)
    }

    @Test
    fun partialProgressResumesAtTheFirstUnstudiedLessonInAuthoredOrder() {
        val outcome = ContinueLearningPolicy.resolve(
            units = listOf(
                learningUnit(
                    "unit_a",
                    lessons = listOf(learningLesson("a1"), learningLesson("a2"), learningLesson("a3")),
                ),
            ),
            studiedLessonIds = setOf("a1"),
        )

        assertEquals(next("unit_a", "a2"), outcome)
    }

    /**
     * The answer is positional, not "the one after the newest mark": a learner who marked a later
     * Lesson first is sent back to the earliest gap, because authored order is the curriculum's
     * pedagogical sequence and nothing here reads when anything was marked.
     */
    @Test
    fun anEarlierGapWinsOverALaterStudiedLesson() {
        val outcome = ContinueLearningPolicy.resolve(
            units = listOf(
                learningUnit(
                    "unit_a",
                    lessons = listOf(learningLesson("a1"), learningLesson("a2"), learningLesson("a3")),
                ),
            ),
            studiedLessonIds = setOf("a3"),
        )

        assertEquals(next("unit_a", "a1"), outcome)
    }

    @Test
    fun aFullyStudiedUnitIsSkippedAndTheNextUnitContinues() {
        val outcome = ContinueLearningPolicy.resolve(
            units = listOf(
                learningUnit("unit_a", lessons = listOf(learningLesson("a1"), learningLesson("a2"))),
                learningUnit("unit_b", lessons = listOf(learningLesson("b1"))),
            ),
            studiedLessonIds = setOf("a1", "a2"),
        )

        assertEquals(next("unit_b", "b1"), outcome)
    }

    /**
     * The Units interleave Topics on purpose. A policy that grouped by Topic, or sorted by title or
     * ID, would answer `unit_ui_second` here instead of the Unit the author actually placed second.
     */
    @Test
    fun globalAuthoredUnitOrderIsWalkedRatherThanTopicGroupingOrIdOrder() {
        val outcome = ContinueLearningPolicy.resolve(
            units = listOf(
                learningUnit(
                    "unit_ui_first",
                    topicId = "android_ui",
                    lessons = listOf(learningLesson("ui1")),
                ),
                learningUnit(
                    "unit_coroutines",
                    topicId = "kotlin_coroutines",
                    lessons = listOf(learningLesson("co1")),
                ),
                learningUnit(
                    "unit_ui_second",
                    topicId = "android_ui",
                    lessons = listOf(learningLesson("ui2")),
                ),
            ),
            studiedLessonIds = setOf("ui1"),
        )

        assertEquals(next("unit_coroutines", "co1"), outcome)
    }

    @Test
    fun aDeprecatedUnitCanNeverBeTheNextDestination() {
        val outcome = ContinueLearningPolicy.resolve(
            units = listOf(
                learningUnit(
                    "unit_retired",
                    lessons = listOf(learningLesson("retired1")),
                    status = ContentStatus.DEPRECATED,
                ),
                learningUnit("unit_a", lessons = listOf(learningLesson("a1"))),
            ),
            studiedLessonIds = emptySet(),
        )

        assertEquals(next("unit_a", "a1"), outcome)
    }

    @Test
    fun aDeprecatedLessonIsSkippedInsideAnActiveUnit() {
        val outcome = ContinueLearningPolicy.resolve(
            units = listOf(
                learningUnit(
                    "unit_a",
                    lessons = listOf(
                        learningLesson("a1", status = ContentStatus.DEPRECATED),
                        learningLesson("a2"),
                    ),
                ),
            ),
            studiedLessonIds = emptySet(),
        )

        assertEquals(next("unit_a", "a2"), outcome)
    }

    /**
     * A retired Lesson also cannot block completion: the Unit is finished once its ACTIVE Lessons
     * are studied, even though a DEPRECATED Lesson beside them was never marked.
     */
    @Test
    fun aDeprecatedLessonCannotHoldACourseBackFromComplete() {
        val outcome = ContinueLearningPolicy.resolve(
            units = listOf(
                learningUnit(
                    "unit_a",
                    lessons = listOf(
                        learningLesson("a1"),
                        learningLesson("a_retired", status = ContentStatus.DEPRECATED),
                    ),
                ),
            ),
            studiedLessonIds = setOf("a1"),
        )

        assertEquals(ContinueLearningOutcome.Complete, outcome)
    }

    @Test
    fun studyRecordsForLessonsThatNoLongerResolveAreIgnored() {
        val outcome = ContinueLearningPolicy.resolve(
            units = listOf(learningUnit("unit_a", lessons = listOf(learningLesson("a1")))),
            // Two identities the current document knows nothing about: a Lesson that was removed and
            // one that was never in this document at all. Neither may satisfy or skip anything.
            studiedLessonIds = setOf("lesson_removed", "lesson_from_another_life"),
        )

        assertEquals(next("unit_a", "a1"), outcome)
    }

    @Test
    fun everyActiveLessonStudiedIsCompleteRatherThanAnArbitraryLesson() {
        val outcome = ContinueLearningPolicy.resolve(
            units = listOf(
                learningUnit("unit_a", lessons = listOf(learningLesson("a1"))),
                learningUnit("unit_b", lessons = listOf(learningLesson("b1"))),
            ),
            studiedLessonIds = setOf("a1", "b1"),
        )

        assertEquals(ContinueLearningOutcome.Complete, outcome)
    }

    /**
     * The distinction the whole outcome type exists for: nothing to study is not the same statement
     * as everything studied, and an empty studied set must not turn one into the other.
     */
    @Test
    fun contentWithNoActiveLessonIsEmptyRatherThanComplete() {
        val noUnits = ContinueLearningPolicy.resolve(units = emptyList(), studiedLessonIds = emptySet())
        val onlyDeprecatedUnits = ContinueLearningPolicy.resolve(
            units = listOf(
                learningUnit(
                    "unit_retired",
                    lessons = listOf(learningLesson("retired1")),
                    status = ContentStatus.DEPRECATED,
                ),
            ),
            studiedLessonIds = emptySet(),
        )
        val onlyDeprecatedLessons = ContinueLearningPolicy.resolve(
            units = listOf(
                learningUnit(
                    "unit_a",
                    lessons = listOf(learningLesson("a1", status = ContentStatus.DEPRECATED)),
                ),
            ),
            studiedLessonIds = emptySet(),
        )
        val emptyUnit = ContinueLearningPolicy.resolve(
            units = listOf(learningUnit("unit_a", lessons = emptyList())),
            studiedLessonIds = emptySet(),
        )

        assertEquals(ContinueLearningOutcome.Empty, noUnits)
        assertEquals(ContinueLearningOutcome.Empty, onlyDeprecatedUnits)
        assertEquals(ContinueLearningOutcome.Empty, onlyDeprecatedLessons)
        assertEquals(ContinueLearningOutcome.Empty, emptyUnit)
    }

    @Test
    fun anEmptyUnitIsWalkedThroughRatherThanEndingTheSearch() {
        val outcome = ContinueLearningPolicy.resolve(
            units = listOf(
                learningUnit("unit_empty", lessons = emptyList()),
                learningUnit("unit_a", lessons = listOf(learningLesson("a1"))),
            ),
            studiedLessonIds = emptySet(),
        )

        assertEquals(next("unit_a", "a1"), outcome)
    }

    @Test
    fun theSameContentAndStudyStateAlwaysGiveTheSameAnswer() {
        val units = listOf(
            learningUnit("unit_a", lessons = listOf(learningLesson("a1"), learningLesson("a2"))),
            learningUnit("unit_b", lessons = listOf(learningLesson("b1"))),
        )
        val studied = setOf("a1")

        val answers = List(5) { ContinueLearningPolicy.resolve(units, studied) }.distinct()

        assertEquals(listOf(next("unit_a", "a2")), answers)
    }

    private fun next(unitId: String, lessonId: String): ContinueLearningOutcome.Next =
        ContinueLearningOutcome.Next(ContinueLearningTarget(unitId = unitId, lessonId = lessonId))
}
