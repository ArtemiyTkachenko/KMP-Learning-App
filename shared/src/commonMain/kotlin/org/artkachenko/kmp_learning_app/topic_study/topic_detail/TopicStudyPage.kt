package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.learning_study_progress_unavailable
import kmp_learning_app.shared.generated.resources.learning_unit_lessons_studied
import kmp_learning_app.shared.generated.resources.topic_detail_learning_unavailable
import kmp_learning_app.shared.generated.resources.topic_detail_learning_unit_lessons
import kmp_learning_app.shared.generated.resources.topic_detail_study_empty
import org.artkachenko.kmp_learning_app.lesson_study.LearningUnitStudyProgress
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressSummary
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressUiState
import org.artkachenko.kmp_learning_app.lesson_study.TopicStudyProgress
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.ProgressMeter
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The Topic's authored study material: the Study tab's whole content.
 *
 * Four states, and the tab now has to say something in three of them. A dedicated page cannot use
 * the old "absent rather than empty" rule — a blank tab reads as broken — so a successfully empty
 * read says so plainly instead of rendering nothing. Only [TopicLearningUnitsUiState.Loading] is
 * still silent: the Topic is already usable and the page simply fills in when the read resolves.
 *
 * A failed read still says the material could not be *read* rather than that there is none, and
 * still takes no practice control with it: practice lives on its own page and is not reachable from
 * anything here.
 */
@Composable
internal fun TopicStudyPage(
    state: TopicLearningUnitsUiState,
    studyProgress: StudyProgressUiState<TopicStudyProgress>,
    onLearningUnitClick: ((String) -> Unit)?,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    when (state) {
        TopicLearningUnitsUiState.Loading -> Unit

        TopicLearningUnitsUiState.Unavailable -> ScreenMessage(
            message = stringResource(Res.string.topic_detail_learning_unavailable),
            modifier = modifier,
        )

        is TopicLearningUnitsUiState.Available -> if (state.units.isEmpty()) {
            ScreenMessage(
                message = stringResource(Res.string.topic_detail_study_empty),
                modifier = modifier,
            )
        } else {
            LearningUnitList(
                units = state.units,
                studyProgress = studyProgress,
                onLearningUnitClick = onLearningUnitClick,
                listState = listState,
                modifier = modifier,
            )
        }
    }
}

/**
 * The Units as a progression rather than as a stack of equally prominent cards.
 *
 * Dividers and typography carry the hierarchy that a Card per Unit used to carry with a container,
 * which is what lets a Topic's six Units read as one curriculum. Authored order is rendered exactly
 * as the repository returned it: that sequence is pedagogical and is never re-sorted by title,
 * size, or anything the learner has done.
 */
