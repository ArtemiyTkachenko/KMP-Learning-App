package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
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
import org.artkachenko.kmp_learning_app.ui.LearningContextUiModel

@OptIn(ExperimentalTestApi::class)
internal class TopicDetailScreenTest {
    @Test
    fun contentRendersTopicSubtopicsCountsAndStartActions() = runComposeUiTest {
        var topicStarts = 0
        var subtopicId: String? = null
        val topic = Topic("topic_a", "Topic A")
        val subtopic = Subtopic("subtopic_a", topic.id, "Subtopic A")

        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Content(
                        topic = topic,
                        topicQuestionCount = 3,
                        subtopics = listOf(SubtopicPracticeItem(subtopic, 2)),
                    ),
                    onBack = {},
                    onStartTopicPractice = { topicStarts += 1 },
                    onStartSubtopicPractice = { subtopicId = it },
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Topic A").assertIsDisplayed()
        onNodeWithText("Subtopic A").assertIsDisplayed()
        onNodeWithText("Available questions: 3").assertIsDisplayed()
        onNodeWithText("Available questions: 2").assertIsDisplayed()
        onNodeWithTag(TopicPracticeButtonTag).performClick()
        assertEquals(1, topicStarts)
    }

    @Test
    fun subtopicStartUsesStableIdAndEmptyItemsAreAbsent() = runComposeUiTest {
        var clicked: String? = null
        val topic = Topic("topic_a", "Topic A")
        val subtopic = Subtopic("subtopic_stable", topic.id, "Visible Subtopic")

        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Content(
                        topic = topic,
                        topicQuestionCount = 1,
                        subtopics = listOf(SubtopicPracticeItem(subtopic, 1)),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = { clicked = it },
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithTag(SubtopicPracticeButtonTag).performClick()
        assertEquals("subtopic_stable", clicked)
        onAllNodesWithText("Empty Subtopic").assertCountEquals(0)
    }

    @Test
    fun anObservedTopicShowsAllTimeAccuracyAndCurrentCoverageAsSeparateThings() = runComposeUiTest {
        val topic = Topic("topic_a", "Topic A")
        val subtopic = Subtopic("subtopic_a", topic.id, "StateFlow & SharedFlow")

        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Content(
                        topic = topic,
                        topicQuestionCount = 28,
                        subtopics = listOf(
                            SubtopicPracticeItem(
                                subtopic = subtopic,
                                questionCount = 10,
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

        // Both figures are on screen and each says what it measures.
        onNodeWithText("76%").assertIsDisplayed()
        onNodeWithText("All-time accuracy").assertIsDisplayed()
        onNodeWithText("Curriculum coverage").assertIsDisplayed()
        onNodeWithText("12 of 28 questions explored").assertIsDisplayed()
        // The subtopic row carries the same pair, and drops the authored count that now duplicates
        // the coverage denominator.
        onNodeWithText("6 of 10 explored").assertIsDisplayed()
        onNodeWithText("67%").assertIsDisplayed()
        onAllNodesWithText("Available questions: 10").assertCountEquals(0)
        onAllNodesWithText("Available questions: 28").assertCountEquals(0)
        onNodeWithTag(TopicPracticeButtonTag).assertIsDisplayed()
    }

    @Test
    fun anUnseenTopicAndSubtopicShowCoverageWithoutAZeroPercent() = runComposeUiTest {
        val topic = Topic("topic_a", "Topic A")
        var topicStarts = 0

        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Content(
                        topic = topic,
                        topicQuestionCount = 28,
                        subtopics = listOf(
                            SubtopicPracticeItem(
                                subtopic = Subtopic("subtopic_a", topic.id, "Subtopic A"),
                                questionCount = 10,
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

        onAllNodesWithText("Not studied yet").assertCountEquals(2)
        onNodeWithText("0 of 28 questions explored").assertIsDisplayed()
        onNodeWithText("0 of 10 explored").assertIsDisplayed()
        onNodeWithText("Curriculum coverage").assertIsDisplayed()
        // Nothing fabricates a score for content that was never answered.
        onAllNodesWithText("0%").assertCountEquals(0)
        onAllNodesWithText("Weak area").assertCountEquals(0)
        // And practice is exactly as available as it was.
        onNodeWithTag(TopicPracticeButtonTag).performClick()
        assertEquals(1, topicStarts)
    }

    @Test
    fun subtopicWeakBadgesFollowTheDomainFlagOnly() = runComposeUiTest {
        val topic = Topic("topic_a", "Topic A")

        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Content(
                        topic = topic,
                        topicQuestionCount = 20,
                        subtopics = listOf(
                            SubtopicPracticeItem(
                                subtopic = Subtopic("weak_sub", topic.id, "Weak Subtopic"),
                                questionCount = 10,
                                learningContext = learningContext(4, 10, 41.0, isWeak = true),
                            ),
                            SubtopicPracticeItem(
                                // Just as low, but on too little evidence to be called weak.
                                subtopic = Subtopic("sparse_sub", topic.id, "Sparse Subtopic"),
                                questionCount = 10,
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

        onAllNodesWithText("Weak area").assertCountEquals(1)
        onNodeWithText("41%").assertIsDisplayed()
        // A real 0% from a real answer stays visible and is not relabelled as unstudied.
        onNodeWithText("0%").assertIsDisplayed()
        onAllNodesWithText("Not studied yet").assertCountEquals(0)
    }

    @Test
    fun anUnavailableHistoryLeavesTheAuthoredCountsAndPracticeInPlace() = runComposeUiTest {
        val topic = Topic("topic_a", "Topic A")
        var subtopicStarts: String? = null

        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    // learningContext is null: analytics have not loaded, which says nothing about
                    // the learner and must not be presented as an empty history.
                    state = TopicDetailUiState.Content(
                        topic = topic,
                        topicQuestionCount = 3,
                        subtopics = listOf(
                            SubtopicPracticeItem(
                                Subtopic("subtopic_a", topic.id, "Subtopic A"),
                                2,
                            ),
                        ),
                    ),
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = { subtopicStarts = it },
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        onAllNodesWithText("Not studied yet").assertCountEquals(0)
        onAllNodesWithText("Curriculum coverage").assertCountEquals(0)
        onNodeWithText("Available questions: 3").assertIsDisplayed()
        onNodeWithText("Available questions: 2").assertIsDisplayed()
        onNodeWithTag(SubtopicPracticeButtonTag).performClick()
        assertEquals("subtopic_a", subtopicStarts)
    }

    /**
     * A Topic with neither capability. It is still a found Topic, so the screen renders it and says
     * only that there is nothing to practise — it does not become a terminal message.
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
        onNodeWithText("Practice").assertIsDisplayed()
        onNodeWithText("No practice questions are currently available.").assertIsDisplayed()
        onAllNodesWithText("Start Practice").assertCountEquals(0)
        // No authored Units, so the study section is absent rather than empty.
        onAllNodesWithText("Study").assertCountEquals(0)
        onAllNodesWithText("Subtopics").assertCountEquals(0)
    }

    /**
     * Study-only: the correction this issue exists for. Learning material must survive a Topic that
     * has no assessment questions at all.
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

        onNodeWithText("Study").assertIsDisplayed()
        onNodeWithText("Thinking in Compose").assertIsDisplayed()
        onNodeWithText("Summary for unit_a").assertIsDisplayed()
        onNodeWithText("3 lessons").assertIsDisplayed()
        onNodeWithText("No practice questions are currently available.").assertIsDisplayed()
        onNodeWithTag(TopicPracticeButtonTag).assertDoesNotExist()
        onNodeWithTag(SubtopicPracticeButtonTag).assertDoesNotExist()
        // Nothing on this surface claims the learner has or has not read anything.
        onAllNodesWithText("Not started").assertCountEquals(0)
    }

    /** Practice-only: no authored study material must not make an ordinary Topic look broken. */
    @Test
    fun aPracticeOnlyTopicShowsNoStudySection() = runComposeUiTest {
        var topicStarts = 0
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Content(
                        topic = Topic("topic_a", "Topic A"),
                        topicQuestionCount = 3,
                        subtopics = listOf(
                            SubtopicPracticeItem(
                                Subtopic("subtopic_a", "topic_a", "Subtopic A"),
                                2,
                            ),
                        ),
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

        onAllNodesWithText("Study").assertCountEquals(0)
        onAllNodesWithText("Learning material could not be loaded.").assertCountEquals(0)
        // With nothing to separate practice from, the section label is not worth its vertical
        // space: this is the layout every Topic without authored study material keeps.
        onAllNodesWithText("Practice").assertCountEquals(0)
        onNodeWithText("Subtopics").assertIsDisplayed()
        onNodeWithText("Subtopic A").assertIsDisplayed()
        onNodeWithTag(TopicPracticeButtonTag).performClick()
        assertEquals(1, topicStarts)
    }

    /** Combined: the production android_ui shape. Neither section replaces the other. */
    @Test
    fun aStudyableAndPracticeableTopicShowsBothSections() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Content(
                        topic = Topic("topic_a", "Topic A"),
                        topicQuestionCount = 26,
                        subtopics = listOf(
                            SubtopicPracticeItem(
                                Subtopic("subtopic_a", "topic_a", "Subtopic A"),
                                10,
                            ),
                        ),
                        learningUnits = TopicLearningUnitsUiState.Available(
                            listOf(learningUnitItem("unit_a", "Thinking in Compose", lessons = 3)),
                        ),
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

        onNodeWithText("Study").assertIsDisplayed()
        onNodeWithText("Thinking in Compose").assertIsDisplayed()
        onNodeWithText("3 lessons").assertIsDisplayed()
        onNodeWithText("Practice").assertIsDisplayed()
        // The two systems stay separate: lesson counts never join coverage or accuracy.
        onNodeWithText("12 of 26 questions explored").assertIsDisplayed()
        onNodeWithText("76%").assertIsDisplayed()
        onNodeWithTag(TopicPracticeButtonTag).assertIsDisplayed()
        onNodeWithText("Subtopic A").assertIsDisplayed()
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
     * A learning-content failure is inline and local. It says the material could not be read rather
     * than that there is none, and it takes no practice control with it.
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
                        subtopics = listOf(
                            SubtopicPracticeItem(
                                Subtopic("subtopic_a", "topic_a", "Subtopic A"),
                                2,
                            ),
                        ),
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
        onNodeWithTag(TopicPracticeButtonTag).performClick()
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

        onAllNodesWithText("Study").assertCountEquals(0)
        onAllNodesWithText("Learning material could not be loaded.").assertCountEquals(0)
        onNodeWithTag(TopicPracticeButtonTag).performClick()
        assertEquals(1, topicStarts)
    }

    /**
     * The handoff E21-03 will supply navigation for: the card emits the stable Unit ID, never the
     * presentation model and never the authored Unit.
     */
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
     * The transitional production state: with no handler the card is study content, not a control.
     * A clickable card that leads nowhere would be worse than no affordance at all.
     */
    @Test
    fun aUnitCardWithNoHandlerAdvertisesNoClickAction() = runComposeUiTest {
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
    }

    @Test
    fun targetSubtopicIsPositionedByStableIdWhenContentLoads() = runComposeUiTest {
        val topic = Topic("topic_a", "Topic A")
        // Enriched rows and an enriched summary: the header block must stay one lazy item, so the
        // stable-ID positioning still lands on subtopicIndex + 1.
        val subtopics = (1..16).map { index ->
            SubtopicPracticeItem(
                subtopic = Subtopic("subtopic_$index", topic.id, "Subtopic $index"),
                questionCount = 1,
                learningContext = learningContext(1, 1, accuracy = 50.0),
            )
        }
        setContent {
            MaterialTheme {
                Box(Modifier.size(400.dp, 360.dp)) {
                    TopicDetailScreen(
                        state = TopicDetailUiState.Content(
                            topic = topic,
                            topicQuestionCount = subtopics.size,
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

        onNodeWithText("Subtopic 15").assertIsDisplayed()
    }

    @Test
    fun missingTargetSubtopicKeepsNormalTopicContent() = runComposeUiTest {
        val topic = Topic("topic_a", "Topic A")
        setContent {
            MaterialTheme {
                TopicDetailScreen(
                    state = TopicDetailUiState.Content(
                        topic = topic,
                        topicQuestionCount = 1,
                        subtopics = listOf(
                            SubtopicPracticeItem(
                                Subtopic("subtopic_a", topic.id, "Subtopic A"),
                                1,
                            ),
                        ),
                    ),
                    targetSubtopicId = "retired_subtopic",
                    onBack = {},
                    onStartTopicPractice = {},
                    onStartSubtopicPractice = {},
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Subtopic A").assertIsDisplayed()
        onNodeWithTag(TopicPracticeButtonTag).assertIsDisplayed()
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
                        subtopics = listOf(
                            SubtopicPracticeItem(
                                Subtopic("subtopic_a", "topic_a", "Subtopic A"),
                                4,
                            ),
                        ),
                    ),
                    onBack = {},
                    onStartTopicPractice = { ordinaryStarts += 1 },
                    onStartSubtopicPractice = { subtopicStarts += 1 },
                    onPracticePreset = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithTag(TopicWeakPracticeTag).assertDoesNotExist()
        onNodeWithTag(TopicUnseenPracticeTag).assertDoesNotExist()
        onNodeWithTag(subtopicWeakPracticeTag("subtopic_a")).assertDoesNotExist()
        onNodeWithTag(subtopicUnseenPracticeTag("subtopic_a")).assertDoesNotExist()

        onNodeWithTag(TopicPracticeButtonTag).performClick()
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
                            SubtopicPracticeItem(
                                subtopic = Subtopic("subtopic_a", "topic_a", "Subtopic A"),
                                questionCount = 10,
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
                            SubtopicPracticeItem(
                                subtopic = Subtopic("subtopic_a", "topic_a", "Subtopic A"),
                                questionCount = 10,
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

private fun topicContent(
    learningContext: LearningContextUiModel? = null,
    subtopics: List<SubtopicPracticeItem> = emptyList(),
    learningUnits: TopicLearningUnitsUiState = TopicLearningUnitsUiState.Loading,
): TopicDetailUiState.Content =
    TopicDetailUiState.Content(
        topic = Topic("topic_a", "Topic A"),
        topicQuestionCount = 10,
        subtopics = subtopics,
        learningUnits = learningUnits,
        learningContext = learningContext,
    )

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
