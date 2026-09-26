package org.artkachenko.kmp_learning_app.topic_study.practice_builder

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.app_retry
import kmp_learning_app.shared.generated.resources.practice_builder_available_questions
import kmp_learning_app.shared.generated.resources.practice_builder_available_unit
import kmp_learning_app.shared.generated.resources.practice_builder_checking
import kmp_learning_app.shared.generated.resources.practice_builder_error
import kmp_learning_app.shared.generated.resources.practice_builder_level_advanced
import kmp_learning_app.shared.generated.resources.practice_builder_level_applied
import kmp_learning_app.shared.generated.resources.practice_builder_level_foundation
import kmp_learning_app.shared.generated.resources.practice_builder_level_help
import kmp_learning_app.shared.generated.resources.practice_builder_levels
import kmp_learning_app.shared.generated.resources.practice_builder_no_practiceable_concepts
import kmp_learning_app.shared.generated.resources.practice_builder_no_questions
import kmp_learning_app.shared.generated.resources.practice_builder_question_count
import kmp_learning_app.shared.generated.resources.practice_builder_question_count_option
import kmp_learning_app.shared.generated.resources.practice_builder_question_count_unit
import kmp_learning_app.shared.generated.resources.practice_builder_scope_learning_unit
import kmp_learning_app.shared.generated.resources.practice_builder_scope_subtopic
import kmp_learning_app.shared.generated.resources.practice_builder_scope_topic
import kmp_learning_app.shared.generated.resources.practice_builder_scope_unknown
import kmp_learning_app.shared.generated.resources.practice_builder_source
import kmp_learning_app.shared.generated.resources.practice_builder_source_all
import kmp_learning_app.shared.generated.resources.practice_builder_source_mistakes
import kmp_learning_app.shared.generated.resources.practice_builder_source_unavailable
import kmp_learning_app.shared.generated.resources.practice_builder_source_unseen
import kmp_learning_app.shared.generated.resources.practice_builder_source_weak_areas
import kmp_learning_app.shared.generated.resources.practice_builder_start
import kmp_learning_app.shared.generated.resources.practice_builder_start_count
import kmp_learning_app.shared.generated.resources.practice_builder_select_any
import kmp_learning_app.shared.generated.resources.practice_builder_target_unavailable
import kmp_learning_app.shared.generated.resources.practice_builder_title
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.MetricFigure
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.SectionHeading
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.artkachenko.kmp_learning_app.ui.AppTwoPaneRow
import org.artkachenko.kmp_learning_app.ui.theme.AppContentWidth
import org.artkachenko.kmp_learning_app.ui.theme.AppMotion
import org.artkachenko.kmp_learning_app.ui.theme.AppScreenPane
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppWindowSizeClass

internal const val PracticeBuilderStartButtonTag = "practice_builder_start"
internal const val PracticeBuilderAvailabilityTag = "practice_builder_availability"

/** The Retry a failed eligibility read offers, and the only availability state that has one. */
internal const val PracticeBuilderRetryTag = "practice_builder_retry"

/**
 * The builder's scrollers: one column when the window is compact or medium, and two panes when it
 * is expanded. Named so a test can scroll the column an option is actually in — at a large font
 * scale every option below the first is out of composition until something scrolls to it.
 */
internal const val PracticeBuilderContentTag = "practice_builder_content"
internal const val PracticeBuilderFormPaneTag = "practice_builder_form_pane"
internal const val PracticeBuilderSummaryPaneTag = "practice_builder_summary_pane"

internal fun practiceLevelTag(level: QuestionLevel): String = "practice_builder_level_${level.name}"

internal fun practiceSourceTag(source: PracticeQuestionSource): String =
    "practice_builder_source_${source.name}"

internal fun practiceQuestionCountTag(questionCount: Int): String =
    "practice_builder_count_$questionCount"

/**
 * Four decisions, all of them already answered on arrival, and the one action that acts on them.
 *
 * Every control wraps, so a narrow window or a large font scale reflows instead of clipping, and
 * Back is the top app bar's, matching every other detail and setup destination in the app.
 *
 * On an expanded window the configuration and the summary separate into two panes. That is the
 * split the screen already had in its ordering — four questions, then what the answers add up to
 * and the control that starts it — and putting the summary beside the form rather than below it
 * means a learner changing the level filter can see the eligible-question count change as they do
 * it, instead of scrolling down to find out. No new configuration concept was added to occupy the
 * second pane: it holds exactly the availability line, its retry, and Start, which is what the
 * bottom of the single column holds on a phone.
 *
 * ## Why this is not a settings form
 *
 * Structurally it was already right and it still read as three rows of chips under three headings,
 * because every choice was a default `FilterChip` whose selected state is Material's
 * `secondaryContainer` — which this palette defines as the primary hue *drained of chroma*, the
 * same tint the neutral surface ramp carries. Choosing an option therefore made it slightly darker
 * rather than making it chosen. `AssessmentTakingScreen` reached that conclusion first for answer
 * options and moved them to `primaryContainer` with a `primary` border; the builder now speaks the
 * same vocabulary, so the surfaces a learner picks from look the same whether they are configuring
 * a run or answering inside one. [BuilderChoice] is that surface, and its three call sites are the
 * whole reason it exists — it is deliberately private to this screen rather than a general
 * selection component the app does not otherwise need.
 */
