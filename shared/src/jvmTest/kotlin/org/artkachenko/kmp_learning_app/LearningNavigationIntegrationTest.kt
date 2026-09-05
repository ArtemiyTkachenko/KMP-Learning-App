package org.artkachenko.kmp_learning_app

import androidx.navigation3.runtime.NavKey
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.curriculum.learning.content.BundledLearningContentRepository
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.AdjacentLessonUiModel
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonUiState
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonViewModel
import org.artkachenko.kmp_learning_app.topic_study.learning_unit.LearningUnitUiState
import org.artkachenko.kmp_learning_app.topic_study.learning_unit.LearningUnitViewModel

/**
 * Learn -> Topic -> Unit -> Lesson on the content that actually ships.
 *
 * Nothing is faked and no title is hardcoded: the Unit and its Lessons are read through the same
 * repository the destinations use, so retiring a Lesson or authoring another one changes what this
 * asserts, which is the point. Structure is asserted rather than prose — that a Lesson has
 * Sections, Sources, and the right neighbours — because pinning authored sentences would make an
 * editorial improvement look like a regression.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class LearningNavigationIntegrationTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun theProductionComposeUnitIsReachedFromItsTopicAndListsItsThreeLessonsInOrder() =
        runIntegrationTest {
            val repository: LearningContentRepository = BundledLearningContentRepository()
            val navigator = navigator()

            // The Topic the catalogue opens, then the Unit its study section offers.
            navigator.push(AppRoute.Topic(AndroidUiTopicId))
            val unitId = repository.getActiveUnitsByTopic(AndroidUiTopicId).single().id
            assertEquals(ComposeUnitId, unitId)
            navigator.push(AppRoute.LearningUnit(unitId))

            val unitState = assertIs<LearningUnitUiState.Content>(
                unitState(unitId, repository),
            )
            assertEquals(
                listOf(
                    "lesson_declarative_ui",
                    "lesson_composable_execution",
                    "lesson_state_down_events_up",
                ),
                unitState.lessons.map { it.lessonId },
            )
            // Authored prose reaches the overview rather than a bare list of identities.
            assertTrue(unitState.title.isNotBlank())
            assertTrue(unitState.summary.isNotBlank())
            assertTrue(unitState.lessons.all { it.title.isNotBlank() && it.summary.isNotBlank() })

            // Every Lesson the overview offers has to open under the Unit it was offered from.
            unitState.lessons.forEach { lesson ->
                val lessonState = assertIs<LearningLessonUiState.Content>(
                    lessonState(unitId, lesson.lessonId, repository),
                )
                assertEquals(unitId, lessonState.unitId)
                assertEquals(lesson.lessonId, lessonState.lessonId)
                assertEquals(lesson.title, lessonState.title)
            }

            navigator.push(AppRoute.LearningLesson(unitId, unitState.lessons.first().lessonId))
            assertEquals(AppTopLevelDestination.TOPICS, navigator.area)

            // Back returns through the Unit to the Topic the learner came from.
            navigator.popBack()
            assertEquals(AppRoute.LearningUnit(unitId), navigator.currentRoute)
            navigator.popBack()
            assertEquals(AppRoute.Topic(AndroidUiTopicId), navigator.currentRoute)
        }

    /**
     * The shipped structured body reaches the reader's state, not just the Lesson's identity.
     *
     * Structural rather than textual: every current Lesson must arrive with Sections and with the
     * authoritative Sources the authoring contract requires, and its neighbours must be its Unit's
     * other ACTIVE Lessons in authored order — first with no Previous, last with no Next.
     */
    @Test
    fun everyProductionComposeLessonReachesTheReaderWithItsAuthoredBodyAndNeighbours() =
        runIntegrationTest {
            val repository: LearningContentRepository = BundledLearningContentRepository()
            val unit = repository.getUnitById(ComposeUnitId)!!
            val lessonIds = unit.lessons.map { it.id }

            val states = lessonIds.map { lessonId ->
                assertIs<LearningLessonUiState.Content>(
                    lessonState(ComposeUnitId, lessonId, repository),
                )
            }

            states.forEachIndexed { index, state ->
                assertTrue(
                    state.sections.isNotEmpty(),
                    "${state.lessonId} reached the reader with no authored sections.",
                )
                assertTrue(
                    state.sections.all { it.blocks.isNotEmpty() },
                    "${state.lessonId} has an authored section with no blocks.",
                )
                assertTrue(
                    state.sources.isNotEmpty(),
                    "${state.lessonId} reached the reader with no authoritative sources.",
                )
                assertTrue(state.sources.all { it.title.isNotBlank() })

                val previous = lessonIds.getOrNull(index - 1)
                val next = lessonIds.getOrNull(index + 1)
                assertEquals(previous, state.previousLesson?.lessonId)
                assertEquals(next, state.nextLesson?.lessonId)
            }
            // The ends of the sequence, stated explicitly: nothing precedes the first Lesson and
            // nothing follows the last, so neither control is offered where it cannot lead.
            assertNull(states.first().previousLesson)
            assertNull(states.last().nextLesson)
            assertEquals(
                AdjacentLessonUiModel(lessonIds[1], unit.lessons[1].title),
                states.first().nextLesson,
            )
        }

    /**
     * Reading on replaces the Lesson entry rather than stacking another one, so a learner who has
     * read a whole Unit is still one Back press from the Unit overview rather than N.
     */
    @Test
    fun readingOnReplacesTheLessonEntrySoBackStillLeavesTheReader() = runIntegrationTest {
        val repository: LearningContentRepository = BundledLearningContentRepository()
        val lessonIds = repository.getUnitById(ComposeUnitId)!!.lessons.map { it.id }
        val navigator = navigator()
        navigator.push(AppRoute.Topic(AndroidUiTopicId))
        navigator.push(AppRoute.LearningUnit(ComposeUnitId))
        navigator.push(AppRoute.LearningLesson(ComposeUnitId, lessonIds.first()))
        val depthWhileReading = navigator.backStack.size

        // Next, twice: the shell replaces the top entry with the sibling the reader emitted.
        navigator.replaceTop(AppRoute.LearningLesson(ComposeUnitId, lessonIds[1]))
        navigator.replaceTop(AppRoute.LearningLesson(ComposeUnitId, lessonIds[2]))

        assertEquals(AppRoute.LearningLesson(ComposeUnitId, lessonIds[2]), navigator.currentRoute)
        assertEquals(depthWhileReading, navigator.backStack.size)

        // Previous is a sibling move too, not a pop: it shows the earlier Lesson without leaving.
        navigator.replaceTop(AppRoute.LearningLesson(ComposeUnitId, lessonIds[1]))
        assertEquals(AppRoute.LearningLesson(ComposeUnitId, lessonIds[1]), navigator.currentRoute)
        assertEquals(depthWhileReading, navigator.backStack.size)

        // Back after three sibling moves still leaves the reader for the Unit it was opened from.
        navigator.popBack()
        assertEquals(AppRoute.LearningUnit(ComposeUnitId), navigator.currentRoute)
    }

    /**
     * The route-integrity rule against real content: a shipped Lesson paired with a Unit it does
     * not belong to must not open, even though its ID resolves globally.
     */
    @Test
    fun aProductionLessonDoesNotOpenUnderAnotherUnit() = runIntegrationTest {
        val repository: LearningContentRepository = BundledLearningContentRepository()
        assertEquals(
            "lesson_declarative_ui",
            repository.getLessonById("lesson_declarative_ui")?.id,
        )

        assertEquals(
            LearningLessonUiState.NotFound,
            lessonState("unit_not_authored", "lesson_declarative_ui", repository),
        )
    }

    private fun TestScope.unitState(
        unitId: String,
        repository: LearningContentRepository,
    ): LearningUnitUiState {
        val viewModel = LearningUnitViewModel(unitId, repository)
        advanceUntilIdle()
        return viewModel.uiState.value
    }

    private fun TestScope.lessonState(
        unitId: String,
        lessonId: String,
        repository: LearningContentRepository,
    ): LearningLessonUiState {
        val viewModel = LearningLessonViewModel(unitId, lessonId, repository)
        advanceUntilIdle()
        return viewModel.uiState.value
    }

    private fun navigator(): AppNavigator =
        AppNavigator(
            AppTopLevelDestination.entries.associateWith { mutableListOf<NavKey>(it.route) },
        )

    private fun runIntegrationTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }

    private companion object {
        const val AndroidUiTopicId = "android_ui"
        const val ComposeUnitId = "unit_thinking_in_compose"
    }
}
