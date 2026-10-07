package org.artkachenko.kmp_learning_app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.curriculum.learning.content.learningContentModule
import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.lesson_study.lessonStudyDataModule
import org.artkachenko.kmp_learning_app.data.local.progress_reset.progressResetDataModule
import org.artkachenko.kmp_learning_app.data.local.saved_questions.savedQuestionDataModule
import org.artkachenko.kmp_learning_app.product.ProductMetadata
import org.artkachenko.kmp_learning_app.settings.AppPreferenceStorage
import org.artkachenko.kmp_learning_app.settings.AppearanceStateHolder
import org.artkachenko.kmp_learning_app.settings.SettingsDarkThemeSwitchTag
import org.artkachenko.kmp_learning_app.settings.ThemePreference
import org.artkachenko.kmp_learning_app.settings.ThemePreferenceStore
import org.artkachenko.kmp_learning_app.settings.appearanceModule
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserHeaderTag
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserSettingsTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performTextInput
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.artkachenko.kmp_learning_app.curriculum.content.BundledCurriculumSource
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibilityStateHolder
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.settings.KmpContentPreferenceStore
import org.artkachenko.kmp_learning_app.settings.SettingsKmpContentSwitchTag
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserNoResultsTag
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserSearchFieldTag
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserViewportTag
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.artkachenko.kmp_learning_app.curriculum.visibility.curriculumVisibilityModule

