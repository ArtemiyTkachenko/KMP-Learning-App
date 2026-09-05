package org.artkachenko.kmp_learning_app

import androidx.navigation3.runtime.NavKey
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
import org.artkachenko.kmp_learning_app.curriculum.learning.content.BundledLearningContentRepository
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonUiState
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonViewModel
import org.artkachenko.kmp_learning_app.topic_study.learning_unit.LearningUnitUiState
import org.artkachenko.kmp_learning_app.topic_study.learning_unit.LearningUnitViewModel

/**
 * Learn -> Topic -> Unit -> Lesson on the content that actually ships.
 *
 * Nothing is faked and no title is hardcoded: the Unit and its Lessons are read through the same
 * repository the destinations use, so retiring a Lesson or authoring another one changes what this
 * asserts, which is the point. The Lesson prose itself is not asserted — E21-03 is about identity,
 * order, and navigation, and E21-04 owns what a Lesson renders.
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
