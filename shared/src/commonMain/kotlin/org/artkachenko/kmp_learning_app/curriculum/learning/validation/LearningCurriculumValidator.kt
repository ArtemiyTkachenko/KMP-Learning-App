package org.artkachenko.kmp_learning_app.curriculum.learning.validation

import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningBlock
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningCurriculum
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningLesson
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningSection
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import org.artkachenko.kmp_learning_app.curriculum.validation.containsPlaceholder
import org.artkachenko.kmp_learning_app.curriculum.validation.duplicateNonBlankValues
import org.artkachenko.kmp_learning_app.curriculum.validation.isPlaceholderUrl
import org.artkachenko.kmp_learning_app.curriculum.validation.isValidHttpUrl
import org.artkachenko.kmp_learning_app.curriculum.validation.normalizedForComparison

/**
 * Authoring validation for the learning document and its references into the assessment
 * [Curriculum], which is the source of truth for Topic and Subtopic identity.
 *
 * Like `CurriculumValidator`, one pass reports every independent defect it can find rather
 * than failing on the first one, and validation never mutates or repairs content: trimming
 * text, de-duplicating ids, or padding a table would hide the defect instead of reporting
 * it. Results are deterministic and follow authored order.
 *
 * Two rules matter more than the rest because they encode product decisions:
 *
 * - A Unit's home Topic decides where the Unit is browsed, and deliberately does not
 *   constrain the Topics its Lessons may reference. Cross-Topic primary and supporting
 *   concepts are valid by design, never a mismatch.
 * - Minimum-content requirements — primary concepts, Sections, blocks, Sources, and a
 *   Unit's Lessons — apply to [ContentStatus.ACTIVE] content only. Deprecated content is
 *   historical rather than currently teachable, but its identity, references, and whatever
 *   content it does carry are still validated in full.
 */
internal class LearningCurriculumValidator {
    fun validate(
        learningCurriculum: LearningCurriculum,
        curriculum: Curriculum,
    ): List<LearningCurriculumValidationError> {
        val errors = mutableListOf<LearningCurriculumValidationError>()
        val topicIds = curriculum.topics.map { it.id }.toSet()
        val subtopicIds = curriculum.subtopics.map { it.id }.toSet()

        val lessons = learningCurriculum.units.flatMap { it.lessons }
        val lessonIds = lessons.map { it.id }.filter { it.isNotBlank() }.toSet()
        val duplicateUnitIds = duplicateNonBlankValues(learningCurriculum.units.map { it.id })
        // Lesson ids are compared across every Unit: `relatedLessonIds` names a Lesson
        // without naming its Unit, so an id repeated in two Units is ambiguous.
        val duplicateLessonIds = duplicateNonBlankValues(lessons.map { it.id })

        learningCurriculum.units.forEach { unit ->
            validateUnit(unit, topicIds, duplicateUnitIds, errors)
            unit.lessons.forEach { lesson ->
                validateLesson(lesson, subtopicIds, lessonIds, duplicateLessonIds, errors)
            }
        }

        return errors
    }

    private fun validateUnit(
        unit: LearningUnit,
        topicIds: Set<String>,
        duplicateUnitIds: Set<String>,
        errors: MutableList<LearningCurriculumValidationError>,
    ) {
        if (unit.id.isBlank()) {
            errors.add(error(LearningCurriculumValidationErrorCode.BLANK_UNIT_ID, null, "Learning unit id must not be blank."))
        }
        if (unit.id in duplicateUnitIds) {
            errors.add(error(LearningCurriculumValidationErrorCode.DUPLICATE_UNIT_ID, unit.id, "Learning unit id '${unit.id}' is duplicated."))
        }
        if (unit.title.isBlank()) {
            errors.add(error(LearningCurriculumValidationErrorCode.BLANK_UNIT_TITLE, unit.id, "Learning unit '${unit.id}' title must not be blank."))
        } else if (unit.title.containsPlaceholder()) {
            errors.add(error(LearningCurriculumValidationErrorCode.PLACEHOLDER_UNIT_TITLE, unit.id, "Learning unit '${unit.id}' title contains placeholder content."))
        }
        if (unit.summary.isBlank()) {
            errors.add(error(LearningCurriculumValidationErrorCode.BLANK_UNIT_SUMMARY, unit.id, "Learning unit '${unit.id}' summary must not be blank."))
        } else if (unit.summary.containsPlaceholder()) {
            errors.add(error(LearningCurriculumValidationErrorCode.PLACEHOLDER_UNIT_SUMMARY, unit.id, "Learning unit '${unit.id}' summary contains placeholder content."))
        }
        if (unit.topicId.isBlank()) {
            errors.add(error(LearningCurriculumValidationErrorCode.BLANK_HOME_TOPIC_ID, unit.id, "Learning unit '${unit.id}' home topicId must not be blank."))
        } else if (unit.topicId !in topicIds) {
            errors.add(error(LearningCurriculumValidationErrorCode.UNKNOWN_HOME_TOPIC, unit.id, "Learning unit '${unit.id}' references unknown home topic '${unit.topicId}'."))
        }
        // An active Unit with nothing to study is an authoring defect; a deprecated Unit is
        // kept for stable identity, so it is allowed to be empty.
        if (unit.status == ContentStatus.ACTIVE && unit.lessons.isEmpty()) {
            errors.add(error(LearningCurriculumValidationErrorCode.NO_LESSONS, unit.id, "Active learning unit '${unit.id}' must contain at least one lesson."))
        }
    }

