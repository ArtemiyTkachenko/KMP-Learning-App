package org.artkachenko.kmp_learning_app.assessment_review

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.assessment.AllQuestionLevels
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeState
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme

private const val RetakeTag = "outcome_retake"
private const val RetakeProgressTag = "outcome_retake_progress"

/**
 * The summary block both result screens now share: the outcome, its caveats, and the two things the
 * learner can do next.
 *
 * What is covered here is the part that is a *decision* rather than a rendering — which action is
 * the primary one, and whether the remediation it offers is the exact scope the run just failed.
 * The hero's own figure is covered by `AssessmentCompletionHeroTest`, the retake control's six
 * states by `AssessmentRetakeActionTest`, and the wording each product supplies by each screen's
 * test.
 */
@OptIn(ExperimentalTestApi::class)
internal class AssessmentResultOutcomeTest {

    /**
     * Mistakes present: practising them is the primary action, and it starts a run scoped to exactly
     * the Subtopics this attempt got wrong.
     *
     * The configuration is asserted in full because it is the one thing on this screen that is not
     * presentation: the scope is the Subtopics of the *incorrect* Questions only, the count is how
     * many there were, and `UNRESOLVED_MISTAKES` is what narrows selection from "questions in these
     * Subtopics" to "questions you still have wrong". A correct answer contributing its Subtopic
     * here would silently widen every remediation run in the product.
     */
    @Test
    fun retainedMistakesStartAnExactUnresolvedPractice() = runComposeUiTest {
        val configs = mutableListOf<AssessmentConfig.Focused>()
        setContent {
            AppTheme {
                AssessmentResultOutcome(
                    title = "Practice complete",
                    correctAnswers = 1,
                    totalQuestions = 3,
                    percentage = 33.3,
                    questions = listOf(
                        ReviewQuestionItem.Available(question(isCorrect = false)),
                        ReviewQuestionItem.Available(
                            question(isCorrect = false).copy(questionId = "q2", subtopicId = "other"),
                        ),
                        ReviewQuestionItem.Available(
                            question(isCorrect = true).copy(questionId = "q3", subtopicId = "correct_one"),
                        ),
                    ),
                    retakeState = AssessmentRetakeState.Idle,
                    retakeWording = wording,
                    onRetake = {},
                    retakeActionTestTag = RetakeTag,
                    retakeProgressTestTag = RetakeProgressTag,
                    onPracticeMistakes = configs::add,
                )
            }
        }

        onNodeWithText("2 questions remain in Mistakes for review.").assertIsDisplayed()
        onNodeWithText("Practice 2 mistakes").performClick()

        assertEquals(
            listOf(
                AssessmentConfig.Focused(
                    scope = AssessmentScope.Subtopics(setOf("topic_basics", "other")),
                    questionCount = 2,
                    levels = AllQuestionLevels,
                    source = PracticeQuestionSource.UNRESOLVED_MISTAKES,
                ),
            ),
            configs,
        )
        // The retake is the alternative beside it, and still its own working control.
        onNodeWithTag(RetakeTag).assertIsDisplayed()
    }

    /**
     * Nothing left wrong: there is no remediation to offer, so the retake is the only action and is
     * the screen's primary one.
     *
     * This is the case the old layout got wrong. With the practice button living inside the mistakes
     * notice, a run with no mistakes rendered no notice and therefore no filled control at all — the
     * only thing to press on a completed, perfect result was an outlined button.
     */
    @Test
    fun aRunWithNothingToFixOffersOnlyTheRetake() = runComposeUiTest {
        var retakes = 0
        setContent {
            AppTheme {
                AssessmentResultOutcome(
                    title = "Practice complete",
                    correctAnswers = 2,
                    totalQuestions = 2,
                    percentage = 100.0,
                    questions = listOf(
                        ReviewQuestionItem.Available(question(isCorrect = true)),
                        ReviewQuestionItem.Available(
                            question(isCorrect = true).copy(questionId = "q2"),
                        ),
                    ),
                    retakeState = AssessmentRetakeState.Idle,
                    retakeWording = wording,
                    onRetake = { retakes += 1 },
                    retakeActionTestTag = RetakeTag,
                    retakeProgressTestTag = RetakeProgressTag,
                    onPracticeMistakes = {},
                )
            }
        }

        onNodeWithText("remain in Mistakes", substring = true).assertDoesNotExist()
        onNodeWithText("mistakes", substring = true).assertDoesNotExist()
        onNodeWithTag(RetakeTag).performClick()
        assertEquals(1, retakes)
    }

    /**
     * A host that cannot start a practice run offers no remediation even where there are mistakes.
     *
     * The nullability of `onPracticeMistakes` is how the two products state whether this navigation
     * exists from here at all, which is why it is the absence of a callback rather than a flag: a
     * flag would allow "show the button but have nowhere to go".
     */
    @Test
    fun mistakesWithNowhereToPractiseStillReportThemselvesAsACaveat() = runComposeUiTest {
        setContent {
            AppTheme {
                AssessmentResultOutcome(
                    title = "Interview complete",
                    correctAnswers = 0,
                    totalQuestions = 1,
                    percentage = 0.0,
                    questions = listOf(ReviewQuestionItem.Available(question(isCorrect = false))),
                    retakeState = AssessmentRetakeState.Idle,
                    retakeWording = wording,
                    onRetake = {},
                    retakeActionTestTag = RetakeTag,
                    retakeProgressTestTag = RetakeProgressTag,
                    onPracticeMistakes = null,
                )
            }
        }

        onNodeWithText("1 question remains in Mistakes for review.").assertIsDisplayed()
        onNodeWithText("Practice 1 mistake").assertDoesNotExist()
        onNodeWithTag(RetakeTag).assertIsDisplayed()
    }

    /**
     * A Question the curriculum has dropped is not a mistake this screen can act on.
     *
     * It cannot be shown and it cannot be practised, so it neither raises the retained count nor
     * contributes a Subtopic — the same availability rule saving already follows. What it does do is
     * make the transcript shorter than the score, which is what the unresolved notice is for.
     */
    @Test
    fun aDroppedQuestionIsNotCountedAsAPractisableMistake() = runComposeUiTest {
        setContent {
            AppTheme {
                AssessmentResultOutcome(
                    title = "Practice complete",
                    correctAnswers = 1,
                    totalQuestions = 2,
                    percentage = 50.0,
                    questions = listOf(
                        ReviewQuestionItem.Available(question(isCorrect = true)),
                        ReviewQuestionItem.Missing("gone"),
                    ),
                    retakeState = AssessmentRetakeState.Idle,
                    retakeWording = wording,
                    onRetake = {},
                    retakeActionTestTag = RetakeTag,
                    retakeProgressTestTag = RetakeProgressTag,
                    onPracticeMistakes = {},
                )
            }
        }

        onNodeWithText("remain in Mistakes", substring = true).assertDoesNotExist()
        onNodeWithText("1 of 2 questions", substring = true).assertIsDisplayed()
    }

    private val wording = AssessmentRetakeWording(
        action = "Practice again",
        starting = "Starting practice",
        sourceMissing = "The original attempt is gone.",
        noQuestions = "No questions available.",
        error = "Could not start. Try again.",
    )

    private fun question(isCorrect: Boolean) = ReviewQuestionUiModel(
        questionId = "q1",
        topicId = "topic",
        subtopicId = "topic_basics",
        text = "Question text",
        isCorrect = isCorrect,
        answers = listOf(ReviewAnswerUiModel("a", "Answer A", true, isCorrect)),
        explanation = "Explanation",
        sources = emptyList(),
    )
}
