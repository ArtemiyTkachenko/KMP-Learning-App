package org.artkachenko.kmp_learning_app.curriculum.learning.serialization

import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningBlock
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningCalloutKind
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningCurriculum
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningDepth
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningLesson
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningSection
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class LearningCurriculumJsonCodecTest {
    @Test
    fun learningCurriculumRoundTripPreservesEquivalentValue() {
        val curriculum = sampleCurriculum()

        assertEquals(curriculum, roundTrip(curriculum))
    }

    @Test
    fun unitOrderIsPreservedByRoundTrip() {
        val decoded = roundTrip(sampleCurriculum())

        assertEquals(
            listOf("unit_thinking_in_compose", "unit_state_and_ownership"),
            decoded.units.map { it.id },
        )
    }

    @Test
    fun lessonOrderWithinAUnitIsPreservedByRoundTrip() {
        val decoded = roundTrip(sampleCurriculum())

        assertEquals(
            listOf("lesson_declarative_ui", "lesson_state_down_events_up"),
            decoded.units.first().lessons.map { it.id },
        )
    }

    @Test
    fun homeTopicSurvivesRoundTripSeparatelyFromLessonConcepts() {
        val decoded = roundTrip(sampleCurriculum())

        assertEquals("android_ui", decoded.units.first().topicId)
    }

    @Test
    fun primaryAndSupportingSubtopicIdsRemainDistinct() {
        val lesson = roundTrip(sampleCurriculum()).units.first().lessons.last()

        assertEquals(listOf("compose_udf"), lesson.primarySubtopicIds)
        assertEquals(
            listOf("compose_state_hoisting", "unidirectional_data_flow"),
            lesson.supportingSubtopicIds,
        )
    }

    @Test
    fun supportingSubtopicIdsMayNameAnotherTopicsConcepts() {
        // `unidirectional_data_flow` belongs to the architecture Topic while the Unit's
        // home Topic is `android_ui`; cross-Topic support is intended, not an error.
        val lesson = roundTrip(sampleCurriculum()).units.first().lessons.last()

        assertTrue(lesson.supportingSubtopicIds.contains("unidirectional_data_flow"))
    }

    @Test
    fun unresolvableConceptIdsStillDecode() {
        // Reference validity is content validation's responsibility, not the model's.
        val curriculum = sampleCurriculum(
            primarySubtopicIds = listOf("subtopic_that_does_not_exist"),
        )

        assertEquals(
            listOf("subtopic_that_does_not_exist"),
            roundTrip(curriculum).units.first().lessons.last().primarySubtopicIds,
        )
    }

    @Test
    fun relatedLessonIdsSurviveRoundTrip() {
        val lesson = roundTrip(sampleCurriculum()).units.first().lessons.last()

        assertEquals(listOf("lesson_state_hoisting"), lesson.relatedLessonIds)
    }

    @Test
    fun sourcesSurviveRoundTrip() {
        val lesson = roundTrip(sampleCurriculum()).units.first().lessons.first()

        assertEquals(
            listOf(
                SourceReference(
                    title = "Thinking in Compose",
                    url = "https://developer.android.com/develop/ui/compose/mental-model",
                ),
            ),
            lesson.sources,
        )
    }

    @Test
    fun allThreeDepthsSurviveRoundTrip() {
        val lesson = roundTrip(sampleCurriculum()).units.first().lessons.first()

        assertEquals(
            listOf(LearningDepth.CORE, LearningDepth.PRACTICAL, LearningDepth.SENIOR),
            lesson.sections.map { it.depth },
        )
    }

    @Test
    fun aLessonWithoutSeniorDepthRoundTripsUnchanged() {
        val lesson = roundTrip(sampleCurriculum()).units.first().lessons.last()

        assertEquals(listOf(LearningDepth.CORE, LearningDepth.PRACTICAL), lesson.sections.map { it.depth })
    }

    @Test
    fun optionalSectionTitleRoundTripsWhenAuthoredAndWhenAbsent() {
        val sections = roundTrip(sampleCurriculum()).units.first().lessons.first().sections

        assertNull(sections.first().title)
        assertEquals("Where the imperative version breaks", sections[1].title)
    }

    @Test
    fun optionalCodeLanguageRoundTripsWhenAuthoredAndWhenAbsent() {
        val decoded = roundTrip(sampleCurriculum())
        val kotlinCode = decoded.units.first().lessons.first().sections[1].blocks
            .filterIsInstance<LearningBlock.Code>()
            .single()
        val untaggedCode = decoded.units.last().lessons.single().sections.last().blocks
            .filterIsInstance<LearningBlock.Code>()
            .single()

        assertEquals("kotlin", kotlinCode.language)
        assertNull(untaggedCode.language)
    }

    @Test
    fun paragraphBlocksRoundTrip() {
        assertEquals(paragraph, roundTripBlock(paragraph))
    }

    @Test
    fun bulletListBlocksRoundTrip() {
        assertEquals(bulletList, roundTripBlock(bulletList))
    }

    @Test
    fun codeBlocksRoundTrip() {
        assertEquals(code, roundTripBlock(code))
    }

    @Test
    fun comparisonBlocksRoundTrip() {
        assertEquals(comparison, roundTripBlock(comparison))
    }

    @Test
    fun calloutBlocksRoundTrip() {
        assertEquals(callout, roundTripBlock(callout))
    }

    @Test
    fun everyCalloutKindRoundTrips() {
        LearningCalloutKind.entries.forEach { kind ->
            val block = LearningBlock.Callout(kind = kind, text = "Emphasised content.")

            assertEquals(block, roundTripBlock(block))
        }
    }

    @Test
    fun blockTypesUseStableSerializedDiscriminators() {
        val encoded = LearningCurriculumJsonCodec.encode(curriculumWithEveryBlockType())

        listOf("paragraph", "bullet_list", "code", "comparison", "callout").forEach { type ->
            assertTrue(encoded.contains("\"type\":\"$type\""), "missing discriminator $type")
        }
    }

    @Test
    fun lifecycleStatusSurvivesRoundTrip() {
        val decoded = roundTrip(sampleCurriculum())

        assertEquals(ContentStatus.ACTIVE, decoded.units.first().status)
        assertEquals(ContentStatus.ACTIVE, decoded.units.first().lessons.first().status)
        assertEquals(ContentStatus.DEPRECATED, decoded.units.last().status)
        assertEquals(ContentStatus.DEPRECATED, decoded.units.last().lessons.single().status)
    }

    @Test
    fun authoredJsonShapeDecodesIntoTheDomainModel() {
        val decoded = LearningCurriculumJsonCodec.decode(authoredLessonJson)
        val lesson = decoded.units.single().lessons.single()

        assertEquals("lesson_remember", lesson.id)
        assertEquals(listOf("compose_state"), lesson.primarySubtopicIds)
        assertEquals(
            listOf(
                LearningBlock.Paragraph("`remember` keeps a value across recompositions."),
                LearningBlock.Code(code = "val count = remember { mutableStateOf(0) }", language = "kotlin"),
                LearningBlock.Comparison(
                    headers = listOf("API", "Survives"),
                    rows = listOf(listOf("remember", "recomposition")),
                ),
                LearningBlock.BulletList(listOf("Composition memory is not persistence.")),
                LearningBlock.Callout(
                    kind = LearningCalloutKind.INTERVIEW_FOCUS,
                    text = "Explain why remember is not enough for process death.",
                ),
            ),
            lesson.sections.single().blocks,
        )
    }

    private fun roundTrip(curriculum: LearningCurriculum): LearningCurriculum =
        LearningCurriculumJsonCodec.decode(LearningCurriculumJsonCodec.encode(curriculum))

    private fun roundTripBlock(block: LearningBlock): LearningBlock =
        roundTrip(curriculumWithBlocks(listOf(block))).units.single().lessons.single().sections.single()
            .blocks.single()

    private fun curriculumWithEveryBlockType(): LearningCurriculum =
        curriculumWithBlocks(listOf(paragraph, bulletList, code, comparison, callout))

    private fun curriculumWithBlocks(blocks: List<LearningBlock>) = LearningCurriculum(
        units = listOf(
            LearningUnit(
                id = "unit_block_fixture",
                topicId = "android_ui",
                title = "Block fixture",
                summary = "A single lesson used to exercise structured content blocks.",
                lessons = listOf(
                    LearningLesson(
                        id = "lesson_block_fixture",
                        title = "Block fixture",
                        summary = "Carries one section of authored blocks.",
                        primarySubtopicIds = listOf("compose_fundamentals"),
                        supportingSubtopicIds = emptyList(),
                        sections = listOf(LearningSection(depth = LearningDepth.CORE, blocks = blocks)),
                        relatedLessonIds = emptyList(),
                        sources = listOf(
                            SourceReference(
                                title = "Compose documentation",
                                url = "https://developer.android.com/develop/ui/compose/documentation",
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )

    private fun sampleCurriculum(
        primarySubtopicIds: List<String> = listOf("compose_udf"),
    ) = LearningCurriculum(
        units = listOf(
            LearningUnit(
                id = "unit_thinking_in_compose",
                topicId = "android_ui",
                title = "Thinking in Compose",
                summary = "Establish the declarative mental model before any API is introduced.",
                lessons = listOf(
                    LearningLesson(
                        id = "lesson_declarative_ui",
                        title = "Declarative UI and why Compose exists",
                        summary = "What changes when UI is described rather than mutated.",
                        primarySubtopicIds = listOf("compose_fundamentals"),
                        supportingSubtopicIds = listOf("views_fundamentals"),
                        sections = listOf(
                            LearningSection(
                                depth = LearningDepth.CORE,
                                blocks = listOf(
                                    paragraph,
                                    LearningBlock.BulletList(
                                        items = listOf(
                                            "A View tree is inflated once and then mutated.",
                                            "A composable describes the UI for the current state.",
                                        ),
                                    ),
                                ),
                            ),
                            LearningSection(
                                depth = LearningDepth.PRACTICAL,
                                title = "Where the imperative version breaks",
                                blocks = listOf(code, comparison),
                            ),
                            LearningSection(
                                depth = LearningDepth.SENIOR,
                                blocks = listOf(callout),
                            ),
                        ),
                        relatedLessonIds = emptyList(),
                        sources = listOf(
                            SourceReference(
                                title = "Thinking in Compose",
                                url = "https://developer.android.com/develop/ui/compose/mental-model",
                            ),
                        ),
                    ),
                    LearningLesson(
                        id = "lesson_state_down_events_up",
                        title = "State down, events up",
                        summary = "Why the direction of data flow matters in Compose.",
                        primarySubtopicIds = primarySubtopicIds,
                        // `unidirectional_data_flow` is owned by the architecture Topic.
                        supportingSubtopicIds = listOf("compose_state_hoisting", "unidirectional_data_flow"),
                        sections = listOf(
                            LearningSection(depth = LearningDepth.CORE, blocks = listOf(paragraph)),
                            LearningSection(depth = LearningDepth.PRACTICAL, blocks = listOf(bulletList)),
                        ),
                        relatedLessonIds = listOf("lesson_state_hoisting"),
                        sources = listOf(
                            SourceReference(
                                title = "State and Jetpack Compose",
                                url = "https://developer.android.com/develop/ui/compose/state",
                            ),
                        ),
                    ),
                ),
            ),
            LearningUnit(
                id = "unit_state_and_ownership",
                topicId = "android_ui",
                title = "State and state ownership",
                summary = "What Compose state is, how long it lives, and who should own it.",
                lessons = listOf(
                    LearningLesson(
                        id = "lesson_observable_state",
                        title = "Observable state",
                        summary = "How Compose observes the values it reads.",
                        primarySubtopicIds = listOf("compose_state"),
                        supportingSubtopicIds = emptyList(),
                        sections = listOf(
                            LearningSection(
                                depth = LearningDepth.CORE,
                                blocks = listOf(
                                    paragraph,
                                    LearningBlock.Code(code = "var count by mutableStateOf(0)"),
                                ),
                            ),
                        ),
                        relatedLessonIds = listOf("lesson_declarative_ui"),
                        sources = listOf(
                            SourceReference(
                                title = "State and Jetpack Compose",
                                url = "https://developer.android.com/develop/ui/compose/state",
                            ),
                        ),
                        status = ContentStatus.DEPRECATED,
                    ),
                ),
                status = ContentStatus.DEPRECATED,
            ),
        ),
    )

    private val paragraph = LearningBlock.Paragraph(
        text = "Compose describes the UI for a given state instead of mutating a View tree.",
    )

    private val bulletList = LearningBlock.BulletList(
        items = listOf("State flows down.", "Events flow up."),
    )

    private val code = LearningBlock.Code(
        code = "@Composable\nfun Counter(count: Int, onIncrement: () -> Unit) { }",
        language = "kotlin",
    )

    private val comparison = LearningBlock.Comparison(
        headers = listOf("API", "Survives"),
        rows = listOf(
            listOf("remember", "recomposition"),
            listOf("rememberSaveable", "activity recreation"),
        ),
    )

    private val callout = LearningBlock.Callout(
        kind = LearningCalloutKind.COMMON_MISTAKE,
        text = "Treating a composable as a one-time lifecycle callback.",
    )

    private val authoredLessonJson =
        """
        {
          "units": [
            {
              "id": "unit_state_and_ownership",
              "topicId": "android_ui",
              "title": "State and state ownership",
              "summary": "What Compose state is and who owns it.",
              "status": "ACTIVE",
              "lessons": [
                {
                  "id": "lesson_remember",
                  "title": "remember: composition memory",
                  "summary": "How a value survives recomposition.",
                  "primarySubtopicIds": ["compose_state"],
                  "supportingSubtopicIds": ["compose_recomposition"],
                  "relatedLessonIds": ["lesson_remember_saveable"],
                  "status": "ACTIVE",
                  "sources": [
                    {
                      "title": "State and Jetpack Compose",
                      "url": "https://developer.android.com/develop/ui/compose/state"
                    }
                  ],
                  "sections": [
                    {
                      "depth": "CORE",
                      "title": null,
                      "blocks": [
                        {
                          "type": "paragraph",
                          "text": "`remember` keeps a value across recompositions."
                        },
                        {
                          "type": "code",
                          "language": "kotlin",
                          "code": "val count = remember { mutableStateOf(0) }"
                        },
                        {
                          "type": "comparison",
                          "headers": ["API", "Survives"],
                          "rows": [["remember", "recomposition"]]
                        },
                        {
                          "type": "bullet_list",
                          "items": ["Composition memory is not persistence."]
                        },
                        {
                          "type": "callout",
                          "kind": "INTERVIEW_FOCUS",
                          "text": "Explain why remember is not enough for process death."
                        }
                      ]
                    }
                  ]
                }
              ]
            }
          ]
        }
        """.trimIndent()
}
