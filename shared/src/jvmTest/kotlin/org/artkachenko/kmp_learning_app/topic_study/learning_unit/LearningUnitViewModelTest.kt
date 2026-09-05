package org.artkachenko.kmp_learning_app.topic_study.learning_unit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.assertIs
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository
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

    private fun viewModel(
        unitId: String,
        repository: LearningContentRepository,
    ): LearningUnitViewModel =
        LearningUnitViewModel(
            unitId = unitId,
            learningContentRepository = repository,
        )

    private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }
}
