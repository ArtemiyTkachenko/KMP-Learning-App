package org.artkachenko.kmp_learning_app.topic_study.learning_lesson

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class LessonScrollStateReducerTest {
    @Test
    fun downwardDistanceMustCrossTheThresholdBeforeChromeCollapses() {
        val reducer = LessonScrollStateReducer(DirectionThreshold)
        reducer.update(position = 0, isScrollInProgress = false, isAtTop = true)

        val belowThreshold = reducer.update(
            position = DirectionThreshold - 1,
            isScrollInProgress = true,
        )
        assertTrue(belowThreshold.showsToolbarSubtitle)
        assertTrue(belowThreshold.showsBottomNavigation)

        val atThreshold = reducer.update(
            position = DirectionThreshold,
            isScrollInProgress = true,
        )
        assertFalse(atThreshold.showsToolbarSubtitle)
        assertFalse(atThreshold.showsBottomNavigation)
    }

    @Test
    fun upwardDistanceRestoresChromeOnlyAfterItsOwnThreshold() {
        val reducer = LessonScrollStateReducer(DirectionThreshold)
        reducer.update(position = 50, isScrollInProgress = false)
        reducer.update(position = 74, isScrollInProgress = true)

        val belowThreshold = reducer.update(position = 51, isScrollInProgress = true)
        assertFalse(belowThreshold.showsToolbarSubtitle)
        assertFalse(belowThreshold.showsBottomNavigation)

        val atThreshold = reducer.update(position = 50, isScrollInProgress = true)
        assertTrue(atThreshold.showsToolbarSubtitle)
        assertTrue(atThreshold.showsBottomNavigation)
    }

    @Test
    fun aDirectionReversalStartsANewAccumulationWindow() {
        val reducer = LessonScrollStateReducer(DirectionThreshold)
        reducer.update(position = 50, isScrollInProgress = false)
        reducer.update(position = 60, isScrollInProgress = true)
        reducer.update(position = 55, isScrollInProgress = true)

        val stillVisible = reducer.update(position = 65, isScrollInProgress = true)
        assertTrue(stillVisible.showsToolbarSubtitle)
        assertTrue(stillVisible.showsBottomNavigation)

        val collapsed = reducer.update(position = 79, isScrollInProgress = true)
        assertFalse(collapsed.showsToolbarSubtitle)
        assertFalse(collapsed.showsBottomNavigation)
    }

    @Test
    fun topAndBottomOverrideDirectionalNavigationState() {
        val reducer = LessonScrollStateReducer(DirectionThreshold)
        reducer.update(position = 0, isScrollInProgress = false, isAtTop = true)
        reducer.update(position = DirectionThreshold, isScrollInProgress = true)

        val bottom = reducer.update(
            position = 100,
            isScrollInProgress = true,
            isAtBottom = true,
        )
        assertTrue(bottom.showsBottomNavigation)
        assertFalse(bottom.showsToolbarSubtitle)

        val top = reducer.update(
            position = 0,
            isScrollInProgress = true,
            isAtTop = true,
        )
        assertTrue(top.showsToolbarSubtitle)
        assertTrue(top.showsBottomNavigation)
    }

    @Test
    fun passiveLayoutChangesDoNotAccumulateAsReadingDirection() {
        val reducer = LessonScrollStateReducer(DirectionThreshold)
        reducer.update(position = 0, isScrollInProgress = false, isAtTop = true)

        val afterLargeRemeasurement = reducer.update(
            position = DirectionThreshold * 2,
            isScrollInProgress = false,
        )
        assertTrue(afterLargeRemeasurement.showsToolbarSubtitle)
        assertTrue(afterLargeRemeasurement.showsBottomNavigation)

        val afterSmallIntentionalScroll = reducer.update(
            position = DirectionThreshold * 3 - 1,
            isScrollInProgress = true,
        )
        assertTrue(afterSmallIntentionalScroll.showsToolbarSubtitle)
        assertTrue(afterSmallIntentionalScroll.showsBottomNavigation)
    }

    private fun LessonScrollStateReducer.update(
        position: Int,
        isScrollInProgress: Boolean,
        isAtTop: Boolean = false,
        isAtBottom: Boolean = false,
    ): LessonScrollUiState = update(
        position = position,
        isScrollInProgress = isScrollInProgress,
        isAtTop = isAtTop,
        isAtBottom = isAtBottom,
    )
}

private const val DirectionThreshold = 24