@Composable
internal fun PracticeBuilderScreen(
    state: PracticeBuilderUiState,
    onBack: () -> Unit,
    onQuestionCountClick: (Int) -> Unit,
    onLevelClick: (QuestionLevel) -> Unit,
    onSourceClick: (PracticeQuestionSource) -> Unit,
    onStartClick: () -> Unit,
    onRetryAvailability: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = rememberAppTopBarScrollBehavior()
    Column(modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        AppTopBar(
            title = stringResource(Res.string.practice_builder_title),
            onBack = onBack,
            scrollBehavior = scrollBehavior,
        )
        AppScreenPane(AppContentWidth.Paned) {
            if (LocalAppWindowSizeClass.current.isExpanded) {
                AppTwoPaneRow(
                    primary = {
                        BuilderPane(
                            Modifier
                                .weight(ConfigurationPaneWeight)
                                .testTag(PracticeBuilderFormPaneTag),
                        ) {
                            configurationSection(
                                state = state,
                                onQuestionCountClick = onQuestionCountClick,
                                onLevelClick = onLevelClick,
                                onSourceClick = onSourceClick,
                            )
                        }
                    },
                    secondary = {
                        BuilderPane(
                            Modifier
                                .weight(SummaryPaneWeight)
                                .testTag(PracticeBuilderSummaryPaneTag),
                        ) {
                            summarySection(
                                state = state,
                                onStartClick = onStartClick,
                                onRetryAvailability = onRetryAvailability,
                            )
                        }
                    },
                )
                return@AppScreenPane
            }
            BuilderPane(Modifier.fillMaxSize().testTag(PracticeBuilderContentTag)) {
                configurationSection(
                    state = state,
                    onQuestionCountClick = onQuestionCountClick,
                    onLevelClick = onLevelClick,
                    onSourceClick = onSourceClick,
                )
                summarySection(
                    state = state,
                    onStartClick = onStartClick,
                    onRetryAvailability = onRetryAvailability,
                )
            }
        }
    }
}

/**
 * One column of the builder.
 *
 * A `LazyColumn` of option groups rather than a settings form, and it scrolls rather than pinning
 * the Start action over the content: the summary explains what Start will do, and a control
 * floating above its own explanation is the arrangement that makes a learner press it without
 * reading.
 *
 * The arrangement is the app's ordinary [AppSpacing.Comfortable], not [AppSpacing.Section]. Every
 * group here opens with a `SectionHeading`, which carries a `Section` break of its own, so a
 * `Section` arrangement was paying for the break twice and putting 48dp between a row of options
 * and the heading of the next — half again the separation the same pairing gets on every other
 * screen. It is also what connects the summary to the configuration on a phone: the conclusion
 * follows the last group at an ordinary 16dp while the groups themselves are 24dp apart, so
 * proximity says "these choices, therefore this run" rather than "a fifth section".
 */
