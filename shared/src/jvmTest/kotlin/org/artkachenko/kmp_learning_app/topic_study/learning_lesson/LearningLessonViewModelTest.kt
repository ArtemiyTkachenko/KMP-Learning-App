package org.artkachenko.kmp_learning_app.topic_study.learning_lesson

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
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
import org.artkachenko.kmp_learning_app.lesson_study.FakeLessonStudyRepository
import org.artkachenko.kmp_learning_app.lesson_study.StudiedLesson
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressStateHolder
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressUiState
import org.artkachenko.kmp_learning_app.lesson_study.studyProgressStateHolder
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

    /**
     * The orientation line names the Unit and places the Lesson in it, from the same ACTIVE list
     * the neighbours come from — so "Lesson 2 of 3" and what Next actually opens can never disagree.
     */
    @Test
    fun placementNamesTheUnitAndCountsPositionAcrossActiveLessons() = runViewModelTest {
        assertEquals(
            LessonPlacementUiModel("Title of unit_a", position = 1, lessonCount = 3),
            adjacentState(lessonId = "lesson_a").placement,
        )
        assertEquals(
            LessonPlacementUiModel("Title of unit_a", position = 2, lessonCount = 3),
            adjacentState(lessonId = "lesson_b").placement,
        )
        assertEquals(
            LessonPlacementUiModel("Title of unit_a", position = 3, lessonCount = 3),
            adjacentState(lessonId = "lesson_c").placement,
        )
    }

    /**
     * A retired Lesson is neither a waypoint nor a denominator: counting it would tell the learner
     * they are 2 of 3 through a Unit that only has two readable Lessons left in it.
     */
    @Test
    fun placementCountsOnlyActiveLessons() = runViewModelTest {
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

        assertEquals(
            LessonPlacementUiModel("Title of unit_a", position = 2, lessonCount = 2),
            contentState("unit_a", "lesson_c", repository).placement,
        )
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
        val placement = assertNotNull(state.placement)
        assertEquals("Title of unit_a", placement.unitTitle)
        assertEquals(1, placement.position)
        assertEquals(1, placement.lessonCount)
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


    /**
     * The document is readable before the study record is, and stays readable if it never becomes
     * readable at all. Study state is enrichment layered over the Lesson, so it produces no page
     * state of its own.
     */
    @Test
    fun aLessonRendersItsContentWhileStudyStateIsStillLoading() = runViewModelTest {
        val repository = FakeLessonStudyRepository()
        val gate = CompletableDeferred<Unit>()
        repository.readGate = gate
        val viewModel = viewModel(
            unitId = "unit_a",
            lessonId = "lesson_a",
            repository = lessonRepository(),
            studyProgressStateHolder = studyProgressStateHolder(repository),
        )

        advanceUntilIdle()

        val state = assertIs<LearningLessonUiState.Content>(viewModel.uiState.value)
        assertEquals("Title of lesson_a", state.title)
        assertEquals(StudyProgressUiState.Loading, state.studyState)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(false, studyModel(viewModel).isStudied)
    }

    @Test
    fun anUnstudiedLessonReportsNotStudied() = runViewModelTest {
        val viewModel = studyViewModel(FakeLessonStudyRepository(StudiedLesson("lesson_b", 1_000)))

        assertEquals(LessonStudyUiModel(isStudied = false, isPending = false), studyModel(viewModel))
    }

    @Test
    fun aStudiedLessonReportsStudied() = runViewModelTest {
        val viewModel = studyViewModel(FakeLessonStudyRepository(StudiedLesson("lesson_a", 1_000)))

        assertEquals(LessonStudyUiModel(isStudied = true, isPending = false), studyModel(viewModel))
    }

    /**
     * An unreadable study record costs the indicator and nothing else: the Lesson, its neighbours,
     * and its Sources are all still there, and the page is not an Error.
     */
    @Test
    fun anUnreadableStudyRecordLeavesTheLessonReadableAndItsStudyStateUnavailable() =
        runViewModelTest {
            val repository = FakeLessonStudyRepository(StudiedLesson("lesson_a", 1_000))
            repository.failReads = true
            val viewModel = viewModel(
                unitId = "unit_a",
                lessonId = "lesson_a",
                repository = lessonRepository(),
                studyProgressStateHolder = studyProgressStateHolder(repository),
            )

            advanceUntilIdle()

            val state = assertIs<LearningLessonUiState.Content>(viewModel.uiState.value)
            assertEquals("Title of lesson_a", state.title)
            assertEquals(2, state.sources.size)
            assertEquals(StudyProgressUiState.Unavailable, state.studyState)
        }

    /**
     * The boundary the whole epic rests on: arriving at a Lesson is not studying it. Asserted on the
     * repository rather than on a screen callback, because a ViewModel that marked on load would
     * never reach the callback layer at all.
     */
    @Test
    fun openingALessonPersistsNothing() = runViewModelTest {
        val repository = FakeLessonStudyRepository()
        studyViewModel(repository)

        assertEquals(emptyList(), repository.markCalls)
        assertEquals(emptyList(), repository.unmarkCalls)
        assertEquals(0, repository.isStudiedCalls)
    }

    /** Nor is moving to a sibling: a second reader over the same holder writes nothing either. */
    @Test
    fun openingASiblingLessonPersistsNothing() = runViewModelTest {
        val repository = FakeLessonStudyRepository()
        val holder = studyProgressStateHolder(repository)
        viewModel("unit_a", "lesson_a", lessonRepository(), holder)
        advanceUntilIdle()
        viewModel("unit_a", "lesson_b", lessonRepository(), holder)
        advanceUntilIdle()

        assertEquals(emptyList(), repository.markCalls)
        assertEquals(emptyList(), repository.unmarkCalls)
    }

    @Test
    fun markingStudiedPersistsAndThenReportsWhatWasReadBack() = runViewModelTest {
        val repository = FakeLessonStudyRepository()
        val viewModel = studyViewModel(repository)

        viewModel.toggleStudied()
        advanceUntilIdle()

        assertEquals(listOf("lesson_a"), repository.markCalls)
        assertEquals(LessonStudyUiModel(isStudied = true, isPending = false), studyModel(viewModel))
    }

    @Test
    fun unmarkingStudiedReversesIt() = runViewModelTest {
        val repository = FakeLessonStudyRepository(StudiedLesson("lesson_a", 1_000))
        val viewModel = studyViewModel(repository)

        viewModel.toggleStudied()
        advanceUntilIdle()

        assertEquals(listOf("lesson_a"), repository.unmarkCalls)
        assertEquals(false, studyModel(viewModel).isStudied)
    }

    /** A write in flight is reported as pending, and the persisted value is what stays visible. */
    @Test
    fun aPendingMarkKeepsThePersistedValueVisible() = runViewModelTest {
        val repository = FakeLessonStudyRepository()
        val viewModel = studyViewModel(repository)
        repository.writeGate = CompletableDeferred()

        viewModel.toggleStudied()
        advanceUntilIdle()

        assertEquals(LessonStudyUiModel(isStudied = false, isPending = true), studyModel(viewModel))
    }

    @Test
    fun aFailedMarkNeverShowsTheLessonAsStudied() = runViewModelTest {
        val repository = FakeLessonStudyRepository()
        val viewModel = studyViewModel(repository)
        repository.failMutations = true

        viewModel.toggleStudied()
        advanceUntilIdle()

        assertEquals(LessonStudyUiModel(isStudied = false, isPending = false), studyModel(viewModel))
    }

    @Test
    fun aFailedUnmarkLeavesTheLessonStudied() = runViewModelTest {
        val repository = FakeLessonStudyRepository(StudiedLesson("lesson_a", 1_000))
        val viewModel = studyViewModel(repository)
        repository.failMutations = true

        viewModel.toggleStudied()
        advanceUntilIdle()

        assertEquals(LessonStudyUiModel(isStudied = true, isPending = false), studyModel(viewModel))
    }

    private fun TestScope.studyViewModel(
        studyRepository: FakeLessonStudyRepository,
    ): LearningLessonViewModel =
        viewModel(
            unitId = "unit_a",
            lessonId = "lesson_a",
            repository = lessonRepository(),
            studyProgressStateHolder = studyProgressStateHolder(studyRepository),
        ).also { advanceUntilIdle() }

    private fun studyModel(viewModel: LearningLessonViewModel): LessonStudyUiModel {
        val state = assertIs<LearningLessonUiState.Content>(viewModel.uiState.value)
        return assertIs<StudyProgressUiState.Available<LessonStudyUiModel>>(state.studyState).value
    }

    private fun lessonRepository(): FakeLearningContentRepository =
        FakeLearningContentRepository(
            units = listOf(
                testLearningUnit(
                    "unit_a",
                    lessons = listOf(
                        testLearningLesson(
                            "lesson_a",
                            sources = listOf(
                                SourceReference("First source", "https://example.test/a"),
                                SourceReference("Second source", "https://example.test/b"),
                            ),
                        ),
                        testLearningLesson("lesson_b"),
                    ),
                ),
            ),
        )

    private fun TestScope.viewModel(
        unitId: String,
        lessonId: String,
        repository: LearningContentRepository,
        studyProgressStateHolder: StudyProgressStateHolder = studyProgressStateHolder(),
    ): LearningLessonViewModel =
        LearningLessonViewModel(
            unitId = unitId,
            lessonId = lessonId,
            learningContentRepository = repository,
            studyProgressStateHolder = studyProgressStateHolder,
        )

    private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }
}
