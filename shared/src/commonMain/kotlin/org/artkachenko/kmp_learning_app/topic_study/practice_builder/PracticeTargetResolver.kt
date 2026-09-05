package org.artkachenko.kmp_learning_app.topic_study.practice_builder

import kotlin.coroutines.cancellation.CancellationException
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

/**
 * Turns what the learner chose to practise into what an assessment can run.
 *
 * This is the only crossing from learning content into assessment configuration, and it runs in one
 * direction: a Unit is read here and leaves as a plain set of Subtopic IDs, so nothing downstream —
 * selection, the engine, persistence, retake — ever learns that Learning Units exist. Putting the
 * crossing in the presentation layer rather than in the assessment domain is what keeps that
 * dependency edge from reversing.
 *
 * It sits beside the builder rather than inside its ViewModel so the resolution rules can be tested
 * without a ViewModel, and so the builder depends on one collaborator instead of on two
 * repositories, only one of which any given target uses.
 */
internal class PracticeTargetResolver(
    private val curriculumRepository: CurriculumRepository,
    private val learningContentRepository: LearningContentRepository,
) {
    /**
     * Resolving a Topic or Subtopic target is unchanged behaviour: the scope is already known from
     * the stable ID, and the curriculum is read only for a display name. A failed name read
     * therefore stays a missing label rather than a failed resolution — practice on a Topic whose
     * name will not load still runs, exactly as it did before targets existed.
     *
     * A Learning Unit is the opposite: the document read *is* the resolution, so it is allowed to
     * throw and the caller decides what an unreadable document means on screen.
     */
    suspend fun resolve(target: PracticeBuilderTarget): PracticeTargetResolution =
        when (target) {
            is PracticeBuilderTarget.Topic -> PracticeTargetResolution.Resolved(
                name = displayName { curriculumRepository.getTopicById(target.topicId)?.name },
                scope = AssessmentScope.Topic(target.topicId),
            )

            is PracticeBuilderTarget.Subtopic -> PracticeTargetResolution.Resolved(
                name = displayName {
                    curriculumRepository.getSubtopicById(target.subtopicId)?.name
                },
                scope = AssessmentScope.Subtopic(target.subtopicId),
            )

            is PracticeBuilderTarget.LearningUnit -> resolveLearningUnit(target.unitId)
        }

    /**
     * `getUnitById` resolves retired material on purpose, so the ACTIVE check is applied here for
     * the same reason the Unit overview applies it: browsing eligibility is presentation's rule.
     * Deprecated study material must not become newly practiceable through a stale back-stack
     * entry that still names it.
     *
     * The empty case is checked *before* `AssessmentScope.Subtopics`, whose non-empty requirement
     * is a domain invariant rather than a user-facing outcome: a Unit that currently teaches
     * nothing assessable is a controlled answer, not a crash.
     */
    private suspend fun resolveLearningUnit(unitId: String): PracticeTargetResolution {
        val unit = learningContentRepository.getUnitById(unitId)
            ?.takeIf { it.status == ContentStatus.ACTIVE }
            ?: return PracticeTargetResolution.Unavailable

        val subtopicIds = unit.activePrimarySubtopicIds()
        if (subtopicIds.isEmpty()) return PracticeTargetResolution.NoPracticeableConcepts

        return PracticeTargetResolution.Resolved(
            // The current title, read from the document that was just resolved. Nothing carries a
            // title into this screen, so there is no stale label it could be preferred over.
            name = unit.title,
            scope = AssessmentScope.Subtopics(subtopicIds),
        )
    }

    private suspend fun displayName(read: suspend () -> String?): String? =
        try {
            read()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (@Suppress("TooGenericExceptionCaught") failure: Throwable) {
            null
        }
}

/**
 * What a Unit is responsible for assessing: the deduplicated primary concepts of its current
 * Lessons.
 *
 * Three authoring rules are enforced by this one expression. Only ACTIVE Lessons contribute, so
 * retired material does not keep quizzing the learner. Only `primarySubtopicIds` contribute —
 * `supportingSubtopicIds` exist so a Lesson can explain enough surrounding context to stand on its
 * own, which is a very different claim from being responsible for teaching it. And the Unit's home
 * Topic is never consulted: a Unit may legitimately own primary concepts from several Topics, and
 * narrowing to its own Topic would silently drop exactly the cross-Topic bridging the learning
 * content is authored to do.
 *
 * A `Set` because two Lessons may deliberately share a primary concept and it is still one concept.
 * Blank IDs are dropped rather than rejected: content validation already refuses them, so the only
 * way one arrives is a document that should not have loaded, and reaching an unavailable state is
 * better than failing a domain precondition on a screen.
 */
private fun LearningUnit.activePrimarySubtopicIds(): Set<String> =
    lessons
        .filter { it.status == ContentStatus.ACTIVE }
        .flatMap { it.primarySubtopicIds }
        .filter { it.isNotBlank() }
        .toSet()

/**
 * The answer to "what is being practised", separated from whether any Question currently matches
 * it. A target that cannot become a scope is a settled statement about content; an eligible pool
 * that happens to be empty is a statement about the learner's filters, and the builder reports the
 * two differently.
 */
internal sealed interface PracticeTargetResolution {
    /**
     * A runnable scope, plus the label the screen shows. [name] is null when the display lookup
     * found nothing, which never blocks practice — the scope is what Start depends on.
     */
    data class Resolved(
        val name: String?,
        val scope: AssessmentScope,
    ) : PracticeTargetResolution

    /** The target names nothing that is current study material — a stale or retired Unit. */
    data object Unavailable : PracticeTargetResolution

    /** The target resolves, but currently teaches no concept that can be assessed. */
    data object NoPracticeableConcepts : PracticeTargetResolution
}