@Composable
private fun BuilderPane(
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

/** What the run will be: its scope, its size, which levels, and which questions to draw from. */
private fun LazyListScope.configurationSection(
    state: PracticeBuilderUiState,
    onQuestionCountClick: (Int) -> Unit,
    onLevelClick: (QuestionLevel) -> Unit,
    onSourceClick: (PracticeQuestionSource) -> Unit,
) {
    item { BuilderScopeLine(state.scope) }
    item {
        BuilderSection(heading = stringResource(Res.string.practice_builder_question_count)) {
            QuestionCountOptions(
                options = state.questionCountOptions,
                selected = state.questionCount,
                onClick = onQuestionCountClick,
            )
        }
    }
    item {
        BuilderSection(
            heading = stringResource(Res.string.practice_builder_levels),
            // Under the heading it belongs to, and before the controls it describes. It used to sit
            // above the heading, where it read as a trailing note on the question-count row above
            // and told a learner that *that* set was multi-select.
            hint = stringResource(Res.string.practice_builder_select_any),
        ) {
            LevelOptions(selected = state.levels, onClick = onLevelClick)
            Text(
                text = stringResource(Res.string.practice_builder_level_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    item {
        BuilderSection(heading = stringResource(Res.string.practice_builder_source)) {
            SourceOptions(
                options = state.sourceOptions,
                selected = state.source,
                onClick = onSourceClick,
            )
            if (state.sourceOptions.any { !it.isAvailable }) {
                Text(
                    text = stringResource(Res.string.practice_builder_source_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * What is being configured.
 *
 * One step above the section headings under it. At `titleLarge` it was the same size as
 * "Questions", "Levels" and "Draw from", so the thing being configured read as a fourth group
 * rather than as what the other three are about; the icon is the rest of that separation, and it is
 * a small `primary` accent rather than a container, because the builder is the subject of this
 * screen and a scope card would be a second one.
 *
 * The glyph is the app's generic study-scope mark and is decorative: the label states in words
 * whether this is a Topic, a Subtopic, or a Learning unit, so announcing the icon would repeat it.
 */
@Composable
private fun BuilderScopeLine(scope: PracticeScopeUiModel) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Related),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = AppIcons.Topics,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(ScopeIconSize),
        )
        Text(
            text = scope.label(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            // What is being configured, announced as a heading like every other screen's subject
            // line. The three `SectionHeading`s below it already were, so the form's fields were
            // reachable by heading navigation while the thing they configure was not.
            modifier = Modifier.semantics { heading() },
        )
    }
}

/**
 * How long the run is, as a set of tiles rather than a set of sentences.
 *
 * The number is the whole content of this decision and it was previously set at label size inside
 * "10 questions", so four chips read as four sentences to be compared word by word. Stacking the
 * figure over the unit makes the four options four sizes, which is what the learner is actually
 * choosing between. The full sentence survives as the tile's accessible name, so nothing is lost
 * to a screen reader by the split.
 *
 * [QuestionCountTileMinWidth] is a minimum and not a width: the tile still grows for a longer
 * translation or a doubled type scale, and [BuilderOptionFlow] wraps when the row runs out.
 */
@Composable
private fun QuestionCountOptions(
    options: List<Int>,
    selected: Int,
    onClick: (Int) -> Unit,
) {
    BuilderOptionFlow {
        options.forEach { option ->
            val spokenLabel = pluralStringResource(
                Res.plurals.practice_builder_question_count_option,
                option,
                option,
            )
            BuilderChoice(
                selected = option == selected,
                // Single-select: choosing a length unchooses the previous one, and the role is what
                // says so. It carries no leading control, unlike the two sets below, because
                // exactly one is always chosen and a radio in every tile would state that four
                // times over.
                role = Role.RadioButton,
                onClick = { onClick(option) },
                modifier = Modifier
                    .widthIn(min = QuestionCountTileMinWidth)
                    .testTag(practiceQuestionCountTag(option))
                    .semantics { contentDescription = spokenLabel },
            ) { colors ->
                Column(
                    // Deliberately not `fillMaxWidth`: inside a `FlowRow` the incoming maximum is
                    // the whole row, so filling it would make each tile a full-width bar and the
                    // set would stack instead of wrapping. The tile is as wide as its content or
                    // [QuestionCountTileMinWidth], whichever is larger.
                    modifier = Modifier.padding(
                        horizontal = AppSpacing.Grouped,
                        vertical = AppSpacing.Related,
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = option.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.content,
                    )
                    Text(
                        text = pluralStringResource(
                            Res.plurals.practice_builder_question_count_unit,
                            option,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.supporting,
                    )
                }
            }
        }
    }
}

/**
 * Which levels to draw from. Multi-select, and it says so with a checkbox inside the tile.
 *
 * The checkbox stays exactly where it was, because it is the only thing that tells a learner
 * whether choosing one option unchooses another — the tile shape is shared with the source rows
 * below and the leading control is what distinguishes them. What changed is that its tints travel
 * with the tile around it on the same transition, so Material's own check animation and the
 * container it sits in arrive together instead of the mark repainting a frame ahead.
 */
@Composable
private fun LevelOptions(
    selected: Set<QuestionLevel>,
    onClick: (QuestionLevel) -> Unit,
) {
    BuilderOptionFlow {
        QuestionLevel.entries.forEach { level ->
            val isSelected = level in selected
            BuilderChoice(
                selected = isSelected,
                // `Role.Checkbox` on a `selectable`, which is what Material's own `FilterChip`
                // publishes: the node keeps both the selected state and the multi-select role, so
                // nothing about how this group is announced changed with its appearance.
                role = Role.Checkbox,
                onClick = { onClick(level) },
                modifier = Modifier.testTag(practiceLevelTag(level)),
            ) { colors ->
                Row(
                    modifier = Modifier.padding(
                        horizontal = AppSpacing.Grouped,
                        vertical = AppSpacing.Related,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.Related),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BuilderChoiceControl {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = colors.control,
                                uncheckedColor = colors.control,
                            ),
                        )
                    }
                    Text(
                        text = stringResource(level.labelResource()),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.content,
                    )
                }
            }
        }
    }
}

/**
 * Where questions come from. Single-select, with a radio button saying so.
 *
 * Full-width rows rather than wrapped tiles, because these are the longest labels on the screen and
 * a radio button nested inside a 32dp chip beside "Weak areas" left neither the control nor the
 * words any room. They are *not* rows inside a `ContentGroup`, which is the other pattern the app
 * has for a bounded set: a chosen option has to be singled out, it is singled out with a
 * `primary` border because `primaryContainer` alone is barely a step above the light theme's page,
 * and `docs/development/surface-hierarchy.md` records that a border is a property of a container —
 * inside a group there is none to put it on. So they stay the same option surface as the tiles
 * above, which is also the surface the assessment taking screen draws an answer on.
 *
 * Unavailable sources stay visible and disabled rather than disappearing: the learner can see that
 * weak-area and mistake practice exist and are not ready yet.
 */
@Composable
private fun SourceOptions(
    options: List<PracticeSourceOption>,
    selected: PracticeQuestionSource,
    onClick: (PracticeQuestionSource) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Related)) {
        options.forEach { option ->
            val isSelected = option.source == selected
            BuilderChoice(
                selected = isSelected,
                role = Role.RadioButton,
                enabled = option.isAvailable,
                onClick = { onClick(option.source) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(practiceSourceTag(option.source)),
            ) { colors ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = AppSpacing.Comfortable,
                            vertical = AppSpacing.Grouped,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BuilderChoiceControl {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            enabled = option.isAvailable,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.control,
                                unselectedColor = colors.control,
                                // The row already carries the disabled semantics and the disabled
                                // content colour. Letting Material fade the control a second time
                                // on top of that is what made an unavailable source read as a
                                // rendering fault rather than as a state.
                                disabledSelectedColor = colors.control,
                                disabledUnselectedColor = colors.control,
                            ),
                        )
                    }
                    Text(
                        text = stringResource(option.source.labelResource()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.content,
                    )
                }
            }
        }
    }
}

/**
 * What the four answers add up to, and the single control that acts on them.
 *
 * ## Why this is a level-2 surface
 *
 * It was a `SecondarySummaryCard`, which is `surfaceContainerLow` — the same level every option
 * surface above it now sits on. That made the screen's conclusion tonally one more option. The
 * tone ramp's level 2 is defined as "the one surface on a screen that outranks the rest", and after
 * this change that is exactly what this is: the only filled, unbordered, non-selectable surface on
 * the page, and the only one holding an action. `surfaceContainer` plus a hairline
 * `outlineVariant` edge, one step below `AccuracyHeroCard` and with no shadow, so the builder does
 * not acquire a hero it has no figure for.
 *
 * ## Why it is animated and the configuration is not
 *
 * The options respond to being chosen; this responds to what was chosen. One `AnimatedContent` over
 * the whole [PracticeAvailability] carries the count, the sentence, the tone, the icon and the
 * block's height together, so widening a level filter reads as the run being recalculated rather
 * than as a caption being replaced. Keying it on the availability itself rather than on a tone
 * means the outgoing content keeps its own figure while it leaves — an outgoing "12 questions
 * ready" that read the *new* state would lose its number a frame into fading out.
 *
 * Nothing here waits on an animation and nothing new is in the ViewModel: the whole verdict is
 * derived from the state the screen is already handed.
 */
private fun LazyListScope.summarySection(
    state: PracticeBuilderUiState,
    onStartClick: () -> Unit,
    onRetryAvailability: () -> Unit,
) {
    item {
        BuilderSummarySurface {
            AvailabilityVerdict(state.availability)
            // A failed check is the one availability state the learner cannot resolve by changing
            // the setup, so it is the only one that offers an action. It fades rather than appears,
            // for the same reason the verdict above it does: the two are one answer.
            AnimatedVisibility(
                visible = state.availability is PracticeAvailability.Error,
                enter = fadeIn(AppMotion.revealSpec()),
                exit = fadeOut(AppMotion.effectSpec()),
            ) {
                TextButton(
                    onClick = onRetryAvailability,
                    modifier = Modifier.testTag(PracticeBuilderRetryTag),
                ) {
                    Text(text = stringResource(Res.string.app_retry))
                }
            }
            StartAction(state = state, onStartClick = onStartClick)
        }
    }
}

/** The screen's conclusion, on the one surface that outranks the options; see [summarySection]. */
@Composable
private fun BuilderSummarySurface(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(SummaryBorderWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.Generous),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Comfortable),
            content = content,
        )
    }
}

/**
 * Whether this setup will run, and — when it will — how much of it there is.
 *
 * The available state is the strongest of the six and leads with the count as a figure, in the
 * **brand** rather than in the `correct` green: being able to practise is not an achievement, and
 * borrowing the palette that means "you got this right" for it would spend the one colour the
 * product has for that on a precondition.
 *
 * The figure and its caption are one statement, so they are published as one: the existing
 * "%1$d questions ready" sentence is set on the block through `clearAndSetSemantics`, which is what
 * keeps it a single announcement instead of "12" followed by "questions ready".
 */
@Composable
private fun AvailabilityVerdict(availability: PracticeAvailability) {
    AnimatedContent(
        targetState = availability,
        transitionSpec = {
            val enter = fadeIn(AppMotion.revealSpec()) +
                scaleIn(AppMotion.revealSpec(), initialScale = VerdictInitialScale)
            val exit = fadeOut(AppMotion.effectSpec(AppMotion.StateChangeDurationMillis / 2))
            // The block's height changes with the verdict — a figure over a caption is taller than
            // one sentence — so the resize travels with the crossfade rather than snapping under
            // it. Unclipped, because the outgoing content is still on screen at its old height.
            enter togetherWith exit using SizeTransform(clip = false)
        },
        modifier = Modifier.fillMaxWidth().testTag(PracticeBuilderAvailabilityTag),
        label = "practiceAvailability",
    ) { verdict ->
        if (verdict is PracticeAvailability.Available) {
            val sentence = stringResource(
                Res.string.practice_builder_available_questions,
                verdict.eligibleQuestionCount,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
                modifier = Modifier.clearAndSetSemantics { text = AnnotatedString(sentence) },
            ) {
                MetricFigure(
                    text = verdict.eligibleQuestionCount.toString(),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = pluralStringResource(
                        Res.plurals.practice_builder_available_unit,
                        verdict.eligibleQuestionCount,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@AnimatedContent
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.Related),
            verticalAlignment = Alignment.Top,
        ) {
            val tone = verdict.messageColor()
            verdict.icon()?.let {
                Icon(
                    imageVector = it,
                    // The sentence beside it says the same thing in words, which is the channel
                    // that has to carry the state; the icon is the second one.
                    contentDescription = null,
                    tint = tone,
                    modifier = Modifier.size(VerdictIconSize),
                )
            }
            Text(
                text = verdict.message(),
                style = MaterialTheme.typography.titleMedium,
                color = tone,
            )
        }
    }
}

/**
 * The one primary action, and the only control on the screen that starts anything.
 *
 * Its label follows the count, so it is an `AnimatedContent` over the label itself: an unchanged
 * label is the same state and does not transition, while "Start 10-question practice" becoming
 * "Start 6-question practice" crosses over with the width rather than jumping.
 *
 * The container and content colours travel on one transition over whether Start can be pressed, and
 * both the enabled and the disabled colour slots are given the *same* animated value. Material
 * still owns `enabled`, so the semantics and the click are untouched; what changes is that becoming
 * available reads as the action arriving rather than as a repaint.
 */
@Composable
private fun ColumnScope.StartAction(
    state: PracticeBuilderUiState,
    onStartClick: () -> Unit,
) {
    val transition = updateTransition(targetState = state.isStartEnabled, label = "practiceStart")
    val containerColor by transition.animateColor(
        transitionSpec = { AppMotion.effectSpec() },
        label = "practiceStartContainer",
    ) { enabled ->
        if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContainerAlpha)
        }
    }
    val contentColor by transition.animateColor(
        transitionSpec = { AppMotion.effectSpec() },
        label = "practiceStartContent",
    ) { enabled ->
        if (enabled) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentAlpha)
        }
    }
    val count = (state.availability as? PracticeAvailability.Available)
        ?.eligibleQuestionCount
        ?.let { minOf(it, state.questionCount) }
    val label = count?.let {
        pluralStringResource(Res.plurals.practice_builder_start_count, it, it)
    } ?: stringResource(Res.string.practice_builder_start)
    Button(
        onClick = onStartClick,
        // The configuration is checked before Start, so an impossible run never navigates into
        // assessment taking to fail there.
        enabled = state.isStartEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor,
        ),
        modifier = Modifier
            // Full width where the card is a phone's width, and its own width where the card is
            // half a desktop window: a 500dp button is not a bigger affordance, it is a bar that
            // happens to be pressable.
            .then(
                if (LocalAppWindowSizeClass.current.isExpanded) {
                    Modifier
                } else {
                    Modifier.fillMaxWidth()
                },
            )
            .testTag(PracticeBuilderStartButtonTag),
    ) {
        AnimatedContent(
            targetState = label,
            transitionSpec = {
                val enter = fadeIn(AppMotion.revealSpec())
                val exit = fadeOut(AppMotion.effectSpec(AppMotion.StateChangeDurationMillis / 2))
                enter togetherWith exit using SizeTransform(clip = false)
            },
            label = "practiceStartLabel",
        ) { Text(it) }
    }
}