@Composable
private fun LearningUnitList(
    units: List<LearningUnitItemUiModel>,
    studyProgress: StudyProgressUiState<TopicStudyProgress>,
    onLearningUnitClick: ((String) -> Unit)?,
    listState: LazyListState,
    modifier: Modifier,
) {
    // Joined by stable Unit ID rather than by position: the derivation filters to this Topic's
    // ACTIVE home Units and the row list is built from the same read, but an index would be a
    // second, weaker identity that stops meaning the same thing the moment either list changes.
    val unitProgress = studyProgress.unitProgressById
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().testTag(TopicStudyListTag),
        contentPadding = appScreenContentPadding(top = AppSpacing.Related),
    ) {
        // Said once for the section rather than repeated on every row, which would turn one missing
        // record into a wall of identical notices. Only a failed read speaks: a study record still
        // being read says nothing, because the Units are already usable without it.
        if (studyProgress is StudyProgressUiState.Unavailable) {
            item {
                Text(
                    text = stringResource(Res.string.learning_study_progress_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(bottom = AppSpacing.Related)
                        .testTag(TopicStudyUnavailableTag),
                )
            }
        }
        items(items = units, key = { it.unitId }) { unit ->
            LearningUnitRow(
                unit = unit,
                studyProgress = unitProgress?.get(unit.unitId),
                onLearningUnitClick = onLearningUnitClick,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/**
 * The per-Unit study results this page may annotate its rows with, keyed by stable Unit ID, or null
 * when study state is not something the screen knows.
 *
 * Null for Loading as well as Unavailable, so a row can never show "0 of 3" for a record that has
 * not been read yet. Derived once for the whole list, so two rows cannot disagree about whether
 * study state is known.
 */
private val StudyProgressUiState<TopicStudyProgress>.unitProgressById:
    Map<String, LearningUnitStudyProgress>?
    get() = (this as? StudyProgressUiState.Available)
        ?.value
        ?.units
        ?.associateBy(LearningUnitStudyProgress::unitId)

/**
 * One Unit, clickable only when something can actually handle the click.
 *
 * The shell supplies the handler, which opens the Learning Unit overview. Without one — rendered in
 * a test or a preview — the row stays plain informational content, because advertising a control
 * that goes nowhere is worse than a row that reads as content. The click emits the stable Unit ID
 * rather than this model or the authored Unit behind it.
 */
@Composable
private fun LearningUnitRow(
    unit: LearningUnitItemUiModel,
    studyProgress: LearningUnitStudyProgress?,
    onLearningUnitClick: ((String) -> Unit)?,
) {
    val base = Modifier.fillMaxWidth().testTag(learningUnitCardTag(unit.unitId))
    val clickable = if (onLearningUnitClick == null) {
        base
    } else {
        base.clickable { onLearningUnitClick(unit.unitId) }
    }
    LearningUnitRowContent(unit, studyProgress, clickable)
}

/**
 * Title, summary, and how much there is to read — as the learner's own progress through it when
 * that is known, and as the authored count when it is not.
 *
 * The last line is one line either way. "3 lessons" and "1 of 3 lessons studied" answer the same
 * question with different amounts of information, and showing both would state the total twice. So
 * the studied form replaces the plain count when a study record has actually been read, and the
 * plain count stands while the record is loading, unreadable, or describes a Unit with no current
 * Lessons at all — where `Empty` has no fraction to report and "0 of 0" would read as finished.
 *
 * Emphasis follows the same record and adds no information of its own: a finished Unit steps its
 * title down to the variant colour and gains a completion mark, so a learner scanning the list can
 * see where they are in the progression. A Unit with no readable record is rendered exactly as it
 * always was, because unknown progress must not look like unstarted progress.
 *
 * Assessment coverage for this Topic stays on the Practice page: studied Lessons are a claim about
 * reading and coverage is measured from attempts, so the two are never shown as one figure.
 */
@Composable
private fun LearningUnitRowContent(
    unit: LearningUnitItemUiModel,
    studyProgress: LearningUnitStudyProgress?,
    modifier: Modifier,
) {
    val summary = studyProgress?.summary as? StudyProgressSummary.Progress
    val isComplete = summary?.isComplete == true
    Column(
        modifier = modifier.padding(vertical = AppSpacing.Comfortable),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.Related),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = unit.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isComplete) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
            )
            if (isComplete) {
                Icon(
                    imageVector = AppIcons.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text = unit.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (summary == null) {
                pluralStringResource(
                    Res.plurals.topic_detail_learning_unit_lessons,
                    unit.activeLessonCount,
                    unit.activeLessonCount,
                )
            } else {
                pluralStringResource(
                    Res.plurals.learning_unit_lessons_studied,
                    summary.totalCount,
                    summary.studiedCount,
                    summary.totalCount,
                )
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = if (summary == null) {
                Modifier
            } else {
                Modifier.testTag(learningUnitStudyTag(unit.unitId))
            },
        )
        // Only ever driven by a record that was actually read. A meter under a Unit whose progress
        // is still loading or unreadable would draw an empty bar, which states "nothing studied".
        if (summary != null) {
            ProgressMeter(
                fraction = summary.studiedCount.toFloat() / summary.totalCount,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = AppSpacing.Tight),
            )
        }
    }
}
