package org.artkachenko.kmp_learning_app.curriculum.learning.content

import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.content.BundledCurriculumSource
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningBlock
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningCurriculum
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningDepth
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningLesson
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningSection
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Proves the whole publisher-owned learning path holds together on the shipped content:
 * bundled resource -> [LearningContentLoader] -> [BundledLearningContentRepository], read
 * through [LearningContentRepository] because that interface is exactly what a future
 * Learn surface will consume. Nothing here is mocked and no private state is reached into;
 * only the repository contract is exercised, so the assertions stay true of any later
 * implementation of it.
 *
 * The complementary pieces are covered elsewhere and are deliberately not repeated:
 * `LearningCurriculumJsonCodecTest` pins every serialized block variant,
 * `LearningCurriculumValidatorTest` pins every authoring rule,
 * `LearningContentLoaderTest` and `BundledLearningContentRepositoryTest` pin failure and
 * caching behavior, and `BundledLearningCurriculumTest` pins what the shipped document
 * says. What is only provable here is that those layers compose without any of them
 * dropping, reordering, or narrowing authored content on the way out.
 *
 * The repository is built with no database, driver, or platform binding, which is the
 * architectural point of EPIC-20: authored learning content is an in-memory document,
 * while learner and assessment state stays in Room.
 */
internal class LearningContentEndToEndTest {
    @Test
    fun bundledLearningContentReachesConsumersThroughTheRepository() = runTest {
        val repository: LearningContentRepository = BundledLearningContentRepository()

        val units = repository.getActiveUnitsByTopic("android_ui")

        assertEquals(listOf("unit_thinking_in_compose"), units.map { it.id })
        // Stable identity is authored identity: the Unit reached by browsing a Topic and the
        // Unit resolved by its id are the same content, whatever the instances are.
        assertEquals(units.single(), repository.getUnitById("unit_thinking_in_compose"))
        assertNull(repository.getUnitById("unit_missing"))
    }

    @Test
    fun authoredLessonOrderSurvivesToTheRepository() = runTest {
        val unit = assertNotNull(BundledLearningContentRepository().getUnitById("unit_thinking_in_compose"))

        // List position is the ordering contract, so this is asserted unsorted: the blueprint
        // builds the mental model before execution and execution before state flow.
        assertEquals(
            listOf(
                "lesson_declarative_ui",
                "lesson_composable_execution",
                "lesson_state_down_events_up",
            ),
            unit.lessons.map { it.id },
        )
    }

    @Test
    fun everyAuthoredLessonResolvesGloballyByItsOwnId() = runTest {
        val repository = BundledLearningContentRepository()
        val unit = assertNotNull(repository.getUnitById("unit_thinking_in_compose"))

        unit.lessons.forEach { authored ->
            // Lesson ids are unique document-wide, so a caller holding one never needs to know
            // which Unit contains it.
            assertEquals(authored, repository.getLessonById(authored.id), authored.id)
        }
        assertNull(repository.getLessonById("lesson_missing"))
    }

    @Test
    fun aLessonKeepsASupportingConceptOwnedByAnotherTopic() = runTest {
        val repository = BundledLearningContentRepository()

        val lesson = assertNotNull(repository.getLessonById("lesson_state_down_events_up"))
        assertContains(lesson.supportingSubtopicIds, "unidirectional_data_flow")

        // The product rule made explicit against the real taxonomy: the Unit is browsed under
        // Android UI, while the concept it leans on belongs to Architecture. A Unit's home
        // Topic decides where it is found, not which concepts it may teach.
        val curriculum = BundledCurriculumSource.load()
        assertEquals("android_ui", repository.getUnitById("unit_thinking_in_compose")?.topicId)
        assertEquals(
            "architecture",
            curriculum.subtopics.single { it.id == "unidirectional_data_flow" }.topicId,
        )
    }

    @Test
    fun aPrimaryConceptOwnedByAnotherTopicSurvivesTheSameLoadPath() = runTest {
        // The shipped Unit has no cross-Topic primary concept, and inventing one to satisfy a
        // test would corrupt authored content. A fixture run through the same loader and
        // repository proves instead that no layer below authoring reintroduces a home-Topic
        // restriction on the concept a Lesson teaches.
        val repository: LearningContentRepository = BundledLearningContentRepository(
            loader = LearningContentLoader(
                loadLearningCurriculum = { crossTopicPrimaryCurriculum },
                loadCurriculum = { fixtureCurriculum },
            ),
        )

        val units = repository.getActiveUnitsByTopic("android_ui")

        assertEquals(listOf("unit_cross_topic"), units.map { it.id })
        assertEquals(
            listOf("coroutine_scope"),
            repository.getLessonById("lesson_cross_topic")?.primarySubtopicIds,
        )
    }

