package org.artkachenko.kmp_learning_app.assessment

/**
 * Where targeted practice draws its eligible Questions from.
 *
 * The four values are mutually exclusive selection policies, not independent filters, which is
 * why they are one dimension rather than a set of `onlyUnseen`/`weakOnly`/`mistakesOnly` flags:
 * "unseen weak mistakes" is not a product concept, and boolean combinations would invent one.
 *
 * Only [ALL] is selectable today. [UNSEEN], [WEAK_AREAS], and [UNRESOLVED_MISTAKES] are derived
 * from completed assessment history and are owned by E16-03, E16-04, and E16-05 respectively;
 * until then they are representable but produce an explicit no-content selection outcome instead
 * of quietly widening back to [ALL].
 */
internal enum class PracticeQuestionSource {
    /** Every ACTIVE Question in the configured scope and levels. */
    ALL,

    /** ACTIVE Questions never observed in completed history. Reserved for E16-03. */
    UNSEEN,

    /** ACTIVE Questions belonging to scopes the weak-area policy already flags. Reserved for E16-04. */
    WEAK_AREAS,

    /** ACTIVE Questions whose latest completed occurrence was incorrect. Reserved for E16-05. */
    UNRESOLVED_MISTAKES,
}
