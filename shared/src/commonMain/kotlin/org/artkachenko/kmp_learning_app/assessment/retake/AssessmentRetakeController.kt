package org.artkachenko.kmp_learning_app.assessment.retake

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Where one result surface has got to in taking its assessment again.
 *
 * This is the learner's action on a result, not a property of the result itself: the score, the
 * transcript, and the per-Topic breakdown are settled facts about a completed attempt and cannot
 * change while the screen is open, whereas this moves every time the learner presses the button.
 * Holding it apart from the result content is what lets every transition below be an unconditional
 * write rather than a cast that silently drops the change.
 */
internal sealed interface AssessmentRetakeState {
    /** No retake has been asked for, or the last one has been navigated to. */
    data object Idle : AssessmentRetakeState

    data object Creating : AssessmentRetakeState

    /**
     * A retake exists and is waiting to be opened.
     *
     * Terminal until [AssessmentRetakeController.onCreatedAttemptHandled] confirms the same
     * identity reached navigation. See [AssessmentRetakeController.start] for why.
     */
    data class Created(val attemptId: String) : AssessmentRetakeState

    data object SourceAttemptNotFound : AssessmentRetakeState

    data object NoEligibleQuestions : AssessmentRetakeState

    data object Error : AssessmentRetakeState
}

/** A retake attempt was created, and its identity has to reach navigation exactly once. */
internal data class AssessmentRetakeCreated(val attemptId: String)

/**
 * Taking one completed attempt again, for the surface that shows its result.
 *
 * The Focused practice result and the Mixed interview result offer the same action over the same
 * [AssessmentRetakeService], with the same six outcomes and the same re-entry rules; they had two
 * identical copies of this state machine, under two names, with two one-case event hierarchies
 * beside them. The copies are what made the post-persistence re-entry defect a thing that had to be
 * found and fixed twice. One owner, delegated to from both, is what stops the third copy.
 *
 * What stays with each result ViewModel is what actually differs: whether there is a loaded result
 * to retake at all, and the wording the screen puts on each of these states.
 */
internal class AssessmentRetakeController(
    private val sourceAttemptId: String,
    private val retakeService: AssessmentRetakeService,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<AssessmentRetakeState>(AssessmentRetakeState.Idle)
    val state: StateFlow<AssessmentRetakeState> = _state.asStateFlow()

    private val _createdAttempts = Channel<AssessmentRetakeCreated>(Channel.BUFFERED)

    /**
     * Buffered and single-consumer: the destination collects this while it is composed and turns
     * each identity into one navigation.
     */
    val createdAttempts: Flow<AssessmentRetakeCreated> = _createdAttempts.receiveAsFlow()

    /**
     * Creates one retake, unless one is already being created or is waiting to be opened.
     *
     * [AssessmentRetakeState.Created] is deliberately terminal rather than a transient success.
     * Persistence finishes before the buffered event is consumed, so returning to
     * [AssessmentRetakeState.Idle] on success would leave a real window in which pressing the
     * button again created a second, independently identified durable attempt. Re-entry reopens
     * only once [onCreatedAttemptHandled] confirms that the same identity reached navigation.
     *
     * The three failure states do not block re-entry: nothing durable was created, so pressing the
     * button again is the retry.
     */
    fun start() {
        val current = _state.value
        if (current == AssessmentRetakeState.Creating || current is AssessmentRetakeState.Created) {
            return
        }
        _state.value = AssessmentRetakeState.Creating
        scope.launch {
            runCatching { retakeService.createRetake(sourceAttemptId) }
                .onSuccess { result ->
                    when (result) {
                        is AssessmentRetakeResult.Created -> {
                            _state.value = AssessmentRetakeState.Created(result.attemptId)
                            _createdAttempts.send(AssessmentRetakeCreated(result.attemptId))
                        }
                        AssessmentRetakeResult.SourceAttemptNotFound ->
                            _state.value = AssessmentRetakeState.SourceAttemptNotFound
                        AssessmentRetakeResult.NoEligibleQuestions ->
                            _state.value = AssessmentRetakeState.NoEligibleQuestions
                    }
                }
                .onFailure { failure ->
                    // Cancellation means the owning scope is ending, which is not an outcome to
                    // report to the learner as a failed retake.
                    if (failure is CancellationException) throw failure
                    _state.value = AssessmentRetakeState.Error
                }
        }
    }

    /**
     * Reopens the action once [attemptId] has been navigated to.
     *
     * Matching the identity matters: a stale confirmation for an earlier retake must not release
     * the guard that is protecting the current one.
     */
    fun onCreatedAttemptHandled(attemptId: String) {
        val created = _state.value as? AssessmentRetakeState.Created ?: return
        if (created.attemptId == attemptId) _state.value = AssessmentRetakeState.Idle
    }
}
