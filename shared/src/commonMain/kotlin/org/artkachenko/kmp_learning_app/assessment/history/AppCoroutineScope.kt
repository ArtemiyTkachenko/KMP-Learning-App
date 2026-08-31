package org.artkachenko.kmp_learning_app.assessment.history

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * A scope that lives as long as the app process, for state that must outlive any one screen.
 *
 * The caches built on it are shared by several screens and survive a navigation entry being
 * destroyed, so they cannot belong to a `viewModelScope`. A [SupervisorJob] keeps one failing cache
 * from cancelling the others, and it is a distinct type rather than a plain `CoroutineScope` so
 * injecting it is unambiguous.
 */
internal class AppCoroutineScope(
    delegate: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : CoroutineScope by delegate
