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
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.curriculum.learning.content.learningContentModule
import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.lesson_study.lessonStudyDataModule
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
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

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
        runSettingsTest(stored = ThemePreference.Light) { holder ->
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
        runSettingsTest(stored = ThemePreference.Light) { holder ->
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

    @Test
    fun theAboutSectionReportsTheCanonicalProductIdentity() = runSettingsTest { _ ->
        onNodeWithTag(TopicBrowserSettingsTag).performClick()
        waitForIdle()

        onNodeWithText(ProductMetadata.NAME).assertIsDisplayed()
        onNodeWithText("Version ${ProductMetadata.VERSION} (${ProductMetadata.BUILD_NUMBER})")
            .assertIsDisplayed()
    }

    /**
     * Boots the real application over an empty in-memory database.
     *
     * No curriculum is imported: this exercises the Learn header and the Settings destination, and
     * the header is present whatever the catalogue holds. `TopicDiscoveryIntegrationTest` is where
     * catalogue content is fixtured.
     */
    private fun runSettingsTest(
        stored: ThemePreference = ThemePreference.System,
        block: suspend ComposeUiTest.(AppearanceStateHolder) -> Unit,
    ) {
        synchronized(appIntegrationMainDispatcherLock) {
            stopKoin()
            Dispatchers.setMain(Dispatchers.Unconfined)
            try {
                runComposeUiTest {
                    // Deliberately not closed, for the reason the other app-level tests give: Room
                    // runs queries on its own executor, and closing under in-flight ViewModel work
                    // throws into the global handler and fails whichever test runs next.
                    val database = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
                        .setDriver(BundledSQLiteDriver())
                        .build()
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
                            topicStudyPresentationModule,
                            appearanceModule,
                        )
                    }.koin

                    setContent { App() }
                    waitForIdle()

                    block(koin.get<AppearanceStateHolder>())
                }
            } finally {
                stopKoin()
            }
        }
    }
}

/** In-memory durable storage, so the test never reads or writes the developer's own preference. */
private class MapPreferenceStorage : AppPreferenceStorage {
    private val values = mutableMapOf<String, String>()

    override fun read(key: String): String? = values[key]

    override fun write(key: String, value: String?) {
        if (value == null) values.remove(key) else values[key] = value
    }
}
