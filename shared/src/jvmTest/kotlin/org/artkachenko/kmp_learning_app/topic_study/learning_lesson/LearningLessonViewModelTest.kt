package org.artkachenko.kmp_learning_app.topic_study.learning_lesson

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningBlock
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningDepth
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningSection
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

    /**
     * The authored body reaches the reader whole and in authored order. Sections are not regrouped
     * by depth and Sources are not sorted: both sequences are the document's, not the screen's.
     */
    @Test
    fun theAuthoredBodyAndSourcesReachContentInAuthoredOrder() = runViewModelTest {
        val sections = listOf(
            LearningSection(LearningDepth.SENIOR, listOf(LearningBlock.Paragraph("deeper"))),
            LearningSection(LearningDepth.CORE, listOf(LearningBlock.Paragraph("core")), "Basics"),
        )
        val sources = listOf(
            SourceReference("Second in the document", "https://example.com/b"),
            SourceReference("First in the document", "https://example.com/a"),
        )
        val viewModel = viewModel(
            unitId = "unit_a",
            lessonId = "lesson_a",
            repository = FakeLearningContentRepository(
                units = listOf(
                    testLearningUnit(
                        id = "unit_a",
                        lessons = listOf(
                            testLearningLesson("lesson_a", sections = sections, sources = sources),
                        ),
                    ),
                ),
            ),
        )

        advanceUntilIdle()

        val state = assertIs<LearningLessonUiState.Content>(viewModel.uiState.value)
        assertEquals(sections, state.sections)
        assertEquals(sources, state.sources)
    }

    @Test
    fun theFirstLessonHasNoPreviousAndPointsAtTheSecond() = runViewModelTest {
        val state = adjacentState(lessonId = "lesson_a")

        assertNull(state.previousLesson)
        assertEquals(AdjacentLessonUiModel("lesson_b", "Title of lesson_b"), state.nextLesson)
    }

    @Test
    fun aMiddleLessonHasBothNeighbours() = runViewModelTest {
        val state = adjacentState(lessonId = "lesson_b")

        assertEquals(AdjacentLessonUiModel("lesson_a", "Title of lesson_a"), state.previousLesson)
        assertEquals(AdjacentLessonUiModel("lesson_c", "Title of lesson_c"), state.nextLesson)
    }

    @Test
    fun theLastLessonHasNoNext() = runViewModelTest {
        val state = adjacentState(lessonId = "lesson_c")

        assertEquals(AdjacentLessonUiModel("lesson_b", "Title of lesson_b"), state.previousLesson)
        assertNull(state.nextLesson)
    }

    /**
     * A retired Lesson is not a stop on the way between two current ones. It cannot be opened, so
     * offering it as Next would hand the learner a control that leads to an unavailable page.
     */
    @Test
    fun aDeprecatedSiblingIsSkippedInBothDirections() = runViewModelTest {
        val repository = FakeLearningContentRepository(
            units = listOf(
                testLearningUnit(
                    id = "unit_a",
                    lessons = listOf(
                        testLearningLesson("lesson_a"),
                        testLearningLesson("lesson_retired", ContentStatus.DEPRECATED),
                        testLearningLesson("lesson_c"),
                    ),
                ),
            ),
        )

        val first = contentState("unit_a", "lesson_a", repository)
        assertEquals(AdjacentLessonUiModel("lesson_c", "Title of lesson_c"), first.nextLesson)

        val last = contentState("unit_a", "lesson_c", repository)
        assertEquals(AdjacentLessonUiModel("lesson_a", "Title of lesson_a"), last.previousLesson)
    }

    @Test
    fun theOnlyLessonInAUnitHasNeitherNeighbour() = runViewModelTest {
        val state = contentState(
            unitId = "unit_a",
            lessonId = "lesson_a",
            repository = FakeLearningContentRepository(
                units = listOf(
                    testLearningUnit("unit_a", lessons = listOf(testLearningLesson("lesson_a"))),
                ),
            ),
        )

        assertNull(state.previousLesson)
        assertNull(state.nextLesson)
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

    /** Three current Lessons in authored order, which is the sequence previous/next walks. */
    private suspend fun TestScope.adjacentState(lessonId: String): LearningLessonUiState.Content =
        contentState(
            unitId = "unit_a",
            lessonId = lessonId,
            repository = FakeLearningContentRepository(
                units = listOf(
                    testLearningUnit(
                        id = "unit_a",
                        lessons = listOf(
                            testLearningLesson("lesson_a"),
                            testLearningLesson("lesson_b"),
                            testLearningLesson("lesson_c"),
                        ),
                    ),
                ),
            ),
        )

    private suspend fun TestScope.contentState(
        unitId: String,
        lessonId: String,
        repository: LearningContentRepository,
    ): LearningLessonUiState.Content {
        val viewModel = viewModel(unitId, lessonId, repository)
        advanceUntilIdle()
        return assertIs(viewModel.uiState.value)
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
