package org.artkachenko.kmp_learning_app.assessment_review

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.assessment_review_collapse
import kmp_learning_app.shared.generated.resources.assessment_review_expand
import kmp_learning_app.shared.generated.resources.assessment_review_hidden_questions
import kmp_learning_app.shared.generated.resources.assessment_review_missing_question
import kmp_learning_app.shared.generated.resources.assessment_review_interview_complete
import kmp_learning_app.shared.generated.resources.assessment_review_practice_complete
import kmp_learning_app.shared.generated.resources.assessment_review_mistakes_retained
import kmp_learning_app.shared.generated.resources.assessment_review_selected
import kmp_learning_app.shared.generated.resources.assessment_review_unresolved_questions
import org.artkachenko.kmp_learning_app.ui.theme.AppMotion
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.artkachenko.kmp_learning_app.ui.withInlineCode
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * That the mistakes in this run are still waiting in the Mistakes queue.
 *
 * A caveat about the transcript below, not an action: the button that offers to practise them is a
 * result *action* and lives with the other one in [AssessmentResultOutcome], which is what decides
 * which of the two is the screen's primary. Keeping the button here made it the only filled control
 * on the screen and put it between two warning lines, and left a run with nothing to fix with no
 * primary action at all.
 *
 * Emits nothing at zero, so a clean run adds no gap to the column it sits in.
 */
@Composable
internal fun MistakeRetentionNotice(
    retainedCount: Int,
    modifier: Modifier = Modifier,
) {
    if (retainedCount <= 0) return
    Text(
        text = pluralStringResource(
            Res.plurals.assessment_review_mistakes_retained,
            retainedCount,
            retainedCount,
        ),
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = AppThemeExtras.semanticColors.partiallyCorrect,
    )
}

/**
 * Explains why the score above can exceed the questions listed below.
 *
 * [AssessmentCompletionHero] renders the persisted AssessmentScore and stays
 * authoritative, while review items and any topic breakdown can only count
 * questions whose curriculum content still resolves. The count is derived here
 * rather than in each result ViewModel so both result screens share one rule.
 */
@Composable
internal fun UnresolvedReviewQuestionsNotice(
    questions: List<ReviewQuestionItem>,
    totalQuestions: Int,
    modifier: Modifier = Modifier,
) {
    val resolved = questions.count { it is ReviewQuestionItem.Available }
    if (resolved >= totalQuestions) return

    Text(
        stringResource(
            Res.string.assessment_review_unresolved_questions,
            totalQuestions - resolved,
            totalQuestions,
        ),
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        // A caveat about missing curriculum content, not a failure: full error red overstated it.
        color = AppThemeExtras.semanticColors.partiallyCorrect,
    )
}

/**
 * Explains why this result counts fewer Questions than were asked: the learner's content setting
 * hides some of them.
 *
 * The result is projected through the current curriculum visibility, so the score above already
 * covers only the visible Questions. This is a neutral note about that projection, not a caveat or
 * a warning — nothing is missing or wrong, and the hidden Questions return with the setting. Emits
 * nothing at zero.
 */
