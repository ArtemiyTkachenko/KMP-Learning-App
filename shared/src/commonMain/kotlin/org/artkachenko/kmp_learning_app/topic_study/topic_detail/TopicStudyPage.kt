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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.learning_study_progress_unavailable
import kmp_learning_app.shared.generated.resources.learning_unit_lessons_studied
import kmp_learning_app.shared.generated.resources.topic_detail_learning_unavailable
import kmp_learning_app.shared.generated.resources.topic_detail_learning_unit_lessons
import kmp_learning_app.shared.generated.resources.topic_detail_practice_this_topic
import kmp_learning_app.shared.generated.resources.topic_detail_study_empty
import org.artkachenko.kmp_learning_app.lesson_study.LearningUnitStudyProgress
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressSummary
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressUiState
import org.artkachenko.kmp_learning_app.lesson_study.TopicStudyProgress
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.ProgressMeter
import org.artkachenko.kmp_learning_app.ui.ScreenAction
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppContentMargin
import org.artkachenko.kmp_learning_app.ui.theme.appListContentPadding
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
    onBrowsePractice: () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    when (state) {
        TopicLearningUnitsUiState.Loading -> Unit

        // A failed read gets no action. Practice is genuinely available, but offering it here would
        // read as the answer to "the material could not be loaded", which it is not.
        TopicLearningUnitsUiState.Unavailable -> ScreenMessage(
            message = stringResource(Res.string.topic_detail_learning_unavailable),
            modifier = modifier,
        )

        is TopicLearningUnitsUiState.Available -> if (state.units.isEmpty()) {
            // Nothing authored yet is an ordinary state for most Topics, and the learner still came
            // here to do something with this Topic — so the page names the capability that does
            // exist rather than leaving them on a sentence with nowhere to go.
            ScreenAction(
                message = stringResource(Res.string.topic_detail_study_empty),
                actionLabel = stringResource(Res.string.topic_detail_practice_this_topic),
                onAction = onBrowsePractice,
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
    // Full-bleed: the rows own the horizontal margin, so their state layers reach the pane edges.
    // See `appListContentPadding`.
    val margin = LocalAppContentMargin.current
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().testTag(TopicStudyListTag),
        contentPadding = appListContentPadding(top = AppSpacing.Related),
    ) {
        item {
            TopicStudyProgressHeader(studyProgress)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = margin),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
        items(items = units, key = { it.unitId }) { unit ->
            LearningUnitRow(
                unit = unit,
                studyProgress = unitProgress?.get(unit.unitId),
                onLearningUnitClick = onLearningUnitClick,
            )
            HorizontalDivider(
                // Inset to the row's own margin, so the rule stays aligned with the text.
                modifier = Modifier.padding(horizontal = margin),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

/**
 * How far through this Topic's authored material the learner has read, as one figure above the list.
 *
 * The Study tab previously opened straight onto six paragraphs with nothing to orient against, while
 * the aggregate it needed was already being derived and thrown away: `TopicStudyProgress.summary` is
 * lesson-weighted across the Topic's Units, so a Unit of ten Lessons counts for more than a Unit of
 * two, and until now nothing in the app rendered it.
 *
 * The shape is the Unit overview's `UnitStudyProgress`, one level up, and it stays silent in exactly
 * the same three places. Loading says nothing, because a meter at zero drawn before the record has
 * been read is a claim about the learner rather than a report about them. `Empty` — a Topic whose
 * Units hold no current Lessons — says nothing either, since there is no fraction to draw and
 * "0 of 0" renders as finished. Only a failed read speaks, and it says the progress could not be
 * read rather than that nothing has been studied.
 *
 * Said once for the whole page rather than repeated on every row, which would turn one missing
 * record into a wall of identical notices.
 */
@Composable
private fun TopicStudyProgressHeader(studyProgress: StudyProgressUiState<TopicStudyProgress>) {
    when (studyProgress) {
        StudyProgressUiState.Loading -> Unit

        StudyProgressUiState.Unavailable -> Text(
            text = stringResource(Res.string.learning_study_progress_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(
                    start = LocalAppContentMargin.current,
                    end = LocalAppContentMargin.current,
                    bottom = AppSpacing.Comfortable,
                )
                .testTag(TopicStudyUnavailableTag),
        )

        is StudyProgressUiState.Available -> {
            val summary = studyProgress.value.summary as? StudyProgressSummary.Progress ?: return
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LocalAppContentMargin.current)
                    .padding(bottom = AppSpacing.Comfortable)
                    .testTag(TopicStudyProgressTag),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
            ) {
                // The figure is written out as well as drawn, so the meter is a second channel
                // rather than the only one, and no percentage is invented: the ratio is computed
                // here, at the moment of drawing, from the counts the derivation produced.
                Text(
                    text = pluralStringResource(
                        Res.plurals.learning_unit_lessons_studied,
                        summary.totalCount,
                        summary.studiedCount,
                        summary.totalCount,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ProgressMeter(
                    fraction = summary.studiedCount.toFloat() / summary.totalCount,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
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
    // The margin goes inside the clickable, so the state layer spans the pane rather than being
    // inset with the row and drawing a band that hugs the text. See `appListContentPadding`.
    LearningUnitRowContent(
        unit = unit,
        studyProgress = studyProgress,
        modifier = clickable.padding(horizontal = LocalAppContentMargin.current),
    )
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
 * The fraction is stated in words only. A row carried a meter of its own until this page gained the
 * Topic-level one above the list, and the two together turned a curriculum into a stack of bars —
 * four of them on a three-Unit Topic, each redrawing the sentence printed directly above it. One
 * meter for the Topic and a sentence per Unit says the same thing and reads as a list again.
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
        // The only clipped prose in the app: everywhere else text wraps and the layout absorbs it,
        // because no other row carries a whole authored paragraph. Six of them do here, and at full
        // length each Unit runs to most of a phone screen — the list stops being a progression the
        // learner can scan and becomes an essay they have to read to find the row they wanted. The
        // Unit overview opens with this same summary in full, one tap away and a type step larger,
        // so nothing is lost by ending the sentence here.
        Text(
            text = unit.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = SummaryMaxLines,
            overflow = TextOverflow.Ellipsis,
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
    }
}

/** Enough of the Unit's description to tell two Units apart, and no more. */
private const val SummaryMaxLines = 2
