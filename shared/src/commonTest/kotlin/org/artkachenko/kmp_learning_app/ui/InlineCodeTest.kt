package org.artkachenko.kmp_learning_app.ui

import androidx.compose.ui.text.AnnotatedString
import kotlin.test.Test
import kotlin.test.assertEquals

internal class InlineCodeTest {
    @Test
    fun textWithoutCodeIsUnchanged() {
        assertInlineCode("Which scope survives recomposition?", "Which scope survives recomposition?")
    }

    @Test
    fun aSingleSpanLosesItsBackticksAndBecomesCode() {
        assertInlineCode(
            "Why does `LaunchedEffect(Unit)` run once?",
            "Why does LaunchedEffect(Unit) run once?",
            "LaunchedEffect(Unit)",
        )
    }

    @Test
    fun everySpanInTheTextIsCode() {
        assertInlineCode(
            "Compare `launch`, `async` and `withTimeout`.",
            "Compare launch, async and withTimeout.",
            "launch",
            "async",
            "withTimeout",
        )
    }

    @Test
    fun spansAtTheStartAndEndAreCode() {
        assertInlineCode("`onTimeout` fires after `delay`", "onTimeout fires after delay", "onTimeout", "delay")
    }

    @Test
    fun adjacentSpansStaySeparateCodeSpans() {
        assertInlineCode("`a``b`", "ab", "a", "b")
    }

    @Test
    fun anUnmatchedBacktickStaysLiteralAndNothingIsDropped() {
        assertInlineCode("A stray ` backtick", "A stray ` backtick")
        assertInlineCode("`code` then ` alone", "code then ` alone", "code")
    }

    @Test
    fun anEmptyPairIsNotCode() {
        assertInlineCode("Empty `` pair", "Empty `` pair")
    }

    @Test
    fun markdownLikeCharactersOutsideCodeAreNotInterpreted() {
        assertInlineCode(
            "Use *args, _name and [list] with `a * b`",
            "Use *args, _name and [list] with a * b",
            "a * b",
        )
        assertInlineCode("**not bold** and *not italic*", "**not bold** and *not italic*")
    }

    private fun assertInlineCode(input: String, expectedText: String, vararg expectedCode: String) {
        val result = input.withInlineCode()
        assertEquals(expectedText, result.text)
        assertEquals(expectedCode.toList(), result.codeSpans())
    }

    private fun AnnotatedString.codeSpans(): List<String> = spanStyles
        .onEach { assertEquals(InlineCodeStyle, it.item) }
        .sortedBy { it.start }
        .map { text.substring(it.start, it.end) }
}
