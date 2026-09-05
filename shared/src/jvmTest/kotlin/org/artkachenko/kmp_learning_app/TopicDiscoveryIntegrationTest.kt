package org.artkachenko.kmp_learning_app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.curriculum.learning.content.learningContentModule
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImportResult
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.data.local.curriculum.repository.LocalCurriculumRepository
import org.artkachenko.kmp_learning_app.data.local.saved_questions.savedQuestionDataModule
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicPracticeButtonTag
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserSearchFieldTag
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserHeaderTag
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserViewportTag
import org.artkachenko.kmp_learning_app.ui.topicVisualMarkerTag
import org.koin.compose.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module

/**
 * EPIC-13 spread the discovery experience across four owners: the shell's inset and navigation
 * layout (E13-01), the shared surface hierarchy (E13-02), Topic and Subtopic search (E13-03), and
 * the Topic visual identity (E13-04). Each has isolated tests; these compose the real thing and
 * check that the four still hold together.
 *
 * The boundary is the whole shell — `App()` over Koin and an in-memory Room database holding an
 * imported curriculum — so the navigation graph, the real ViewModels, and the real repositories all
 * take part. Assertions here are therefore about interactions the isolated tests cannot see: what
 * the shell does to the screen's layout, what a search result does to navigation, and what
 * navigation does to the next screen.
 *
 * The fixture uses real curriculum Topic IDs so the markers exercised are the authored ones rather
 * than the unknown-ID fallback, and it is a fixture rather than the bundled question bank so these
 * tests do not depend on authored content that changes for unrelated reasons.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal class TopicDiscoveryIntegrationTest {
    @Test
    fun compactDiscoveryBrowsesSearchesAndOpensATopicResult() = runDiscoveryTest(CompactWidth) {
        waitForTag(topicVisualMarkerTag(UiTopicId))

        // Browsing: the Topic name stays the identity and the authored marker sits beside it.
        onNodeWithText(UiTopicName).assertIsDisplayed()
        onNodeWithText(NetworkingTopicName).assertIsDisplayed()
        onNodeWithTag(topicVisualMarkerTag(NetworkingTopicId), useUnmergedTree = true)
            .assertIsDisplayed()
        onNodeWithTag(TopicBrowserSearchFieldTag).assertIsDisplayed()

        // E13-01 in the assembled shell rather than against a synthetic inset: this host reports
        // no system insets, so the heading must sit at exactly the screen's own header spacing —
        // the shell adds nothing above it — and the list must run into the navigation bar with no
        // band of background between them.
        val rootTop = onNodeWithTag(WindowTag).fetchSemanticsNode().boundsInRoot.top
        val headerTop = onNodeWithTag(TopicBrowserHeaderTag).fetchSemanticsNode().boundsInRoot.top
        assertEquals(
            expected = rootTop + with(density) { HeaderSpacing.toPx() },
            actual = headerTop,
            absoluteTolerance = 0.5f,
        )

        val viewport = onNodeWithTag(TopicBrowserViewportTag).fetchSemanticsNode().boundsInRoot
        val topicsItem = navigationItemBounds(AppTopLevelDestination.TOPICS)
        assertTrue(
            topicsItem.top >= viewport.bottom - 0.5f && topicsItem.left < viewport.right,
            "a compact window should put navigation below the content, not beside it",
        )
        assertTrue(
            topicsItem.top - viewport.bottom < with(density) { NoBandTolerance.toPx() },
            "no background band should remain between the topic list and the navigation bar",
        )

        // Search: a Topic match is the same Topic, so it keeps the browsing marker, and the
        // non-matching Topic leaves the list.
        onNodeWithTag(TopicBrowserSearchFieldTag).performTextInput("networking")
        waitUntilGone(UiTopicName)
        onNodeWithText(NetworkingTopicName).assertIsDisplayed()
        onNodeWithTag(topicVisualMarkerTag(NetworkingTopicId), useUnmergedTree = true)
            .assertIsDisplayed()

        // The result navigates by stable Topic ID into the existing Topic Detail flow, which opens
        // on its own summary rather than starting practice.
        onNodeWithText(NetworkingTopicName).performClick()
        waitForTag(TopicPracticeButtonTag)
        onNodeWithText(HttpSubtopicName).assertIsDisplayed()
        assertNoPracticeQuestionOnScreen()

        // Topic Detail carries the same two concepts for a learner who has completed nothing:
        // current coverage against the ACTIVE bank, and no accuracy at all rather than a fake 0%.
        waitForTextContaining("questions explored")
        onNodeWithText("Curriculum coverage").assertIsDisplayed()
        onNodeWithText("0 of 1 questions explored").assertIsDisplayed()
        onNodeWithText("0 of 1 explored").assertIsDisplayed()
        onNodeWithText("0%").assertDoesNotExist()
        onNodeWithText("All-time accuracy").assertDoesNotExist()

        onNodeWithContentDescription("Back").performClick()
        waitForTag(TopicBrowserSearchFieldTag)
        // Topics keeps its own back stack entry, so returning lands on the search results the
        // learner left rather than on a reloaded list.
        onNodeWithText(NetworkingTopicName).assertIsDisplayed()
        onNodeWithText(UiTopicName).assertDoesNotExist()

        onNodeWithContentDescription("Clear search").performClick()
        waitForText(UiTopicName)
        onNodeWithTag(topicVisualMarkerTag(UiTopicId), useUnmergedTree = true).assertIsDisplayed()

        assertEveryAreaIsReachable()
    }

    @Test
    fun compactSubtopicSearchOpensItsParentTopicAtTheMatchingSubtopic() =
        runDiscoveryTest(CompactWidth) {
            waitForTag(topicVisualMarkerTag(UiTopicId))

            // A Subtopic-only query: no Topic name contains both tokens, so the result has to
            // carry its parent context itself.
            onNodeWithTag(TopicBrowserSearchFieldTag).performTextInput("snapshot state")
            waitForText(StateSubtopicName)
            onNodeWithText(UnpopulatedSubtopicName).assertDoesNotExist()
            onNodeWithText(UiTopicName).assertIsDisplayed()
            onNodeWithTag(topicVisualMarkerTag(UiTopicId), useUnmergedTree = true)
                .assertIsDisplayed()

            // Navigating with the parent Topic ID and the Subtopic ID opens the Topic and brings
            // the match into view: it is the last of thirteen rows on a phone-shaped window, so it
            // is only on screen if the stable-ID positioning ran.
            onNodeWithText(StateSubtopicName).performClick()
            waitForBackControl()
            onNodeWithText(UiTopicName).assertIsDisplayed()
            onNodeWithText(StateSubtopicName).assertIsDisplayed()
            // Positioning is not the same as starting: a Subtopic result opens the Topic, it does
            // not begin practice on it.
            onNodeWithText(QuestionText, substring = true).assertDoesNotExist()

            onNodeWithContentDescription("Back").performClick()
            waitForTag(TopicBrowserSearchFieldTag)

            // A Subtopic with no questions is searchable but is not one of the Topic's practice
            // rows, so there is nothing to scroll to. The Topic must still open normally.
            onNodeWithContentDescription("Clear search").performClick()
            waitForText(UiTopicName)
            onNodeWithTag(TopicBrowserSearchFieldTag).performTextInput("flow bridging")
            waitForText(UnpopulatedSubtopicName)
            onNodeWithText(UnpopulatedSubtopicName).performClick()
            waitForTag(TopicPracticeButtonTag)
            // Nothing to scroll to, so the Topic simply opens at the top of its own content.
            onNodeWithText(uiSubtopicName(1)).assertIsDisplayed()
            onNodeWithText("Topic not available").assertDoesNotExist()
            assertNoPracticeQuestionOnScreen()
        }

    @Test
    fun searchStatesStayDistinctAndFilterTheLoadedCatalogWithoutReadingItAgain() =
        runDiscoveryTest(CompactWidth) { repository ->
            waitForTag(topicVisualMarkerTag(UiTopicId))
            // Waits for learning context too, so the baseline is taken after the enrichment read
            // rather than racing it. No assessment has been completed in this fixture, so every
            // Topic reports the unstudied state rather than a fabricated 0%.
            waitForTextContaining("explored")
            // All three Topics: a brand-new learner gets the neutral state everywhere, never a
            // fabricated 0%.
            assertEquals(3, onAllNodesWithText("Not studied yet").fetchSemanticsNodes().size)
            onNodeWithText("0%").assertDoesNotExist()
            val readsAfterLoad = repository.reads()
            // Coverage costs one read of the ACTIVE bank for the whole screen, not one per Topic
            // card: three Topics are on screen.
            assertEquals(1, repository.questionReads)

            // Question text is deliberately outside search: this word appears only in the
            // fixture's question text, and searching it must find nothing. Typed one character at
            // a time, because what must not reach Room is a keystroke.
            "quizzical".forEach { character ->
                onNodeWithTag(TopicBrowserSearchFieldTag).performTextInput(character.toString())
            }
            waitForText("No topics or subtopics match \"quizzical\"")
            // The no-result state is not the empty-curriculum state.
            onNodeWithText("No topics available").assertDoesNotExist()
            onNodeWithTag(TopicBrowserSearchFieldTag).assertIsDisplayed()

            onNodeWithContentDescription("Clear search").performClick()
            waitForText(UiTopicName)
            onNodeWithTag(topicVisualMarkerTag(UiTopicId), useUnmergedTree = true)
                .assertIsDisplayed()
            onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.TOPICS))
                .assertIsDisplayed()

            // Nine keystrokes and a clear against a catalog that was read once: search filters the
            // loaded catalog in memory rather than querying Room per keystroke, and neither the
            // curriculum nor the coverage derivation runs again.
            assertEquals(readsAfterLoad, repository.reads())
        }

    @Test
    fun anEmptyCatalogKeepsTheEmptyStateAndTheNavigationShell() =
        runDiscoveryTest(CompactWidth, curriculum = deprecatedCurriculum()) {
            waitForText("No topics available")

            // Nothing about the empty state should suggest a search that found nothing, and there
            // is no catalog to search.
            onNodeWithTag(TopicBrowserSearchFieldTag).assertDoesNotExist()
            onNodeWithText("No topics or subtopics match", substring = true).assertDoesNotExist()

            onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.INTERVIEW)).performClick()
            waitForText("Start Mixed Interview")
        }

    @Test
    fun aCatalogFailureRecoversIntoSearchableDiscovery() =
        runDiscoveryTest(CompactWidth, initialFailures = 1) {
            waitForText("Topics could not be loaded")
            onNodeWithText("Retry").performClick()

            waitForTag(topicVisualMarkerTag(UiTopicId))
            onNodeWithText(UiTopicName).assertIsDisplayed()

            onNodeWithTag(TopicBrowserSearchFieldTag).performTextInput("networking")
            waitUntilGone(UiTopicName)
            onNodeWithTag(topicVisualMarkerTag(NetworkingTopicId), useUnmergedTree = true)
                .assertIsDisplayed()
        }

    @Test
    fun wideDiscoveryPutsTheRailBesideSearchAndKeepsResultsNavigable() =
        runDiscoveryTest(WideWidth) {
            waitForTag(topicVisualMarkerTag(UiTopicId))

            val viewport = onNodeWithTag(TopicBrowserViewportTag).fetchSemanticsNode().boundsInRoot
            val topicsItem = navigationItemBounds(AppTopLevelDestination.TOPICS)
            assertTrue(
                topicsItem.right <= viewport.left,
                "a wide window should put navigation beside the content, not below it",
            )
            // Beside a rail there is no bottom bar to clear, so the content runs to the window
            // edge: no bottom-navigation spacing should be reserved for a bar that is not there.
            val windowBottom = onNodeWithTag(WindowTag).fetchSemanticsNode().boundsInRoot.bottom
            assertEquals(windowBottom, viewport.bottom, absoluteTolerance = 0.5f)

            onNodeWithTag(TopicBrowserSearchFieldTag).performTextInput("compose")
            // One query, both result groups: the Topic whose name matches and the Subtopic whose
            // name matches, the latter still showing its parent Topic name and marker.
            waitForText(StateSubtopicName)
            onNodeWithText(KmpTopicName).assertIsDisplayed()
            // The UI Topic appears twice on purpose: once as a Topic match, once as the parent
            // context of the Subtopic match.
            assertEquals(2, onAllNodesWithText(UiTopicName).fetchSemanticsNodes().size)
            onNodeWithTag(topicVisualMarkerTag(KmpTopicId), useUnmergedTree = true)
                .assertIsDisplayed()
            // Both of those rows carry the Topic's marker, the Subtopic result by way of its
            // parent, so the same Topic ID resolves to the same glyph in both result groups.
            assertEquals(
                2,
                onAllNodesWithTag(topicVisualMarkerTag(UiTopicId), useUnmergedTree = true)
                    .fetchSemanticsNodes().size,
            )

            onNodeWithText(StateSubtopicName).performClick()
            waitForBackControl()
            onNodeWithText(StateSubtopicName).assertIsDisplayed()
            // The rail stays available from a detail screen, unlike an assessment.
            onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.TOPICS))
                .assertIsDisplayed()

            onNodeWithContentDescription("Back").performClick()
            waitForTag(TopicBrowserSearchFieldTag)
            assertEveryAreaIsReachable()
        }

    /**
     * The four areas are the same set at both widths; only the control changes. Called at the end
     * of the compact and wide journeys so the check runs against a shell that has already been
     * navigated through, which is where a route-ownership regression would show.
     */
    private suspend fun ComposeUiTest.assertEveryAreaIsReachable() {
        onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.INTERVIEW)).performClick()
        waitForText("Start Mixed Interview")
        onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.PROGRESS)).performClick()
        waitForTextContaining("start tracking your progress")
        onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.MISTAKES)).performClick()
        waitForTextContaining("No unresolved mistakes")
        onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.TOPICS)).performClick()
        waitForTag(topicVisualMarkerTag(UiTopicId))
    }

    /** Topic Detail opens on its own summary; a search result must never start practice. */
    private fun ComposeUiTest.assertNoPracticeQuestionOnScreen() {
        onNodeWithTag(TopicPracticeButtonTag).assertIsDisplayed()
        onNodeWithText(QuestionText, substring = true).assertDoesNotExist()
    }

    private fun ComposeUiTest.navigationItemBounds(destination: AppTopLevelDestination) =
        onNodeWithTag(appNavigationBarItemTag(destination)).fetchSemanticsNode().boundsInRoot

    private suspend fun ComposeUiTest.waitForText(text: String) {
        waitUntil(timeoutMillis = IntegrationWaitTimeoutMillis) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private suspend fun ComposeUiTest.waitForTextContaining(text: String) {
        waitUntil(timeoutMillis = IntegrationWaitTimeoutMillis) {
            onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private suspend fun ComposeUiTest.waitUntilGone(text: String) {
        waitUntil(timeoutMillis = IntegrationWaitTimeoutMillis) {
            onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        }
    }

    /**
     * Waits for a detail screen: Topic Detail carries an AppTopBar and the Topic browser does not,
     * so its Back control is the reliable signal that navigation has landed — the practice button
     * is not, because a positioned Subtopic scrolls the summary that holds it out of the viewport.
     */
    private suspend fun ComposeUiTest.waitForBackControl() {
        waitUntil(timeoutMillis = IntegrationWaitTimeoutMillis) {
            onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private suspend fun ComposeUiTest.waitForTag(tag: String) {
        waitUntil(timeoutMillis = IntegrationWaitTimeoutMillis) {
            onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Boots the real app over an in-memory database at a chosen window width.
     *
     * The width is what decides bar or rail, so it is the one knob these tests need; everything
     * else — navigation, ViewModels, repositories — is the production wiring.
     */
    private fun runDiscoveryTest(
        windowWidth: Dp,
        curriculum: Curriculum = discoveryCurriculum(),
        initialFailures: Int = 0,
        block: suspend ComposeUiTest.(RecordingCurriculumRepository) -> Unit,
    ) {
        synchronized(appIntegrationMainDispatcherLock) {
            stopKoin()
            Dispatchers.setMain(Dispatchers.Unconfined)
            var database: CurriculumDatabase? = null
            try {
                // Sized to the window under test rather than left at the default 1024x768
                // surface: at density 1 that surface silently clips anything taller, so a window
                // height chosen for the fixture would quietly become a shorter one.
                runSkikoComposeUiTest(size = Size(windowWidth.value, WindowHeight.value)) {
                    val db = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
                        .setDriver(BundledSQLiteDriver())
                        .build()
                    database = db
                    assertIs<CurriculumImportResult.Imported>(
                        CurriculumImporter(db, loadCurriculum = { curriculum }).importCurriculum(),
                    )
                    val repository = RecordingCurriculumRepository(
                        delegate = LocalCurriculumRepository(db),
                        initialFailures = initialFailures,
                    )

                    setContent {
                        KoinApplication(
                            configuration = koinConfiguration {
                                modules(
                                    listOf(
                                        curriculumDataModule,
                                        learningContentModule,
                                        assessmentDataModule,
                                        savedQuestionDataModule,
                                        topicStudyPresentationModule,
                                        module {
                                            single<CurriculumDatabase> { db }
                                            single<CurriculumRepository> { repository }
                                        },
                                    ),
                                )
                            },
                        ) {
                            Box(Modifier.size(windowWidth, WindowHeight).testTag(WindowTag)) {
                                App()
                            }
                        }
                    }

                    block(repository)
                }
            } finally {
                stopKoin()
                database?.close()
                Dispatchers.resetMain()
            }
        }
    }

    /**
     * The production repository with a read counter, and an optional opening failure.
     *
     * Counting reads is how "search filters the loaded catalog" is provable end to end rather than
     * only against a fake, and delegating keeps the real Room queries in the test.
     */
    private class RecordingCurriculumRepository(
        private val delegate: CurriculumRepository,
        initialFailures: Int,
    ) : CurriculumRepository {
        private var remainingFailures = initialFailures

        var topicReads: Int = 0
            private set
        var subtopicReads: Int = 0
            private set
        var questionReads: Int = 0
            private set

        fun reads(): Triple<Int, Int, Int> = Triple(topicReads, subtopicReads, questionReads)

        override suspend fun getActiveTopics(): List<Topic> {
            topicReads += 1
            if (remainingFailures > 0) {
                remainingFailures -= 1
                error("catalog unavailable")
            }
            return delegate.getActiveTopics()
        }

        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> {
            subtopicReads += 1
            return delegate.getActiveSubtopics(topicId)
        }

        override suspend fun getActiveQuestions(): List<Question> {
            questionReads += 1
            return delegate.getActiveQuestions()
        }

        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> {
            questionReads += 1
            return delegate.getActiveQuestionsByTopic(topicId)
        }

        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> {
            questionReads += 1
            return delegate.getActiveQuestionsBySubtopic(subtopicId)
        }

        override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> {
            questionReads += 1
            return delegate.getActiveQuestionsByLevels(levels)
        }

        override suspend fun getActiveQuestionsByTopicAndLevels(
            topicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> {
            questionReads += 1
            return delegate.getActiveQuestionsByTopicAndLevels(topicId, levels)
        }

        override suspend fun getActiveQuestionsBySubtopicAndLevels(
            subtopicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> {
            questionReads += 1
            return delegate.getActiveQuestionsBySubtopicAndLevels(subtopicId, levels)
        }

        override suspend fun getTopicById(topicId: String): Topic? = delegate.getTopicById(topicId)

        override suspend fun getSubtopicById(subtopicId: String): Subtopic? =
            delegate.getSubtopicById(subtopicId)

        override suspend fun getQuestionById(questionId: String): Question? {
            questionReads += 1
            return delegate.getQuestionById(questionId)
        }
    }
}

private const val IntegrationWaitTimeoutMillis = 5_000L

/** The sized root the app is composed into, so a test can measure against the window edges. */
private const val WindowTag = "discovery_test_window"

/** Phone-shaped and wide enough for a rail, either side of AppNavigationRailBreakpoint. */
private val CompactWidth = 400.dp
private val WideWidth = 900.dp

/**
 * Tall enough to compose the whole fixture catalogue, with room to spare.
 *
 * The Topic list is a `LazyColumn`, so a row past the viewport is never composed and is absent
 * from the semantics tree — which makes any assertion that counts rows really an assertion about
 * this height. At 700.dp the third Topic card did not fit: it was composed only because a sliver
 * of it still showed, and that sliver was what the count of three rested on. The learning-unit
 * badge E21-01 added to the first row pushed the sliver from 43 pixels down to 15, and roughly
 * one more wrapped line above it would have taken the row out of composition and the count with
 * it. At this height every card is laid out in full with margin to spare, so the assertion says
 * what it means. Width stays the knob these tests actually vary.
 */
private val WindowHeight = 900.dp

/** Mirrors TopicBrowserHeaderSpacing, which is private to the screen. */
private val HeaderSpacing = 12.dp

/** Anything above a hairline here would be the background band E13-01 removed. */
private val NoBandTolerance = 1.dp

private const val UiTopicId = "android_ui"
private const val UiTopicName = "UI — Views & Jetpack Compose"
private const val NetworkingTopicId = "networking"
private const val NetworkingTopicName = "Networking & Serialization"
private const val KmpTopicId = "kmp"
private const val KmpTopicName = "Kotlin Multiplatform & Compose Multiplatform"

private const val StateSubtopicId = "compose_state"
private const val StateSubtopicName = "Compose snapshot state"
private const val UnpopulatedSubtopicId = "snapshot_flow"
private const val UnpopulatedSubtopicName = "Snapshot flow bridging"
private const val HttpSubtopicName = "HTTP clients"

/** Present only in question text, so searching it proves Questions are not searched. */
private const val QuestionText = "Quizzical"

private fun uiSubtopicName(index: Int) = "UI building block $index"

/**
 * Three real curriculum Topics, so the markers are authored ones, and enough Subtopics under the
 * first that the search target sits below the fold of a phone-shaped window.
 */
private fun discoveryCurriculum(): Curriculum {
    val uiBlocks = (1..12).map { index ->
        Subtopic("ui_block_$index", UiTopicId, uiSubtopicName(index))
    }
    val subtopics = uiBlocks +
        Subtopic(StateSubtopicId, UiTopicId, StateSubtopicName) +
        // Active and therefore searchable, but with no questions it is not one of the Topic's
        // practice rows: the missing-scroll-target case.
        Subtopic(UnpopulatedSubtopicId, UiTopicId, UnpopulatedSubtopicName) +
        Subtopic("http_clients", NetworkingTopicId, HttpSubtopicName) +
        Subtopic("expect_actual", KmpTopicId, "expect and actual declarations")

    val questions = subtopics
        .filter { it.id != UnpopulatedSubtopicId }
        .mapIndexed { index, subtopic ->
            question(
                id = "question_$index",
                topicId = subtopic.topicId,
                subtopicId = subtopic.id,
            )
        }

    return Curriculum(
        topics = listOf(
            Topic(UiTopicId, UiTopicName),
            Topic(NetworkingTopicId, NetworkingTopicName),
            Topic(KmpTopicId, KmpTopicName),
        ),
        subtopics = subtopics,
        questions = questions,
    )
}

/** A curriculum whose content has all been deprecated, which is how an empty catalog happens. */
private fun deprecatedCurriculum(): Curriculum = Curriculum(
    topics = listOf(Topic(UiTopicId, UiTopicName, ContentStatus.DEPRECATED)),
    subtopics = listOf(
        Subtopic(StateSubtopicId, UiTopicId, StateSubtopicName, ContentStatus.DEPRECATED),
    ),
    questions = listOf(
        question("question_retired", UiTopicId, StateSubtopicId, ContentStatus.DEPRECATED),
    ),
)

private fun question(
    id: String,
    topicId: String,
    subtopicId: String,
    status: ContentStatus = ContentStatus.ACTIVE,
) = Question(
    id = id,
    topicId = topicId,
    subtopicId = subtopicId,
    text = "$QuestionText question $id",
    answers = listOf(AnswerOption("a", "A"), AnswerOption("b", "B")),
    selectionMode = AnswerSelectionMode.SINGLE,
    level = QuestionLevel.FOUNDATION,
    correctAnswerIds = listOf("a"),
    explanation = "Explanation $id",
    sources = listOf(SourceReference("Source $id", "https://example.com/$id")),
    status = status,
)
