package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.interview_history_attempts
import kmp_learning_app.shared.generated.resources.interview_history_best
import kmp_learning_app.shared.generated.resources.interview_history_empty_detail
import kmp_learning_app.shared.generated.resources.interview_history_empty_title
import kmp_learning_app.shared.generated.resources.interview_history_latest
import kmp_learning_app.shared.generated.resources.interview_history_score
import kmp_learning_app.shared.generated.resources.interview_history_title
import kmp_learning_app.shared.generated.resources.mixed_interview_description
import kmp_learning_app.shared.generated.resources.mixed_interview_how_it_works
import kmp_learning_app.shared.generated.resources.mixed_interview_mix_rule_title
import kmp_learning_app.shared.generated.resources.mixed_interview_question_count
import kmp_learning_app.shared.generated.resources.mixed_interview_question_count_unit
import kmp_learning_app.shared.generated.resources.mixed_interview_review_note
import kmp_learning_app.shared.generated.resources.mixed_interview_review_rule_title
import kmp_learning_app.shared.generated.resources.mixed_interview_start
import kmp_learning_app.shared.generated.resources.mixed_interview_title
import org.artkachenko.kmp_learning_app.ui.AccuracyRow
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.AppTwoPaneRow
import org.artkachenko.kmp_learning_app.ui.ContentGroup
import org.artkachenko.kmp_learning_app.ui.theme.AppContentPane
import org.artkachenko.kmp_learning_app.ui.theme.AppContentWidth
import org.artkachenko.kmp_learning_app.ui.theme.AppMotion
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppWindowSizeClass
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.artkachenko.kmp_learning_app.ui.time.timestampText
import org.jetbrains.compose.resources.stringResource

internal const val InterviewStartButtonTag = "interview_start"

internal const val InterviewRecordTag = "interview_record"

/** The gradient invitation itself, so a test can reach it without matching one of its many lines. */
internal const val InterviewHeroTag = "interview_hero"

/**
 * The single container the record's rows share, so a test can assert that the latest and the best
 * are one record rather than two stacked results.
 */
internal const val InterviewRecordGroupTag = "interview_record_group"

/** The deliberate first-visit state, so a test can tell it from an absent record. */
internal const val InterviewNoHistoryTag = "interview_no_history"
internal const val InterviewHistoryLoadingTag = "interview_history_loading"

/**
 * The mixed interview's own destination.
 *
 * It used to be a card competing for room on the topic list. On its own screen the call to action
 * can lead, and there is space to say what the interview actually is before starting one.
 */
@Composable
internal fun InterviewStartScreen(
    onStartMixedInterview: () -> Unit,
    modifier: Modifier = Modifier,
    history: InterviewHistoryUiState = InterviewHistoryUiState.Loading,
    onOpenResult: (String) -> Unit = {},
) {
    AppContentPane(
        width = AppContentWidth.Paned,
        modifier = modifier
            // This screen leads with its own heading instead of an AppTopBar, so there is no bar
            // here to pad for the status bar; without this the heading sits underneath it.
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
    ) {
        if (LocalAppWindowSizeClass.current.isExpanded) {
            // The landing page stays what it was — an invitation and a record of how it has gone
            // — and gains no features here. What an expanded window changes is only that the two
            // stop being stacked: side by side they fill the window without anything being
            // invented to fill it, where one narrow column centred in a desktop viewport read as
            // a small card adrift in a large empty page.
            AppTwoPaneRow(
                primary = {
                    InterviewPane(Modifier.weight(1f)) {
                        invitationSection(onStartMixedInterview)
                    }
                },
                secondary = {
                    InterviewPane(Modifier.weight(1f)) {
                        historySection(history = history, onOpenResult = onOpenResult)
                    }
                },
            )
            return@AppContentPane
        }
        // Scrollable rather than a fixed Column: with both a latest and a best result the heading,
        // invitation, explanation, and two record cards overflow a compact window or a large font
        // scale, and the lower cards were then unreachable.
        InterviewPane(Modifier.fillMaxSize()) {
            invitationSection(onStartMixedInterview)
            historySection(history = history, onOpenResult = onOpenResult)
        }
    }
}

/** One column of the landing page, with the same padding and rhythm in either arrangement. */
@Composable
private fun InterviewPane(
    modifier: Modifier,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxHeight(),
        contentPadding = appScreenContentPadding(
            top = AppSpacing.Section,
            bottom = AppSpacing.Section,
        ),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Comfortable),
        content = content,
    )
}

