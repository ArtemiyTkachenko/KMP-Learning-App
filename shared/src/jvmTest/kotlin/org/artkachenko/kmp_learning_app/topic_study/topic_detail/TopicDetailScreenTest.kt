package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset
import org.artkachenko.kmp_learning_app.lesson_study.LearningUnitStudyProgress
import org.artkachenko.kmp_learning_app.lesson_study.LessonStudyProgress
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressSummary
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressUiState
import org.artkachenko.kmp_learning_app.lesson_study.TopicStudyProgress
import org.artkachenko.kmp_learning_app.ui.LearningContextUiModel

/**
 * Topic Detail as three tabbed pages.
 *
 * Every practice-intent assertion here is unchanged by the tabs: the same callbacks still carry the
 * same scopes and the same `PracticePreset`s. What changed is only where a control is reachable
 * from, so a test that used to read the whole column now selects the page it is asking about first.
 */
@OptIn(ExperimentalTestApi::class)
internal class TopicDetailScreenTest {

    // ---------------------------------------------------------------- tabs

    /** A Topic opens on the material, not on a control: Study is the first page. */
    @Test
    fun aLoadedTopicOpensOnStudyWithAllThreeTabsAvailable() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        subtopics = listOf(subtopicItem("subtopic_a", "Subtopic A")),
                        learningUnits = TopicLearningUnitsUiState.Available(
                            listOf(learningUnitItem("unit_a", "Thinking in Compose", lessons = 3)),
                        ),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        // All three capabilities are discoverable without scrolling anything.
        onNodeWithTag(TopicStudyTabTag).assertIsDisplayed().assertIsSelected()
        onNodeWithTag(TopicPracticeTabTag).assertIsDisplayed().assertIsNotSelected()
        onNodeWithTag(TopicSubtopicsTabTag).assertIsDisplayed().assertIsNotSelected()
        // Study is what is actually on screen; the other pages are not composed behind it.
        onNodeWithText("Thinking in Compose").assertIsDisplayed()
        onNodeWithTag(TopicPracticeButtonTag).assertDoesNotExist()
        onNodeWithTag(SubtopicPracticeButtonTag).assertDoesNotExist()
    }

    /**
     * Selection and page position are one value, so a selected tab and the content underneath it
     * cannot disagree. Asserted through the standard `Tab` selection semantics rather than by
     * sampling an animation frame.
     */
    @Test
    fun eachTabSelectsItsOwnPageAndTheSelectionFollows() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        subtopics = listOf(subtopicItem("subtopic_a", "Subtopic A")),
                        learningUnits = TopicLearningUnitsUiState.Available(
                            listOf(learningUnitItem("unit_a", "Thinking in Compose", lessons = 3)),
                        ),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        selectTab(TopicPracticeTabTag)
        onNodeWithTag(TopicPracticeTabTag).assertIsSelected()
        onNodeWithTag(TopicStudyTabTag).assertIsNotSelected()
        onNodeWithTag(TopicPracticeButtonTag).assertIsDisplayed()
        onNodeWithText("Thinking in Compose").assertDoesNotExist()

        selectTab(TopicSubtopicsTabTag)
        onNodeWithTag(TopicSubtopicsTabTag).assertIsSelected()
        onNodeWithText("Subtopic A").assertIsDisplayed()
        onNodeWithTag(TopicPracticeButtonTag).assertDoesNotExist()

        selectTab(TopicStudyTabTag)
        onNodeWithTag(TopicStudyTabTag).assertIsSelected()
        onNodeWithText("Thinking in Compose").assertIsDisplayed()
    }

    /**
     * The tabs describe one loaded Topic's capabilities. A Topic that failed to load has none to
     * describe, so the terminal states stay whole-screen and never appear inside a page.
     */
    @Test
    fun terminalTopicStatesShowNoTabs() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.NotFound,
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Topic not available").assertIsDisplayed()
        onNodeWithTag(TopicStudyTabTag).assertDoesNotExist()
        onNodeWithTag(TopicPracticeTabTag).assertDoesNotExist()
        onNodeWithTag(TopicSubtopicsTabTag).assertDoesNotExist()
    }

    @Test
    fun loadingAndErrorStatesRenderActions() = runComposeUiTest {
        var retryCount = 0
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Error,
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = { retryCount += 1 },
                )
            }
        }
        onNodeWithText("Topics could not be loaded").assertIsDisplayed()
        onNodeWithTag(TopicStudyTabTag).assertDoesNotExist()
        onNodeWithText("Retry").performClick()
        assertEquals(1, retryCount)

        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Loading,
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }
        onNodeWithTag(TopicDetailLoadingTag).assertIsDisplayed()
        onNodeWithTag(TopicStudyTabTag).assertDoesNotExist()
    }

    /**
     * Each page owns its scroll position, so looking something up on another tab does not cost the
     * learner their place in a long Subtopic list.
     */
    @Test
    fun switchingPagesKeepsEachPagesOwnScrollPosition() = runComposeUiTest {
        val subtopics = (1..20).map { subtopicItem("subtopic_$it", "Subtopic $it") }
        setContent {
            MaterialTheme {
                Box(Modifier.size(400.dp, 400.dp)) {
                    TopicDetailScreen(
                        state = topicContent(subtopics = subtopics),
                        onBack = {},
                        onStartTopicPractice = {},
                        onStartSubtopicPractice = {},
                        onPracticePreset = {},
                        onRetry = {},
                    )
                }
            }
        }

        selectTab(TopicSubtopicsTabTag)
        onNodeWithTag(TopicSubtopicsListTag).performScrollToIndex(19)
        waitForIdle()
        onNodeWithText("Subtopic 20").assertIsDisplayed()
        onNodeWithText("Subtopic 1").assertDoesNotExist()

        selectTab(TopicPracticeTabTag)
        selectTab(TopicSubtopicsTabTag)

        // Back where it was, not rebuilt from the top.
        onNodeWithText("Subtopic 20").assertIsDisplayed()
        onNodeWithText("Subtopic 1").assertDoesNotExist()
    }

    // ------------------------------------------------- targeted subtopic arrival

    /**
     * Arriving with a Subtopic in hand opens the page that Subtopic is on and travels to it. No
     * fixed header offset is involved any more: the Subtopics list holds Subtopics and nothing
     * else, so the row's index in the state is the index in the list.
     */
    @Test
    fun targetSubtopicOpensSubtopicsAndBringsTheRowIntoView() = runComposeUiTest {
        val subtopics = (1..16).map { index ->
            subtopicItem(
                id = "subtopic_$index",
                name = "Subtopic $index",
                learningContext = learningContext(1, 1, accuracy = 50.0),
            )
        }
        setContent {
            MaterialTheme {
                Box(Modifier.size(400.dp, 360.dp)) {
                    TopicDetailScreen(
                        state = topicContent(
                            subtopics = subtopics,
                            learningContext = learningContext(8, 16, accuracy = 50.0),
                        ),
                        targetSubtopicId = "subtopic_15",
                        onBack = {},
                        onStartTopicPractice = {},
                        onStartSubtopicPractice = {},
                        onPracticePreset = {},
                        onRetry = {},
                    )
                }
            }
        }

        onNodeWithTag(TopicSubtopicsTabTag).assertIsSelected()
        onNodeWithText("Subtopic 15").assertIsDisplayed()
    }

    /** A retired or renamed target is not an error: the page opens and stays usable. */
    @Test
    fun missingTargetSubtopicKeepsTheSubtopicsPageUsable() = runComposeUiTest {
        var started: String? = null
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        subtopics = listOf(subtopicItem("subtopic_a", "Subtopic A")),
                    ),
                    targetSubtopicId = "retired_subtopic",
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = { started = it },
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithTag(TopicSubtopicsTabTag).assertIsSelected()
        onNodeWithText("Subtopic A").assertIsDisplayed()
        onNodeWithTag(SubtopicPracticeButtonTag).performClick()
        assertEquals("subtopic_a", started)

        // And the rest of the Topic is one tap away, exactly as on an ordinary opening.
        selectTab(TopicPracticeTabTag)
        onNodeWithTag(TopicPracticeButtonTag).assertIsDisplayed()
    }

    /** An ordinary opening carries no target and opens on Study. */
    @Test
    fun noTargetSubtopicOpensOnStudy() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        subtopics = listOf(subtopicItem("subtopic_a", "Subtopic A")),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithTag(TopicStudyTabTag).assertIsSelected()
        onNodeWithText("Subtopic A").assertDoesNotExist()
    }

    // ------------------------------------------------------------- study page

    /**
     * Study-only: the correction E21-02 exists for. Learning material must survive a Topic that has
     * no assessment questions at all, and the Practice page must say so rather than take the Topic
     * down with it.
     */
    @Test
    fun aStudyOnlyTopicRendersItsUnitsAndNoPracticeActions() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Content(
                        topic = Topic("topic_a", "Topic A"),
                        topicQuestionCount = 0,
                        subtopics = emptyList(),
                        learningUnits = TopicLearningUnitsUiState.Available(
                            listOf(learningUnitItem("unit_a", "Thinking in Compose", lessons = 3)),
                        ),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Thinking in Compose").assertIsDisplayed()
        onNodeWithText("Summary for unit_a").assertIsDisplayed()
        onNodeWithText("3 lessons").assertIsDisplayed()
        // Nothing on this surface claims the learner has or has not read anything.
        onAllNodesWithText("Not started").assertCountEquals(0)

        selectTab(TopicPracticeTabTag)
        onNodeWithText("No practice questions are currently available.").assertIsDisplayed()
        onNodeWithTag(TopicPracticeButtonTag).assertDoesNotExist()
        onAllNodesWithText("Start Practice").assertCountEquals(0)
    }

    /**
     * A Topic with no authored Units is an ordinary Topic, not a broken one — but a blank tab reads
     * as broken, so the page says what it has rather than rendering nothing.
     */
    @Test
    fun aTopicWithNoAuthoredUnitsShowsAnEmptyStudyPageAndKeepsItsOtherTabs() = runComposeUiTest {
        var topicStarts = 0
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        subtopics = listOf(subtopicItem("subtopic_a", "Subtopic A")),
                        learningUnits = TopicLearningUnitsUiState.Available(emptyList()),
                    ),
                    onBack = {},
                    onStartTopicPractice = { topicStarts += 1 },
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("No learning material for this topic yet.").assertIsDisplayed()
        onAllNodesWithText("Learning material could not be loaded.").assertCountEquals(0)

        selectTab(TopicPracticeTabTag)
        onNodeWithTag(TopicPracticeButtonTag).performClick()
        assertEquals(1, topicStarts)

        selectTab(TopicSubtopicsTabTag)
        onNodeWithText("Subtopic A").assertIsDisplayed()
    }

    /**
     * A learning-content failure says the material could not be read rather than that there is
     * none, and it takes no practice control with it.
     */
    @Test
    fun anUnreadableLearningDocumentDoesNotHidePracticeControls() = runComposeUiTest {
        var topicStarts = 0
        var subtopicStarts: String? = null
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Content(
                        topic = Topic("topic_a", "Topic A"),
                        topicQuestionCount = 3,
                        subtopics = listOf(subtopicItem("subtopic_a", "Subtopic A", count = 2)),
                        learningUnits = TopicLearningUnitsUiState.Unavailable,
                    ),
                    onBack = {},
                    onStartTopicPractice = { topicStarts += 1 },
                    onStartSubtopicPractice = { subtopicStarts = it },
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Learning material could not be loaded.").assertIsDisplayed()
        // Not the screen-level curriculum error, and no Retry that would imply the Topic failed.
        onAllNodesWithText("Topics could not be loaded").assertCountEquals(0)
        onAllNodesWithText("Retry").assertCountEquals(0)

        selectTab(TopicPracticeTabTag)
        onNodeWithTag(TopicPracticeButtonTag).performClick()
        selectTab(TopicSubtopicsTabTag)
        onNodeWithTag(SubtopicPracticeButtonTag).performClick()
        assertEquals(1, topicStarts)
        assertEquals("subtopic_a", subtopicStarts)
    }

    /** Learning content that has not resolved yet says nothing at all, and blocks nothing. */
    @Test
    fun stillLoadingLearningContentLeavesTheTopicFullyUsable() = runComposeUiTest {
        var topicStarts = 0
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(learningUnits = TopicLearningUnitsUiState.Loading),
                    onBack = {},
                    onStartTopicPractice = { topicStarts += 1 },
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        // Silent, not empty: an unresolved read must not claim the Topic has nothing to read.
        onAllNodesWithText("No learning material for this topic yet.").assertCountEquals(0)
        onAllNodesWithText("Learning material could not be loaded.").assertCountEquals(0)

        selectTab(TopicPracticeTabTag)
        onNodeWithTag(TopicPracticeButtonTag).performClick()
        assertEquals(1, topicStarts)
    }

    /** The handoff the shell supplies navigation for: the row emits the stable Unit ID only. */
    @Test
    fun aSelectedUnitEmitsItsStableIdOnly() = runComposeUiTest {
        val selected = mutableListOf<String>()
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        learningUnits = TopicLearningUnitsUiState.Available(
                            listOf(
                                learningUnitItem("unit_b", "Unit B", lessons = 2),
                                learningUnitItem("unit_a", "Unit A", lessons = 1),
                            ),
                        ),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                    onLearningUnitClick = selected::add,
                )
            }
        }

        onNodeWithTag(learningUnitCardTag("unit_b")).assertHasClickAction().performClick()
        assertEquals(listOf("unit_b"), selected)
    }

    /**
     * With no handler the row is study content, not a control. A clickable row that leads nowhere
     * would be worse than no affordance at all.
     */
    @Test
    fun aUnitRowWithNoHandlerAdvertisesNoClickAction() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        learningUnits = TopicLearningUnitsUiState.Available(
                            listOf(learningUnitItem("unit_a", "Unit A", lessons = 2)),
                        ),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithTag(learningUnitCardTag("unit_a")).assertHasNoClickAction()
        onNodeWithText("Unit A").assertIsDisplayed()
        onNodeWithText("2 lessons").assertIsDisplayed()
    }

    /** Authored order is preserved on screen, not just in the state that feeds it. */
    @Test
    fun unitsRenderInTheOrderTheStateCarries() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        learningUnits = TopicLearningUnitsUiState.Available(
                            listOf(
                                learningUnitItem("unit_b", "Unit B", lessons = 2),
                                learningUnitItem("unit_a", "Unit A", lessons = 1),
                            ),
                        ),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        val first = onNodeWithText("Unit B").fetchSemanticsNode().positionInRoot.y
        val second = onNodeWithText("Unit A").fetchSemanticsNode().positionInRoot.y
        assertTrue(first < second, "Authored order must survive rendering.")
    }

    /** One lesson, so the plural resource has to select the singular form. */
    @Test
    fun aSingleLessonUnitReadsAsOneLesson() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        learningUnits = TopicLearningUnitsUiState.Available(
                            listOf(learningUnitItem("unit_a", "Unit A", lessons = 1)),
                        ),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("1 lesson").assertIsDisplayed()
    }

    /**
     * Each Unit row shows the learner's own progress through it, joined by stable Unit ID, and the
     * Units keep their authored order while carrying different counts.
     */
    @Test
    fun eachUnitRowShowsItsCurrentStudyProgress() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        learningUnits = TopicLearningUnitsUiState.Available(
                            listOf(
                                learningUnitItem("unit_a", "Thinking in Compose", lessons = 3),
                                learningUnitItem("unit_b", "State in Compose", lessons = 1),
                            ),
                        ),
                        studyProgress = topicStudyProgress(
                            Triple("unit_a", 1, 3),
                            Triple("unit_b", 1, 1),
                        ),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                    onLearningUnitClick = {},
                )
            }
        }

        onNodeWithTag(learningUnitCardTag("unit_a")).assert(hasText("1 of 3 lessons studied"))
        // The plural resource selects the singular form from the total, not from the numerator.
        onNodeWithTag(learningUnitCardTag("unit_b")).assert(hasText("1 of 1 lesson studied"))
        // The studied line replaces the authored count rather than repeating the total beside it.
        onNodeWithText("3 lessons").assertDoesNotExist()
    }

    /**
     * A study-record failure costs the figures and nothing else: the authored Units are still shown
     * and still clickable, practice is untouched, and no row claims a count nobody could read.
     */
    @Test
    fun anUnavailableStudyRecordKeepsUnitsAndPracticeWithoutFabricatingCounts() = runComposeUiTest {
        val selected = mutableListOf<String>()
        var practiceCount = 0
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        subtopics = listOf(subtopicItem("subtopic_a", "Subtopic A", count = 10)),
                        learningUnits = TopicLearningUnitsUiState.Available(
                            listOf(learningUnitItem("unit_a", "Thinking in Compose", lessons = 3)),
                        ),
                        studyProgress = StudyProgressUiState.Unavailable,
                    ),
                    onBack = {},
                    onStartTopicPractice = { practiceCount += 1 },
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                    onLearningUnitClick = selected::add,
                )
            }
        }

        // Said once for the page rather than repeated inside every row.
        onNodeWithTag(TopicStudyUnavailableTag).assertIsDisplayed()
        onNodeWithText("Study progress unavailable").assertIsDisplayed()
        onNodeWithText("0 of 3 lessons studied").assertDoesNotExist()
        // The authored count is the truthful fallback, and the Unit is still an ordinary target.
        onNodeWithTag(learningUnitCardTag("unit_a")).assert(hasText("3 lessons"))
        onNodeWithTag(learningUnitCardTag("unit_a")).performClick()
        assertEquals(listOf("unit_a"), selected)
        // Learning availability is a different question and is unchanged by this.
        onNodeWithText("Learning material could not be loaded.").assertDoesNotExist()

        selectTab(TopicPracticeTabTag)
        onNodeWithTag(TopicPracticeButtonTag).performClick()
        assertEquals(1, practiceCount)
    }

    /** While the record is still being read the page says nothing and stays fully practiceable. */
    @Test
    fun aLoadingStudyRecordKeepsUnitsVisibleWithoutAZeroCount() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        learningUnits = TopicLearningUnitsUiState.Available(
                            listOf(learningUnitItem("unit_a", "Thinking in Compose", lessons = 3)),
                        ),
                        studyProgress = StudyProgressUiState.Loading,
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                    onLearningUnitClick = {},
                )
            }
        }

        onNodeWithText("Thinking in Compose").assertIsDisplayed()
        onNodeWithTag(learningUnitCardTag("unit_a")).assert(hasText("3 lessons"))
        onNodeWithText("0 of 3 lessons studied").assertDoesNotExist()
        onNodeWithTag(TopicStudyUnavailableTag).assertDoesNotExist()

        selectTab(TopicPracticeTabTag)
        onNodeWithTag(TopicPracticeButtonTag).assertIsDisplayed()
    }

    /**
     * A Unit whose Lessons have all been retired has no fraction to show. Its authored count stays
     * truthful rather than becoming "0 of 0 studied" — which would read as finished.
     */
    @Test
    fun aUnitWithNoActiveLessonsKeepsItsAuthoredCountRatherThanAnEmptyFraction() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        learningUnits = TopicLearningUnitsUiState.Available(
                            listOf(learningUnitItem("unit_a", "Retired Unit", lessons = 0)),
                        ),
                        studyProgress = topicStudyProgress(Triple("unit_a", 0, 0)),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                    onLearningUnitClick = {},
                )
            }
        }

        onNodeWithText("0 of 0 lessons studied").assertDoesNotExist()
        onNodeWithText("100%").assertDoesNotExist()
        onNodeWithTag(learningUnitCardTag("unit_a")).assert(hasText("0 lessons"))
    }

    /**
     * Study progress is not assessment coverage. Both belong to this Topic, they now live on
     * different pages, and neither figure is derived from or replaced by the other.
     */
    @Test
    fun studyProgressAndAssessmentCoverageStaySeparateMeasures() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        learningUnits = TopicLearningUnitsUiState.Available(
                            listOf(learningUnitItem("unit_a", "Thinking in Compose", lessons = 3)),
                        ),
                        studyProgress = topicStudyProgress(Triple("unit_a", 3, 3)),
                        learningContext = learningContext(12, 26, accuracy = 76.0),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        // Twice, and legitimately: this Topic has a single Unit, so the Topic aggregate and that
        // Unit's own figure are the same sentence. The header is asserted by its tag as well, so
        // the count below cannot be satisfied by one surface rendering it twice.
        onNodeWithTag(TopicStudyProgressTag).assertIsDisplayed()
        onAllNodesWithText("3 of 3 lessons studied").assertCountEquals(2)
        onNodeWithText("Mastered").assertDoesNotExist()

        // Every Lesson studied, and the Topic's assessment coverage is untouched by that.
        selectTab(TopicPracticeTabTag)
        onNodeWithText("12 of 26 questions explored").assertIsDisplayed()
        onNodeWithText("76%").assertIsDisplayed()
    }


    // ------------------------------------------------ topic study progress header

    /**
     * The Study page opens with one figure saying how far through the Topic's material the learner
     * is. It is lesson-weighted across the Units, so it is not the first Unit's fraction repeated.
     */
    @Test
    fun theStudyPageLeadsWithTheTopicsOwnLessonProgress() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        learningUnits = TopicLearningUnitsUiState.Available(
                            listOf(
                                learningUnitItem("unit_a", "Thinking in Compose", lessons = 3),
                                learningUnitItem("unit_b", "State in Compose", lessons = 2),
                            ),
                        ),
                        studyProgress = topicStudyProgress(
                            Triple("unit_a", 1, 3),
                            Triple("unit_b", 1, 2),
                        ),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                    onLearningUnitClick = {},
                )
            }
        }

        onNodeWithTag(TopicStudyProgressTag).assertIsDisplayed()
        onNodeWithText("2 of 5 lessons studied").assertIsDisplayed()
        // And each Unit still carries its own, which the aggregate does not replace.
        onNodeWithTag(learningUnitCardTag("unit_a")).assert(hasText("1 of 3 lessons studied"))
        onNodeWithTag(learningUnitCardTag("unit_b")).assert(hasText("1 of 2 lessons studied"))
    }

    /**
     * A record that has not been read is not a record of nothing: drawing a meter at zero would be a
     * claim about the learner made before anything was known about them.
     */
    @Test
    fun aLoadingStudyRecordShowsNoTopicProgressFigure() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        learningUnits = TopicLearningUnitsUiState.Available(
                            listOf(learningUnitItem("unit_a", "Thinking in Compose", lessons = 3)),
                        ),
                        studyProgress = StudyProgressUiState.Loading,
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithTag(TopicStudyProgressTag).assertDoesNotExist()
        onNodeWithTag(TopicStudyUnavailableTag).assertDoesNotExist()
        onNodeWithText("Thinking in Compose").assertIsDisplayed()
    }

    /**
     * A Topic whose Units hold no current Lessons has no fraction to report. "0 of 0" renders as
     * finished, so the header says nothing rather than claiming the Topic is complete.
     */
    @Test
    fun aTopicWithNoCurrentLessonsShowsNoProgressFigure() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        learningUnits = TopicLearningUnitsUiState.Available(
                            listOf(learningUnitItem("unit_a", "Retired Unit", lessons = 0)),
                        ),
                        studyProgress = topicStudyProgress(Triple("unit_a", 0, 0)),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                    onLearningUnitClick = {},
                )
            }
        }

        onNodeWithTag(TopicStudyProgressTag).assertDoesNotExist()
        onNodeWithText("100%").assertDoesNotExist()
        onNodeWithTag(learningUnitCardTag("unit_a")).assert(hasText("0 lessons"))
    }

    // ------------------------------------------------- unseen practice shortcut

    /**
     * With nothing attempted, unseen practice and ordinary practice draw from one identical pool, so
     * offering both is two controls for one outcome. It is not offered at either scope.
     */
    @Test
    fun anUntouchedTopicAndSubtopicOfferNoUnseenShortcut() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Content(
                        topic = Topic("topic_a", "Topic A"),
                        topicQuestionCount = 28,
                        subtopics = listOf(
                            subtopicItem(
                                id = "subtopic_a",
                                name = "Subtopic A",
                                count = 10,
                                learningContext = learningContext(0, 10),
                            ),
                        ),
                        learningContext = learningContext(0, 28),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        selectTab(TopicPracticeTabTag)
        onNodeWithTag(TopicUnseenPracticeTag).assertDoesNotExist()
        // Ordinary practice is untouched: it is the one way in, which is the point.
        onNodeWithTag(TopicPracticeButtonTag).assertIsDisplayed()

        selectTab(TopicSubtopicsTabTag)
        onNodeWithTag(subtopicUnseenPracticeTag("subtopic_a")).assertDoesNotExist()
        onNodeWithTag(SubtopicPracticeButtonTag).assertIsDisplayed()
    }

    /**
     * Once some of a scope has been met the shortcut narrows to a real remainder, and says how big
     * it is from the same two counts the surface is already displaying.
     */
    @Test
    fun aPartlyCoveredScopeOffersACountedUnseenShortcut() = runComposeUiTest {
        val presets = mutableListOf<PracticePreset>()
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Content(
                        topic = Topic("topic_a", "Topic A"),
                        topicQuestionCount = 28,
                        subtopics = listOf(
                            subtopicItem(
                                id = "subtopic_a",
                                name = "Subtopic A",
                                count = 10,
                                learningContext = learningContext(9, 10, accuracy = 60.0),
                            ),
                        ),
                        learningContext = learningContext(12, 28, accuracy = 70.0),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = presets::add,
                    onRetry = {},
                )
            }
        }

        selectTab(TopicPracticeTabTag)
        onNodeWithText("Practice 16 unseen questions").assertIsDisplayed()
        onNodeWithTag(TopicUnseenPracticeTag).performClick()

        selectTab(TopicSubtopicsTabTag)
        // One left, so the plural resource has to select the singular form.
        onNodeWithText("Practice 1 unseen question").assertIsDisplayed()
        onNodeWithTag(subtopicUnseenPracticeTag("subtopic_a")).performClick()

        // Narrowing when it is offered changed nothing about what it emits.
        assertEquals(
            listOf(
                PracticePreset(AssessmentScope.Topic("topic_a"), PracticeQuestionSource.UNSEEN),
                PracticePreset(
                    AssessmentScope.Subtopic("subtopic_a"),
                    PracticeQuestionSource.UNSEEN,
                ),
            ),
            presets,
        )
    }

    /**
     * The coverage rule governs the unseen shortcut only. A weak verdict is the domain's, and it
     * still earns its shortcut whatever the coverage counts beside it say.
     */
    @Test
    fun theWeakShortcutIsUnaffectedByTheCoverageRule() = runComposeUiTest {
        val presets = mutableListOf<PracticePreset>()
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        learningContext = learningContext(0, 10, isWeak = true),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = presets::add,
                    onRetry = {},
                )
            }
        }

        selectTab(TopicPracticeTabTag)
        onNodeWithTag(TopicUnseenPracticeTag).assertDoesNotExist()
        onNodeWithTag(TopicWeakPracticeTag).assertIsDisplayed().performClick()

        assertEquals(
            listOf(
                PracticePreset(
                    AssessmentScope.Topic("topic_a"),
                    PracticeQuestionSource.WEAK_AREAS,
                ),
            ),
            presets,
        )
    }

    // ----------------------------------------------------------- empty tab exits

    /** An empty Study page names the capability the Topic does have, and goes there. */
    @Test
    fun anEmptyStudyPageLeadsToPractice() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        learningUnits = TopicLearningUnitsUiState.Available(emptyList()),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("No learning material for this topic yet.").assertIsDisplayed()
        onNodeWithText("Practice this topic").performClick()
        waitForIdle()

        onNodeWithTag(TopicPracticeTabTag).assertIsSelected()
        onNodeWithTag(TopicPracticeButtonTag).assertIsDisplayed()
    }

    @Test
    fun anEmptySubtopicsPageLeadsToPractice() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(subtopics = emptyList()),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        selectTab(TopicSubtopicsTabTag)
        onNodeWithText("This topic has no subtopics.").assertIsDisplayed()
        onNodeWithText("Practice this topic").performClick()
        waitForIdle()

        onNodeWithTag(TopicPracticeTabTag).assertIsSelected()
        onNodeWithTag(TopicPracticeButtonTag).assertIsDisplayed()
    }

    /**
     * A failed read is not an empty one. It keeps the plain message: offering practice as the answer
     * to "the material could not be loaded" would read as a consolation for the wrong problem.
     */
    @Test
    fun anUnreadableStudyPageOffersNoWayForward() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        learningUnits = TopicLearningUnitsUiState.Unavailable,
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Learning material could not be loaded.").assertIsDisplayed()
        onNodeWithText("Practice this topic").assertDoesNotExist()
    }

    // ---------------------------------------------------------- practice page

    @Test
    fun theTopicPracticeActionStartsOrdinaryTopicPractice() = runComposeUiTest {
        var topicStarts = 0
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Content(
                        topic = Topic("topic_a", "Topic A"),
                        topicQuestionCount = 3,
                        subtopics = listOf(subtopicItem("subtopic_a", "Subtopic A", count = 2)),
                    ),
                    onBack = {},
                    onStartTopicPractice = { topicStarts += 1 },
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Topic A").assertIsDisplayed()

        selectTab(TopicPracticeTabTag)
        onNodeWithText("Available questions: 3").assertIsDisplayed()
        onNodeWithTag(TopicPracticeButtonTag).performClick()
        assertEquals(1, topicStarts)
    }

    /**
     * A Topic with no questions. It is still a found Topic, so the Practice page says only that
     * there is nothing to practise — the Topic does not become a terminal screen, and its other two
     * capabilities are untouched.
     */
    @Test
    fun aTopicWithNoQuestionsShowsAnInlineMessageInsteadOfPracticeActions() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Content(
                        topic = Topic("topic_a", "Topic A"),
                        topicQuestionCount = 0,
                        subtopics = emptyList(),
                        learningUnits = TopicLearningUnitsUiState.Available(emptyList()),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Topic A").assertIsDisplayed()
        // The three capabilities stay visible: one of them being empty is not the Topic's verdict.
        onNodeWithTag(TopicStudyTabTag).assertIsDisplayed()
        onNodeWithTag(TopicPracticeTabTag).assertIsDisplayed()
        onNodeWithTag(TopicSubtopicsTabTag).assertIsDisplayed()

        selectTab(TopicPracticeTabTag)
        onNodeWithText("No practice questions are currently available.").assertIsDisplayed()
        onAllNodesWithText("Start Practice").assertCountEquals(0)

        selectTab(TopicSubtopicsTabTag)
        onNodeWithText("This topic has no subtopics.").assertIsDisplayed()
    }

    @Test
    fun anObservedTopicShowsAllTimeAccuracyAndCurrentCoverageAsSeparateThings() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Content(
                        topic = Topic("topic_a", "Topic A"),
                        topicQuestionCount = 28,
                        subtopics = listOf(
                            subtopicItem(
                                id = "subtopic_a",
                                name = "StateFlow & SharedFlow",
                                count = 10,
                                learningContext = learningContext(6, 10, accuracy = 67.0),
                            ),
                        ),
                        learningContext = learningContext(12, 28, accuracy = 76.0),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        selectTab(TopicPracticeTabTag)
        // Both figures are on screen and each says what it measures.
        onNodeWithText("76%").assertIsDisplayed()
        onNodeWithText("All-time accuracy").assertIsDisplayed()
        onNodeWithText("Curriculum coverage").assertIsDisplayed()
        onNodeWithText("12 of 28 questions explored").assertIsDisplayed()
        onAllNodesWithText("Available questions: 28").assertCountEquals(0)
        onNodeWithTag(TopicPracticeButtonTag).assertIsDisplayed()

        selectTab(TopicSubtopicsTabTag)
        // The subtopic row carries the same pair, and drops the authored count that would only
        // duplicate the coverage denominator.
        onNodeWithText("6 of 10 explored").assertIsDisplayed()
        onNodeWithText("67%").assertIsDisplayed()
        onAllNodesWithText("Available questions: 10").assertCountEquals(0)
    }

    @Test
    fun anUnseenTopicAndSubtopicShowCoverageWithoutAZeroPercent() = runComposeUiTest {
        var topicStarts = 0
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Content(
                        topic = Topic("topic_a", "Topic A"),
                        topicQuestionCount = 28,
                        subtopics = listOf(
                            subtopicItem(
                                id = "subtopic_a",
                                name = "Subtopic A",
                                count = 10,
                                learningContext = learningContext(0, 10),
                            ),
                        ),
                        learningContext = learningContext(0, 28),
                    ),
                    onBack = {},
                    onStartTopicPractice = { topicStarts += 1 },
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        selectTab(TopicPracticeTabTag)
        onNodeWithText("Not studied yet").assertIsDisplayed()
        onNodeWithText("0 of 28 questions explored").assertIsDisplayed()
        onNodeWithText("Curriculum coverage").assertIsDisplayed()
        // Nothing fabricates a score for content that was never answered.
        onAllNodesWithText("0%").assertCountEquals(0)
        onAllNodesWithText("Weak area").assertCountEquals(0)
        // And practice is exactly as available as it was.
        onNodeWithTag(TopicPracticeButtonTag).performClick()
        assertEquals(1, topicStarts)

        selectTab(TopicSubtopicsTabTag)
        // The row states it once. "0 of 10 explored" already says nothing has been attempted, so
        // the separate "Not studied yet" line beneath it was the same fact in a smaller type size.
        onNodeWithText("0 of 10 explored").assertIsDisplayed()
        onAllNodesWithText("Not studied yet").assertCountEquals(0)
        onAllNodesWithText("0%").assertCountEquals(0)
        onAllNodesWithText("Weak area").assertCountEquals(0)
        // Nothing has been attempted, so unseen practice would draw the same pool as the row's own
        // tap and is not offered as a second way to do one thing.
        onNodeWithTag(subtopicUnseenPracticeTag("subtopic_a")).assertDoesNotExist()
    }

    /**
     * A scope with no current Questions has no coverage line to carry the fact, so there the
     * neutral note is the only thing that can say it and stays.
     */
    @Test
    fun aSubtopicWithNoCurrentQuestionsStillReadsAsUnstudied() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        subtopics = listOf(
                            subtopicItem(
                                id = "subtopic_a",
                                name = "Subtopic A",
                                learningContext = learningContext(0, 0),
                            ),
                        ),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        selectTab(TopicSubtopicsTabTag)
        onNodeWithText("Not studied yet").assertIsDisplayed()
        onAllNodesWithText("0 of 0 explored").assertCountEquals(0)
    }

    /**
     * A weak Topic offers weak-area practice for itself, and the ordinary Start Practice action is
     * still there and still carries no source of its own.
     */
    @Test
    fun aWeakTopicOffersWeakAreaPracticeBesideOrdinaryPractice() = runComposeUiTest {
        val presets = mutableListOf<PracticePreset>()
        var ordinaryStarts = 0
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        learningContext = learningContext(10, 10, 41.0, isWeak = true),
                    ),
                    onBack = {},
                    onStartTopicPractice = { ordinaryStarts += 1 },
                    onStartSubtopicPractice = {},
                    onPracticePreset = presets::add,
                    onRetry = {},
                )
            }
        }

        selectTab(TopicPracticeTabTag)
        onNodeWithTag(TopicWeakPracticeTag).assertIsDisplayed().performClick()
        onNodeWithTag(TopicPracticeButtonTag).assertIsDisplayed().performClick()

        assertEquals(
            listOf(
                PracticePreset(
                    scope = AssessmentScope.Topic("topic_a"),
                    source = PracticeQuestionSource.WEAK_AREAS,
                ),
            ),
            presets,
        )
        assertEquals(1, ordinaryStarts)
    }

    @Test
    fun aTopicTheDomainDoesNotCallWeakOffersNoWeakAreaShortcut() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    // A low percentage that the policy did not call weak — presentation must not
                    // second-guess that from the number it is displaying.
                    state = topicContent(
                        learningContext = learningContext(10, 10, 22.0, isWeak = false),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        selectTab(TopicPracticeTabTag)
        onNodeWithTag(TopicWeakPracticeTag).assertDoesNotExist()
        onNodeWithTag(TopicPracticeButtonTag).assertIsDisplayed()
    }

    @Test
    fun aTopicWithRemainingCoverageOffersUnseenPractice() = runComposeUiTest {
        val presets = mutableListOf<PracticePreset>()
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(learningContext = learningContext(6, 10, 80.0)),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = presets::add,
                    onRetry = {},
                )
            }
        }

        selectTab(TopicPracticeTabTag)
        onNodeWithTag(TopicUnseenPracticeTag).assertIsDisplayed().performClick()

        assertEquals(
            listOf(
                PracticePreset(
                    scope = AssessmentScope.Topic("topic_a"),
                    source = PracticeQuestionSource.UNSEEN,
                ),
            ),
            presets,
        )
    }

    @Test
    fun aFullyCoveredTopicOffersNoUnseenPractice() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(learningContext = learningContext(10, 10, 80.0)),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        selectTab(TopicPracticeTabTag)
        onNodeWithTag(TopicUnseenPracticeTag).assertDoesNotExist()
        onNodeWithTag(TopicPracticeButtonTag).assertIsDisplayed()
    }

    /**
     * Both conditions can hold at once, and both actions are then offered: the learner chose to look
     * at this scope, so nothing here ranks one intent above the other. Choosing one action globally
     * is Recommended Next's job, on a different surface.
     */
    @Test
    fun aTopicThatIsBothWeakAndPartlyCoveredOffersBothWithNoPrecedence() = runComposeUiTest {
        val presets = mutableListOf<PracticePreset>()
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        learningContext = learningContext(4, 10, 35.0, isWeak = true),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = presets::add,
                    onRetry = {},
                )
            }
        }

        selectTab(TopicPracticeTabTag)
        onNodeWithTag(TopicWeakPracticeTag).assertIsDisplayed().performClick()
        onNodeWithTag(TopicUnseenPracticeTag).assertIsDisplayed().performClick()

        assertEquals(
            listOf(
                PracticePreset(
                    AssessmentScope.Topic("topic_a"),
                    PracticeQuestionSource.WEAK_AREAS,
                ),
                PracticePreset(AssessmentScope.Topic("topic_a"), PracticeQuestionSource.UNSEEN),
            ),
            presets,
        )
    }

    // --------------------------------------------------------- subtopics page

    @Test
    fun subtopicStartUsesStableIdAndEmptyItemsAreAbsent() = runComposeUiTest {
        var clicked: String? = null
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        subtopics = listOf(subtopicItem("subtopic_stable", "Visible Subtopic")),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = { clicked = it },
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        selectTab(TopicSubtopicsTabTag)
        onNodeWithTag(SubtopicPracticeButtonTag).performClick()
        assertEquals("subtopic_stable", clicked)
        onAllNodesWithText("Empty Subtopic").assertCountEquals(0)
    }

    @Test
    fun aTopicWithNoSubtopicsShowsAnEmptySubtopicsPage() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(subtopics = emptyList()),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        selectTab(TopicSubtopicsTabTag)
        onNodeWithText("This topic has no subtopics.").assertIsDisplayed()
        onNodeWithTag(SubtopicPracticeButtonTag).assertDoesNotExist()
    }

    @Test
    fun subtopicWeakBadgesFollowTheDomainFlagOnly() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Content(
                        topic = Topic("topic_a", "Topic A"),
                        topicQuestionCount = 20,
                        subtopics = listOf(
                            subtopicItem(
                                id = "weak_sub",
                                name = "Weak Subtopic",
                                count = 10,
                                learningContext = learningContext(4, 10, 41.0, isWeak = true),
                            ),
                            subtopicItem(
                                // Just as low, but on too little evidence to be called weak.
                                id = "sparse_sub",
                                name = "Sparse Subtopic",
                                count = 10,
                                learningContext = learningContext(1, 10, 0.0),
                            ),
                        ),
                        learningContext = learningContext(5, 20, accuracy = 33.0),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        selectTab(TopicSubtopicsTabTag)
        onAllNodesWithText("Weak area").assertCountEquals(1)
        onNodeWithText("41%").assertIsDisplayed()
        // A real 0% from a real answer stays visible and is not relabelled as unstudied.
        onNodeWithText("0%").assertIsDisplayed()
        onAllNodesWithText("Not studied yet").assertCountEquals(0)
    }

    @Test
    fun anUnavailableHistoryLeavesTheAuthoredCountsAndPracticeInPlace() = runComposeUiTest {
        var subtopicStarts: String? = null
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    // learningContext is null: analytics have not loaded, which says nothing about
                    // the learner and must not be presented as an empty history.
                    state = TopicDetailUiState.Content(
                        topic = Topic("topic_a", "Topic A"),
                        topicQuestionCount = 3,
                        subtopics = listOf(subtopicItem("subtopic_a", "Subtopic A", count = 2)),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = { subtopicStarts = it },
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        selectTab(TopicPracticeTabTag)
        onAllNodesWithText("Not studied yet").assertCountEquals(0)
        onAllNodesWithText("Curriculum coverage").assertCountEquals(0)
        onNodeWithText("Available questions: 3").assertIsDisplayed()

        selectTab(TopicSubtopicsTabTag)
        onAllNodesWithText("Not studied yet").assertCountEquals(0)
        onNodeWithText("Available questions: 2").assertIsDisplayed()
        onNodeWithTag(SubtopicPracticeButtonTag).performClick()
        assertEquals("subtopic_a", subtopicStarts)
    }

    /**
     * Unknown analytics are not empty history: an absent context must not read as "not weak" or as
     * "nothing seen yet", and it must not take ordinary practice away either.
     */
    @Test
    fun unknownAnalyticsInferNoShortcutsAndLeaveOrdinaryPracticeIntact() = runComposeUiTest {
        var ordinaryStarts = 0
        var subtopicStarts = 0
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Content(
                        topic = Topic("topic_a", "Topic A"),
                        topicQuestionCount = 10,
                        subtopics = listOf(subtopicItem("subtopic_a", "Subtopic A", count = 4)),
                    ),
                    onBack = {},
                    onStartTopicPractice = { ordinaryStarts += 1 },
                    onStartSubtopicPractice = { subtopicStarts += 1 },
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        selectTab(TopicPracticeTabTag)
        onNodeWithTag(TopicWeakPracticeTag).assertDoesNotExist()
        onNodeWithTag(TopicUnseenPracticeTag).assertDoesNotExist()
        onNodeWithTag(TopicPracticeButtonTag).performClick()

        selectTab(TopicSubtopicsTabTag)
        onNodeWithTag(subtopicWeakPracticeTag("subtopic_a")).assertDoesNotExist()
        onNodeWithTag(subtopicUnseenPracticeTag("subtopic_a")).assertDoesNotExist()
        onNodeWithTag(SubtopicPracticeButtonTag).performClick()

        assertEquals(1, ordinaryStarts)
        assertEquals(1, subtopicStarts)
    }

    @Test
    fun aWeakSubtopicRowOffersWeakAreaPracticeForItsOwnScope() = runComposeUiTest {
        val presets = mutableListOf<PracticePreset>()
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        subtopics = listOf(
                            subtopicItem(
                                id = "subtopic_a",
                                name = "Subtopic A",
                                count = 10,
                                learningContext = learningContext(10, 10, 30.0, isWeak = true),
                            ),
                        ),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = presets::add,
                    onRetry = {},
                )
            }
        }

        selectTab(TopicSubtopicsTabTag)
        onNodeWithTag(subtopicWeakPracticeTag("subtopic_a")).assertIsDisplayed().performClick()
        onNodeWithTag(subtopicUnseenPracticeTag("subtopic_a")).assertDoesNotExist()

        assertEquals(
            listOf(
                PracticePreset(
                    scope = AssessmentScope.Subtopic("subtopic_a"),
                    source = PracticeQuestionSource.WEAK_AREAS,
                ),
            ),
            presets,
        )
    }

    /**
     * The row's own tap is still ordinary practice for the whole Subtopic; the shortcut is a
     * separate, labelled control that emits a different intent.
     */
    @Test
    fun aPartlyCoveredSubtopicRowOffersUnseenPracticeAlongsideItsOwnTap() = runComposeUiTest {
        val presets = mutableListOf<PracticePreset>()
        val ordinaryStarts = mutableListOf<String>()
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = topicContent(
                        subtopics = listOf(
                            subtopicItem(
                                id = "subtopic_a",
                                name = "Subtopic A",
                                count = 10,
                                learningContext = learningContext(3, 10, 70.0),
                            ),
                        ),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = ordinaryStarts::add,
                    onPracticePreset = presets::add,
                    onRetry = {},
                )
            }
        }

        selectTab(TopicSubtopicsTabTag)
        onNodeWithTag(subtopicUnseenPracticeTag("subtopic_a")).assertIsDisplayed().performClick()
        onNodeWithTag(subtopicWeakPracticeTag("subtopic_a")).assertDoesNotExist()
        onNodeWithTag(SubtopicPracticeButtonTag).performClick()

        assertEquals(
            listOf(
                PracticePreset(
                    scope = AssessmentScope.Subtopic("subtopic_a"),
                    source = PracticeQuestionSource.UNSEEN,
                ),
            ),
            presets,
        )
        assertEquals(listOf("subtopic_a"), ordinaryStarts)
    }
}

