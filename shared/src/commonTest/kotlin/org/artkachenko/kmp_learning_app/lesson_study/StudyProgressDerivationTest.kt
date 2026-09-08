package org.artkachenko.kmp_learning_app.lesson_study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus

/**
 * The fixture is authored, not alphabetical: Units and Lessons are named so that authored order and
 * alphabetical order disagree, which is what makes the ordering assertions meaningful.
 */
internal class StudyProgressDerivationTest {
    @Test
    fun anUnstudiedUnitReportsEveryActiveLessonAsUnstudied() {
        val progress = StudyProgressDerivation.deriveUnit(
            learningUnit(
                id = "unit_compose",
                lessons = listOf(
                    learningLesson("lesson_a"),
                    learningLesson("lesson_b"),
                    learningLesson("lesson_c"),
                ),
            ),
            studiedLessonIds = emptySet(),
        )

        assertEquals(
            listOf(
                LessonStudyProgress("lesson_a", isStudied = false),
                LessonStudyProgress("lesson_b", isStudied = false),
                LessonStudyProgress("lesson_c", isStudied = false),
            ),
            progress.lessons,
        )
        assertEquals(StudyProgressSummary.Progress(studiedCount = 0, totalCount = 3), progress.summary)
        assertFalse(progress.summary.requireProgress().isComplete)
    }

    @Test
    fun aPartiallyStudiedUnitMarksOnlyTheStudiedLessons() {
        val progress = StudyProgressDerivation.deriveUnit(
            learningUnit(
                id = "unit_compose",
                lessons = listOf(
                    learningLesson("lesson_a"),
                    learningLesson("lesson_b"),
                    learningLesson("lesson_c"),
                ),
            ),
            studiedLessonIds = setOf("lesson_b"),
        )

        assertEquals(
            listOf(
                LessonStudyProgress("lesson_a", isStudied = false),
                LessonStudyProgress("lesson_b", isStudied = true),
                LessonStudyProgress("lesson_c", isStudied = false),
            ),
            progress.lessons,
        )
        assertEquals(StudyProgressSummary.Progress(studiedCount = 1, totalCount = 3), progress.summary)
        assertFalse(progress.summary.requireProgress().isComplete)
    }

    @Test
    fun aFullyStudiedUnitIsComplete() {
        val progress = StudyProgressDerivation.deriveUnit(
            learningUnit(
                id = "unit_compose",
                lessons = listOf(
                    learningLesson("lesson_a"),
                    learningLesson("lesson_b"),
                    learningLesson("lesson_c"),
                ),
            ),
            studiedLessonIds = setOf("lesson_a", "lesson_b", "lesson_c"),
        )

        assertEquals(StudyProgressSummary.Progress(studiedCount = 3, totalCount = 3), progress.summary)
        assertTrue(progress.summary.requireProgress().isComplete)
    }

    @Test
    fun aDeprecatedLessonLeavesBothSidesOfTheUnitFraction() {
        val unit = learningUnit(
            id = "unit_compose",
            lessons = listOf(
                learningLesson("lesson_a"),
                learningLesson("lesson_b"),
                learningLesson("lesson_c", status = ContentStatus.DEPRECATED),
            ),
        )

        val progress = StudyProgressDerivation.deriveUnit(unit, studiedLessonIds = setOf("lesson_a", "lesson_c"))

        // The retired Lesson is absent from the output and from the denominator, so studying it
        // cannot raise the numerator either.
        assertEquals(listOf("lesson_a", "lesson_b"), progress.lessons.map { it.lessonId })
        assertEquals(StudyProgressSummary.Progress(studiedCount = 1, totalCount = 2), progress.summary)
    }

