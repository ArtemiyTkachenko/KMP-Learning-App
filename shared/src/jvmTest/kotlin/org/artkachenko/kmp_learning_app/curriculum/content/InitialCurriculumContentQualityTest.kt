package org.artkachenko.kmp_learning_app.curriculum.content

import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.Question
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Deterministic content-quality gates for the bundled question bank.
 *
 * [InitialCurriculumSmokeTest] asserts the bank's shape and structural validity. This test
 * asserts the editorial invariants that `docs/content/question-validation.md` lists as
 * machine-enforceable, so the mechanical audits in the authoring playbook run in CI instead of
 * being copy-pasted into a shell by hand.
 *
 * Everything these checks cannot decide — whether a distractor is plausible, whether a stem is
 * ambiguous, whether a source substantiates its claim — stays in the reasoning-based rubric.
 */
internal class InitialCurriculumContentQualityTest {
    @Test
    fun bundledQuestionStemsAreNotDuplicated() = runTest {
        val duplicates = BundledCurriculumSource.load().questions
            .groupBy { it.text.trim().lowercase() }
            .filterValues { it.size > 1 }
            .map { (_, questions) -> questions.map(Question::id) }

        assertEquals(emptyList(), duplicates, "Question stems must be unique across the bank.")
    }

    @Test
    fun bundledMultipleSelectionQuestionsTellTheReaderTheModeExplicitly() = runTest {
        val missingPrompt = BundledCurriculumSource.load().questions
            .filter { it.selectionMode == AnswerSelectionMode.MULTIPLE }
            .filterNot { it.text.contains(MULTIPLE_SELECTION_PROMPT) }
            .map(Question::id)

        assertEquals(emptyList(), missingPrompt, "MULTIPLE questions must say \"$MULTIPLE_SELECTION_PROMPT\"")
    }

    @Test
    fun bundledSourcesCiteApprovedPrimaryDocumentationHosts() = runTest {
        val offenders = BundledCurriculumSource.load().questions
            .flatMap { question -> question.sources.map { question.id to it.url } }
            .filterNot { (_, url) -> url.host() in APPROVED_SOURCE_HOSTS }

        assertEquals(
            emptyList(),
            offenders,
            "Sources must cite an approved primary documentation host; extend APPROVED_SOURCE_HOSTS " +
                "deliberately rather than citing a blog, an aggregator, or a placeholder domain.",
        )
    }

    /**
     * The anti-cue length audit from `docs/content/question-authoring-playbook.md`. A correct
     * answer that is conspicuously the longest option lets a candidate pass by test-taking
     * strategy rather than knowledge, so no keyed answer may exceed the longest distractor by
     * more than 10%.
     */
    @Test
    fun noKeyedAnswerIsConspicuouslyLongerThanItsLongestDistractor() = runTest {
        val offenders = BundledCurriculumSource.load().questions
            .filter { it.correctAnswerIds.size == 1 }
            .mapNotNull { question ->
                val lengths = question.answers.associate { it.id to it.text.length }
                val keyed = lengths[question.correctAnswerIds.single()] ?: return@mapNotNull null
                val longestDistractor = lengths
                    .filterKeys { it !in question.correctAnswerIds }
                    .values
                    .maxOrNull() ?: return@mapNotNull null

                "${question.id} (keyed $keyed, longest distractor $longestDistractor)"
                    .takeIf { keyed > longestDistractor * LENGTH_TOLERANCE }
            }

        assertEquals(emptyList(), offenders, "Keyed answers must not be conspicuously the longest option.")
    }

    /**
     * The anti-cue absolutes audit. The defect this catches is not the use of absolute words but
     * their *distribution*: if only distractors ever contain them, "never pick the absolute"
     * becomes a knowledge-free strategy. Absolutes belong in keyed answers wherever they are
     * literally true.
     */
    @Test
    fun absoluteWordsAppearInKeyedAnswersAndNotOnlyInDistractors() = runTest {
        val questions = BundledCurriculumSource.load().questions
        var keyedHits = 0
        var distractorHits = 0

        questions.forEach { question ->
            question.answers.forEach { answer ->
                val hits = ABSOLUTE_WORDS.findAll(answer.text).count()
                if (answer.id in question.correctAnswerIds) keyedHits += hits else distractorHits += hits
            }
        }

        assertTrue(distractorHits > 0, "Expected the bank to use absolute words in distractors.")
        assertTrue(
            keyedHits > 0,
            "Absolute words appear only in distractors ($distractorHits occurrences), which makes " +
                "\"never pick the absolute\" a reliable strategy. Use absolutes in keyed answers " +
                "wherever they are literally true.",
        )
    }

    private fun String.host() = substringAfter("://").substringBefore('/').substringBefore('?').lowercase()

    private companion object {
        const val MULTIPLE_SELECTION_PROMPT = "Select all that apply."
        const val LENGTH_TOLERANCE = 1.10

        val ABSOLUTE_WORDS = Regex(
            """\b(always|never|completely|automatically|guaranteed|only|every|cannot|impossible)\b""",
            RegexOption.IGNORE_CASE,
        )

        /**
         * The source hierarchy from `docs/content/content-authoring.md`: official platform,
         * language, and library documentation only. Never SEO interview sites, blogs, or forums.
         */
        val APPROVED_SOURCE_HOSTS = setOf(
            "developer.android.com",
            "kotlinlang.org",
            "source.android.com",
            "docs.gradle.org",
            "firebase.google.com",
            "docs.cloud.google.com",
            "google.aip.dev",
            "dagger.dev",
            "insert-koin.io",
            "ktor.io",
            "sqldelight.github.io",
            "www.sqlite.org",
            "www.rfc-editor.org",
            "www.jetbrains.com",
            "github.com",
        )
    }
}
