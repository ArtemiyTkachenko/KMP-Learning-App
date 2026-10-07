package org.artkachenko.kmp_learning_app.progress

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.artkachenko.kmp_learning_app.learning_progress.ReadinessTarget

/**
 * The interview level the learner is preparing for, owned for the lifetime of the application.
 *
 * Follows [org.artkachenko.kmp_learning_app.settings.AppearanceStateHolder] and
 * `CurriculumVisibilityStateHolder`: the app-scoped [ProgressStateHolder] derives readiness from it,
 * and the Progress section that changes it is a navigation entry that comes and goes, so the holder
 * must outlive every destination. It is a Koin `single`.
 *
 * The stored choice is read once, synchronously, in the constructor, so the dashboard's first
 * derivation already knows whether to show statuses or the chooser.
 */
internal class ReadinessTargetStateHolder(private val store: ReadinessTargetPreferenceStore) {

    private val _target = MutableStateFlow(store.read())

    /** `null` until the learner has chosen, which Progress shows as a chooser. */
    val target: StateFlow<ReadinessTarget?> = _target.asStateFlow()

    /**
     * Records an explicit choice and persists it. The observable state is updated first, so the
     * readiness section follows the learner's tap immediately; the synchronous write is the same
     * one-key write the other preference holders make.
     */
    fun setTarget(target: ReadinessTarget) {
        if (_target.value == target) return
        _target.value = target
        store.write(target)
    }
}
