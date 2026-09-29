package org.artkachenko.kmp_learning_app.curriculum.content

import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningBlock
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningCalloutKind
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningDepth
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningLesson
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningSection
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Proves the leak scanner itself, so the bundled-content leak test cannot pass merely
 * because the scanner stopped detecting anything.
 */
internal class KmpVocabularyTest {
    @Test
    fun detectsEachForbiddenTermAndItsFormattingVariants() {
        mapOf(
            "Put the interface in commonMain." to "commonMain",
            "Kotlin Multiplatform shares this code." to "Multiplatform",
            "Compose Multiplatform renders it on desktop." to "Multiplatform",
            "A multiplatform library." to "Multiplatform",
            "Most KMP projects do this." to "KMP",
            "Kotlin/Native compiles it for iOS." to "Kotlin/Native",
            "kotlin / native memory model" to "Kotlin/Native",
            "Declare it with expect/actual." to "expect/actual",
            "Declare it with expect / actual." to "expect/actual",
            "Declare it with `expect`/`actual`." to "expect/actual",
            "The iosMain source set supplies it." to "iosMain",
        ).forEach { (text, term) -> assertEquals(listOf(term), KmpVocabulary.termsIn(text), text) }
    }

    @Test
    fun ignoresGenericEngineeringVocabulary() {
        listOf(
            "The host Activity owns the platform lifecycle.",
            "Shared state lives in a source set of its own.",
            "The same pattern appears on iOS and on the desktop.",
            "We expected the actual value to match.",
            "An actual class is expected here.",
            "import org.artkachenko.kmp_learning_app.ui",
            "commonMainActivity is not a source set.",
        ).forEach { text -> assertEquals(emptyList(), KmpVocabulary.termsIn(text), text) }
    }

    @Test
    fun reportsEveryLeakInOnePassWithItsLocation() {
        val leaks = KmpVocabulary.leaksIn(
            listOf(
                LearnerText("first", "KMP code in commonMain"),
                LearnerText("second", "Plain Android"),
                LearnerText("third", "Kotlin/Native"),
            ),
        )

        assertEquals(
            listOf(KmpLeak("first", "KMP"), KmpLeak("first", "commonMain"), KmpLeak("third", "Kotlin/Native")),
            leaks,
        )
    }

    @Test
    fun questionSourcesAreNotLearnerText() {
        val question = Question(
            id = "q",
            topicId = "android_ui",
            subtopicId = "compose_state",
            text = "Which call observes the state?",
            answers = listOf(AnswerOption("a", "Reading value"), AnswerOption("b", "Writing value")),
            selectionMode = AnswerSelectionMode.SINGLE,
            level = QuestionLevel.FOUNDATION,
            correctAnswerIds = listOf("a"),
            explanation = "Reading records the dependency.",
            sources = listOf(
                SourceReference(
                    title = "SnapshotState.kt (Compose Multiplatform mirror)",
                    url = "https://android.googlesource.com/platform/frameworks/support/+/androidx-main/" +
                        "compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/SnapshotState.kt",
                ),
            ),
        )

        assertEquals(emptyList(), KmpVocabulary.leaksIn(question.learnerTexts()))
        assertEquals(
            listOf(
                "Question q text",
                "Question q answer a",
                "Question q answer b",
                "Question q explanation",
            ),
            question.learnerTexts().map { it.location },
        )
    }

    @Test
    fun lessonSourcesAreNotLearnerTextButEveryBlockKindIs() {
        val unit = LearningUnit(
            id = "unit",
            topicId = "android_ui",
            title = "Unit",
            summary = "Summary",
            lessons = listOf(
                LearningLesson(
                    id = "lesson",
                    title = "Lesson",
                    summary = "Summary",
                    primarySubtopicIds = listOf("compose_state"),
                    supportingSubtopicIds = emptyList(),
                    sections = listOf(
                        LearningSection(
                            depth = LearningDepth.PRACTICAL,
                            title = "Section",
                            blocks = listOf(
                                LearningBlock.Paragraph("KMP"),
                                LearningBlock.BulletList(listOf("safe", "commonMain")),
                                LearningBlock.Code("// Kotlin/Native"),
                                LearningBlock.Comparison(listOf("Multiplatform"), listOf(listOf("safe"))),
                                LearningBlock.Callout(LearningCalloutKind.NOTE, "expect/actual"),
                            ),
                        ),
                    ),
                    relatedLessonIds = emptyList(),
                    sources = listOf(
                        SourceReference(
                            title = "Kotlin Multiplatform guide",
                            url = "https://example.com/src/commonMain/kotlin/State.kt",
                        ),
                    ),
                ),
            ),
        )

        assertEquals(
            listOf("KMP", "commonMain", "Kotlin/Native", "Multiplatform", "expect/actual"),
            KmpVocabulary.leaksIn(unit.learnerTexts()).map { it.term },
        )
        assertEquals(
            "Lesson lesson PRACTICAL section 1 block 2 (BulletList)",
            KmpVocabulary.leaksIn(unit.learnerTexts())[1].location,
        )
    }
}