/**
 * Selects a page by its tab and waits for the pager to settle there.
 *
 * The wait is on the tab's own selected state rather than on a frame count, so nothing here depends
 * on how long the Material tab and pager animations happen to run for.
 */
@OptIn(ExperimentalTestApi::class)
private suspend fun ComposeUiTest.selectTab(testTag: String) {
    onNodeWithTag(testTag).performClick()
    waitUntil {
        onAllNodesWithTag(testTag).fetchSemanticsNodes().isNotEmpty() &&
            runCatching { onNodeWithTag(testTag).assertIsSelected() }.isSuccess
    }
    waitForIdle()
}

private fun topicContent(
    learningContext: LearningContextUiModel? = null,
    subtopics: List<SubtopicPracticeItem> = emptyList(),
    learningUnits: TopicLearningUnitsUiState = TopicLearningUnitsUiState.Loading,
    studyProgress: StudyProgressUiState<TopicStudyProgress> = StudyProgressUiState.Loading,
): TopicDetailUiState.Content =
    TopicDetailUiState.Content(
        topic = Topic("topic_a", "Topic A"),
        topicQuestionCount = 10,
        subtopics = subtopics,
        learningUnits = learningUnits,
        learningContext = learningContext,
        studyProgress = studyProgress,
    )

private fun subtopicItem(
    id: String,
    name: String,
    count: Int = 1,
    learningContext: LearningContextUiModel? = null,
) = SubtopicPracticeItem(
    subtopic = Subtopic(id, "topic_a", name),
    questionCount = count,
    learningContext = learningContext,
)

