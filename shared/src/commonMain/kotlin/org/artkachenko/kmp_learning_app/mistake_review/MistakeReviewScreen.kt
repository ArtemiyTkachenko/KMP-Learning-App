package org.artkachenko.kmp_learning_app.mistake_review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.mistake_review_description
import kmp_learning_app.shared.generated.resources.mistake_review_empty
import kmp_learning_app.shared.generated.resources.mistake_review_empty_action
import kmp_learning_app.shared.generated.resources.mistake_review_empty_detail
import kmp_learning_app.shared.generated.resources.mistake_review_error
import kmp_learning_app.shared.generated.resources.mistake_review_loading
import kmp_learning_app.shared.generated.resources.mistake_review_practice_all
import kmp_learning_app.shared.generated.resources.mistake_review_study_lesson
import kmp_learning_app.shared.generated.resources.mistake_review_title
import kmp_learning_app.shared.generated.resources.mistake_review_unresolved_count
import org.artkachenko.kmp_learning_app.assessment.AllQuestionLevels
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import kmp_learning_app.shared.generated.resources.practice_shortcut_subtopic_mistakes
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment_review.MissingReviewQuestion
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionCard
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.assessment_review.reviewSaveAction
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionsState
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.ScreenAction
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.artkachenko.kmp_learning_app.ui.theme.AppContentWidth
import org.artkachenko.kmp_learning_app.ui.theme.AppScreenPane
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListScope
import org.artkachenko.kmp_learning_app.ui.AppTwoPaneRow
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppWindowSizeClass

internal const val MistakeReviewLoadingTag = "mistake_review_loading"
internal const val MistakeReviewPracticeAllTag = "mistake_review_practice_all"

/** The two panes of the expanded queue, named for the same reason the Progress panes are. */
internal const val MistakeRemediationPaneTag = "mistake_remediation_pane"
internal const val MistakeQueuePaneTag = "mistake_queue_pane"

/** The queue is the substance of the screen; the standing offer beside it is three lines. */
private const val RemediationPaneWeight = 2f
private const val QueuePaneWeight = 3f

/** Stable per-entry handle for the scoped practice shortcut, whose label repeats on every card. */
internal fun mistakePracticeShortcutTag(questionId: String): String =
    "mistake_review_practice_$questionId"

/**
 * [onPracticePreset] carries the Subtopic the tapped entry already belongs to, together with the
 * existing unresolved-mistake source. The queue itself is unchanged: which Questions are unresolved
 * remains `UnresolvedMistakeDerivation`'s answer, and which of a Subtopic's unresolved Questions are
 * currently eligible remains the selector's.
 */
