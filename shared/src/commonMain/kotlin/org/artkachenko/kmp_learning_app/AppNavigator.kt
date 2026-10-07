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
 *
 * Results are the exception to "exactly where it was left" (see [AppRoute.isAssessmentResult]): a
 * finished result never resurfaces once the learner has moved past it, either by completing another
 * attempt on top of it or by leaving its area.
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
     *
     * Except when a result is on top of the area being left: that area is reset to its root, so
     * returning to it opens its home screen, which already shows the latest record, rather than an
     * old score the learner has moved past. Anything else on top — an unfinished attempt, a lesson,
     * a builder — is kept, because it is still work in progress.
     */
    fun select(destination: AppTopLevelDestination) {
        if (destination == area) {
            popToRoot()
            return
        }
        if (currentRoute?.isAssessmentResult() == true) popToRoot()
        area = destination
    }

    fun push(route: AppRoute) {
        backStack.add(route)
    }

    /** Replaces the current entry when the current workflow advances without preserving it. */
    fun replaceTop(route: AppRoute) {
        backStack.replaceTopWith(route)
    }

    /**
     * Replaces the completed attempt on top with its [result], collapsing every result directly
     * beneath it.
     *
     * A retake or mistakes practice is pushed on top of the result it started from, so that Back
     * during the unfinished attempt returns there. Once the attempt completes, that source result is
     * stale: keeping it would let Back walk through a pile of older scores. The collapse stops at the
     * first entry that is not a result, so whatever the run was started from — the area root, a
     * builder, a Topic — stays.
     */
    fun completeAttempt(result: AppRoute) {
        val stack = backStack
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
        while (stack.size > 1 && (stack.last() as? AppRoute)?.isAssessmentResult() == true) {
            stack.removeAt(stack.lastIndex)
        }
        stack.add(result)
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

    /**
     * Every detail route on every area's stack, roots excluded — what a caller validating the whole
     * navigation state needs to look at, not only the area on screen.
     */
    fun detailRoutes(): Set<AppRoute> =
        stacks.values.flatMapTo(linkedSetOf()) { stack -> stack.drop(1).filterIsInstance<AppRoute>() }

    /**
     * Removes, from every area's stack, the first entry in [invalid] and everything above it.
     *
     * Structural only: which routes are invalid is the caller's decision, and this knows nothing
     * about why. The whole tail goes because an entry above an invalid one was reached *through* it —
     * a Lesson through its Unit, a builder through its Topic — so it belongs to a path that no longer
     * exists. Nothing is reconstructed and the root always stays.
     *
     * [AppRoute.Settings] on top of a pruned stack is the one exception: it stays open, rebased
     * directly on the root, so a learner who changes a setting is not thrown out of Settings by the
     * consequences of that change. Back then returns to the area's root.
     */
    fun pruneFrom(invalid: Set<AppRoute>) {
        stacks.values.forEach { stack ->
            val first = stack.indexOfFirst { it in invalid }
            if (first < 1) return@forEach
            val keepsSettings = stack.last() == AppRoute.Settings
            while (stack.size > first) stack.removeAt(stack.lastIndex)
            if (keepsSettings) stack.add(AppRoute.Settings)
        }
    }

    /**
     * Returns every area to its root and leaves the learner on [AppRoute.Settings], on top of the
     * start area's root — what resetting learner progress needs, since every result and attempt route
     * on any stack now names a deleted attempt.
     *
     * Every detail goes, not only attempt routes: what remains above an area's root was reached in a
     * session whose history no longer exists, and a Topic or a builder left open would show figures
     * the learner has just erased. Settings stays open for the reason [pruneFrom] keeps it.
     *
     * When Settings is already on top of the start area — the only place it is pushed from — the
     * entries *beneath* it are removed and the entry itself is left in place, so the open screen
     * keeps its state rather than being rebuilt by the reset it just ran.
     */
    fun resetToRootsKeepingSettings() {
        stacks.forEach { (destination, stack) ->
            if (destination != AppTopLevelDestination.Start) {
                while (stack.size > 1) stack.removeAt(stack.lastIndex)
            } else if (stack.size > 1 && stack.last() == AppRoute.Settings) {
                while (stack.size > 2) stack.removeAt(stack.lastIndex - 1)
            } else {
                while (stack.size > 1) stack.removeAt(stack.lastIndex)
                stack.add(AppRoute.Settings)
            }
        }
        area = AppTopLevelDestination.Start
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
