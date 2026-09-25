package org.artkachenko.kmp_learning_app.assessment_review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.assessment_review_practice_mistakes
import org.artkachenko.kmp_learning_app.assessment.AllQuestionLevels
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeState
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.jetbrains.compose.resources.pluralStringResource

/**
 * What a finished assessment amounts to, and what the learner does next.
 *
 * Both result screens had this block written out, identically, forty lines each: the completion
 * hero, the two caveats about the transcript below it, and the retake control. The only differences
 * were the title string, the retake wording, and the test tags — which are the three things this
 * takes as parameters. `AssessmentResultLayout` already shares the adaptive shell and
 * `AssessmentCompletionHero` already shares the figure; this is the piece between them that was
 * still duplicated, and duplicating it is how the two screens would come to disagree about their own
 * action hierarchy the next time one of them was touched.
 *
 * Every input is explicit. There is no `isInterview` and no `showMistakes`: the products differ in
 * *wording*, which is data, and in whether mistake practice is reachable from this host at all,
 * which is the nullability of [onPracticeMistakes]. A flag would allow combinations that do not
 * exist, such as an interview whose retake is worded as practice.
 *
 * ## The hierarchy this owns
 *
 * Before this existed, the summary was four loose siblings at one spacing: hero, amber caveat, amber
 * caveat with a **filled** button attached, outlined retake. So the one filled button on the screen
 * was buried between two warning lines, and a learner who answered everything correctly reached a
 * result screen with *no* primary action at all — the only thing to press was an outlined button
 * under two notices that were not shown.
 *
 * The order is now the reading order the screen wants: the outcome, then anything qualifying it,
 * then the actions grouped together at the bottom. Which action is primary depends on what the run
 * produced, and that is the whole decision:
 *
 * - **Unresolved mistakes**: practising them is the primary action, because it is the one thing the
 *   result has just told the learner they need. The retake is the alternative beside it.
 * - **Nothing left to fix**: there is no remediation to offer, so taking it again is the primary
 *   action and wears the filled weight.
 *
 * Neither branch changes what any action *does*. The mistake configuration below is the same
 * `AssessmentConfig.Focused` the notice used to build, and the retake callback is untouched.
 */
@Composable
internal fun AssessmentResultOutcome(
    title: String,
    correctAnswers: Int,
    totalQuestions: Int,
    percentage: Double,
    questions: List<ReviewQuestionItem>,
    retakeState: AssessmentRetakeState,
    retakeWording: AssessmentRetakeWording,
    onRetake: () -> Unit,
    retakeActionTestTag: String,
    retakeProgressTestTag: String,
    onPracticeMistakes: ((AssessmentConfig.Focused) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val mistakes = questions.unresolvedMistakes()
    // Reachable only when this host can start a practice run *and* there is something to practise.
    val practiceMistakes = onPracticeMistakes?.takeIf { mistakes.isNotEmpty() }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.Comfortable)) {
        AssessmentCompletionHero(
            correctAnswers = correctAnswers,
            totalQuestions = totalQuestions,
            percentage = percentage,
            title = title,
        )
        UnresolvedReviewQuestionsNotice(questions, totalQuestions)
        MistakeRetentionNotice(mistakes.size)
        // The actions are one group at their own, tighter spacing, so they read as two ways forward
        // rather than as two more items in the column of notices above them.
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Related)) {
            if (practiceMistakes != null) {
                PracticeMistakesButton(mistakes, practiceMistakes)
            }
            AssessmentRetakeAction(
                state = retakeState,
                wording = retakeWording,
                onRetake = onRetake,
                emphasis = if (practiceMistakes == null) {
                    AssessmentActionEmphasis.PRIMARY
                } else {
                    AssessmentActionEmphasis.SECONDARY
                },
                actionTestTag = retakeActionTestTag,
                progressTestTag = retakeProgressTestTag,
            )
        }
    }
}

/**
 * Practice exactly the Questions this run got wrong.
 *
 * The scope is the Subtopics those Questions came from and the count is how many there were, so the
 * run the learner starts covers what they just missed rather than the Topic it happened to sit in.
 * `PracticeQuestionSource.UNRESOLVED_MISTAKES` is what narrows it from "questions in these
 * Subtopics" to "questions in these Subtopics you still have wrong"; selection does the rest.
 */
@Composable
private fun PracticeMistakesButton(
    mistakes: List<ReviewQuestionUiModel>,
    onPracticeMistakes: (AssessmentConfig.Focused) -> Unit,
) {
    Button(
        onClick = {
            onPracticeMistakes(
                AssessmentConfig.Focused(
                    scope = AssessmentScope.Subtopics(
                        mistakes.mapTo(linkedSetOf()) { it.subtopicId },
                    ),
                    questionCount = mistakes.size,
                    levels = AllQuestionLevels,
                    source = PracticeQuestionSource.UNRESOLVED_MISTAKES,
                ),
            )
        },
    ) {
        Text(
            pluralStringResource(
                Res.plurals.assessment_review_practice_mistakes,
                mistakes.size,
                mistakes.size,
            ),
        )
    }
}

/**
 * The Questions this attempt answered incorrectly and whose content the curriculum still holds.
 *
 * A Question the curriculum has dropped cannot be practised and cannot be shown, so it is not a
 * mistake this screen can offer to do anything about — which is the same availability rule
 * [isAvailableFor] states for saving.
 */
private fun List<ReviewQuestionItem>.unresolvedMistakes(): List<ReviewQuestionUiModel> = mapNotNull {
    (it as? ReviewQuestionItem.Available)?.question?.takeIf { question -> !question.isCorrect }
}