    private fun validateLesson(
        lesson: LearningLesson,
        subtopicIds: Set<String>,
        lessonIds: Set<String>,
        duplicateLessonIds: Set<String>,
        errors: MutableList<LearningCurriculumValidationError>,
    ) {
        validateLessonFields(lesson, duplicateLessonIds, errors)
        validateLessonConcepts(lesson, subtopicIds, errors)
        validateRelatedLessons(lesson, lessonIds, errors)
        validateSections(lesson, errors)
        validateSources(lesson, errors)
    }

    private fun validateLessonFields(
        lesson: LearningLesson,
        duplicateLessonIds: Set<String>,
        errors: MutableList<LearningCurriculumValidationError>,
    ) {
        if (lesson.id.isBlank()) {
            errors.add(error(LearningCurriculumValidationErrorCode.BLANK_LESSON_ID, null, "Lesson id must not be blank."))
        }
        if (lesson.id in duplicateLessonIds) {
            errors.add(error(LearningCurriculumValidationErrorCode.DUPLICATE_LESSON_ID, lesson.id, "Lesson id '${lesson.id}' is duplicated across learning units."))
        }
        if (lesson.title.isBlank()) {
            errors.add(error(LearningCurriculumValidationErrorCode.BLANK_LESSON_TITLE, lesson.id, "Lesson '${lesson.id}' title must not be blank."))
        } else if (lesson.title.containsPlaceholder()) {
            errors.add(error(LearningCurriculumValidationErrorCode.PLACEHOLDER_LESSON_TITLE, lesson.id, "Lesson '${lesson.id}' title contains placeholder content."))
        }
        if (lesson.summary.isBlank()) {
            errors.add(error(LearningCurriculumValidationErrorCode.BLANK_LESSON_SUMMARY, lesson.id, "Lesson '${lesson.id}' summary must not be blank."))
        } else if (lesson.summary.containsPlaceholder()) {
            errors.add(error(LearningCurriculumValidationErrorCode.PLACEHOLDER_LESSON_SUMMARY, lesson.id, "Lesson '${lesson.id}' summary contains placeholder content."))
        }
    }

