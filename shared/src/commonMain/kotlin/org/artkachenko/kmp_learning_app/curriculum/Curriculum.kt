package org.artkachenko.kmp_learning_app.curriculum

import kotlinx.serialization.Serializable

/**
 * The authored assessment curriculum; in production, the bundled question bank.
 *
 * [topics], [subtopics] and [questions] are ordered authored sequences. Each entry's list
 * position is persisted as its `sortOrder` on import, so position is ordering metadata
 * while the stable `id` stays the identity. Once an entry is accepted it keeps its
 * position: new entries are appended, and deprecation or a `topicId`/`subtopicId` change
 * never moves an entry. Sorting, regrouping or inserting before an accepted entry is not
 * routine maintenance. See `docs/content/content-authoring.md`, "Stable Authored Order".
 */
@Serializable
internal data class Curriculum(
    val topics: List<Topic>,
    val subtopics: List<Subtopic>,
    val questions: List<Question>,
)
