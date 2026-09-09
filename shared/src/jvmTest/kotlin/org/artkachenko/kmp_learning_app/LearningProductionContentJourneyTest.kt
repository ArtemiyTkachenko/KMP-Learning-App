package org.artkachenko.kmp_learning_app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.content.BundledCurriculumSource
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningBlock
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningLesson
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import org.artkachenko.kmp_learning_app.curriculum.learning.content.BundledLearningContentRepository
import org.artkachenko.kmp_learning_app.curriculum.learning.content.learningContentModule
import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImportResult
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.data.local.lesson_study.lessonStudyDataModule
import org.artkachenko.kmp_learning_app.data.local.saved_questions.savedQuestionDataModule
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonCodeBlockTag
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonComparisonTag
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonNextTag
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonPracticeButtonTag
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonPreviousTag
import org.artkachenko.kmp_learning_app.topic_study.learning_lesson.LearningLessonReadingColumnTag
import org.artkachenko.kmp_learning_app.topic_study.learning_unit.LearningUnitPracticeButtonTag
import org.artkachenko.kmp_learning_app.topic_study.learning_unit.learningLessonRowTag
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.DefaultPracticeQuestionCount
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderAvailabilityTag
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderStartButtonTag
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.practiceLevelTag
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.practiceQuestionCountTag
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.practiceSourceTag
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserSearchFieldTag
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicStudyListTag
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.learningUnitCardTag
import org.koin.compose.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module

