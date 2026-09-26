package org.artkachenko.kmp_learning_app.mistake_review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
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
import kmp_learning_app.shared.generated.resources.mistake_review_unavailable
import kmp_learning_app.shared.generated.resources.mistake_review_unresolved_count
import kmp_learning_app.shared.generated.resources.mistake_review_unresolved_unit
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
import org.artkachenko.kmp_learning_app.ui.MetricFigure
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.ScreenAction
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenStateTransition
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.artkachenko.kmp_learning_app.ui.theme.AppContentWidth
import org.artkachenko.kmp_learning_app.ui.theme.AppScreenPane
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListScope
import org.artkachenko.kmp_learning_app.ui.AppTwoPaneRow
import org.artkachenko.kmp_learning_app.ui.theme.AppMotion
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppWindowSizeClass

internal const val MistakeReviewLoadingTag = "mistake_review_loading"
internal const val MistakeReviewPracticeAllTag = "mistake_review_practice_all"

/** The level-2 block the screen leads with, so a test can reach it without matching its lines. */
internal const val MistakeRemediationSurfaceTag = "mistake_remediation_surface"

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
            // The queue is read from disk, so the screen always opens on the spinner and
            // replaces it a moment later, and swapping in one frame is what made the queue appear
            // to drop into place underneath the learner. `ScreenStateTransition` is the app's
            // answer to that and Progress already uses it; its default `contentKey` is the state's
            // class, which is the behaviour this screen needs — resolving a mistake hands it a new
            // `Content` with one fewer entry, and fading the whole queue out and back in would
            // claim the list arrived when a single row left it. That removal is already told by
            // `animateItem` on the row itself.
            ScreenStateTransition(state = state, modifier = Modifier.fillMaxSize()) { current ->
                when (current) {
                    MistakeReviewUiState.Loading -> ScreenLoading(
                        message = stringResource(Res.string.mistake_review_loading),
                        testTag = MistakeReviewLoadingTag,
                        modifier = Modifier.fillMaxSize(),
                    )
                    MistakeReviewUiState.Empty -> ScreenAction(
                        message = stringResource(Res.string.mistake_review_empty),
                        actionLabel = stringResource(Res.string.mistake_review_empty_action),
                        onAction = onBrowseTopics,
                        modifier = Modifier.fillMaxSize(),
                        detail = stringResource(Res.string.mistake_review_empty_detail),
                        icon = AppIcons.CheckCircle,
                        iconTint = AppThemeExtras.semanticColors.correct,
                    )
                    MistakeReviewUiState.Error -> ScreenError(
                        message = stringResource(Res.string.mistake_review_error),
                        onRetry = onRetry,
                        modifier = Modifier.fillMaxSize(),
                    )
                    is MistakeReviewUiState.Content -> MistakeReviewContent(
                        state = current,
                        onSourceClick = onSourceClick,
                        onPracticePreset = onPracticePreset,
                        onStartPractice = onStartPractice,
                        onStudyLesson = onStudyLesson,
                        savedQuestions = savedQuestions,
                        onToggleSaved = onToggleSaved,
                        failedSourceUrl = failedSourceUrl,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
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
    val practiceTarget = remember(state.mistakes) {
        state.mistakes.toPracticeTarget()
    }

    if (LocalAppWindowSizeClass.current.isExpanded) {
        AppTwoPaneRow(
            modifier = modifier,
            primary = {
                MistakePane(Modifier.weight(RemediationPaneWeight).testTag(MistakeRemediationPaneTag)) {
                    remediationSection(
                        mistakeCount = state.mistakes.size,
                        practiceableMistakeCount = practiceTarget.questionCount,
                        practiceSubtopicIds = practiceTarget.subtopicIds,
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
    // Tagged as the queue pane in this arrangement too: there is one list here and it is the one
    // the queue scrolls in, so a test that scrolls to an entry addresses the same handle whichever
    // arrangement it is running in. Lazy items outside the viewport are not composed, so reaching a
    // later entry is a scroll on this node rather than a search of the whole tree.
    MistakePane(modifier.testTag(MistakeQueuePaneTag)) {
        remediationSection(
            mistakeCount = state.mistakes.size,
            practiceableMistakeCount = practiceTarget.questionCount,
            practiceSubtopicIds = practiceTarget.subtopicIds,
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

private data class MistakePracticeTarget(
    val questionCount: Int,
    val subtopicIds: Set<String>,
)

private fun List<UnresolvedMistake>.toPracticeTarget(): MistakePracticeTarget {
    var questionCount = 0
    val subtopicIds = mutableSetOf<String>()
    forEach { mistake ->
        val question = (mistake.reviewItem as? ReviewQuestionItem.Available)?.question
        if (question != null && question.subtopicId.isNotBlank()) {
            questionCount += 1
            subtopicIds += question.subtopicId
        }
    }
    return MistakePracticeTarget(questionCount, subtopicIds)
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

/**
 * How many are outstanding, what that means, and the one way to work through all of them.
 *
 * ## Why this is a level-2 surface
 *
 * It was four loose pieces of type on the page background — the quietest rank the vocabulary has,
 * which is the rank [surface hierarchy](../../../../../../../docs/development/surface-hierarchy.md)
 * reserves for a record or a sequence. But this is the screen's conclusion and the only thing on it
 * holding a primary action, sitting above a column of level-1 review cards that are each visually
 * louder than it. The screen's own subject line was being outranked by every entry beneath it.
 *
 * The rank it takes is the Practice Builder's, for the same reason and in the same words:
 * `surfaceContainer` behind a hairline `outlineVariant` edge, one step below `AccuracyHeroCard`
 * and with no shadow. Level 2 is defined as "the one surface on a screen that outranks the rest",
 * and after this change that is exactly what it is — the only filled, unbordered, non-selectable
 * surface here, and the only one that starts anything. The two screens are also doing the same
 * job: a block that states what a run will contain and holds the control that begins it.
 *
 * ## Why it is not the brand gradient
 *
 * The gradient is for a surface that is the whole reason its destination exists, and here that is
 * the **queue**, not the offer. A learner opens Mistakes to work through the entries; the count and
 * the standing offer are three lines that stay true while they do. Promoting the offer to a hero
 * would claim the screen is about the summary of the thing rather than the thing, and it would put
 * the loudest surface in the app above a list the learner is going to scroll past it. Moving the
 * top up is not always the fix — here the offer needed a container, not a crown.
 *
 * ## The ranks inside it
 *
 * Figure, then what it means, then the caveat when there is one, then the action. The count was a
 * whole sentence at `titleLarge`, which made the one scannable number on the screen something to
 * read rather than something to see; it now leads as a figure over its unit, the shape the builder's
 * summary and both heroes already use. The pair is announced as the sentence it replaced, so heading
 * navigation still lands on "N unresolved mistakes to review" and hears it as one fact rather than
 * as "3" followed by "unresolved mistakes".
 */
private fun LazyListScope.remediationSection(
    mistakeCount: Int,
    practiceableMistakeCount: Int,
    practiceSubtopicIds: Set<String>,
    onStartPractice: (AssessmentConfig.Focused) -> Unit,
) {
    item {
        RemediationSurface {
            OutstandingCount(mistakeCount)
            Text(
                text = stringResource(Res.string.mistake_review_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Why the button below can offer fewer than the count above it. The two figures
            // differ whenever a curriculum import drops a Question the history still refers to,
            // and without this the learner reads "12 unresolved mistakes" over a button offering
            // to practise nine and has no way to account for the other three. The same caveat the
            // result screens state, in the same warning tone — this is missing content, not a
            // failure.
            if (practiceableMistakeCount < mistakeCount) {
                Text(
                    text = stringResource(
                        Res.string.mistake_review_unavailable,
                        mistakeCount - practiceableMistakeCount,
                        mistakeCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppThemeExtras.semanticColors.partiallyCorrect,
                )
            }
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

/**
 * The screen's conclusion, on the one surface that outranks the queue; see [remediationSection].
 *
 * The entrance is tied to the destination's presentation lifecycle rather than to the queue's
 * contents. A mistake leaves the list the moment it is answered correctly elsewhere, so this block
 * recomposes with a new count while the learner is looking at it — and a surface that slid in again
 * every time the count dropped would announce an arrival when nothing arrived. The flag is
 * `rememberSaveable` for the same reason the interview invitation's is: it survives the
 * recompositions and the configuration changes that are not the screen being opened.
 */
@Composable
private fun RemediationSurface(content: @Composable ColumnScope.() -> Unit) {
    // Seeded false and flipped to true is how an `AnimatedVisibility` animates its *first*
    // composition; `visible = true` would simply draw the block already there.
    var alreadyRevealed by rememberSaveable { mutableStateOf(false) }
    val entrance = remember { MutableTransitionState(alreadyRevealed) }
    entrance.targetState = true
    // Claimed from an effect rather than from the composition body: the seed above reads this
    // value, and writing a state the same composition reads is how a recomposition loop starts.
    LaunchedEffect(Unit) { alreadyRevealed = true }

    AnimatedVisibility(
        visibleState = entrance,
        enter = fadeIn(AppMotion.revealSpec()) +
            slideInVertically(AppMotion.spatialSpec()) { height -> height / EntranceRiseFraction },
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().testTag(MistakeRemediationSurfaceTag),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(RemediationBorderWidth, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(AppSpacing.Generous),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Comfortable),
                content = content,
            )
        }
    }
}

/**
 * What the queue currently holds, as a figure rather than as a sentence.
 *
 * The screen's own subject line, so it publishes `heading()` for the reason every screen's subject
 * line does: the bar above says which screen this is, and this says what this instance of it holds.
 *
 * It is deliberately not counted up. The figure is a standing fact about the queue that was true
 * before the learner arrived, not a measurement just taken — and it changes downwards as entries
 * resolve, where a count-up would animate in the wrong direction and read as mistakes accumulating.
 *
 * The colour is named rather than inherited. It resolved correctly through `LocalContentColor` —
 * the shell's `Scaffold` supplies `onBackground` — but it was the one styled `Text` in the app whose
 * colour depended on an ambient the screen does not control, and it renders black outside that
 * shell, as a preview or an isolated test composes it.
 */
@Composable
private fun OutstandingCount(mistakeCount: Int) {
    val spoken = pluralStringResource(
        Res.plurals.mistake_review_unresolved_count,
        mistakeCount,
        mistakeCount,
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
        // Announced as the one sentence it replaced, and still a heading. Split across two nodes a
        // screen reader would read "3" and "unresolved mistakes" as unrelated fragments, and
        // heading navigation would land on a bare numeral.
        modifier = Modifier.clearAndSetSemantics {
            heading()
            text = AnnotatedString(spoken)
        },
    ) {
        MetricFigure(
            text = mistakeCount.toString(),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = pluralStringResource(
                Res.plurals.mistake_review_unresolved_unit,
                mistakeCount,
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            is ReviewQuestionItem.Available -> ReviewQuestionCard(
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
                modifier = Modifier.animateItem(),
            ) {
                // Inside the card, in a row that wraps. These two were siblings of the card, so on
                // a phone they were two full-width text links on the page background between
                // entries and on a desktop they sat in the gutter — either way reading as
                // navigation for the screen rather than as what this Question offers. A `FlowRow`
                // because the lesson label is an authored title of unknown length: side by side
                // where they fit, stacked where they do not.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.Related),
                ) {
                    // Offered per entry rather than for the queue as a whole: this Question names
                    // its own Subtopic, so the scope is read off the card the learner is looking at
                    // instead of being ranked out of the queue. The clicked Question is context,
                    // not a candidate list — nothing about which Questions the run will draw
                    // travels with it.
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

/** An edge, not an outline: the hairline that separates the block from the page. */
private val RemediationBorderWidth = 1.dp

/**
 * The block rises by a fraction of its own height, so the distance suits the surface rather than
 * being a fixed offset that reads as a long slide on a short block and a twitch on a tall one.
 */
private const val EntranceRiseFraction = 6
