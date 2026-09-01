package org.artkachenko.kmp_learning_app.topic_study.practice_builder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.app_retry
import kmp_learning_app.shared.generated.resources.practice_builder_available_questions
import kmp_learning_app.shared.generated.resources.practice_builder_checking
import kmp_learning_app.shared.generated.resources.practice_builder_error
import kmp_learning_app.shared.generated.resources.practice_builder_level_advanced
import kmp_learning_app.shared.generated.resources.practice_builder_level_applied
import kmp_learning_app.shared.generated.resources.practice_builder_level_foundation
import kmp_learning_app.shared.generated.resources.practice_builder_levels
import kmp_learning_app.shared.generated.resources.practice_builder_no_questions
import kmp_learning_app.shared.generated.resources.practice_builder_question_count
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
import kmp_learning_app.shared.generated.resources.practice_builder_title
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.SectionHeading
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal const val PracticeBuilderStartButtonTag = "practice_builder_start"
internal const val PracticeBuilderAvailabilityTag = "practice_builder_availability"

internal fun practiceLevelTag(level: QuestionLevel): String = "practice_builder_level_${level.name}"

internal fun practiceSourceTag(source: PracticeQuestionSource): String =
    "practice_builder_source_${source.name}"

internal fun practiceQuestionCountTag(questionCount: Int): String =
    "practice_builder_count_$questionCount"

/**
 * Four decisions on one scrollable screen, all of them already answered on arrival.
 *
 * The layout is a `LazyColumn` of chip rows rather than a settings form: every control wraps, so a
 * narrow window or a large font scale reflows instead of clipping, and the Start action stays
 * reachable by scrolling rather than being pinned over the content it explains. Back is the top
 * app bar's, matching every other detail and setup destination in the app.
 */
@OptIn(ExperimentalLayoutApi::class)
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
    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(
            title = stringResource(Res.string.practice_builder_title),
            onBack = onBack,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
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
                        FilterChip(
                            selected = option == state.questionCount,
                            onClick = { onQuestionCountClick(option) },
                            label = { Text(option.toString()) },
                            modifier = Modifier.testTag(practiceQuestionCountTag(option)),
                        )
                    }
                }
            }
            item {
                BuilderSection(heading = stringResource(Res.string.practice_builder_levels)) {
                    QuestionLevel.entries.forEach { level ->
                        FilterChip(
                            selected = level in state.levels,
                            onClick = { onLevelClick(level) },
                            label = { Text(stringResource(level.labelResource())) },
                            modifier = Modifier.testTag(practiceLevelTag(level)),
                        )
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BuilderSection(heading = stringResource(Res.string.practice_builder_source)) {
                        state.sourceOptions.forEach { option ->
                            FilterChip(
                                selected = option.source == state.source,
                                onClick = { onSourceClick(option.source) },
                                label = { Text(stringResource(option.source.labelResource())) },
                                // Disabled rather than absent: the learner can see that unseen,
                                // weak area, and mistake practice exist and are not ready yet.
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
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        Text(text = stringResource(Res.string.practice_builder_start))
                    }
                }
            }
        }
    }
}

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
    chips: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeading(text = heading)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
