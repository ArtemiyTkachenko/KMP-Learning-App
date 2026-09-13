package org.artkachenko.kmp_learning_app.topic_study.learning_lesson

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class LessonScrollStateReducerTest {
    @Test
    fun unmeasuredAndShortLessonsKeepReadingChromeVisibleWithoutAFab() {
        val reducer = LessonScrollStateReducer(DirectionThreshold)

        val unmeasured = reducer.update(position = 0, maxPosition = Int.MAX_VALUE)
        assertTrue(unmeasured.isAtTop)
        assertFalse(unmeasured.hasScrollableContent)
        assertFalse(unmeasured.hasContentBelow)
        assertTrue(unmeasured.showsToolbarSubtitle)
        assertTrue(unmeasured.showsBottomNavigation)

        val short = reducer.update(position = 0, maxPosition = 0)
        assertTrue(short.isAtTop)
        assertTrue(short.isAtBottom)
        assertFalse(short.hasScrollableContent)
        assertFalse(short.hasContentBelow)
        assertTrue(short.showsBottomNavigation)
    }

    @Test
    fun downwardDistanceMustCrossTheThresholdBeforeChromeCollapses() {
        val reducer = LessonScrollStateReducer(DirectionThreshold)
        reducer.update(position = 0, maxPosition = 200)

        val belowThreshold = reducer.update(position = DirectionThreshold - 1, maxPosition = 200)
        assertTrue(belowThreshold.showsToolbarSubtitle)
        assertTrue(belowThreshold.showsBottomNavigation)

        val atThreshold = reducer.update(position = DirectionThreshold, maxPosition = 200)
        assertFalse(atThreshold.showsToolbarSubtitle)
        assertFalse(atThreshold.showsBottomNavigation)
    }

    @Test
    fun upwardDistanceRestoresChromeOnlyAfterItsOwnThreshold() {
        val reducer = LessonScrollStateReducer(DirectionThreshold)
        reducer.update(position = 50, maxPosition = 200)
        reducer.update(position = 74, maxPosition = 200)

        val belowThreshold = reducer.update(position = 51, maxPosition = 200)
        assertFalse(belowThreshold.showsToolbarSubtitle)
        assertFalse(belowThreshold.showsBottomNavigation)

        val atThreshold = reducer.update(position = 50, maxPosition = 200)
        assertTrue(atThreshold.showsToolbarSubtitle)
        assertTrue(atThreshold.showsBottomNavigation)
    }

    @Test
    fun aDirectionReversalStartsANewAccumulationWindow() {
        val reducer = LessonScrollStateReducer(DirectionThreshold)
        reducer.update(position = 50, maxPosition = 200)
        reducer.update(position = 60, maxPosition = 200)
        reducer.update(position = 55, maxPosition = 200)

        val stillVisible = reducer.update(position = 65, maxPosition = 200)
        assertTrue(stillVisible.showsToolbarSubtitle)
        assertTrue(stillVisible.showsBottomNavigation)

        val collapsed = reducer.update(position = 79, maxPosition = 200)
        assertFalse(collapsed.showsToolbarSubtitle)
        assertFalse(collapsed.showsBottomNavigation)
    }

    @Test
    fun topAndBottomOverrideDirectionalNavigationState() {
        val reducer = LessonScrollStateReducer(DirectionThreshold)
        reducer.update(position = 0, maxPosition = 100)
        reducer.update(position = DirectionThreshold, maxPosition = 100)

        val bottom = reducer.update(position = 100, maxPosition = 100)
        assertTrue(bottom.isAtBottom)
        assertTrue(bottom.showsBottomNavigation)
        assertFalse(bottom.showsToolbarSubtitle)
        assertFalse(bottom.hasContentBelow)

        val top = reducer.update(position = 0, maxPosition = 100)
        assertTrue(top.isAtTop)
        assertTrue(top.showsToolbarSubtitle)
        assertTrue(top.showsBottomNavigation)
    }

    @Test
    fun endAnchorSurvivesTheViewportShrinkingForTheRestoredBottomBar() {
        val reducer = LessonScrollStateReducer(DirectionThreshold)
        reducer.update(position = 0, maxPosition = 100)
        reducer.update(position = 100, maxPosition = 100)

        val afterRemeasurement = reducer.update(position = 100, maxPosition = 140)
        assertTrue(afterRemeasurement.isAtBottom)
        assertTrue(afterRemeasurement.showsBottomNavigation)
        assertFalse(afterRemeasurement.hasContentBelow)

        val afterMovingUp = reducer.update(position = 99, maxPosition = 140)
        assertFalse(afterMovingUp.isAtBottom)
        assertTrue(afterMovingUp.hasContentBelow)
    }

    @Test
    fun fabVisibilityTracksWhetherMeasuredContentRemainsBelow() {
        val reducer = LessonScrollStateReducer(DirectionThreshold)

        assertTrue(reducer.update(position = 0, maxPosition = 100).hasContentBelow)
        assertTrue(reducer.update(position = 99, maxPosition = 100).hasContentBelow)
        assertFalse(reducer.update(position = 100, maxPosition = 100).hasContentBelow)
    }
}

private const val DirectionThreshold = 24
