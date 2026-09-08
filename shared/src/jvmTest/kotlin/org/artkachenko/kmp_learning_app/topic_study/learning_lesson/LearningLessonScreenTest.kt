package org.artkachenko.kmp_learning_app.topic_study.learning_lesson

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningBlock
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningCalloutKind
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningDepth
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningSection
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressUiState

/**
 * The Lesson reader as a learner meets it: every authored block variant, the depth layers, the
 * Sources, and the sibling controls.
 *
 * Compact fixtures rather than the shipped Lessons, deliberately. Production content is exercised
 * separately, but an editorial change that removed the last Comparison from the bundled document
 * would quietly remove this coverage with it, so each variant gets a fixture that cannot be
 * edited away.
 */
@OptIn(ExperimentalTestApi::class)
internal class LearningLessonScreenTest {
    @Test
    fun loadingStateRenders() = runComposeUiTest {
        setContent { MaterialTheme { LessonScreen(LearningLessonUiState.Loading) } }

        onNodeWithTag(LearningLessonLoadingTag).assertIsDisplayed()
        onNodeWithText("Loading lesson").assertIsDisplayed()
    }

    @Test
    fun notFoundStateSaysTheLessonIsUnavailableWithoutOfferingRetry() = runComposeUiTest {
        setContent { MaterialTheme { LessonScreen(LearningLessonUiState.NotFound) } }

        onNodeWithText("This lesson is not available.").assertIsDisplayed()
        onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test
    fun errorStateRendersAndRetries() = runComposeUiTest {
        var retryCount = 0
        setContent {
            MaterialTheme {
                LessonScreen(LearningLessonUiState.Error, onRetry = { retryCount += 1 })
            }
        }

        onNodeWithText("Learning content could not be loaded.").assertIsDisplayed()
        onNodeWithText("Retry").performClick()
        assertEquals(1, retryCount)
    }

    @Test
    fun contentRendersTitleAndSummaryAndBackIsAvailable() = runComposeUiTest {
        var backCount = 0
        setContent {
            MaterialTheme {
                LessonScreen(content(), onBack = { backCount += 1 })
            }
        }

        onNodeWithText("Title of lesson_a").assertIsDisplayed()
        onNodeWithText("Summary of lesson_a").assertIsDisplayed()

        onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backCount)
    }

    /**
     * The reading meter measures the document, so it exists only where there is a document to move
     * through. A Lesson that already fits the viewport has no position to report, and a bar pinned
     * at zero underneath it would be stating a journey the reader is not on.
     */
    @Test
    fun readingProgressAppearsForALessonTallerThanTheViewport() = runComposeUiTest {
        setContentWith(
            sections = List(12) { section(LearningBlock.Paragraph("Body paragraph $it.")) },
            height = ShortHeight,
        )

        onNodeWithTag(LearningLessonReadingProgressTag).assertIsDisplayed()
    }

    @Test
    fun readingProgressIsAbsentWhenTheWholeLessonFitsOnScreen() = runComposeUiTest {
        setContentWith(sections = listOf(section(LearningBlock.Paragraph("A one-line lesson."))))

        onNodeWithTag(LearningLessonReadingProgressTag).assertDoesNotExist()
    }

    /**
     * Loading, NotFound, and Error fill the page with one centred message that cannot be scrolled,
     * so a meter over any of them would be measuring nothing.
     */
    @Test
    fun readingProgressIsAbsentOnEveryPageThatIsNotALesson() = runComposeUiTest {
        var state: LearningLessonUiState by mutableStateOf(LearningLessonUiState.Loading)
        setContent {
            MaterialTheme {
                Box(Modifier.size(NarrowWidth, ShortHeight).testTag(TestRootTag)) {
                    LessonScreen(state)
                }
            }
        }

        listOf(
            LearningLessonUiState.Loading,
            LearningLessonUiState.NotFound,
            LearningLessonUiState.Error,
        ).forEach { pageWithoutALesson ->
            state = pageWithoutALesson
            waitForIdle()
            onNodeWithTag(LearningLessonReadingProgressTag).assertDoesNotExist()
        }
    }

