package org.artkachenko.kmp_learning_app.assessment_review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
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
import kmp_learning_app.shared.generated.resources.assessment_review_accuracy_caption
import kmp_learning_app.shared.generated.resources.assessment_review_correct
import kmp_learning_app.shared.generated.resources.assessment_review_correct_answer
import kmp_learning_app.shared.generated.resources.assessment_review_correctly_selected
import kmp_learning_app.shared.generated.resources.assessment_review_collapse
import kmp_learning_app.shared.generated.resources.assessment_review_expand
import kmp_learning_app.shared.generated.resources.assessment_review_incorrect
import kmp_learning_app.shared.generated.resources.assessment_review_incorrectly_selected
import kmp_learning_app.shared.generated.resources.assessment_review_missing_question
import kmp_learning_app.shared.generated.resources.assessment_review_interview_complete
import kmp_learning_app.shared.generated.resources.assessment_review_practice_complete
import kmp_learning_app.shared.generated.resources.assessment_review_mistakes_retained
import kmp_learning_app.shared.generated.resources.assessment_review_missed
import kmp_learning_app.shared.generated.resources.assessment_review_practice_mistakes
import kmp_learning_app.shared.generated.resources.assessment_review_partially_correct
import kmp_learning_app.shared.generated.resources.assessment_review_save_question
import kmp_learning_app.shared.generated.resources.assessment_review_saved_state
import kmp_learning_app.shared.generated.resources.assessment_review_unsaved_state
import kmp_learning_app.shared.generated.resources.assessment_review_score
import kmp_learning_app.shared.generated.resources.assessment_review_selected
import kmp_learning_app.shared.generated.resources.assessment_review_unresolved_questions
import kmp_learning_app.shared.generated.resources.assessment_review_unsave_question
import org.artkachenko.kmp_learning_app.ui.AccuracyHeadline
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.assessment.AllQuestionLevels
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.ui.PrimarySummaryCard
import org.artkachenko.kmp_learning_app.ui.StatusBadge
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AssessmentScoreSummary(
    correctAnswers: Int,
    totalQuestions: Int,
    percentage: Double,
    title: String? = null,
    modifier: Modifier = Modifier,
) {
    PrimarySummaryCard(modifier) {
        title?.let {
            Text(it, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        if (totalQuestions < MeaningfulPercentageQuestionCount) {
            Text(
                text = stringResource(
                    Res.string.assessment_review_score,
                    correctAnswers,
                    totalQuestions,
                ),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            AccuracyHeadline(
                percentage = percentage,
                caption = stringResource(Res.string.assessment_review_accuracy_caption),
                supporting = stringResource(
                    Res.string.assessment_review_score,
                    correctAnswers,
                    totalQuestions,
                ),
            )
        }
    }
}

private const val MeaningfulPercentageQuestionCount = 5

@Composable
internal fun MistakeRetentionNotice(
    questions: List<ReviewQuestionItem>,
    onPracticeMistakes: ((AssessmentConfig.Focused) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val retainedQuestions = questions.mapNotNull { item ->
        (item as? ReviewQuestionItem.Available)?.question?.takeIf { !it.isCorrect }
    }
    val retained = retainedQuestions.size
    if (retained > 0) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.Related)) {
            Text(
                text = org.jetbrains.compose.resources.pluralStringResource(
                    Res.plurals.assessment_review_mistakes_retained,
                    retained,
                    retained,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = AppThemeExtras.semanticColors.partiallyCorrect,
            )
            if (onPracticeMistakes != null) {
                Button(
                    onClick = {
                        onPracticeMistakes(
                            AssessmentConfig.Focused(
                                scope = AssessmentScope.Subtopics(
                                    retainedQuestions.mapTo(linkedSetOf()) { it.subtopicId },
                                ),
                                questionCount = retained,
                                levels = AllQuestionLevels,
                                source = PracticeQuestionSource.UNRESOLVED_MISTAKES,
                            ),
                        )
                    },
                ) {
                    Text(
                        org.jetbrains.compose.resources.pluralStringResource(
                            Res.plurals.assessment_review_practice_mistakes,
                            retained,
                            retained,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Explains why the score above can exceed the questions listed below.
 *
 * [AssessmentScoreSummary] renders the persisted AssessmentScore and stays
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

/** How a reviewed answer relates to the authored correct set, for colouring only. */
private enum class AnswerOutcome { CORRECT, MISSED, WRONG, NEUTRAL }

/** Overall outcome of one reviewed question. */
private enum class QuestionOutcome { CORRECT, PARTIAL, INCORRECT }

/**
 * Derived purely from what the review model already carries. Persisted correctness stays
 * authoritative for [QuestionOutcome.CORRECT]; the partial case only refines how a question that
 * was *scored incorrect* is presented, so no scoring behaviour changes.
 */
private fun ReviewQuestionUiModel.outcome(): QuestionOutcome {
    if (isCorrect) return QuestionOutcome.CORRECT
    val pickedWrong = answers.any { it.wasSelected && !it.isCorrectAnswer }
    val pickedAnyCorrect = answers.any { it.wasSelected && it.isCorrectAnswer }
    return if (!pickedWrong && pickedAnyCorrect) QuestionOutcome.PARTIAL else QuestionOutcome.INCORRECT
}

private fun ReviewAnswerUiModel.outcome(): AnswerOutcome = when {
    wasSelected && isCorrectAnswer -> AnswerOutcome.CORRECT
    wasSelected -> AnswerOutcome.WRONG
    isCorrectAnswer -> AnswerOutcome.MISSED
    else -> AnswerOutcome.NEUTRAL
}

/**
 * [saveAction] is optional so this component stays usable where saved state is unknown, or where a
 * surface has no saving to offer at all. Everything else about the card is unchanged by it: the
 * action sits beside the heading and leaves the outcome, answers, explanation, and source links
 * exactly as they were.
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
) {
    var expanded by rememberSaveable(question.questionId) { mutableStateOf(!question.isCorrect) }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    stringResource(
                        if (expanded) {
                            Res.string.assessment_review_collapse
                        } else {
                            Res.string.assessment_review_expand
                        },
                    ),
                )
            }
            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Related)) {
                    question.answers.forEach { ReviewAnswerRow(it) }
                }
                QuestionExplanationBlock(question.explanation)
                QuestionSources(
                    sources = question.sources,
                    onSourceClick = onSourceClick,
                    failedSourceUrl = failedSourceUrl,
                )
            }
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
    val semantic = AppThemeExtras.semanticColors
    val (text, content, container) = when (outcome) {
        QuestionOutcome.CORRECT -> Triple(
            stringResource(Res.string.assessment_review_correct),
            semantic.onCorrectContainer,
            semantic.correctContainer,
        )
        QuestionOutcome.PARTIAL -> Triple(
            stringResource(Res.string.assessment_review_partially_correct),
            semantic.onPartiallyCorrectContainer,
            semantic.partiallyCorrectContainer,
        )
        QuestionOutcome.INCORRECT -> Triple(
            stringResource(Res.string.assessment_review_incorrect),
            semantic.onIncorrectContainer,
            semantic.incorrectContainer,
        )
    }

    StatusBadge(text = text, contentColor = content, containerColor = container)
}

/**
 * One answered option: the shared option container, coloured and labelled by how this attempt's
 * selection related to the authored correct set.
 */
@Composable
private fun ReviewAnswerRow(answer: ReviewAnswerUiModel) {
    val semantic = AppThemeExtras.semanticColors
    val outcome = answer.outcome()
    val border = when (outcome) {
        AnswerOutcome.CORRECT, AnswerOutcome.MISSED -> semantic.correct
        AnswerOutcome.WRONG -> semantic.incorrect
        AnswerOutcome.NEUTRAL -> MaterialTheme.colorScheme.outlineVariant
    }
    val container = when (outcome) {
        AnswerOutcome.CORRECT -> semantic.correctContainer
        AnswerOutcome.WRONG -> semantic.incorrectContainer
        AnswerOutcome.MISSED, AnswerOutcome.NEUTRAL -> MaterialTheme.colorScheme.surface
    }

    val label = when (outcome) {
        AnswerOutcome.CORRECT -> stringResource(Res.string.assessment_review_correctly_selected)
        AnswerOutcome.MISSED -> stringResource(Res.string.assessment_review_missed)
        AnswerOutcome.WRONG -> stringResource(Res.string.assessment_review_incorrectly_selected)
        AnswerOutcome.NEUTRAL -> null
    }
    QuestionAnswerOption(
        text = answer.text,
        borderColor = border,
        containerColor = container,
        tags = if (label != null) {
            {
                QuestionAnswerTag(
                    text = label,
                    color = if (outcome == AnswerOutcome.WRONG) semantic.incorrect else semantic.correct,
                )
            }
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
            modifier = Modifier.padding(12.dp),
        )
    }
}
