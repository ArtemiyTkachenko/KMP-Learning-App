package org.artkachenko.kmp_learning_app.topic_study.learning_unit

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository
import org.artkachenko.kmp_learning_app.lesson_study.FakeLessonStudyRepository
import org.artkachenko.kmp_learning_app.lesson_study.LearningUnitStudyProgress
import org.artkachenko.kmp_learning_app.lesson_study.StudiedLesson
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressStateHolder
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressSummary
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressUiState
import org.artkachenko.kmp_learning_app.lesson_study.studyProgressStateHolder
import org.artkachenko.kmp_learning_app.topic_study.FakeLearningContentRepository
import org.artkachenko.kmp_learning_app.topic_study.testLearningLesson
import org.artkachenko.kmp_learning_app.topic_study.testLearningUnit

@OptIn(ExperimentalCoroutinesApi::class)
internal class LearningUnitViewModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * The overview's whole job: current Unit prose, and the Lessons that are current study material
     * in the order they were authored in. The deprecated Lesson between them is what makes the
     * order assertion meaningful — filtering must not disturb the sequence around it.
     */
    @Test
    fun anActiveUnitListsItsActiveLessonsInAuthoredOrder() = runViewModelTest {
        val viewModel = viewModel(
            unitId = "unit_a",
            repository = FakeLearningContentRepository(
                units = listOf(
                    testLearningUnit(
                        id = "unit_a",
                        lessons = listOf(
                            testLearningLesson("lesson_first"),
                            testLearningLesson("lesson_retired", ContentStatus.DEPRECATED),
                            testLearningLesson("lesson_second"),
                        ),
                    ),
                ),
            ),
        )

        advanceUntilIdle()

        val state = assertIs<LearningUnitUiState.Content>(viewModel.uiState.value)
        assertEquals("unit_a", state.unitId)
        assertEquals("Title of unit_a", state.title)
        assertEquals("Summary of unit_a", state.summary)
        assertEquals(
            listOf("lesson_first", "lesson_second"),
            state.lessons.map { it.lessonId },
        )
        assertEquals("Title of lesson_first", state.lessons.first().title)
        assertEquals("Summary of lesson_first", state.lessons.first().summary)
    }

    @Test
    fun anUnknownUnitIdIsNotFound() = runViewModelTest {
        val viewModel = viewModel(
            unitId = "unit_missing",
            repository = FakeLearningContentRepository(units = listOf(testLearningUnit("unit_a"))),
        )

        advanceUntilIdle()

        assertEquals(LearningUnitUiState.NotFound, viewModel.uiState.value)
    }

    /**
     * The E20 contract this destination has to filter on top of: the stable lookup still resolves
     * retired material, and normal browsing must not present it as somewhere to study.
     */
    @Test
    fun aDeprecatedUnitResolvesByIdAndIsStillNotFoundForBrowsing() = runViewModelTest {
        val repository = FakeLearningContentRepository(
            units = listOf(
                testLearningUnit(
                    id = "unit_retired",
                    lessons = listOf(testLearningLesson("lesson_a")),
                    status = ContentStatus.DEPRECATED,
                ),
            ),
        )
        val viewModel = viewModel(unitId = "unit_retired", repository = repository)

        advanceUntilIdle()

        assertEquals(LearningUnitUiState.NotFound, viewModel.uiState.value)
        // The repository was asked and answered; the ACTIVE rule is presentation's, not its.
        assertEquals(listOf("unit_retired"), repository.unitReadIds)
    }

    @Test
    fun aRepositoryFailureIsAnErrorRatherThanNotFound() = runViewModelTest {
        val viewModel = viewModel(
            unitId = "unit_a",
            repository = FakeLearningContentRepository(
                units = listOf(testLearningUnit("unit_a")),
                failuresRemaining = 1,
            ),
        )

        advanceUntilIdle()

        // Unreadable is not absent: telling the learner the Unit is gone would be wrong, and
        // there would be nothing to retry.
        assertEquals(LearningUnitUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun retryAfterAFailureLoadsTheUnit() = runViewModelTest {
        val viewModel = viewModel(
            unitId = "unit_a",
            repository = FakeLearningContentRepository(
                units = listOf(
                    testLearningUnit("unit_a", lessons = listOf(testLearningLesson("lesson_a"))),
                ),
                failuresRemaining = 1,
            ),
        )
        advanceUntilIdle()
        assertEquals(LearningUnitUiState.Error, viewModel.uiState.value)

        viewModel.retry()
        advanceUntilIdle()

        val state = assertIs<LearningUnitUiState.Content>(viewModel.uiState.value)
        assertEquals(listOf("lesson_a"), state.lessons.map { it.lessonId })
    }

    /**
     * Content authoring should not produce this, but a Unit whose Lessons have all been retired is
     * a coherent document and must render as an empty overview rather than crash or claim the Unit
     * does not exist.
     */
    @Test
    fun anActiveUnitWithNoActiveLessonsIsStillContent() = runViewModelTest {
        val viewModel = viewModel(
            unitId = "unit_a",
            repository = FakeLearningContentRepository(
                units = listOf(
                    testLearningUnit(
                        id = "unit_a",
                        lessons = listOf(testLearningLesson("lesson_a", ContentStatus.DEPRECATED)),
                    ),
                ),
            ),
        )

        advanceUntilIdle()

        val state = assertIs<LearningUnitUiState.Content>(viewModel.uiState.value)
        assertTrue(state.lessons.isEmpty())
    }


    @Test
    fun unitStudyProgressIsDerivedFromTheCurrentActiveLessonsAndTheStudiedIdentities() =
        runViewModelTest {
            val viewModel = viewModel(
                unitId = "unit_a",
                repository = threeLessonUnit(),
                studyProgressStateHolder = studyProgressStateHolder(
                    FakeLessonStudyRepository(
                        StudiedLesson("lesson_b", 2_000),
                        // A record for a Lesson this Unit retired, and one for a Lesson that no
                        // longer resolves at all: neither may reach either side of the fraction.
                        StudiedLesson("lesson_retired", 3_000),
                        StudiedLesson("lesson_deleted", 4_000),
                    ),
                ),
            )
            advanceUntilIdle()

            val progress = studyProgress(viewModel)
            assertEquals(
                listOf("lesson_a", "lesson_b", "lesson_c"),
                progress.lessons.map { it.lessonId },
            )
            assertEquals(listOf(false, true, false), progress.lessons.map { it.isStudied })
            assertEquals(StudyProgressSummary.Progress(studiedCount = 1, totalCount = 3), progress.summary)
        }

    /** A Unit whose Lessons have all been retired reports the explicit empty result, not 0 or 100%. */
    @Test
    fun aUnitWithNoCurrentLessonsReportsEmptyStudyProgress() = runViewModelTest {
        val viewModel = viewModel(
            unitId = "unit_a",
            repository = FakeLearningContentRepository(
                units = listOf(
                    testLearningUnit(
                        "unit_a",
                        lessons = listOf(
                            testLearningLesson("lesson_retired", status = ContentStatus.DEPRECATED),
                        ),
                    ),
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(StudyProgressSummary.Empty, studyProgress(viewModel).summary)
    }

    @Test
    fun anUnreadableStudyRecordLeavesTheUnitReadableWithUnavailableStudyProgress() =
        runViewModelTest {
            val repository = FakeLessonStudyRepository()
            repository.failReads = true
            val viewModel = viewModel(
                unitId = "unit_a",
                repository = threeLessonUnit(),
                studyProgressStateHolder = studyProgressStateHolder(repository),
            )
            advanceUntilIdle()

            val state = assertIs<LearningUnitUiState.Content>(viewModel.uiState.value)
            assertEquals(3, state.lessons.size)
            assertEquals(StudyProgressUiState.Unavailable, state.studyProgress)
        }

    /**
     * The reason the projection is app-scoped rather than per-screen.
     *
     * Navigation 3 keeps this overview alive while the learner reads a Lesson below it, so a mark
     * made down there has to reach *this* instance. Asserted on the same ViewModel object across the
     * mutation, and on the content read count, because rebuilding the screen or refetching the
     * document would both hide the bug this exists to catch.
     */
    @Test
    fun aMarkMadeElsewhereUpdatesThisLiveUnitWithoutReloadingTheDocument() = runViewModelTest {
        val studyRepository = FakeLessonStudyRepository()
        val holder = studyProgressStateHolder(studyRepository)
        val content = threeLessonUnit()
        val viewModel = viewModel("unit_a", content, holder)
        advanceUntilIdle()

        assertEquals(
            StudyProgressSummary.Progress(studiedCount = 0, totalCount = 3),
            studyProgress(viewModel).summary,
        )
        val documentReads = content.unitReadIds.size

        // The Lesson reader's mutation, through the one holder both screens observe.
        holder.toggleStudied("lesson_b")
        advanceUntilIdle()

        val progress = studyProgress(viewModel)
        assertEquals(StudyProgressSummary.Progress(studiedCount = 1, totalCount = 3), progress.summary)
        assertEquals(listOf(false, true, false), progress.lessons.map { it.isStudied })
        // Publisher content and learner state have independent lifecycles: nothing was re-authored.
        assertEquals(documentReads, content.unitReadIds.size)
    }

    /** Opening the overview reads study state and writes none of it. */
    @Test
    fun openingTheUnitPersistsNoStudyState() = runViewModelTest {
        val studyRepository = FakeLessonStudyRepository()
        viewModel("unit_a", threeLessonUnit(), studyProgressStateHolder(studyRepository))
        advanceUntilIdle()

        assertEquals(emptyList(), studyRepository.markCalls)
        assertEquals(emptyList(), studyRepository.unmarkCalls)
    }

    private fun threeLessonUnit(): FakeLearningContentRepository =
        FakeLearningContentRepository(
            units = listOf(
                testLearningUnit(
                    "unit_a",
                    lessons = listOf(
                        testLearningLesson("lesson_a"),
                        testLearningLesson("lesson_b"),
                        testLearningLesson("lesson_retired", status = ContentStatus.DEPRECATED),
                        testLearningLesson("lesson_c"),
                    ),
                ),
            ),
        )

    private fun studyProgress(viewModel: LearningUnitViewModel): LearningUnitStudyProgress {
        val state = assertIs<LearningUnitUiState.Content>(viewModel.uiState.value)
        return assertIs<StudyProgressUiState.Available<LearningUnitStudyProgress>>(
            state.studyProgress,
        ).value
    }

    private fun TestScope.viewModel(
        unitId: String,
        repository: LearningContentRepository,
        studyProgressStateHolder: StudyProgressStateHolder = studyProgressStateHolder(),
    ): LearningUnitViewModel =
        LearningUnitViewModel(
            unitId = unitId,
            learningContentRepository = repository,
            studyProgressStateHolder = studyProgressStateHolder,
        )

    private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }
}
