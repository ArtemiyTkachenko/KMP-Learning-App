package org.artkachenko.kmp_learning_app.curriculum.learning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A unit of structured lesson content.
 *
 * The hierarchy is sealed so authored JSON stays type-safe, and every variant carries an
 * explicit [SerialName] because the serialized discriminator is part of the long-lived
 * content contract and must not depend on generated class names.
 *
 * Blocks describe what content means, never how it is presented: no styling, layout, or
 * platform types belong here. Prose is plain authored text rather than Markdown or a
 * rich-text tree, so nothing downstream has to assume a parser.
 */
@Serializable
internal sealed interface LearningBlock {
    /** Ordinary explanatory prose. */
    @Serializable
    @SerialName("paragraph")
    data class Paragraph(
        val text: String,
    ) : LearningBlock

    /** An unordered teaching list. Items are plain text; nesting is intentionally absent. */
    @Serializable
    @SerialName("bullet_list")
    data class BulletList(
        val items: List<String>,
    ) : LearningBlock

    /**
     * A code example. [code] is the raw source without Markdown fences; [language] is an
     * optional identifier a future renderer may use for syntax highlighting.
     */
    @Serializable
    @SerialName("code")
    data class Code(
        val code: String,
        val language: String? = null,
    ) : LearningBlock

    /**
     * Tabular content, typically a comparison of alternatives. Rows are structured rather
     * than pre-aligned text; column-count consistency is a content-validation concern, so
     * it is not enforced here.
     */
    @Serializable
    @SerialName("comparison")
    data class Comparison(
        val headers: List<String>,
        val rows: List<List<String>>,
    ) : LearningBlock

    /** Emphasised content whose [kind] states why it is emphasised. */
    @Serializable
    @SerialName("callout")
    data class Callout(
        val kind: LearningCalloutKind,
        val text: String,
    ) : LearningBlock
}