    private fun validateLessonConcepts(
        lesson: LearningLesson,
        subtopicIds: Set<String>,
        errors: MutableList<LearningCurriculumValidationError>,
    ) {
        if (lesson.status == ContentStatus.ACTIVE && lesson.primarySubtopicIds.isEmpty()) {
            errors.add(error(LearningCurriculumValidationErrorCode.NO_PRIMARY_SUBTOPICS, lesson.id, "Active lesson '${lesson.id}' must have at least one primary subtopic."))
        }

        val duplicatePrimaryIds = duplicateNonBlankValues(lesson.primarySubtopicIds)
        lesson.primarySubtopicIds.forEach { subtopicId ->
            if (subtopicId.isBlank()) {
                errors.add(error(LearningCurriculumValidationErrorCode.BLANK_PRIMARY_SUBTOPIC_ID, lesson.id, "Lesson '${lesson.id}' has a blank primary subtopic id."))
            } else if (subtopicId !in subtopicIds) {
                errors.add(error(LearningCurriculumValidationErrorCode.UNKNOWN_PRIMARY_SUBTOPIC, lesson.id, "Lesson '${lesson.id}' references unknown primary subtopic '$subtopicId'."))
            }
            if (subtopicId in duplicatePrimaryIds) {
                errors.add(error(LearningCurriculumValidationErrorCode.DUPLICATE_PRIMARY_SUBTOPIC_ID, lesson.id, "Lesson '${lesson.id}' lists primary subtopic '$subtopicId' more than once."))
            }
        }

        val duplicateSupportingIds = duplicateNonBlankValues(lesson.supportingSubtopicIds)
        val primaryIds = lesson.primarySubtopicIds.toSet()
        lesson.supportingSubtopicIds.forEach { subtopicId ->
            if (subtopicId.isBlank()) {
                errors.add(error(LearningCurriculumValidationErrorCode.BLANK_SUPPORTING_SUBTOPIC_ID, lesson.id, "Lesson '${lesson.id}' has a blank supporting subtopic id."))
            } else if (subtopicId !in subtopicIds) {
                errors.add(error(LearningCurriculumValidationErrorCode.UNKNOWN_SUPPORTING_SUBTOPIC, lesson.id, "Lesson '${lesson.id}' references unknown supporting subtopic '$subtopicId'."))
            }
            if (subtopicId in duplicateSupportingIds) {
                errors.add(error(LearningCurriculumValidationErrorCode.DUPLICATE_SUPPORTING_SUBTOPIC_ID, lesson.id, "Lesson '${lesson.id}' lists supporting subtopic '$subtopicId' more than once."))
            }
            // Which role was intended cannot be inferred, so the overlap is reported rather
            // than resolved.
            if (subtopicId.isNotBlank() && subtopicId in primaryIds) {
                errors.add(
                    error(
                        LearningCurriculumValidationErrorCode.PRIMARY_SUPPORTING_SUBTOPIC_OVERLAP,
                        lesson.id,
                        "Lesson '${lesson.id}' lists subtopic '$subtopicId' as both primary and supporting.",
                    ),
                )
            }
        }
    }

    private fun validateRelatedLessons(
        lesson: LearningLesson,
        lessonIds: Set<String>,
        errors: MutableList<LearningCurriculumValidationError>,
    ) {
        val duplicateRelatedIds = duplicateNonBlankValues(lesson.relatedLessonIds)

        lesson.relatedLessonIds.forEach { relatedLessonId ->
            if (relatedLessonId.isBlank()) {
                errors.add(error(LearningCurriculumValidationErrorCode.BLANK_RELATED_LESSON_ID, lesson.id, "Lesson '${lesson.id}' has a blank related lesson id."))
            } else if (relatedLessonId !in lessonIds) {
                errors.add(error(LearningCurriculumValidationErrorCode.UNKNOWN_RELATED_LESSON, lesson.id, "Lesson '${lesson.id}' references unknown related lesson '$relatedLessonId'."))
            }
            if (relatedLessonId in duplicateRelatedIds) {
                errors.add(error(LearningCurriculumValidationErrorCode.DUPLICATE_RELATED_LESSON_ID, lesson.id, "Lesson '${lesson.id}' lists related lesson '$relatedLessonId' more than once."))
            }
            if (relatedLessonId.isNotBlank() && relatedLessonId == lesson.id) {
                errors.add(error(LearningCurriculumValidationErrorCode.SELF_RELATED_LESSON, lesson.id, "Lesson '${lesson.id}' relates to itself."))
            }
        }
    }

    private fun validateSections(
        lesson: LearningLesson,
        errors: MutableList<LearningCurriculumValidationError>,
    ) {
        if (lesson.status == ContentStatus.ACTIVE && lesson.sections.isEmpty()) {
            errors.add(error(LearningCurriculumValidationErrorCode.NO_SECTIONS, lesson.id, "Active lesson '${lesson.id}' must contain at least one section."))
        }

        lesson.sections.forEachIndexed { index, section ->
            validateSection(lesson, section, index, errors)
        }
    }

    private fun validateSection(
        lesson: LearningLesson,
        section: LearningSection,
        index: Int,
        errors: MutableList<LearningCurriculumValidationError>,
    ) {
        val location = "lesson '${lesson.id}' section $index (${section.depth})"

        // A section title is optional — many lessons need no heading beyond the depth — but
        // an authored one must say something.
        section.title?.let { title ->
            if (title.isBlank()) {
                errors.add(error(LearningCurriculumValidationErrorCode.BLANK_SECTION_TITLE, lesson.id, "Title of $location must not be blank when present."))
            } else if (title.containsPlaceholder()) {
                errors.add(error(LearningCurriculumValidationErrorCode.PLACEHOLDER_SECTION_TITLE, lesson.id, "Title of $location contains placeholder content."))
            }
        }

        if (lesson.status == ContentStatus.ACTIVE && section.blocks.isEmpty()) {
            errors.add(error(LearningCurriculumValidationErrorCode.EMPTY_SECTION_BLOCKS, lesson.id, "Section $index (${section.depth}) of active lesson '${lesson.id}' must contain at least one content block."))
        }

        section.blocks.forEachIndexed { blockIndex, block ->
            validateBlock(lesson, block, "block $blockIndex of $location", errors)
        }
    }

