package org.artkachenko.kmp_learning_app.progress

import org.artkachenko.kmp_learning_app.learning_progress.ReadinessTarget
import org.artkachenko.kmp_learning_app.settings.InMemoryAppPreferenceStorage

/** A target holder over in-memory storage, with [target] already chosen when it is not `null`. */
internal fun readinessTargetStateHolder(target: ReadinessTarget? = null): ReadinessTargetStateHolder =
    ReadinessTargetStateHolder(
        ReadinessTargetPreferenceStore(InMemoryAppPreferenceStorage()).also { store ->
            target?.let(store::write)
        },
    )
