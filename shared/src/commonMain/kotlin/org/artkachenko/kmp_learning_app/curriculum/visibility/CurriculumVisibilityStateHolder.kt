package org.artkachenko.kmp_learning_app.curriculum.visibility

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.artkachenko.kmp_learning_app.settings.KmpContentPreferenceStore

/**
 * The learner's curriculum visibility, owned for the lifetime of the application.
 *
 * Follows [org.artkachenko.kmp_learning_app.settings.AppearanceStateHolder] for the same reasons:
 * the repository decorators, the history projection, the Settings switch, the Topic Browser and
 * Topic Detail — and later the navigator — all read it, so it must outlive every destination and be
 * the one instance they share. It is a Koin `single`.
 *
 * The stored preference is read once, synchronously, in the constructor, so the first read of the
 * curriculum already sees the learner's choice and optional content never appears for a moment
 * before disappearing.
 */
internal class CurriculumVisibilityStateHolder(private val store: KmpContentPreferenceStore) {

    private val _includeKmpContent = MutableStateFlow(store.read())

    val includeKmpContent: StateFlow<Boolean> = _includeKmpContent.asStateFlow()

    private val _visibility = MutableStateFlow(CurriculumVisibility.from(_includeKmpContent.value))

    /** The visibility every eligibility read and history derivation applies. */
    val visibility: StateFlow<CurriculumVisibility> = _visibility.asStateFlow()

    /**
     * Records an explicit choice and persists it.
     *
     * The write is synchronous, as the appearance preference's is, so the choice is durable by the
     * time the call returns. The observable state is updated first so observers follow the action
     * immediately. Only the preference is written: hiding content never touches stored attempts,
     * saved Questions or study records.
     *
     * Written even when the value is unchanged: on a fresh install the state is already OFF with
     * nothing stored, and a learner who explicitly chooses OFF must end up with a stored "off" that
     * a future change of default cannot reinterpret.
     */
    fun setIncludeKmpContent(enabled: Boolean) {
        _includeKmpContent.value = enabled
        _visibility.value = CurriculumVisibility.from(enabled)
        store.write(enabled)
    }
}
