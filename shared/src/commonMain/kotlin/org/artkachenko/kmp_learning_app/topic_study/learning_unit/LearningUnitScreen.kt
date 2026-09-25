package org.artkachenko.kmp_learning_app.topic_study.learning_unit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.learning_lesson_not_studied
import kmp_learning_app.shared.generated.resources.learning_lesson_studied
import kmp_learning_app.shared.generated.resources.learning_practice_unit
import kmp_learning_app.shared.generated.resources.learning_study_progress_unavailable
import kmp_learning_app.shared.generated.resources.learning_unit_lessons_studied
import kmp_learning_app.shared.generated.resources.learning_unit_error
import kmp_learning_app.shared.generated.resources.learning_unit_lessons
import kmp_learning_app.shared.generated.resources.learning_unit_loading
import kmp_learning_app.shared.generated.resources.learning_unit_no_lessons
import kmp_learning_app.shared.generated.resources.learning_unit_not_found
import kmp_learning_app.shared.generated.resources.learning_unit_title
import org.artkachenko.kmp_learning_app.lesson_study.LearningUnitStudyProgress
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressSummary
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressUiState
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.ProgressMeter
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.SectionHeading
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppContentMargin
import org.artkachenko.kmp_learning_app.ui.theme.appListContentPadding
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.artkachenko.kmp_learning_app.ui.theme.AppContentWidth
import org.artkachenko.kmp_learning_app.ui.theme.AppScreenPane

internal const val LearningUnitLoadingTag = "learning_unit_loading"
internal const val LearningUnitPracticeButtonTag = "learning_unit_practice_button"
internal const val LearningUnitStudyProgressTag = "learning_unit_study_progress"
internal const val LearningUnitStudyUnavailableTag = "learning_unit_study_unavailable"

internal fun learningLessonRowTag(lessonId: String): String = "learning_lesson_$lessonId"

/**
 * The Unit overview: what this Unit is about, and what there is to read in it.
 *
 * A reading and discovery surface, so it stays deliberately quiet — no scores and no assessment
 * figures, because none of those exist for learning content and borrowing the assessment screens'
 * visual weight would imply they do. The one figure it does carry is the learner's own study
 * progress over this Unit's current Lessons, which is a count of explicit claims rather than a
 * measurement of performance.
 */