/** The form is the substance of the screen; the summary beside it is short by design. */
private const val ConfigurationPaneWeight = 3f
private const val SummaryPaneWeight = 2f

/**
 * A labelled group of options.
 *
 * The heading and its optional hint are fixed; the options themselves are the caller's, because the
 * three groups do not lay out the same way — two wrap as tiles and one is a column of full-width
 * rows — and a component that took a `wrap: Boolean` would be answering for a decision each group
 * makes for its own reasons.
 */
@Composable
private fun BuilderSection(
    heading: String,
    hint: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Related)) {
        SectionHeading(text = heading)
        hint?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

/**
 * Options that wrap.
 *
 * `FlowRow` rather than a `Row`: four count tiles or three level tiles do not fit one line on a
 * compact width or at a large font scale, and a fixed row would push the last option off-screen
 * instead of moving it to the next line.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuilderOptionFlow(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Related),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Related),
        content = content,
    )
}

/**
 * One choice in the builder, in the three states it can hold.
 *
 * The surface *is* the touch target and the selection indicator, which is the arrangement the
 * assessment taking screen already uses for an answer option, and the two now agree: a chosen
 * option is `primaryContainer` behind a 2dp `primary` border, an unchosen one is a level-1 surface
 * behind a hairline `outlineVariant`, and an unavailable one keeps its container and drops its
 * content and edge to Material's disabled alphas — a state rather than a fade.
 *
 * Everything the surface says about itself is one discrete fact, [BuilderChoiceState], so it is one
 * `Transition` rather than five `animate*AsState` calls on the same input. Independent animations
 * on one fact drift apart under a fast state change and let a later edit teach one property a rule
 * the other four do not know. Colours are tweened and the border width is sprung, because a width
 * is a size and a colour has no "past the target".
 *
 * [content] receives the mid-animation colours rather than resolving its own, so the label and the
 * leading control travel with the container instead of repainting ahead of it.
 *
 * The press scale is draw-layer only: layout, hit testing, and when [onClick] runs are all
 * untouched, so a press treatment is never something the callback waits for.
 */
@Composable
private fun BuilderChoice(
    selected: Boolean,
    role: Role,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable (BuilderChoiceColors) -> Unit,
) {
    // Owned here rather than left to `selectable`'s own, because the surface reads the press as
    // well as indicating it. Material still draws its ripple from the same source, so there is one
    // press and two responses to it instead of a hand-rolled gesture detector.
    val interactionSource = remember { MutableInteractionSource() }
    val transition = updateTransition(
        targetState = when {
            !enabled -> BuilderChoiceState.Disabled
            selected -> BuilderChoiceState.Selected
            else -> BuilderChoiceState.Resting
        },
        label = "builderChoice",
    )
    val container by transition.animateColor(
        transitionSpec = { AppMotion.effectSpec() },
        label = "builderChoiceContainer",
    ) { it.colors().container }
    val border by transition.animateColor(
        transitionSpec = { AppMotion.effectSpec() },
        label = "builderChoiceBorder",
    ) { it.colors().border }
    val borderWidth by transition.animateDp(
        transitionSpec = { AppMotion.spatialSpec() },
        label = "builderChoiceBorderWidth",
    ) { if (it == BuilderChoiceState.Selected) SelectedBorderWidth else UnselectedBorderWidth }
    val contentColor by transition.animateColor(
        transitionSpec = { AppMotion.effectSpec() },
        label = "builderChoiceContent",
    ) { it.colors().content }
    val supportingColor by transition.animateColor(
        transitionSpec = { AppMotion.effectSpec() },
        label = "builderChoiceSupporting",
    ) { it.colors().supporting }
    val controlColor by transition.animateColor(
        transitionSpec = { AppMotion.effectSpec() },
        label = "builderChoiceControl",
    ) { it.colors().control }

    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) PressedScale else 1f,
        animationSpec = AppMotion.spatialSpec(),
        label = "builderChoicePress",
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            // One node per choice. Without this the label and a nested checkbox are announced
            // separately from the thing that is selectable, which is the defect a row containing a
            // control has and a control that *is* the row does not.
            .semantics(mergeDescendants = true) {}
            .selectable(
                selected = selected,
                enabled = enabled,
                role = role,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        shape = MaterialTheme.shapes.medium,
        color = container,
        border = BorderStroke(width = borderWidth, color = border),
    ) {
        Box(
            modifier = Modifier.heightIn(min = BuilderChoiceMinHeight),
            contentAlignment = Alignment.Center,
        ) {
            content(
                BuilderChoiceColors(
                    container = container,
                    border = border,
                    content = contentColor,
                    supporting = supportingColor,
                    control = controlColor,
                ),
            )
        }
    }
}