    /**
     * Scroll position is not something a screen reader can act on: it moves through the Lesson
     * element by element and never sees where the viewport happens to sit. A `ProgressBarRangeInfo`
     * is enough on its own to make a node focusable, so an uncleared meter would be an unlabelled
     * stop announced as a bare percentage between the toolbar and the title.
     *
     * The assertion is on the merged tree, which is what an accessibility service consumes, and it
     * is what fails if someone later moves the clear onto the indicator itself — where a collapsed
     * semantics chain would keep the range info rather than drop it.
     */
    @Test
    fun readingProgressIsNotAScreenReaderStop() = runComposeUiTest {
        setContentWith(
            sections = List(12) { section(LearningBlock.Paragraph("Body paragraph $it.")) },
            height = ShortHeight,
        )

        onNodeWithTag(LearningLessonReadingProgressTag)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ProgressBarRangeInfo))
    }

    @Test
    fun paragraphBlocksRenderProse() = runComposeUiTest {
        setContentWith(sections = listOf(section(LearningBlock.Paragraph("Composition builds."))))

        onNodeWithText("Composition builds.").performScrollTo().assertIsDisplayed()
    }

    /**
     * The E20/E21 plain-text contract. Authored prose is validated as plain text, so a renderer
     * that quietly interpreted Markdown or HTML would be showing something the author did not
     * write — and would drop the characters they did.
     */
    @Test
    fun authoredMarkdownLikeCharactersRenderLiterally() = runComposeUiTest {
        val authored = "Use **remember** and `State` — see [docs](https://example.com)."
        setContentWith(sections = listOf(section(LearningBlock.Paragraph(authored))))

        onNodeWithText(authored).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun bulletListsRenderEveryItemInAuthoredOrder() = runComposeUiTest {
        val items = listOf("Zebra runs first", "Apple runs second", "Mango runs third")
        setContentWith(sections = listOf(section(LearningBlock.BulletList(items))))

        val tops = items.map { item ->
            onNodeWithText(item).performScrollTo().assertIsDisplayed()
            onNodeWithText(item).fetchSemanticsNode().positionInRoot.y
        }
        assertEquals(tops.sorted(), tops, "Bullet items must keep authored order, not be sorted.")
    }

    @Test
    fun codeBlocksRenderTheirCodeAndAuthoredLanguageLabel() = runComposeUiTest {
        setContentWith(
            sections = listOf(
                section(
                    LearningBlock.Code("val state = remember { 0 }", language = "kotlin"),
                ),
            ),
        )

        onNodeWithText("val state = remember { 0 }").performScrollTo().assertIsDisplayed()
        onNodeWithText("kotlin").performScrollTo().assertIsDisplayed()
    }

    /**
     * The narrow-viewport contract for both overflowing block types: a source line and a table far
     * wider than a phone must scroll inside their own container, so the reading column — and with
     * it the page — never becomes wider than the screen.
     */
    @Test
    fun wideCodeAndTablesScrollInternallyWithoutWideningThePage() = runComposeUiTest {
        setContentWith(
            sections = listOf(
                section(
                    LearningBlock.Paragraph("Ordinary prose above the overflowing blocks."),
                    LearningBlock.Code(
                        code = "fun veryLongSignature(" +
                            (1..12).joinToString { "parameter$it: String" } + ")",
                        language = "kotlin",
                    ),
                    LearningBlock.Comparison(
                        headers = listOf("Concern", "Views", "Compose", "Notes", "Trade-off"),
                        rows = listOf(
                            listOf("Updating", "Setters", "Recompose", "Runtime", "Fewer paths"),
                        ),
                    ),
                ),
            ),
            width = NarrowWidth,
        )

        val rootWidth = onNodeWithTag(TestRootTag).fetchSemanticsNode().boundsInRoot.width
        assertWithinRootWidth(LearningLessonReadingColumnTag, rootWidth)

        onNodeWithTag(LearningLessonCodeBlockTag).performScrollTo().assert(hasScrollAction())
        assertWithinRootWidth(LearningLessonCodeBlockTag, rootWidth)

        onNodeWithTag(LearningLessonComparisonTag).performScrollTo().assert(hasScrollAction())
        assertWithinRootWidth(LearningLessonComparisonTag, rootWidth)
    }

    @Test
    fun comparisonBlocksRenderHeadersAndCells() = runComposeUiTest {
        setContentWith(
            sections = listOf(
                section(
                    LearningBlock.Comparison(
                        headers = listOf("Concern", "Views"),
                        rows = listOf(
                            listOf("Creating the UI", "Inflate a tree"),
                            listOf("Updating the UI", "Call setters"),
                        ),
                    ),
                ),
            ),
        )

        listOf(
            "Concern",
            "Views",
            "Creating the UI",
            "Inflate a tree",
            "Updating the UI",
            "Call setters",
        ).forEach { cell ->
            onNodeWithText(cell).performScrollTo().assertIsDisplayed()
        }
    }

    /**
     * All four kinds carry a distinct visible label. Colour is a second signal here, never the
     * only one, so the callouts stay tellable apart in monochrome or high contrast.
     */
    @Test
    fun everyCalloutKindShowsItsOwnSemanticLabel() = runComposeUiTest {
        setContentWith(
            sections = listOf(
                section(
                    LearningBlock.Callout(LearningCalloutKind.NOTE, "A note body."),
                    LearningBlock.Callout(LearningCalloutKind.KEY_TAKEAWAY, "A takeaway body."),
                    LearningBlock.Callout(LearningCalloutKind.INTERVIEW_FOCUS, "A focus body."),
                    LearningBlock.Callout(LearningCalloutKind.COMMON_MISTAKE, "A mistake body."),
                ),
            ),
        )

        mapOf(
            "Note" to "A note body.",
            "Key takeaway" to "A takeaway body.",
            "Interview focus" to "A focus body.",
            "Common mistake" to "A mistake body.",
        ).forEach { (label, body) ->
            onNodeWithText(label).performScrollTo().assertIsDisplayed()
            onNodeWithText(body).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun depthLayersRenderLearnerFacingLabelsRatherThanEnumNames() = runComposeUiTest {
        setContentWith(
            sections = listOf(
                section(LearningBlock.Paragraph("Core body."), depth = LearningDepth.CORE),
                section(
                    LearningBlock.Paragraph("Practical body."),
                    depth = LearningDepth.PRACTICAL,
                ),
                section(LearningBlock.Paragraph("Senior body."), depth = LearningDepth.SENIOR),
            ),
        )

        listOf("Core", "Practical", "Senior deep dive").forEach { label ->
            onNodeWithText(label).performScrollTo().assertIsDisplayed()
        }
        listOf("CORE", "PRACTICAL", "SENIOR").forEach { enumName ->
            onNodeWithText(enumName).assertDoesNotExist()
        }
    }

    /** A Lesson is not required to carry every depth, and none is manufactured to fill the gap. */
    @Test
    fun aLessonWithoutASeniorSectionShowsNoSeniorLayer() = runComposeUiTest {
        setContentWith(
            sections = listOf(
                section(LearningBlock.Paragraph("Core body."), depth = LearningDepth.CORE),
            ),
        )

        onNodeWithText("Core").assertIsDisplayed()
        onNodeWithText("Senior deep dive").assertDoesNotExist()
        onNodeWithText("Practical").assertDoesNotExist()
    }

    /**
     * An optional subheading appears when authored, and consecutive Sections at one depth keep
     * their authored order and their own headings under a single depth marker.
     */
    @Test
    fun authoredSectionTitlesRenderAndAbsentOnesLeaveNoHeading() = runComposeUiTest {
        setContentWith(
            sections = listOf(
                LearningSection(
                    depth = LearningDepth.CORE,
                    blocks = listOf(LearningBlock.Paragraph("First core body.")),
                    title = "What a composable is",
                ),
                LearningSection(
                    depth = LearningDepth.CORE,
                    blocks = listOf(LearningBlock.Paragraph("Second core body.")),
                    title = "The execution contract",
                ),
                LearningSection(
                    depth = LearningDepth.PRACTICAL,
                    blocks = listOf(LearningBlock.Paragraph("Untitled practical body.")),
                ),
            ),
        )

        onNodeWithText("What a composable is").performScrollTo().assertIsDisplayed()
        onNodeWithText("The execution contract").performScrollTo().assertIsDisplayed()
        onNodeWithText("Untitled practical body.").performScrollTo().assertIsDisplayed()
        // Two Core Sections, one Core marker: the layer is announced once per run.
        onNodeWithText("Core").assertIsDisplayed()
        onNodeWithText("Practical").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sourcesRenderTheirAuthoredTitlesInOrderAndOpenTheirExactUrl() = runComposeUiTest {
        val opened = mutableListOf<String>()
        setContentWith(
            sources = listOf(
                SourceReference("Thinking in Compose", "https://example.com/mental-model"),
                SourceReference("Lifecycle of composables", "https://example.com/lifecycle"),
            ),
            onOpenSource = { opened += it },
        )

        onNodeWithText("Sources").performScrollTo().assertIsDisplayed()
        val first = onNodeWithText("Thinking in Compose").performScrollTo()
        val second = onNodeWithText("Lifecycle of composables").performScrollTo()
        assertTrue(
            first.fetchSemanticsNode().positionInRoot.y <
                second.fetchSemanticsNode().positionInRoot.y,
            "Sources must keep authored order.",
        )

        onNodeWithText("Thinking in Compose").performScrollTo().performClick()
        assertEquals(listOf("https://example.com/mental-model"), opened)
    }

    @Test
    fun aLessonWithoutSourcesShowsNoSourcesSection() = runComposeUiTest {
        setContentWith(sources = emptyList())

        onNodeWithText("Sources").assertDoesNotExist()
    }

    @Test
    fun aFailedSourceIsReportedBesideTheLink() = runComposeUiTest {
        setContentWith(
            sources = listOf(SourceReference("Thinking in Compose", "https://example.com/a")),
            failedSourceUrl = "https://example.com/a",
        )

        onNodeWithText("This source could not be opened.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theFirstLessonOffersOnlyNext() = runComposeUiTest {
        val chosen = mutableListOf<String>()
        setContentWith(
            next = AdjacentLessonUiModel("lesson_b", "State Down, Events Up"),
            onNavigateLesson = { chosen += it },
        )

        onNodeWithTag(LearningLessonPreviousTag).assertDoesNotExist()
        onNodeWithTag(LearningLessonNextTag).performScrollTo().assertIsDisplayed()
        // The title is part of the control, so the action says where it leads rather than only
        // that it moves.
        onNodeWithText("Next").performScrollTo().assertIsDisplayed()
        onNodeWithText("State Down, Events Up").performScrollTo().assertIsDisplayed()

        onNodeWithTag(LearningLessonNextTag).performClick()
        assertEquals(listOf("lesson_b"), chosen)
    }

    @Test
    fun aMiddleLessonOffersBothDirectionsAndEmitsStableIds() = runComposeUiTest {
        val chosen = mutableListOf<String>()
        setContentWith(
            previous = AdjacentLessonUiModel("lesson_a", "Declarative UI"),
            next = AdjacentLessonUiModel("lesson_c", "State Down, Events Up"),
            onNavigateLesson = { chosen += it },
        )

        onNodeWithTag(LearningLessonPreviousTag).performScrollTo().performClick()
        onNodeWithTag(LearningLessonNextTag).performScrollTo().performClick()
        assertEquals(listOf("lesson_a", "lesson_c"), chosen)
    }

    @Test
    fun theLastLessonOffersOnlyPrevious() = runComposeUiTest {
        setContentWith(previous = AdjacentLessonUiModel("lesson_a", "Declarative UI"))

        onNodeWithTag(LearningLessonNextTag).assertDoesNotExist()
        onNodeWithTag(LearningLessonPreviousTag).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theOnlyLessonInAUnitOffersNeitherDirection() = runComposeUiTest {
        setContentWith()

        onNodeWithTag(LearningLessonPreviousTag).assertDoesNotExist()
        onNodeWithTag(LearningLessonNextTag).assertDoesNotExist()
    }

    /**
     * Sibling navigation replaces the route, so the reader must start the new Lesson at the top.
     * Carrying the previous Lesson's offset over would drop the learner into the middle of a page
     * they have not read a word of.
     */
    @Test
    fun openingAnotherLessonStartsAtTheTopOfTheNewLesson() = runComposeUiTest {
        var state by mutableStateOf(
            content(
                lessonId = "lesson_a",
                sections = List(12) { section(LearningBlock.Paragraph("Body paragraph $it.")) },
            ),
        )
        setContent {
            MaterialTheme {
                Box(Modifier.size(NarrowWidth, ShortHeight).testTag(TestRootTag)) {
                    LessonScreen(state)
                }
            }
        }

        onNodeWithText("Body paragraph 11.").performScrollTo().assertIsDisplayed()
        onNodeWithText("Title of lesson_a").assertIsNotDisplayed()

        state = content(
            lessonId = "lesson_b",
            sections = List(12) { section(LearningBlock.Paragraph("Next body paragraph $it.")) },
        )
        waitForIdle()

        onNodeWithText("Title of lesson_b").assertIsDisplayed()
    }

    /**
     * A wide window must not stretch prose across its whole width. The cap is expressed as an
     * invariant rather than an exact dp so retuning `AppLayout.MaxContentWidth` does not break it.
     */
    @Test
    fun aWideWindowKeepsTheReadingColumnNarrowerThanTheWindow() = runComposeUiTest {
        setContentWith(
            sections = listOf(section(LearningBlock.Paragraph("Prose that must not span 1600px."))),
            width = WideWidth,
        )

        val rootWidth = onNodeWithTag(TestRootTag).fetchSemanticsNode().boundsInRoot.width
        val columnWidth =
            onNodeWithTag(LearningLessonReadingColumnTag).fetchSemanticsNode().boundsInRoot.width
        assertTrue(
            columnWidth < rootWidth,
            "Reading column was $columnWidth wide in a $rootWidth window.",
        )
    }

    /**
     * Finishing a Lesson should not mean walking back to the overview to practise. The action is
     * the Unit's, not this Lesson's: it emits no Lesson identity at all, so nothing here can turn
     * into a Lesson-sized quiz under a Unit label.
     */
    @Test
    fun theReaderOffersPracticeForTheOwningUnit() = runComposeUiTest {
        var practiceCount = 0
        setContentWith(onPracticeUnit = { practiceCount += 1 })

        onNodeWithTag(LearningLessonPracticeButtonTag).performClick()

        assertEquals(1, practiceCount)
    }

    /** After the reading and after the way on, so nothing offers to quiz before the material. */
    @Test
    fun practiceFollowsTheReadingAndTheSiblingControls() = runComposeUiTest {
        setContentWith(
            sections = listOf(section(LearningBlock.Paragraph("The body of the lesson."))),
            next = AdjacentLessonUiModel("lesson_b", "The next lesson"),
        )

        val next = onNodeWithTag(LearningLessonNextTag)
            .fetchSemanticsNode().positionInRoot.y
        val practice = onNodeWithTag(LearningLessonPracticeButtonTag)
            .fetchSemanticsNode().positionInRoot.y

        assertTrue(next < practice, "Practice was placed before the sibling controls.")
    }

    /** Nothing to practise while there is no Lesson to read. */
    @Test
    fun practiceIsAbsentWhileTheLessonIsUnavailable() = runComposeUiTest {
        setContent { MaterialTheme { LessonScreen(LearningLessonUiState.NotFound) } }

        onNodeWithTag(LearningLessonPracticeButtonTag).assertDoesNotExist()
    }

    /**
     * Renders the reader inside a fixed viewport so width behaviour is observable, and defaults
     * every callback the test under way does not care about.
     */
    private fun ComposeUiTest.setContentWith(
        sections: List<LearningSection> = emptyList(),
        sources: List<SourceReference> = emptyList(),
        previous: AdjacentLessonUiModel? = null,
        next: AdjacentLessonUiModel? = null,
        onNavigateLesson: (String) -> Unit = {},
        onPracticeUnit: () -> Unit = {},
        onOpenSource: (String) -> Unit = {},
        failedSourceUrl: String? = null,
        width: Dp = NarrowWidth,
        height: Dp = TestHeight,
        studyState: StudyProgressUiState<LessonStudyUiModel> = StudyProgressUiState.Loading,
        onToggleStudied: () -> Unit = {},
    ) {
        setContent {
            MaterialTheme {
                Box(Modifier.size(width, height).testTag(TestRootTag)) {
                    LearningLessonScreen(
                        state = content(
                            sections = sections,
                            sources = sources,
                            previous = previous,
                            next = next,
                            studyState = studyState,
                        ),
                        onBack = {},
                        onRetry = {},
                        onNavigateLesson = onNavigateLesson,
                        onPracticeUnit = onPracticeUnit,
                        onOpenSource = onOpenSource,
                        failedSourceUrl = failedSourceUrl,
                        onToggleStudied = onToggleStudied,
                    )
                }
            }
        }
    }


    /**
     * Unstudied: the state is stated in words, and the control names the action that changes it.
     * The two together are what a screen reader announces — no tick, no colour, and no reliance on
     * either.
     */
    @Test
    fun anUnstudiedLessonShowsItsStateAndOffersTheMarkAction() = runComposeUiTest {
        var toggles = 0
        setContentWith(
            studyState = available(isStudied = false),
            onToggleStudied = { toggles += 1 },
        )

        onNodeWithTag(LearningLessonStudyStatusTag).assertIsDisplayed()
        onNodeWithText("Not studied").assertIsDisplayed()
        onNodeWithTag(LearningLessonStudyActionTag)
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
            // The action's own label, so the control says what it will do and not only what is true.
            .assert(hasText("Mark as studied"))
            // ... and the current value, so what is true is available from the control as well.
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Not studied"))

        onNodeWithTag(LearningLessonStudyActionTag).performClick()
        assertEquals(1, toggles)
    }

    @Test
    fun aStudiedLessonShowsItsStateAndOffersTheUnmarkAction() = runComposeUiTest {
        setContentWith(studyState = available(isStudied = true))

        onNodeWithText("Studied").assertIsDisplayed()
        onNodeWithTag(LearningLessonStudyActionTag)
            .assertIsEnabled()
            .assert(hasText("Mark as not studied"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Studied"))
    }

    /**
     * A pending write is not a persisted one. The badge keeps saying what the database says, and the
     * control cannot be fired again while its own write is in flight.
     */
    @Test
    fun aPendingMutationKeepsThePersistedValueAndDisablesTheAction() = runComposeUiTest {
        var toggles = 0
        setContentWith(
            studyState = available(isStudied = false, isPending = true),
            onToggleStudied = { toggles += 1 },
        )

        onNodeWithText("Not studied").assertIsDisplayed()
        onNodeWithText("Studied").assertDoesNotExist()
        onNodeWithTag(LearningLessonStudyActionTag)
            .assertIsNotEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Not studied"))

        onNodeWithTag(LearningLessonStudyActionTag).performClick()
        assertEquals(0, toggles)
    }

    /** Reading is unaffected while a write is in flight: the page still scrolls and still navigates. */
    @Test
    fun aPendingMutationLeavesTheLessonReadableAndNavigable() = runComposeUiTest {
        setContentWith(
            sections = List(12) { section(LearningBlock.Paragraph("Body paragraph $it.")) },
            next = AdjacentLessonUiModel("lesson_b", "State down, events up"),
            studyState = available(isStudied = false, isPending = true),
            height = ShortHeight,
        )

        onNodeWithText("Title of lesson_a").assertIsDisplayed()
        onNodeWithTag(LearningLessonNextTag).performScrollTo().assertIsDisplayed()
        onNodeWithTag(LearningLessonPracticeButtonTag).performScrollTo().assertIsDisplayed()
    }

    /**
     * An unreadable study record costs the indicator and the action, and nothing else. It must not
     * claim the Lesson is unstudied, and it must not become the screen-level error component.
     */
    @Test
    fun anUnavailableStudyRecordLeavesTheWholeLessonUsableWithoutClaimingNotStudied() =
        runComposeUiTest {
            setContentWith(
                sections = List(12) { section(LearningBlock.Paragraph("Body paragraph $it.")) },
                sources = listOf(SourceReference("Thinking in Compose", "https://example.test/a")),
                previous = AdjacentLessonUiModel("lesson_z", "Declarative UI"),
                next = AdjacentLessonUiModel("lesson_b", "State down, events up"),
                studyState = StudyProgressUiState.Unavailable,
                height = ShortHeight,
            )

            onNodeWithText("Title of lesson_a").assertIsDisplayed()
            onNodeWithTag(LearningLessonStudyUnavailableTag).assertIsDisplayed()
            onNodeWithText("Study progress unavailable").assertIsDisplayed()
            // Never fabricated, in either direction.
            onNodeWithText("Not studied").assertDoesNotExist()
            onNodeWithText("Studied").assertDoesNotExist()
            onNodeWithTag(LearningLessonStudyActionTag).assertDoesNotExist()
            // The screen-level retry component belongs to an unreadable Lesson, not to this.
            onNodeWithText("Retry").assertDoesNotExist()

            onNodeWithText("Thinking in Compose").performScrollTo().assertIsDisplayed()
            onNodeWithTag(LearningLessonPreviousTag).performScrollTo().assertIsDisplayed()
            onNodeWithTag(LearningLessonNextTag).performScrollTo().assertIsDisplayed()
            onNodeWithTag(LearningLessonPracticeButtonTag).performScrollTo().assertIsDisplayed()
        }

    /** Silence while the record is being read: a badge before the answer would be a guess. */
    @Test
    fun aLoadingStudyRecordSaysNothingAboutTheLearner() = runComposeUiTest {
        setContentWith(studyState = StudyProgressUiState.Loading)

        onNodeWithText("Title of lesson_a").assertIsDisplayed()
        onNodeWithTag(LearningLessonStudyStatusTag).assertDoesNotExist()
        onNodeWithTag(LearningLessonStudyActionTag).assertDoesNotExist()
        onNodeWithTag(LearningLessonStudyUnavailableTag).assertDoesNotExist()
        onNodeWithText("Not studied").assertDoesNotExist()
    }

    /**
     * Reading the whole Lesson changes nothing. Scrolling to the bottom, opening a Source, stepping
     * to the next Lesson, and starting practice all leave the study callback untouched — the screen
     * has exactly one way to change study state, and it is the control.
     */
    @Test
    fun readingNavigatingAndPractisingNeverToggleStudyState() = runComposeUiTest {
        var toggles = 0
        setContentWith(
            sections = List(12) { section(LearningBlock.Paragraph("Body paragraph $it.")) },
            sources = listOf(SourceReference("Thinking in Compose", "https://example.test/a")),
            next = AdjacentLessonUiModel("lesson_b", "State down, events up"),
            onNavigateLesson = {},
            onPracticeUnit = {},
            onOpenSource = {},
            studyState = available(isStudied = false),
            onToggleStudied = { toggles += 1 },
            height = ShortHeight,
        )

        onNodeWithText("Thinking in Compose").performScrollTo().performClick()
        onNodeWithTag(LearningLessonNextTag).performScrollTo().performClick()
        onNodeWithTag(LearningLessonPracticeButtonTag).performScrollTo().performClick()

        assertEquals(0, toggles)
    }

    /**
     * The narrow contract: on a phone-shaped window the control fits the viewport, the reading
     * content is still usable, and the way onward is still reachable.
     */
    @Test
    fun theStudyControlFitsANarrowWindowWithoutCrowdingTheReading() = runComposeUiTest {
        setContentWith(
            sections = List(12) { section(LearningBlock.Paragraph("Body paragraph $it.")) },
            next = AdjacentLessonUiModel("lesson_b", "State down, events up"),
            studyState = available(isStudied = true),
            width = NarrowWidth,
            height = ShortHeight,
        )

        val rootWidth = onNodeWithTag(TestRootTag).fetchSemanticsNode().boundsInRoot.width
        assertWithinRootWidth(LearningLessonStudyStatusTag, rootWidth)
        assertWithinRootWidth(LearningLessonStudyActionTag, rootWidth)
        onNodeWithTag(LearningLessonStudyActionTag).assertIsDisplayed()
        onNodeWithText("Title of lesson_a").assertIsDisplayed()
        onNodeWithTag(LearningLessonNextTag).performScrollTo().assertIsDisplayed()
        onNodeWithTag(LearningLessonPracticeButtonTag).performScrollTo().assertIsDisplayed()
    }

    /**
     * The reading meter and the study control measure different things and must not be confused for
     * one another: scrolling to the very bottom moves the meter and leaves the badge alone.
     */
    @Test
    fun reachingTheBottomMovesTheReadingMeterAndNotTheStudyState() = runComposeUiTest {
        setContentWith(
            sections = List(12) { section(LearningBlock.Paragraph("Body paragraph $it.")) },
            studyState = available(isStudied = false),
            height = ShortHeight,
        )

        onNodeWithTag(LearningLessonReadingProgressTag).assertIsDisplayed()
        onNodeWithTag(LearningLessonPracticeButtonTag).performScrollTo()
        waitForIdle()

        // The control is above the fold now, so it is asserted on existence rather than on being
        // visible; what matters is that reaching the end of the document did not change it.
        onNodeWithText("Not studied").assertExists()
        onNodeWithTag(LearningLessonStudyActionTag).assert(hasText("Mark as studied"))
    }

    private fun available(
        isStudied: Boolean,
        isPending: Boolean = false,
    ): StudyProgressUiState<LessonStudyUiModel> =
        StudyProgressUiState.Available(LessonStudyUiModel(isStudied = isStudied, isPending = isPending))

    private fun ComposeUiTest.assertWithinRootWidth(tag: String, rootWidth: Float) {
        val width = onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.width
        assertTrue(width <= rootWidth, "$tag was $width wide in a $rootWidth viewport.")
    }
}

private const val TestRootTag = "learning_lesson_test_root"
private val NarrowWidth = 360.dp
private val WideWidth = 1600.dp
private val TestHeight = 800.dp
private val ShortHeight = 400.dp

private fun section(
    vararg blocks: LearningBlock,
    depth: LearningDepth = LearningDepth.CORE,
): LearningSection = LearningSection(depth = depth, blocks = blocks.toList())

private fun content(
    lessonId: String = "lesson_a",
    sections: List<LearningSection> = emptyList(),
    sources: List<SourceReference> = emptyList(),
    previous: AdjacentLessonUiModel? = null,
    next: AdjacentLessonUiModel? = null,
    studyState: StudyProgressUiState<LessonStudyUiModel> = StudyProgressUiState.Loading,
): LearningLessonUiState.Content =
    LearningLessonUiState.Content(
        unitId = "unit_a",
        lessonId = lessonId,
        title = "Title of $lessonId",
        summary = "Summary of $lessonId",
        sections = sections,
        sources = sources,
        previousLesson = previous,
        nextLesson = next,
        studyState = studyState,
    )

/**
 * The screen with every callback defaulted, for the states that exercise one thing at a time. A
 * distinct name rather than an overload, so nothing in this file can shadow the real screen.
 */
@Composable
private fun LessonScreen(
    state: LearningLessonUiState,
    onBack: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    LearningLessonScreen(
        state = state,
        onBack = onBack,
        onRetry = onRetry,
        onNavigateLesson = {},
        onPracticeUnit = {},
        onOpenSource = {},
    )
}