    @Test
    fun authoredSourcesSurviveToTheRepository() = runTest {
        val repository = BundledLearningContentRepository()
        val unit = assertNotNull(repository.getUnitById("unit_thinking_in_compose"))

        unit.lessons.forEach { lesson ->
            assertTrue(lesson.sources.isNotEmpty(), lesson.id)
            lesson.sources.forEach { source ->
                assertTrue(source.title.isNotBlank(), lesson.id)
                assertTrue(source.url.isNotBlank(), lesson.id)
            }
        }

        // One representative Source is pinned whole, which is what proves title and URL
        // travelled together and intact rather than merely being present. Whether a URL is
        // well formed is `LearningCurriculumValidatorTest`'s concern, not this one's.
        assertContains(
            assertNotNull(repository.getLessonById("lesson_declarative_ui")).sources,
            SourceReference(
                title = "Thinking in Compose",
                url = "https://developer.android.com/develop/ui/compose/mental-model",
            ),
        )
    }

    @Test
    fun structuredLessonContentSurvivesToTheRepository() = runTest {
        val unit = assertNotNull(BundledLearningContentRepository().getUnitById("unit_thinking_in_compose"))
        val sections = unit.lessons.flatMap { it.sections }

        unit.lessons.forEach { lesson ->
            assertEquals(ContentStatus.ACTIVE, lesson.status, lesson.id)
            assertTrue(lesson.sections.isNotEmpty(), lesson.id)
            assertTrue(lesson.sections.all { it.blocks.isNotEmpty() }, lesson.id)
        }

        // The authored depth ladder reaches the repository rather than collapsing into one
        // undifferentiated body of text.
        assertEquals(
            setOf(LearningDepth.CORE, LearningDepth.PRACTICAL, LearningDepth.SENIOR),
            sections.map { it.depth }.toSet(),
        )

        // Structure survives the resource path as typed blocks, not as flattened prose. The
        // per-variant serialization detail belongs to `LearningCurriculumJsonCodecTest`.
        val blocks = sections.flatMap { it.blocks }
        assertTrue(blocks.filterIsInstance<LearningBlock.Paragraph>().isNotEmpty())
        assertTrue(blocks.filterIsInstance<LearningBlock.BulletList>().isNotEmpty())
        assertTrue(blocks.filterIsInstance<LearningBlock.Code>().isNotEmpty())
        assertTrue(blocks.filterIsInstance<LearningBlock.Comparison>().isNotEmpty())
        assertTrue(blocks.filterIsInstance<LearningBlock.Callout>().isNotEmpty())
    }

    private val crossTopicPrimaryCurriculum = LearningCurriculum(
        units = listOf(
            LearningUnit(
                id = "unit_cross_topic",
                topicId = "android_ui",
                title = "Side effects in Compose",
                summary = "Where suspending work belongs in a composable.",
                lessons = listOf(
                    LearningLesson(
                        id = "lesson_cross_topic",
                        title = "Launching work from a composable",
                        summary = "Why a composable needs a scope tied to its lifecycle.",
                        // The concept taught is owned by the coroutines Topic, not the Unit's.
                        primarySubtopicIds = listOf("coroutine_scope"),
                        supportingSubtopicIds = emptyList(),
                        sections = listOf(
                            LearningSection(
                                depth = LearningDepth.CORE,
                                blocks = listOf(
                                    LearningBlock.Paragraph(
                                        text = "A composable launches suspending work in a scope that ends with it.",
                                    ),
                                ),
                            ),
                        ),
                        relatedLessonIds = emptyList(),
                        sources = listOf(
                            SourceReference(
                                title = "Side-effects in Compose",
                                url = "https://developer.android.com/develop/ui/compose/side-effects",
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )

    private val fixtureCurriculum = Curriculum(
        topics = listOf(
            Topic(id = "android_ui", name = "Android UI"),
            Topic(id = "kotlin_coroutines", name = "Kotlin coroutines"),
        ),
        subtopics = listOf(
            Subtopic(id = "coroutine_scope", topicId = "kotlin_coroutines", name = "Coroutine scope"),
        ),
        questions = emptyList(),
    )
}
