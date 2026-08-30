package org.artkachenko.kmp_learning_app

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The motion depends on whether both sides of a transition are navigation-bar areas: siblings
 * cross-fade, while moving in or out of a detail slides. This pins that decision, which the
 * transition specs read.
 */
internal class AppNavigationTransitionsTest {
    @Test
    fun switchingBetweenAreasIsTreatedAsASiblingMove() {
        AppTopLevelDestination.entries.forEach { from ->
            AppTopLevelDestination.entries.forEach { to ->
                assertTrue(
                    isTopLevelSwitch(from.route, to.route),
                    "$from -> $to should cross-fade",
                )
            }
        }
    }

    @Test
    fun movingBetweenAnAreaAndADetailIsNotASiblingMove() {
        val detail = AppRoute.ProgressTopic("topic")

        assertFalse(isTopLevelSwitch(AppRoute.Progress, detail))
        assertFalse(isTopLevelSwitch(detail, AppRoute.Progress))
        assertFalse(isTopLevelSwitch(detail, AppRoute.MixedInterviewResult("attempt")))
    }
}
