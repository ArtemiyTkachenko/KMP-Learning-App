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
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
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
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
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
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentProgressMeterTag
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingFinishTag
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingSubmitTag
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
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
import org.artkachenko.kmp_learning_app.topic_study.learning_unit.LearningUnitStudyProgressTag
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultPracticeAgainTag
import org.artkachenko.kmp_learning_app.topic_study.learning_unit.learningLessonRowTag
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderAvailabilityTag
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderStartButtonTag
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.practiceLevelTag
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.practiceSourceTag
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserSearchFieldTag
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicStudyListTag
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicStudyProgressTag
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
            val topicLessonCount = ShippedUnits
                .filter { it.topicId == unit.topicId }
                .sumOf { it.lessons.size }
            openFirstShippedLesson(unit)
            waitForText("Complete lesson")
            onNodeWithText("Complete lesson").performScrollTo().assertOperable("Complete lesson")
            onNodeWithText("Complete lesson").performClick()
            waitForText("Mark as not studied")
            onNodeWithContentDescription("Back").performClick()
            onNode(hasScrollAction()).performScrollToNode(hasTestTag(LearningUnitStudyProgressTag))
            waitForText("1 of ${unit.lessons.size} lessons studied")
            onNodeWithContentDescription("Back").performClick()
            onNodeWithTag(TopicStudyListTag).performScrollToNode(hasTestTag(TopicStudyProgressTag))
            waitForText("1 of $topicLessonCount lessons studied")
            scrollToLearningUnit(unit.id)
            onNodeWithTag(learningUnitCardTag(unit.id))
                .performSemanticsAction(SemanticsActions.OnClick)
            waitUntil(
                conditionDescription = "Unit ${unit.id} reopens after its progress update",
                timeoutMillis = JourneyTimeoutMillis,
            ) {
                onAllNodesWithTag(TopicStudyListTag, useUnmergedTree = true)
                    .fetchSemanticsNodes().isEmpty() &&
                    onAllNodesWithText(unit.title).fetchSemanticsNodes().isNotEmpty()
            }
            onNode(hasScrollAction()).performScrollToNode(
                hasTestTag(learningLessonRowTag(unit.lessons.first().id)),
            )
            onNodeWithTag(learningLessonRowTag(unit.lessons.first().id)).performClick()
            waitForText("Mark as not studied")
            onNodeWithText("Mark as not studied").performScrollTo().assertOperable("Mark as not studied")
            onNodeWithText("Mark as not studied").performClick()
            waitForText("Complete lesson")
            onNodeWithContentDescription("Back").performClick()
            onNode(hasScrollAction()).performScrollToNode(hasTestTag(LearningUnitStudyProgressTag))
            waitForText("0 of ${unit.lessons.size} lessons studied")
            onNodeWithContentDescription("Back").performClick()
            onNodeWithTag(TopicStudyListTag).performScrollToNode(hasTestTag(TopicStudyProgressTag))
            waitForText("0 of $topicLessonCount lessons studied")
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
        assertTrue(ShippedUnits.size > 1, "Expected the learning document to ship more than one Unit.")

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
                // Compact comparisons stack their cells and therefore need no hidden scroll.

                // Every authored Source is reachable and opens the URL it declares.
                lesson.sources.forEach { source ->
                    val sourceNode = onNode(hasText(source.title) and hasClickAction())
                    sourceNode.performScrollTo()
                    sourceNode.performClick()
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
     * The widest authored comparison, read at a desktop-shaped window rather than a phone-shaped one.
     *
     * [everyShippedUnitsAuthoredBlocksRenderInTheReader] reads the whole document at 400dp, where
     * every comparison renders as the stacked compact form. The table form is a different renderer:
     * it lays every column out at a fixed width inside a horizontal scroll, so the wider the
     * authored table, the further its content exceeds the reading measure. Nothing exercised that
     * path over authored content, and the Units that ship the widest decision tables are the newest
     * ones — a six-column selection table is over 1200dp of content inside a 600dp column.
     *
     * The Lesson is derived rather than named, so this follows the widest table the document
     * actually has. The assertions are what separates the two renderers: the compact form drops the
     * first header and never scrolls horizontally, so a visible first header plus a genuine scroll
     * range is the proof that the table form was taken and contained.
     */
    @Test
    fun theWidestAuthoredComparisonStaysInsideTheReadingColumnAsATable() =
        runProductionJourneyTest(windowWidth = WideWindowWidth, windowHeight = WideWindowHeight) {
            val (unit, lesson, widest) = ShippedUnits
                .flatMap { shipped -> shipped.lessons.map { shipped to it } }
                .flatMap { (shipped, authored) ->
                    authored.blocks().filterIsInstance<LearningBlock.Comparison>()
                        .map { Triple(shipped, authored, it) }
                }
                .maxByOrNull { it.third.headers.size }
                ?: error("The learning document authors no comparison blocks.")
            assertTrue(
                widest.headers.size >= WideComparisonColumns,
                "The widest authored comparison has ${widest.headers.size} columns, " +
                    "so this journey no longer meets a table wider than the reading column.",
            )

            openShippedLesson(unit, lesson)

            // The table form renders every header, including the first, which the compact form
            // consumes as each row's heading instead.
            widest.headers.forEach { assertReadable(it) }
            widest.rows.flatten().forEach { assertReadable(it) }

            val rootWidth = onNodeWithTag(WindowTag).fetchSemanticsNode().boundsInRoot.width
            assertWithin(LearningLessonReadingColumnTag, rootWidth)
            assertTrue(
                assertOverflowScrollsInternally(LearningLessonComparisonTag, rootWidth),
                "No comparison on ${lesson.id} overflowed its container, so the table form was " +
                    "either not taken or never met content wider than the reading column.",
            )
            // Code blocks are the other renderer that may legitimately exceed the measure, and the
            // wider window must not have let one of them widen the page either.
            assertOverflowScrollsInternally(LearningLessonCodeBlockTag, rootWidth)
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
            val link = onNode(hasText(source.title) and hasClickAction()).performScrollTo()
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
            onNode(hasText(middle.sources.first().title) and hasClickAction())
                .performScrollTo()
                .assert(hasClickAction())

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

    /**
     * The other end of that handoff: shipped Questions answered through the running assessment UI.
     *
     * Every other suite stops short of this. `LearningUnitPracticeIntegrationTest` resolves a Unit's
     * pool through the production resolver and drives a run through the `AssessmentTakingViewModel`,
     * which proves the routing and the persistence but renders nothing;
     * `FocusedLearningJourneyIntegrationTest` drives the assessment UI but over a fixture
     * catalogue of short, invented Questions; and
     * [readingFlowsIntoTheUnitBuilderWhichStaysConfigurationAndKeepsAreaNavigation] reaches an
     * enabled Start button and deliberately never presses it. So no authored Question had ever
     * been seen through the screen a learner answers it on, and the failure that hides is a real
     * one: an authored stem or option long enough to clip or to widen the page, or an explanation
     * that never reaches the screen after submission.
     *
     * The synthesis Unit is used because its pool carries both of the architecture bank's ADVANCED
     * Questions, which have the longest stems and options in the Topic. Nothing here restates the
     * pool: the candidates are derived from the Unit's own primary concepts exactly as the resolver
     * derives them, and the Question on screen is recognised by its authored stem, so a re-mapping
     * changes what this journey reads rather than breaking it.
     *
     * The first Question is answered wrongly on purpose. Submission is one-way, so it is the only
     * place the incorrect branch can be reached, and both branches have to put the authored
     * explanation on screen.
     */
    @Test
    fun shippedArchitectureQuestionsAreAnsweredThroughTheRunningAssessmentUi() =
        runProductionJourneyTest(expectedAttempts = 1) {
            val unit = ArchitectureSynthesisUnit
            val candidates = architecturePracticeCandidates(unit)
            assertTrue(
                candidates.any { it.level == QuestionLevel.ADVANCED },
                "The synthesis Unit's concepts no longer reach an ADVANCED Question, " +
                    "so this journey no longer reads the longest authored stems.",
            )

            openShippedUnit(unit)
            // The practice control sits above the Lesson list, and `openShippedUnit` leaves the
            // lazy list scrolled down to the first Lesson row, so it has to be brought back.
            val builderLabel = "Learning unit: ${unit.title}"
            tapUntil(LearningUnitPracticeButtonTag, arrived = { isDisplayingText(builderLabel) }) {
                onNode(hasScrollAction())
                    .performScrollToNode(hasTestTag(LearningUnitPracticeButtonTag))
            }
            waitForText(builderLabel)

            // Start is disabled until the builder's availability preflight settles, so the tap is
            // retried rather than assumed to have landed on an enabled control.
            tapUntil(PracticeBuilderStartButtonTag, arrived = { isDisplayingTag(AssessmentProgressMeterTag) }) {
                onNodeWithTag(PracticeBuilderStartButtonTag).performScrollTo()
            }
            waitForTag(AssessmentProgressMeterTag)

            val rootWidth = onNodeWithTag(WindowTag).fetchSemanticsNode().boundsInRoot.width
            val answered = mutableSetOf<String>()
            while (true) {
                waitForIdle()
                // The progress meter is pinned outside the scrolling content and renders only
                // while a Question is on screen, so its absence is the end of the run — which
                // arrives straight after the last answer, because practice completes itself
                // rather than stopping on a finish step.
                if (!isDisplayingTag(AssessmentProgressMeterTag)) {
                    break
                }

                // The lazy list keeps the scroll position the previous Question's explanation
                // left it at, so the new stem is off screen and uncomposed until the header it
                // shares an item with is scrolled back into view.
                onNode(hasScrollAction())
                    .performScrollToNode(hasText("Question ", substring = true))
                val question = candidates.firstOrNull { candidate ->
                    candidate.id !in answered &&
                        onAllNodesWithText(candidate.text).fetchSemanticsNodes().isNotEmpty()
                }
                assertNotNull(
                    question,
                    "The taking screen showed no unanswered authored stem from the Unit's own pool.",
                )
                val firstQuestion = answered.isEmpty()

                // How far through the run the learner is stays on screen beside the stem.
                onNodeWithTag(AssessmentProgressMeterTag).assertIsDisplayed()
                assertReadableWithin(question.text, rootWidth)
                // Every authored option, including the long ones, is on screen and selectable.
                question.answers.forEach { answer ->
                    assertReadableWithin(answer.text, rootWidth)
                    answerRow(answer.text).assertIsEnabled()
                }

                // The first Question takes the incorrect branch; the rest take the correct one.
                val chosen = question.answers.first {
                    (it.id in question.correctAnswerIds) != firstQuestion
                }
                answerRow(chosen.text).performSemanticsAction(SemanticsActions.OnClick)

                // Feedback names the outcome and carries the authored explanation either way. The
                // wrong branch always picks an option outside the correct set, so it is the plain
                // incorrect verdict rather than the partial one.
                val verdict = if (firstQuestion) "Incorrect" else "Correct"
                onNode(hasScrollAction())
                    .performScrollToNode(hasTestTag(AssessmentTakingSubmitTag))
                // Submit is disabled until the selection above has been applied, and it is not
                // re-tapped once it fires: after feedback the same control becomes Next, so a
                // retry here would skip a Question rather than recover anything.
                waitUntil(timeoutMillis = JourneyTimeoutMillis) {
                    isClickable(AssessmentTakingSubmitTag)
                }
                onNodeWithTag(AssessmentTakingSubmitTag)
                    .performSemanticsAction(SemanticsActions.OnClick)
                waitForText(verdict)
                assertReadableWithin(question.explanation, rootWidth)
                if (firstQuestion) {
                    // The answer they should have picked is marked on its own row now, so what has
                    // to be readable is the option itself carrying the missed label — not a
                    // sentence restating it under the list.
                    onNodeWithText("\u2715 Missed").assertIsDisplayed()
                    val key = question.answers.single { it.id in question.correctAnswerIds }
                    assertReadableWithin(key.text, rootWidth)
                }

                answered += question.id
                // The same control, now labelled Next and always enabled while feedback is shown.
                onNode(hasScrollAction()).performScrollToNode(hasTestTag(AssessmentTakingSubmitTag))
                onNodeWithTag(AssessmentTakingSubmitTag)
                    .performSemanticsAction(SemanticsActions.OnClick)
            }

            // The run covered the Unit's whole pool rather than stopping after one Question.
            assertEquals(candidates.map { it.id }.toSet(), answered)
            // Practice completes itself after its final feedback rather than showing the finish
            // step Interview uses, so the learner arrives at the existing Results screen directly.
            assertEquals(
                0,
                onAllNodesWithTag(AssessmentTakingFinishTag, useUnmergedTree = true)
                    .fetchSemanticsNodes().size,
                "Unit practice stopped on a finish step that this product deliberately skips.",
            )
            waitForTag(FocusedResultPracticeAgainTag)
            // Scored over the authored keys: one deliberate wrong answer out of the whole pool.
            onNodeWithText("Practice complete").assertIsDisplayed()
            onNodeWithText("Score: ${candidates.size - 1} / ${candidates.size}").assertIsDisplayed()
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
        assertReadable(block.code, markdown = false)
        block.language?.let { assertReadable(it, markdown = false) }
        onAllNodesWithTag(LearningLessonCodeBlockTag, useUnmergedTree = true)
            .assertAll(hasScrollAction())
        "code"
    }
    is LearningBlock.Comparison -> {
        // Compact comparisons use the first cell in each row as the concern heading, so only the
        // value-column headers are repeated in the stacked presentation.
        block.headers.drop(1).forEach { assertReadable(it) }
        block.rows.flatten().forEach { assertReadable(it) }
        assertTrue(
            onAllNodesWithTag(LearningLessonComparisonTag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty(),
        )
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
private fun ComposeUiTest.assertReadable(text: String, markdown: Boolean = true) {
    val visibleText = if (markdown) {
        text.replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
            .replace("**", "")
            .replace("*", "")
            .replace("`", "")
    } else {
        text
    }
    val snippet = visibleText.take(TextSnippetLength)
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
    windowWidth: Dp = WindowWidth,
    windowHeight: Dp = WindowHeight,
    // Reading must start nothing, so zero is the default and every reading journey keeps it. The
    // one journey that presses Start states the single attempt it means to create, which is what
    // stops an accidental run elsewhere from passing as ordinary reading.
    expectedAttempts: Int = 0,
    block: suspend ComposeUiTest.(openedUris: List<String>) -> Unit,
) {
    synchronized(appIntegrationMainDispatcherLock) {
        stopKoin()
        Dispatchers.setMain(Dispatchers.Unconfined)
        var database: CurriculumDatabase? = null
        try {
            runSkikoComposeUiTest(size = Size(windowWidth.value, windowHeight.value)) {
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
                                Box(Modifier.size(windowWidth, windowHeight).testTag(WindowTag)) {
                                    App()
                                }
                            }
                        }
                    }
                }

                block(openedUris)
                assertEquals(expectedAttempts, db.assessmentAttemptDao().countTestAttempts())
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
 * Every active Unit in the document, in authored order.
 *
 * Read rather than listed, so authoring a Unit brings it under the renderer automatically
 * instead of leaving newly authored prose as the one thing this suite never looked at.
 */
private val ShippedUnits: List<LearningUnit> by lazy {
    runBlocking {
        BundledLearningContentRepository().getActiveUnits()
    }
}

private val ShippedTopicNames: Map<String, String> by lazy {
    runBlocking {
        BundledCurriculumSource.load().topics.associate { it.id to it.name }
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
 * Search makes every Topic selectable in the phone-sized viewport, including rows that the browser's
 * lazy list has not composed. The query is replaced because the browser preserves it after Back.
 * The Topic's name is also on Topic Detail's top bar, so the search field is the screen-specific
 * signal that the browser is ready before the row is clicked.
 */
@OptIn(ExperimentalTestApi::class)
private suspend fun ComposeUiTest.openTopicFromBrowser(topicName: String = UiTopicName) {
    waitForTag(TopicBrowserSearchFieldTag)
    onNodeWithTag(TopicBrowserSearchFieldTag).performTextReplacement(topicName)
    waitUntil(
        conditionDescription = "Clickable Topic result $topicName appears after search",
        timeoutMillis = JourneyTimeoutMillis,
    ) {
        onAllNodesWithText(topicName).fetchSemanticsNodes().any { node ->
            node.config.getOrElseNullable(SemanticsProperties.EditableText) { null } == null &&
                node.config.getOrElseNullable(SemanticsActions.OnClick) { null } != null
        }
    }
    repeat(NavigationAttempts) {
        val browserVisible = onAllNodesWithTag(TopicBrowserSearchFieldTag, useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
        val topicVisible = onAllNodesWithTag(TopicStudyListTag, useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
        if (!browserVisible && topicVisible) {
            return
        }
        // Re-tap only while the row is still on screen. The retry exists for a tap that did not
        // navigate, but a tap that *did* leaves the Topic's list a frame or more away while the
        // row it came from has already left composition — and tapping a node that is gone throws
        // a node-not-found instead of retrying, turning a slow frame into a failure about the
        // test's own timing rather than about the content under test.
        val matches = onAllNodesWithText(topicName)
        val resultIndex = matches.fetchSemanticsNodes().indexOfFirst { node ->
            node.config.getOrElseNullable(SemanticsProperties.EditableText) { null } == null &&
                node.config.getOrElseNullable(SemanticsActions.OnClick) { null } != null
        }
        if (browserVisible && resultIndex >= 0) {
            // Search rows animate into place. Invoke their click contract instead of tapping a
            // coordinate that may have moved between the semantics lookup and input dispatch.
            matches[resultIndex].performSemanticsAction(SemanticsActions.OnClick)
        }
        waitForIdle()
    }
    // Nothing arrived after several attempts, so let the ordinary wait produce the failure and its
    // message rather than throwing something less informative from here.
    waitUntil(
        conditionDescription = "Topic $topicName opens from its search result",
        timeoutMillis = JourneyTimeoutMillis,
    ) {
        onAllNodesWithTag(TopicBrowserSearchFieldTag, useUnmergedTree = true)
            .fetchSemanticsNodes().isEmpty() &&
            onAllNodesWithTag(TopicStudyListTag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
    }
}

/** Learn -> the Topic -> a shipped Unit, by clicking what a learner clicks. */
@OptIn(ExperimentalTestApi::class)
private suspend fun ComposeUiTest.openShippedUnit(unit: LearningUnit = ShippedUnit) {
    openTopicFromBrowser(ShippedTopicNames.getValue(unit.topicId))
    // A Topic opens on its Study tab, and the Units are a lazy list: a Unit further down does not
    // exist in the semantics tree until the list has been scrolled to it.
    waitForTag(TopicStudyListTag)
    repeat(NavigationAttempts) {
        if (onAllNodesWithTag(TopicStudyListTag, useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        ) {
            waitForIdle()
            return@repeat
        }
        // A late progress refresh can replace the lazy list and reset its scroll position.
        scrollToLearningUnit(unit.id)
        // Invoke the row's click contract after scrolling it into the lazy semantics tree. A
        // coordinate click can land on the floating navigation when scroll-to-visible places a
        // final row beneath it, and an extra swipe can evict an early row from a longer list.
        onNodeWithTag(learningUnitCardTag(unit.id))
            .performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
    }
    waitUntil(
        conditionDescription = "Unit ${unit.id} opens from the Topic Study list",
        timeoutMillis = JourneyTimeoutMillis,
    ) {
        onAllNodesWithTag(TopicStudyListTag, useUnmergedTree = true)
            .fetchSemanticsNodes().isEmpty() &&
            onAllNodesWithText(unit.title).fetchSemanticsNodes().isNotEmpty()
    }
    // A long title and summary can put the first Lesson below a phone-sized viewport.
    onNode(hasScrollAction()).performScrollToNode(hasTestTag(learningLessonRowTag(unit.lessons.first().id)))
    waitForTag(learningLessonRowTag(unit.lessons.first().id))
}

/** Learn -> the Topic -> a Unit -> one named Lesson, wherever it sits in the Unit's list. */
@OptIn(ExperimentalTestApi::class)
private suspend fun ComposeUiTest.openShippedLesson(unit: LearningUnit, lesson: LearningLesson) {
    openShippedUnit(unit)
    onNode(hasScrollAction()).performScrollToNode(hasTestTag(learningLessonRowTag(lesson.id)))
    onNodeWithTag(learningLessonRowTag(lesson.id)).performClick()
    waitForText(lesson.title)
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

/**
 * Tap a control and keep tapping until the screen it opens is on screen.
 *
 * A single coordinate tap on a control in a lazy list is not reliable, and this is the hazard
 * [openShippedUnit] already documents and guards against the same way: a late progress refresh can
 * replace the list and move what was just scrolled to, so the tap lands somewhere that is no longer
 * the control and does nothing. A control whose screen is still settling can also be disabled when
 * the tap arrives, which drops it just as quietly. So the click contract is invoked as a semantics
 * action rather than by coordinate, the node is re-scrolled and re-tapped, and a tap is attempted
 * only while the node actually carries a click action — which is what lets a disabled control be
 * waited through instead of throwing.
 *
 * Only safe for a control that leaves its own screen: once it has, the tag is gone and the
 * remaining attempts find nothing to tap.
 */
@OptIn(ExperimentalTestApi::class)
private suspend fun ComposeUiTest.tapUntil(
    tag: String,
    arrived: () -> Boolean,
    scrollIntoView: () -> Unit,
) {
    repeat(NavigationAttempts) {
        if (arrived()) return
        // Scrolling has to come first: in a lazy list the control does not exist in the semantics
        // tree until it has been scrolled into it, so checking for it beforehand would find
        // nothing and never tap. It is allowed to fail because the screen it belongs to may have
        // already been left, or may not have finished loading it yet — both are states this loop
        // exists to wait through rather than to fail on.
        runCatching { scrollIntoView() }
        if (isClickable(tag)) {
            onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)
        }
        waitForIdle()
    }
    // Nothing arrived after several attempts, so let the ordinary wait produce the failure and its
    // message rather than throwing something less informative from here.
    waitUntil(timeoutMillis = JourneyTimeoutMillis) { arrived() }
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.isClickable(tag: String): Boolean =
    onAllNodesWithTag(tag).fetchSemanticsNodes().firstOrNull()
        ?.config
        ?.getOrElseNullable(SemanticsActions.OnClick) { null } != null

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.isDisplayingText(text: String): Boolean =
    onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.isDisplayingTag(tag: String): Boolean =
    onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

/**
 * A text the learner has to be able to read, on screen and inside the window.
 *
 * The taking screen is a lazy list, so the node has to be scrolled into composition before it can
 * be found at all. Long authored prose wraps, so it is matched by a leading snippet rather than by
 * the whole string, and then checked for containment: a stem, an option or an explanation wide
 * enough to exceed the window would push the control this journey clicks off screen.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.assertReadableWithin(text: String, rootWidth: Float) {
    val snippet = text.take(TextSnippetLength)
    onNode(hasScrollAction()).performScrollToNode(hasText(snippet, substring = true))
    val node = onAllNodesWithText(snippet, substring = true)[0]
    node.assertIsDisplayed()
    val width = node.fetchSemanticsNode().boundsInRoot.width
    assertTrue(width <= rootWidth, "\"$snippet\" was $width wide in a $rootWidth window.")
}

/** One answer row, scrolled into the lazy list and matched by the text it carries. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.answerRow(answerText: String): SemanticsNodeInteraction {
    val matcher = hasText(answerText.take(TextSnippetLength), substring = true) and hasClickAction()
    onNode(hasScrollAction()).performScrollToNode(matcher)
    return onNode(matcher)
}

/** The Unit whose practice pool carries the architecture bank's ADVANCED Questions. */
private val ArchitectureSynthesisUnit: LearningUnit by lazy {
    runBlocking {
        requireNotNull(BundledLearningContentRepository().getUnitById(ArchitectureSynthesisUnitId)) {
            "The bundled learning document no longer contains $ArchitectureSynthesisUnitId."
        }
    }
}

/**
 * The Questions a Unit's practice can reach, derived the way the resolver derives them.
 *
 * Primary concepts only, ACTIVE only, deduplicated — which is the production rule rather than a
 * copy of a pool. `LearningUnitPracticeIntegrationTest` owns the claim about *which* ids that
 * produces; this only needs the authored stems, keys and explanations to read from the screen.
 */
private fun architecturePracticeCandidates(unit: LearningUnit): List<Question> {
    val concepts = unit.lessons.flatMap { it.primarySubtopicIds }.toSet()
    return runBlocking { BundledCurriculumSource.load() }.questions
        .filter { it.status == ContentStatus.ACTIVE && it.subtopicId in concepts }
        .distinctBy { it.id }
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

/**
 * A desktop-shaped window, which is where the reading column is wide enough for a comparison to
 * render as a table rather than as the compact stack, and where this project is actually run.
 */
private val WideWindowWidth: Dp = 1100.dp
private val WideWindowHeight: Dp = 1000.dp

/** Enough columns that the table form cannot fit the reading measure at any supported width. */
private const val WideComparisonColumns = 4

/** Shipped identities, not fixtures: see the class comment. */
private const val ShippedUnitId = "unit_thinking_in_compose"
private const val UiTopicId = "android_ui"
private const val UiTopicName = "UI — Views & Jetpack Compose"
private const val ShippedUnitBuilderLabel = "Learning unit: Thinking in Compose"
private const val ArchitectureSynthesisUnitId = "unit_state_events_lifetime_and_selection"
