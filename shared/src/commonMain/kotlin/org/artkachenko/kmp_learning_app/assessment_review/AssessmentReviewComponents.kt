package org.artkachenko.kmp_learning_app.assessment_review

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.assessment_review_collapse
import kmp_learning_app.shared.generated.resources.assessment_review_expand
import kmp_learning_app.shared.generated.resources.assessment_review_missing_question
import kmp_learning_app.shared.generated.resources.assessment_review_interview_complete
import kmp_learning_app.shared.generated.resources.assessment_review_practice_complete
import kmp_learning_app.shared.generated.resources.assessment_review_mistakes_retained
import kmp_learning_app.shared.generated.resources.assessment_review_save_question
import kmp_learning_app.shared.generated.resources.assessment_review_saved_state
import kmp_learning_app.shared.generated.resources.assessment_review_unsaved_state
import kmp_learning_app.shared.generated.resources.assessment_review_selected
import kmp_learning_app.shared.generated.resources.assessment_review_unresolved_questions
import kmp_learning_app.shared.generated.resources.assessment_review_unsave_question
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.theme.AppMotion
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
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
                    question.text,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (saveAction != null) {
                    SaveQuestionAction(questionId = question.questionId, action = saveAction)
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
 * The bookmark control: one affordance whose two states are distinguishable three ways over.
 *
 * It was a bare "Save"/"Unsave" text button, which is unambiguous but looks like a command rather
 * than a state and reads as a different control each time it is pressed. It is now the conventional
 * bookmark — a filled ribbon when the Question is saved, an outlined one when it is not — with the
 * word kept beside it, because an icon alone would leave the state readable only to someone who
 * knows which ribbon means which. Shape, fill, and the word all change together, so the state is
 * never carried by colour alone or by shape alone.
 *
 * The accessible reading is deliberately split across two properties. The visible label is the
 * *action* ("Save" / "Saved"), which is what Material's button semantics announce, and the current
 * value is published separately as `stateDescription` and as `toggleableState` — so a screen reader
 * says what is true now as well as what pressing it will do, which a label alone cannot express.
 *
 * Disabled only while this Question's own mutation is being persisted; the icon and label keep
 * showing the stored value throughout, so a pending save never draws as though it had been written.
 */
@Composable
private fun SaveQuestionAction(
    questionId: String,
    action: ReviewSaveAction,
) {
    val stateLabel = stringResource(
        if (action.isSaved) {
            Res.string.assessment_review_saved_state
        } else {
            Res.string.assessment_review_unsaved_state
        },
    )
    TextButton(
        onClick = action.onToggle,
        enabled = !action.isPending,
        modifier = Modifier
            .testTag(reviewQuestionSaveTag(questionId))
            .semantics {
                stateDescription = stateLabel
                toggleableState = ToggleableState(action.isSaved)
            },
    ) {
        Icon(
            imageVector = if (action.isSaved) AppIcons.Bookmark else AppIcons.BookmarkBorder,
            contentDescription = null,
            modifier = Modifier.size(SaveIconSize),
        )
        Text(
            text = stringResource(
                if (action.isSaved) {
                    Res.string.assessment_review_unsave_question
                } else {
                    Res.string.assessment_review_save_question
                },
            ),
            modifier = Modifier.padding(start = AppSpacing.Tight),
        )
    }
}

/** Matches the leading-icon size Material gives a text button. */
private val SaveIconSize = 18.dp

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
