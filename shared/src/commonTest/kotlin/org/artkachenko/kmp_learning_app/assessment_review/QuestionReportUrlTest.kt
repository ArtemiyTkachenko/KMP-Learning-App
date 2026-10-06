package org.artkachenko.kmp_learning_app.assessment_review

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class QuestionReportUrlTest {
    @Test
    fun aPlainQuestionBuildsTheExactFormUrl() {
        assertEquals(
            "https://github.com/ArtemiyTkachenko/KMP-Learning-App/issues/new" +
                "?template=question-report.yml" +
                "&title=%5BQuestion%5D%20android_process_model_001" +
                "&question_id=android_process_model_001" +
                "&app_version=0.1.0%20%281%29",
            questionReportUrl("android_process_model_001", "0.1.0 (1)"),
        )
    }

    @Test
    fun theUrlOpensTheQuestionReportFormOnTheRepository() {
        val url = questionReportUrl("q1", "1.0")

        assertTrue(url.startsWith("https://github.com/ArtemiyTkachenko/KMP-Learning-App/issues/new?"))
        assertEquals("question-report.yml", url.queryParameters()["template"])
    }

    @Test
    fun theQuestionIdAndAppVersionArePrefilled() {
        val parameters = questionReportUrl("q1", "1.0").queryParameters()

        assertEquals("q1", parameters["question_id"])
        assertEquals("1.0", parameters["app_version"])
        assertEquals("[Question] q1", parameters["title"])
    }

    @Test
    fun reservedAndNonAsciiCharactersArePercentEncodedInEveryValue() {
        val url = questionReportUrl(
            questionId = "a b&c#d?e/f+g=h é",
            appVersion = "1.0 β+&#?/",
        )

        assertEquals(
            "a%20b%26c%23d%3Fe%2Ff%2Bg%3Dh%20%C3%A9",
            url.rawParameter("question_id"),
        )
        assertEquals("%5BQuestion%5D%20a%20b%26c%23d%3Fe%2Ff%2Bg%3Dh%20%C3%A9", url.rawParameter("title"))
        assertEquals("1.0%20%CE%B2%2B%26%23%3F%2F", url.rawParameter("app_version"))
        // Only the one separator before the query: nothing in a value ends the path or the query.
        assertEquals(1, url.count { it == '?' })
        assertFalse('#' in url)
        assertFalse(' ' in url)
    }

    /** A crafted ID stays one value; it cannot add a parameter or override one the app sets. */
    @Test
    fun aCraftedQuestionIdCannotInjectParameters() {
        val parameters = questionReportUrl("q1&labels=bug&template=other.yml", "1.0").queryParameters()

        assertEquals(listOf("template", "title", "question_id", "app_version"), parameters.keys.toList())
        assertEquals("question-report.yml", parameters["template"])
        assertEquals("q1&labels=bug&template=other.yml", parameters["question_id"])
    }

    @Test
    fun onlyTheQuestionIdentityAndBuildAreCarried() {
        val url = questionReportUrl("q1", "1.0")

        assertEquals(
            setOf("template", "title", "question_id", "app_version"),
            url.queryParameters().keys,
        )
        assertFalse("body" in url)
        assertFalse("explanation" in url)
    }
}

private fun String.rawParameter(name: String): String? =
    substringAfter('?').split('&').firstOrNull { it.startsWith("$name=") }?.substringAfter('=')

private fun String.queryParameters(): Map<String, String> =
    substringAfter('?').split('&').associate { pair ->
        pair.substringBefore('=') to percentDecode(pair.substringAfter('='))
    }

private fun percentDecode(value: String): String {
    val bytes = mutableListOf<Byte>()
    var index = 0
    while (index < value.length) {
        if (value[index] == '%') {
            bytes += value.substring(index + 1, index + 3).toInt(16).toByte()
            index += 3
        } else {
            bytes += value[index].code.toByte()
            index++
        }
    }
    return bytes.toByteArray().decodeToString()
}
