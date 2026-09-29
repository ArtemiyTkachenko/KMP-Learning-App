package org.artkachenko.kmp_learning_app.curriculum.content

import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.curriculum.learning.content.BundledLearningCurriculumSource
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Keeps Kotlin Multiplatform teaching out of core content, so hiding the `kmp` Topic hides
 * all of it. The structural half of the boundary — mappings and links — is asserted in
 * `BundledLearningCurriculumTest`; this is the lexical half.
 *
 * There is deliberately no allowlist: a KMP mention in core content is fixed in the
 * content. Every Question is scanned, deprecated ones included, because attempt history can
 * still show them.
 */
internal class KmpContentLeakTest {
    @Test
    fun coreQuestionsTeachNoKmpVocabulary() = runTest {
        val questions = BundledCurriculumSource.load().questions.filter { it.topicId != KMP_TOPIC_ID }

        assertTrue(questions.isNotEmpty())
        assertNoLeaks(KmpVocabulary.leaksIn(questions.flatMap { it.learnerTexts() }))
    }

    @Test
    fun coreLearningContentTeachesNoKmpVocabulary() = runTest {
        val units = BundledLearningCurriculumSource.load().units.filter { it.topicId != KMP_TOPIC_ID }

        assertTrue(units.isNotEmpty())
        assertNoLeaks(KmpVocabulary.leaksIn(units.flatMap { it.learnerTexts() }))
    }

    private fun assertNoLeaks(leaks: List<KmpLeak>) {
        if (leaks.isEmpty()) return
        fail(
            "Core content contains KMP vocabulary; move the teaching into a `$KMP_TOPIC_ID` Unit or " +
                "Question, or rewrite it for Android:\n" +
                leaks.joinToString("\n") { "- ${it.location} contains forbidden KMP term \"${it.term}\"" },
        )
    }
}