@Composable
internal fun LearningUnitScreen(
    state: LearningUnitUiState,
    onBack: () -> Unit,
    onLessonClick: (String) -> Unit,
    onPracticeUnit: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = rememberAppTopBarScrollBehavior()
    Column(modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        // A stable label rather than the Unit title: the title is the first thing the content
        // says, so repeating it in the bar would say it twice — and the bar would otherwise have
        // nothing to show while the Unit is still resolving or turns out not to be current.
        AppTopBar(stringResource(Res.string.learning_unit_title), onBack, scrollBehavior)
        AppScreenPane(AppContentWidth.Standard) {
            when (state) {
                LearningUnitUiState.Loading -> ScreenLoading(
                    message = stringResource(Res.string.learning_unit_loading),
                    testTag = LearningUnitLoadingTag,
                    modifier = Modifier.weight(1f),
                )
                LearningUnitUiState.NotFound -> ScreenMessage(
                    message = stringResource(Res.string.learning_unit_not_found),
                    modifier = Modifier.weight(1f),
                )
                LearningUnitUiState.Error -> ScreenError(
                    message = stringResource(Res.string.learning_unit_error),
                    onRetry = onRetry,
                    modifier = Modifier.weight(1f),
                )
                is LearningUnitUiState.Content -> LearningUnitContent(
                    state = state,
                    onLessonClick = onLessonClick,
                    onPracticeUnit = onPracticeUnit,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LearningUnitContent(
    state: LearningUnitUiState.Content,
    onLessonClick: (String) -> Unit,
    onPracticeUnit: () -> Unit,
    modifier: Modifier,
) {
    // Full-bleed, because the Lessons below own their horizontal margin: a row's state layer has
    // to reach the pane edges rather than being inset with the row and drawing a band that hugs the
    // text. Everything that is not a row therefore applies the margin itself. See
    // `appListContentPadding`.
    val margin = LocalAppContentMargin.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = appListContentPadding(),
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = margin),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Related),
            ) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = state.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Directly under the Unit's own description, because it describes this Unit: how
                // much of it the learner has claimed to have studied. It sits above Practice this
                // unit rather than below the Lessons, so the pedagogical order of the page —
                // what this Unit is, what you can do with it, what there is to read — is unchanged.
                UnitStudyProgress(state.studyProgress)
            }
        }
        item {
            // Above the Lessons rather than after them: practising the Unit is the second thing
            // this screen offers, and burying it under a list the learner may not scroll would
            // make studying and quizzing look like sequential steps rather than two ways in.
            //
            // A labelled Button, not an icon: the action names what it practises, which is what a
            // screen reader announces and what makes it distinguishable from the Lesson cards.
            // It opens the builder — nothing is started here, so the learner still chooses the
            // length, levels, and source.
            Button(
                onClick = onPracticeUnit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = margin)
                    .padding(top = AppSpacing.Comfortable)
                    .testTag(LearningUnitPracticeButtonTag),
            ) {
                Text(text = stringResource(Res.string.learning_practice_unit))
            }
        }
        if (state.lessons.isEmpty()) {
            item {
                // Controlled rather than fatal: an ACTIVE Unit whose Lessons have all been retired
                // is a coherent document, and nothing is invented to fill the list.
                Text(
                    text = stringResource(Res.string.learning_unit_no_lessons),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = margin)
                        .padding(top = AppSpacing.Comfortable),
                )
            }
        } else {
            item {
                SectionHeading(
                    text = stringResource(Res.string.learning_unit_lessons),
                    modifier = Modifier.padding(horizontal = margin),
                )
                // The rule under the heading is what opens the list, exactly as the Topic's Unit
                // list opens under its progress header. Without it the first Lesson would be the
                // only row in either list with nothing above it.
                HorizontalDivider(
                    modifier = Modifier
                        .padding(horizontal = margin)
                        .padding(top = AppSpacing.Related),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            // Authored order, exactly as the repository returned it: that sequence is how the Unit
            // is meant to be read, so nothing here sorts by title or length. Studied state is an
            // annotation on that order and never a re-ordering of it, and it never gates a row:
            // every Lesson stays a clickable card whether it has been studied or not.
            val studiedLessonIds = state.studyProgress.studiedLessonIds
            items(state.lessons, key = LearningLessonItemUiModel::lessonId) { lesson ->
                LearningLessonRow(
                    lesson = lesson,
                    isStudied = studiedLessonIds?.contains(lesson.lessonId),
                    onClick = { onLessonClick(lesson.lessonId) },
                )
                HorizontalDivider(
                    // Inset to the row's own margin, so the rule stays aligned with the text.
                    modifier = Modifier.padding(horizontal = margin),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

/**
 * The learner's progress through this Unit's current Lessons, when it is known.
 *
 * Silent while the study record is still being read: a meter at zero would be a claim about the
 * learner made before anything was read. `Empty` — an ACTIVE Unit whose Lessons have all been
 * retired — is silent too, because there is no fraction to draw. `0 / 0` renders as complete and 0%
 * announces outstanding work that does not exist, and the overview's existing "no lessons" message
 * is the honest explanation. Only a failed read speaks, and it says the progress is unavailable
 * rather than that nothing has been studied.
 *
 * The figure is written out — "1 of 3 lessons studied" — so the meter beside it is a second channel
 * rather than the only one, and no percentage is invented or persisted: the ratio is computed here,
 * at the moment of drawing, from the counts E22-03 derived.
 */
@Composable
private fun UnitStudyProgress(studyProgress: StudyProgressUiState<LearningUnitStudyProgress>) {
    when (studyProgress) {
        StudyProgressUiState.Loading -> Unit
        StudyProgressUiState.Unavailable -> Text(
            text = stringResource(Res.string.learning_study_progress_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(LearningUnitStudyUnavailableTag),
        )
        is StudyProgressUiState.Available -> {
            val summary = studyProgress.value.summary
            if (summary !is StudyProgressSummary.Progress) return
            Column(
                modifier = Modifier.fillMaxWidth().testTag(LearningUnitStudyProgressTag),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
            ) {
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
 * The studied Lesson IDs the rows may annotate themselves with, or null when study state is not
 * something this screen knows.
 *
 * Null for both Loading and Unavailable, because a row must not read "Not studied" in either case:
 * the first has not been answered yet and the second could not be. Asked once for the whole list so
 * every row agrees, and derived from the E22-03 per-Lesson results rather than from a second set.
 */
private val StudyProgressUiState<LearningUnitStudyProgress>.studiedLessonIds: Set<String>?
    get() = (this as? StudyProgressUiState.Available)
        ?.value
        ?.lessons
        ?.filter { it.isStudied }
        ?.mapTo(mutableSetOf()) { it.lessonId }

/**
 * One Lesson, as a row of the Unit's table of contents.
 *
 * This was a Card, and it was the only list in the study hierarchy still drawn as one. The Topic's
 * Units and a Topic's Subtopics are both full-bleed rows separated by rules, for a reason that
 * applies just as exactly one level further down: an authored sequence is a *progression*, and a
 * card per member draws five independent choices where the content is one ordered document. The
 * rule under each row is now what the container used to be, and the three lists finally read as
 * three depths of one table of contents rather than as three unrelated screens.
 *
 * Emphasis follows the study record and adds no information of its own, exactly as a Unit row's
 * does: a studied Lesson steps its title down to the variant colour and gains a completion mark.
 * [isStudied] is null when study state is unknown, and the row then says nothing about it — no
 * words, no mark, no colour change — because unknown progress must not look like unstarted
 * progress. When it *is* known both values are stated in words, so a row with no mark cannot be
 * mistaken for one that simply has not been studied and the colour is never the only channel.
 *
 * Nothing about the state changes what the row does. Studied Lessons are not moved, hidden, or made
 * unclickable, and the chevron is on every row because every row navigates.
 */
@Composable
private fun LearningLessonRow(
    lesson: LearningLessonItemUiModel,
    isStudied: Boolean?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(learningLessonRowTag(lesson.lessonId))
            .clickable(role = Role.Button, onClick = onClick)
            // The margin goes inside the clickable, so the state layer spans the pane rather than
            // being inset with the row. See `appListContentPadding`.
            .padding(horizontal = LocalAppContentMargin.current, vertical = AppSpacing.Comfortable),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.Related),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isStudied == true) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f),
                )
                if (isStudied == true) {
                    Icon(
                        imageVector = AppIcons.CheckCircle,
                        // The line below says "Studied" in words; the mark is how a learner finds
                        // it while scanning, and repeating it here would announce it twice.
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(StudiedMarkSize),
                    )
                }
            }
            Text(
                text = lesson.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isStudied != null) {
                Text(
                    text = stringResource(
                        if (isStudied) {
                            Res.string.learning_lesson_studied
                        } else {
                            Res.string.learning_lesson_not_studied
                        },
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    // Colour is a second channel behind the words, never the only one.
                    color = if (isStudied) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
        Icon(
            imageVector = AppIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(NavigationChevronSize),
        )
    }
}

/** The completion mark and the navigation chevron, at the size every other row draws them. */
private val StudiedMarkSize = 20.dp
private val NavigationChevronSize = 20.dp