/**
 * A leading `Checkbox` or `RadioButton` inside a choice surface.
 *
 * Material enforces a 48dp minimum interactive size on both, which inside a surface that is already
 * the touch target buys nothing and pads the control away from its own label. The surface carries
 * the target through [BuilderChoiceMinHeight] instead.
 */
@Composable
private fun BuilderChoiceControl(control: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
        content = control,
    )
}

/** The three appearances a choice has. Derived from selection and availability, never stored. */
private enum class BuilderChoiceState { Resting, Selected, Disabled }

/**
 * The colours of one appearance, resolved together so a later edit cannot move only one of them.
 *
 * [container] and [border] are the surface's own; [content], [supporting] and [control] are handed
 * to the caller's content, which is why this is one type rather than a pair — an appearance is one
 * decision and splitting it would let a call site take the label colour of a state whose fill it
 * did not take.
 */
@Immutable
private data class BuilderChoiceColors(
    val container: Color,
    val border: Color,
    val content: Color,
    val supporting: Color,
    val control: Color,
)

/**
 * What each appearance is drawn in.
 *
 * `Selected` takes the **primary** family. It used to be Material's chip default,
 * `secondaryContainer`, and the secondary family is by this palette's own definition the primary
 * hue *drained of chroma* — the same colour the neutral surface ramp is tinted with, so a chosen
 * option was one more step on that ramp rather than a different kind of thing.
 * `AssessmentTakingScreen` records the same reasoning for an answer option; the builder now matches
 * it, which is why the border is `primary` and the fill agrees with it.
 *
 * `Disabled` keeps the resting container and moves only the content and the edge, to Material's own
 * disabled opacities. Fading the whole surface would have made an unavailable source look like a
 * rendering fault; leaving the container alone and dimming what is written on it reads as a state.
 */
