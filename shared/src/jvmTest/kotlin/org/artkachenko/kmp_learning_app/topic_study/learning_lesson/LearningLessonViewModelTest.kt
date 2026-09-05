package org.artkachenko.kmp_learning_app.topic_study.learning_lesson

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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
import org.artkachenko.kmp_learning_app.topic_study.FakeLearningContentRepository
import org.artkachenko.kmp_learning_app.topic_study.testLearningLesson
import org.artkachenko.kmp_learning_app.topic_study.testLearningUnit

@OptIn(ExperimentalCoroutinesApi::class)
internal class LearningLessonViewModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun anActiveLessonInItsActiveUnitResolvesToContent() = runViewModelTest {
        val viewModel = viewModel(
            unitId = "unit_a",
            lessonId = "lesson_a",
            repository = FakeLearningContentRepository(
                units = listOf(
                    testLearningUnit("unit_a", lessons = listOf(testLearningLesson("lesson_a"))),
                ),
            ),
        )

        advanceUntilIdle()

        val state = assertIs<LearningLessonUiState.Content>(viewModel.uiState.value)
        assertEquals("unit_a", state.unitId)
        assertEquals("lesson_a", state.lessonId)
        assertEquals("Title of lesson_a", state.title)
        assertEquals("Summary of lesson_a", state.summary)
    }

    @Test
    fun anUnknownUnitIsNotFound() = runViewModelTest {
        val viewModel = viewModel(
            unitId = "unit_missing",
            lessonId = "lesson_a",
            repository = FakeLearningContentRepository(
                units = listOf(
                    testLearningUnit("unit_a", lessons = listOf(testLearningLesson("lesson_a"))),
                ),
            ),
        )

        advanceUntilIdle()

        assertEquals(LearningLessonUiState.NotFound, viewModel.uiState.value)
    }

    /** A Lesson is only current if the Unit it is read in is current too. */
    @Test
    fun anActiveLessonInsideADeprecatedUnitIsNotFound() = runViewModelTest {
        val viewModel = viewModel(
            unitId = "unit_retired",
            lessonId = "lesson_a",
            repository = FakeLearningContentRepository(
                units = listOf(
                    testLearningUnit(
                        id = "unit_retired",
                        lessons = listOf(testLearningLesson("lesson_a")),
                        status = ContentStatus.DEPRECATED,
                    ),
                ),
            ),
        )

        advanceUntilIdle()

        assertEquals(LearningLessonUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun anUnknownLessonIsNotFound() = runViewModelTest {
        val viewModel = viewModel(
            unitId = "unit_a",
            lessonId = "lesson_missing",
            repository = FakeLearningContentRepository(
                units = listOf(
                    testLearningUnit("unit_a", lessons = listOf(testLearningLesson("lesson_a"))),
                ),
            ),
        )

        advanceUntilIdle()

        assertEquals(LearningLessonUiState.NotFound, viewModel.uiState.value)
    }

    /**
     * Retired material still resolves by stable ID so old references stay readable, but it is not
     * something normal browsing may open.
     */
    @Test
    fun aDeprecatedLessonInsideAnActiveUnitIsNotFound() = runViewModelTest {
        val viewModel = viewModel(
            unitId = "unit_a",
            lessonId = "lesson_retired",
            repository = FakeLearningContentRepository(
                units = listOf(
                    testLearningUnit(
                        id = "unit_a",
                        lessons = listOf(
                            testLearningLesson("lesson_a"),
                            testLearningLesson("lesson_retired", ContentStatus.DEPRECATED),
                        ),
                    ),
                ),
            ),
        )

        advanceUntilIdle()

        assertEquals(LearningLessonUiState.NotFound, viewModel.uiState.value)
    }

    /**
     * The route-integrity case the Unit ID exists for. Lesson `lesson_b` is perfectly resolvable by
     * its globally unique ID, so a resolver that ignored the parent would open it here — under the
     * wrong Unit, reached by a back stack the learner never travelled.
     */
    @Test
    fun aLessonBelongingToAnotherUnitIsNotFoundEvenThoughItResolvesGlobally() = runViewModelTest {
        val repository = FakeLearningContentRepository(
            units = listOf(
                testLearningUnit("unit_a", lessons = listOf(testLearningLesson("lesson_a"))),
                testLearningUnit("unit_b", lessons = listOf(testLearningLesson("lesson_b"))),
            ),
        )
        assertEquals("lesson_b", repository.getLessonById("lesson_b")?.id)

        val viewModel = viewModel(
            unitId = "unit_a",
            lessonId = "lesson_b",
            repository = repository,
        )

        advanceUntilIdle()

        assertEquals(LearningLessonUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun aRepositoryFailureIsAnErrorAndRetryRecovers() = runViewModelTest {
        val viewModel = viewModel(
            unitId = "unit_a",
            lessonId = "lesson_a",
            repository = FakeLearningContentRepository(
                units = listOf(
                    testLearningUnit("unit_a", lessons = listOf(testLearningLesson("lesson_a"))),
                ),
                failuresRemaining = 1,
            ),
        )
        advanceUntilIdle()

        assertEquals(LearningLessonUiState.Error, viewModel.uiState.value)

        viewModel.retry()
        advanceUntilIdle()

        val state = assertIs<LearningLessonUiState.Content>(viewModel.uiState.value)
        assertEquals("lesson_a", state.lessonId)
    }

    private fun viewModel(
        unitId: String,
        lessonId: String,
        repository: LearningContentRepository,
    ): LearningLessonViewModel =
        LearningLessonViewModel(
            unitId = unitId,
            lessonId = lessonId,
            learningContentRepository = repository,
        )

    private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }
}