/**
 * Settings reached through the real shell, from the surface a learner actually taps.
 *
 * The narrower tests cover the route rules, the preference rules and the screen; this covers the
 * sentence that joins them — that the action on the Learn home surface opens Settings inside the
 * Learn stack, that the switch there shows and changes the application's own preference, and that
 * Back returns to Learn without a navigation area having appeared.
 *
 * The graph is started globally rather than provided as a composition local, because that is what
 * every host does: the appearance holder is resolved by the application theme as well as by the
 * Settings screen, and starting it the host's way is what makes both of them the same instance.
 * Storage is in memory so the test never touches the developer's own saved appearance.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal class SettingsNavigationIntegrationTest {

    @Test
    fun settingsOpensFromTheLearnHeaderAndBackReturnsToLearn() = runSettingsTest { _ ->
        onNodeWithTag(TopicBrowserHeaderTag).assertIsDisplayed()

        onNodeWithTag(TopicBrowserSettingsTag).performClick()
        waitForIdle()

        onNodeWithText("Appearance").assertIsDisplayed()
        onNodeWithText("About").assertIsDisplayed()
        // Learn's header is gone: Settings was pushed onto the stack, not shown beside it.
        assertEquals(0, onAllNodesWithTag(TopicBrowserHeaderTag).fetchSemanticsNodes().size)

        onNodeWithContentDescription("Back").performClick()
        waitForIdle()

        onNodeWithTag(TopicBrowserHeaderTag).assertIsDisplayed()
    }

    /** Area navigation stays available on Settings: it is browsing, not an assessment in flight. */
    @Test
    fun areaNavigationRemainsAvailableWhileSettingsIsOpen() = runSettingsTest { _ ->
        onNodeWithTag(TopicBrowserSettingsTag).performClick()
        waitForIdle()

        AppTopLevelDestination.entries.forEach { destination ->
            onNodeWithTag(appNavigationBarItemTag(destination)).assertIsDisplayed()
        }
    }

    @Test
    fun theSwitchShowsTheStoredChoiceAndChangingItUpdatesTheApplicationPreference() =
        runSettingsTest(stored = ThemePreference.Light) { graph ->
            val holder = graph.appearance
            onNodeWithTag(TopicBrowserSettingsTag).performClick()
            waitForIdle()

            onNodeWithTag(SettingsDarkThemeSwitchTag).assertIsOff().performClick()
            waitForIdle()

            assertEquals(ThemePreference.Dark, holder.preference.value)
            onNodeWithTag(SettingsDarkThemeSwitchTag).assertIsOn()
        }

    /**
     * The ownership rule, observed: leaving Settings and coming back finds the same choice, because
     * the preference is held by the application rather than by the entry that changed it.
     */
    @Test
    fun theChoiceOutlivesTheSettingsEntryThatMadeIt() =
        runSettingsTest(stored = ThemePreference.Light) { graph ->
            val holder = graph.appearance
            onNodeWithTag(TopicBrowserSettingsTag).performClick()
            waitForIdle()
            onNodeWithTag(SettingsDarkThemeSwitchTag).performClick()
            waitForIdle()

            onNodeWithContentDescription("Back").performClick()
            waitForIdle()
            onNodeWithTag(TopicBrowserSettingsTag).performClick()
            waitForIdle()

            onNodeWithTag(SettingsDarkThemeSwitchTag).assertIsOn()
            assertEquals(ThemePreference.Dark, holder.preference.value)
        }

    /**
     * The Kotlin Multiplatform switch through the real destination: a fresh install shows it off, and
     * each move reaches the app-scoped holder and is persisted as an explicit token. The theme is a
     * separate preference and does not move with it.
     */
    @Test
    fun theKmpSwitchChangesAndPersistsTheApplicationVisibility() =
        runSettingsTest(stored = ThemePreference.Light) { graph ->
            onNodeWithTag(TopicBrowserSettingsTag).performClick()
            waitForIdle()

            assertNull(graph.storage.read(KmpContentPreferenceStore.Key))
            onNodeWithTag(SettingsKmpContentSwitchTag).assertIsOff().performClick()
            waitForIdle()
            assertTrue(graph.visibility.includeKmpContent.value)
            assertEquals(KmpContentPreferenceStore.OnToken, graph.storage.read(KmpContentPreferenceStore.Key))
            onNodeWithTag(SettingsKmpContentSwitchTag).assertIsOn()

            onNodeWithTag(SettingsKmpContentSwitchTag).performClick()
            waitForIdle()
            assertFalse(graph.visibility.includeKmpContent.value)
            assertEquals(KmpContentPreferenceStore.OffToken, graph.storage.read(KmpContentPreferenceStore.Key))
            onNodeWithTag(SettingsKmpContentSwitchTag).assertIsOff()

            // The appearance holder still drives only its own switch.
            assertEquals(ThemePreference.Light, graph.appearance.preference.value)
            onNodeWithTag(SettingsDarkThemeSwitchTag).assertIsOff().performClick()
            waitForIdle()
            assertEquals(ThemePreference.Dark, graph.appearance.preference.value)
            assertFalse(graph.visibility.includeKmpContent.value)
            onNodeWithTag(SettingsKmpContentSwitchTag).assertIsOff()
        }

    /**
     * The learner's actual path: Learn is alive underneath Settings, the switch moves, and Back finds
     * the same Learn screen already showing the new catalogue.
     *
     * The query typed before opening Settings is still in the field afterwards, which a newly created
     * Topic Browser could not show; the KMP results under it prove the retained screen re-read the
     * curriculum. Nothing is reset or re-imported between the two round trips.
     */
    @Test
    fun theLiveLearnScreenFollowsTheSwitchAcrossSettingsRoundTrips() =
        runSettingsTest(importCurriculum = true) { graph ->
            waitUntil(timeoutMillis = AwaitTimeoutMillis) { exists(AndroidSectionHeading) }
            onNodeWithText(AndroidSectionHeading).assertIsDisplayed()
            assertFalse(exists(KmpSectionHeading))

            onNodeWithTag(TopicBrowserSearchFieldTag).performTextInput(KmpOnlyQuery)
            waitForIdle()
            onNodeWithTag(TopicBrowserNoResultsTag).assertIsDisplayed()

            toggleKmpInSettings()
            assertTrue(graph.visibility.includeKmpContent.value)

            waitUntil(timeoutMillis = AwaitTimeoutMillis) { exists(KmpOnlySubtopicName) }
            onNodeWithTag(TopicBrowserSearchFieldTag).assert(hasText(KmpOnlyQuery))
            onNodeWithContentDescription("Clear search").performClick()
            waitForIdle()
            scrollCatalogueToEnd()
            onNodeWithText(KmpSectionHeading).assertIsDisplayed()

            toggleKmpInSettings()
            assertFalse(graph.visibility.includeKmpContent.value)

            // Back on the same scrolled list, now read under the new visibility.
            waitUntil(timeoutMillis = AwaitTimeoutMillis) { catalogueIsShown() && !exists(KmpSectionHeading) }
            scrollCatalogueToEnd()
            // The end of the catalogue is on screen, and it no longer holds a KMP section.
            assertFalse(exists(KmpSectionHeading))
            onNodeWithTag(TopicBrowserSearchFieldTag).performTextInput(KmpOnlyQuery)
            waitForIdle()
            onNodeWithTag(TopicBrowserNoResultsTag).assertIsDisplayed()
        }

    private fun ComposeUiTest.toggleKmpInSettings() {
        onNodeWithTag(TopicBrowserSettingsTag).performClick()
        waitForIdle()
        onNodeWithTag(SettingsKmpContentSwitchTag).performClick()
        waitForIdle()
        onNodeWithContentDescription("Back").performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.catalogueIsShown(): Boolean =
        onAllNodes(BrowseList).fetchSemanticsNodes().isNotEmpty()

    /** Scrolls the browse list as far as it goes, so its last items are composed and on screen. */
    private fun ComposeUiTest.scrollCatalogueToEnd() {
        onNode(BrowseList)
            .performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy -> scrollBy(0f, 100_000f) }
        waitForIdle()
    }

    private fun ComposeUiTest.exists(text: String): Boolean =
        onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun theAboutSectionReportsTheCanonicalProductIdentity() = runSettingsTest { _ ->
        onNodeWithTag(TopicBrowserSettingsTag).performClick()
        waitForIdle()

        onNodeWithText(ProductMetadata.NAME).assertIsDisplayed()
        onNodeWithText("Version ${ProductMetadata.VERSION} (${ProductMetadata.BUILD_NUMBER})")
            .assertIsDisplayed()
    }

    /** The app-scoped preferences a test observes, and the durable storage behind both. */
    private class SettingsTestGraph(
        val appearance: AppearanceStateHolder,
        val visibility: CurriculumVisibilityStateHolder,
        val storage: AppPreferenceStorage,
    )

    /**
     * Boots the real application over an in-memory database.
     *
     * By default no curriculum is imported: most of these tests exercise the Learn header and the
     * Settings destination, and the header is present whatever the catalogue holds.
     * [importCurriculum] loads the bundled curriculum for a test about what the catalogue shows.
     */
    private fun runSettingsTest(
        stored: ThemePreference = ThemePreference.System,
        importCurriculum: Boolean = false,
        block: suspend ComposeUiTest.(SettingsTestGraph) -> Unit,
    ) {
        synchronized(appIntegrationMainDispatcherLock) {
            stopKoin()
            Dispatchers.setMain(Dispatchers.Unconfined)
            try {
                runComposeUiTest {
                    // Deliberately not closed, for the reason SharedHostStartupTest gives: Room
                    // runs queries on its own executor, and closing under in-flight ViewModel work
                    // throws into the global handler and fails whichever test runs next.
                    val database = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
                        .setDriver(BundledSQLiteDriver())
                        .build()
                    if (importCurriculum) {
                        runBlocking {
                            CurriculumImporter(database, loadCurriculum = { BundledCurriculumSource.load() })
                                .importCurriculum()
                        }
                    }
                    val storage = MapPreferenceStorage()
                    ThemePreferenceStore(storage).write(stored)

                    val koin = startKoin {
                        modules(
                            module {
                                single<CurriculumDatabase> { database }
                                single<AppPreferenceStorage> { storage }
                            },
                            curriculumDataModule,
                            learningContentModule,
                            assessmentDataModule,
                            savedQuestionDataModule,
                            lessonStudyDataModule,
                            progressResetDataModule,
                            topicStudyPresentationModule,
                            appearanceModule,
                            curriculumVisibilityModule,
                        )
                    }.koin

                    setContent { App() }
                    waitForIdle()

                    block(
                        SettingsTestGraph(
                            appearance = koin.get(),
                            visibility = koin.get(),
                            storage = storage,
                        ),
                    )
                }
            } finally {
                stopKoin()
                Dispatchers.resetMain()
            }
        }
    }
}

private const val AndroidSectionHeading = "Android Engineering"
private const val KmpSectionHeading = "Kotlin Multiplatform"

/** A shipped KMP Subtopic whose full name matches nothing in the core curriculum. */
private const val KmpOnlySubtopicName = "Koin in KMP"
private const val KmpOnlyQuery = KmpOnlySubtopicName

private const val AwaitTimeoutMillis = 10_000L
private val BrowseList = hasScrollAction() and hasAnyAncestor(hasTestTag(TopicBrowserViewportTag))

/** In-memory durable storage, so the test never reads or writes the developer's own preference. */
private class MapPreferenceStorage : AppPreferenceStorage {
    private val values = mutableMapOf<String, String>()

    override fun read(key: String): String? = values[key]

    override fun write(key: String, value: String?) {
        if (value == null) values.remove(key) else values[key] = value
    }
}