@Composable
internal fun HiddenReviewQuestionsNotice(
    hiddenCount: Int,
    modifier: Modifier = Modifier,
) {
    if (hiddenCount <= 0) return
    Text(
        text = pluralStringResource(
            Res.plurals.assessment_review_hidden_questions,
            hiddenCount,
            hiddenCount,
        ),
        modifier = modifier.testTag(HiddenReviewQuestionsNoticeTag),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

internal const val HiddenReviewQuestionsNoticeTag = "hidden_review_questions_notice"

/**
 * Stable per-Question handle for the save action.
 *
 * One convention for all three review surfaces, because they render the same shared card: a
 * surface-specific tag would suggest three affordances where there is one.
 */
internal fun reviewQuestionSaveTag(questionId: String): String =
    "review_question_save_$questionId"

/**
 * The attempt's answers, put through the shared derivation in [questionOutcome].
 *
 * The review model carries a per-answer `wasSelected` flag rather than a set of selected IDs, so
 * this is where the two shapes meet; the rule itself is not restated here.
 */
private fun ReviewQuestionUiModel.outcome(): QuestionOutcome = questionOutcome(
    scoredCorrect = isCorrect,
    selectedAnswerIds = answers.filter { it.wasSelected }.map { it.id }.toSet(),
    correctAnswerIds = answers.filter { it.isCorrectAnswer }.map { it.id },
)

/**
 * [saveAction] is optional so this component stays usable where saved state is unknown, or where a
 * surface has no saving to offer at all. Everything else about the card is unchanged by it: the
 * action sits beside the heading and leaves the outcome, answers, explanation, and source links
 * exactly as they were.
 *
 * [footer] is what the hosting surface offers to do about this particular Question — practise its
 * Subtopic, open the Lesson behind it — and is a slot rather than a fixed pair of buttons because
 * only the Mistakes queue has anything to put there. It renders inside the card, below the
 * disclosure, so an action about one Question cannot be mistaken for an action about the screen.
 *
 * [statesOutcome] says whether the surface around this card has already told the learner what these
 * questions are. A result screen lists correct, partially correct, and incorrect questions together,
 * so every card has to declare which it is. The Mistakes queue does not: it is titled by its count
 * of unresolved mistakes, every entry in it is one by definition, and stamping a saturated
 * `Incorrect` badge on each card repeats that fact once per card while making a list the learner
 * opened deliberately look like a wall of failures. The distinction that *is* still worth drawing
 * there — partially correct, where they picked only correct options but missed one — is a different
 * state from the one the screen declares, so it is kept; see [QuestionOutcomeLabel].
 */
@Composable
internal fun ReviewQuestionCard(
    question: ReviewQuestionUiModel,
    onSourceClick: (String) -> Unit,
    failedSourceUrl: String? = null,
    saveAction: ReviewSaveAction? = null,
    statesOutcome: Boolean = true,
    modifier: Modifier = Modifier,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    var expanded by rememberSaveable(question.questionId) { mutableStateOf(!question.isCorrect) }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.Comfortable),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.Related),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    question.text.withInlineCode(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (saveAction != null) {
                    QuestionBookmarkAction(
                        isSaved = saveAction.isSaved,
                        isPending = saveAction.isPending,
                        onToggle = saveAction.onToggle,
                        modifier = Modifier.testTag(reviewQuestionSaveTag(question.questionId)),
                    )
                }
            }
            QuestionOutcomeLabel(outcome = question.outcome(), statesOutcome = statesOutcome)
            QuestionDisclosure(expanded = expanded, onToggle = { expanded = !expanded }) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.Related),
                        modifier = Modifier.animateEnterExit(
                            enter = fadeIn(
                                AppMotion.revealSpec(QuestionAnswersRevealDelayMillis),
                            ),
                        ),
                    ) {
                        question.answers.forEach { ReviewAnswerRow(it) }
                    }
                    QuestionExplanationBlock(
                        explanation = question.explanation,
                        modifier = Modifier.animateEnterExit(
                            enter = fadeIn(
                                AppMotion.revealSpec(QuestionExplanationRevealDelayMillis),
                            ),
                        ),
                    )
                    QuestionSources(
                        sources = question.sources,
                        onSourceClick = onSourceClick,
                        failedSourceUrl = failedSourceUrl,
                        modifier = Modifier.animateEnterExit(
                            enter = fadeIn(
                                AppMotion.revealSpec(QuestionExplanationRevealDelayMillis),
                            ),
                        ),
                    )
                }
            }
            // Whatever this surface offers to do about this Question, inside the card it belongs
            // to. The Mistakes queue used to emit its two shortcuts as siblings of the card, which
            // left two full-width text links floating on the page background between entries,
            // reading as navigation for the screen rather than as actions for one Question.
            footer?.invoke(this)
        }
    }
}

/**
 * The badge, unless the surface has already said it.
 *
 * When [statesOutcome] is false the plain incorrect verdict is dropped, because the screen declared
 * it: what is left is the one case that still distinguishes something — partially correct. Correct
 * cannot occur on such a surface today and is kept rather than special-cased, so a future caller
 * that mixes outcomes under a heading of its own still reads correctly.
 */
@Composable
private fun QuestionOutcomeLabel(outcome: QuestionOutcome, statesOutcome: Boolean) {
    if (!statesOutcome && outcome == QuestionOutcome.INCORRECT) return
    QuestionOutcomeBadge(outcome)
}

/**
 * One answered option: the shared option container, coloured and labelled by how this attempt's
 * selection related to the authored correct set.
 */
@Composable
private fun ReviewAnswerRow(answer: ReviewAnswerUiModel) {
    val outcome = answerOutcome(
        wasSelected = answer.wasSelected,
        isCorrectAnswer = answer.isCorrectAnswer,
    )
    // These options sit inside a `surfaceContainerLow` card, so an unmarked one takes the page tone
    // and reads as a well within it rather than as another card on top of one.
    val colors = outcome.colors(neutralContainer = MaterialTheme.colorScheme.surface)
    val label = outcome.tagLabel()
    QuestionAnswerOption(
        text = answer.text,
        borderColor = colors.border,
        containerColor = colors.container,
        tags = if (label != null && colors.tagColor != null) {
            { QuestionAnswerTag(text = label, color = colors.tagColor) }
        } else {
            null
        },
    )
}

@Composable
internal fun MissingReviewQuestion(
    questionId: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            stringResource(Res.string.assessment_review_missing_question, questionId),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(AppSpacing.Grouped),
        )
    }
}