    @Test
    fun anUnstudiedDeprecatedLessonCannotBlockUnitCompletion() {
        val unit = learningUnit(
            id = "unit_compose",
            lessons = listOf(
                learningLesson("lesson_a"),
                learningLesson("lesson_b"),
                learningLesson("lesson_c", status = ContentStatus.DEPRECATED),
            ),
        )

        val progress = StudyProgressDerivation.deriveUnit(unit, studiedLessonIds = setOf("lesson_a", "lesson_b"))

        assertEquals(StudyProgressSummary.Progress(studiedCount = 2, totalCount = 2), progress.summary)
        assertTrue(progress.summary.requireProgress().isComplete)
    }

    @Test
    fun aStudyRecordForAnUnresolvableLessonIsIgnoredWithoutError() {
        val unit = learningUnit(
            id = "unit_compose",
            lessons = listOf(learningLesson("lesson_a"), learningLesson("lesson_b")),
        )

        val progress = StudyProgressDerivation.deriveUnit(
            unit,
            studiedLessonIds = setOf("lesson_a", "lesson_deleted_last_release"),
        )

        assertEquals(listOf("lesson_a", "lesson_b"), progress.lessons.map { it.lessonId })
        assertEquals(StudyProgressSummary.Progress(studiedCount = 1, totalCount = 2), progress.summary)
    }

    @Test
    fun aUnitWithNoActiveLessonsIsEmptyRatherThanZeroOrComplete() {
        val noLessons = StudyProgressDerivation.deriveUnit(
            learningUnit(id = "unit_planned", lessons = emptyList()),
            studiedLessonIds = setOf("lesson_a"),
        )
        val onlyDeprecatedLessons = StudyProgressDerivation.deriveUnit(
            learningUnit(
                id = "unit_retired",
                lessons = listOf(learningLesson("lesson_a", status = ContentStatus.DEPRECATED)),
            ),
            studiedLessonIds = setOf("lesson_a"),
        )

        assertEquals(emptyList(), noLessons.lessons)
        assertEquals(StudyProgressSummary.Empty, noLessons.summary)
        assertEquals(emptyList(), onlyDeprecatedLessons.lessons)
        assertEquals(StudyProgressSummary.Empty, onlyDeprecatedLessons.summary)
    }

    @Test
    fun rewritingALessonUnderTheSameIdKeepsItStudied() {
        val studied = setOf("lesson_a")
        val before = learningUnit(
            id = "unit_compose",
            lessons = listOf(learningLesson("lesson_a", title = "Thinking in Compose", body = "The original text.")),
        )
        val after = learningUnit(
            id = "unit_compose",
            lessons = listOf(
                learningLesson(
                    id = "lesson_a",
                    title = "Thinking in Compose, revised",
                    primarySubtopicIds = listOf("compose_state"),
                    body = "A completely rewritten explanation with new examples and new sources.",
                ),
            ),
        )

        // Only stable identity decides this. No title, body, section, source, or hash comparison
        // takes part, so an editorial revision never resets the learner's claim.
        assertEquals(
            StudyProgressDerivation.deriveUnit(before, studied).summary,
            StudyProgressDerivation.deriveUnit(after, studied).summary,
        )
        assertTrue(StudyProgressDerivation.deriveUnit(after, studied).lessons.single().isStudied)
    }

    @Test
    fun republishingALessonUnderANewIdMakesItUnstudied() {
        val unit = learningUnit(id = "unit_compose", lessons = listOf(learningLesson("lesson_new")))

        val progress = StudyProgressDerivation.deriveUnit(unit, studiedLessonIds = setOf("lesson_old"))

        // A new ID is how an author says the accomplishment is a different one, so nothing migrates.
        assertFalse(progress.lessons.single().isStudied)
        assertEquals(StudyProgressSummary.Progress(studiedCount = 0, totalCount = 1), progress.summary)
    }

    @Test
    fun aTopicWithNoActiveHomeUnitsIsEmpty() {
        val progress = StudyProgressDerivation.deriveTopic(
            topicId = "android_ui",
            units = emptyList(),
            studiedLessonIds = setOf("lesson_a"),
        )

        assertEquals(emptyList(), progress.units)
        assertEquals(StudyProgressSummary.Empty, progress.summary)
    }

