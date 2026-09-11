package org.artkachenko.kmp_learning_app.topic_study.learning_lesson

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.learning_lesson_callout_common_mistake
import kmp_learning_app.shared.generated.resources.learning_lesson_callout_interview_focus
import kmp_learning_app.shared.generated.resources.learning_lesson_callout_key_takeaway
import kmp_learning_app.shared.generated.resources.learning_lesson_callout_note
import kmp_learning_app.shared.generated.resources.learning_lesson_depth_core
import kmp_learning_app.shared.generated.resources.learning_lesson_depth_practical
import kmp_learning_app.shared.generated.resources.learning_lesson_depth_senior
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningBlock
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningCalloutKind
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningDepth
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningSection
import org.artkachenko.kmp_learning_app.ui.SectionHeading
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Both tags sit on the *scrolling* element rather than on its container, so one node answers both
 * questions a test has: it carries the horizontal scroll action, and its bounds are the clipped
 * viewport the block is allowed to occupy rather than the width of the content inside it.
 */
internal const val LearningLessonCodeBlockTag = "learning_lesson_code_block"
internal const val LearningLessonComparisonTag = "learning_lesson_comparison"

/**
 * Renders one authored Section: its depth layer, its optional subheading, and its blocks.
 *
 * [showDepthHeading] is decided by the caller rather than here because it depends on the Section
 * before this one. A Lesson may carry several Sections at the same depth — the bundled Compose
 * Unit already does — and repeating "Core" above each of them would turn a layer marker into
 * noise. Order and grouping are untouched: the Sections still render exactly as authored, only the
 * marker above them is drawn once per run.
 */
@Composable
internal fun LearningSectionContent(
    section: LearningSection,
    showDepthHeading: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
    ) {
        if (showDepthHeading) {
            SectionHeading(stringResource(section.depth.labelResource()))
        }
        // Optional, and absent means absent: no placeholder heading and no reserved gap, so a
        // Lesson whose depth layers are each a single Section reads as one continuous piece.
        section.title?.let { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
        }
        section.blocks.forEach { block -> LearningBlockContent(block) }
    }
}

/**
 * The one place a block variant becomes pixels.
 *
 * The `when` is exhaustive with no `else`. That is deliberate: `LearningBlock` is sealed, so adding
 * an authored variant should fail to compile here until it has a renderer, rather than silently
 * reaching learners as a gap in the page or as developer copy about an unsupported type.
 */
@Composable
internal fun LearningBlockContent(
    block: LearningBlock,
    modifier: Modifier = Modifier,
) {
    when (block) {
        is LearningBlock.Paragraph -> ParagraphBlock(block, modifier)
        is LearningBlock.BulletList -> BulletListBlock(block, modifier)
        is LearningBlock.Code -> CodeBlock(block, modifier)
        is LearningBlock.Comparison -> ComparisonBlock(block, modifier)
        is LearningBlock.Callout -> CalloutBlock(block, modifier)
    }
}

/**
 * Authored prose, rendered as prose.
 *
 * The curriculum's prose is authored with a small Markdown subset. Keeping parsing here means a
 * lesson reads consistently regardless of the block that contains it, without mutating stable
 * authored content or making each lesson compensate for renderer details.
 */
