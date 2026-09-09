package org.artkachenko.kmp_learning_app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick

/**
 * Selects one of Topic Detail's three tabs from a test that drives the whole app.
 *
 * Topic Detail is three pages behind a pager, so a control that used to be somewhere in one long
 * column is now on exactly one of them. Journey tests state which capability they are exercising by
 * calling this rather than by scrolling, which is both what a learner does and the only thing that
 * works — an unselected page is not composed at all.
 *
 * The wait is on the tab's own selected state rather than on a frame count, so nothing here depends
 * on how long the Material tab and pager animations happen to run for.
 */
@OptIn(ExperimentalTestApi::class)
internal suspend fun ComposeUiTest.selectTopicDetailTab(
    tabTestTag: String,
    timeoutMillis: Long = 10_000,
) {
    waitUntil(timeoutMillis = timeoutMillis) {
        onAllNodesWithTag(tabTestTag).fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithTag(tabTestTag).performClick()
    waitUntil(timeoutMillis = timeoutMillis) {
        runCatching { onNodeWithTag(tabTestTag).assertIsSelected() }.isSuccess
    }
    waitForIdle()
}