/**
 * E21-07: the shipped Learn experience, read and practised through the whole running application.
 *
 * Neither half of the content is a fixture here. The Units, Lessons, prose, code, tables, callouts,
 * and Sources are the ones that ship, and so is the assessment catalogue behind the Practice
 * Builder — imported through the real importer, so the builder's preflight counts Questions that
 * actually exist. Only the window size, the database instance, and the URI handler are overridden.
 *
 * What the existing suites cannot say on their own, and this one does:
 *
 * - `LearningContentEndToEndTest` proves every structured block type the shipped Lessons use
 *   survives to the repository, and `LearningLessonScreenTest` proves each type renders — but
 *   from fixtures. Nothing joined the two, so a block type could be authored into production and
 *   never seen through a renderer. [everyBlockTypeTheShippedUnitUsesRendersInTheReader] closes that
 *   loop, and its `when` over the sealed [LearningBlock] is exhaustive, so a new authored variant
 *   fails to compile here until this suite knows how to look for it.
 * - Source links were proven to emit their exact URL from a fixture. Here the learner clicks a
 *   Source the Lesson really carries, and the URL is caught at the boundary the app really uses.
 * - The reader's controls were asserted through test tags, which say nothing about what a screen
 *   reader would announce. Here they are asserted as operable controls carrying visible labels.
 * - Reading and Unit practice were verified from either end — the shell's routes in
 *   `AppNavigationTest`, the derived scope in `LearningUnitPracticeIntegrationTest` — but never
 *   as one movement through the running shell.
 *
 * Content is addressed by what the bundle says rather than by copied prose: snippets are read from
 * the authored blocks at runtime, so an editorial improvement changes both sides of the assertion
 * and only a structural change fails the test.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal class LearningProductionContentJourneyTest {
    @Test
    fun studyActionsOnNewLessonsUpdateTheExistingUnitAndTopicScreens() = runProductionJourneyTest {
        ShippedUnits.drop(1).forEach { unit ->
            openFirstShippedLesson(unit)
            waitForText("Mark as studied")
            onNodeWithText("Mark as studied").performScrollTo().assertOperable("Mark as studied")
            onNodeWithText("Mark as studied").performClick()
            waitForText("Mark as not studied")
            onNodeWithContentDescription("Back").performClick()
            waitForText("1 of ${unit.lessons.size} lessons studied")
            onNodeWithContentDescription("Back").performClick()
            waitForText("1 of ${unit.lessons.size} lessons studied")
            scrollToLearningUnit(unit.id)
            onNodeWithTag(learningUnitCardTag(unit.id)).performClick()
            onNodeWithTag(learningLessonRowTag(unit.lessons.first().id)).performScrollTo().performClick()
            waitForText("Mark as not studied")
            onNodeWithText("Mark as not studied").performScrollTo().assertOperable("Mark as not studied")
            onNodeWithText("Mark as not studied").performClick()
            waitForText("Mark as studied")
            onNodeWithContentDescription("Back").performClick()
            waitForText("0 of ${unit.lessons.size} lessons studied")
            onNodeWithTag(LearnAreaTag).performClick()
        }
    }

    /**
     * Every block type the shipped Unit actually uses, seen rendered in the reader it ships in.
     *
     * The three Lessons are read in the order a learner reads them, through Next, so this also
     * states that each shipped Lesson is reachable and carries its own body rather than the
     * previous one still on screen.
     */
    @Test
    fun everyBlockTypeTheShippedUnitUsesRendersInTheReader() = runProductionJourneyTest {
        openFirstShippedLesson()

        val seen = mutableSetOf<String>()
        var asserted = 0
        var overflowing = 0
        ShippedUnit.lessons.forEachIndexed { index, lesson ->
            waitForText(lesson.title)
            lesson.blocks().forEach { block ->
                seen += assertRenders(block)
                asserted += 1
            }
            if (index < ShippedUnit.lessons.lastIndex) {
                onNodeWithTag(LearningLessonNextTag).performScrollTo().performClick()
            }
        }

        // The guards that keep the loop above from passing vacuously: every authored block was
        // looked for, and the Unit really does exercise more than one kind of block.
        assertEquals(
            ShippedUnit.lessons.sumOf { it.blocks().size },
            asserted,
            "Not every authored block reached an assertion.",
        )
        assertTrue(asserted > 0, "The shipped Lessons authored no blocks to render.")
        assertTrue(seen.size > 1, "The shipped Unit exercised only one block type.")
    }

    /**
     * The same loop over every Unit the document ships, not only the first one.
     *
     * [everyBlockTypeTheShippedUnitUsesRendersInTheReader] proves the renderer handles every block
     * *type*; this proves that every authored Lesson currently in the bundle has actually been
     * through it. The distinction matters because a Unit is authored one issue at a time, and the
     * failure this catches is prose that ships without anyone having seen it rendered — a
     * comparison row that overflows its table, a code block that never became scrollable, a
     * callout whose text never reached the screen.
     *
     * The Units are read from the repository rather than listed, so this widens by itself as the
     * curriculum grows.
     */
    @Test
    fun everyShippedUnitsAuthoredBlocksRenderInTheReader() = runProductionJourneyTest { openedUris ->
        assertTrue(ShippedUnits.size > 1, "Expected the Compose Topic to ship more than one Unit.")

        var asserted = 0
        var overflowing = 0
        ShippedUnits.forEach { unit ->
            openFirstShippedLesson(unit)

            unit.lessons.forEachIndexed { index, lesson ->
                waitForText(lesson.title)
                lesson.blocks().forEach { block ->
                    assertRenders(block)
                    asserted += 1
                }
                // The page never widens: the reading column fits, and the two block types that
                // carry genuinely over-wide content scroll inside themselves instead.
                val rootWidth = onNodeWithTag(WindowTag).fetchSemanticsNode().boundsInRoot.width
                assertWithin(LearningLessonReadingColumnTag, rootWidth)
                if (assertOverflowScrollsInternally(LearningLessonCodeBlockTag, rootWidth)) {
                    overflowing += 1
                }
                if (assertOverflowScrollsInternally(LearningLessonComparisonTag, rootWidth)) {
                    overflowing += 1
                }

                // Every authored Source is reachable and opens the URL it declares.
                lesson.sources.forEach { source ->
                    onNodeWithText(source.title).performScrollTo().assert(hasClickAction())
                    onNodeWithText(source.title).performClick()
                    assertEquals(source.url, openedUris.last())
                }
                if (index < unit.lessons.lastIndex) {
                    onNodeWithTag(LearningLessonNextTag).performScrollTo().performClick()
                }
            }

            onNodeWithTag(LearningLessonPreviousTag).performScrollTo().performClick()
            waitForText(unit.lessons[unit.lessons.lastIndex - 1].title)
            onNodeWithContentDescription("Back").performClick()
            waitForTag(learningLessonRowTag(unit.lessons.first().id))

            // Back to the Topic list so the next Unit is opened the way a learner would.
            onNodeWithTag(LearnAreaTag).performClick()
        }

        assertEquals(
            ShippedUnits.sumOf { unit -> unit.lessons.sumOf { it.blocks().size } },
            asserted,
            "Not every authored block reached an assertion.",
        )
        // Without this the containment check above could pass on content that never overflowed.
        assertTrue(overflowing > 0, "No shipped block was wider than the window: containment unproven.")
    }

    /**
     * A Source the shipped Lesson really carries, opened the way the app really opens it.
     *
     * The URL is never compared against a literal: it is read from the bundle, so re-pointing an
     * authoritative source is an ordinary content edit rather than a test failure, while dropping
     * the link or passing the wrong one is not.
     */
    @Test
    fun aShippedSourceOpensItsAuthoredUrlThroughTheAppsOwnUriBoundary() =
        runProductionJourneyTest { openedUris ->
            openFirstShippedLesson()
            val source = ShippedUnit.lessons.first().sources.first()

            onNodeWithText("Sources").performScrollTo().assertIsDisplayed()
            // The whole control, not a label inside one: what the learner activates has to be the
            // thing carrying the title, or the title is decoration beside an unnamed button.
            val link = onNodeWithText(source.title).performScrollTo()
            link.assert(hasClickAction())
            link.performClick()

            assertEquals(listOf(source.url), openedUris)
        }

    /**
     * The reader's interactive controls, asserted as controls.
     *
     * A test tag proves a node exists; it says nothing about whether the control is operable or
     * what it announces. Each one here has to be enabled, carry a click action, and carry the text
     * that names it — for the sibling cards, both the direction and where it leads, which is the
     * whole reason they are cards carrying a title rather than chevrons.
     */
    @Test
    fun theReadersInteractiveControlsAreOperableAndCarryTheirVisibleLabels() =
        runProductionJourneyTest {
            openFirstShippedLesson()
            onNodeWithTag(LearningLessonNextTag).performScrollTo().performClick()

            val middle = ShippedUnit.lessons[1]
            waitForText(middle.title)

            onNodeWithTag(LearningLessonPreviousTag).performScrollTo().assertOperable(
                "Previous",
                ShippedUnit.lessons[0].title,
            )
            onNodeWithTag(LearningLessonNextTag).performScrollTo().assertOperable(
                "Next",
                ShippedUnit.lessons[2].title,
            )
            onNodeWithTag(LearningLessonPracticeButtonTag).performScrollTo()
                .assertOperable("Practice this unit")
            onNodeWithText(middle.sources.first().title).performScrollTo().assert(hasClickAction())

            // Nothing the learner needs was pushed off a phone-shaped page: the reading column is
            // inside the window, and the blocks too wide for it scroll inside themselves instead of
            // widening it. Asserted against shipped content rather than a synthetic wide line.
            val rootWidth = onNodeWithTag(WindowTag).fetchSemanticsNode().boundsInRoot.width
            assertWithin(LearningLessonReadingColumnTag, rootWidth)
            onAllNodesWithTag(LearningLessonCodeBlockTag, useUnmergedTree = true)
                .assertAll(hasScrollAction())
        }

    /**
     * Reading into practice, in one movement through the running shell.
     *
     * The builder has to arrive as configuration rather than as a started assessment: the shipped
     * Unit named, the ordinary count, level, and source controls present, a preflight count over
     * the shipped Question bank, and area navigation still on screen because nothing has begun. And
     * Back has to return to the Lesson the learner left rather than to the Unit or the Topic.
     */
    @Test
    fun readingFlowsIntoTheUnitBuilderWhichStaysConfigurationAndKeepsAreaNavigation() =
        runProductionJourneyTest {
            openFirstShippedLesson()
            // Area navigation is present while reading, so Progress stays one move away.
            onNodeWithTag(LearnAreaTag).assertIsDisplayed()

            onNodeWithTag(LearningLessonPracticeButtonTag).performScrollTo().performClick()

            waitForText(ShippedUnitBuilderLabel)
            // Configuration, not a started run: every dimension the builder owns is offered.
            onNodeWithTag(practiceQuestionCountTag(DefaultPracticeQuestionCount))
                .assertIsDisplayed()
            QuestionLevel.entries.forEach { level ->
                onNodeWithTag(practiceLevelTag(level)).performScrollTo().assertIsDisplayed()
            }
            PracticeQuestionSource.entries.forEach { source ->
                onNodeWithTag(practiceSourceTag(source)).performScrollTo().assertIsDisplayed()
            }
            onNodeWithTag(PracticeBuilderAvailabilityTag).performScrollTo().assertIsDisplayed()
            onNodeWithTag(PracticeBuilderStartButtonTag).performScrollTo().assertIsEnabled()
            // Setting practice up has started nothing, so the bar stays.
            onNodeWithTag(LearnAreaTag).assertIsDisplayed()

            onNodeWithContentDescription("Back").performClick()
            waitForText(ShippedUnit.lessons.first().title)
        }

    /** The Unit overview offers the same handoff, so practice is not a Lesson-only affair. */
    @Test
    fun theUnitOverviewOffersTheSameHandoffIntoTheBuilder() = runProductionJourneyTest {
        openShippedUnit()

        onNodeWithTag(LearningUnitPracticeButtonTag).performScrollTo()
            .assertOperable("Practice this unit")
        onNodeWithTag(LearningUnitPracticeButtonTag).performClick()

        waitForText(ShippedUnitBuilderLabel)
        onNodeWithTag(LearnAreaTag).assertIsDisplayed()
    }
}

