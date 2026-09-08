package org.artkachenko.kmp_learning_app.topic_study.learning_unit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.lesson_study.LearningUnitStudyProgress
import org.artkachenko.kmp_learning_app.lesson_study.LessonStudyProgress
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressSummary
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressUiState

@OptIn(ExperimentalTestApi::class)
internal class LearningUnitScreenTest {
    @Test
    fun loadingStateRenders() = runComposeUiTest {
        setContent {
            MaterialTheme {
                LearningUnitScreen(LearningUnitUiState.Loading, {}, {}, {}, {})
            }
        }

        onNodeWithTag(LearningUnitLoadingTag).assertIsDisplayed()
        onNodeWithText("Loading learning unit").assertIsDisplayed()
    }

    @Test
    fun notFoundStateSaysTheUnitIsUnavailableWithoutOfferingRetry() = runComposeUiTest {
        setContent {
            MaterialTheme {
                LearningUnitScreen(LearningUnitUiState.NotFound, {}, {}, {}, {})
            }
        }

        onNodeWithText("This learning unit is not available.").assertIsDisplayed()
        // Retrying an identity that names nothing current would only fail the same way.
        onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test
    fun errorStateRendersAndRetries() = runComposeUiTest {
        var retryCount = 0
        setContent {
            MaterialTheme {
                LearningUnitScreen(LearningUnitUiState.Error, {}, {}, {}, { retryCount += 1 })
            }
        }

        onNodeWithText("Learning content could not be loaded.").assertIsDisplayed()
        onNodeWithText("Retry").performClick()
        assertEquals(1, retryCount)
    }

    @Test
    fun contentRendersTheUnitAndItsLessonsAndBackIsAvailable() = runComposeUiTest {
        var backCount = 0
        setContent {
            MaterialTheme {
                LearningUnitScreen(
                    state = content(),
                    onBack = { backCount += 1 },
                    onLessonClick = {},
                    onPracticeUnit = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Thinking in Compose").assertIsDisplayed()
        onNodeWithText("Why Compose changes how UI is written.").assertIsDisplayed()
        onNodeWithText("Lessons").assertIsDisplayed()
        onNodeWithText("Declarative UI").assertIsDisplayed()
        onNodeWithText("Why declarative UI exists.").assertIsDisplayed()
        onNodeWithText("State down, events up").assertIsDisplayed()

        onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backCount)
    }

    @Test
    fun selectingALessonEmitsItsStableIdentity() = runComposeUiTest {
        val clicked = mutableListOf<String>()
        setContent {
            MaterialTheme {
                LearningUnitScreen(
                    state = content(),
                    onBack = {},
                    onLessonClick = { clicked += it },
                    onPracticeUnit = {},
                    onRetry = {},
                )
            }
        }

        // The row is addressed by its Lesson ID and emits that same ID: neither the row's position
        // nor its title is what leaves this screen.
        onNodeWithTag(learningLessonRowTag("lesson_state_down_events_up")).performClick()

        assertEquals(listOf("lesson_state_down_events_up"), clicked)
    }

    /**
     * Authored order is the reading order, so it has to be observable rather than assumed: the row
     * the state listed first has to be the row drawn first.
     */
    @Test
    fun lessonRowsAreLaidOutInTheOrderTheStateGaveThem() = runComposeUiTest {
        setContent {
            MaterialTheme {
                LearningUnitScreen(content(), {}, {}, {}, {})
            }
        }

        val first = onNodeWithTag(learningLessonRowTag("lesson_declarative_ui"))
            .fetchSemanticsNode().positionInRoot.y
        val second = onNodeWithTag(learningLessonRowTag("lesson_state_down_events_up"))
            .fetchSemanticsNode().positionInRoot.y

        assertTrue(first < second, "Authored order was not preserved: $first, $second")
    }

    @Test
    fun aUnitWithNoCurrentLessonsExplainsItselfWithoutInventingRows() = runComposeUiTest {
        setContent {
            MaterialTheme {
                LearningUnitScreen(
                    state = LearningUnitUiState.Content(
                        unitId = "unit_thinking_in_compose",
                        title = "Thinking in Compose",
                        summary = "Why Compose changes how UI is written.",
                        lessons = emptyList(),
                    ),
                    onBack = {},
                    onLessonClick = {},
                    onPracticeUnit = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Thinking in Compose").assertIsDisplayed()
        onNodeWithText("No lessons are currently available in this unit.").assertIsDisplayed()
        // The heading belongs to a list; with nothing to list it would be a dangling label.
        onNodeWithText("Lessons").assertDoesNotExist()
    }

    /**
     * Study flows into practice from the overview, and the action says what it practises: a visible
     * label rather than an icon, so the Material button's own semantics are what a screen reader
     * announces without a second custom description.
     */
    @Test
    fun theUnitOffersPracticeAndTheActionEmitsIt() = runComposeUiTest {
        var practiceCount = 0
        setContent {
            MaterialTheme {
                LearningUnitScreen(
                    state = content(),
                    onBack = {},
                    onLessonClick = {},
                    onPracticeUnit = { practiceCount += 1 },
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Practice this unit").assertIsDisplayed()
        onNodeWithTag(LearningUnitPracticeButtonTag).performClick()

        assertEquals(1, practiceCount)
    }

    /** The action is part of the Unit, not of the reading list, so it survives an empty one. */
    @Test
    fun practiceStaysAvailableOnAUnitWithNoCurrentLessons() = runComposeUiTest {
        setContent {
            MaterialTheme {
                LearningUnitScreen(
                    state = LearningUnitUiState.Content(
                        unitId = "unit_thinking_in_compose",
                        title = "Thinking in Compose",
                        summary = "Why Compose changes how UI is written.",
                        lessons = emptyList(),
                    ),
                    onBack = {},
                    onLessonClick = {},
                    onPracticeUnit = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithTag(LearningUnitPracticeButtonTag).assertIsDisplayed()
    }

    /** Nothing to practise on a screen that is not showing a Unit. */
    @Test
    fun practiceIsAbsentWhileTheUnitIsUnavailable() = runComposeUiTest {
        setContent {
            MaterialTheme {
                LearningUnitScreen(LearningUnitUiState.NotFound, {}, {}, {}, {})
            }
        }

        onNodeWithTag(LearningUnitPracticeButtonTag).assertDoesNotExist()
    }


    /**
     * Both values are stated in words on every row. An unstudied Lesson must not simply be blank:
     * blank is what an unknown study record looks like, and the two must not be confusable.
     */
    @Test
    fun everyLessonRowStatesWhetherItIsStudied() = runComposeUiTest {
        setContent {
            MaterialTheme {
                LearningUnitScreen(studyContent(available(studied = setOf("lesson_b"))), {}, {}, {}, {})
            }
        }

        // Asserted on the merged row, which is the node a screen reader announces: the state has
        // to reach the same tree the title and summary do.
        onNodeWithTag(learningLessonRowTag("lesson_a")).assert(hasText("Not studied"))
        onNodeWithTag(learningLessonRowTag("lesson_b")).assert(hasText("Studied"))
        onNodeWithTag(learningLessonRowTag("lesson_c")).assert(hasText("Not studied"))
    }

    /** Study progress annotates the authored order; it never re-orders, hides, or locks a row. */
    @Test
    fun studiedLessonsStayInAuthoredOrderAndEveryRowStaysClickable() = runComposeUiTest {
        val clicked = mutableListOf<String>()
        setContent {
            MaterialTheme {
                LearningUnitScreen(
                    state = studyContent(available(studied = setOf("lesson_c"))),
                    onBack = {},
                    onLessonClick = { clicked += it },
                    onPracticeUnit = {},
                    onRetry = {},
                )
            }
        }

        val positions = StudyLessonIds.map {
            onNodeWithTag(learningLessonRowTag(it)).fetchSemanticsNode().positionInRoot.y
        }
        assertTrue(positions == positions.sorted(), "Authored order was not preserved: $positions")

        // The studied Lesson and the unstudied ones are all ordinary navigation targets.
        onNodeWithTag(learningLessonRowTag("lesson_c")).performClick()
        onNodeWithTag(learningLessonRowTag("lesson_a")).performClick()
        assertEquals(listOf("lesson_c", "lesson_a"), clicked)
    }

    @Test
    fun partialUnitProgressRendersTheCountAndItsMeter() = runComposeUiTest {
        setContent {
            MaterialTheme {
                LearningUnitScreen(studyContent(available(studied = setOf("lesson_b"))), {}, {}, {}, {})
            }
        }

        onNodeWithTag(LearningUnitStudyProgressTag).assertIsDisplayed()
        onNodeWithText("1 of 3 lessons studied").assertIsDisplayed()
    }

    @Test
    fun aFullyStudiedUnitSaysSoWithoutMasteryLanguage() = runComposeUiTest {
        setContent {
            MaterialTheme {
                LearningUnitScreen(
                    state = studyContent(
                        available(studied = setOf("lesson_a", "lesson_b", "lesson_c")),
                    ),
                    onBack = {},
                    onLessonClick = {},
                    onPracticeUnit = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("3 of 3 lessons studied").assertIsDisplayed()
        onNodeWithText("Mastered").assertDoesNotExist()
    }

    /** `Empty` has no fraction: 0 / 0 reads as finished and 0% claims work that does not exist. */
    @Test
    fun anEmptyUnitIsNeverRenderedAsZeroOrOneHundredPercent() = runComposeUiTest {
        setContent {
            MaterialTheme {
                LearningUnitScreen(
                    state = LearningUnitUiState.Content(
                        unitId = "unit_thinking_in_compose",
                        title = "Thinking in Compose",
                        summary = "Why Compose changes how UI is written.",
                        lessons = emptyList(),
                        studyProgress = StudyProgressUiState.Available(
                            LearningUnitStudyProgress(
                                unitId = "unit_thinking_in_compose",
                                lessons = emptyList(),
                                summary = StudyProgressSummary.Empty,
                            ),
                        ),
                    ),
                    onBack = {},
                    onLessonClick = {},
                    onPracticeUnit = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithTag(LearningUnitStudyProgressTag).assertDoesNotExist()
        onNodeWithText("0%").assertDoesNotExist()
        onNodeWithText("100%").assertDoesNotExist()
        onNodeWithText("0 of 0 lessons studied").assertDoesNotExist()
        // The existing explanation is the honest one and is unchanged.
        onNodeWithText("No lessons are currently available in this unit.").assertIsDisplayed()
    }

    /** A record still being read says nothing: not an aggregate, and not a row label. */
    @Test
    fun aLoadingStudyRecordRendersNoFalseZeroAndNoRowLabels() = runComposeUiTest {
        setContent {
            MaterialTheme {
                LearningUnitScreen(studyContent(StudyProgressUiState.Loading), {}, {}, {}, {})
            }
        }

        onNodeWithTag(LearningUnitStudyProgressTag).assertDoesNotExist()
        onNodeWithText("0 of 3 lessons studied").assertDoesNotExist()
        onNodeWithText("Not studied").assertDoesNotExist()
        onNodeWithText("Studied").assertDoesNotExist()
    }

    /**
     * A study-record failure costs the indicators and nothing else: the Unit is not an Error, the
     * rows are all still there and still clickable, and practice is untouched.
     */
    @Test
    fun anUnavailableStudyRecordLeavesEveryLessonAndPracticeUsable() = runComposeUiTest {
        val clicked = mutableListOf<String>()
        var practiceCount = 0
        setContent {
            MaterialTheme {
                LearningUnitScreen(
                    state = studyContent(StudyProgressUiState.Unavailable),
                    onBack = {},
                    onLessonClick = { clicked += it },
                    onPracticeUnit = { practiceCount += 1 },
                    onRetry = {},
                )
            }
        }

        onNodeWithTag(LearningUnitStudyUnavailableTag).assertIsDisplayed()
        onNodeWithText("Study progress unavailable").assertIsDisplayed()
        // Never every Lesson shown as unstudied.
        onNodeWithText("Not studied").assertDoesNotExist()
        onNodeWithTag(LearningUnitStudyProgressTag).assertDoesNotExist()
        onNodeWithText("Retry").assertDoesNotExist()

        onNodeWithTag(learningLessonRowTag("lesson_b")).performClick()
        assertEquals(listOf("lesson_b"), clicked)
        onNodeWithTag(LearningUnitPracticeButtonTag).performClick()
        assertEquals(1, practiceCount)
    }

    /** The phone-shaped contract: the aggregate, the rows, and practice all fit and stay usable. */
    @Test
    fun theUnitStaysUsableAtANarrowWidth() = runComposeUiTest {
        setContent {
            MaterialTheme {
                Box(Modifier.size(NarrowWidth, NarrowHeight)) {
                    LearningUnitScreen(
                        state = studyContent(available(studied = setOf("lesson_b"))),
                        onBack = {},
                        onLessonClick = {},
                        onPracticeUnit = {},
                        onRetry = {},
                    )
                }
            }
        }

        onNodeWithTag(LearningUnitPracticeButtonTag).assertIsDisplayed()
        onNodeWithText("1 of 3 lessons studied").assertIsDisplayed()
        onNodeWithTag(learningLessonRowTag("lesson_a")).performScrollTo().assertIsDisplayed()
        onNodeWithTag(learningLessonRowTag("lesson_c"))
            .performScrollTo()
            .assertIsDisplayed()
            .assert(hasText("Not studied"))
    }

    private fun available(studied: Set<String>): StudyProgressUiState<LearningUnitStudyProgress> =
        StudyProgressUiState.Available(
            LearningUnitStudyProgress(
                unitId = "unit_thinking_in_compose",
                lessons = listOf("lesson_a", "lesson_b", "lesson_c").map {
                    LessonStudyProgress(lessonId = it, isStudied = it in studied)
                },
                summary = StudyProgressSummary.Progress(
                    studiedCount = studied.size,
                    totalCount = 3,
                ),
            ),
        )

    private fun content(): LearningUnitUiState.Content =
        LearningUnitUiState.Content(
            unitId = "unit_thinking_in_compose",
            title = "Thinking in Compose",
            summary = "Why Compose changes how UI is written.",
            lessons = listOf(
                LearningLessonItemUiModel(
                    lessonId = "lesson_declarative_ui",
                    title = "Declarative UI",
                    summary = "Why declarative UI exists.",
                ),
                LearningLessonItemUiModel(
                    lessonId = "lesson_state_down_events_up",
                    title = "State down, events up",
                    summary = "How state and events flow.",
                ),
            ),
        )

    /** Three Lessons, so a partially studied Unit has a fraction that is neither 0 nor complete. */
    private fun studyContent(
        studyProgress: StudyProgressUiState<LearningUnitStudyProgress>,
    ): LearningUnitUiState.Content =
        LearningUnitUiState.Content(
            unitId = "unit_thinking_in_compose",
            title = "Thinking in Compose",
            summary = "Why Compose changes how UI is written.",
            lessons = StudyLessonIds.map {
                LearningLessonItemUiModel(
                    lessonId = it,
                    title = "Title of $it",
                    summary = "Summary of $it",
                )
            },
            studyProgress = studyProgress,
        )
}

private val StudyLessonIds = listOf("lesson_a", "lesson_b", "lesson_c")

private val NarrowWidth = 360.dp
private val NarrowHeight = 640.dp