@Composable
private fun ParagraphBlock(
    block: LearningBlock.Paragraph,
    modifier: Modifier = Modifier,
) {
    Text(
        text = block.text.toLessonAnnotatedString(),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * An unordered list, one item per row.
 *
 * Each item is its own `Text` beside its marker rather than one joined string, so a long item wraps
 * against the text column instead of running back under the bullet. Nothing is numbered — the
 * authored type is unordered — and there is no nesting, because the model has none.
 */
@Composable
private fun BulletListBlock(
    block: LearningBlock.BulletList,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Related),
    ) {
        block.items.forEach { item ->
            Row(Modifier.fillMaxWidth()) {
                Text(
                    // A typographic marker rather than display copy, which is why it is not a
                    // localized string: there is no wording here to translate.
                    text = BulletMarker,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(BulletMarkerWidth),
                )
                Text(
                    text = item.toLessonAnnotatedString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * A code example in a tonal container, scrolling horizontally on its own.
 *
 * The container fills the reading column and the scroll lives inside it, so a source line wider
 * than the window moves the code and nothing else — the page itself never becomes wider than the
 * screen. Long lines are not wrapped (`softWrap = false`): wrapping code changes what the example
 * looks like, and horizontal scrolling preserves the authored line structure instead.
 *
 * The code stays ordinary readable text with no syntax colouring. Highlighting would mean a new
 * dependency, and colour spans earn nothing for text that is also read aloud or copied out.
 */
@Composable
private fun CodeBlock(
    block: LearningBlock.Code,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.Grouped),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Related),
        ) {
            // The authored identifier verbatim. It is metadata the author already chose, so no
            // per-language resource exists to disagree with it or to leave a language unlabelled.
            block.language?.takeIf { it.isNotBlank() }?.let { language ->
                Text(
                    text = language,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = block.code,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                softWrap = false,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .testTag(LearningLessonCodeBlockTag),
            )
        }
    }
}

/** A wide table or a compact stack, chosen once at the reusable content boundary. */
@Composable
private fun ComparisonBlock(
    block: LearningBlock.Comparison,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < CompactComparisonBreakpoint) {
                CompactComparison(block)
            } else {
                WideComparison(block)
            }
        }
    }
}

