package org.artkachenko.kmp_learning_app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A configuration change on Android recreates the composition, which is where the shell's own
 * state either survives or silently resets.
 *
 * `StateRestorationTester` is unimplemented on skiko, so these drive the same mechanism it does:
 * save through a [SaveableStateRegistry], dispose the subtree, and recompose it seeded from the
 * saved values. Both the area and the per-area stacks travel through that one registry, so this
 * exercises the real `rememberSaveable` path rather than a stand-in for it.
 */
@OptIn(ExperimentalTestApi::class)
internal class AppNavigatorRestorationTest {
    private class RestorableShell {
        var isComposed by mutableStateOf(true)
        lateinit var navigator: AppNavigator
        var registry = SaveableStateRegistry(restoredValues = null, canBeSaved = { true })
    }

    private fun ComposeUiTest.restorableShell(): RestorableShell {
        val shell = RestorableShell()
        setContent {
            if (shell.isComposed) {
                CompositionLocalProvider(LocalSaveableStateRegistry provides shell.registry) {
                    shell.navigator = rememberAppNavigator()
                }
            }
        }
        waitForIdle()
        return shell
    }

    private fun ComposeUiTest.restore(shell: RestorableShell) {
        val saved = shell.registry.performSave()
        shell.isComposed = false
        waitForIdle()
        shell.registry = SaveableStateRegistry(saved, canBeSaved = { true })
        shell.isComposed = true
        waitForIdle()
    }

    @Test
    fun theSelectedAreaAndItsOpenDetailBothSurviveRestoration() = runComposeUiTest {
        val shell = restorableShell()

        shell.navigator.select(AppTopLevelDestination.PROGRESS)
        shell.navigator.push(AppRoute.ProgressTopic("kotlin"))
        waitForIdle()

        restore(shell)

        // The stacks were always restored by rememberNavBackStack. The area was not, so the shell
        // came back on Topics while Progress silently kept the drill-down the learner had open.
        assertEquals(AppTopLevelDestination.PROGRESS, shell.navigator.area)
        assertEquals(AppRoute.ProgressTopic("kotlin"), shell.navigator.currentRoute)
    }

    @Test
    fun aRestoredSecondaryAreaStillHandsBackToTheOuterHandler() = runComposeUiTest {
        val shell = restorableShell()

        shell.navigator.select(AppTopLevelDestination.MISTAKES)
        waitForIdle()

        restore(shell)

        assertEquals(AppTopLevelDestination.MISTAKES, shell.navigator.area)
        assertTrue(shell.navigator.canLeaveArea)
    }

    @Test
    fun aShellRestoredOnItsStartAreaStaysThere() = runComposeUiTest {
        val shell = restorableShell()

        restore(shell)

        assertEquals(AppTopLevelDestination.TOPICS, shell.navigator.area)
        assertEquals(AppRoute.Topics, shell.navigator.currentRoute)
    }

    @Test
    fun topicAndSubtopicSearchTargetSurviveRestorationAsStableIds() = runComposeUiTest {
        val shell = restorableShell()
        shell.navigator.push(
            AppRoute.Topic(
                topicId = "topic_stable_id",
                subtopicId = "subtopic_stable_id",
            ),
        )
        waitForIdle()

        restore(shell)

        assertEquals(
            AppRoute.Topic(
                topicId = "topic_stable_id",
                subtopicId = "subtopic_stable_id",
            ),
            shell.navigator.currentRoute,
        )
    }
}
