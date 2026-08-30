package org.artkachenko.kmp_learning_app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

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
    // Hoisted rather than owned so the caller can make it survive state restoration. Held inside
    // the class it was rebuilt as Topics on every configuration change, which hid the restored
    // stack of whichever area was actually on screen.
    private val areaState: MutableState<AppTopLevelDestination> =
        mutableStateOf(AppTopLevelDestination.Start),
) {
    var area: AppTopLevelDestination by areaState
        private set

    val backStack: MutableList<NavKey> get() = stacks.getValue(area)

    val currentRoute: AppRoute? get() = backStack.lastOrNull() as? AppRoute

    /**
     * Whether back should leave the current area for the start area.
     *
     * True exactly when `NavDisplay` declines the event: it enables its own back handler only while
     * the stack it was given has a previous entry, so at an area's root nothing inside it consumes
     * back. The shell reads this to enable the outer handler that covers that case.
     */
    val canLeaveArea: Boolean
        get() = backStack.size == 1 && area != AppTopLevelDestination.Start

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

/**
 * Stores the area by enum name.
 *
 * Restoring through [AppTopLevelDestination.entries] rather than `valueOf` means a name that no
 * longer exists — an area renamed or removed in a later version — restores as null, which makes
 * `rememberSaveable` fall back to its initial value instead of throwing on startup.
 */
internal val AppTopLevelDestinationSaver: Saver<AppTopLevelDestination, String> =
    Saver(
        save = { it.name },
        restore = { name -> AppTopLevelDestination.entries.firstOrNull { it.name == name } },
    )

/**
 * Builds the shell's navigator with one saveable back stack per area.
 *
 * Each stack has an explicit call site because `rememberNavBackStack` derives its saved-state key
 * from the composition location. Constructing them in a loop can restore an area's root while
 * dropping its detail route, even if each iteration is wrapped in `key`.
 */
@Composable
internal fun rememberAppNavigator(): AppNavigator {
    val topics = rememberNavBackStack(appNavigationSavedStateConfiguration, AppRoute.Topics)
    val interview = rememberNavBackStack(appNavigationSavedStateConfiguration, AppRoute.Interview)
    val progress = rememberNavBackStack(appNavigationSavedStateConfiguration, AppRoute.Progress)
    val mistakes = rememberNavBackStack(
        appNavigationSavedStateConfiguration,
        AppRoute.MistakeReview,
    )
    // Saveable so the selected area survives a configuration change alongside the stacks
    // themselves, which rememberNavBackStack already restores.
    val areaState = rememberSaveable(stateSaver = AppTopLevelDestinationSaver) {
        mutableStateOf(AppTopLevelDestination.Start)
    }
    return remember(topics, interview, progress, mistakes, areaState) {
        AppNavigator(
            stacks = mapOf(
                AppTopLevelDestination.TOPICS to topics,
                AppTopLevelDestination.INTERVIEW to interview,
                AppTopLevelDestination.PROGRESS to progress,
                AppTopLevelDestination.MISTAKES to mistakes,
            ),
            areaState = areaState,
        )
    }
}