@Composable
private fun WideComparison(block: LearningBlock.Comparison) {
    Column(
        Modifier.horizontalScroll(rememberScrollState()).testTag(LearningLessonComparisonTag),
    ) {
            Row {
                block.headers.forEach { header ->
                    ComparisonCell(
                        text = header,
                        // Weight, not colour alone: the header row still reads as headings in a
                        // monochrome or high-contrast rendering.
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            block.rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row {
                    row.forEach { cell ->
                        ComparisonCell(
                        text = cell,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
    }
}

/** On phones, rows become labelled facts rather than a hidden horizontal spreadsheet. */
@Composable
private fun CompactComparison(block: LearningBlock.Comparison) {
    Column(
        Modifier.fillMaxWidth().testTag(LearningLessonComparisonTag),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
    ) {
        block.rows.forEachIndexed { rowIndex, row ->
            Column(
                Modifier.fillMaxWidth().padding(AppSpacing.Grouped),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Related),
            ) {
                row.firstOrNull()?.let { concern ->
                    Text(concern.toLessonAnnotatedString(), style = MaterialTheme.typography.titleSmall)
                }
                row.drop(1).forEachIndexed { index, value ->
                    val heading = block.headers.getOrNull(index + 1) ?: return@forEachIndexed
                    Text(heading.toLessonAnnotatedString(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value.toLessonAnnotatedString(), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (rowIndex < block.rows.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun ComparisonCell(
    text: String,
    style: TextStyle,
    color: Color,
) {
    Text(
        text = text.toLessonAnnotatedString(),
        style = style,
        color = color,
        modifier = Modifier.width(ComparisonColumnWidth).padding(AppSpacing.Grouped),
    )
}

/**
 * Emphasised content, labelled with what the emphasis means.
 *
 * The visible label is the accessible one: it is ordinary text read immediately before the callout
 * body, so a screen reader already announces "Interview focus" and then the sentence, and adding a
 * content description would only make it say so twice. Colour is a second signal rather than the
 * signal — the four kinds stay distinguishable with the tone removed.
 */
@Composable
private fun CalloutBlock(
    block: LearningBlock.Callout,
    modifier: Modifier = Modifier,
) {
    val colors = block.kind.calloutColors()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.container,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.Comfortable),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
        ) {
            Text(
                text = stringResource(block.kind.labelResource()),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onContainer,
            )
            Text(
            text = block.text.toLessonAnnotatedString(),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onContainer,
            )
        }
    }
}

/**
 * Learner-facing wording for a depth layer.
 *
 * `CORE` is a serialization discriminator, not something a learner should ever read, so the mapping
 * lives here rather than as a display field on the enum — that would put presentation into the
 * authored model.
 */
private fun LearningDepth.labelResource(): StringResource =
    when (this) {
        LearningDepth.CORE -> Res.string.learning_lesson_depth_core
        LearningDepth.PRACTICAL -> Res.string.learning_lesson_depth_practical
        LearningDepth.SENIOR -> Res.string.learning_lesson_depth_senior
    }

private fun LearningCalloutKind.labelResource(): StringResource =
    when (this) {
        LearningCalloutKind.NOTE -> Res.string.learning_lesson_callout_note
        LearningCalloutKind.KEY_TAKEAWAY -> Res.string.learning_lesson_callout_key_takeaway
        LearningCalloutKind.INTERVIEW_FOCUS -> Res.string.learning_lesson_callout_interview_focus
        LearningCalloutKind.COMMON_MISTAKE -> Res.string.learning_lesson_callout_common_mistake
    }

@Immutable
private data class CalloutColors(
    val container: Color,
    val onContainer: Color,
)

/**
 * Theme roles only, chosen for what each kind means.
 *
 * `COMMON_MISTAKE` takes Material's error container rather than the app's answer-correctness
 * tokens: those belong to scoring, and a Lesson borrowing them would start reading like a graded
 * screen. Nothing here is a brand literal, so both themes follow the scheme.
 */
@Composable
private fun LearningCalloutKind.calloutColors(): CalloutColors =
    when (this) {
        LearningCalloutKind.NOTE -> CalloutColors(
            container = MaterialTheme.colorScheme.surfaceContainerHigh,
            onContainer = MaterialTheme.colorScheme.onSurface,
        )
        LearningCalloutKind.KEY_TAKEAWAY -> CalloutColors(
            container = MaterialTheme.colorScheme.primaryContainer,
            onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        LearningCalloutKind.INTERVIEW_FOCUS -> CalloutColors(
            container = MaterialTheme.colorScheme.tertiaryContainer,
            onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        LearningCalloutKind.COMMON_MISTAKE -> CalloutColors(
            container = MaterialTheme.colorScheme.errorContainer,
            onContainer = MaterialTheme.colorScheme.onErrorContainer,
        )
    }

private const val BulletMarker = "•"

private val BulletMarkerWidth: Dp = 20.dp

private val CompactComparisonBreakpoint = 520.dp

/** Small, intentionally conservative Markdown subset supported by the learning format. */
private fun String.toLessonAnnotatedString() = buildAnnotatedString {
    appendLessonMarkdown(this@toLessonAnnotatedString)
}

private fun AnnotatedString.Builder.appendLessonMarkdown(text: String) {
    var cursor = 0
    val pattern = Regex("(\\*\\*[^*]+\\*\\*)|(`[^`]+`)|(\\*[^*]+\\*)|(\\[[^]]+](?:\\([^)]*\\)))")
    pattern.findAll(text).forEach { match ->
        append(text.substring(cursor, match.range.first))
        val token = match.value
        when {
            token.startsWith("**") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                appendLessonMarkdown(token.drop(2).dropLast(2))
            }
            token.startsWith('`') -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(token.drop(1).dropLast(1)) }
            token.startsWith('*') -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                appendLessonMarkdown(token.drop(1).dropLast(1))
            }
            token.startsWith('[') -> {
                val label = token.substringAfter('[').substringBefore(']')
                withStyle(SpanStyle(fontWeight = FontWeight.Medium)) {
                    appendLessonMarkdown(label)
                }
            }
        }
        cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}

/**
 * Wide enough for a short sentence to wrap sensibly, narrow enough that a three-column table is
 * only just wider than a phone rather than several screens wide.
 */
private val ComparisonColumnWidth: Dp = 200.dp
