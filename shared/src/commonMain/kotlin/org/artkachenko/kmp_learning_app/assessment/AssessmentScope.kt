package org.artkachenko.kmp_learning_app.assessment

internal sealed interface AssessmentScope {
    data class Topic(
        val topicId: String,
    ) : AssessmentScope {
        init {
            require(topicId.isNotBlank()) {
                "topicId must not be blank."
            }
        }
    }

    data class Subtopic(
        val subtopicId: String,
    ) : AssessmentScope {
        init {
            require(subtopicId.isNotBlank()) {
                "subtopicId must not be blank."
            }
        }
    }

    /**
     * Several Subtopics practised as one assessment: the union of their eligible Questions.
     *
     * This exists because a single teaching unit can be responsible for concepts that live under
     * different Topics, and quizzing it as a Topic run would ask about material it never taught.
     * The scope stays a plain set of stable Subtopic IDs on purpose: whatever resolves a teaching
     * unit into concepts does so *before* assessment, so nothing here — selection, persistence,
     * reconstruction, retake — depends on learning content, and an attempt stays the assessment it
     * originally was even if that unit is later re-authored.
     *
     * A `Set` rather than a `List` because the scope means "these concepts are eligible", not
     * "these concepts have an authored order". `{a, b}` and `{b, a}` are the same scope, and a
     * repeated ID is the same concept named twice, so callers canonicalise by building the set —
     * a duplicate cannot survive construction and cannot weight selection. Order does appear once,
     * in the persisted encoding, where it is sorted purely so equal scopes store identically.
     *
     * [Subtopic] is deliberately not redefined as a one-element [Subtopics]: single-Subtopic
     * practice has shipped behaviour and its own persisted representation, and folding it in here
     * would trade a regression risk for a handful of removed branches.
     */
    data class Subtopics(
        val subtopicIds: Set<String>,
    ) : AssessmentScope {
        init {
            require(subtopicIds.isNotEmpty()) {
                "subtopicIds must not be empty."
            }
            require(subtopicIds.none { it.isBlank() }) {
                "subtopicIds must not contain blank IDs."
            }
        }
    }
}