/**
 * One shipped block, seen in the reader, returning the type name it proved.
 *
 * The `when` is exhaustive over the sealed [LearningBlock] on purpose: authoring a new variant into
 * production must fail to compile here until this suite states how to recognise it, which is the
 * same protection `LearningBlockContent` gives the renderer.
 *
 * What is asserted is what the learner can read — a snippet of the authored text, taken from the
 * bundle at runtime. Code and comparison blocks are additionally checked for their own horizontal
 * scroll, because that is what keeps them readable without widening the page.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.assertRenders(block: LearningBlock): String = when (block) {
    is LearningBlock.Paragraph -> {
        assertReadable(block.text)
        "paragraph"
    }
    is LearningBlock.BulletList -> {
        block.items.forEach { assertReadable(it) }
        "bullet_list"
    }
    is LearningBlock.Code -> {
        assertReadable(block.code)
        block.language?.let { assertReadable(it) }
        onAllNodesWithTag(LearningLessonCodeBlockTag, useUnmergedTree = true)
            .assertAll(hasScrollAction())
        "code"
    }
    is LearningBlock.Comparison -> {
        block.headers.forEach { assertReadable(it) }
        block.rows.flatten().forEach { assertReadable(it) }
        onAllNodesWithTag(LearningLessonComparisonTag, useUnmergedTree = true)
            .assertAll(hasScrollAction())
        "comparison"
    }
    is LearningBlock.Callout -> {
        assertReadable(block.text)
        "callout"
    }
}

/**
 * Authored text, found on the page by a leading snippet.
 *
 * A snippet rather than the whole string: a paragraph taller than the viewport can only ever be
 * partly visible, and a substring match still fails when the block did not render, rendered its
 * enum name, or was dropped. Very short authored strings are matched whole.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.assertReadable(text: String) {
    val snippet = text.take(TextSnippetLength)
    val matches = onAllNodesWithText(snippet, substring = true).fetchSemanticsNodes()
    assertTrue(matches.isNotEmpty(), "Authored content did not reach the reader: \"$snippet\".")
}

/** An operable control: enabled, activatable, and named by every label it is supposed to carry. */
private fun SemanticsNodeInteraction.assertOperable(vararg labels: String) {
    assertIsEnabled()
    assert(hasClickAction())
    labels.forEach { assertTextContains(it, substring = true) }
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.assertWithin(tag: String, rootWidth: Float) {
    val width = onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.width
    assertTrue(width <= rootWidth, "$tag was $width wide in a $rootWidth window.")
}

/**
 * Over-wide content scrolls inside its own box rather than stretching the page.
 *
 * `assertRenders` already proves these blocks own a scroll action. The property this adds is
 * containment: a block whose *content* is wider than the window still reports *bounds* that fit
 * the window, which is exactly the difference between scrolling internally and widening the page.
 *
 * Returns whether any node here actually overflowed, so the caller can prove the check met
 * genuinely wide content instead of passing vacuously on content that always fitted.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.assertOverflowScrollsInternally(tag: String, rootWidth: Float): Boolean {
    var overflowed = false
    repeat(onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size) { index ->
        val node = onAllNodesWithTag(tag, useUnmergedTree = true)[index].fetchSemanticsNode()
        val width = node.boundsInRoot.width
        assertTrue(width <= rootWidth, "$tag was $width wide in a $rootWidth window: the page widened.")
        val range = node.config.getOrElseNullable(SemanticsProperties.HorizontalScrollAxisRange) { null }
        assertNotNull(range, "$tag reports no horizontal scroll range, so wide content would widen the page.")
        if (range.maxValue() > 0f) overflowed = true
    }
    return overflowed
}

/**
 * Boots the real `App()` over the shipped curriculum and the shipped learning document.
 *
 * The window is phone shaped, which is the demanding case: a shipped Lesson carries code lines and
 * a five-column comparison far wider than 400dp, so anything that widened the page instead of
 * scrolling inside itself would push the controls these journeys click off screen.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
private fun runProductionJourneyTest(
    block: suspend ComposeUiTest.(openedUris: List<String>) -> Unit,
) {
    synchronized(appIntegrationMainDispatcherLock) {
        stopKoin()
        Dispatchers.setMain(Dispatchers.Unconfined)
        var database: CurriculumDatabase? = null
        try {
            runSkikoComposeUiTest(size = Size(WindowWidth.value, WindowHeight.value)) {
                val db = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
                    .setDriver(BundledSQLiteDriver())
                    .build()
                database = db
                assertIs<CurriculumImportResult.Imported>(
                    CurriculumImporter(db, loadCurriculum = { BundledCurriculumSource.load() })
                        .importCurriculum(),
                )

                val openedUris = mutableListOf<String>()
                setContent {
                    MaterialTheme {
                        KoinApplication(
                            configuration = koinConfiguration {
                                modules(
                                    listOf(
                                        curriculumDataModule,
                                        learningContentModule,
                                        assessmentDataModule,
                                        savedQuestionDataModule,
                                        lessonStudyDataModule,
                                        topicStudyPresentationModule,
                                        module { single<CurriculumDatabase> { db } },
                                    ),
                                )
                            },
                        ) {
                            // The only boundary replaced inside the app: a Source must be shown
                            // reaching the host's URI handler, and a test may not open a browser.
                            CompositionLocalProvider(
                                LocalUriHandler provides RecordingUriHandler(openedUris),
                            ) {
                                Box(Modifier.size(WindowWidth, WindowHeight).testTag(WindowTag)) {
                                    App()
                                }
                            }
                        }
                    }
                }

                block(openedUris)
                assertEquals(0, db.assessmentAttemptDao().countTestAttempts())
                assertTrue(db.studiedLessonDao().getAll().isEmpty(), "Reading must not mark Lessons studied.")
            }
        } finally {
            stopKoin()
            database?.close()
            Dispatchers.resetMain()
        }
    }
}

/**
 * The shipped Unit, read through the same repository the destinations use.
 *
 * Resolved once for the suite rather than per test: it is publisher-owned content that no journey
 * mutates, and loading it is the same work `BundledLearningContentRepository` already caches.
 */
private val ShippedUnit: LearningUnit by lazy {
    runBlocking {
        requireNotNull(BundledLearningContentRepository().getUnitById(ShippedUnitId)) {
            "The bundled learning document no longer contains $ShippedUnitId."
        }
    }
}

/**
 * Every Unit the document currently ships under the Compose Topic, in authored order.
 *
 * Read rather than listed, so authoring a Unit brings it under the renderer automatically
 * instead of leaving newly authored prose as the one thing this suite never looked at.
 */
private val ShippedUnits: List<LearningUnit> by lazy {
    runBlocking {
        BundledLearningContentRepository().getActiveUnitsByTopic(UiTopicId)
    }
}

private val LearnAreaTag: String = appNavigationBarItemTag(AppTopLevelDestination.TOPICS)

/** Brings one Unit's row into view on the Topic's Study tab, whatever its position in the list. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.scrollToLearningUnit(unitId: String) {
    onNodeWithTag(TopicStudyListTag)
        .performScrollToNode(hasTestTag(learningUnitCardTag(unitId)))
}

/**
 * Learn -> the Topic, by clicking what a learner clicks, and confirming they arrived.
 *
 * Two things make a single blind click unreliable here. The Topic's name is on the browser row *and*
 * in Topic Detail's own top bar, so waiting on the name alone can match the screen being left rather
 * than the one being opened — hence waiting for the search field, which only the browser has. And
 * the browser rebuilds its rows as learning context and Continue Learning resolve underneath them,
 * so a click dispatched into a row that is being replaced is simply lost. A learner who tapped a
 * Topic and stayed put would tap again; this does the same, rather than waiting out a navigation
 * that was never started.
 */
@OptIn(ExperimentalTestApi::class)
private suspend fun ComposeUiTest.openTopicFromBrowser() {
    waitForTag(TopicBrowserSearchFieldTag)
    waitForText(UiTopicName)
    repeat(NavigationAttempts) {
        if (onAllNodesWithTag(TopicStudyListTag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        ) {
            return
        }
        onNodeWithText(UiTopicName).performClick()
        waitForIdle()
    }
    // Nothing arrived after several attempts, so let the ordinary wait produce the failure and its
    // message rather than throwing something less informative from here.
    waitForTag(TopicStudyListTag)
}

/** Learn -> the Topic -> a shipped Unit, by clicking what a learner clicks. */
@OptIn(ExperimentalTestApi::class)
private suspend fun ComposeUiTest.openShippedUnit(unit: LearningUnit = ShippedUnit) {
    openTopicFromBrowser()
    // A Topic opens on its Study tab, and the Units are a lazy list: a Unit further down does not
    // exist in the semantics tree until the list has been scrolled to it.
    waitForTag(TopicStudyListTag)
    scrollToLearningUnit(unit.id)
    onNodeWithTag(learningUnitCardTag(unit.id)).performClick()
    waitForTag(learningLessonRowTag(unit.lessons.first().id))
}

@OptIn(ExperimentalTestApi::class)
private suspend fun ComposeUiTest.openFirstShippedLesson(unit: LearningUnit = ShippedUnit) {
    openShippedUnit(unit)
    onNodeWithTag(learningLessonRowTag(unit.lessons.first().id)).performClick()
    waitForText(unit.lessons.first().title)
}

@OptIn(ExperimentalTestApi::class)
private suspend fun ComposeUiTest.waitForText(text: String) {
    waitUntil(timeoutMillis = JourneyTimeoutMillis) {
        onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
}

@OptIn(ExperimentalTestApi::class)
private suspend fun ComposeUiTest.waitForTag(tag: String) {
    waitUntil(timeoutMillis = JourneyTimeoutMillis) {
        onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
}

private class RecordingUriHandler(private val opened: MutableList<String>) : UriHandler {
    override fun openUri(uri: String) {
        opened += uri
    }
}

private fun LearningLesson.blocks(): List<LearningBlock> = sections.flatMap { it.blocks }

private const val JourneyTimeoutMillis = 10_000L

/** How many times the journey re-taps a Topic row that did not navigate. */
private const val NavigationAttempts = 3
private const val TextSnippetLength = 40
private const val WindowTag = "learning_journey_window"
private val WindowWidth: Dp = 400.dp
private val WindowHeight: Dp = 900.dp

/** Shipped identities, not fixtures: see the class comment. */
private const val ShippedUnitId = "unit_thinking_in_compose"
private const val UiTopicId = "android_ui"
private const val UiTopicName = "UI — Views & Jetpack Compose"
private const val ShippedUnitBuilderLabel = "Learning unit: Thinking in Compose"