/** What an Interview is, and the one control that starts one. */
private fun LazyListScope.invitationSection(onStartMixedInterview: () -> Unit) {
    item { InterviewHero(onStartMixedInterview = onStartMixedInterview) }
}

/**
 * The invitation, as the one object this destination exists for.
 *
 * ## Why this is the gradient's third call site
 *
 * `AppSemanticColors` reserves the brand sweep for "the rare surface that is the single most
 * important thing on its screen", and requires a new adopter to make the argument rather than
 * inherit it. The argument here is the same one `ProgressHero` made, read from the other end.
 * Nothing on this page competes: there is exactly one other object, the record, and it is a bounded
 * supporting set in a single [ContentGroup] — a rank below by construction, not by tone. And a
 * learner who chose **Interview** in the navigation bar came for precisely one thing, which is the
 * surface that starts an interview. This was a `Card(primaryContainer)`, which in the light scheme
 * is byte-for-byte `heroGradientStart`: the screen was already sitting one flat fill away from the
 * hero language, with none of the lift, edge, or hierarchy that make it mean something.
 *
 * The three uses stay distinguishable by **motion**, which is the rule the token documents.
 * `AssessmentCompletionHero` is an *arrival* and counts a score out over the app's one celebratory
 * duration. `ProgressHero` is a *standing answer* and settles a measured figure into place. This
 * one animates no figure at all — twenty is a configuration fact that was true before the learner
 * arrived, and counting it up would dress a constant as a measurement — and it is the only one of
 * the three that holds the screen's primary action. An invitation is not a score card.
 *
 * ## The ranks inside it
 *
 * Mode, then figure, then purpose, then rules, then the action. The screen's heading moved *into*
 * the hero rather than sitting above it as a bare line, because a title outside the surface makes
 * the surface a card underneath a title; inside, the hero is the subject. It keeps its `heading()`
 * semantics, so the screen still has exactly one, in the same place in the reading order.
 *
 * The two behavioural rules used to be two of three visually identical `bodyMedium` paragraphs —
 * and the one about topic mixing was stranded *below* the button that starts the run it describes.
 * They are what actually separates an Interview from Practice, so they are drawn as labelled facts
 * under a divider instead of as prose. Neither states anything the strings did not already say.
 *
 * There is deliberately no decorative graphic. The rank is carried by the gradient, the edge, the
 * type ramp, and the reveal; a tonal shape behind all of that would be filling space that is not
 * empty.
 */
