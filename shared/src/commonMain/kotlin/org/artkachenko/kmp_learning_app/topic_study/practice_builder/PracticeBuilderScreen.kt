package org.artkachenko.kmp_learning_app.topic_study.practice_builder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.app_retry
import kmp_learning_app.shared.generated.resources.practice_builder_available_questions
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
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.SectionHeading
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Alignment
import org.artkachenko.kmp_learning_app.ui.AppTwoPaneRow
import org.artkachenko.kmp_learning_app.ui.theme.AppContentWidth
import org.artkachenko.kmp_learning_app.ui.theme.AppScreenPane
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppWindowSizeClass

internal const val PracticeBuilderStartButtonTag = "practice_builder_start"
internal const val PracticeBuilderAvailabilityTag = "practice_builder_availability"

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
 * A `LazyColumn` of chip rows rather than a settings form, and it scrolls rather than pinning the
 * Start action over the content: the summary explains what Start will do, and a control floating
 * above its own explanation is the arrangement that makes a learner press it without reading.
 */
@Composable
private fun BuilderPane(
    modifier: Modifier,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxHeight(),
        contentPadding = appScreenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Section),
        content = content,
    )
}

/** What the run will be: its scope, its size, which levels, and which questions to draw from. */
@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.configurationSection(
    state: PracticeBuilderUiState,
    onQuestionCountClick: (Int) -> Unit,
    onLevelClick: (QuestionLevel) -> Unit,
    onSourceClick: (PracticeQuestionSource) -> Unit,
) {
    item {
        Text(
            text = state.scope.label(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
    item {
        BuilderSection(heading = stringResource(Res.string.practice_builder_question_count)) {
            state.questionCountOptions.forEach { option ->
                // A single-select set of mutually exclusive sizes. It carries no leading control,
                // unlike the two sets below: exactly one is always chosen and the chips are short
                // enough that the selected fill is unambiguous on its own.
                FilterChip(
                    selected = option == state.questionCount,
                    onClick = { onQuestionCountClick(option) },
                    label = {
                        Text(
                            pluralStringResource(
                                Res.plurals.practice_builder_question_count_option,
                                option,
                                option,
                            ),
                        )
                    },
                    modifier = Modifier.testTag(practiceQuestionCountTag(option)),
                )
            }
        }
    }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Related)) {
            BuilderSection(
                heading = stringResource(Res.string.practice_builder_levels),
                // Under the heading it belongs to, and before the controls it describes. It used
                // to sit above the heading, where it read as a trailing note on the question-count
                // row above and told a learner that *that* set was multi-select.
                hint = stringResource(Res.string.practice_builder_select_any),
            ) {
                QuestionLevel.entries.forEach { level ->
                    // Multi-select, and it says so with a checkbox inside the chip. The shape is
                    // shared with the source row below; what distinguishes them is the leading
                    // control, which is the only thing that tells a learner whether choosing one
                    // option unchooses another.
                    FilterChip(
                        selected = level in state.levels,
                        onClick = { onLevelClick(level) },
                        label = {
                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(AppSpacing.Related),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = level in state.levels, onCheckedChange = null)
                                Text(stringResource(level.labelResource()))
                            }
                        },
                        modifier = Modifier.testTag(practiceLevelTag(level)),
                    )
                }
            }
            Text(
                text = stringResource(Res.string.practice_builder_level_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Related)) {
            BuilderSection(heading = stringResource(Res.string.practice_builder_source)) {
                state.sourceOptions.forEach { option ->
                    // Single-select, with a radio button saying so.
                    FilterChip(
                        selected = option.source == state.source,
                        onClick = { onSourceClick(option.source) },
                        label = {
                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(AppSpacing.Related),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = option.source == state.source,
                                    onClick = null,
                                )
                                Text(stringResource(option.source.labelResource()))
                            }
                        },
                        // Disabled rather than absent: the learner can see that weak-area
                        // and mistake practice exist and are not ready yet.
                        enabled = option.isAvailable,
                        modifier = Modifier.testTag(practiceSourceTag(option.source)),
                    )
                }
            }
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

/** What the four answers add up to, and the single control that acts on them. */
private fun LazyListScope.summarySection(
    state: PracticeBuilderUiState,
    onStartClick: () -> Unit,
    onRetryAvailability: () -> Unit,
) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Related)) {
            Text(
                text = state.availability.message(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(PracticeBuilderAvailabilityTag),
            )
            // A failed check is the one availability state the learner cannot resolve by
            // changing the setup, so it is the only one that offers an action.
            if (state.availability is PracticeAvailability.Error) {
                TextButton(onClick = onRetryAvailability) {
                    Text(text = stringResource(Res.string.app_retry))
                }
            }
            Button(
                onClick = onStartClick,
                // The configuration is checked before Start, so an impossible run never
                // navigates into assessment taking to fail there.
                enabled = state.isStartEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(PracticeBuilderStartButtonTag),
            ) {
                val count = (state.availability as? PracticeAvailability.Available)
                    ?.eligibleQuestionCount
                    ?.let { minOf(it, state.questionCount) }
                Text(
                    text = count?.let {
                        pluralStringResource(
                            Res.plurals.practice_builder_start_count,
                            it,
                            it,
                        )
                    } ?: stringResource(Res.string.practice_builder_start),
                )
            }
        }
    }
}

/** The form is the substance of the screen; the summary beside it is short by design. */
private const val ConfigurationPaneWeight = 3f
private const val SummaryPaneWeight = 2f

/**
 * A labelled row of chips that wraps.
 *
 * `FlowRow` rather than a `Row`: four count chips or four source labels do not fit one line on a
 * compact width or at a large font scale, and a fixed row would push the last option off-screen
 * instead of moving it to the next line.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuilderSection(
    heading: String,
    hint: String? = null,
    chips: @Composable () -> Unit,
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
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.Related),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Related),
        ) {
            chips()
        }
    }
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