@Composable
@ReadOnlyComposable
private fun BuilderChoiceState.colors(): BuilderChoiceColors = when (this) {
    BuilderChoiceState.Resting -> BuilderChoiceColors(
        container = MaterialTheme.colorScheme.surfaceContainerLow,
        border = MaterialTheme.colorScheme.outlineVariant,
        content = MaterialTheme.colorScheme.onSurface,
        supporting = MaterialTheme.colorScheme.onSurfaceVariant,
        control = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    BuilderChoiceState.Selected -> BuilderChoiceColors(
        container = MaterialTheme.colorScheme.primaryContainer,
        border = MaterialTheme.colorScheme.primary,
        content = MaterialTheme.colorScheme.onPrimaryContainer,
        supporting = MaterialTheme.colorScheme.onPrimaryContainer,
        control = MaterialTheme.colorScheme.primary,
    )
    BuilderChoiceState.Disabled -> BuilderChoiceColors(
        container = MaterialTheme.colorScheme.surfaceContainerLow,
        border = MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContainerAlpha),
        content = MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentAlpha),
        supporting = MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentAlpha),
        control = MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentAlpha),
    )
}

@Composable
private fun PracticeScopeUiModel.label(): String =
    when (name) {
        null -> stringResource(Res.string.practice_builder_scope_unknown)
        else -> when (kind) {
            PracticeScopeKind.TOPIC -> stringResource(Res.string.practice_builder_scope_topic, name)
            PracticeScopeKind.SUBTOPIC ->
                stringResource(Res.string.practice_builder_scope_subtopic, name)
            PracticeScopeKind.LEARNING_UNIT ->
                stringResource(Res.string.practice_builder_scope_learning_unit, name)
        }
    }

