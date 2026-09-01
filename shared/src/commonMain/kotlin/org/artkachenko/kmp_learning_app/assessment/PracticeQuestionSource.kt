package org.artkachenko.kmp_learning_app.assessment

import kotlinx.serialization.Serializable

/**
 * Where targeted practice draws its eligible Questions from.
 *
 * The four values are mutually exclusive selection policies, not independent filters, which is
 * why they are one dimension rather than a set of `onlyUnseen`/`weakOnly`/`mistakesOnly` flags:
 * "unseen weak mistakes" is not a product concept, and boolean combinations would invent one.
 *
 * [ALL] and [UNSEEN] are implemented. [WEAK_AREAS] and [UNRESOLVED_MISTAKES] are derived from
 * completed assessment history too and are owned by E16-04 and E16-05; until then they are
 * representable but produce an explicit no-content selection outcome instead of quietly widening
 * back to [ALL].
 *
 * Serializable because the Practice Builder's chosen source travels to assessment taking as a
 * typed navigation argument.
 */
@Serializable
internal enum class PracticeQuestionSource {
    /** Every ACTIVE Question in the configured scope and levels. */
    ALL,

    /**
     * ACTIVE Questions in the configured scope and levels whose stable IDs have never appeared in
     * completed assessment history. Exposure, not performance: how the learner answered a Question
     * makes no difference to whether they have seen it.
     */
    UNSEEN,

    /** ACTIVE Questions belonging to scopes the weak-area policy already flags. Reserved for E16-04. */
    WEAK_AREAS,

    /** ACTIVE Questions whose latest completed occurrence was incorrect. Reserved for E16-05. */
    UNRESOLVED_MISTAKES,
}