    @Test
    fun aTopicWhoseActiveUnitsHoldNoActiveLessonsIsEmpty() {
        val progress = StudyProgressDerivation.deriveTopic(
            topicId = "android_ui",
            units = listOf(
                learningUnit(id = "unit_planned", lessons = emptyList()),
                learningUnit(
                    id = "unit_retired",
                    lessons = listOf(learningLesson("lesson_a", status = ContentStatus.DEPRECATED)),
                ),
            ),
            studiedLessonIds = setOf("lesson_a"),
        )

        // The Units are still reported, each empty in its own right; the Topic has no denominator.
        assertEquals(listOf("unit_planned", "unit_retired"), progress.units.map { it.unitId })
        assertEquals(listOf(StudyProgressSummary.Empty, StudyProgressSummary.Empty), progress.units.map { it.summary })
        assertEquals(StudyProgressSummary.Empty, progress.summary)
    }

    @Test
    fun topicProgressIsLessonWeightedRatherThanAnAverageOfUnitCompletion() {
        val progress = StudyProgressDerivation.deriveTopic(
            topicId = "android_ui",
            units = listOf(
                learningUnit(
                    id = "unit_small",
                    lessons = listOf(learningLesson("lesson_a"), learningLesson("lesson_b")),
                ),
                learningUnit(
                    id = "unit_large",
                    lessons = listOf(
                        learningLesson("lesson_c"),
                        learningLesson("lesson_d"),
                        learningLesson("lesson_e"),
                    ),
                ),
            ),
            studiedLessonIds = setOf("lesson_a", "lesson_c", "lesson_d"),
        )

        // 1/2 and 2/3 aggregate to 3/5 by Lesson count. Averaging the two completion ratios would
        // give 58.3% instead of 60%, and would let a two-Lesson Unit outweigh a ten-Lesson one.
        assertEquals(
            listOf(
                StudyProgressSummary.Progress(studiedCount = 1, totalCount = 2),
                StudyProgressSummary.Progress(studiedCount = 2, totalCount = 3),
            ),
            progress.units.map { it.summary },
        )
        assertEquals(StudyProgressSummary.Progress(studiedCount = 3, totalCount = 5), progress.summary)
    }

    @Test
    fun aTopicIsCompleteWhenEveryActiveLessonIsStudiedDespiteAnEmptyUnit() {
        val progress = StudyProgressDerivation.deriveTopic(
            topicId = "android_ui",
            units = listOf(
                learningUnit(id = "unit_planned", lessons = emptyList()),
                learningUnit(
                    id = "unit_compose",
                    lessons = listOf(learningLesson("lesson_a"), learningLesson("lesson_b")),
                ),
            ),
            studiedLessonIds = setOf("lesson_a", "lesson_b"),
        )

        assertEquals(StudyProgressSummary.Progress(studiedCount = 2, totalCount = 2), progress.summary)
        assertTrue(progress.summary.requireProgress().isComplete)
    }

    @Test
    fun crossTopicSubtopicMappingsDoNotMoveStudyCompletionBetweenTopics() {
        val units = listOf(
            learningUnit(
                id = "unit_compose",
                topicId = "android_ui",
                lessons = listOf(
                    learningLesson(
                        id = "lesson_side_effects",
                        // What the Lesson teaches spans Topics; where its study progress lives does
                        // not. Both mappings name concepts owned by other Topics.
                        primarySubtopicIds = listOf("lifecycle_owner"),
                        supportingSubtopicIds = listOf("architecture_state_holder"),
                    ),
                ),
            ),
            learningUnit(
                id = "unit_lifecycle",
                topicId = "android_lifecycle",
                lessons = listOf(learningLesson("lesson_lifecycle_basics")),
            ),
        )
        val studied = setOf("lesson_side_effects")

        val homeTopic = StudyProgressDerivation.deriveTopic("android_ui", units, studied)
        val mappedTopic = StudyProgressDerivation.deriveTopic("android_lifecycle", units, studied)

        assertEquals(listOf("unit_compose"), homeTopic.units.map { it.unitId })
        assertEquals(StudyProgressSummary.Progress(studiedCount = 1, totalCount = 1), homeTopic.summary)
        assertEquals(listOf("unit_lifecycle"), mappedTopic.units.map { it.unitId })
        assertEquals(StudyProgressSummary.Progress(studiedCount = 0, totalCount = 1), mappedTopic.summary)
    }

