package org.artkachenko.kmp_learning_app.curriculum.content

import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningBlock
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit

/**
 * The single content visibility boundary: KMP material is whatever the curriculum places
 * under this Topic. Units, Lessons, Subtopics and Questions are classified through it
 * rather than through a flag or a naming convention of their own.
 */
internal const val KMP_TOPIC_ID = "kmp"

/**
 * One learner-visible string of authored content, with enough location to find it.
 *
 * Only text a learner reads is represented. Source titles and URLs, IDs and other
 * authoring metadata are deliberately absent: an AndroidX source path such as
 * `.../commonMain/...` does not teach KMP to anyone.
 */
internal data class LearnerText(
    val location: String,
    val text: String,
)

/** A forbidden [term] found at [location]. */
internal data class KmpLeak(
    val location: String,
    val term: String,
)

/**
 * Authoring-time scan for Kotlin Multiplatform teaching in core (non-`kmp`) content.
 *
 * Each pattern names a KMP-only concept, so generic engineering words — host, platform,
 * shared, source set, iOS — are intentionally not in the vocabulary.
 */
internal object KmpVocabulary {
    private val patterns: List<Pair<String, Regex>> = listOf(
        // Covers "Kotlin Multiplatform", "Compose Multiplatform" and "multiplatform".
        "Multiplatform" to Regex("""multiplatform""", RegexOption.IGNORE_CASE),
        "KMP" to Regex("""\bKMP\b""", RegexOption.IGNORE_CASE),
        "commonMain" to Regex("""\bcommonMain\b""", RegexOption.IGNORE_CASE),
        "iosMain" to Regex("""\biosMain\b""", RegexOption.IGNORE_CASE),
        "Kotlin/Native" to Regex("""\bKotlin\s*/\s*Native\b""", RegexOption.IGNORE_CASE),
        // Covers "expect/actual", "expect / actual" and "`expect`/`actual`".
        "expect/actual" to Regex("""\bexpect`?\s*/\s*`?actual\b""", RegexOption.IGNORE_CASE),
    )

    /** The vocabulary, in the order it is reported. */
    val terms: List<String> = patterns.map { it.first }

    fun termsIn(text: String): List<String> =
        patterns.filter { (_, pattern) -> pattern.containsMatchIn(text) }.map { it.first }

    fun leaksIn(texts: List<LearnerText>): List<KmpLeak> =
        texts.flatMap { text -> termsIn(text.text).map { term -> KmpLeak(text.location, term) } }
}

/** Question stem, every answer option, and the explanation. */
internal fun Question.learnerTexts(): List<LearnerText> =
    listOf(LearnerText("Question $id text", text)) +
        answers.map { answer -> LearnerText("Question $id answer ${answer.id}", answer.text) } +
        LearnerText("Question $id explanation", explanation)

/** Unit title and summary, then each Lesson's title, summary, section titles and blocks. */
internal fun LearningUnit.learnerTexts(): List<LearnerText> =
    listOf(
        LearnerText("Unit $id title", title),
        LearnerText("Unit $id summary", summary),
    ) + lessons.flatMap { lesson ->
        listOf(
            LearnerText("Lesson ${lesson.id} title", lesson.title),
            LearnerText("Lesson ${lesson.id} summary", lesson.summary),
        ) + lesson.sections.flatMapIndexed { sectionIndex, section ->
            val sectionLocation = "Lesson ${lesson.id} ${section.depth} section ${sectionIndex + 1}"
            listOfNotNull(section.title?.let { LearnerText("$sectionLocation title", it) }) +
                section.blocks.mapIndexed { blockIndex, block ->
                    LearnerText(
                        "$sectionLocation block ${blockIndex + 1} (${block::class.simpleName})",
                        block.learnerText(),
                    )
                }
        }
    }

private fun LearningBlock.learnerText(): String = when (this) {
    is LearningBlock.Paragraph -> text
    is LearningBlock.BulletList -> items.joinToString("\n")
    is LearningBlock.Code -> code
    is LearningBlock.Comparison -> (headers + rows.flatten()).joinToString("\n")
    is LearningBlock.Callout -> text
}
