package org.artkachenko.kmp_learning_app.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle

/**
 * How a `code` span reads wherever authored content contains one. Lessons and questions share it,
 * so an identifier looks the same in the explanation that teaches it and the question that tests it.
 *
 * Monospace only, without a background tint: question text sits on six different containers —
 * resting, selected, correct, partially correct, incorrect, and the explanation block — in two
 * themes, and one tint cannot be tuned against all of them without costing contrast on some.
 */
internal val InlineCodeStyle = SpanStyle(fontFamily = FontFamily.Monospace)

private val InlineCodeSpan = Regex("`[^`]+`")

/**
 * Question content with its `` `code` `` spans rendered as code and nothing else interpreted.
 *
 * Questions are not lesson Markdown: authored text contains literal `*`, `_` and `[`, which the
 * lesson subset would turn into emphasis or links. Only a matched, non-empty backtick pair is
 * consumed; an unmatched backtick stays as a literal character, so nothing authored is dropped.
 */
internal fun String.withInlineCode(): AnnotatedString {
    val source = this
    return buildAnnotatedString {
        var cursor = 0
        InlineCodeSpan.findAll(source).forEach { match ->
            append(source, cursor, match.range.first)
            withStyle(InlineCodeStyle) { append(source, match.range.first + 1, match.range.last) }
            cursor = match.range.last + 1
        }
        append(source, cursor, source.length)
    }
}