@Composable
private fun InterviewHero(onStartMixedInterview: () -> Unit) {
    val semantic = AppThemeExtras.semanticColors
    // The gradient carries no on-colour of its own; `onPrimaryContainer` is its documented
    // contract, asserted against both endpoints in `AppColorSchemeTest`. The muted share is the
    // same one `ProgressHero` uses, and `ProgressHeroThemeTest` already measures that composite
    // against all four endpoints — so this surface adds no contrast question of its own except the
    // filled action, which `InterviewStartHeroThemeTest` covers.
    val onHero = MaterialTheme.colorScheme.onPrimaryContainer
    val onHeroMuted = onHero.copy(alpha = SupportingTextAlpha)

    // Seeded false and flipped to true is how an `AnimatedVisibility` animates its *first*
    // composition; `visible = true` would simply draw the hero already there.
    //
    // The flag is `rememberSaveable`, which is what ties the entrance to the *presentation
    // lifecycle of the destination* rather than to anything the screen is showing. The record's
    // `StateFlow` re-emits on every lifecycle resume and on every settled history refresh, and the
    // hero sits in a `LazyColumn` that disposes it once it scrolls away; without the flag the
    // invitation would slide in again every time the history finished loading, every time a best
    // result changed, and every time the learner scrolled back up. None of those are the hero
    // arriving. Nothing about this belongs in the ViewModel.
    var alreadyRevealed by rememberSaveable { mutableStateOf(false) }
    val entrance = remember { MutableTransitionState(alreadyRevealed) }
    entrance.targetState = true
    // Claimed from an effect rather than from the composition body: the seed above reads this
    // value, and writing a state that the same composition reads is how a recomposition loop
    // starts. Both existing heroes claim theirs the same way.
    LaunchedEffect(Unit) { alreadyRevealed = true }

    Surface(
        modifier = Modifier.fillMaxWidth().testTag(InterviewHeroTag),
        shape = MaterialTheme.shapes.large,
        // Transparent, because the fill is a brush rather than a colour and `Surface` takes only
        // the latter. The shape still clips the gradient and still owns the elevation semantics.
        color = Color.Transparent,
        // The edge carries the separation in dark, where one tonal step is not enough and a shadow
        // is almost invisible; the shadow carries it in light, where the pale sweep sits close in
        // value to the page. Neither alone works in both schemes.
        border = BorderStroke(HeroBorderWidth, onHero.copy(alpha = HeroBorderAlpha)),
        shadowElevation = HeroElevation,
    ) {
        AnimatedVisibility(
            visibleState = entrance,
            enter = fadeIn(AppMotion.revealSpec()) +
                slideInVertically(AppMotion.spatialSpec()) { height -> height / EntranceRiseFraction },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(semantic.heroGradientStart, semantic.heroGradientEnd),
                        ),
                    )
                    .padding(AppSpacing.Generous),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Comfortable),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.Related),
                    // Top, not centre, for the same reason [InterviewRule] is: at a large type
                    // scale the destination's name wraps to two lines, and a centred glyph then
                    // floats against the gap between them instead of against the name it labels.
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = AppIcons.Interview,
                        // The destination's own glyph, beside the destination's own name. The name
                        // says it, so the glyph is decoration and is not announced a second time.
                        contentDescription = null,
                        tint = onHero,
                        modifier = Modifier.size(ModeIconSize),
                    )
                    Text(
                        text = stringResource(Res.string.mixed_interview_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = onHero,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                QuestionCountFigure(onHero = onHero, onHeroMuted = onHeroMuted)
                Text(
                    text = stringResource(Res.string.mixed_interview_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = onHeroMuted,
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.Comfortable),
                    // Settles in just behind the figure rather than with it, so the hero reads as
                    // one arrival with an order to it instead of everything appearing at once. The
                    // stagger is in the animation spec, not in a coroutine, so nothing is waiting
                    // on it and the rules are readable from the first frame.
                    modifier = Modifier.animateEnterExit(
                        enter = fadeIn(AppMotion.revealSpec(RulesRevealDelayMillis)),
                    ),
                ) {
                    HorizontalDivider(color = onHero.copy(alpha = HeroBorderAlpha))
                    InterviewRule(
                        icon = AppIcons.Topics,
                        title = stringResource(Res.string.mixed_interview_mix_rule_title),
                        detail = stringResource(Res.string.mixed_interview_how_it_works),
                        onHero = onHero,
                        onHeroMuted = onHeroMuted,
                    )
                    // The rule that makes an Interview different from Practice, stated before the
                    // learner commits to twenty questions rather than discovered on question one.
                    // Practice marks each answer as it is given; an Interview holds every verdict
                    // back until it is over, which is exactly the asymmetry this screen exists to
                    // make deliberate.
                    InterviewRule(
                        icon = AppIcons.CheckCircle,
                        title = stringResource(Res.string.mixed_interview_review_rule_title),
                        detail = stringResource(Res.string.mixed_interview_review_note),
                        onHero = onHero,
                        onHeroMuted = onHeroMuted,
                    )
                }
                Button(
                    onClick = onStartMixedInterview,
                    modifier = Modifier
                        // Full width where the hero is the content column, and its own width where
                        // the hero is half a desktop window. This pane is `weight(1f)` of the
                        // window rather than a column capped at a reading measure, so at an
                        // expanded width `fillMaxWidth` made the invitation's action a six-hundred
                        // pixel bar — wider than the sentence above it that explains what it does.
                        .then(
                            if (LocalAppWindowSizeClass.current.isExpanded) {
                                Modifier
                            } else {
                                Modifier.fillMaxWidth()
                            },
                        )
                        // Last in, and by a margin small enough that the whole entrance still lands
                        // inside 400ms. `fadeIn` animates alpha only, so the control is composed,
                        // hit-testable, and announced from the first frame — the motion never gates
                        // the one thing the learner came here to press.
                        .animateEnterExit(
                            enter = fadeIn(AppMotion.revealSpec(ActionRevealDelayMillis)),
                        )
                        .testTag(InterviewStartButtonTag),
                ) {
                    Text(text = stringResource(Res.string.mixed_interview_start))
                }
            }
        }
    }
}

