package org.artkachenko.kmp_learning_app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey

/**
 * Navigation state for the whole shell.
 *
 * Each area keeps its own back stack. A single shared stack meant switching away from a detail
 * threw it away, so returning to an area dropped the learner back at its root; with one stack per
 * area, leaving Topics mid-way through a topic and coming back returns to that topic.
 *
 * Back leaves the current area's detail first, then returns to the start area, and only then
 * reports that it did not consume the event so the host can close the app.
 */
@Stable
internal class AppNavigator(
    private val stacks: Map<AppTopLevelDestination, MutableList<NavKey>>,
    initialArea: AppTopLevelDestination = AppTopLevelDestination.Start,
) {
    var area: AppTopLevelDestination by mutableStateOf(initialArea)
        private set

    val backStack: MutableList<NavKey> get() = stacks.getValue(area)

    val currentRoute: AppRoute? get() = backStack.lastOrNull() as? AppRoute

    /**
     * Selecting the area already shown returns it to its root, which is what re-tapping a
     * navigation item conventionally does. Selecting another area switches to it, leaving that
     * area exactly where it was left.
     */
    fun select(destination: AppTopLevelDestination) {
        if (destination == area) {
            popToRoot()
            return
        }
        area = destination
    }

    fun push(route: AppRoute) {
        backStack.add(route)
    }

    /** Replaces the current entry, used when a configuration route becomes a persisted attempt. */
    fun replaceTop(route: AppRoute) {
        backStack.replaceTopWith(route)
    }

    fun popBack(): Boolean {
        val stack = backStack
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
            return true
        }
        if (area != AppTopLevelDestination.Start) {
            area = AppTopLevelDestination.Start
            return true
        }
        return false
    }

    private fun popToRoot() {
        val stack = backStack
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
}

@Composable
internal fun rememberAppNavigator(
    topics: MutableList<NavKey>,
    interview: MutableList<NavKey>,
    progress: MutableList<NavKey>,
    mistakes: MutableList<NavKey>,
): AppNavigator =
    remember(topics, interview, progress, mistakes) {
        AppNavigator(
            mapOf(
                AppTopLevelDestination.TOPICS to topics,
                AppTopLevelDestination.INTERVIEW to interview,
                AppTopLevelDestination.PROGRESS to progress,
                AppTopLevelDestination.MISTAKES to mistakes,
            ),
        )
    }
