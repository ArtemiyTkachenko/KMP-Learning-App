package org.artkachenko.kmp_learning_app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource

@Serializable
internal sealed interface AppRoute : NavKey {
    @Serializable
    data object Topics : AppRoute

    @Serializable
    data object Interview : AppRoute

    @Serializable
    data object Progress : AppRoute

    @Serializable
    data class ProgressTopic(
        val topicId: String,
    ) : AppRoute

    @Serializable
    data object MistakeReview : AppRoute

    /**
     * The learner's saved Questions.
     *
     * A detail destination inside Topics rather than an area of its own: saved Questions are
     * learner-curated curriculum content, so they belong beside Topic detail and the Practice
     * Builder instead of becoming a fifth navigation-bar item. The route carries nothing — which
     * Questions are saved is read from the shared saved state on arrival, never from the back
     * stack.
     */
    @Serializable
    data object SavedQuestions : AppRoute

    @Serializable
    data class Topic(
        val topicId: String,
        val subtopicId: String? = null,
    ) : AppRoute

    /**
     * One authored Learning Unit's overview.
     *
     * Only the stable Unit ID travels. Title, summary, and the Lessons that make the Unit up are
     * publisher-owned content resolved from `LearningContentRepository` on arrival, so a re-authored
     * Unit is never shown under prose that was serialized into the back stack when it was opened.
     *
     * The home Topic is deliberately absent: this route is pushed from `Topic`, so back returns
     * there through the stack rather than through a Topic ID reconstructed here.
     */
    @Serializable
    data class LearningUnit(
        val unitId: String,
    ) : AppRoute

    /**
     * One Lesson, addressed within the Unit it was opened from.
     *
     * Lesson IDs are unique across the whole learning document, so [unitId] is not needed to find
     * the Lesson — it is here to say which Unit the learner is reading it in. The destination
     * resolves the Lesson *through* that Unit, which makes parent membership a precondition rather
     * than an assumption: an inconsistent pair becomes a controlled unavailable state instead of
     * quietly opening another Unit's Lesson. It also keeps the authored ordering context E21-04
     * needs for previous/next without adding a route then.
     *
     * No Lesson prose travels: title, summary, sections, and Sources are resolved on arrival.
     */
    @Serializable
    data class LearningLesson(
        val unitId: String,
        val lessonId: String,
    ) : AppRoute

    @Serializable
    data class MixedInterviewAttempt(
        val attemptId: String,
    ) : AppRoute

    @Serializable
    data class MixedInterviewResult(
        val attemptId: String,
    ) : AppRoute

    /**
     * The Practice Builder, scoped by the stable ID it was opened from.
     *
     * Only the ID travels: the Topic or Subtopic name is resolved from the curriculum on arrival,
     * so a renamed Topic cannot be shown under a stale label saved into the back stack.
     *
     * [source] is the builder's *initial* selection, not a decided run. It defaults to `ALL`, which
     * is the entry Topic Detail has always used, so opening the builder from content is unchanged.
     * A caller that already knows which practice intent it means — a remembered targeted run, or a
     * later guided-learning shortcut — passes it here rather than adding a route per source. Every
     * other dimension stays the builder's: count, levels, preflight, and Start.
     */
    @Serializable
    data class PracticeBuilderTopic(
        val topicId: String,
        val source: PracticeQuestionSource = PracticeQuestionSource.ALL,
    ) : AppRoute

    @Serializable
    data class PracticeBuilderSubtopic(
        val subtopicId: String,
        val source: PracticeQuestionSource = PracticeQuestionSource.ALL,
    ) : AppRoute

    /**
     * The Practice Builder, opened on a Learning Unit.
     *
     * Only the stable Unit ID travels, for a stronger reason than the Topic and Subtopic routes
     * have: what a Unit teaches is authored content that changes between releases. The title *and*
     * the concepts practised are resolved from `LearningContentRepository` on arrival, so a
     * re-authored Unit is practised as it currently reads rather than as it read when this entry
     * was pushed. The derived Subtopic set is deliberately absent — a back-stack entry holding it
     * would be a second, silently stale copy of the Unit's teaching responsibility.
     *
     * No `source` field, unlike the two above: nothing produces a Learning-Unit practice intent, so
     * both entries open on the builder's own `ALL` default.
     */
    @Serializable
    data class PracticeBuilderLearningUnit(
        val unitId: String,
    ) : AppRoute

    @Serializable
    data class FocusedPracticeResult(
        val attemptId: String,
    ) : AppRoute

    @Serializable
    data class FocusedPracticeAttempt(
        val attemptId: String,
    ) : AppRoute
}