/**
 * How many questions an interview is, as a figure rather than as a sentence.
 *
 * This was `MetricFigure("20-question interview")` — the one scannable fact on the screen buried
 * mid-string in a hyphenated compound, at a heading role. The numeral now leads at display scale
 * with its unit beneath it, which is the same shape the two other heroes give their figures.
 *
 * It is deliberately **not** counted up. The other heroes animate a number because the number is a
 * measurement that has just been taken; twenty was true before the learner opened the screen, and
 * counting a constant would dress configuration as a result.
 *
 * The pair is announced as one fact. Split across two nodes a screen reader would read "20" and
 * "questions" as unrelated fragments, so the figure and its unit are cleared and given the sentence
 * the app already had a string for.
 */
@Composable
private fun QuestionCountFigure(onHero: Color, onHeroMuted: Color) {
    val spoken = stringResource(
        Res.string.mixed_interview_question_count,
        MixedInterviewDefaults.QuestionCount,
    )
    Column(
        modifier = Modifier.clearAndSetSemantics { text = AnnotatedString(spoken) },
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
    ) {
        Text(
            text = MixedInterviewDefaults.QuestionCount.toString(),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = onHero,
        )
        Text(
            text = stringResource(Res.string.mixed_interview_question_count_unit),
            style = MaterialTheme.typography.titleMedium,
            color = onHeroMuted,
        )
    }
}

/**
 * One of the two things that make an Interview different from Practice.
 *
 * A labelled fact rather than a paragraph: the title is what the learner scans, the detail is what
 * they read if the title raises a question. Both sentences already existed on this screen as loose
 * body text, and nothing here claims anything they did not.
 *
 * The icon is decorative on purpose. The title beside it says the same thing in words, so
 * announcing the glyph would be announcing the row twice — and neither symbol carries information
 * the text lacks. The row is top-aligned rather than centred so a detail that wraps to three lines
 * at a large type scale keeps its icon against the first one.
 */
@Composable
private fun InterviewRule(
    icon: ImageVector,
    title: String,
    detail: String,
    onHero: Color,
    onHeroMuted: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Comfortable),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = onHero,
            modifier = Modifier.size(RuleIconSize),
        )
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = onHero,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = onHeroMuted,
            )
        }
    }
}

/** How it has gone so far, or a deliberate statement that it has not gone at all yet. */
private fun LazyListScope.historySection(
    history: InterviewHistoryUiState,
    onOpenResult: (String) -> Unit,
) {
    item {
        // Loading is distinct from "no record yet": rendering the empty shape while the read is in
        // flight is what made the card appear underneath the learner a moment after arriving. The
        // arrival is a crossfade rather than a swap, because a spinner that vanishes and a record
        // that appears in the same frame reads as two unrelated events in the place where one
        // thing was being waited for.
        //
        // Keyed on which *state* this is, not on the state's contents: a refreshed record with a
        // new best result is the same record, and fading the whole section out and back in would
        // claim something arrived when nothing did.
        AnimatedContent(
            targetState = history,
            contentKey = { it.transitionKey },
            transitionSpec = {
                fadeIn(AppMotion.revealSpec()) togetherWith
                    fadeOut(AppMotion.effectSpec())
            },
            label = "interview record",
        ) { state ->
            when (state) {
                InterviewHistoryUiState.Loading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.Grouped),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.testTag(InterviewHistoryLoadingTag),
                    )
                }
                InterviewHistoryUiState.Empty -> FirstInterviewNote()
                is InterviewHistoryUiState.Content -> InterviewRecord(
                    history = state.history,
                    onOpenResult = onOpenResult,
                )
            }
        }
    }
}

/** Which of the three record states this is, so a refresh within one of them is not a transition. */
private val InterviewHistoryUiState.transitionKey: String
    get() = when (this) {
        InterviewHistoryUiState.Loading -> "loading"
        InterviewHistoryUiState.Empty -> "empty"
        is InterviewHistoryUiState.Content -> "content"
    }

/**
 * The first-visit state, said deliberately rather than left as a gap.
 *
 * A learner with no interviews used to reach a screen that simply stopped after the explanation,
 * with the space where the record will be showing nothing at all — which reads as something that
 * failed to load rather than as something they have not done yet. This states what will appear here
 * and why it is worth coming back to, in the quietest treatment on the page: the invitation to start
 * is the filled button in the hero, and a second call to action down here would compete with it.
 *
 * It deliberately shows no empty table, no zeroed score, and no placeholder row — a 0 of 20 would be
 * a result the learner never got.
 */