/**
 * Study progress for the named Units, each as `studied of total` Lessons.
 *
 * Built as the E22-03 result the ViewModel really derives, rather than as a presentation shortcut,
 * so the join the screen performs is exercised on the real shape.
 */
private fun topicStudyProgress(
    vararg units: Triple<String, Int, Int>,
): StudyProgressUiState<TopicStudyProgress> {
    val unitProgress = units.map { (unitId, studied, total) ->
        LearningUnitStudyProgress(
            unitId = unitId,
            lessons = (0 until total).map {
                LessonStudyProgress(lessonId = "${unitId}_lesson_$it", isStudied = it < studied)
            },
            summary = StudyProgressSummary.of(studied, total),
        )
    }
    return StudyProgressUiState.Available(
        TopicStudyProgress(
            topicId = "topic_a",
            units = unitProgress,
            summary = StudyProgressSummary.of(
                studiedCount = units.sumOf { it.second },
                totalCount = units.sumOf { it.third },
            ),
        ),
    )
}

private fun learningUnitItem(
    unitId: String,
    title: String,
    lessons: Int,
) = LearningUnitItemUiModel(
    unitId = unitId,
    title = title,
    summary = "Summary for $unitId",
    activeLessonCount = lessons,
)

private fun learningContext(
    attempted: Int,
    total: Int,
    accuracy: Double? = null,
    isWeak: Boolean = false,
) = LearningContextUiModel(
    attemptedQuestionCount = attempted,
    totalQuestionCount = total,
    coveragePercentage = if (total == 0) null else attempted.toDouble() / total * 100.0,
    accuracyPercentage = accuracy,
    isWeak = isWeak,
)
