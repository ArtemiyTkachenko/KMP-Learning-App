package org.artkachenko.kmp_learning_app.curriculum.validation

/**
 * Low-level checks shared by the assessment and learning content validators.
 *
 * They live here rather than inside one validator because both documents are authored by
 * hand against the same editorial rules: an authoring stub, a broken URL, or a duplicated
 * string means the same thing in a question as in a lesson, and two copies of these rules
 * would drift apart. They deliberately stay primitive — each validator keeps its own error
 * model, entity vocabulary, and ordering.
 */

/**
 * Placeholder detection is deliberately narrow. It catches authoring stubs that no finished
 * content should ship, and nothing that a real Android or Kotlin question or lesson might
 * legitimately discuss: `TODO(` is excluded because Kotlin's `TODO()` is a fair subject to
 * write about and a fair thing for an example to contain.
 */
internal fun String.containsPlaceholder(): Boolean = PLACEHOLDER_MARKER.containsMatchIn(this)

internal fun String.isValidHttpUrl(): Boolean {
    val schemeSeparator = indexOf("://")
    if (schemeSeparator <= 0) return false

    val scheme = substring(0, schemeSeparator)
    if (scheme != "http" && scheme != "https") return false

    val remainder = substring(schemeSeparator + 3)
    val host = remainder.takeWhile { it != '/' && it != '?' && it != '#' }
    return host.isNotBlank()
}

internal fun String.isPlaceholderUrl(): Boolean {
    val host = substringAfter("://")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .substringBefore(':')
        .lowercase()
    return PLACEHOLDER_URL_HOSTS.any { host == it || host.endsWith(".$it") }
}

/** Comparison form for authored text, so casing and padding cannot hide a duplicate. */
internal fun String.normalizedForComparison(): String = trim().lowercase()

internal fun duplicateNonBlankValues(values: List<String>): Set<String> {
    val seen = mutableSetOf<String>()
    val duplicates = mutableSetOf<String>()

    values.forEach { value ->
        if (value.isNotBlank() && !seen.add(value)) {
            duplicates.add(value)
        }
    }

    return duplicates
}

private val PLACEHOLDER_MARKER = Regex(
    """\b(todo(?!\()|tbd|fixme|lorem ipsum|xxx)\b""",
    RegexOption.IGNORE_CASE,
)

/**
 * Hosts that can never cite anything. Reserved documentation domains such as
 * `example.com` are deliberately absent: this repository uses them throughout its test
 * fixtures, so rejecting them here would fail valid fixtures without catching anything
 * the bundled bank does. Authoritative-host enforcement for shipped content lives in
 * `InitialCurriculumContentQualityTest` instead.
 */
private val PLACEHOLDER_URL_HOSTS = setOf("localhost", "127.0.0.1", "0.0.0.0")