@Composable
private fun FirstInterviewNote() {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(InterviewNoHistoryTag),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
    ) {
        Text(
            text = stringResource(Res.string.interview_history_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(Res.string.interview_history_empty_detail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The learner's own results, shown only once there are some.
 *
 * The most recent interview leads, and carries its date. Recency is what makes the record useful —
 * "how did I do last time, and how long ago was that?" is the question a returning learner has — and
 * a record without a date cannot answer the second half of it.
 *
 * Best is kept, and kept second. It is genuine information and the data model already supports it,
 * but it is not what this screen is for: a personal best promoted above the latest result turns a
 * preparation tool into a high-score table, and the app has no leaderboard, streak, or points
 * anywhere else. Repeating one attempt under both headings would be noise, so the best row appears
 * only once it is a different interview from the latest one.
 *
 * Its rank is unchanged and deliberately below the hero. This is "how your interviews have gone",
 * which is a reference the learner consults; the hero is what the destination is for. The heading
 * stays at `titleMedium` on the bare page, one step under the hero's own — the screen's headline
 * moved into the gradient, so nothing up here is competing for it any more.
 */
@Composable
private fun InterviewRecord(
    history: InterviewHistoryUiModel,
    onOpenResult: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(InterviewRecordTag),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Related),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.interview_history_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    Res.string.interview_history_attempts,
                    history.attemptCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // One container for the record rather than a card per entry. At most two rows ever reach
        // this, they answer the same question about the same thing, and the pair was reading as two
        // unrelated results stacked under a heading instead of as one record with two lines in it.
        ContentGroup(
            modifier = Modifier.testTag(InterviewRecordGroupTag),
            rows = buildList {
                add {
                    InterviewRecordRow(
                        title = stringResource(Res.string.interview_history_latest),
                        attempt = history.latest,
                        showsDate = true,
                        onOpenResult = onOpenResult,
                    )
                }
                if (history.best.attemptId != history.latest.attemptId) {
                    add {
                        InterviewRecordRow(
                            title = stringResource(Res.string.interview_history_best),
                            attempt = history.best,
                            // The best result's own date is not the point of the row — it is the
                            // score that earns it a place — and printing two dates invites reading
                            // the pair as a timeline.
                            showsDate = false,
                            onOpenResult = onOpenResult,
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun InterviewRecordRow(
    title: String,
    attempt: InterviewAttemptUiModel,
    showsDate: Boolean,
    onOpenResult: (String) -> Unit,
) {
    AccuracyRow(
        title = title,
        detail = stringResource(
            Res.string.interview_history_score,
            attempt.correctAnswers,
            attempt.totalQuestions,
        ),
        percentage = attempt.percentage,
        caption = if (showsDate) timestampText(attempt.completedAt) else null,
        onClick = { onOpenResult(attempt.attemptId) },
    )
}

/**
 * Supporting text on the hero, as a share of its on-colour.
 *
 * The same value `ProgressHero` uses, and for the same reason: the gradient documents exactly one
 * on-colour, so a separate muted token would be a colour nothing has verified against either
 * endpoint. `ProgressHeroThemeTest` already measures this composite over all four endpoints of both
 * schemes, which is what makes reusing the number cheaper than inventing one.
 */
private const val SupportingTextAlpha = 0.8f

/** An edge, not an outline: visible where the gradient meets the page and nowhere else. */
private const val HeroBorderAlpha = 0.14f

private val HeroBorderWidth = 1.dp
private val HeroElevation = 2.dp

/**
 * The hero rises by a fraction of its own height, so the distance suits the surface rather than
 * being a fixed offset that reads as a long slide on a short card and a twitch on a tall one.
 */
private const val EntranceRiseFraction = 6

/** Behind the figure, not after it: the whole entry still lands inside 400ms. */
private const val RulesRevealDelayMillis = 60
private const val ActionRevealDelayMillis = 100

/** Beside the destination's name, at its own type size rather than at a list row's icon size. */
private val ModeIconSize = 20.dp

/** One step up from the mode glyph: these two rows are the hero's scannable content. */
private val RuleIconSize = 24.dp