/**
 * The tone of the verdict: a fact, a setup that will not run, or a check that failed.
 *
 * Amber is the app's warning tone and says *this configuration needs changing* — which covers both
 * the case the learner can fix by widening levels and the two where the target itself is no longer
 * practiceable, because in all three Start is off and the sentence beside it explains why. Red is
 * reserved for the read having failed, which is the only state with a Retry, and matches
 * `colorScheme.error` being the same red the product uses for a failed operation everywhere else.
 *
 * `Available` is not in this list: it is drawn as a figure in the brand rather than as a tinted
 * sentence, because being able to practise is a precondition and not a result.
 */
@Composable
@ReadOnlyComposable
private fun PracticeAvailability.messageColor(): Color =
    when (this) {
        PracticeAvailability.Checking,
        is PracticeAvailability.Available,
        -> MaterialTheme.colorScheme.onSurface
        PracticeAvailability.NoEligibleQuestions,
        PracticeAvailability.TargetUnavailable,
        PracticeAvailability.NoPracticeableConcepts,
        -> AppThemeExtras.semanticColors.partiallyCorrect
        PracticeAvailability.Error -> MaterialTheme.colorScheme.error
    }

/**
 * The second channel on a verdict that is not a run.
 *
 * Nothing is marked while the check is in flight or once it has succeeded: an icon on every state
 * would make the mark furniture rather than a signal.
 */