    private fun validateBlock(
        lesson: LearningLesson,
        block: LearningBlock,
        location: String,
        errors: MutableList<LearningCurriculumValidationError>,
    ) {
        when (block) {
            is LearningBlock.Paragraph -> validateParagraph(lesson, block, location, errors)
            is LearningBlock.BulletList -> validateBulletList(lesson, block, location, errors)
            is LearningBlock.Code -> validateCode(lesson, block, location, errors)
            is LearningBlock.Comparison -> validateComparison(lesson, block, location, errors)
            is LearningBlock.Callout -> validateCallout(lesson, block, location, errors)
        }
    }

    private fun validateParagraph(
        lesson: LearningLesson,
        block: LearningBlock.Paragraph,
        location: String,
        errors: MutableList<LearningCurriculumValidationError>,
    ) {
        if (block.text.isBlank()) {
            errors.add(error(LearningCurriculumValidationErrorCode.BLANK_PARAGRAPH_TEXT, lesson.id, "Paragraph $location must not be blank."))
        } else if (block.text.containsPlaceholder()) {
            errors.add(error(LearningCurriculumValidationErrorCode.PLACEHOLDER_PARAGRAPH_TEXT, lesson.id, "Paragraph $location contains placeholder content."))
        }
    }

    private fun validateBulletList(
        lesson: LearningLesson,
        block: LearningBlock.BulletList,
        location: String,
        errors: MutableList<LearningCurriculumValidationError>,
    ) {
        if (block.items.isEmpty()) {
            errors.add(error(LearningCurriculumValidationErrorCode.EMPTY_BULLET_LIST, lesson.id, "Bullet list $location must contain at least one item."))
        }

        val duplicateItems = duplicateNonBlankValues(block.items.map { it.normalizedForComparison() })
        block.items.forEach { item ->
            if (item.isBlank()) {
                errors.add(error(LearningCurriculumValidationErrorCode.BLANK_BULLET_ITEM, lesson.id, "Bullet list $location has a blank item."))
            } else if (item.containsPlaceholder()) {
                errors.add(error(LearningCurriculumValidationErrorCode.PLACEHOLDER_BULLET_ITEM, lesson.id, "Bullet list $location has an item containing placeholder content."))
            }
            if (item.normalizedForComparison() in duplicateItems) {
                errors.add(error(LearningCurriculumValidationErrorCode.DUPLICATE_BULLET_ITEM, lesson.id, "Bullet list $location repeats the item '$item'."))
            }
        }
    }

    private fun validateCode(
        lesson: LearningLesson,
        block: LearningBlock.Code,
        location: String,
        errors: MutableList<LearningCurriculumValidationError>,
    ) {
        if (block.code.isBlank()) {
            errors.add(error(LearningCurriculumValidationErrorCode.BLANK_CODE, lesson.id, "Code $location must not be blank."))
        } else if (block.code.containsPlaceholder()) {
            errors.add(error(LearningCurriculumValidationErrorCode.PLACEHOLDER_CODE, lesson.id, "Code $location contains placeholder content."))
        }
        // The language identifier stays open: a closed enum would reject the next language
        // a lesson needs. Only an authored-but-empty value is a defect.
        if (block.language != null && block.language.isBlank()) {
            errors.add(error(LearningCurriculumValidationErrorCode.BLANK_CODE_LANGUAGE, lesson.id, "Code $location has a blank language."))
        }
    }

