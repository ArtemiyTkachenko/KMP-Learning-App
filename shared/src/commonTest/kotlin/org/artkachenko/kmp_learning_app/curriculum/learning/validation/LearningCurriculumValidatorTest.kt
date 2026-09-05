package org.artkachenko.kmp_learning_app.curriculum.learning.validation

import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningBlock
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningCalloutKind
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningCurriculum
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningDepth
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningLesson
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningSection
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class LearningCurriculumValidatorTest {
    private val validator = LearningCurriculumValidator()

    @Test
    fun representativeLearningCurriculumHasNoErrors() {
        assertTrue(validator.validate(representativeLearningCurriculum(), baseCurriculum).isEmpty())
    }

    // region Learning units

    @Test
    fun validatesUnitIdentityFieldsAndDuplicateIds() {
        val learningCurriculum = learningCurriculum(
            units = listOf(
                unit(id = "unit_compose"),
                unit(id = "unit_compose", lessons = listOf(lesson(id = "lesson_other"))),
                unit(id = "", title = "", summary = "", lessons = listOf(lesson(id = "lesson_third"))),
            ),
        )

        assertCodes(
            learningCurriculum,
            LearningCurriculumValidationErrorCode.DUPLICATE_UNIT_ID,
            LearningCurriculumValidationErrorCode.DUPLICATE_UNIT_ID,
            LearningCurriculumValidationErrorCode.BLANK_UNIT_ID,
            LearningCurriculumValidationErrorCode.BLANK_UNIT_TITLE,
            LearningCurriculumValidationErrorCode.BLANK_UNIT_SUMMARY,
        )
    }

    @Test
    fun rejectsPlaceholderUnitTitleAndSummary() {
        assertCodes(
            learningCurriculum(units = listOf(unit(title = "TBD", summary = "TODO: write this"))),
            LearningCurriculumValidationErrorCode.PLACEHOLDER_UNIT_TITLE,
            LearningCurriculumValidationErrorCode.PLACEHOLDER_UNIT_SUMMARY,
        )
    }

    @Test
    fun validatesHomeTopicIdentity() {
        assertCodes(
            learningCurriculum(units = listOf(unit(topicId = ""))),
            LearningCurriculumValidationErrorCode.BLANK_HOME_TOPIC_ID,
        )
        assertCodes(
            learningCurriculum(units = listOf(unit(topicId = "topic_that_does_not_exist"))),
            LearningCurriculumValidationErrorCode.UNKNOWN_HOME_TOPIC,
        )
    }

    @Test
    fun deprecatedHomeTopicIsAcceptedBecauseOnlyExistenceIsRequired() {
        val curriculumWithDeprecatedTopic = baseCurriculum.copy(
            topics = listOf(
                Topic(id = "android_ui", name = "Android UI", status = ContentStatus.DEPRECATED),
                Topic(id = "kotlin_coroutines", name = "Kotlin coroutines"),
            ),
        )

        assertTrue(
            validator.validate(learningCurriculum(), curriculumWithDeprecatedTopic).isEmpty(),
        )
    }

    @Test
    fun activeUnitWithoutLessonsIsRejected() {
        assertCodes(
            learningCurriculum(units = listOf(unit(lessons = emptyList()))),
            LearningCurriculumValidationErrorCode.NO_LESSONS,
        )
    }

    @Test
    fun deprecatedUnitWithoutLessonsIsAccepted() {
        assertTrue(
            validator.validate(
                learningCurriculum(
                    units = listOf(unit(lessons = emptyList(), status = ContentStatus.DEPRECATED)),
                ),
                baseCurriculum,
            ).isEmpty(),
        )
    }

    // endregion

    // region Lesson identity

    @Test
    fun validatesLessonIdentityFields() {
        assertCodes(
            learningCurriculum(
                units = listOf(unit(lessons = listOf(lesson(id = "", title = "", summary = "")))),
            ),
            LearningCurriculumValidationErrorCode.BLANK_LESSON_ID,
            LearningCurriculumValidationErrorCode.BLANK_LESSON_TITLE,
            LearningCurriculumValidationErrorCode.BLANK_LESSON_SUMMARY,
        )
    }

    @Test
    fun rejectsPlaceholderLessonTitleAndSummary() {
        assertCodes(
            learningCurriculum(
                units = listOf(unit(lessons = listOf(lesson(title = "FIXME", summary = "lorem ipsum")))),
            ),
            LearningCurriculumValidationErrorCode.PLACEHOLDER_LESSON_TITLE,
            LearningCurriculumValidationErrorCode.PLACEHOLDER_LESSON_SUMMARY,
        )
    }

    @Test
    fun lessonIdsMustBeUniqueAcrossUnitsNotOnlyWithinOne() {
        val learningCurriculum = learningCurriculum(
            units = listOf(
                unit(id = "unit_compose", lessons = listOf(lesson(id = "lesson_shared_id"))),
                unit(
                    id = "unit_flow",
                    topicId = "kotlin_coroutines",
                    lessons = listOf(lesson(id = "lesson_shared_id")),
                ),
            ),
        )

        val errors = validator.validate(learningCurriculum, baseCurriculum)

        assertEquals(
            listOf(
                LearningCurriculumValidationErrorCode.DUPLICATE_LESSON_ID,
                LearningCurriculumValidationErrorCode.DUPLICATE_LESSON_ID,
            ),
            errors.map { it.code },
        )
        assertTrue(errors.all { it.entityId == "lesson_shared_id" })
    }

    // endregion

    // region Concept relationships

    @Test
    fun activeLessonWithoutPrimaryConceptsIsRejected() {
        assertCodes(
            learningCurriculum(
                units = listOf(unit(lessons = listOf(lesson(primarySubtopicIds = emptyList())))),
            ),
            LearningCurriculumValidationErrorCode.NO_PRIMARY_SUBTOPICS,
        )
    }

    @Test
    fun deprecatedLessonWithoutPrimaryConceptsIsAccepted() {
        assertTrue(
            validator.validate(
                learningCurriculum(
                    units = listOf(
                        unit(
                            lessons = listOf(
                                lesson(
                                    primarySubtopicIds = emptyList(),
                                    status = ContentStatus.DEPRECATED,
                                ),
                            ),
                        ),
                    ),
                ),
                baseCurriculum,
            ).isEmpty(),
        )
    }

    @Test
    fun validatesPrimarySubtopicReferences() {
        assertCodes(
            learningCurriculum(
                units = listOf(
                    unit(
                        lessons = listOf(
                            lesson(
                                primarySubtopicIds = listOf(
                                    "compose_recomposition",
                                    "compose_recomposition",
                                    "",
                                    "subtopic_that_does_not_exist",
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            LearningCurriculumValidationErrorCode.DUPLICATE_PRIMARY_SUBTOPIC_ID,
            LearningCurriculumValidationErrorCode.DUPLICATE_PRIMARY_SUBTOPIC_ID,
            LearningCurriculumValidationErrorCode.BLANK_PRIMARY_SUBTOPIC_ID,
            LearningCurriculumValidationErrorCode.UNKNOWN_PRIMARY_SUBTOPIC,
        )
    }

    @Test
    fun validatesSupportingSubtopicReferences() {
        assertCodes(
            learningCurriculum(
                units = listOf(
                    unit(
                        lessons = listOf(
                            lesson(
                                supportingSubtopicIds = listOf(
                                    "flow_basics",
                                    "flow_basics",
                                    "",
                                    "subtopic_that_does_not_exist",
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            LearningCurriculumValidationErrorCode.DUPLICATE_SUPPORTING_SUBTOPIC_ID,
            LearningCurriculumValidationErrorCode.DUPLICATE_SUPPORTING_SUBTOPIC_ID,
            LearningCurriculumValidationErrorCode.BLANK_SUPPORTING_SUBTOPIC_ID,
            LearningCurriculumValidationErrorCode.UNKNOWN_SUPPORTING_SUBTOPIC,
        )
    }

    @Test
    fun rejectsSubtopicThatIsBothPrimaryAndSupporting() {
        assertCodes(
            learningCurriculum(
                units = listOf(
                    unit(
                        lessons = listOf(
                            lesson(
                                primarySubtopicIds = listOf("compose_recomposition"),
                                supportingSubtopicIds = listOf("compose_recomposition"),
                            ),
                        ),
                    ),
                ),
            ),
            LearningCurriculumValidationErrorCode.PRIMARY_SUPPORTING_SUBTOPIC_OVERLAP,
        )
    }

    // endregion

    // region Cross-topic relationships

    @Test
    fun primaryConceptFromAnotherTopicThanTheHomeTopicIsValid() {
        // The home Topic decides where a Unit is browsed; it does not own the concepts its
        // Lessons teach.
        val learningCurriculum = learningCurriculum(
            units = listOf(
                unit(
                    topicId = "android_ui",
                    lessons = listOf(lesson(primarySubtopicIds = listOf("flow_basics"))),
                ),
            ),
        )

        assertTrue(validator.validate(learningCurriculum, baseCurriculum).isEmpty())
    }

    @Test
    fun composeUnitMayBridgeToConceptsOwnedByAnotherTopic() {
        val learningCurriculum = learningCurriculum(
            units = listOf(
                unit(
                    topicId = "android_ui",
                    lessons = listOf(
                        lesson(
                            primarySubtopicIds = listOf("compose_state"),
                            supportingSubtopicIds = listOf("flow_basics", "coroutine_scope"),
                        ),
                    ),
                ),
            ),
        )

        assertTrue(validator.validate(learningCurriculum, baseCurriculum).isEmpty())
    }

    @Test
    fun crossTopicPrimaryAndSupportingConceptsAreValidTogether() {
        val learningCurriculum = learningCurriculum(
            units = listOf(
                unit(
                    topicId = "android_ui",
                    lessons = listOf(
                        lesson(
                            primarySubtopicIds = listOf("flow_basics"),
                            supportingSubtopicIds = listOf("coroutine_scope", "compose_state"),
                        ),
                    ),
                ),
            ),
        )

        assertTrue(validator.validate(learningCurriculum, baseCurriculum).isEmpty())
    }

    // endregion

    // region Related lessons

    @Test
    fun validatesRelatedLessonReferences() {
        assertCodes(
            learningCurriculum(
                units = listOf(
                    unit(
                        lessons = listOf(
                            lesson(
                                id = "lesson_declarative_ui",
                                relatedLessonIds = listOf(
                                    "lesson_that_does_not_exist",
                                    "lesson_declarative_ui",
                                    "",
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            LearningCurriculumValidationErrorCode.UNKNOWN_RELATED_LESSON,
            LearningCurriculumValidationErrorCode.SELF_RELATED_LESSON,
            LearningCurriculumValidationErrorCode.BLANK_RELATED_LESSON_ID,
        )
    }

    @Test
    fun rejectsDuplicateRelatedLessonIds() {
        val learningCurriculum = learningCurriculum(
            units = listOf(
                unit(
                    lessons = listOf(
                        lesson(id = "lesson_a", relatedLessonIds = listOf("lesson_b", "lesson_b")),
                        lesson(id = "lesson_b"),
                    ),
                ),
            ),
        )

        assertCodes(
            learningCurriculum,
            LearningCurriculumValidationErrorCode.DUPLICATE_RELATED_LESSON_ID,
            LearningCurriculumValidationErrorCode.DUPLICATE_RELATED_LESSON_ID,
        )
    }

    @Test
    fun relatedLessonsMayCrossUnitsAndNeedNotBeSymmetrical() {
        val learningCurriculum = learningCurriculum(
            units = listOf(
                unit(
                    id = "unit_compose",
                    lessons = listOf(lesson(id = "lesson_a", relatedLessonIds = listOf("lesson_b"))),
                ),
                unit(
                    id = "unit_flow",
                    topicId = "kotlin_coroutines",
                    lessons = listOf(lesson(id = "lesson_b", relatedLessonIds = emptyList())),
                ),
            ),
        )

        assertTrue(validator.validate(learningCurriculum, baseCurriculum).isEmpty())
    }

    // endregion

    // region Sections

    @Test
    fun activeLessonWithoutSectionsIsRejected() {
        assertCodes(
            learningCurriculum(
                units = listOf(unit(lessons = listOf(lesson(sections = emptyList())))),
            ),
            LearningCurriculumValidationErrorCode.NO_SECTIONS,
        )
    }

    @Test
    fun everyEmptySectionOfAnActiveLessonIsReported() {
        assertCodes(
            learningCurriculum(
                units = listOf(
                    unit(
                        lessons = listOf(
                            lesson(
                                sections = listOf(
                                    section(depth = LearningDepth.CORE, blocks = emptyList()),
                                    section(depth = LearningDepth.PRACTICAL, blocks = emptyList()),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            LearningCurriculumValidationErrorCode.EMPTY_SECTION_BLOCKS,
            LearningCurriculumValidationErrorCode.EMPTY_SECTION_BLOCKS,
        )
    }

    @Test
    fun sectionTitleIsOptionalButMustSaySomethingWhenPresent() {
        assertTrue(
            validator.validate(
                learningCurriculum(
                    units = listOf(unit(lessons = listOf(lesson(sections = listOf(section(title = null)))))),
                ),
                baseCurriculum,
            ).isEmpty(),
        )

        assertCodes(
            learningCurriculum(
                units = listOf(unit(lessons = listOf(lesson(sections = listOf(section(title = " ")))))),
            ),
            LearningCurriculumValidationErrorCode.BLANK_SECTION_TITLE,
        )

        assertCodes(
            learningCurriculum(
                units = listOf(unit(lessons = listOf(lesson(sections = listOf(section(title = "TODO")))))),
            ),
            LearningCurriculumValidationErrorCode.PLACEHOLDER_SECTION_TITLE,
        )
    }

    @Test
    fun severalSectionsMayShareTheSameDepth() {
        val learningCurriculum = learningCurriculum(
            units = listOf(
                unit(
                    lessons = listOf(
                        lesson(
                            sections = listOf(
                                section(depth = LearningDepth.PRACTICAL, title = "Ownership"),
                                section(depth = LearningDepth.PRACTICAL, title = "Failure modes"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertTrue(validator.validate(learningCurriculum, baseCurriculum).isEmpty())
    }

    @Test
    fun aLessonNeedNotCoverEveryDepth() {
        val learningCurriculum = learningCurriculum(
            units = listOf(
                unit(lessons = listOf(lesson(sections = listOf(section(depth = LearningDepth.CORE))))),
            ),
        )

        assertTrue(validator.validate(learningCurriculum, baseCurriculum).isEmpty())
    }

    // endregion

    // region Content blocks

    @Test
    fun validatesParagraphs() {
        assertCodes(
            learningCurriculumWithBlocks(LearningBlock.Paragraph(text = " ")),
            LearningCurriculumValidationErrorCode.BLANK_PARAGRAPH_TEXT,
        )
        assertCodes(
            learningCurriculumWithBlocks(LearningBlock.Paragraph(text = "TODO: explain recomposition.")),
            LearningCurriculumValidationErrorCode.PLACEHOLDER_PARAGRAPH_TEXT,
        )
    }

    @Test
    fun validatesBulletLists() {
        assertCodes(
            learningCurriculumWithBlocks(LearningBlock.BulletList(items = emptyList())),
            LearningCurriculumValidationErrorCode.EMPTY_BULLET_LIST,
        )
        assertCodes(
            learningCurriculumWithBlocks(
                LearningBlock.BulletList(items = listOf("State flows down.", " ", "TBD")),
            ),
            LearningCurriculumValidationErrorCode.BLANK_BULLET_ITEM,
            LearningCurriculumValidationErrorCode.PLACEHOLDER_BULLET_ITEM,
        )
    }

    @Test
    fun rejectsRepeatedBulletItems() {
        assertCodes(
            learningCurriculumWithBlocks(
                LearningBlock.BulletList(items = listOf("State flows down.", "  state flows down.  ")),
            ),
            LearningCurriculumValidationErrorCode.DUPLICATE_BULLET_ITEM,
            LearningCurriculumValidationErrorCode.DUPLICATE_BULLET_ITEM,
        )
    }

    @Test
    fun aSingleItemBulletListIsAccepted() {
        assertTrue(
            validator.validate(
                learningCurriculumWithBlocks(LearningBlock.BulletList(items = listOf("State flows down."))),
                baseCurriculum,
            ).isEmpty(),
        )
    }

    @Test
    fun validatesCodeBlocks() {
        assertCodes(
            learningCurriculumWithBlocks(LearningBlock.Code(code = " ")),
            LearningCurriculumValidationErrorCode.BLANK_CODE,
        )
        assertCodes(
            learningCurriculumWithBlocks(LearningBlock.Code(code = "// TODO: write the example")),
            LearningCurriculumValidationErrorCode.PLACEHOLDER_CODE,
        )
        assertCodes(
            learningCurriculumWithBlocks(LearningBlock.Code(code = "val x = 1", language = " ")),
            LearningCurriculumValidationErrorCode.BLANK_CODE_LANGUAGE,
        )
    }

    @Test
    fun codeMayUseKotlinTodoAndAnyLanguageIdentifier() {
        // `TODO()` is real Kotlin worth teaching, and the language identifier is deliberately
        // open rather than a closed set.
        assertTrue(
            validator.validate(
                learningCurriculumWithBlocks(
                    LearningBlock.Code(
                        code = "fun render(): Nothing = TODO()",
                        language = "kotlin",
                    ),
                    LearningBlock.Code(code = "<Text />", language = "xml"),
                ),
                baseCurriculum,
            ).isEmpty(),
        )
    }

    @Test
    fun validatesComparisonStructure() {
        assertCodes(
            learningCurriculumWithBlocks(
                LearningBlock.Comparison(headers = emptyList(), rows = emptyList()),
            ),
            LearningCurriculumValidationErrorCode.EMPTY_COMPARISON_HEADERS,
            LearningCurriculumValidationErrorCode.EMPTY_COMPARISON_ROWS,
        )
        assertCodes(
            learningCurriculumWithBlocks(
                LearningBlock.Comparison(
                    headers = listOf("API", " ", "API"),
                    rows = listOf(listOf("remember", "x", "y")),
                ),
            ),
            LearningCurriculumValidationErrorCode.DUPLICATE_COMPARISON_HEADER,
            LearningCurriculumValidationErrorCode.BLANK_COMPARISON_HEADER,
            LearningCurriculumValidationErrorCode.DUPLICATE_COMPARISON_HEADER,
        )
    }

    @Test
    fun reportsComparisonRowsWhoseColumnCountDoesNotMatchTheHeaders() {
        val errors = validator.validate(
            learningCurriculumWithBlocks(
                LearningBlock.Comparison(
                    headers = listOf("API", "Survives"),
                    rows = listOf(
                        listOf("remember", "recomposition"),
                        listOf("rememberSaveable", "activity recreation", "process death"),
                    ),
                ),
            ),
            baseCurriculum,
        )

        assertEquals(
            listOf(LearningCurriculumValidationErrorCode.COMPARISON_COLUMN_COUNT_MISMATCH),
            errors.map { it.code },
        )
        assertTrue(errors.single().message.contains("row 1"))
    }

    @Test
    fun validatesComparisonCells() {
        assertCodes(
            learningCurriculumWithBlocks(
                LearningBlock.Comparison(
                    headers = listOf("API", "Survives"),
                    rows = listOf(listOf(" ", "TBD")),
                ),
            ),
            LearningCurriculumValidationErrorCode.BLANK_COMPARISON_CELL,
            LearningCurriculumValidationErrorCode.PLACEHOLDER_COMPARISON_CELL,
        )
    }

    @Test
    fun validatesCallouts() {
        assertCodes(
            learningCurriculumWithBlocks(
                LearningBlock.Callout(kind = LearningCalloutKind.NOTE, text = " "),
            ),
            LearningCurriculumValidationErrorCode.BLANK_CALLOUT_TEXT,
        )
        assertCodes(
            learningCurriculumWithBlocks(
                LearningBlock.Callout(kind = LearningCalloutKind.KEY_TAKEAWAY, text = "TBD"),
            ),
            LearningCurriculumValidationErrorCode.PLACEHOLDER_CALLOUT_TEXT,
        )
    }

    // endregion

    // region Sources

    @Test
    fun activeLessonWithoutSourcesIsRejected() {
        assertCodes(
            learningCurriculum(units = listOf(unit(lessons = listOf(lesson(sources = emptyList()))))),
            LearningCurriculumValidationErrorCode.NO_SOURCES,
        )
    }

    @Test
    fun deprecatedLessonWithoutSourcesIsAccepted() {
        assertTrue(
            validator.validate(
                learningCurriculum(
                    units = listOf(
                        unit(
                            lessons = listOf(
                                lesson(sources = emptyList(), status = ContentStatus.DEPRECATED),
                            ),
                        ),
                    ),
                ),
                baseCurriculum,
            ).isEmpty(),
        )
    }

    @Test
    fun validatesSourceStructure() {
        assertCodes(
            learningCurriculum(
                units = listOf(
                    unit(
                        lessons = listOf(
                            lesson(
                                sources = listOf(
                                    SourceReference(title = "", url = ""),
                                    SourceReference(title = "TBD", url = "developer.android.com/state"),
                                    SourceReference(title = "Local draft", url = "http://localhost:8080/state"),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            LearningCurriculumValidationErrorCode.BLANK_SOURCE_TITLE,
            LearningCurriculumValidationErrorCode.BLANK_SOURCE_URL,
            LearningCurriculumValidationErrorCode.PLACEHOLDER_SOURCE_TITLE,
            LearningCurriculumValidationErrorCode.INVALID_SOURCE_URL,
            LearningCurriculumValidationErrorCode.PLACEHOLDER_SOURCE_URL,
        )
    }

    // endregion

    // region Lifecycle

    @Test
    fun deprecatedLessonsStillHaveIdentityReferencesAndContentValidated() {
        val learningCurriculum = learningCurriculum(
            units = listOf(
                unit(
                    lessons = listOf(
                        lesson(
                            id = "lesson_history",
                            title = "",
                            primarySubtopicIds = listOf("subtopic_that_does_not_exist"),
                            supportingSubtopicIds = listOf("subtopic_that_does_not_exist"),
                            relatedLessonIds = listOf("lesson_that_does_not_exist"),
                            sections = listOf(section(blocks = listOf(LearningBlock.Paragraph(text = " ")))),
                            sources = listOf(SourceReference(title = "Docs", url = "ftp://example.com/docs")),
                            status = ContentStatus.DEPRECATED,
                        ),
                    ),
                ),
            ),
        )

        assertCodes(
            learningCurriculum,
            LearningCurriculumValidationErrorCode.BLANK_LESSON_TITLE,
            LearningCurriculumValidationErrorCode.UNKNOWN_PRIMARY_SUBTOPIC,
            LearningCurriculumValidationErrorCode.UNKNOWN_SUPPORTING_SUBTOPIC,
            LearningCurriculumValidationErrorCode.PRIMARY_SUPPORTING_SUBTOPIC_OVERLAP,
            LearningCurriculumValidationErrorCode.UNKNOWN_RELATED_LESSON,
            LearningCurriculumValidationErrorCode.BLANK_PARAGRAPH_TEXT,
            LearningCurriculumValidationErrorCode.INVALID_SOURCE_URL,
        )
    }

    @Test
    fun aDeprecatedLessonIsStillResolvableAsRelatedContent() {
        val learningCurriculum = learningCurriculum(
            units = listOf(
                unit(
                    lessons = listOf(
                        lesson(id = "lesson_a", relatedLessonIds = listOf("lesson_retired")),
                        lesson(id = "lesson_retired", status = ContentStatus.DEPRECATED),
                    ),
                ),
            ),
        )

        assertTrue(validator.validate(learningCurriculum, baseCurriculum).isEmpty())
    }

    // endregion

    // region Aggregation and determinism

    @Test
    fun independentDefectsAreAllReportedFromOnePass() {
        val errors = validator.validate(
            learningCurriculum(
                units = listOf(
                    unit(
                        id = "unit_compose",
                        lessons = listOf(
                            lesson(
                                id = "lesson_broken",
                                primarySubtopicIds = listOf("subtopic_that_does_not_exist", "compose_state"),
                                supportingSubtopicIds = listOf("compose_state"),
                                relatedLessonIds = listOf("lesson_that_does_not_exist"),
                                sections = listOf(section(blocks = emptyList())),
                                sources = listOf(SourceReference(title = "Docs", url = "notaurl")),
                            ),
                        ),
                    ),
                ),
            ),
            baseCurriculum,
        )

        assertTrue(
            errors.map { it.code }.containsAll(
                listOf(
                    LearningCurriculumValidationErrorCode.UNKNOWN_PRIMARY_SUBTOPIC,
                    LearningCurriculumValidationErrorCode.PRIMARY_SUPPORTING_SUBTOPIC_OVERLAP,
                    LearningCurriculumValidationErrorCode.UNKNOWN_RELATED_LESSON,
                    LearningCurriculumValidationErrorCode.EMPTY_SECTION_BLOCKS,
                    LearningCurriculumValidationErrorCode.INVALID_SOURCE_URL,
                ),
            ),
            "Expected every independent defect to be reported, got ${errors.map { it.code }}",
        )
        assertTrue(errors.all { it.entityId == "lesson_broken" })
    }

    @Test
    fun defectsInDifferentUnitsAreReportedTogether() {
        val errors = validator.validate(
            learningCurriculum(
                units = listOf(
                    unit(id = "unit_compose", title = ""),
                    unit(
                        id = "unit_flow",
                        topicId = "topic_that_does_not_exist",
                        lessons = listOf(lesson(id = "lesson_flow", sources = emptyList())),
                    ),
                ),
            ),
            baseCurriculum,
        )

        assertEquals(
            listOf(
                LearningCurriculumValidationErrorCode.BLANK_UNIT_TITLE to "unit_compose",
                LearningCurriculumValidationErrorCode.UNKNOWN_HOME_TOPIC to "unit_flow",
                LearningCurriculumValidationErrorCode.NO_SOURCES to "lesson_flow",
            ),
            errors.map { it.code to it.entityId },
        )
    }

    @Test
    fun validationResultIsDeterministic() {
        val malformed = learningCurriculum(
            units = listOf(
                unit(
                    title = "",
                    lessons = listOf(
                        lesson(
                            primarySubtopicIds = listOf("subtopic_that_does_not_exist"),
                            sections = listOf(section(blocks = emptyList())),
                            sources = emptyList(),
                        ),
                    ),
                ),
            ),
        )

        val firstResult = validator.validate(malformed, baseCurriculum)

        repeat(5) {
            assertEquals(firstResult, validator.validate(malformed, baseCurriculum))
        }
    }

    // endregion

    private fun assertCodes(
        learningCurriculum: LearningCurriculum,
        vararg expectedCodes: LearningCurriculumValidationErrorCode,
    ) {
        assertEquals(
            expectedCodes.toList(),
            validator.validate(learningCurriculum, baseCurriculum).map { it.code },
        )
    }

    private fun learningCurriculumWithBlocks(vararg blocks: LearningBlock) =
        learningCurriculum(
            units = listOf(unit(lessons = listOf(lesson(sections = listOf(section(blocks = blocks.toList())))))),
        )

    private fun learningCurriculum(units: List<LearningUnit> = listOf(unit())) =
        LearningCurriculum(units = units)

    private fun unit(
        id: String = "unit_thinking_in_compose",
        topicId: String = "android_ui",
        title: String = "Thinking in Compose",
        summary: String = "How declarative UI changes the way a screen is written.",
        lessons: List<LearningLesson> = listOf(lesson()),
        status: ContentStatus = ContentStatus.ACTIVE,
    ) = LearningUnit(
        id = id,
        topicId = topicId,
        title = title,
        summary = summary,
        lessons = lessons,
        status = status,
    )

    private fun lesson(
        id: String = "lesson_declarative_ui",
        title: String = "Declarative UI",
        summary: String = "Why Compose describes the UI for a state instead of mutating a tree.",
        primarySubtopicIds: List<String> = listOf("compose_recomposition"),
        supportingSubtopicIds: List<String> = emptyList(),
        sections: List<LearningSection> = listOf(section()),
        relatedLessonIds: List<String> = emptyList(),
        sources: List<SourceReference> = listOf(source),
        status: ContentStatus = ContentStatus.ACTIVE,
    ) = LearningLesson(
        id = id,
        title = title,
        summary = summary,
        primarySubtopicIds = primarySubtopicIds,
        supportingSubtopicIds = supportingSubtopicIds,
        sections = sections,
        relatedLessonIds = relatedLessonIds,
        sources = sources,
        status = status,
    )

    private fun section(
        depth: LearningDepth = LearningDepth.CORE,
        blocks: List<LearningBlock> = listOf(paragraph),
        title: String? = null,
    ) = LearningSection(depth = depth, blocks = blocks, title = title)

    /**
     * A representative document: two Units, several Lessons, every depth, every block type,
     * cross-Unit related links, and cross-Topic primary and supporting concepts.
     */
    private fun representativeLearningCurriculum() = LearningCurriculum(
        units = listOf(
            LearningUnit(
                id = "unit_thinking_in_compose",
                topicId = "android_ui",
                title = "Thinking in Compose",
                summary = "How declarative UI changes the way a screen is written.",
                lessons = listOf(
                    LearningLesson(
                        id = "lesson_declarative_ui",
                        title = "Declarative UI",
                        summary = "Why Compose describes the UI for a state.",
                        primarySubtopicIds = listOf("compose_recomposition"),
                        supportingSubtopicIds = listOf("compose_state"),
                        sections = listOf(
                            LearningSection(depth = LearningDepth.CORE, blocks = listOf(paragraph)),
                            LearningSection(
                                depth = LearningDepth.PRACTICAL,
                                blocks = listOf(bulletList, code),
                                title = "In a real screen",
                            ),
                            LearningSection(
                                depth = LearningDepth.SENIOR,
                                blocks = listOf(comparison, callout),
                            ),
                        ),
                        relatedLessonIds = listOf("lesson_collecting_flow"),
                        sources = listOf(source),
                    ),
                    LearningLesson(
                        id = "lesson_state_ownership",
                        title = "State ownership",
                        summary = "Who should hold the state a composable reads.",
                        // Primary and supporting concepts both cross into the coroutines Topic.
                        primarySubtopicIds = listOf("compose_state", "flow_basics"),
                        supportingSubtopicIds = listOf("coroutine_scope"),
                        sections = listOf(
                            LearningSection(depth = LearningDepth.CORE, blocks = listOf(paragraph)),
                        ),
                        relatedLessonIds = listOf("lesson_declarative_ui"),
                        sources = listOf(source),
                    ),
                ),
            ),
            LearningUnit(
                id = "unit_flow_on_screen",
                topicId = "kotlin_coroutines",
                title = "Flow on screen",
                summary = "Collecting a stream safely from the UI layer.",
                lessons = listOf(
                    LearningLesson(
                        id = "lesson_collecting_flow",
                        title = "Collecting a flow",
                        summary = "How a screen observes a stream without leaking work.",
                        primarySubtopicIds = listOf("flow_basics"),
                        supportingSubtopicIds = listOf("compose_recomposition"),
                        sections = listOf(
                            LearningSection(
                                depth = LearningDepth.CORE,
                                blocks = listOf(paragraph, callout),
                            ),
                        ),
                        relatedLessonIds = listOf("lesson_state_ownership"),
                        sources = listOf(source),
                        status = ContentStatus.DEPRECATED,
                    ),
                ),
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

    private val source = SourceReference(
        title = "State and Jetpack Compose",
        url = "https://developer.android.com/develop/ui/compose/state",
    )

    /**
     * The assessment curriculum is the source of truth for concept identity. Two Topics with
     * Subtopics of their own are the minimum needed to prove that cross-Topic learning
     * relationships are accepted. Questions are irrelevant here: this validator reads only
     * Topic and Subtopic identity.
     */
    private val baseCurriculum = Curriculum(
        topics = listOf(
            Topic(id = "android_ui", name = "Android UI"),
            Topic(id = "kotlin_coroutines", name = "Kotlin coroutines"),
        ),
        subtopics = listOf(
            Subtopic(id = "compose_recomposition", topicId = "android_ui", name = "Recomposition"),
            Subtopic(id = "compose_state", topicId = "android_ui", name = "Compose state"),
            Subtopic(id = "flow_basics", topicId = "kotlin_coroutines", name = "Flow basics"),
            Subtopic(id = "coroutine_scope", topicId = "kotlin_coroutines", name = "Coroutine scope"),
        ),
        questions = emptyList(),
    )
}