private fun PracticeAvailability.icon(): ImageVector? =
    when (this) {
        PracticeAvailability.Checking,
        is PracticeAvailability.Available,
        -> null
        PracticeAvailability.NoEligibleQuestions,
        PracticeAvailability.TargetUnavailable,
        PracticeAvailability.NoPracticeableConcepts,
        PracticeAvailability.Error,
        -> AppIcons.Warning
    }

@Composable
private fun PracticeAvailability.message(): String =
    when (this) {
        PracticeAvailability.Checking -> stringResource(Res.string.practice_builder_checking)
        is PracticeAvailability.Available -> stringResource(
            Res.string.practice_builder_available_questions,
            eligibleQuestionCount,
        )
        PracticeAvailability.NoEligibleQuestions ->
            stringResource(Res.string.practice_builder_no_questions)
        PracticeAvailability.TargetUnavailable ->
            stringResource(Res.string.practice_builder_target_unavailable)
        PracticeAvailability.NoPracticeableConcepts ->
            stringResource(Res.string.practice_builder_no_practiceable_concepts)
        PracticeAvailability.Error -> stringResource(Res.string.practice_builder_error)
    }

private fun QuestionLevel.labelResource(): StringResource =
    when (this) {
        QuestionLevel.FOUNDATION -> Res.string.practice_builder_level_foundation
        QuestionLevel.APPLIED -> Res.string.practice_builder_level_applied
        QuestionLevel.ADVANCED -> Res.string.practice_builder_level_advanced
    }

private fun PracticeQuestionSource.labelResource(): StringResource =
    when (this) {
        PracticeQuestionSource.ALL -> Res.string.practice_builder_source_all
        PracticeQuestionSource.UNSEEN -> Res.string.practice_builder_source_unseen
        PracticeQuestionSource.WEAK_AREAS -> Res.string.practice_builder_source_weak_areas
        PracticeQuestionSource.UNRESOLVED_MISTAKES -> Res.string.practice_builder_source_mistakes
    }

/** A small accent beside the subject line, not a container around it. */
private val ScopeIconSize = 20.dp

/** Sized to the `titleMedium` line the verdict is set in, so it marks the first line of it. */
private val VerdictIconSize = 20.dp

/** Material's `InteractiveComponentSize`. The surface carries it so its controls need not. */
private val BuilderChoiceMinHeight = 48.dp

/**
 * A minimum, never a width. Four count tiles fit one row on a small phone at the default type
 * scale and wrap from there, which is what `FlowRow` is for.
 */
private val QuestionCountTileMinWidth = 64.dp

private val SelectedBorderWidth = 2.dp
private val UnselectedBorderWidth = 1.dp

/** The hairline that makes the summary read as a lifted surface rather than a tone change. */
private val SummaryBorderWidth = 1.dp

/** Material's own disabled opacities, applied to content and to an edge rather than to a surface. */
private const val DisabledContentAlpha = 0.38f
private const val DisabledContainerAlpha = 0.12f

/** Barely a shrink. Enough to read as a surface giving under a finger, not as a button bouncing. */
private const val PressedScale = 0.98f

/** The verdict settles in from just under its size; a larger start would read as a pop. */
private const val VerdictInitialScale = 0.94f