    private fun validateComparison(
        lesson: LearningLesson,
        block: LearningBlock.Comparison,
        location: String,
        errors: MutableList<LearningCurriculumValidationError>,
    ) {
        if (block.headers.isEmpty()) {
            errors.add(error(LearningCurriculumValidationErrorCode.EMPTY_COMPARISON_HEADERS, lesson.id, "Comparison $location must have at least one header."))
        }

        val duplicateHeaders = duplicateNonBlankValues(block.headers.map { it.normalizedForComparison() })
        block.headers.forEach { header ->
            if (header.isBlank()) {
                errors.add(error(LearningCurriculumValidationErrorCode.BLANK_COMPARISON_HEADER, lesson.id, "Comparison $location has a blank header."))
            }
            if (header.normalizedForComparison() in duplicateHeaders) {
                errors.add(error(LearningCurriculumValidationErrorCode.DUPLICATE_COMPARISON_HEADER, lesson.id, "Comparison $location repeats the header '$header'."))
            }
        }

        if (block.rows.isEmpty()) {
            errors.add(error(LearningCurriculumValidationErrorCode.EMPTY_COMPARISON_ROWS, lesson.id, "Comparison $location must have at least one row."))
        }

        block.rows.forEachIndexed { rowIndex, row ->
            // A row is never padded or truncated to fit: a short row means the author left a
            // cell out, and only the author knows which.
            if (row.size != block.headers.size) {
                errors.add(
                    error(
                        LearningCurriculumValidationErrorCode.COMPARISON_COLUMN_COUNT_MISMATCH,
                        lesson.id,
                        "Comparison $location row $rowIndex has ${row.size} cells but the table has ${block.headers.size} columns.",
                    ),
                )
            }
            row.forEach { cell ->
                if (cell.isBlank()) {
                    errors.add(error(LearningCurriculumValidationErrorCode.BLANK_COMPARISON_CELL, lesson.id, "Comparison $location row $rowIndex has a blank cell."))
                } else if (cell.containsPlaceholder()) {
                    errors.add(error(LearningCurriculumValidationErrorCode.PLACEHOLDER_COMPARISON_CELL, lesson.id, "Comparison $location row $rowIndex has a cell containing placeholder content."))
                }
            }
        }
    }

    private fun validateCallout(
        lesson: LearningLesson,
        block: LearningBlock.Callout,
        location: String,
        errors: MutableList<LearningCurriculumValidationError>,
    ) {
        // `kind` is already constrained by its enum, and what a callout looks like is the
        // renderer's business, so only the authored text is validated here.
        if (block.text.isBlank()) {
            errors.add(error(LearningCurriculumValidationErrorCode.BLANK_CALLOUT_TEXT, lesson.id, "Callout $location must not be blank."))
        } else if (block.text.containsPlaceholder()) {
            errors.add(error(LearningCurriculumValidationErrorCode.PLACEHOLDER_CALLOUT_TEXT, lesson.id, "Callout $location contains placeholder content."))
        }
    }

    /**
     * Structural source validation only. Whether a source is *authoritative* — the editorial
     * requirement of the learning-content authoring contract — cannot be decided from a URL,
     * because authoritative documentation for Android, Kotlin, Koin, Ktor, Room and every
     * library the curriculum grows into lives on hosts no fixed allowlist could enumerate.
     * That judgement stays with content review.
     */
    private fun validateSources(
        lesson: LearningLesson,
        errors: MutableList<LearningCurriculumValidationError>,
    ) {
        if (lesson.status == ContentStatus.ACTIVE && lesson.sources.isEmpty()) {
            errors.add(error(LearningCurriculumValidationErrorCode.NO_SOURCES, lesson.id, "Active lesson '${lesson.id}' must have at least one source."))
        }

        lesson.sources.forEach { source ->
            validateSource(lesson, source, errors)
        }
    }

    private fun validateSource(
        lesson: LearningLesson,
        source: SourceReference,
        errors: MutableList<LearningCurriculumValidationError>,
    ) {
        if (source.title.isBlank()) {
            errors.add(error(LearningCurriculumValidationErrorCode.BLANK_SOURCE_TITLE, lesson.id, "Lesson '${lesson.id}' has a source with a blank title."))
        } else if (source.title.containsPlaceholder()) {
            errors.add(error(LearningCurriculumValidationErrorCode.PLACEHOLDER_SOURCE_TITLE, lesson.id, "Lesson '${lesson.id}' has placeholder source title '${source.title}'."))
        }
        if (source.url.isBlank()) {
            errors.add(error(LearningCurriculumValidationErrorCode.BLANK_SOURCE_URL, lesson.id, "Lesson '${lesson.id}' has a source with a blank URL."))
        } else if (!source.url.isValidHttpUrl()) {
            errors.add(error(LearningCurriculumValidationErrorCode.INVALID_SOURCE_URL, lesson.id, "Lesson '${lesson.id}' has invalid source URL '${source.url}'."))
        } else if (source.url.isPlaceholderUrl()) {
            errors.add(error(LearningCurriculumValidationErrorCode.PLACEHOLDER_SOURCE_URL, lesson.id, "Lesson '${lesson.id}' has placeholder source URL '${source.url}'."))
        }
    }

    private fun error(
        code: LearningCurriculumValidationErrorCode,
        entityId: String?,
        message: String,
    ) = LearningCurriculumValidationError(
        code = code,
        entityId = entityId?.takeUnless { it.isBlank() },
        message = message,
    )
}