@Composable
internal fun MistakeReviewScreen(
    state: MistakeReviewUiState,
    onBack: (() -> Unit)? = null,
    onRetry: () -> Unit,
    onBrowseTopics: () -> Unit,
    onSourceClick: (String) -> Unit,
    onPracticePreset: (PracticePreset) -> Unit,
    onStartPractice: (AssessmentConfig.Focused) -> Unit = {},
    onStudyLesson: (MistakeStudyLesson) -> Unit = {},
    savedQuestions: SavedQuestionsState = SavedQuestionsState.Loading,
    onToggleSaved: (String) -> Unit = {},
    failedSourceUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = rememberAppTopBarScrollBehavior()
    Column(modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        AppTopBar(stringResource(Res.string.mistake_review_title), onBack, scrollBehavior)
        AppScreenPane(AppContentWidth.Paned) {
            when (state) {
                MistakeReviewUiState.Loading -> ScreenLoading(
                    message = stringResource(Res.string.mistake_review_loading),
                    testTag = MistakeReviewLoadingTag,
                    modifier = Modifier.weight(1f),
                )
                MistakeReviewUiState.Empty -> ScreenAction(
                    message = stringResource(Res.string.mistake_review_empty),
                    actionLabel = stringResource(Res.string.mistake_review_empty_action),
                    onAction = onBrowseTopics,
                    modifier = Modifier.weight(1f),
                    detail = stringResource(Res.string.mistake_review_empty_detail),
                    icon = AppIcons.CheckCircle,
                    iconTint = AppThemeExtras.semanticColors.correct,
                )
                MistakeReviewUiState.Error -> ScreenError(
                    message = stringResource(Res.string.mistake_review_error),
                    onRetry = onRetry,
                    modifier = Modifier.weight(1f),
                )
                is MistakeReviewUiState.Content -> MistakeReviewContent(
                    state = state,
                    onSourceClick = onSourceClick,
                    onPracticePreset = onPracticePreset,
                    onStartPractice = onStartPractice,
                    onStudyLesson = onStudyLesson,
                    savedQuestions = savedQuestions,
                    onToggleSaved = onToggleSaved,
                    failedSourceUrl = failedSourceUrl,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * The queue, with its remediation offer beside it or above it.
 *
 * The screen has two parts and they behave differently: the count, what an unresolved mistake
 * means, and the one control that practises the whole queue are short and stay true while the
 * learner works; the queue itself is long, and entries leave it as they are resolved. On a phone
 * they share a scroll and the offer is at the top. On a desktop the offer stays put beside the
 * queue, which is what makes "practise all of these" available at the bottom of a long list
 * instead of a scroll away.
 *
 * It is not a mail client. There is no selection, no detail pane, no filtering and no sorting: the
 * queue is already in the domain's order, every entry is already expanded into a full review card,
 * and the per-entry scoped practice shortcut is on the entry it belongs to. The second pane holds
 * what the top of the single column holds, and nothing that did not exist before.
 */
@Composable
private fun MistakeReviewContent(
    state: MistakeReviewUiState.Content,
    onSourceClick: (String) -> Unit,
    onPracticePreset: (PracticePreset) -> Unit,
    onStartPractice: (AssessmentConfig.Focused) -> Unit,
    onStudyLesson: (MistakeStudyLesson) -> Unit,
    savedQuestions: SavedQuestionsState,
    onToggleSaved: (String) -> Unit,
    failedSourceUrl: String?,
    modifier: Modifier,
) {
    val practiceableMistakes = state.mistakes.mapNotNull { mistake ->
        (mistake.reviewItem as? ReviewQuestionItem.Available)?.question
            ?.takeIf { it.subtopicId.isNotBlank() }
    }
    val practiceSubtopicIds = practiceableMistakes.mapTo(mutableSetOf()) { it.subtopicId }

    if (LocalAppWindowSizeClass.current.isExpanded) {
        AppTwoPaneRow(
            modifier = modifier,
            primary = {
                MistakePane(Modifier.weight(RemediationPaneWeight).testTag(MistakeRemediationPaneTag)) {
                    remediationSection(
                        mistakeCount = state.mistakes.size,
                        practiceableMistakeCount = practiceableMistakes.size,
                        practiceSubtopicIds = practiceSubtopicIds,
                        onStartPractice = onStartPractice,
                    )
                }
            },
            secondary = {
                MistakePane(Modifier.weight(QueuePaneWeight).testTag(MistakeQueuePaneTag)) {
                    queueSection(
                        state = state,
                        onSourceClick = onSourceClick,
                        onPracticePreset = onPracticePreset,
                        onStudyLesson = onStudyLesson,
                        savedQuestions = savedQuestions,
                        onToggleSaved = onToggleSaved,
                        failedSourceUrl = failedSourceUrl,
                    )
                }
            },
        )
        return
    }
    MistakePane(modifier) {
        remediationSection(
            mistakeCount = state.mistakes.size,
            practiceableMistakeCount = practiceableMistakes.size,
            practiceSubtopicIds = practiceSubtopicIds,
            onStartPractice = onStartPractice,
        )
        queueSection(
            state = state,
            onSourceClick = onSourceClick,
            onPracticePreset = onPracticePreset,
            onStudyLesson = onStudyLesson,
            savedQuestions = savedQuestions,
            onToggleSaved = onToggleSaved,
            failedSourceUrl = failedSourceUrl,
        )
    }
}

/** One column of the queue screen, with the same padding and rhythm in either arrangement. */
@Composable
private fun MistakePane(
    modifier: Modifier,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxHeight(),
        contentPadding = appScreenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Comfortable),
        content = content,
    )
}

/** How many are outstanding, what that means, and the one way to work through all of them. */
private fun LazyListScope.remediationSection(
    mistakeCount: Int,
    practiceableMistakeCount: Int,
    practiceSubtopicIds: Set<String>,
    onStartPractice: (AssessmentConfig.Focused) -> Unit,
) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Related)) {
            Text(
                text = stringResource(
                    Res.string.mistake_review_unresolved_count,
                    mistakeCount,
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(Res.string.mistake_review_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (practiceableMistakeCount > 0) {
                Button(
                    onClick = {
                        onStartPractice(
                            AssessmentConfig.Focused(
                                scope = AssessmentScope.Subtopics(practiceSubtopicIds),
                                questionCount = practiceableMistakeCount,
                                levels = AllQuestionLevels,
                                source = PracticeQuestionSource.UNRESOLVED_MISTAKES,
                            ),
                        )
                    },
                    modifier = Modifier.testTag(MistakeReviewPracticeAllTag),
                ) {
                    Text(
                        pluralStringResource(
                            Res.plurals.mistake_review_practice_all,
                            practiceableMistakeCount,
                            practiceableMistakeCount,
                        ),
                    )
                }
            }
        }
    }
}

/** The queue itself, in the domain's order. */
private fun LazyListScope.queueSection(
    state: MistakeReviewUiState.Content,
    onSourceClick: (String) -> Unit,
    onPracticePreset: (PracticePreset) -> Unit,
    onStudyLesson: (MistakeStudyLesson) -> Unit,
    savedQuestions: SavedQuestionsState,
    onToggleSaved: (String) -> Unit,
    failedSourceUrl: String?,
) {
    // Review rendering is reused from the shared assessment-review components so selected
    // answers, correct answers, explanation, and sources stay consistent with result screens.
    // A mistake leaves this list the moment it is answered correctly elsewhere, so entries are
    // genuinely removed while the learner is looking at them. Animating the removal is what
    // shows which one resolved; without it the remaining cards simply jump up a slot.
    items(state.mistakes, key = UnresolvedMistake::questionId) { mistake ->
        when (val item = mistake.reviewItem) {
            is ReviewQuestionItem.Available -> Column(
                modifier = Modifier.animateItem(),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
            ) {
                ReviewQuestionCard(
                    question = item.question,
                    onSourceClick = onSourceClick,
                    failedSourceUrl = failedSourceUrl,
                    // The screen is titled "N unresolved mistakes to review" and every entry
                    // under it is one, so the card does not repeat that verdict per row. A
                    // partially correct answer still earns its badge: that is a different fact
                    // from the one the heading states.
                    statesOutcome = false,
                    // Saving is learner intent about this Question, independent of the scoped
                    // practice shortcut below and of whether the mistake is still unresolved.
                    saveAction = savedQuestions.reviewSaveAction(
                        questionId = item.question.questionId,
                        onToggleSaved = onToggleSaved,
                    ),
                )
                // Secondary to the explanation above it, and offered per entry rather than for
                // the queue as a whole: this Question names its own Subtopic, so the scope is
                // read off the card the learner is looking at instead of being ranked out of
                // the queue. The clicked Question is context, not a candidate list — nothing
                // about which Questions the run will draw travels with it.
                if (item.question.subtopicId.isNotBlank()) {
                    TextButton(
                        onClick = {
                            onPracticePreset(
                                PracticePreset(
                                    scope = AssessmentScope.Subtopic(item.question.subtopicId),
                                    source = PracticeQuestionSource.UNRESOLVED_MISTAKES,
                                ),
                            )
                        },
                        modifier = Modifier.testTag(
                            mistakePracticeShortcutTag(item.question.questionId),
                        ),
                    ) {
                        Text(
                            text = stringResource(
                                Res.string.practice_shortcut_subtopic_mistakes,
                            ),
                        )
                    }
                }
                mistake.studyLesson?.let { lesson ->
                    TextButton(onClick = { onStudyLesson(lesson) }) {
                        Text(
                            stringResource(
                                Res.string.mistake_review_study_lesson,
                                lesson.title,
                            ),
                        )
                    }
                }
            }
            // A Question the curriculum no longer holds cannot name a current scope, so it gets
            // no shortcut rather than one built from metadata that is not there.
            is ReviewQuestionItem.Missing -> MissingReviewQuestion(
                questionId = item.questionId,
                modifier = Modifier.animateItem(),
            )
        }
    }
}