    @Test
    fun aDeprecatedUnitIsAbsentFromTopicProgress() {
        val progress = StudyProgressDerivation.deriveTopic(
            topicId = "android_ui",
            units = listOf(
                learningUnit(id = "unit_compose", lessons = listOf(learningLesson("lesson_a"))),
                learningUnit(
                    id = "unit_retired",
                    lessons = listOf(learningLesson("lesson_b")),
                    status = ContentStatus.DEPRECATED,
                ),
            ),
            studiedLessonIds = setOf("lesson_a", "lesson_b"),
        )

        assertEquals(listOf("unit_compose"), progress.units.map { it.unitId })
        assertEquals(StudyProgressSummary.Progress(studiedCount = 1, totalCount = 1), progress.summary)
    }

    @Test
    fun authoredUnitAndLessonOrderSurvivesFiltering() {
        val progress = StudyProgressDerivation.deriveTopic(
            topicId = "android_ui",
            units = listOf(
                learningUnit(id = "unit_c", lessons = listOf(learningLesson("lesson_c1"))),
                learningUnit(
                    id = "unit_a",
                    lessons = listOf(
                        learningLesson("lesson_a3"),
                        learningLesson("lesson_a9", status = ContentStatus.DEPRECATED),
                        learningLesson("lesson_a1"),
                        learningLesson("lesson_a2"),
                    ),
                ),
                learningUnit(
                    id = "unit_z",
                    lessons = listOf(learningLesson("lesson_z1")),
                    status = ContentStatus.DEPRECATED,
                ),
                learningUnit(
                    id = "unit_b",
                    topicId = "android_lifecycle",
                    lessons = listOf(learningLesson("lesson_b1")),
                ),
            ),
            studiedLessonIds = emptySet(),
        )

        // Authored order is the curriculum's pedagogical order, and removing an ineligible item must
        // not reorder its neighbours or sort them by ID.
        assertEquals(listOf("unit_c", "unit_a"), progress.units.map { it.unitId })
        assertEquals(
            listOf("lesson_a3", "lesson_a1", "lesson_a2"),
            progress.units[1].lessons.map { it.lessonId },
        )
    }

    @Test
    fun theSameContentAndStudiedIdentitiesAlwaysProduceTheSameResult() {
        val units = listOf(
            learningUnit(
                id = "unit_compose",
                lessons = listOf(learningLesson("lesson_a"), learningLesson("lesson_b")),
            ),
            learningUnit(id = "unit_state", lessons = listOf(learningLesson("lesson_c"))),
        )

        val first = StudyProgressDerivation.deriveTopic(
            topicId = "android_ui",
            units = units,
            studiedLessonIds = linkedSetOf("lesson_a", "lesson_c", "lesson_orphan"),
        )
        val second = StudyProgressDerivation.deriveTopic(
            topicId = "android_ui",
            units = units,
            // A different encounter order, and a duplicate identity, must change nothing: output
            // order comes from authored content and membership is by identity.
            studiedLessonIds = linkedSetOf("lesson_orphan", "lesson_c", "lesson_a", "lesson_c"),
        )

        assertEquals(first, second)
        assertEquals(StudyProgressSummary.Progress(studiedCount = 2, totalCount = 3), first.summary)
    }

    private fun StudyProgressSummary.requireProgress(): StudyProgressSummary.Progress =
        assertIs<StudyProgressSummary.Progress>(this)
}
