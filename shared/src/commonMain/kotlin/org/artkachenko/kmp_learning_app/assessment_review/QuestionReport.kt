package org.artkachenko.kmp_learning_app.assessment_review

import org.artkachenko.kmp_learning_app.product.ProductMetadata
import org.artkachenko.kmp_learning_app.product.ProductRepositoryUrl

/**
 * The form's file name under `.github/ISSUE_TEMPLATE/`, which GitHub's `template` parameter selects.
 */
private const val QuestionReportTemplate = "question-report.yml"

/** The form's own title prefix, repeated so a pre-filled title reads like a hand-written one. */
private const val QuestionReportTitlePrefix = "[Question] "

/** The version a report names: the marketing version and build, as Settings shows them. */
internal val QuestionReportAppVersion: String =
    "${ProductMetadata.VERSION} (${ProductMetadata.BUILD_NUMBER})"

/**
 * The pre-filled issue form for reporting one Question.
 *
 * Only the identity of the Question and the build are carried. The question text, the answers,
 * and anything about the learner's attempt are deliberately left out: the URL stays short, and the
 * learner decides on GitHub what else to say before anything is submitted. Nothing is sent from
 * the device — this only builds the address a browser opens.
 *
 * Every value is percent-encoded, so a Question ID containing `&`, `#` or `=` stays one value
 * rather than ending the parameter or starting another.
 */
internal fun questionReportUrl(questionId: String, appVersion: String): String {
    val parameters = listOf(
        "template" to QuestionReportTemplate,
        "title" to QuestionReportTitlePrefix + questionId,
        "question_id" to questionId,
        "app_version" to appVersion,
    )
    return parameters.joinToString(separator = "&", prefix = "$ProductRepositoryUrl/issues/new?") { (name, value) ->
        "$name=${value.percentEncoded()}"
    }
}

/**
 * RFC 3986 percent-encoding of a query value: every byte of the UTF-8 form is escaped except the
 * unreserved characters. Spaces become `%20` rather than `+`, which GitHub reads either way, and a
 * literal `+` is escaped so it cannot be read as a space.
 */
private fun String.percentEncoded(): String = buildString {
    for (byte in this@percentEncoded.encodeToByteArray()) {
        val char = byte.toInt().toChar()
        if (byte >= 0 && char.isUnreserved()) {
            append(char)
        } else {
            append('%')
            append(HexDigits[(byte.toInt() shr 4) and 0x0F])
            append(HexDigits[byte.toInt() and 0x0F])
        }
    }
}

private fun Char.isUnreserved(): Boolean =
    this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this == '-' || this == '.' ||
        this == '_' || this == '~'

private const val HexDigits = "0123456789ABCDEF"
